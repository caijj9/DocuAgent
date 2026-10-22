// 汇总实测结果；--apply时在停服、校验原索引、完整备份后应用所选可派生分块。
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),net=require('node:net');
const {root,query}=require('./local-db.cjs');
const planFile=path.join(root,'data/eval/stage3-plan.json'),plan=JSON.parse(fs.readFileSync(planFile,'utf8'));
const runs=plan.experiments.map(e=>JSON.parse(fs.readFileSync(path.join(root,'data/eval/latest-'+e.name+'.json'),'utf8')));
const initial=runs.find(r=>r.summary.label==='baseline');
if(runs.length!==5||runs.some(r=>r.summary.total!==25||r.summary.questionSha256!==initial.summary.questionSha256))throw Error('缺少同一固定25题的完整对照');
const ranked=[...runs].sort((a,b)=>b.summary.grounding.passed-a.summary.grounding.passed||b.summary.retrieval.passed-a.summary.retrieval.passed||a.summary.averageElapsedMs-b.summary.averageElapsedMs);
const chosen=ranked[0],selected=chosen.summary.parameters;
const relative=p=>path.relative(path.join(root,'eval'),p).replace(/\\/g,'/');
const score=m=>m.passed+'/'+m.total;
const configs={size800:['chunk.size',500,800],overlap0:['chunk.overlap',100,0],topk5:['qa.top-k',10,5],rerank:['qa.rerank-enabled',false,true]};
const lines=['# 单参数调优实录','','同一构建、同一25题、同一份原始课件。先统一修复Unicode代理对边界，再建立基线；没有把字符修复算成参数收益。每组只相对基线改变一个参数。','',
 '|参数|改前|改后|Top3命中|有依据回答|平均场景耗时ms|估算单次成本CNY|观察|','|---|---|---|---|---|---|---|---|'];
for(const run of runs.filter(r=>r.summary.label!=='baseline')){
 const s=run.summary,b=initial.summary,[name,before,after]=configs[s.label];
 const delta=(now,old,label)=>label+(now>old?'变好':now<old?'变差':'无变化');
 const observations=[delta(s.retrieval.passed,b.retrieval.passed,'检索'),delta(s.grounding.passed,b.grounding.passed,'依据'),s.averageElapsedMs>b.averageElapsedMs?'耗时变差':'耗时降低',s.estimatedCostPerCase>b.estimatedCostPerCase?'成本增加':'成本降低'];
 lines.push(`|${name}|${before}|${after}|${score(b.retrieval)} → ${score(s.retrieval)}|${score(b.grounding)} → ${score(s.grounding)}|${b.averageElapsedMs} → ${s.averageElapsedMs}|${b.estimatedCostPerCase.toFixed(8)} → ${s.estimatedCostPerCase.toFixed(8)}|${observations.join('；')}|`);
}
lines.push('','本轮选择：`'+JSON.stringify({size:selected.size,overlap:selected.overlap,topK:selected.topK,rerank:selected.rerank})+'`。优先依据通过数，其次Top3完整覆盖，再比较耗时。',
 '本轮最影响实际回答依据率的是overlap：100→0带来4/11→6/11，并使Top3由7/11到9/11；重排也达到6/11，但耗时更高、Top3为8/11。这里是本课件与固定题集的单次对照结论，不是所有RAG场景的普遍规律。',
 '块大小800虽然提高召回，但回答依据没有变化且成本、耗时变差，因此不保留。TopK5的Top3没有变化，成本较低，但依据结果低于所选overlap0。负结果和无变化都保留。','',
 '工具否定指令修复在参数实验完成后独立验证；五组里Q25失败均保留，不把功能修复收益冒充参数收益。完整报告：','');
