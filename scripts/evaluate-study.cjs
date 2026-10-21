// 用同一份固定15题验证第二段QA/拒答/真实会话；不把gold history直接塞给服务。
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const {root}=require('./local-db.cjs');
const source=fs.readFileSync(path.join(root,'eval/questions-v1.md'),'utf8');
const questions=JSON.parse(source.match(/```json\s*([\s\S]*?)```/)[1]);
const directory=path.join(root,'eval/study-runs',new Date().toISOString().replace(/[:.]/g,'-'));
fs.mkdirSync(directory,{recursive:true});const rows=[];
async function call(url,body,method='POST'){
  const r=await fetch('http://127.0.0.1:18081'+url,{method,headers:body?{'Content-Type':'application/json'}:{},body:body?JSON.stringify(body):undefined,signal:AbortSignal.timeout(240000)});
  const result=await r.json();if(result.code!==0)throw Error('business='+result.code+' '+result.msg);return result.data;
}
const norm=s=>s.replace(/\s+/g,'').toLowerCase();
const symbols=s=>new Set([...s.matchAll(/(?<![A-Za-z0-9_])([A-Za-z][A-Za-z0-9_]*[+*?])(?![A-Za-z0-9_+*?])/g)].map(m=>m[1]));
async function main(){
  for(const q of questions){
    let session=null;const start=performance.now();
    try{
      if(q.type==='followup'){
        session=await call('/session',{courseId:q.courseId});
        // 先让系统自己回答上一轮问题，不能把标准答案当成模型记忆。
        for(const turn of q.history.filter(h=>h.role==='USER'))await call('/qa/ask',{question:turn.content,sessionId:session.sessionId});
      }
      const answer=await call('/qa/ask',{question:q.question,courseId:q.courseId,sessionId:session?.sessionId});
      const expectedRefusal=['unanswerable','course_isolation'].includes(q.type);
      const evidence=q.checks.every(check=>answer.references.some(ref=>ref.documentName===check.document&&check.anchors.some(anchor=>norm(ref.snippet).includes(norm(anchor)))));
      const present=symbols(answer.answer),symbolCoverage=[...symbols(q.resolvedQuestion||q.question)].every(s=>present.has(s));
      const passed=expectedRefusal?/资料中没有相关内容/.test(answer.answer)&&answer.references.length===0:evidence&&answer.references.length>0&&symbolCoverage;
      rows.push({id:q.id,type:q.type,passed,citationPassed:evidence&&answer.references.length>0,symbolCoverage,elapsedMs:Math.round(performance.now()-start),question:q.question,expectedAnswer:q.answer,...answer});
      console.log(q.id+' '+q.type+' '+(passed?'PASS':'FAIL')+' refs='+answer.references.length);
    }catch(error){rows.push({id:q.id,type:q.type,passed:false,error:error.message,elapsedMs:Math.round(performance.now()-start)});console.log(q.id+' ERROR '+error.message);}
    finally{if(session){try{await call('/session/'+session.sessionId,null,'DELETE');}catch{}}}
    fs.writeFileSync(path.join(directory,'results.json'),JSON.stringify(rows,null,2));
  }
  const passed=rows.filter(r=>r.passed).length;
  const lines=['# 第二段固定15题QA验收','','评测规则v2：增加运算符对象覆盖检查；问题集及答案锚点不变。旧v1曾把Q14答成r?误算通过，历史报告保留，不与v2直接当作同口径准确率比较。','',
    '题目SHA256：`'+crypto.createHash('sha256').update(source).digest('hex')+'`','',
    '正例自动指标为所需原文锚点是否全部出现在返回引用中，以及问题中的正则运算符对象是否完整出现。rr*不能代替r*本身。仍不能代替人工语义评审。无答案/课程隔离要求明确拒答且无引用；多轮题先运行真实上一轮问答。','',
    '|题号|类型|结果|耗时ms|改写问题|','|---|---|---|---|---|'];
  for(const row of rows)lines.push('|'+[row.id,row.type,row.passed?'PASS':row.error||'FAIL',row.elapsedMs,(row.rewrittenQuestion||'').replace(/\|/g,'/')].join('|')+'|');
  lines.push('','通过：'+passed+'/'+rows.length+'。失败全部保留。','本次明细：`'+path.relative(path.join(root,'eval'),directory).replace(/\\/g,'/')+'/results.json`。','');
  fs.writeFileSync(path.join(directory,'report.md'),lines.join('\n'));fs.writeFileSync(path.join(root,'eval/study-baseline-v1.md'),lines.join('\n'));
  console.log('Result: '+passed+'/'+rows.length);if(rows.some(r=>r.error))process.exitCode=1;
}
main().catch(e=>{console.error(e.message);process.exitCode=1});
