# SSE接口与显示口径

`POST /qa/stream`，JSON入参与`/qa/ask`相同：question、courseId（可选）、sessionId（可选）。返回`text/event-stream`。

|事件|内容|
|---|---|
|status|资料检索、生成与复核中的状态|
|references|已核准的引用数组|
|delta|`{"text":"一个Unicode字符"}`|
|done|完整Qa对象，含answer、references、rewrittenQuestion、sessionId|
|error|业务错误码code和可读msg；不发送虚假的done|

生成与依据检查沿用段2的完整链路。**回答通过校验后**才逐字推送，这是服务端分段传输，不是模型原生token直传。首字等待较长，是保留复核的代价；前端不会在网络完成后自行播放假流式动画。

服务端线程池最多4个工作线程、队列8个；客户端停止或断线会尝试取消工作任务。已经发给供应商的调用不保证能撤销。会话仍受Redis处理锁约束。

真实HTTP验证见[eval/sse-verification.json](../eval/sse-verification.json)：46个delta事件，首字约3502 ms，首末字符相隔889 ms，完整拼接与done.answer一致。浏览器同时验证最终显示、可展开引用与无控制台错误。Java HTTP测试另覆盖辅助平面数学字符，以及业务失败只发error、不假装成功。

`X-Usage-Id`可用于查询`GET /diagnostics/usage/{id}`，返回该请求的模型调用次数与供应商token用量，不包含提示词、回答正文或密钥。内存中只保留最近512个请求；该诊断接口用于本地验收。
