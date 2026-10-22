// 只读导出业务数据与向量差异；不把连接密码写进快照或命令参数。
const fs = require('node:fs');
const path = require('node:path');
const {root,query} = require('./local-db.cjs');
const sql = `SELECT JSON_OBJECT(
 'courses',(SELECT COALESCE(JSON_ARRAYAGG(JSON_OBJECT('id',id,'name',name,'deleted',deleted)),JSON_ARRAY()) FROM course),
 'documents',(SELECT COALESCE(JSON_ARRAYAGG(JSON_OBJECT('id',id,'courseId',course_id,'name',original_name,'path',storage_path,'status',status,'deleted',deleted)),JSON_ARRAY()) FROM document),
 'chunks',(SELECT COALESCE(JSON_ARRAYAGG(JSON_OBJECT('id',id,'documentId',document_id,'chunkIndex',chunk_index,'content',content,'deleted',deleted)),JSON_ARRAY()) FROM chunk)
);`;
const output = query(sql);
const snapshot = JSON.parse(output.trim());
const vectors = JSON.parse(fs.readFileSync(path.join(root, 'data/vector-store.json'), 'utf8'));
const courses = new Set(snapshot.courses.filter(c => !c.deleted).map(c => c.id));
const documents = new Map(snapshot.documents.filter(d => !d.deleted && d.status === 'DONE' && courses.has(d.courseId)).map(d => [d.id, d]));
const chunks = new Map(snapshot.chunks.filter(c => !c.deleted && documents.has(c.documentId)).map(c => [String(c.id), c]));
const valid = {}, orphanIds = [], invalidIds = [];
for (const [id, vector] of Object.entries(vectors)) {
  const chunk = chunks.get(id);
  if (!chunk) { orphanIds.push(id); continue; }
  const document = documents.get(chunk.documentId);
  if (Number(vector.metadata.chunkId) !== chunk.id || Number(vector.metadata.documentId) !== document.id ||
      Number(vector.metadata.courseId) !== document.courseId || vector.text !== chunk.content) {
    invalidIds.push(id); continue;
  }
  valid[id] = vector;
}
const summary = { documents: documents.size, activeChunks: chunks.size, vectors: Object.keys(vectors).length,
  validVectors: Object.keys(valid).length, orphanIds, invalidIds,
  missingIds: [...chunks.keys()].filter(id => !valid[id]) };
const evidence = path.join(root, 'data/evidence');
fs.mkdirSync(evidence, { recursive: true });
fs.writeFileSync(path.join(evidence, 'database-snapshot.json'), JSON.stringify(snapshot, null, 2));
fs.writeFileSync(path.join(evidence, 'index-audit.json'), JSON.stringify(summary, null, 2));
// 候选文件不覆盖原索引；修复时仍须停止服务、备份并检查 missingIds。
fs.writeFileSync(path.join(evidence, 'valid-vectors.json'), JSON.stringify(valid));
console.log(JSON.stringify(summary, null, 2));
