// Node 18+；只调用本地检索接口，课件片段由后端按配置发送给模型供应商。
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const root = path.resolve(__dirname, '..');
const arguments_ = process.argv.slice(2);
const option = (name, fallback) => arguments_.find(v => v.startsWith('--' + name + '='))?.slice(name.length + 3) ?? fallback;
const sourcePath = path.join(root, 'eval/questions-v1.md');
const source = fs.readFileSync(sourcePath, 'utf8');
const block = source.match(/```json\s*([\s\S]*?)```/);
if (!block) throw new Error('评测题文件缺少JSON数据块');
const allQuestions = JSON.parse(block[1]);
const ids = option('ids', '').split(',').filter(Boolean);
const questions = ids.length ? allQuestions.filter(q => ids.includes(q.id)) : allQuestions;
const phases = [
  { name: 'vector', parameters: { mode: 'vector', fusion: false, rerank: false } },
  { name: 'fusion', parameters: { mode: 'both', fusion: true, rerank: false } },
  { name: 'rerank', parameters: { mode: 'both', fusion: true, rerank: true } },
];
const selected = option('phases', 'vector,fusion,rerank').split(',');
const normalize = value => value.replace(/\s+/g, '').toLowerCase();
const timestamp = new Date().toISOString().replace(/[:.]/g, '-');
const directory = path.join(root, 'eval/runs', timestamp);
fs.mkdirSync(directory, { recursive: true });
const results = [];
async function run(question, phase) {
  const parameters = new URLSearchParams({ q: question.question, courseId: question.courseId, topK: 5, ...phase.parameters });
  const start = performance.now();
  try {
    const response = await fetch(option('base-url', 'http://127.0.0.1:18081') + '/retrieval/search?' + parameters,
      { signal: AbortSignal.timeout(120000) });
    const body = await response.json();
    if (!response.ok || body.code !== 0 || !Array.isArray(body.data)) throw new Error('HTTP ' + response.status + ' business=' + body.code + ' ' + body.msg);
    const hits = body.data;
    const relevance = hit => question.checks.some(check => check.document === hit.documentName &&
      check.anchors.some(anchor => normalize(hit.content).includes(normalize(anchor))));
    const checkMatched = check => hits.some(hit => check.document === hit.documentName &&
      check.anchors.some(anchor => normalize(hit.content).includes(normalize(anchor))));
    const verdict = question.type === 'unanswerable' ? 'pending_refusal'
      : question.type === 'course_isolation' ? (hits.length === 0 ? 'pass' : 'fail')
      : question.type === 'followup' ? 'pending_history'
      : question.checks.every(checkMatched) ? 'pass' : 'fail';
    return { id: question.id, type: question.type, phase: phase.name, verdict,
      elapsedMs: Math.round(performance.now() - start), top1Score: hits[0]?.score ?? null,
      scoreType: hits[0]?.scoreType ?? null, top3Relevant: hits.slice(0, 3).map(relevance),
      matchedChecks: question.checks.filter(checkMatched).length, totalChecks: question.checks.length,
      rawFollowupHit: question.type === 'followup' ? question.checks.every(checkMatched) : null,
      hits: hits.map(hit => ({ chunkId: hit.chunkId, documentId: hit.documentId, documentName: hit.documentName,
        score: hit.score, scoreType: hit.scoreType, relevant: relevance(hit), content: hit.content })) };
  } catch (error) {
    return { id: question.id, type: question.type, phase: phase.name, verdict: 'error', elapsedMs: Math.round(performance.now() - start), error: error.message };
  }
}
function summary(phase) {
  const rows = results.filter(r => r.phase === phase);
  const retrieval = rows.filter(r => ['direct','exact_term','cross_document'].includes(r.type));
  const isolation = rows.filter(r => r.type === 'course_isolation');
  return { phase, retrievalHit: retrieval.filter(r => r.verdict === 'pass').length, retrievalTotal: retrieval.length,
    isolationPassed: isolation.filter(r => r.verdict === 'pass').length, isolationTotal: isolation.length,
    errors: rows.filter(r => r.verdict === 'error').length,
    pendingRefusal: rows.filter(r => r.verdict === 'pending_refusal').length,
    pendingHistory: rows.filter(r => r.verdict === 'pending_history').length,
    elapsedMs: rows.reduce((total, r) => total + r.elapsedMs, 0) };
}
function render() {
  const lines = ['# 检索基线 v1', '', '生成于 ' + timestamp, '',
    '评测集 SHA256：`' + crypto.createHash('sha256').update(source).digest('hex') + '`', '',
    '共15题：7道可回答检索题、3道无答案、3道空课程隔离、2道追问。拒答和会话尚未实现，分别标记待验，不算通过。',
    'top3 相关性根据固定原文锚点自动判断，仅作检索代理指标；不能证明答案质量。跨文档题要求所有来源证据都进入top5。',
    '向量、RRF、模型相关性分的量纲不同，禁止比较三列原始分数的大小。评分成本未从供应商账单核实，不填造估算值。', '',
    '|题目|类型|仅向量|融合后|重排后|', '|---|---|---|---|---|'];
  for (const q of questions) {
    const cells = phases.map(p => {
      const r = results.find(row => row.id === q.id && row.phase === p.name);
      if (!r) return '未运行';
      if (r.error) return 'ERROR ' + r.error.replace(/\|/g,'/').slice(0,100);
      return r.verdict + '; top1=' + (r.top1Score?.toFixed(4) ?? '空') + '; top3=' + r.top3Relevant.map(v=>v?'相关':'不相关').join('/') + '; ' + r.elapsedMs + 'ms';
    });
    lines.push('|' + [q.id, q.type, ...cells].join('|') + '|');
  }
  lines.push('', '## 汇总', '', '|阶段|可回答题证据命中|课程隔离|调用错误|待验拒答|待验多轮|总耗时ms|', '|---|---|---|---|---|---|---|');
  for (const p of phases.filter(p=>selected.includes(p.name))) {
    const s=summary(p.name);
    lines.push(`|${p.name}|${s.retrievalHit}/${s.retrievalTotal}|${s.isolationPassed}/${s.isolationTotal}|${s.errors}|${s.pendingRefusal}|${s.pendingHistory}|${s.elapsedMs}|`);
  }
  lines.push('', '本次原始明细：`runs/' + timestamp + '/results.json`。失败与待验记录均保留。', '');
  return lines.join('\n');
}
async function main() {
  for (const question of questions) {
    for (const phase of phases.filter(p => selected.includes(p.name))) {
      const result = await run(question, phase);
      results.push(result);
      fs.writeFileSync(path.join(directory, 'results.json'), JSON.stringify(results, null, 2));
      console.log(result.id + ' ' + result.phase + ' ' + result.verdict + ' ' + result.elapsedMs + 'ms');
    }
  }
  const markdown=render();
  fs.writeFileSync(path.join(directory,'baseline.md'),markdown);
  if (!ids.length && selected.length === 3) fs.writeFileSync(path.join(root,'eval/baseline-v1.md'),markdown);
  fs.writeFileSync(path.join(directory,'summary.json'),JSON.stringify(selected.map(summary),null,2));
  const csv=['id,type,phase,verdict,top1Score,elapsedMs',...results.map(r=>[r.id,r.type,r.phase,r.verdict,r.top1Score??'',r.elapsedMs].join(','))].join('\n');
  fs.writeFileSync(path.join(directory,'results.csv'),csv);
  console.log('Report: ' + path.relative(root,directory));
  if (results.some(r => r.verdict === 'error')) process.exitCode=1;
}
main().catch(error=>{console.error(error.message);process.exitCode=1});
