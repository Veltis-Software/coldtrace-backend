"""Generate a local API evidence collection. No credentials or JWTs are persisted."""
import json
from pathlib import Path

target=Path(__file__).resolve().parents[1]/'evidence'
target.mkdir(exist_ok=True)
requests=[]
def item(name,method,path,body=None,headers=None,code=200,extra=None):
    checks=[f"pm.test('HTTP {code}', () => pm.response.to.have.status({code}));"]+(extra or [])
    request={'method':method,'url':'{{baseUrl}}'+path,'header':[{'key':k,'value':v} for k,v in (headers or {}).items()]}
    if body is not None:
        request['header'].append({'key':'Content-Type','value':'application/json'})
        request['body']={'mode':'raw','raw':json.dumps(body,indent=2)}
    requests.append({'name':name,'request':request,'event':[{'listen':'test','script':{'type':'text/javascript','exec':checks}}]})
item('Authenticate local fixture','POST','/api/v1/authentication/sign-in',{'email':'tp1-local-one@coldtrace.example','password':'Local-TP1-only-2026'},extra=["pm.collectionVariables.set('token', pm.response.json().token);"])
item('Resolve owning organization','GET','/api/v1/session/context',headers={'Authorization':'Bearer {{token}}'})
batch={'gatewayUuid':'11111111-1111-1111-1111-111111111111','readings':[{'deviceUuid':'22222222-2222-2222-2222-222222222222','sequenceNumber':9200000,'recordedAt':'{{recordedAt}}','temperature':12,'humidity':60}]}
headers={'X-Gateway-Key':'{{gatewayKey}}','Idempotency-Key':'{{batchKey}}','X-Correlation-Id':'tp1-postman'}
item('Ingest deviation','POST','/api/v1/telemetry/batches',batch,headers,202)
item('Repeat batch without duplication','POST','/api/v1/telemetry/batches',batch,headers,202,["pm.test('duplicate', () => pm.expect(pm.response.json().duplicated).to.eql(1));"])
item('Reject invalid gateway key','POST','/api/v1/telemetry/batches',batch,{**headers,'X-Gateway-Key':'wrong-key'},401)
item('Read latest asset state','GET','/api/v1/assets/1/state',headers={'Authorization':'Bearer {{token}}'})
item('Read projected states','GET','/api/v1/assets/states?page=0&size=10',headers={'Authorization':'Bearer {{token}}'})
item('Read generated alerts','GET','/api/v1/alerts',headers={'Authorization':'Bearer {{token}}'})
collection={'info':{'name':'ColdTrace TP1 local vertical flow','schema':'https://schema.getpostman.com/json/collection/v2.1.0/collection.json'},'variable':[{'key':'baseUrl','value':'http://127.0.0.1:18080'},{'key':'gatewayKey','value':'coldtrace-local-demo-key'}],'event':[{'listen':'prerequest','script':{'type':'text/javascript','exec':["if (pm.info.requestName === 'Ingest deviation') { pm.collectionVariables.set('batchKey', pm.variables.replaceIn('{{$guid}}')); pm.collectionVariables.set('recordedAt', new Date().toISOString()); }"]}}],'item':requests}
(target/'coldtrace-tp1.postman_collection.json').write_text(json.dumps(collection,indent=2)+'\n',encoding='utf-8')
