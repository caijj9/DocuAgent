const fs=require('node:fs'),path=require('node:path'),{root,query}=require('./local-db.cjs');
const directory=path.join(root,'data/backups/stage2-'+new Date().toISOString().replace(/[:.]/g,'-'));
fs.mkdirSync(directory,{recursive:true});
for(const table of ['question','answer_record','wrong_question']){
  const snapshot=query(`SELECT COALESCE(JSON_ARRAYAGG(t.row_json),JSON_ARRAY()) FROM (SELECT JSON_OBJECT(${({question:"'id',id,'courseId',course_id,'stem',stem,'answer',answer,'keyPoints',key_points,'deleted',deleted",answer_record:"'id',id,'questionId',question_id,'requestId',request_id,'userAnswer',user_answer,'score',score,'deleted',deleted",wrong_question:"'id',id,'questionId',question_id,'wrongCount',wrong_count,'source',source,'deleted',deleted"})[table]}) AS row_json FROM ${table}) t;`);
  fs.writeFileSync(path.join(directory,table+'.json'),snapshot);
  console.log(table+' rows='+JSON.parse(snapshot).length);
}
for(const [column,comment] of [['hit_points','命中评分要点（JSON数组）'],['missed_points','遗漏评分要点（JSON数组）']]){
  if(!query(`SHOW COLUMNS FROM answer_record LIKE '${column}';`)){
    query(`ALTER TABLE answer_record ADD COLUMN ${column} TEXT NULL COMMENT '${comment}';`);
    console.log('Added answer_record.'+column);
  }
}
console.log('Backup: '+directory);
