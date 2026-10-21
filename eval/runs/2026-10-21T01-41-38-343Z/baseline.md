# 检索基线 v1

生成于 2026-10-21T01-41-38-343Z

评测集 SHA256：`b753e7b7c0aad4e4b8d4c0d8b91ba0c872acc15dda373d366edce482d08440f0`

共15题：7道可回答检索题、3道无答案、3道空课程隔离、2道追问。拒答和会话尚未实现，分别标记待验，不算通过。
top3 相关性根据固定原文锚点自动判断，仅作检索代理指标；不能证明答案质量。跨文档题要求所有来源证据都进入top5。
向量、RRF、模型相关性分的量纲不同，禁止比较三列原始分数的大小。评分成本未从供应商账单核实，不填造估算值。

|题目|类型|仅向量|融合后|重排后|
|---|---|---|---|---|
|Q01|direct|pass; top1=0.6999; top3=相关/不相关/不相关; 190ms|pass; top1=0.0328; top3=相关/不相关/不相关; 148ms|pass; top1=10.0000; top3=相关/不相关/不相关; 36002ms|
|Q02|direct|fail; top1=0.6074; top3=相关/不相关/不相关; 150ms|fail; top1=0.0328; top3=相关/不相关/不相关; 150ms|pass; top1=10.0000; top3=相关/相关/不相关; 11559ms|
|Q03|direct|pass; top1=0.6836; top3=不相关/不相关/相关; 187ms|pass; top1=0.0325; top3=不相关/相关/不相关; 155ms|pass; top1=10.0000; top3=不相关/相关/相关; 12531ms|
|Q04|exact_term|pass; top1=0.6413; top3=不相关/相关/不相关; 149ms|pass; top1=0.0325; top3=相关/不相关/不相关; 171ms|pass; top1=10.0000; top3=相关/不相关/不相关; 18417ms|
|Q05|unanswerable|pending_refusal; top1=0.4836; top3=不相关/不相关/不相关; 196ms|pending_refusal; top1=0.0164; top3=不相关/不相关/不相关; 138ms|pending_refusal; top1=0.0000; top3=不相关/不相关/不相关; 10097ms|
|Q06|unanswerable|pending_refusal; top1=0.4740; top3=不相关/不相关/不相关; 145ms|pending_refusal; top1=0.0323; top3=不相关/不相关/不相关; 135ms|pending_refusal; top1=3.0000; top3=不相关/不相关/不相关; 2841ms|
|Q07|unanswerable|pending_refusal; top1=0.5828; top3=不相关/不相关/不相关; 147ms|pending_refusal; top1=0.0328; top3=不相关/不相关/不相关; 159ms|pending_refusal; top1=0.0000; top3=不相关/不相关/不相关; 2707ms|
|Q08|cross_document|fail; top1=0.6610; top3=不相关/不相关/不相关; 150ms|fail; top1=0.0325; top3=不相关/不相关/相关; 146ms|fail; top1=9.0000; top3=不相关/不相关/不相关; 2408ms|
|Q09|cross_document|pass; top1=0.6885; top3=不相关/不相关/相关; 139ms|pass; top1=0.0323; top3=相关/相关/不相关; 137ms|fail; top1=10.0000; top3=相关/不相关/不相关; 2596ms|
|Q10|cross_document|fail; top1=0.6372; top3=相关/不相关/不相关; 146ms|pass; top1=0.0323; top3=相关/不相关/不相关; 153ms|pass; top1=10.0000; top3=相关/不相关/不相关; 2720ms|
|Q11|course_isolation|pass; top1=空; top3=; 8ms|pass; top1=空; top3=; 8ms|pass; top1=空; top3=; 8ms|
|Q12|course_isolation|pass; top1=空; top3=; 10ms|pass; top1=空; top3=; 9ms|pass; top1=空; top3=; 6ms|
|Q13|course_isolation|pass; top1=空; top3=; 5ms|pass; top1=空; top3=; 4ms|pass; top1=空; top3=; 4ms|
|Q14|followup|pending_history; top1=0.4790; top3=不相关/不相关/相关; 140ms|pending_history; top1=0.0323; top3=相关/不相关/不相关; 141ms|pending_history; top1=8.0000; top3=不相关/不相关/相关; 2463ms|
|Q15|followup|pending_history; top1=0.5705; top3=不相关/不相关/不相关; 146ms|pending_history; top1=0.0294; top3=不相关/不相关/相关; 140ms|pending_history; top1=8.0000; top3=不相关/不相关/不相关; 13185ms|

## 汇总

|阶段|可回答题证据命中|课程隔离|调用错误|待验拒答|待验多轮|总耗时ms|
|---|---|---|---|---|---|---|
|vector|4/7|3/3|0|3|2|1908|
|fusion|5/7|3/3|0|3|2|1794|
|rerank|5/7|3/3|0|3|2|117544|

本次原始明细：`runs/2026-10-21T01-41-38-343Z/results.json`。失败与待验记录均保留。
