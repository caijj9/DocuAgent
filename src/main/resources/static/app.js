'use strict';
const $=id=>document.getElementById(id);
const state={courseId:null,sessionId:null,mode:'qa',questions:new Map(),locks:new Set(),abort:null};
const names={chat:'学习问答',practice:'课件练习',wrong:'错题与收藏',materials:'课程资料'};
let toastTimer;
function toast(text){$('toast').textContent=text;$('toast').hidden=false;clearTimeout(toastTimer);toastTimer=setTimeout(()=>$('toast').hidden=true,5500);}
function element(tag,cls,text){const e=document.createElement(tag);if(cls)e.className=cls;if(text!==undefined)e.textContent=text;return e;}
async function api(url,body,options={}){
 const response=await fetch(url,{method:body?'POST':'GET',...options,headers:body instanceof FormData?{}:{'Content-Type':'application/json'},body:body instanceof FormData?body:body?JSON.stringify(body):undefined});
 let result;try{result=await response.json();}catch{throw Error('服务暂时不可用，请检查是否已经启动');}
 if(!response.ok||result.code!==0)throw Error(result.msg||'操作没有完成，请重试');return result.data;
}
function lock(name,on){on?state.locks.add(name):state.locks.delete(name);const busy=state.locks.size>0;
 $('courseSelect').disabled=busy;$('createCourse').disabled=busy;$('sendButton').disabled=busy||!state.courseId;
 $('newSession').disabled=busy||!state.courseId;$('generateButton').disabled=busy||!state.courseId;$('uploadButton').disabled=busy||!state.courseId;
 $('cancelAnswer').hidden=!state.locks.has('chat');
}
function chooseView(view){document.querySelectorAll('.view').forEach(e=>e.classList.toggle('active',e.id===view+'View'));document.querySelectorAll('.nav').forEach(e=>e.classList.toggle('active',e.dataset.view===view));$('viewName').textContent=names[view];if(view==='materials')loadDocuments();if(view==='wrong')loadWrong();}
document.querySelectorAll('.nav').forEach(e=>e.addEventListener('click',()=>chooseView(e.dataset.view)));
function requireCourse(){if(state.courseId)return true;toast('请先创建或选择一门课程');$('courseDialog').showModal();return false;}
async function loadCourses(selected){const courses=await api('/course');$('courseSelect').replaceChildren();
 if(!courses.length){$('courseSelect').append(new Option('先创建一门课程',''));state.courseId=null;$('courseName').textContent='选择一门课程';lock('init',false);return;}
 for(const c of courses)$('courseSelect').append(new Option(c.name,c.id));
 $('courseSelect').value=String(selected||courses[0].id);await changeCourse();
}
async function changeCourse(){state.courseId=Number($('courseSelect').value)||null;state.sessionId=null;state.questions.clear();
 $('courseName').textContent=$('courseSelect').selectedOptions[0]?.textContent||'选择一门课程';
 clearMessages();$('questionList').replaceChildren(element('div','empty-state','选一个知识点，生成本课程的练习题。'));$('wrongList').replaceChildren();
 document.querySelector('.suggestions').hidden=!$('courseName').textContent.includes('编译');await loadDocuments();lock('init',false);
}
$('courseSelect').addEventListener('change',()=>changeCourse().catch(e=>toast(e.message)));
$('createCourse').onclick=()=>$('courseDialog').showModal();$('closeCourseDialog').onclick=()=>$('courseDialog').close();
$('courseForm').onsubmit=async event=>{event.preventDefault();const name=$('newCourseName').value.trim();if(!name)return;const button=event.submitter;button.disabled=true;
 try{const response=await fetch('/course?'+new URLSearchParams({name,description:$('newCourseDescription').value.trim()}),{method:'POST'});const result=await response.json();if(result.code!==0)throw Error(result.msg);$('courseDialog').close();$('courseForm').reset();await loadCourses(result.data);toast('课程已创建，接下来上传一份课件');chooseView('materials');}catch(e){toast(e.message);}finally{button.disabled=false;}};
