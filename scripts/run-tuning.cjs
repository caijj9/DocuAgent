const fs=require('node:fs'),path=require('node:path'),{spawn}=require('node:child_process');
const {root,value}=require('./local-db.cjs');
const plan=JSON.parse(fs.readFileSync(path.join(root,'data/eval/stage3-plan.json'),'utf8'));
const java=process.env.JAVA_EXE||'E:/jdk17/bin/java.exe',jar=path.join(root,'target/DocuAgent-0.0.1-SNAPSHOT.jar');
const selected=process.argv.find(a=>a.startsWith('--only='))?.slice(7).split(',');
const sleep=ms=>new Promise(r=>setTimeout(r,ms));
async function main(){
 for(const e of plan.experiments.filter(e=>!selected||selected.includes(e.name))){
  const marker=path.join(root,'data/eval/latest-'+e.name+'.json');if(fs.existsSync(marker)&&!process.argv.includes('--force')){console.log('Already completed '+e.name);continue;}
  if(!e.db.startsWith('docuagent_eval_'))throw Error('拒绝将实验指向正式库');
  const log=fs.openSync(path.join(e.workdir,'runtime.log'),'a');
  const app=spawn(java,['-jar',jar,'--server.port=18082','--spring.datasource.url=jdbc:mysql://127.0.0.1:3306/'+e.db+'?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true',
   '--docuagent.vector-store.path='+e.indexPath,'--docuagent.chunk.size='+e.size,'--docuagent.chunk.overlap='+e.overlap,'--docuagent.qa.top-k='+e.topK,'--docuagent.qa.rerank-enabled='+e.rerank],
   {cwd:e.workdir,env:{...process.env,DB_PASSWORD:value('password'),DB_USER:value('username')},stdio:['ignore',log,log],windowsHide:true});
  try{
   let ready=false;for(let i=0;i<90;i++){if(app.exitCode!==null)throw Error('实验服务启动失败，见本机runtime.log');try{const r=await fetch('http://127.0.0.1:18082/hello',{signal:AbortSignal.timeout(1000)});if((await r.json()).code===0){ready=true;break;}}catch{}await sleep(500);}
   if(!ready)throw Error('实验服务启动超时');console.log('RUNNING '+e.name+' db='+e.db);
   await new Promise((resolve,reject)=>{const task=spawn(process.execPath,[path.join(root,'scripts/evaluate-v2.cjs'),'--label='+e.name,...(process.argv.includes('--validation')?['--validation']:[])],{cwd:root,env:process.env,stdio:'inherit',windowsHide:true});task.on('error',reject);task.on('exit',code=>code===0?resolve():reject(Error('评测脚本失败 '+code)));});
  }finally{app.kill();await Promise.race([new Promise(r=>app.once('exit',r)),sleep(3000)]);fs.closeSync(log);}
 }
}
main().catch(e=>{console.error(e.message);process.exitCode=1});
