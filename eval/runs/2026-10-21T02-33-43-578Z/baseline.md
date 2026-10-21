# 检索基线 v1

生成于 2026-10-21T02-33-43-578Z

评测集 SHA256：`b753e7b7c0aad4e4b8d4c0d8b91ba0c872acc15dda373d366edce482d08440f0`

共15题：7道可回答检索题、3道无答案、3道空课程隔离、2道追问。拒答和会话尚未实现，分别标记待验，不算通过。
top3 相关性根据固定原文锚点自动判断，仅作检索代理指标；不能证明答案质量。跨文档题要求所有来源证据都进入top5。
向量、RRF、模型相关性分的量纲不同，禁止比较三列原始分数的大小。评分成本未从供应商账单核实，不填造估算值。

|题目|类型|仅向量|融合后|重排后|
|---|---|---|---|---|
|Q01|direct|pass; top1=0.6999; top3=相关/不相关/不相关; 771ms|pass; top1=0.0328; top3=相关/不相关/不相关; 202ms|未运行|
|Q02|direct|fail; top1=0.6074; top3=相关/不相关/不相关; 231ms|fail; top1=0.0328; top3=相关/不相关/不相关; 195ms|未运行|
|Q03|direct|pass; top1=0.6836; top3=不相关/不相关/相关; 163ms|pass; top1=0.0325; top3=不相关/相关/不相关; 169ms|未运行|
|Q04|exact_term|pass; top1=0.6413; top3=不相关/相关/不相关; 167ms|pass; top1=0.0325; top3=相关/不相关/不相关; 217ms|未运行|
|Q05|unanswerable|pending_refusal; top1=0.4836; top3=不相关/不相关/不相关; 207ms|pending_refusal; top1=0.0164; top3=不相关/不相关/不相关; 202ms|未运行|
|Q06|unanswerable|pending_refusal; top1=0.4740; top3=不相关/不相关/不相关; 190ms|pending_refusal; top1=0.0323; top3=不相关/不相关/不相关; 186ms|未运行|
|Q07|unanswerable|pending_refusal; top1=0.5828; top3=不相关/不相关/不相关; 263ms|pending_refusal; top1=0.0328; top3=不相关/不相关/不相关; 183ms|未运行|
|Q08|cross_document|fail; top1=0.6610; top3=不相关/不相关/不相关; 202ms|fail; top1=0.0325; top3=不相关/不相关/相关; 177ms|未运行|
|Q09|cross_document|pass; top1=0.6885; top3=不相关/不相关/相关; 178ms|pass; top1=0.0323; top3=相关/相关/不相关; 249ms|未运行|
|Q10|cross_document|fail; top1=0.6372; top3=相关/不相关/不相关; 186ms|pass; top1=0.0323; top3=相关/不相关/不相关; 173ms|未运行|
|Q11|course_isolation|pass; top1=空; top3=; 17ms|pass; top1=空; top3=; 16ms|未运行|
|Q12|course_isolation|pass; top1=空; top3=; 15ms|pass; top1=空; top3=; 17ms|未运行|
|Q13|course_isolation|pass; top1=空; top3=; 16ms|pass; top1=空; top3=; 10ms|未运行|
|Q14|followup|pending_history; top1=0.4790; top3=不相关/不相关/相关; 262ms|pending_history; top1=0.0323; top3=相关/不相关/不相关; 188ms|未运行|
|Q15|followup|pending_history; top1=0.5701; top3=不相关/不相关/不相关; 173ms|pending_history; top1=0.0294; top3=不相关/不相关/相关; 174ms|未运行|

## 汇总

|阶段|可回答题证据命中|课程隔离|调用错误|待验拒答|待验多轮|总耗时ms|
|---|---|---|---|---|---|---|
|vector|4/7|3/3|0|3|2|3041|
|fusion|5/7|3/3|0|3|2|2358|

本次原始明细：`runs/2026-10-21T02-33-43-578Z/results.json`。失败与待验记录均保留。