const welcome=$('welcome');
function clearMessages(){$('messages').replaceChildren(welcome);welcome.hidden=false;$('questionInput').value='';}
$('newSession').onclick=()=>{state.sessionId=null;clearMessages();toast('已开始新对话');};
function mode(value){if(state.locks.has('chat'))return;state.mode=value;$('qaMode').classList.toggle('selected',value==='qa');$('agentMode').classList.toggle('selected',value==='agent');$('composerHint').textContent=value==='qa'?'回答核对依据后，将逐字显示':'可查询答题记录，或按题目编号收藏';$('questionInput').placeholder=value==='qa'?'问问当前课程的知识点，也可以继续追问…':'例如：我最近哪些题答错过？或：帮我收藏题目 ID 11';}
$('qaMode').onclick=()=>mode('qa');$('agentMode').onclick=()=>mode('agent');
$('welcomeUpload').onclick=()=>chooseView('materials');
document.querySelectorAll('[data-question]').forEach(b=>b.onclick=()=>{$('questionInput').value=b.dataset.question;$('questionInput').focus();});
function scrollMessages(){const list=$('messages');list.scrollTop=list.scrollHeight;}
function message(role,text){welcome.hidden=true;const article=element('article','message '+role);article.append(element('div','avatar',role==='user'?'我':'✦'));
 const content=element('div','message-content'),body=element('div','answer-text',text),status=element('div','message-status');content.append(element('div','message-label',role==='user'?'你':'DocuAgent'),body,status);article.append(content);$('messages').append(article);scrollMessages();return {article,content,body,status};
}
function showReferences(view,references){view.refs?.remove();const list=element('div','reference-list');for(const ref of references){const details=element('details','reference');details.dataset.index=ref.index;details.append(element('summary','',`[${ref.index}] ${ref.documentName}`),element('pre','',ref.snippet));list.append(details);}view.refs=list;view.content.append(list);}
function finalText(view,text,references){view.body.replaceChildren();let last=0;for(const m of text.matchAll(/\[(\d+)\]/g)){view.body.append(document.createTextNode(text.slice(last,m.index)));const ref=references.find(r=>r.index===Number(m[1]));if(ref){const button=element('button','citation',m[0]);button.type='button';button.setAttribute('aria-label','展开引用'+m[1]);button.onclick=()=>{const d=view.refs?.querySelector(`[data-index="${ref.index}"]`);if(d){d.open=true;d.scrollIntoView({behavior:'smooth',block:'nearest'});}};view.body.append(button);}else view.body.append(document.createTextNode(m[0]));last=m.index+m[0].length;}view.body.append(document.createTextNode(text.slice(last)));}
function showTools(view,traces){if(!traces?.length)return;const d=element('details','tool-details');d.append(element('summary','',`已执行 ${traces.length} 次学习工具操作`));for(const t of traces){d.append(element('p','',t.tool+' · '+t.parameters),element('pre','',JSON.stringify(JSON.parse(t.result),null,2)));}view.content.append(d);}
async function streamAnswer(body,view,signal){
 const response=await fetch('/qa/stream',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify(body),signal});
 if(!response.headers.get('content-type')?.includes('text/event-stream')){const r=await response.json();throw Error(r.msg||'回答请求未完成');}
 if(!response.body)throw Error('浏览器不支持流式读取');
 const reader=response.body.getReader(),decoder=new TextDecoder();let buffer='',done=false,refs=[],text='';
 function packet(raw){const lines=raw.split('\n');let event='message',data=[];for(const line of lines){if(line.startsWith('event:'))event=line.slice(6).trim();if(line.startsWith('data:'))data.push(line.slice(5).trimStart());}if(!data.length)return;const value=JSON.parse(data.join('\n'));
  if(event==='status')view.status.textContent=value.text+'…';
  if(event==='references'){refs=value;showReferences(view,refs);}
  if(event==='delta'){text+=value.text;view.body.append(document.createTextNode(value.text));view.status.textContent='正在显示已核对的回答…';scrollMessages();}
  if(event==='error')throw Error(value.msg||'回答没有完成');
  if(event==='done'){done=true;refs=value.references||refs;showReferences(view,refs);finalText(view,value.answer,refs);}
 }
 try{while(true){const part=await reader.read();buffer+=decoder.decode(part.value,{stream:!part.done}).replace(/\r\n/g,'\n');let boundary;while((boundary=buffer.indexOf('\n\n'))>=0){packet(buffer.slice(0,boundary));buffer=buffer.slice(boundary+2);}if(part.done)break;}if(buffer.trim())packet(buffer);if(!done)throw Error('连接已结束，回答未完整返回，请重试');}finally{reader.releaseLock();}
}
$('chatForm').onsubmit=async event=>{event.preventDefault();const question=$('questionInput').value.trim();if(!question||state.locks.size||!requireCourse())return;
 message('user',question);$('questionInput').value='';const view=message('assistant','');view.status.textContent='正在准备回答…';lock('chat',true);const started=performance.now();state.abort=new AbortController();
 try{if(!state.sessionId){const session=await api('/session',{courseId:state.courseId});state.sessionId=session.sessionId;}
  const body={question,courseId:state.courseId,sessionId:state.sessionId};if(state.mode==='qa')await streamAnswer(body,view,state.abort.signal);else{const result=await api('/agent/chat',body,{signal:state.abort.signal});showReferences(view,result.references||[]);finalText(view,result.answer,result.references||[]);showTools(view,result.toolCalls);}
  view.status.textContent=`已完成 · ${((performance.now()-started)/1000).toFixed(1)} 秒`;
 }catch(e){view.status.textContent=e.name==='AbortError'?'已停止显示':'未完成：'+e.message;view.status.classList.add('error-text');if(!view.body.textContent)view.body.textContent=e.name==='AbortError'?'可以重新提问，或开始新对话。':'本次未获得完整回答，请稍后重试。';}
 finally{state.abort=null;lock('chat',false);scrollMessages();$('questionInput').focus();}
};
$('cancelAnswer').onclick=()=>state.abort?.abort();$('questionInput').onkeydown=e=>{if(e.key==='Enter'&&!e.shiftKey&&!e.isComposing){e.preventDefault();$('chatForm').requestSubmit();}};

