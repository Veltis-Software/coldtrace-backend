"""Exercise real gateway, brownfield IAM and monitoring; never write tokens to evidence."""
import json, os, urllib.request, urllib.error, uuid, time
from datetime import datetime, timezone
from pathlib import Path

base=os.environ.get('API_BASE_URL','http://localhost:18080')
key=os.environ.get('GATEWAY_API_KEY','coldtrace-local-demo-key')
results=[]
def request(method,path,body=None,headers=None,expected=200):
    req=urllib.request.Request(base+path, data=None if body is None else json.dumps(body).encode(),
        headers={'Content-Type':'application/json',**(headers or {})},method=method)
    try:
        with urllib.request.urlopen(req,timeout=15) as response:
            status=response.status;raw=response.read();correlation=response.headers.get('X-Correlation-Id')
    except urllib.error.HTTPError as error:
        status=error.code;raw=error.read();correlation=error.headers.get('X-Correlation-Id')
    assert status==expected,(path,status,raw.decode()[:300])
    result=json.loads(raw) if raw else None
    results.append({'method':method,'path':path,'status':status,'correlationId':correlation})
    return result
def identity(email,label):
    payload={'legalName':label,'commercialName':label,'contactEmail':email,
        'firstName':'Local','lastName':'Fixture','email':email,'password':'Local-TP1-only-2026'}
    # Explicit local fixtures; fail if pointed at an external service.
    assert base.startswith(('http://localhost:','http://127.0.0.1:')),'Local endpoints only'
    req=urllib.request.Request(base+'/api/v1/organization-sign-ups',data=json.dumps(payload).encode(),headers={'Content-Type':'application/json'},method='POST')
    try:
        with urllib.request.urlopen(req,timeout=15) as response: response.read()
    except urllib.error.HTTPError as error:
        if error.code not in (409,422): raise
    signin=request('POST','/api/v1/authentication/sign-in',{'email':email,'password':payload['password']})
    return {'Authorization':'Bearer '+signin['token']}

one=identity('tp1-local-one@coldtrace.example','TP1 Local Organization One')
two=identity('tp1-local-two@coldtrace.example','TP1 Local Organization Two')
context=request('GET','/api/v1/session/context',headers=one)
assert context['organizationId']==1,'The demo replica expects an empty isolated IAM database with organization 1'
batch={'gatewayUuid':'11111111-1111-1111-1111-111111111111','readings':[{
    'deviceUuid':'22222222-2222-2222-2222-222222222222','sequenceNumber':9000000,
    'recordedAt':datetime.now(timezone.utc).isoformat(),'temperature':4,'humidity':60}]}
headers={'X-Gateway-Key':key,'Idempotency-Key':str(uuid.uuid4()),'X-Correlation-Id':'tp1-local-smoke'}
assert request('POST','/api/v1/telemetry/batches',batch,headers,202)=={'accepted':1,'duplicated':0}
for _ in range(2):
    assert request('POST','/api/v1/telemetry/batches',batch,headers,202)=={'accepted':0,'duplicated':1}
request('POST','/api/v1/telemetry/batches',batch,{**headers,'X-Gateway-Key':'wrong-key'},401)
state=request('GET','/api/v1/assets/1/state',headers=one)
assert state['organizationId']==1 and state['status']=='NORMAL'
request('GET','/api/v1/assets/1/state',headers=two,expected=404)
request('GET','/api/v1/assets/1/state',expected=401)
page=request('GET','/api/v1/assets/states?page=0&size=10',headers=one)
assert page['size']==10 and len(page['items'])<=10
existing_ids={row['id'] for row in request('GET','/api/v1/alerts',headers=one)}
deviation={'gatewayUuid':batch['gatewayUuid'],'readings':[{**batch['readings'][0],
    'sequenceNumber':9000001,'recordedAt':datetime.now(timezone.utc).isoformat(),'temperature':12}]}
assert request('POST','/api/v1/telemetry/batches',deviation,{**headers,'Idempotency-Key':str(uuid.uuid4())},202)['accepted']==1
deadline=time.monotonic()+20
incident=None
while time.monotonic()<deadline:
    candidates=request('GET','/api/v1/alerts',headers=one)
    incident=next((row for row in candidates if row['id'] not in existing_ids and row['type']=='THRESHOLD_BREACHED' and row['asset_id']==1),None)
    if incident: break
    time.sleep(.25)
assert incident,'No threshold incident arrived through Pub/Sub'
detail=request('GET',f"/api/v1/alerts/{incident['id']}",headers=one)
assert detail['notifications'][0]['status']=='AVAILABLE'
assert detail['notifications'][0]['deep_link']==f"/alerts/{incident['id']}"
request('GET',f"/api/v1/alerts/{incident['id']}",headers=two,expected=404)
out=Path(__file__).resolve().parents[1]/'evidence'/'local-smoke.json'
out.parent.mkdir(exist_ok=True)
out.write_text(json.dumps({'observedAt':datetime.now(timezone.utc).isoformat(),'baseUrl':base,'checks':results},indent=2)+'\n',encoding='utf-8')
print(json.dumps({'checks':len(results),'evidence':str(out)}))
