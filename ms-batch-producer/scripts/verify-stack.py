import os,sys,json,time,uuid,zipfile,io,urllib.request,subprocess
from pathlib import Path
A=Path(__file__).resolve().parents[1]
C=['docker','compose']
if os.getenv('ENV_FILE'): C+=['--env-file',os.environ['ENV_FILE']]
C+=['-f',str(A/'docker-compose.yml')]
BASE=os.getenv('PRODUCER_BASE_URL','http://localhost:8080')
def run(args,input=None,timeout=90):
 p=subprocess.run(args,input=input,text=True,capture_output=True,timeout=timeout)
 if p.returncode:raise RuntimeError('Command failed: '+p.stderr[-2000:])
 return p.stdout

def sql(query):
 return run(C+['exec','-T','sqlserver','sh','-c','export SQLCMDPASSWORD="$MSSQL_SA_PASSWORD"; exec /opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -C -b -d reto_db -h -1 -W'], 'SET NOCOUNT ON;\n'+query+'\nGO\n')
def scalar(query):return int(sql(query).strip())
def get(url):return json.load(urllib.request.urlopen(BASE+url,timeout=10))
def upload(n=2):
 z=io.BytesIO()
 with zipfile.ZipFile(z,'w',zipfile.ZIP_DEFLATED) as f:
  f.writestr('XML-Example/','');f.writestr('__MACOSX/._XML-Example','metadata')
  for i in range(n):f.writestr(f'XML-Example/{i}.xml',f'<root><value>{i}</value></root>')
  f.writestr('XML-Example/invalid.xml','<root>')
 boundary='boundary'+uuid.uuid4().hex
 body=(f'--{boundary}\r\nContent-Disposition: form-data; name="file"; filename="check.zip"\r\nContent-Type: application/zip\r\n\r\n'.encode()+z.getvalue()+f'\r\n--{boundary}--\r\n'.encode())
 req=urllib.request.Request(BASE+'/api/batch/launch',body,{'Content-Type':'multipart/form-data; boundary='+boundary},method='POST')
 result=json.load(urllib.request.urlopen(req,timeout=15));print('UPLOAD',result,flush=True);return result

def wait(fn,timeout=100):
 end=time.monotonic()+timeout
 while time.monotonic()<end:
  try:
   result=fn()
   if result:return result
  except (OSError,RuntimeError):pass
  time.sleep(1)
 raise AssertionError('Timed out waiting')
def loaded(job,published=True):
 def check():
  s=get('/api/batch/jobs/'+str(job['jobExecutionId']))
  assert s['status']!='FAILED',s
  return s if s['status']=='COMPLETED' and (not published or s['pendingEvents']==0) else None
 return wait(check)
def completed(job,n):
 u=job['uploadId']
 wait(lambda:scalar(f"SELECT COUNT(*) FROM batch_record WHERE upload_id='{u}' AND status='PROCESSED'")==n)
 assert scalar(f"SELECT COUNT(*) FROM batch_record WHERE upload_id='{u}' AND status='FAILED'")==1
 assert scalar(f"SELECT COUNT(*) FROM batch_record WHERE upload_id='{u}' AND status='PENDING'")==0
 assert scalar(f"SELECT COUNT(*) FROM consumed_event c JOIN batch_record r ON r.id=c.record_id WHERE r.upload_id='{u}'")==n
 print('PASS completed',u,n,'PROCESSED; 1 FAILED',flush=True)
def publish(text,key='test'):
 return run(C+['exec','-T','kafka','/opt/kafka/bin/kafka-console-producer.sh','--bootstrap-server','kafka:29092','--topic','record-ready-topic-v2','--property','parse.key=true','--property','key.separator=|'],key+'|'+text+'\n')
def sample(job):
 parts=sql("SELECT TOP(1) CONCAT(event_id,'|',record_id,'|',business_key,'|',upload_id,'|',CONVERT(VARCHAR(40),created_at,126)) FROM outbox_event WHERE upload_id='"+job['uploadId']+"' ORDER BY id").strip().split('|')
 return {'eventId':parts[0],'schemaVersion':2,'recordId':int(parts[1]),'businessKey':int(parts[2]),'uploadId':parts[3],'occurredAt':parts[4]}
def lagzero():
 out=run(C+['exec','-T','kafka','/opt/kafka/bin/kafka-consumer-groups.sh','--bootstrap-server','kafka:29092','--describe','--group','record-processor-v2'])
 rows=[line.split() for line in out.splitlines() if line.startswith('record-processor-v2 ')]
 return len(rows)==3 and all(row[5]=='0' or (row[3]=='-' and row[4]=='0') for row in rows)

if __name__=='__main__':
 job=upload();loaded(job);completed(job,2)
 event=sample(job);before=scalar('SELECT COUNT(*) FROM consumed_event')
 publish(json.dumps(event),str(event['recordId']));wait(lagzero)
 assert scalar('SELECT COUNT(*) FROM consumed_event')==before
 print('PASS duplicate event',flush=True)
 rejected=scalar('SELECT COUNT(*) FROM rejected_event')
 publish('{broken');event['eventId']=str(uuid.uuid4());event['recordId']=999999999;publish(json.dumps(event))
 wait(lambda:scalar('SELECT COUNT(*) FROM rejected_event')==rejected+2)
 nextjob=upload();loaded(nextjob);completed(nextjob,2);wait(lagzero)
 print('PASS quarantine and subsequent valid events',flush=True)
 if '--faults' not in sys.argv:
  print('SMOKE CHECKS PASSED. --faults also stops and restarts B, Kafka and SQL.',flush=True)
  sys.exit(0)
 run(C+['stop','consumer']);job=upload();loaded(job)
 assert scalar("SELECT COUNT(*) FROM batch_record WHERE upload_id='"+job['uploadId']+"' AND status='PENDING'")==2
 run(C+['start','consumer']);completed(job,2)
 print('PASS consumer downtime',flush=True)
 run(C+['stop','kafka']);job=upload();s=loaded(job,False);assert s['pendingEvents']==2,s
 run(C+['start','kafka']);loaded(job);completed(job,2)
 print('PASS Kafka downtime and outbox retry',flush=True)
 # Existing listener must not acknowledge even a duplicate while SQL is unavailable.
 event=sample(job);wait(lagzero)
 run(C+['stop','sqlserver']);publish(json.dumps(event),str(event['recordId']))
 time.sleep(12)
 assert not lagzero(), 'Offset must not advance while SQL is unavailable'
 run(C+['start','sqlserver']);wait(lagzero,150)
 print('PASS SQL downtime without premature offset commit',flush=True)
 print('ALL CHECKS PASSED',flush=True)
