# 检索评测集 v1

2026-10-21，根据当前数据库中三份真实课件的提取原文编写，尚待课程学习者复核。
固定问题 Q01–Q10 为初始十题；Q11–Q15 为追加用例。更新标准答案须升级评测集版本，不能覆盖历史报告。
分块 ID 是本次语料快照证据；执行匹配使用课件名与原文锚点，重新入库后不依赖旧 ID。

三份材料：1-引论(ch1)-2026.pdf、2-词法分析(ch3).pdf、3-语法分析(ch4)-1.pdf。
目前只有编译原理有课件；课程 2 为独立空课程，用于验证不会返回课程 1 的资料，不能代替两个已填充课程的隔离测试。
本阶段仅有检索接口，无答案拒答与多轮记忆不是已实现能力，不计入普通检索命中率。

以下 JSON 块是评测脚本唯一读取的数据，不要修改字段名。

```json
[
  {"id":"Q01","type":"direct","courseId":1,"question":"词法分析器读入什么、输出什么，还会过滤哪些内容？","answer":"读入字符流、组成词素、输出词法单元序列，过滤空白、换行、制表符和注释。","checks":[{"document":"2-词法分析(ch3).pdf","anchors":["读入字符流，组成词素","过滤空白"]}],"evidenceChunkIds":[2509]},
  {"id":"Q02","type":"direct","courseId":1,"question":"上下文无关文法CFG包含哪四个部分？","answer":"终结符号、非终结符号、产生式和起始符号。","checks":[{"document":"3-语法分析(ch4)-1.pdf","anchors":["包含四个部分"]},{"document":"3-语法分析(ch4)-1.pdf","anchors":["起始符号：某个被指定的非终结符号"]}],"evidenceChunkIds":[2690,2691]},
  {"id":"Q03","type":"direct","courseId":1,"question":"FIRST和FOLLOW集合分别表示什么？","answer":"FIRST是可推导串的首符号集合，能推出空串时含空串；FOLLOW是句型中可能紧跟非终结符右边的终结符集合。","checks":[{"document":"3-语法分析(ch4)-1.pdf","anchors":["可以从 α 推导得到串的首符号的集合"]},{"document":"3-语法分析(ch4)-1.pdf","anchors":["紧跟在 A 右边的终结符号"]}],"evidenceChunkIds":[2713,2714]},
  {"id":"Q04","type":"exact_term","courseId":1,"question":"正则表达式里r+是什么意思，它与rr*是什么关系？","answer":"表示r的一个或多个实例，r+等价于rr*。","checks":[{"document":"2-词法分析(ch3).pdf","anchors":["r+ 等价于 rr*"]}],"evidenceChunkIds":[2518]},
  {"id":"Q05","type":"unanswerable","courseId":1,"question":"MySQL事务的四种隔离级别分别是什么？","answer":"应拒答：这三份编译原理资料未提供MySQL事务隔离级别。","checks":[],"evidenceChunkIds":[]},
  {"id":"Q06","type":"unanswerable","courseId":1,"question":"Python的GIL在多线程执行时如何工作？","answer":"应拒答：现有课件未提供Python GIL的工作方式。","checks":[],"evidenceChunkIds":[]},
  {"id":"Q07","type":"unanswerable","courseId":1,"question":"2026年编译原理期末考试的具体日期和教室是什么？","answer":"应拒答：现有提取文本没有具体考试日期和教室。","checks":[],"evidenceChunkIds":[]},
  {"id":"Q08","type":"cross_document","courseId":1,"question":"结合引论和词法分析课件，编译器前端收集的信息存在哪里，标识符词法单元的属性如何引用这些信息？","answer":"前端把源程序相关信息存入符号表；标识符属性可以包含名字、类型、位置，或者指向符号表条目的链接。","checks":[{"document":"1-引论(ch1)-2026.pdf","anchors":["同时收集和源程序相关的信息"]},{"document":"2-词法分析(ch3).pdf","anchors":["符号表条目链接"]}],"evidenceChunkIds":[2563,2511]},
  {"id":"Q09","type":"cross_document","courseId":1,"question":"结合词法分析和语法分析课件，字符流如何交给语法分析器，语法分析正确后生成什么？","answer":"词法分析器从字符流生成词法单元序列；语法分析器接收该序列，确认能否由文法生成，正确时生成语法分析树，通常是AST。","checks":[{"document":"2-词法分析(ch3).pdf","anchors":["读入字符流，组成词素"]},{"document":"3-语法分析(ch4)-1.pdf","anchors":["从词法分析器获得词法单元的序列"]}],"evidenceChunkIds":[2509,2689]},
  {"id":"Q10","type":"cross_document","courseId":1,"question":"结合引论和语法分析课件，语法分析的输出如何帮助语义分析，语义分析还需要什么信息？","answer":"语法分析构建语法树；语义分析利用语法树和符号表检查语义约束并收集类型信息。","checks":[{"document":"1-引论(ch1)-2026.pdf","anchors":["使用语法树和符号表中的信息"]},{"document":"3-语法分析(ch4)-1.pdf","anchors":["对于语法正确的程序，生成语法分析树"]}],"evidenceChunkIds":[2567,2689]},
  {"id":"Q11","type":"course_isolation","courseId":2,"question":"正则表达式r+等价于什么？","answer":"课程2未上传课件，应返回空检索结果，不得引用课程1词法分析资料。","checks":[],"evidenceChunkIds":[]},
  {"id":"Q12","type":"course_isolation","courseId":2,"question":"上下文无关文法包含哪四个部分？","answer":"课程2为空，不得引用课程1的语法分析资料。","checks":[],"evidenceChunkIds":[]},
  {"id":"Q13","type":"course_isolation","courseId":2,"question":"FIRST集合和FOLLOW集合有什么区别？","answer":"课程2为空，不得引用课程1的FIRST和FOLLOW资料。","checks":[],"evidenceChunkIds":[]},
  {"id":"Q14","type":"followup","courseId":1,"history":[{"role":"USER","content":"正则表达式r+是什么意思？"},{"role":"ASSISTANT","content":"表示r的一个或多个实例。"}],"question":"那它与r*有什么区别？","resolvedQuestion":"正则表达式r+与r*有什么区别？","answer":"r+为一个或多个实例，r*允许零次；本阶段接口尚不接收history，不能据此宣称已实现多轮理解。","checks":[{"document":"2-词法分析(ch3).pdf","anchors":["r+ 等价于 rr*"]}],"evidenceChunkIds":[2518]},
  {"id":"Q15","type":"followup","courseId":1,"history":[{"role":"USER","content":"语法分析器负责什么？"},{"role":"ASSISTANT","content":"分析词法单元序列并构建语法树。"}],"question":"它接收什么，正确时又生成什么？","resolvedQuestion":"语法分析器接收什么，正确时生成什么？","answer":"接收词法单元序列；正确时生成语法分析树，通常为AST。本阶段只测原始追问的检索表现。","checks":[{"document":"3-语法分析(ch4)-1.pdf","anchors":["从词法分析器获得词法单元的序列"]}],"evidenceChunkIds":[2689]}
]
```
