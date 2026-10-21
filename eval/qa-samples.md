# 段2真实问答实录

2026-10-21，真实硅基流动调用；课件为当前编译原理三份资料。

## Q1

词法分析器读入什么、输出什么，还会过滤哪些内容？

```json
{
  "answer": "词法分析器读入字符流，输出词法单元序列。它还会过滤空白、换行、制表符、注释等。[1][2]",
  "references": [
    {
      "index": 1,
      "chunkId": 2509,
      "documentId": 13,
      "documentName": "2-词法分析(ch3).pdf",
      "snippet": "\n编译原理\n2 – 词法分析 (ch3)\n\n王焱林 wangylin36@mail.sysu.edu.cn\n\n词\n法\n分\n析\n\n词法分析器的作用\n词法单元的规约 (正则表达式)\n词法单元的识别 (状态转换图)\n词法分析器生成工具及设计\n有穷自动机\n\n2\n\nØ 读入字符流，组成词素，输出词法单元序列\n\nØ 过滤空白、换行、制表符、注释等\n\nØ 将词素添加到符号表中\n\n词法分析器的作用\n\n3\n\n源程序\n词法分\n析器\n\n词法单元\n\n语法分\n析器\n\n符号表\n\ngetNextToken\n\n输出至语\n义分析\n\n词法分析器与语法分析器之间的交互\n\n词法分析器\n\n语法分析器\n\n语义分析器\n\n中间代码生成器\n\n机器无关代码优化器\n\n目标代码生成器\n\n机器相关代码优化器\n\n字符流\n\n词法单元流\n\n语法树\n\n语法树\n\n中间表示形式\n\n中间表示形式\n\n目标机器语言\n\n目标机器语言\n\n为什么要设立独立的词法分析器.\n\n4\n\nØ简化编译器的设计\n\n• 词法分析器可以首先完成一些简单的处理工作\n\nØ模块化设计，增强编译器的可移植性\n\n词法单元、模式、词素\n\n5\n\nØ词法单元 (Token)\n• <词法单元名、属性值",
      "score": 0.03278688524590164
    },
    {
      "index": 2,
      "chunkId": 2564,
      "documentId": 14,
      "documentName": "1-引论(ch1)-2026.pdf",
      "snippet": "器\n\n语法分析器\n\n语义分析器\n\n中间代码生成器\n\n机器无关代码优化器\n\n目标代码生成器\n\n机器相关代码优化器\n\n字符流\n\n词法单元流\n\n语法树\n\n语法树\n\n中间表示形式\n\n中间表示形式\n\n目标机器语言\n\n目标机器语言\n\n1.词法分析\n\n24\n\n➢ 词法分析/扫描(Lexical analysis/Scanning)\n\n• 读入源程序的字符流，输出为有意义的词素 (Lexeme)的序列\n\n• 对每个词素，产生以下形式的词法单元(token)作为输出：\n\n<token-name, attribute-value>\n\n• token-name是供语法分析步骤使用的抽象符号\n\n• attribute-value指向相应的符号表条目或具体的值，供语义分析/代\n码生成步骤使用\n\n➢ 类比例子：我爱喝可乐 符合词法吗？\n\n➢ 例子\n\n• 输入：position = initial + rate * 60\n\n• 输出：<id, 1> <=, > <id, 2> <+, > <id, 3> <*, > <number, 60>\n\n词法分析器\n\n语法分析器\n\n语义分析器\n\n中间代码生成器\n\n机器无关代码",
      "score": 0.015873015873015872
    }
  ],
  "rewrittenQuestion": "词法分析器读入什么、输出什么，还会过滤哪些内容？",
  "sessionId": null
}
```

## Q2

正则表达式r+表示什么，与rr*是什么关系？

