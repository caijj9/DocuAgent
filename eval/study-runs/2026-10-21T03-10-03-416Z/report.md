# 第二段固定15题QA验收

题目SHA256：`b753e7b7c0aad4e4b8d4c0d8b91ba0c872acc15dda373d366edce482d08440f0`

正例自动指标为所需原文锚点是否全部出现在返回引用中，不能代替人工语义评审。无答案/课程隔离要求明确拒答且无引用；多轮题先运行真实上一轮问答。

|题号|类型|结果|耗时ms|改写问题|
|---|---|---|---|---|
|Q01|direct|PASS|10270|词法分析器读入什么、输出什么，还会过滤哪些内容？|
|Q02|direct|PASS|4133|上下文无关文法CFG包含哪四个部分？|
|Q03|direct|FAIL|5304|FIRST和FOLLOW集合分别表示什么？|
|Q04|exact_term|PASS|4449|正则表达式里r+是什么意思，它与rr*是什么关系？|
|Q05|unanswerable|PASS|1647|MySQL事务的四种隔离级别分别是什么？|
|Q06|unanswerable|PASS|1311|Python的GIL在多线程执行时如何工作？|
|Q07|unanswerable|PASS|1278|2026年编译原理期末考试的具体日期和教室是什么？|
|Q08|cross_document|FAIL|6384|结合引论和词法分析课件，编译器前端收集的信息存在哪里，标识符词法单元的属性如何引用这些信息？|
|Q09|cross_document|FAIL|5378|结合词法分析和语法分析课件，字符流如何交给语法分析器，语法分析正确后生成什么？|
|Q10|cross_document|FAIL|6000|结合引论和语法分析课件，语法分析的输出如何帮助语义分析，语义分析还需要什么信息？|
|Q11|course_isolation|PASS|15|正则表达式r+等价于什么？|
|Q12|course_isolation|PASS|16|上下文无关文法包含哪四个部分？|
|Q13|course_isolation|PASS|14|FIRST集合和FOLLOW集合有什么区别？|
|Q14|followup|PASS|10245|正则表达式中r+和r*有什么区别？|
|Q15|followup|PASS|10958|语法分析器接收什么，正确时又生成什么？|

通过：11/15。失败全部保留。
本次明细：`study-runs/2026-10-21T03-10-03-416Z/results.json`。