async function loadDocuments(){if(!state.courseId){$('documentCount').textContent='0';$('documentList').replaceChildren(element('div','empty-state','先创建课程，再添加资料。'));return;}
 try{const docs=await api('/document?courseId='+state.courseId);$('documentCount').textContent=docs.filter(d=>d.status==='DONE').length;$('documentList').replaceChildren();if(!docs.length)$('documentList').append(element('div','empty-state','这门课还没有课件。添加一份资料后，就可以提问和练习。'));
  const statuses={DONE:'可用于学习',PARSING:'正在解析',INDEXING:'正在建立索引',FAILED:'处理失败'};for(const d of docs){const row=element('article','document-row'),info=element('div','file-info');info.append(element('b','',d.originalName),element('small','',d.charCount?`${d.charCount.toLocaleString()} 字符`:'等待解析'));row.append(element('span','file-icon','▱'),info,element('span','document-status'+(d.status==='FAILED'?' failed':''),statuses[d.status]||d.status));$('documentList').append(row);}
 }catch(e){toast(e.message);}}
$('refreshDocuments').onclick=loadDocuments;$('uploadFile').onchange=()=>{$('chosenFile').textContent=$('uploadFile').files[0]?.name||'尚未选择文件';};
$('uploadForm').onsubmit=async e=>{e.preventDefault();if(!requireCourse()||state.locks.size)return;const file=$('uploadFile').files[0];if(!file)return;if(file.size>50*1024*1024)return toast('文件超过 50 MB，请先拆分课件');lock('upload',true);$('uploadStatus').classList.remove('error-text');$('uploadStatus').textContent='正在解析资料并建立检索索引，请稍等…';
 try{const data=new FormData();data.append('courseId',state.courseId);data.append('file',file);const result=await api('/document/upload',data);if(result.status!=='DONE')throw Error('资料处理失败，请检查文件是否包含可提取的文字');$('uploadStatus').textContent='资料已就绪，现在可以提问或生成练习。';$('uploadForm').reset();$('chosenFile').textContent='尚未选择文件';await loadDocuments();toast('课件处理完成');}catch(e){$('uploadStatus').textContent=e.message;$('uploadStatus').classList.add('error-text');await loadDocuments();}finally{lock('upload',false);}};
