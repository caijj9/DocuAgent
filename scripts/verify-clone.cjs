// 真正从本地Git仓库克隆；不拷贝凭证、target、upload或向量数据。
const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict'),{spawn,execFileSync}=require('node:child_process');
const {root,query,value}=require('./local-db.cjs');
const stamp=new Date().toISOString().replace(/\D/g,'').slice(0,14),database='docuagent_delivery_'+stamp;
const target=path.join(root,'data/delivery-check',stamp,'DocuAgent');fs.mkdirSync(path.dirname(target),{recursive:true});
const delay=ms=>new Promise(resolve=>setTimeout(resolve,ms));
async function request(url,body){const response=await fetch('http://127.0.0.1:18083'+url,{method:body?'POST':'GET',headers:body instanceof FormData?{}:body?{'Content-Type':'application/json'}:{},body:body instanceof FormData?body:body?JSON.stringify(body):undefined,signal:AbortSignal.timeout(120000)});const result=await response.json();if(result.code!==0)throw Error(url+' business='+result.code+' '+result.msg);return result.data;}
async function main(){
 if(!/^docuagent_delivery_\d+$/.test(database)||!target.startsWith(path.join(root,'data/delivery-check')+path.sep))throw Error('验收目录或数据库边界异常');
 execFileSync('git',['clone','--no-hardlinks',root,target],{encoding:'utf8',stdio:['ignore','pipe','pipe']});
 for(const file of ['.local/application-secrets.properties','target/DocuAgent-0.0.1-SNAPSHOT.jar','data/vector-store.json'])assert.equal(fs.existsSync(path.join(target,file)),false,'clone不应携带本机私有状态：'+file);
 query(fs.readFileSync(path.join(target,'sql/init.sql'),'utf8').replace(/\bdocuagent\b/g,database));
 const logFile=path.join(path.dirname(target),'startup.log'),log=fs.openSync(logFile,'w');
 const app=spawn('cmd.exe',['/d','/s','/c','start.bat'],{cwd:target,env:{...process.env,DOCUAGENT_JAVA_HOME:'E:/jdk17',JAVA_HOME:'E:/jdk17',DB_PASSWORD:value('password'),DB_USER:value('username'),DB_URL:'jdbc:mysql://127.0.0.1:3306/'+database+'?useUnicode=true&characterEncoding=utf8&useSSL=false&allowPublicKeyRetrieval=true',APP_PORT:'18083'},stdio:['ignore',log,log],windowsHide:true});
 try{
  let ready=false;for(let i=0;i<180;i++){if(app.exitCode!==null)throw Error('启动脚本退出，见本机startup.log');try{const r=await fetch('http://127.0.0.1:18083/hello',{signal:AbortSignal.timeout(1000)});if((await r.json()).code===0){ready=true;break;}}catch{}await delay(1000);}
  assert.ok(ready,'新目录首次构建或启动超时');const initialCourses=await request('/course');assert.deepEqual(initialCourses,[]);
  const html=await(await fetch('http://127.0.0.1:18083/index.html')).text();assert.match(html,/DocuAgent/);assert.match(html,/课程资料/);
  const courseResponse=await fetch('http://127.0.0.1:18083/course?'+new URLSearchParams({name:'新目录演示课程',description:'公开合成资料，验证README启动步骤'}),{method:'POST'});const created=await courseResponse.json();assert.equal(created.code,0);
  const data=new FormData();data.append('courseId',created.data);data.append('file',new Blob([fs.readFileSync(path.join(target,'eval/fixtures/演示课件.txt'))],{type:'text/plain'}),'演示课件.txt');const uploaded=await request('/document/upload',data);assert.equal(uploaded.status,'DONE');
  const qa=await request('/qa/ask',{courseId:created.data,question:'词法分析器读入什么、输出什么？'});assert.ok(qa.references.length>0);assert.match(qa.answer,/字符流/);
  const result={at:new Date().toISOString(),sourceCommit:execFileSync('git',['-C',target,'rev-parse','HEAD'],{encoding:'utf8'}).trim(),cloneDirectory:target,database,startupScript:'start.bat',preexistingPrivateConfig:false,preexistingJar:false,preexistingIndex:false,initialCourseCount:0,pageServed:true,firstBuildSucceeded:fs.existsSync(path.join(target,'target/DocuAgent-0.0.1-SNAPSHOT.jar')),uploaded,qa,passed:true};
  fs.writeFileSync(path.join(root,'eval/clone-verification.json'),JSON.stringify(result,null,2));console.log(JSON.stringify({passed:true,initialCourses:0,firstBuildSucceeded:result.firstBuildSucceeded,uploadedStatus:uploaded.status,references:qa.references.length}));
 }finally{if(app.exitCode===null)try{execFileSync('taskkill.exe',['/PID',String(app.pid),'/T','/F'],{stdio:'ignore'});}catch{}fs.closeSync(log);}
}
main().catch(e=>{console.error(e.message);process.exitCode=1});
