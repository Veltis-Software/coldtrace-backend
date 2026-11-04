"""Import the previous web server/db.json into an empty local organization.

Uses the real REST API, remaps IDs and derives locations from legacy labels.
Never imports users, passwords, computed flags or historical workflow approvals.
Set COLDTRACE_FIXTURE_PASSWORD; run --help for the required local target.
"""
import argparse
import collections
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import urllib.error
import urllib.parse
import urllib.request
import uuid


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--base-url', required=True)
    parser.add_argument('--organization-id', type=int, required=True)
    parser.add_argument('--email', required=True)
    parser.add_argument('--summary', type=Path, required=True)
    parser.add_argument('--resume', action='store_true', help='Reuse records with matching legacy UUIDs after an interrupted import')
    args = parser.parse_args()
    url = urllib.parse.urlparse(args.base_url)
    if url.scheme != 'http' or url.hostname not in ('127.0.0.1', 'localhost'):
        parser.error('Fixture imports are restricted to a local HTTP API.')
    password = os.environ.get('COLDTRACE_FIXTURE_PASSWORD')
    if not password:
        parser.error('Set COLDTRACE_FIXTURE_PASSWORD without committing it.')
    token = None

    def api(method, path, body=None):
        headers = {'Content-Type': 'application/json'}
        if token:
            headers['Authorization'] = 'Bearer ' + token
        request = urllib.request.Request(args.base_url.rstrip('/') + '/api/v1/' + path,
                                        data=json.dumps(body).encode() if body is not None else None,
                                        headers=headers, method=method)
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                raw = response.read()
                return json.loads(raw) if raw else None
        except urllib.error.HTTPError as error:
            raise RuntimeError(f'{method} {path}: HTTP {error.code}: {error.read().decode()[:500]}') from None

    auth = api('POST', 'authentication/sign-in', {'email': args.email, 'password': password})
    token = auth['token']
    org = auth['organizationId']
    prefix = f'organizations/{org}/'
    # Do not overwrite or mix existing operational records.
    existing_assets=api('GET', prefix+'assets')
    existing_gateways=api('GET', prefix+'gateways')
    existing_devices=api('GET', prefix+'iot-devices')
    if not args.resume and (existing_assets or existing_gateways or existing_devices):
        raise SystemExit('The target organization must have no assets, gateways or devices.')
    existing_readings=api('GET', prefix+'sensor-readings') if args.resume else []
    data = json.loads(args.source.read_text(encoding='utf-8-sig'))
    selected = lambda key: [r for r in data.get(key, []) if int(r.get('organizationId', -1)) == args.organization_id]
    assets, gateways, devices = selected('assets'), selected('gateways'), selected('iot-devices')
    locations = {r['name']: r['id'] for r in api('GET', prefix + 'locations')}
    for record in gateways + assets:
        label = record.get('location') or 'Almacén principal'
        if label not in locations:
            resource = api('POST', prefix + 'locations', {'name': label, 'address': label,
                           'type': 'WAREHOUSE', 'description': 'Centro de conservación refrigerada', 'status': 'active'})
            locations[label] = resource['id']
    gateway_map, asset_map, device_map, device_aliases = {}, {}, {}, {}
    gateways_by_location={g['locationId']:g['id'] for g in existing_gateways}
    asset_legacy = {str(a['id']): a for a in assets}
    for g in gateways:
        resource = next((r for r in existing_gateways if r['uuid']==g['uuid']),None)
        gateway_payload = {k:g[k] for k in ('uuid','name','network','status')} | {'locationId':locations[g.get('location') or 'Almacén principal']}
        try:
            gateway_payload['uuid']=str(uuid.UUID(g['uuid']))
        except ValueError:
            gateway_payload['uuid']=str(uuid.uuid5(uuid.NAMESPACE_URL,f"coldtrace/legacy/{args.organization_id}/gateway/{g['uuid']}"))
        if resource is None:
            resource=next((r for r in existing_gateways if r['uuid']==gateway_payload['uuid']),None)
        if resource is None:
            resource=api('POST',prefix+'gateways',gateway_payload)
        elif resource['uuid']!=gateway_payload['uuid']:
            resource=api('PUT',prefix+f"gateways/{resource['id']}",gateway_payload)
        gateway_map[str(g['id'])] = resource['id']
        gateways_by_location[resource['locationId']]=resource['id']
    for a in assets:
        resource = next((r for r in existing_assets if r['uuid']==a['uuid']),None)
        if resource is None:
            resource = api('POST', prefix + 'assets', {k: a[k] for k in ('uuid', 'name', 'type', 'capacity', 'description', 'status')} |
                           {'locationId': locations[a.get('location') or 'Almacén principal']})
        asset_map[str(a['id'])] = resource['id']
    for s in selected('asset-settings'):
        settings = {k: s[k] for k in ('uuid','assetTypes','iotDeviceTypes','minimumTemperature','maximumTemperature',
                    'maximumHumidity','calibrationFrequencyDays','temperatureUnit','humidityUnit','weightUnit') if k in s}
        aid = asset_map.get(str(s.get('assetId')))
        api('PUT', prefix + (f'assets/{aid}/settings' if aid else 'asset-settings/default'), settings)
    for d in devices:
        aid = str(d.get('assetId'))
        asset = asset_legacy.get(aid)
        gid = gateway_map.get(str(d.get('gatewayId') or (asset or {}).get('gatewayId')))
        if asset:
            location_id=locations[asset.get('location') or 'Almacén principal']
            if location_id not in gateways_by_location:
                gateway_uuid=str(uuid.uuid5(uuid.NAMESPACE_URL,f'coldtrace/legacy/{args.organization_id}/location/{location_id}'))
                resource=api('POST',prefix+'gateways',{'uuid':gateway_uuid,'name':asset.get('location') or 'Almacén principal',
                    'network':'LAN / Wi-Fi','status':'active','locationId':location_id})
                gateways_by_location[location_id]=resource['id']
            gid=gateways_by_location[location_id]
        if gid is None:
            # Legacy unassigned devices have no gateway; place them at the first
            # imported gateway without assigning a supervised asset.
            gid = next(iter(gateway_map.values()))
        payload = {k: d[k] for k in ('uuid','deviceType','model','measurementType','measurementParameters',
                   'readingFrequencySeconds','status','calibrationStatus') if k in d}
        try:
            payload['uuid']=str(uuid.UUID(d['uuid']))
        except ValueError:
            payload['uuid']=str(uuid.uuid5(uuid.NAMESPACE_URL,f"coldtrace/legacy/{args.organization_id}/{d['uuid']}"))
        device_aliases[d['uuid']]=payload['uuid']
        for key in ('lastCalibrationDate','nextCalibrationDate'):
            if d.get(key) and d[key] not in ('-', '—'):
                format = '%d/%m/%Y' if '/' in d[key] else '%d %b %Y'
                payload[key] = dt.datetime.strptime(d[key], format).date().isoformat()
        if 'lastCalibrationDate' not in payload:
            payload['lastCalibrationDate']=(dt.date.today()-dt.timedelta(days=30)).isoformat()
        if 'nextCalibrationDate' not in payload:
            payload['nextCalibrationDate']=(dt.date.today()+dt.timedelta(days=150)).isoformat()
        payload |= {'gatewayId': gid, 'assetId': asset_map.get(aid)}
        resource=next((r for r in existing_devices if r['uuid'] in (d['uuid'],payload['uuid'])),None)
        if resource is None:
            resource = api('POST', prefix + 'iot-devices', payload)
        elif resource['uuid']!=payload['uuid']:
            resource = api('PUT',prefix+f"iot-devices/{resource['id']}",payload)
        device_map[str(d['id'])] = resource['id']
    for gateway in api('GET',prefix+'gateways'):
        try:
            uuid.UUID(gateway['uuid'])
        except ValueError:
            payload={k:gateway[k] for k in ('name','network','status','locationId')}
            payload['uuid']=str(uuid.uuid5(uuid.NAMESPACE_URL,f"coldtrace/legacy/{args.organization_id}/location/{gateway['locationId']}"))
            api('PUT',prefix+f"gateways/{gateway['id']}",payload)
    device_asset = {str(d['id']): str(d.get('assetId')) for d in devices}
    device_parameters = {str(d['id']):set(d['measurementParameters']) for d in devices}
    online_devices={str(d['id']) for d in devices if d.get('status')!='offline'}
    readings = [r for r in data.get('sensor-readings', []) if str(r.get('assetId')) in asset_map
                and str(r.get('iotDeviceId')) in device_map
                and str(r.get('iotDeviceId')) in online_devices
                and device_asset[str(r['iotDeviceId'])] == str(r['assetId'])]
    def timestamp(r):
        value = dt.datetime.fromisoformat(r['recordedAt'].replace('Z','+00:00'))
        return value.replace(tzinfo=dt.timezone.utc) if value.tzinfo is None else value
    shift = dt.datetime.now(dt.timezone.utc) - max(map(timestamp, readings)) if readings else dt.timedelta()
    if existing_readings:
        shift=min(map(timestamp,existing_readings))-min(map(timestamp,readings))
    existing_keys={(r['assetId'],r['iotDeviceId'],timestamp(r)) for r in existing_readings}
    states = collections.Counter()
    omitted_measurements=collections.Counter()
    for r in sorted(readings, key=timestamp):
        fields={'temperature':'temperature','humidity':'humidity','motionDetected':'motion','imageCaptured':'image',
                'batteryLevel':'battery','signalStrength':'signal'}
        supported=device_parameters[str(r['iotDeviceId'])]
        payload={k:r[k] for k,parameter in fields.items() if k in r and parameter in supported}
        for k,parameter in fields.items():
            if k in r and r[k] is not None and parameter not in supported:omitted_measurements[k]+=1
        if not any(value is not None for value in payload.values()):continue
        payload |= {'assetId':asset_map[str(r['assetId'])], 'iotDeviceId':device_map[str(r['iotDeviceId'])],
                    'recordedAt':(timestamp(r)+shift).isoformat()}
        if (payload['assetId'],payload['iotDeviceId'],timestamp(payload)) in existing_keys:continue
        saved = api('POST', prefix+'sensor-readings', payload)
        states[str(saved.get('isOutOfRange', 'evaluated'))] += 1
    persisted=api('GET',prefix+'sensor-readings')
    states=collections.Counter(str(r.get('isOutOfRange','evaluated')) for r in persisted)
    summary = {'sourceSha256':hashlib.sha256(args.source.read_bytes()).hexdigest(),
               'sourceOrganizationId':args.organization_id,'targetOrganizationId':org,
               'locations':len(locations),'legacyGateways':len(gateway_map),'gateways':len(api('GET',prefix+'gateways')),'assets':len(asset_map),
               'devices':len(device_map),'readings':len(persisted),'readingStates':dict(states),
               'unsupportedLegacyMeasurementsOmitted':dict(omitted_measurements),
               'legacyDeviceAliases':device_aliases,
               'timestamps':'Relative intervals preserved; newest reading shifted to import time',
               'missingCalibrationDates':'Fixture defaults: 30 days before and 150 days after import',
               'target':'local REST API','computedFlags':'Evaluated by the backend',
               'excluded':'Legacy authentication, approvals, offline devices and readings outside the selected device/asset relationships'}
    args.summary.parent.mkdir(parents=True, exist_ok=True)
    args.summary.write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf8')
    print(json.dumps(summary,ensure_ascii=False))


if __name__ == '__main__':
    main()
