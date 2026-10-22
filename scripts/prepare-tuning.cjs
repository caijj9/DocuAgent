// 仅创建docuagent_eval_*实验库和data/eval下的独立索引，不清理原库、原文件或原索引。
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),{execFileSync}=require('node:child_process');
const {root,query,value}=require('./local-db.cjs');
const directory=path.join(root,'data/eval');fs.mkdirSync(directory,{recursive:true});
const planFile=path.join(directory,'stage3-plan.json');
const previous=fs.existsSync(planFile)&&!process.argv.includes('--new')?JSON.parse(fs.readFileSync(planFile,'utf8')):null;
if(previous?.experiments.length===5){console.log('Existing complete plan: '+planFile);process.exit(0);}
const stamp=new Date().toISOString().replace(/\D/g,'').slice(0,14);
const jar=path.join(root,'target/DocuAgent-0.0.1-SNAPSHOT.jar');
const java=process.env.JAVA_EXE||'E:/jdk17/bin/java.exe';
const cacheFile=path.join(directory,'embedding-cache.json');
const cache=fs.existsSync(cacheFile)?JSON.parse(fs.readFileSync(cacheFile,'utf8')):{};
const originalIndex=path.join(root,'data/vector-store.json');
for(const v of Object.values(JSON.parse(fs.readFileSync(originalIndex,'utf8'))))cache[v.text]=v.embedding;
const variants=[{name:'baseline',size:500,overlap:100,topK:10,rerank:false},{name:'size800',size:800,overlap:100,topK:10,rerank:false},
 {name:'overlap0',size:500,overlap:0,topK:10,rerank:false},{name:'topk5',size:500,overlap:100,topK:5,rerank:false},{name:'rerank',size:500,overlap:100,topK:10,rerank:true}];