```json
{
  "answer": "正则表达式r+表示一个或多个r的实例，与rr*是等价的[1]。正则表达式r?表示零个或一个r的实例，等价于ε | r[1]。",
  "references": [
    {
      "index": 1,
      "chunkId": 2518,
      "documentId": 13,
      "documentName": "2-词法分析(ch3).pdf",
      "snippet": "的例子\n\n20\n\nØ C语言标识符的正则定义\n\n• letter_ à A | B | … | Z | a | b | … | z | _\n\n• digit à 0 | 1 | … | 9\n\n• id à letter_ ( letter_ | digit )*\n\nØ id对应的正则表达式为\n\n• (A | B | … | Z | a | b | … | z | _) ( (A | B | … | Z | a | b | … | z | _) | (0 | 1 | … | 9) )*\n\n正则表达式的扩展\n\n21\n\nØ 基本运算符：选择、连接、Kleene闭包\n\nØ 扩展运算符\n\n• 一个或多个实例：单目后缀 +\n\n• r+ 等价于 rr*\n\n• 零个或一个实例：？\n• r? 等价于ε | r\n\n• 字符类\n\n• [a1a2…an]等价于a1 | a2 | … | an\n\n• 符号，如：[a-e]等价于a | b | c | d | e\n\nØ 使正则表达式更简洁，但不会使其描述能力增强\n\n正则表达式与词法单元的识别\n\n22\n\nØ 词法分析器要求能够检查输入字符串，在其前缀中找出和某个模式",
      "score": 0.03252247488101534
    }
  ],
  "rewrittenQuestion": "正则表达式r+表示什么，与rr*是什么关系？",
  "sessionId": null
}
```

## Q3

上下文无关文法包含哪四个部分？

```json
{
  "answer": "上下文无关文法包含四个部分：终结符号、非终结符号、产生式和起始符号。[1][2]",
  "references": [
    {
      "index": 1,
      "chunkId": 2690,
      "documentId": 18,
      "documentName": "3-语法分析(ch4)-1.pdf",
      "snippet": "程语言的大\n部分语法\n\nØ “前端的其余部分”\n• 将词法单元的信息收集到符号表中、类型检查和其他类\n\n型的语义分析、生成中间代码等\n\n语法分析器.(2)\n\n4\n\n语\n法\n分\n析\n\n语法分析器\n上下文无关文法\n设计文法\n语法分析技术\n语法分析器生成工具\n\n5\n\nØ程序设计语言构造的语法可使用上下文无关文法 (Context-Free Grammar) \n或 BNF 表示法来描述\n• 文法可给出精确易懂的语法规则\n\n• 可以自动构造出某些类型的文法的语法分析器\n\n• 文法指出了语言的结构，有助于进一步的语义处理/代码生成\n\n• 支持语言的演化和迭代\n\n程序设计语言构造的描述\n\n6\n\nØ一个上下文无关文法 (CFG) 包含四个部分\n• 终结符号：组成串的基本符号(词法单元名字)\n• 非终结符号：表示串的集合的语法变量\n\n• 在程序设计语言中通常对应于某个程序构造，比如stmt (语句)\n• 给出了语言的层次结构，是语法分析和翻译的关键\n\n• 产生式：描述将终结符号和非终结符号组成串的方法\n• 形式：头(左) 部 à 体(右) 部\n• 头部是一个非终结符号，右部是一个符号串\n• 例子：exp",
      "score": 0.03252247488101534
    },
    {
      "index": 2,
      "chunkId": 2691,
      "documentId": 18,
      "documentName": "3-语法分析(ch4)-1.pdf",
      "snippet": "出了语言的层次结构，是语法分析和翻译的关键\n\n• 产生式：描述将终结符号和非终结符号组成串的方法\n• 形式：头(左) 部 à 体(右) 部\n• 头部是一个非终结符号，右部是一个符号串\n• 例子：expression à expression + term\n\n• 起始符号：某个被指定的非终结符号\n• 它对应的串的集合就是文法的语言\n\n上下文无关文法\n\n7\n\n类 ⽐ ： 正 则 定 义\n\n– letter_ àA | … | Z | a | … | z | _\n– digit à 0 | 1 | … | 9\n– id à letter_ ( letter_ | digit )*\n\nØ简单算术表达式 (expression) 的文法\n• 终结符号：id, +, –, *, /, (, )\n• 非终结符号：expression (表达式), term (项), factor (因子)\n• 起始符号：expression\n• 产生式集合\n\n• expression à expression + term  \n• expression à expression – term  \n• express",
      "score": 0.030303030303030304
    }
  ],
  "rewrittenQuestion": "上下文无关文法包含哪四个部分？",
  "sessionId": null
}
```

## Q4

MySQL事务的四种隔离级别是什么？

```json
{
  "answer": "资料中没有相关内容，无法依据当前课程资料回答。",
  "references": [],
  "rewrittenQuestion": "MySQL事务的四种隔离级别是什么？",
  "sessionId": null
}
```
