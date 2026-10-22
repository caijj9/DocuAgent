const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const {root,query}=require('./local-db.cjs');
const arg=(key,fallback)=>process.argv.find(a=>a.startsWith('--'+key+'='))?.slice(key.length+3)||fallback;
const base=arg('base-url','http://127.0.0.1:18082'),label=arg('label','baseline');
const validation=process.argv.includes('--validation'),runName=validation?'final':label;
const plan=JSON.parse(fs.readFileSync(path.join(root,'data/eval/stage3-plan.json'),'utf8'));
const experiment=plan.experiments.find(e=>e.name===label);if(!experiment)throw Error('实验配置不存在');
const source=fs.readFileSync(path.join(root,'eval/questions-v2.json'),'utf8'),questions=JSON.parse(source).questions;
const prices=JSON.parse(fs.readFileSync(path.join(root,'eval/prices.json'),'utf8'));
const directory=path.join(root,'eval/v2-runs',new Date().toISOString().replace(/[:.]/g,'-')+'-'+runName);fs.mkdirSync(directory,{recursive:true});
const rows=[];const norm=s=>s.replace(/\s+/g,'').toLowerCase();
const symbols=s=>new Set([...s.matchAll(/(?<![A-Za-z0-9_])([A-Za-z][A-Za-z0-9_]*[+*?])(?![A-Za-z0-9_+*?])/g)].map(m=>m[1]));
const zero=()=>({chatCalls:0,embeddingCalls:0,inputTokens:0,outputTokens:0,embeddingTokens:0,missingUsageCalls:0});
const add=(a,b)=>{for(const k of Object.keys(a))a[k]+=b?.[k]||0;return a;};
const cost=u=>(u.inputTokens*prices.chatInputPerMillion+u.outputTokens*prices.chatOutputPerMillion+u.embeddingTokens*prices.embeddingPerMillion)/1e6;
async function call(url,body,method=body?'POST':'GET'){
 const start=performance.now(),response=await fetch(base+url,{method,headers:body?{'Content-Type':'application/json'}:{},body:body?JSON.stringify(body):undefined,signal:AbortSignal.timeout(240000)});
 const result=await response.json(),elapsedMs=Math.round(performance.now()-start);const usageId=response.headers.get('x-usage-id');
 let usage=zero();if(usageId){const r=await(await fetch(base+'/diagnostics/usage/'+usageId)).json();if(r.code===0)usage=r.data;}
 return {http:response.status,body:result,elapsedMs,usage};
}
const wrongSnapshot=()=>query('SELECT question_id,wrong_count,source,deleted FROM wrong_question ORDER BY id;',experiment.db);
function citations(checks,hits,textKey){return checks.every(check=>hits.some(h=>h.documentName===check.document&&check.anchors.some(a=>norm(h[textKey]).includes(norm(a)))));}
async function main(){
 for(const q of questions){let session=null;const started=performance.now(),usage=zero();let row={id:q.id,type:q.type,scenario:q.scenario,question:q.question,expectedAnswer:q.answer,expectedRefusal:q.refusal};
 try{
  const courseId=experiment.courses[q.course];
  if(q.type==='followup'){
   const created=await call('/session',{courseId});if(created.body.code!==0)throw Error('创建会话失败');session=created.body.data.sessionId;
   for(const turn of q.history.filter(t=>t.role==='USER')){const prior=await call('/qa/ask',{question:turn.content,sessionId:session});add(usage,prior.usage);if(prior.body.code!==0)throw Error('历史问答失败');}
  }
  const before=q.endpoint==='agent'?wrongSnapshot():null;
  const result=await call(q.endpoint==='agent'?'/agent/chat':'/qa/ask',{courseId,question:q.question,sessionId:session});add(usage,result.usage);
  row.elapsedMs=Math.round(performance.now()-started);row.targetElapsedMs=result.elapsedMs;row.http=result.http;row.code=result.body.code;row.response=result.body.data;row.error=result.body.code===0?undefined:result.body.msg;
  if(q.endpoint==='agent'){
   const unchanged=before===wrongSnapshot(),traces=result.body.data?.toolCalls||[],matching=traces.find(t=>t.tool===q.expectedTool);
   const toolResult=matching?JSON.parse(matching.result):null,answer=result.body.data?.answer||'';
   let toolCorrect=q.expectedTool===null?result.body.code===0&&traces.length===0:
    q.expectFailure?result.body.code===0&&toolResult?.success===false&&/未成功|失败|不支持|不能|不存在|条件/.test(answer):result.body.code===0&&toolResult?.success===true;
   // 参数本身非法时，在调用工具前明确返回业务错误也是安全的拒绝，不当作工具已经执行。
   if(q.scenario==='invalid_days'&&traces.length===0&&[400,1101,1108].includes(result.body.code)){toolCorrect=true;row.rejectedBeforeTool=true;}
   row.toolCorrect=toolCorrect;row.noMutation=unchanged;row.passed=result.http===200&&toolCorrect&&unchanged;
  }else{
   const refs=result.body.data?.references||[],answer=result.body.data?.answer||'';
   const allowed=new Set(query(`SELECT id FROM document WHERE course_id=${courseId} AND deleted=0;`,experiment.db).split(/\r?\n/).filter(Boolean).map(Number));
   row.scopePassed=refs.every(r=>allowed.has(r.documentId));
   row.citationsPassed=citations(q.checks,refs,'snippet');
   row.answerChecksPassed=q.answerChecks.every(p=>new RegExp(p,'i').test(answer));
   const present=symbols(answer);row.symbolCoverage=[...symbols(q.resolvedQuestion||q.question)].every(s=>present.has(s));
   row.grounded=result.body.code===0&&refs.length>0&&row.citationsPassed&&row.answerChecksPassed&&row.symbolCoverage&&row.scopePassed;
   row.refusalPassed=result.body.code===0&&/资料中没有相关内容/.test(answer)&&refs.length===0;
   row.passed=result.http===200&&row.scopePassed&&(q.refusal?row.refusalPassed:row.grounded);
   if(!q.refusal){
    const probe=await call('/retrieval/search?'+new URLSearchParams({q:result.body.data?.rewrittenQuestion||q.resolvedQuestion||q.question,courseId,mode:'both',fusion:true,rerank:experiment.rerank,topK:3}));
    row.retrievalTop3Passed=probe.body.code===0&&citations(q.checks,probe.body.data||[],'content');row.retrievalTop3=probe.body.data;row.probeUsage=probe.usage;
   }
  }
 }catch(error){row.passed=false;row.error=error.message;row.elapsedMs=Math.round(performance.now()-started);}
 finally{if(session)try{await call('/session/'+session,null,'DELETE');}catch{}}
 row.usage=usage;row.estimatedCost=cost(usage);rows.push(row);fs.writeFileSync(path.join(directory,'results.json'),JSON.stringify(rows,null,2));
 console.log(label+' '+q.id+' '+(row.passed?'PASS':'FAIL')+' '+row.elapsedMs+'ms');
 }
 const qa=rows.filter(r=>r.type!=='tool_failure'),positive=qa.filter(r=>!r.expectedRefusal),tools=rows.filter(r=>r.type==='tool_failure');
 const totalUsage=rows.reduce((m,r)=>add(m,r.usage),zero()),probeUsage=rows.reduce((m,r)=>add(m,r.probeUsage),zero());
 const summary={label:runName,sourceExperiment:label,createdAt:new Date().toISOString(),questionSha256:crypto.createHash('sha256').update(source).digest('hex'),parameters:experiment,total:rows.length,passed:rows.filter(r=>r.passed).length,
  retrieval:{passed:positive.filter(r=>r.retrievalTop3Passed).length,total:positive.length},grounding:{passed:positive.filter(r=>r.grounded).length,total:positive.length},
  tools:{passed:tools.filter(r=>r.toolCorrect&&r.noMutation).length,total:tools.length},refusal:{passed:qa.filter(r=>r.expectedRefusal&&r.refusalPassed).length,total:qa.filter(r=>r.expectedRefusal).length},
  isolation:{passed:qa.filter(r=>r.type==='course_isolation'&&r.passed).length,total:qa.filter(r=>r.type==='course_isolation').length},
  averageElapsedMs:Math.round(rows.reduce((n,r)=>n+r.elapsedMs,0)/rows.length),averageRequestMs:Math.round(rows.reduce((n,r)=>n+(r.targetElapsedMs??r.elapsedMs),0)/rows.length),totalUsage,probeUsage,estimatedTotalCost:cost(totalUsage),estimatedCostPerCase:cost(totalUsage)/rows.length,prices};
 if(totalUsage.chatCalls===0||totalUsage.inputTokens===0)throw Error('真实模型用量统计缺失，不能生成成本基线');
 fs.writeFileSync(path.join(directory,'summary.json'),JSON.stringify(summary,null,2));
 const rate=m=>`${m.passed}/${m.total} (${(100*m.passed/m.total).toFixed(1)}%)`;
 const lines=['# 全量评测 '+label,'','25题，六类；原始输出与失败均保留。正例依据检查使用答案要点、原文锚点、符号对象和课程范围；仍需人工语义核对。','',
 '|指标|结果|','|---|---|',`|检索命中率（Top3完整覆盖）|${rate(summary.retrieval)}|`,`|资料内回答有依据率|${rate(summary.grounding)}|`,`|工具调用正确率|${rate(summary.tools)}|`,
 `|平均请求耗时|${summary.averageRequestMs} ms|`,`|平均场景耗时|${summary.averageElapsedMs} ms|`,`|单场景估算成本|￥${summary.estimatedCostPerCase.toFixed(8)}|`,`|应拒答通过|${rate(summary.refusal)}|`,`|课程隔离通过|${rate(summary.isolation)}|`,'',
 '成本口径：'+prices.basis,'业务用量包含多轮历史准备及回答/复核/工具模型调用；Top3辅助探针和离线重建用量另记，不混入单次问答成本。延迟包含场景内历史准备，辅助探针不计。','',
 '用量：`'+JSON.stringify(totalUsage)+'`','',`总通过${summary.passed}/${summary.total}。`,'','|题号|类型|结果|耗时ms|说明|','|---|---|---|---|---|'];
 for(const r of rows)lines.push('|'+[r.id,r.type,r.passed?'PASS':'FAIL',r.elapsedMs,r.error||r.response?.rewrittenQuestion||r.scenario||''].map(x=>String(x).replace(/\|/g,'/')).join('|')+'|');
 lines.push('','明细：[results.json](results.json)，汇总：[summary.json](summary.json)。','');fs.writeFileSync(path.join(directory,'report.md'),lines.join('\n'));
 fs.mkdirSync(path.join(root,'data/eval'),{recursive:true});fs.writeFileSync(path.join(root,'data/eval/latest-'+runName+'.json'),JSON.stringify({directory,summary},null,2));console.log('RESULT '+JSON.stringify({label:runName,passed:summary.passed,retrieval:summary.retrieval,grounding:summary.grounding,tools:summary.tools,averageElapsedMs:summary.averageElapsedMs}));
}
main().catch(e=>{console.error(e.message);process.exitCode=1});