const text=fs.readFileSync(path.join(root,'eval/fixtures/隔离验收资料.txt'),'utf8');
const sqlText=s=>"CONVERT(X'"+Buffer.from(s,'utf8').toString('hex')+"' USING utf8mb4)";
async function embed(texts,stats){
 const missing=[...new Set(texts)].filter(t=>!cache[t]);
 for(let i=0;i<missing.length;i+=10){const batch=missing.slice(i,i+10);
  const r=await fetch('https://api.siliconflow.cn/v1/embeddings',{method:'POST',headers:{Authorization:'Bearer '+process.env.AI_API_KEY,'Content-Type':'application/json'},body:JSON.stringify({model:'BAAI/bge-m3',input:batch}),signal:AbortSignal.timeout(120000)});
  if(!r.ok){let error={};try{error=await r.json();}catch{}throw Error('Embedding HTTP '+r.status+' code='+error.code+' '+String(error.message||error.error?.message||'').replace(/sk-[A-Za-z0-9]+/g,'[redacted]').slice(0,240));}const b=await r.json();
  if(b.data?.length!==batch.length)throw Error('Embedding批量数量异常');
  for(const v of b.data){if(!Number.isInteger(v.index)||v.index<0||v.index>=batch.length||v.embedding?.length!==1024||!v.embedding.every(Number.isFinite))throw Error('Embedding格式异常');cache[batch[v.index]]=v.embedding;}
  stats.calls++;stats.tokens+=b.usage?.prompt_tokens??b.usage?.total_tokens??0;if(!b.usage)stats.missingUsage++;
  fs.writeFileSync(cacheFile,JSON.stringify(cache));console.log('Embedded '+(i+batch.length)+'/'+missing.length);
 }
}
async function main(){
 if(!process.env.AI_API_KEY)throw Error('缺少AI_API_KEY');
 const plan=previous||{createdAt:new Date().toISOString(),originalIndexSha256:crypto.createHash('sha256').update(fs.readFileSync(originalIndex)).digest('hex'),experiments:[]};
 for(const config of variants){
  if(plan.experiments.some(e=>e.name===config.name)){console.log('Already prepared '+config.name);continue;}
  const db='docuagent_eval_'+stamp+'_'+config.name,workdir=path.join(directory,stamp+'_'+config.name);fs.mkdirSync(workdir,{recursive:true});
  if(!/^docuagent_eval_[0-9]+_[a-z0-9]+$/.test(db))throw Error('实验库边界校验失败');
  query(fs.readFileSync(path.join(root,'sql/init.sql'),'utf8').replace(/\bdocuagent\b/g,db));
  if(Number(query('SELECT COUNT(*) FROM course;',db))!==0)throw Error('实验库已有数据，拒绝覆盖');
  for(const table of ['course','document','chunk','question','answer_record','wrong_question']){
   const columns=query(`SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='docuagent' AND TABLE_NAME='${table}' ORDER BY ORDINAL_POSITION;`).split(/\r?\n/);
   if(columns.some(c=>!/^\w+$/.test(c)))throw Error('原库列名异常');const names=columns.map(c=>'`'+c+'`').join(',');
   query(`INSERT INTO ${db}.${table}(${names}) SELECT ${names} FROM docuagent.${table};`);
  }
  query("INSERT INTO course(id,name,description) VALUES(900001,'评测·隔离课程B','合成资料，仅用于隔离验收');",db);
  const fixture=path.join(workdir,'eval/fixtures/隔离验收资料.txt');fs.mkdirSync(path.dirname(fixture),{recursive:true});fs.copyFileSync(path.join(root,'eval/fixtures/隔离验收资料.txt'),fixture);
  query(`INSERT INTO document(id,course_id,original_name,storage_path,file_type,file_size,char_count,status) VALUES(900001,900001,'隔离验收资料.txt','eval/fixtures/隔离验收资料.txt','txt',${Buffer.byteLength(text)},${text.length},'DONE');`,db);
  let vectors,corpus;
  {
   const exported=path.join(workdir,'corpus.json'),log=fs.openSync(path.join(workdir,'export.log'),'w');
   try{execFileSync(java,['-jar',jar,'--spring.profiles.active=export-corpus','--spring.main.web-application-type=none','--docuagent.chunk.size='+config.size,'--docuagent.chunk.overlap='+config.overlap,'--docuagent.export.path='+exported],{cwd:root,env:{...process.env,DB_PASSWORD:value('password')},stdio:['ignore',log,log],timeout:120000});}finally{fs.closeSync(log);}
   corpus=JSON.parse(fs.readFileSync(exported,'utf8'));vectors={};
   query('UPDATE chunk SET deleted=1 WHERE document_id IN ('+corpus.map(d=>d.id).join(',')+');',db);
   let id=Number(query('SELECT MAX(id) FROM chunk;',db));
   for(const doc of corpus)for(const [index,part]of doc.chunks.entries()){
    id++;query(`INSERT INTO chunk(id,document_id,chunk_index,content,char_count) VALUES(${id},${doc.id},${index},${sqlText(part)},${part.length});`,db);
    vectors[id]={id:String(id),text:part,metadata:{courseId:doc.courseId,documentId:doc.id,chunkId:id}};
   }
  }
  query(`INSERT INTO chunk(id,document_id,chunk_index,content,char_count) VALUES(900001,900001,0,${sqlText(text)},${text.length});`,db);
  vectors[900001]={id:'900001',text,metadata:{courseId:900001,documentId:900001,chunkId:900001}};
  const seedUsage={calls:0,tokens:0,missingUsage:0};await embed(Object.values(vectors).map(v=>v.text),seedUsage);
  for(const v of Object.values(vectors))v.embedding=cache[v.text];
  const indexPath=path.join(workdir,'vector-store.json');fs.writeFileSync(indexPath,JSON.stringify(vectors));
  const experiment={...config,db,workdir,indexPath,seedUsage,chunkCount:Object.keys(vectors).length,courses:{compiler:1,empty:2,isolation:900001}};
  plan.experiments.push(experiment);fs.writeFileSync(planFile,JSON.stringify(plan,null,2));console.log('Prepared '+config.name+' chunks='+experiment.chunkCount);
 }
 if(crypto.createHash('sha256').update(fs.readFileSync(originalIndex)).digest('hex')!==plan.originalIndexSha256)throw Error('原索引发生变化');
 console.log('Plan ready: '+planFile);
}
main().catch(e=>{console.error(e.message);process.exitCode=1});
