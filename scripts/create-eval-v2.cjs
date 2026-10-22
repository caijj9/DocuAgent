// 固定v1题目不改写；追加题由已有课件原文与明确标注的合成隔离资料编写。
const fs=require('node:fs'),path=require('node:path');
const {root}=require('./local-db.cjs');
const original=JSON.parse(fs.readFileSync(path.join(root,'eval/questions-v1.md'),'utf8').match(/```json\s*([\s\S]*?)```/)[1]);
const patterns={Q01:['字符流','词法单元','空白','注释'],Q02:['终结','非终结','产生式','起始'],Q03:['FIRST','FOLLOW','首','右|跟'],Q04:['r\\+','rr\\*'],Q08:['符号表','属性|标识','链接|指向'],Q09:['词法单元','语法.*树|AST'],Q10:['语法.*树|AST','符号表','语义'],Q14:['r\\+','r\\*','零|0','一|1|多'],Q15:['词法单元','语法.*树|AST']};
const questions=original.map(q=>({...q,type:['direct','exact_term'].includes(q.type)?'answerable':q.type,endpoint:'qa',answerChecks:patterns[q.id]||[],course:q.courseId===1?'compiler':'empty'}));
questions.push(
{id:'Q16',type:'followup',course:'compiler',endpoint:'qa',history:[{role:'USER',content:'词法分析器读入什么、输出什么？'}],question:'那它还会过滤哪些内容？',resolvedQuestion:'词法分析器还会过滤哪些内容？',answer:'空白、换行、制表符和注释。',answerChecks:['空白','换行','制表符','注释'],checks:[{document:'2-词法分析(ch3).pdf',anchors:['过滤空白']}]},
{id:'Q17',type:'course_isolation',course:'compiler',endpoint:'qa',question:'数据库验收课程的固定校验词是什么？',answer:'应拒答；答案只存在另一课程的合成资料。',checks:[],answerChecks:[],refusal:true},
{id:'Q18',type:'course_isolation',course:'isolation',endpoint:'qa',question:'本数据库验收课程的固定校验词是什么？',answer:'蓝色海豚B7。只能引用当前课程的隔离验收资料。',checks:[{document:'隔离验收资料.txt',anchors:['固定校验词是蓝色海豚B7']}],answerChecks:['蓝色海豚B7'],refusal:false},
{id:'Q19',type:'course_isolation',course:'isolation',endpoint:'qa',question:'上下文无关文法CFG包含哪四个部分？',answer:'应拒答；当前合成资料没有CFG定义，不能引用编译原理课件。',checks:[],answerChecks:[],refusal:true},
{id:'Q20',type:'tool_failure',scenario:'missing_question',course:'compiler',endpoint:'agent',question:'帮我收藏题目ID 99999999，记到错题本。',expectedTool:'saveWrongQuestion',expectFailure:true},
{id:'Q21',type:'tool_failure',scenario:'invalid_days',course:'compiler',endpoint:'agent',question:'请读取最近4000天的答题记录；如果工具不支持这个范围，直接明确告知失败，不要改成其他天数。',expectedTool:'readAnswerRecords',expectFailure:true},
{id:'Q22',type:'tool_failure',scenario:'wrong_course',course:'isolation',endpoint:'agent',question:'帮我收藏题目ID 11，记到错题本。',expectedTool:'saveWrongQuestion',expectFailure:true},
{id:'Q23',type:'tool_failure',scenario:'history_control',course:'compiler',endpoint:'agent',question:'请读取最近30天的答题记录，再告诉我哪些题答错过。',expectedTool:'readAnswerRecords',expectFailure:false},
{id:'Q24',type:'tool_failure',scenario:'search_control',course:'compiler',endpoint:'agent',question:'请实际调用检索资料工具，解释词法分析器读入什么、输出什么。',expectedTool:'searchCourseMaterial',expectFailure:false},
{id:'Q25',type:'tool_failure',scenario:'no_tool_control',course:'compiler',endpoint:'agent',question:'你好，只需简单打个招呼，不要调用工具，也不要收藏题目。',expectedTool:null,expectFailure:false}
);
for(const q of questions){q.refusal??=['unanswerable','course_isolation'].includes(q.type);q.answerChecks??=[];q.checks??=[];}
const file=path.join(root,'eval/questions-v2.json');
if(fs.existsSync(file)&&!process.argv.includes('--replace'))throw Error('固定评测集已存在；修改请另建版本，不能隐式覆盖');
fs.writeFileSync(file,JSON.stringify({version:2,source:'真实编译原理课件及已标注的合成隔离资料',questions},null,2));
console.log(JSON.stringify({total:questions.length,types:questions.reduce((m,q)=>(m[q.type]=(m[q.type]||0)+1,m),{})}));
