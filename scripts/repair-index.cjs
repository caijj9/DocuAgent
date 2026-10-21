// 仅修复可派生的向量索引；数据库、课件原文件与旧索引均保留。
const fs = require('node:fs');
const path = require('node:path');
const net = require('node:net');
const root = path.resolve(__dirname, '..');
async function main() {
  const running = await new Promise(resolve => {
    const socket = net.connect({ host: '127.0.0.1', port: 18081 });
    socket.on('connect', () => { socket.destroy(); resolve(true); });
    socket.on('error', () => resolve(false));
  });
  if (running) throw new Error('请先停止 18081 上的 DocuAgent，避免覆盖正在使用的索引');
  const snapshot = JSON.parse(fs.readFileSync(path.join(root, 'data/evidence/database-snapshot.json'), 'utf8'));
  const originalPath = path.join(root, 'data/vector-store.json');
  const original = JSON.parse(fs.readFileSync(originalPath, 'utf8'));
  const courses = new Set(snapshot.courses.filter(c => !c.deleted).map(c => c.id));
  const documents = new Map(snapshot.documents.filter(d => !d.deleted && d.status === 'DONE' && courses.has(d.courseId)).map(d => [d.id, d]));
  const chunks = snapshot.chunks.filter(c => !c.deleted && documents.has(c.documentId));
  const byText = new Map(Object.values(original).filter(v => v.embedding.length === 1024).map(v => [v.text, v.embedding]));
  const result = {}, missing = [];
  for (const chunk of chunks) {
    const doc = documents.get(chunk.documentId), id = String(chunk.id), embedding = byText.get(chunk.content);
    const vector = { id, text: chunk.content, metadata: { courseId: doc.courseId, documentId: doc.id, chunkId: chunk.id } };
    if (embedding) result[id] = { ...vector, embedding }; else missing.push(vector);
  }
  console.log(JSON.stringify({ documents: documents.size, chunks: chunks.length, reusable: Object.keys(result).length, embeddingRequired: missing.length }));
  if (!process.argv.includes('--apply')) return;
  const config = fs.readFileSync(path.join(root, 'src/main/resources/application.yml'), 'utf8');
  const model = config.match(/^\s+model:\s*(BAAI\/[^\s#]+)/m)?.[1] || 'BAAI/bge-m3';
  if (model !== 'BAAI/bge-m3') throw new Error('旧索引没有模型清单，仅允许以已验证的 BAAI/bge-m3 修复');
  if (missing.length && !process.env.AI_API_KEY) throw new Error('需要 AI_API_KEY 生成缺失向量');
  const backup = path.join(root, 'data/backups', new Date().toISOString().replace(/[:.]/g, '-'));
  fs.mkdirSync(backup, { recursive: true });
  fs.copyFileSync(originalPath, path.join(backup, 'vector-store.json'));
  fs.copyFileSync(path.join(root, 'data/evidence/database-snapshot.json'), path.join(backup, 'database-snapshot.json'));
  for (let i = 0; i < missing.length; i += 10) {
    const batch = missing.slice(i, i + 10);
    const response = await fetch('https://api.siliconflow.cn/v1/embeddings', {
      method: 'POST', headers: { Authorization: 'Bearer ' + process.env.AI_API_KEY, 'Content-Type': 'application/json' },
      body: JSON.stringify({ model, input: batch.map(v => v.text) }), signal: AbortSignal.timeout(90000),
    });
    if (!response.ok) throw new Error('Embedding HTTP ' + response.status + '；原索引未替换');
    const data = (await response.json()).data;
    if (!Array.isArray(data) || data.length !== batch.length) throw new Error('Embedding 数量不匹配');
    const seen = new Set();
    for (const item of data) {
      if (!Number.isInteger(item.index) || item.index < 0 || item.index >= batch.length || seen.has(item.index)
          || !Array.isArray(item.embedding) || item.embedding.length !== 1024 || !item.embedding.every(Number.isFinite))
        throw new Error('Embedding 结果无效');
      seen.add(item.index);
      const vector = batch[item.index];
      result[vector.id] = { ...vector, embedding: item.embedding };
    }
    console.log('Generated ' + Math.min(i + 10, missing.length) + '/' + missing.length);
  }
  if (Object.keys(result).length !== chunks.length) throw new Error('向量与分块数量不一致，未替换');
  const temporary = originalPath + '.repair.tmp';
  fs.writeFileSync(temporary, JSON.stringify(result));
  fs.renameSync(temporary, originalPath);
  console.log('Repaired vectors=' + chunks.length + '; backup=' + backup);
}
main().catch(error => { console.error(error.message); process.exitCode = 1; });
