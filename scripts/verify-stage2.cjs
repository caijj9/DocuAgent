const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {root,query}=require('./local-db.cjs');
const dir=path.join(root,'data/evidence');fs.mkdirSync(dir,{recursive:true});
const stateFile=path.join(dir,'stage2-state.json');
const state=fs.existsSync(stateFile)?JSON.parse(fs.readFileSync(stateFile,'utf8')):{runId:Date.now().toString(36),completed:[]};
const recordsFile=path.join(root,'eval/stage2-results.json');
const records=fs.existsSync(recordsFile)?JSON.parse(fs.readFileSync(recordsFile,'utf8')):[];
const save=()=>fs.writeFileSync(stateFile,JSON.stringify(state,null,2));
const record=(phase,result)=>{records.push({phase,at:new Date().toISOString(),...result});fs.writeFileSync(recordsFile,JSON.stringify(records,null,2));};
async function request(url,body,method=body?'POST':'GET'){
  const response=await fetch('http://127.0.0.1:18081'+url,{method,headers:body?{'Content-Type':'application/json'}:{},
    body:body?JSON.stringify(body):undefined,signal:AbortSignal.timeout(300000)});
  assert.equal(response.status,200);
  const result=await response.json();if(result.code!==0)throw new Error(url+' business='+result.code+' '+result.msg);
  return result.data;
}
function questions(){
  return state.questions.map(view=>JSON.parse(query(`SELECT JSON_OBJECT('id',id,'type',type,'stem',stem,'answer',answer,'keyPoints',key_points,'sourceChunkId',source_chunk_id) FROM question WHERE id=${Number(view.id)};`)));
}
const countWrong=()=>Number(query('SELECT COUNT(*) FROM wrong_question WHERE deleted=0;'));
const wrongCount=id=>JSON.parse(query(`SELECT JSON_OBJECT('rows',COUNT(*),'wrongCount',COALESCE(MAX(wrong_count),0),'source',MAX(source)) FROM wrong_question WHERE deleted=0 AND question_id=${Number(id)};`));
async function qa(){
  const inputs=['词法分析器读入什么、输出什么，还会过滤哪些内容？','正则表达式r+表示什么，与rr*是什么关系？','上下文无关文法包含哪四个部分？','MySQL事务的四种隔离级别是什么？'];
  const lines=['# 段2真实问答实录','','2026-10-21，真实硅基流动调用；课件为当前编译原理三份资料。',''];
  for(let i=0;i<inputs.length;i++){
    const answer=await request('/qa/ask',{question:inputs[i],courseId:1});
    if(i<3){
      assert.ok(answer.references.length>0,'资料内问题缺引用');assert.match(answer.answer,/\[1\]/);
      const evidence=answer.references.map(r=>r.snippet).join('').replace(/\s+/g,'');
      const anchors=[['读入字符流','过滤空白'],['r+等价于rr*'],['终结符号','非终结符号','产生式','起始符号']][i];
      for(const anchor of anchors)assert.ok(evidence.includes(anchor),'引用缺少答案依据：'+anchor);
    }
    else{assert.match(answer.answer,/资料中没有相关内容/);assert.equal(answer.references.length,0);}
    record('qa',{question:inputs[i],...answer});lines.push('## Q'+(i+1),'',inputs[i],'','```json',JSON.stringify(answer,null,2),'```','');
    console.log('QA '+(i+1)+' refs='+answer.references.length+' '+answer.answer.slice(0,100));
  }
  const empty=await request('/qa/ask',{question:'r+是什么？',courseId:2});assert.equal(empty.references.length,0);
  record('qa-empty',{...empty});fs.writeFileSync(path.join(root,'eval/qa-samples.md'),lines.join('\n'));
}
async function generate(){
  if(state.questions?.length){console.log('Reuse generated questions: '+state.questions.map(q=>q.id));return;}
  const generated=await request('/question/generate',{courseId:1,count:5,type:'mixed',keyword:'词法分析器的三个作用、词法单元的模式和词素定义、词法单元属性、词法错误的恢复策略'});
  assert.equal(generated.length,5);assert.ok(generated.some(q=>q.type==='short_answer'));assert.ok(generated.some(q=>q.type==='single_choice'));
  state.questions=generated;save();record('questions',{questions:generated});console.log('Generated question IDs: '+generated.map(q=>q.id));
  for(const q of questions()){
    const source=query(`SELECT c.content FROM chunk c JOIN document d ON d.id=c.document_id WHERE c.id=${Number(q.sourceChunkId)} AND d.course_id=1 AND c.deleted=0 AND d.deleted=0;`);
    assert.ok(source.length>0);record('question-source',{questionId:q.id,stem:q.stem,answer:q.answer,keyPoints:JSON.parse(q.keyPoints),sourceChunkId:q.sourceChunkId,source});
  }
  const before=Number(query('SELECT COUNT(*) FROM question;'));
  const response=await fetch('http://127.0.0.1:18081/question/generate',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({courseId:1,count:11,type:'mixed'})});
  assert.equal((await response.json()).code,400);assert.equal(Number(query('SELECT COUNT(*) FROM question;')),before);
  record('invalid-request',{countBefore:before,countAfter:before,businessCode:400});
}
async function answers(){
  const all=questions().sort((a,b)=>(a.type==='single_choice'?-1:1)-(b.type==='single_choice'?-1:1));
  if(state.initialWrong===undefined){state.initialWrong=countWrong();save();}
  if(!state.initialAnswersVerified){
  for(let i=0;i<all.length;i++){
    const q=all[i],correct=i>=3;const points=JSON.parse(q.keyPoints);
    const userAnswer=q.type==='single_choice'?(correct?q.answer:(q.answer==='A'?'B':'A')):(correct?q.answer+'\n'+points.join('\n'):'完全不知道，这是一段与课程无关的测试答案。');
    const result=await request('/answer/submit',{questionId:q.id,userAnswer,durationMs:1000,requestId:`s2-${state.runId}-initial-${q.id}`});
    assert.equal(result.isCorrect,correct);record('answer-initial',{questionId:q.id,expectedCorrect:correct,...result});
  }
  assert.equal(countWrong(),state.initialWrong+3);
  state.initialAnswersVerified=true;save();
  }
  const repeat=all[0],userAnswer=repeat.answer==='A'?'B':'A';
  const before=wrongCount(repeat.id).wrongCount;
  const body={questionId:repeat.id,userAnswer,durationMs:1000,requestId:`s2-${state.runId}-repeat-${repeat.id}`};
  // 稳定requestId使脚本中断重跑也不会重复计数。
  const result=await request('/answer/submit',body);
  const retries=await Promise.all([request('/answer/submit',body),request('/answer/submit',body),request('/answer/submit',body)]);
  assert.ok(retries.every(r=>r.answerRecordId===result.answerRecordId));assert.equal(wrongCount(repeat.id).rows,1);
  assert.equal(wrongCount(repeat.id).wrongCount,2);
  record('idempotency',{questionId:repeat.id,answerRecordId:result.answerRecordId,retries:3,wrong:wrongCount(repeat.id)});
  const correctChoice=await request('/answer/submit',{questionId:repeat.id,userAnswer:repeat.answer,durationMs:1000,requestId:`s2-${state.runId}-choice-correct-${repeat.id}`});
  assert.equal(Number(correctChoice.score),10);assert.equal(correctChoice.isCorrect,true);assert.equal(wrongCount(repeat.id).wrongCount,2);
  record('choice-correct',{...correctChoice,wrong:wrongCount(repeat.id)});
  const short=all.find(q=>q.type==='short_answer');assert.ok(short);
  const points=JSON.parse(short.keyPoints);
  for(const [name,userAnswer] of [['full',short.answer+'\n'+points.join('\n')],['partial',points[0]],['wrong','完全不知道，这是一段与课程无关的测试答案。']]){
    const graded=await request('/answer/submit',{questionId:short.id,userAnswer,durationMs:1000,requestId:`s2-${state.runId}-${name}-grounded-${short.id}`});
    if(name==='full')assert.equal(Number(graded.score),10);
    if(name==='partial')assert.ok(Number(graded.score)>0&&Number(graded.score)<10,'部分分不合理');
    if(name==='wrong')assert.equal(Number(graded.score),0);
    record('short-'+name,{...graded});console.log('Short answer '+name+' score='+graded.score);
  }
  state.manualQuestionId=all.slice(3).find(q=>q.id!==short.id)?.id;
  if(!state.manualQuestionId)state.manualQuestionId=all.at(-1).id;
  save();
  const conflict=await fetch('http://127.0.0.1:18081/answer/submit',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({...body,userAnswer:'DIFFERENT'})});
  assert.equal((await conflict.json()).code,1104);
  const list=await request('/wrong-question?courseId=1&page=1&size=20');
  assert.equal(list.filter(r=>r.source==='auto'&&all.some(q=>q.id===r.questionId)).length,3);
  assert.ok(list.every(r=>!Object.hasOwn(r,'deleted')));record('wrong-list',{rows:list});
}
async function tools(){
  const history=await request('/agent/chat',{courseId:1,question:'我上次编译原理这门课错在哪？请先读取最近30天的答题记录再总结。'});
  const historyCall=history.toolCalls.find(t=>t.tool==='readAnswerRecords');assert.ok(historyCall);
  const historyData=JSON.parse(historyCall.result);assert.equal(historyData.success,true);assert.ok(historyData.data.length>0);
  record('tool-history',history);console.log('History tool called');
  const manual=await request('/agent/chat',{courseId:1,question:'帮我收藏题目ID '+state.manualQuestionId+'，记到错题本。'});
  assert.ok(manual.toolCalls.some(t=>t.tool==='saveWrongQuestion'));assert.equal(wrongCount(state.manualQuestionId).wrongCount,0);
  assert.equal(wrongCount(state.manualQuestionId).source,'manual');record('tool-manual',{...manual,database:wrongCount(state.manualQuestionId)});
  const search=await request('/agent/chat',{courseId:1,question:'请实际调用检索资料工具，解释正则表达式r+的含义。'});
  const searchCall=search.toolCalls.find(t=>t.tool==='searchCourseMaterial');assert.ok(searchCall);
  assert.equal(JSON.parse(searchCall.result).success,true);assert.ok(JSON.parse(searchCall.result).data.length>0);
  record('tool-search',search);console.log('Three business tools verified');
}
async function sessions(){
  const session=await request('/session',{courseId:1});record('session-create',session);
  let turn=0;
  // 数学符号比较的失败仍保留在固定15题Q14中；此处单独验证会话指代能力。
  for(const question of ['词法分析器读入什么、输出什么？','那它还会过滤哪些内容？','上面那个组件会把词素放到哪里？']){
    const answer=await request('/qa/ask',{question,sessionId:session.sessionId});record('session-turn',{question,...answer});
    assert.ok(answer.references.length>0);if(turn++>0)assert.match(answer.rewrittenQuestion,/词法分析器/);
    if(turn===3)assert.match(answer.answer,/符号表/);console.log('Rewritten: '+answer.rewrittenQuestion);
  }
  const list=await request('/session');assert.ok(list.some(s=>s.sessionId===session.sessionId));
  const mismatch=await fetch('http://127.0.0.1:18081/qa/ask',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({question:'测试',sessionId:session.sessionId,courseId:2})});
  assert.equal((await mismatch.json()).code,1107);
  state.sessionId=session.sessionId;save();
}
const phases={qa,generate,answers,tools,sessions};
async function main(){
  const selected=process.argv.find(a=>a.startsWith('--phase='))?.slice(8)||'all';
  for(const [name,run] of Object.entries(phases))if(selected==='all'||selected===name){
    if(state.completed.includes(name)&&!process.argv.includes('--force')){console.log('Already verified: '+name);continue;}
    state.completed=state.completed.filter(previous=>previous!==name);save();
    await run();if(!state.completed.includes(name))state.completed.push(name);save();console.log('PASS '+name);
  }
}
main().catch(error=>{record('failure',{error:error.message});console.error(error.message);process.exitCode=1});