function renderQuestion(q,index){state.questions.set(q.id,q);const card=element('article','question-card'),meta=element('div','question-meta');meta.append(element('span','',`${index?'练习 '+index+' · ':''}${q.type==='single_choice'?'判断式单选':'简答题'} · 题号 #${q.id}`),element('span','',`满分 ${q.maxScore} 分`));card.append(meta,element('h3','',q.stem));const form=element('form');
 if(q.type==='single_choice'){const choices=element('div','options');for(const [i,option]of q.options.entries()){const label=element('label','option'),radio=element('input');radio.type='radio';radio.name='answer-'+q.id;radio.value=String.fromCharCode(65+i);radio.required=true;label.append(radio,element('span','',option));choices.append(label);}form.append(choices);}else{const answer=element('textarea','short-answer');answer.name='shortAnswer';answer.rows=4;answer.maxLength=5000;answer.required=true;answer.placeholder='用自己的话回答，尽量写清每个要点…';form.append(answer);}
 const submit=element('button','primary','提交并查看批改'),grade=element('div');submit.type='submit';form.append(submit);card.append(form,grade);
 form.onsubmit=async event=>{event.preventDefault();if(submit.disabled)return;const userAnswer=q.type==='single_choice'?form.querySelector('input:checked')?.value:form.querySelector('textarea').value.trim();if(!userAnswer)return;submit.disabled=true;submit.textContent='正在批改…';
  // 网络重试沿用同一份载荷的requestId；用户改答案后创建新请求，防止重复计错。
  if(form.lastPayload?.userAnswer!==userAnswer)form.lastPayload={questionId:q.id,userAnswer,durationMs:0,requestId:crypto.randomUUID()};
  try{const result=await api('/answer/submit',form.lastPayload);grade.replaceChildren();grade.className='grade';const score=element('div');score.append(element('strong','',Number(result.score).toFixed(2)),document.createTextNode('/ '+result.maxScore+' 分'));grade.append(score,element('p','',result.comment));if(result.hitPoints.length)grade.append(element('p','',`已掌握：${result.hitPoints.join('；')}`));if(result.missedPoints.length)grade.append(element('p','missed',`待巩固：${result.missedPoints.join('；')}`));grade.append(element('p','',result.isCorrect?'这次回答正确。':'已自动记入错题本。'));submit.textContent='再次提交';}catch(e){toast(e.message);submit.textContent='重试提交';}finally{submit.disabled=false;}
 };return card;
}
$('generateForm').onsubmit=async event=>{event.preventDefault();if(state.locks.size||!requireCourse())return;lock('generate',true);$('generateStatus').classList.remove('error-text');$('generateStatus').textContent='正在依据课件出题并核对答案，可能需要一些时间…';
 try{const questions=await api('/question/generate',{courseId:state.courseId,count:Number($('questionCount').value),type:$('questionType').value,keyword:$('topicInput').value.trim()||null});$('questionList').replaceChildren(...questions.map((q,i)=>renderQuestion(q,i+1)));$('generateStatus').textContent=`已生成 ${questions.length} 道题。请结合课件复核自动生成的内容。`;}catch(e){$('generateStatus').textContent=e.message+'，本次没有新增练习题。';$('generateStatus').classList.add('error-text');}finally{lock('generate',false);}};
async function loadWrong(){if(!state.courseId){$('wrongList').replaceChildren(element('div','empty-state','选择课程后查看错题。'));return;}
 try{const rows=await api('/wrong-question?'+new URLSearchParams({courseId:state.courseId,page:1,size:50}));$('wrongSummary').textContent=rows.length?'最近 '+rows.length+' 道错题或收藏':'当前课程暂无错题';$('wrongList').replaceChildren();if(!rows.length)$('wrongList').append(element('div','empty-state','还没有错题或收藏。练习中答错的题目会自动出现在这里。'));
  for(const row of rows){const card=element('article','question-card'),meta=element('div','question-meta');meta.append(element('span','',row.source==='manual'?'手动收藏':'自动记录'),element('span','',row.wrongCount?'累计答错 '+row.wrongCount+' 次':'尚未答错'));card.append(meta,element('h3','',row.stem));card.append(element('p','answer-preview','最近作答：'+(row.myAnswer||'尚未作答')));const correct=element('p','answer-preview','参考答案：'+row.correctAnswer);card.append(correct);
   const retry=element('button','secondary','再练一次');retry.onclick=async()=>{retry.disabled=true;try{const q=await api('/question/'+row.questionId);if(q.courseId!==state.courseId)throw Error('这道题不属于当前课程');chooseView('practice');$('questionList').replaceChildren(renderQuestion(q));$('generateStatus').textContent='正在重练这道题；之前的答错次数会保留。';}catch(e){toast(e.message);}finally{retry.disabled=false;}};card.append(retry);$('wrongList').append(card);
   if(row.type==='single_choice')api('/question/'+row.questionId).then(q=>{const selected=q.options.find(o=>o.startsWith(row.correctAnswer+'. '));if(selected)correct.textContent='参考答案：'+selected;}).catch(()=>{});
  }
 }catch(e){toast(e.message);}}
$('refreshWrong').onclick=loadWrong;
lock('init',true);loadCourses().catch(e=>{lock('init',false);toast(e.message);});