for(const r of runs)lines.push('- ['+r.summary.label+']('+relative(path.join(r.directory,'report.md'))+')');
fs.writeFileSync(path.join(root,'eval/tuning-log.md'),lines.join('\n')+'\n');
const baseline=['# 全量基线v2（25题）','','固定题集：[questions-v2.json](questions-v2.json)，六类数量为4/3/3/6/3/6；工具类含3种失败及3种对照。课程隔离含两门均有资料的3例，以及保留的空课程3例。','',
 '## 首次基线','',fs.readFileSync(path.join(initial.directory,'report.md'),'utf8').replace(/^# .*\n/,'')];
// 复制报告时修正其中相对链接，保证README可追溯到同一组数字。
baseline.push('','首次基线明细：['+relative(path.join(initial.directory,'results.json'))+']('+relative(path.join(initial.directory,'results.json'))+')。',
 '','## 参数选择','','所选对照组 '+selected.name+'：Top3 '+score(chosen.summary.retrieval)+'，依据 '+score(chosen.summary.grounding)+'，工具 '+score(chosen.summary.tools)+'，平均 '+chosen.summary.averageElapsedMs+' ms。',
 '逐参数数据见[tuning-log.md](tuning-log.md)。最终构建修复后的25题复验单独附后，不能与参数对照混为一次实验。','');
fs.writeFileSync(path.join(root,'eval/baseline-v2.md'),baseline.join('\n').replace(/\[results.json\]\(results.json\)/g,'[results.json]('+relative(path.join(initial.directory,'results.json'))+')').replace(/\[summary.json\]\(summary.json\)/g,'[summary.json]('+relative(path.join(initial.directory,'summary.json'))+')'));
fs.writeFileSync(path.join(root,'data/eval/selected.json'),JSON.stringify({selected,runDirectory:chosen.directory,summary:chosen.summary},null,2));

async function apply(){
 if(plan.productionAppliedAt){console.log('Already applied '+plan.productionAppliedAt);return;}
 const running=await new Promise(resolve=>{const s=net.connect({host:'127.0.0.1',port:18081});s.on('connect',()=>{s.destroy();resolve(true);});s.on('error',()=>resolve(false));});if(running)throw Error('请先停止18081上的服务');
 const original=path.join(root,'data/vector-store.json');if(crypto.createHash('sha256').update(fs.readFileSync(original)).digest('hex')!==plan.originalIndexSha256)throw Error('正式索引已变化，拒绝覆盖');
 const backup=path.join(root,'data/backups/stage3-'+new Date().toISOString().replace(/[:.]/g,'-'));fs.mkdirSync(backup,{recursive:true});fs.copyFileSync(original,path.join(backup,'vector-store.json'));
 for(const table of ['course','document','chunk','question','answer_record','wrong_question']){
  const columns=query(`SELECT COLUMN_NAME FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='docuagent' AND TABLE_NAME='${table}' ORDER BY ORDINAL_POSITION;`).split(/\r?\n/);if(columns.some(c=>!/^\w+$/.test(c)))throw Error('列名异常');
  const fields=columns.map(c=>"'"+c+"',`"+c+"`").join(',');
  const data=query(`SELECT COALESCE(JSON_ARRAYAGG(row_json),JSON_ARRAY()) FROM (SELECT JSON_OBJECT(${fields}) AS row_json FROM ${table}) rows_;`);
  fs.writeFileSync(path.join(backup,table+'.json'),data);
 }
 const documents=query("SELECT id FROM document WHERE deleted=0 AND status='DONE';").split(/\r?\n/).filter(Boolean).map(Number);if(!documents.length)throw Error('没有有效原课件');
 const priorIds=query(`SELECT id FROM chunk WHERE deleted=0 AND document_id IN (${documents});`).split(/\r?\n/).filter(Boolean).map(Number);
 const sourceRows=query(`SELECT JSON_OBJECT('id',id,'documentId',document_id,'chunkIndex',chunk_index,'content',content) FROM chunk WHERE deleted=0 AND document_id IN (${documents}) ORDER BY document_id,chunk_index;`,selected.db).split(/\r?\n/).filter(Boolean).map(JSON.parse);
 const selectedVectors=JSON.parse(fs.readFileSync(selected.indexPath,'utf8'));let next=Number(query('SELECT COALESCE(MAX(id),0) FROM chunk;'));
 const vectors=Object.fromEntries(Object.entries(JSON.parse(fs.readFileSync(original,'utf8'))).filter(([,v])=>!documents.includes(Number(v.metadata.documentId))));
 const sqlText=s=>"CONVERT(X'"+Buffer.from(s).toString('hex')+"' USING utf8mb4)";const inserts=[],newIds=[];
 for(const row of sourceRows){const v=selectedVectors[row.id];if(!v||v.text!==row.content||v.embedding.length!==1024||!v.embedding.every(Number.isFinite))throw Error('候选索引与实验库不一致');const id=++next;newIds.push(id);
  inserts.push(`INSERT INTO chunk(id,document_id,chunk_index,content,char_count) VALUES(${id},${row.documentId},${row.chunkIndex},${sqlText(row.content)},${row.content.length});`);
  vectors[id]={...v,id:String(id),metadata:{...v.metadata,chunkId:id}};
 }
 const prepared=path.join(root,'data/vector-store.stage3-ready.json');fs.writeFileSync(prepared,JSON.stringify(vectors));
 query(`START TRANSACTION; UPDATE chunk SET deleted=1 WHERE deleted=0 AND document_id IN (${documents}); ${inserts.join('\n')} COMMIT;`);
 try{fs.renameSync(prepared,original);}catch(error){query(`START TRANSACTION; UPDATE chunk SET deleted=0 WHERE id IN (${priorIds}); UPDATE chunk SET deleted=1 WHERE id IN (${newIds}); COMMIT;`);fs.copyFileSync(path.join(backup,'vector-store.json'),original);throw error;}
 for(const file of ['application.yml','application.yml.example']){const p=path.join(root,'src/main/resources',file);let text=fs.readFileSync(p,'utf8');text=text.replace(/^    size:.*$/m,'    size: '+selected.size+' # Unicode码点').replace(/^    overlap:.*$/m,'    overlap: '+selected.overlap+' # 本轮25题实测选择');text=text.replace(/  qa:\r?\n    top-k:.*\r?\n    rerank-enabled:.*/,`  qa:\n    top-k: ${selected.topK}\n    rerank-enabled: ${selected.rerank}`);fs.writeFileSync(p,text);}
 plan.productionAppliedAt=new Date().toISOString();plan.productionBackup=backup;plan.productionChunks=sourceRows.length;plan.runtimeJarSha256=crypto.createHash('sha256').update(fs.readFileSync(path.join(root,'target/DocuAgent-0.0.1-SNAPSHOT.jar'))).digest('hex');fs.writeFileSync(planFile,JSON.stringify(plan,null,2));
 console.log(JSON.stringify({applied:selected.name,documents:documents.length,oldActiveChunks:priorIds.length,newActiveChunks:sourceRows.length,backup}));
}
console.log('Selected '+selected.name);if(process.argv.includes('--apply'))apply().catch(e=>{console.error(e.message);process.exitCode=1});
