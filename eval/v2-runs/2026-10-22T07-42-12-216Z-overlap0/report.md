# 全量评测 overlap0

25题，六类；原始输出与失败均保留。正例依据检查使用答案要点、原文锚点、符号对象和课程范围；仍需人工语义核对。

|指标|结果|
|---|---|
|检索命中率（Top3完整覆盖）|9/11 (81.8%)|
|资料内回答有依据率|6/11 (54.5%)|
|工具调用正确率|5/6 (83.3%)|
|平均请求耗时|4340 ms|
|平均场景耗时|5152 ms|
|单场景估算成本|￥0.00468656|
|应拒答通过|8/8 (100.0%)|
|课程隔离通过|6/6 (100.0%)|

成本口径：统一测算情景：三类均按1元/百万token计算；不是硅基流动实时报价或实际账单。实际免费额度可能使扣费为0。供应商未返回用量的调用单独标记，不伪装成零token。
业务用量包含多轮历史准备及回答/复核/工具模型调用；Top3辅助探针和离线重建用量另记，不混入单次问答成本。延迟包含场景内历史准备，辅助探针不计。

用量：`{"chatCalls":53,"embeddingCalls":22,"inputTokens":114420,"outputTokens":2369,"embeddingTokens":375,"missingUsageCalls":0}`

总通过19/25。

|题号|类型|结果|耗时ms|说明|
|---|---|---|---|---|
|Q01|answerable|PASS|7491|词法分析器读入什么、输出什么，还会过滤哪些内容？|
|Q02|answerable|FAIL|8157|上下文无关文法CFG包含哪四个部分？|
|Q03|answerable|FAIL|8592|FIRST和FOLLOW集合分别表示什么？|
|Q04|answerable|PASS|3857|正则表达式里r+是什么意思，它与rr*是什么关系？|
|Q05|unanswerable|PASS|1445|MySQL事务的四种隔离级别分别是什么？|
|Q06|unanswerable|PASS|1615|Python的GIL在多线程执行时如何工作？|
|Q07|unanswerable|PASS|1303|2026年编译原理期末考试的具体日期和教室是什么？|
|Q08|cross_document|FAIL|6356|结合引论和词法分析课件，编译器前端收集的信息存在哪里，标识符词法单元的属性如何引用这些信息？|
|Q09|cross_document|PASS|6371|结合词法分析和语法分析课件，字符流如何交给语法分析器，语法分析正确后生成什么？|
|Q10|cross_document|FAIL|10641|结合引论和语法分析课件，语法分析的输出如何帮助语义分析，语义分析还需要什么信息？|
|Q11|course_isolation|PASS|10|正则表达式r+等价于什么？|
|Q12|course_isolation|PASS|22|上下文无关文法包含哪四个部分？|
|Q13|course_isolation|PASS|21|FIRST集合和FOLLOW集合有什么区别？|
|Q14|followup|FAIL|12494|正则表达式r+与r*有什么区别？|
|Q15|followup|PASS|17091|语法分析器接收什么，正确时又生成什么？|
|Q16|followup|PASS|8778|词法分析器还会过滤哪些内容？|
|Q17|course_isolation|PASS|1338|数据库验收课程的固定校验词是什么？|
|Q18|course_isolation|PASS|3081|本数据库验收课程的固定校验词是什么？|
|Q19|course_isolation|PASS|879|上下文无关文法CFG包含哪四个部分？|
|Q20|tool_failure|PASS|4091|missing_question|
|Q21|tool_failure|PASS|2335|invalid_days|
|Q22|tool_failure|PASS|3256|wrong_course|
|Q23|tool_failure|PASS|8376|history_control|
|Q24|tool_failure|PASS|10591|search_control|
|Q25|tool_failure|FAIL|608|模型输出格式或依据校验失败，请查看本地日志|

明细：[results.json](results.json)，汇总：[summary.json](summary.json)。
