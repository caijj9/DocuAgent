// 只隔离本脚本系列生成的验收题；有任何其他来源答题记录时拒绝执行。
const fs=require('node:fs'),path=require('node:path'),{root,query}=require('./local-db.cjs');
const file=path.join(root,'data/evidence/stage2-state.json'),state=JSON.parse(fs.readFileSync(file,'utf8'));
if(!state.questions?.length)throw new Error('没有本次验收题');
const ids=state.questions.map(q=>Number(q.id));if(ids.some(id=>!Number.isSafeInteger(id)))throw new Error('无效ID');
const foreign=Number(query(`SELECT COUNT(*) FROM answer_record WHERE question_id IN (${ids}) AND (request_id IS NULL OR request_id NOT LIKE 's2-${state.runId}-%');`));
if(foreign)throw new Error('检测到非本次脚本的答题记录，拒绝隔离');
const backup=path.join(root,'data/backups/stage2-rejected-'+new Date().toISOString().replace(/[:.]/g,'-'));
fs.mkdirSync(backup,{recursive:true});fs.copyFileSync(file,path.join(backup,'state.json'));
fs.copyFileSync(path.join(root,'eval/stage2-results.json'),path.join(backup,'results.json'));
query(`START TRANSACTION; UPDATE wrong_question SET deleted=1 WHERE question_id IN (${ids}); UPDATE answer_record SET deleted=1 WHERE question_id IN (${ids}); UPDATE question SET deleted=1 WHERE id IN (${ids}); COMMIT;`);
state.rejectedAttempts=[...(state.rejectedAttempts||[]),{questionIds:ids,reason:'真实复核发现来源不足或错误评分要点，已保留记录并逻辑隔离',backup}];
delete state.questions;delete state.initialWrong;delete state.manualQuestionId;delete state.initialAnswersVerified;
state.completed=state.completed.filter(phase=>!['generate','answers','tools'].includes(phase));
fs.writeFileSync(file,JSON.stringify(state,null,2));console.log('Isolated only verification question IDs: '+ids.join(','));
