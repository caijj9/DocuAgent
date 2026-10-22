const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const {root}=require('./local-db.cjs');
const option=(k,d)=>process.argv.find(a=>a.startsWith('--'+k+'='))?.slice(k.length+3)||d;
async function main(){
 const base=option('base-url','http://127.0.0.1:18081'),courseId=Number(option('course','3')),start=performance.now();
 const response=await fetch(base+'/qa/stream',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({courseId,question:'词法分析器读入什么、输出什么，还会过滤哪些内容？'}),signal:AbortSignal.timeout(180000)});
 assert.equal(response.status,200);assert.match(response.headers.get('content-type'),/text\/event-stream/);
 const reader=response.body.getReader(),decoder=new TextDecoder(),events=[];let buffer='',text='',done;
 const handle=packet=>{const event=packet.match(/^event:([^\n]+)/m)?.[1]?.trim();const value=JSON.parse(packet.split('\n').filter(l=>l.startsWith('data:')).map(l=>l.slice(5)).join('\n'));events.push({event,atMs:Math.round(performance.now()-start)});if(event==='delta')text+=value.text;if(event==='done')done=value;if(event==='error')throw Error(value.msg);};
 while(true){const part=await reader.read();buffer+=decoder.decode(part.value,{stream:!part.done}).replace(/\r\n/g,'\n');let i;while((i=buffer.indexOf('\n\n'))>=0){const packet=buffer.slice(0,i);buffer=buffer.slice(i+2);if(packet.trim())handle(packet);}if(part.done)break;}
 assert.ok(done);assert.equal(text,done.answer);const deltas=events.filter(e=>e.event==='delta');assert.equal(deltas.length,[...text].length);assert.ok(done.references.length>0);
 const span=deltas.at(-1).atMs-deltas[0].atMs;if(deltas.length>10)assert.ok(span>=100,'传输被缓冲成整段，不能当流式通过');
 const usage=await(await fetch(base+'/diagnostics/usage/'+response.headers.get('x-usage-id'))).json();
 const result={at:new Date().toISOString(),courseId,status:200,firstEvent:events[0],firstDeltaMs:deltas[0].atMs,lastDeltaMs:deltas.at(-1).atMs,deltaCount:deltas.length,emissionSpanMs:span,completed:true,answer:done.answer,references:done.references,usage:usage.data,
  streamingMode:'先完成资料依据校验，再由服务端逐Unicode字符SSE推送；不是供应商原生token直传。'};
 const output=path.join(root,'eval/sse-verification.json');fs.writeFileSync(output,JSON.stringify(result,null,2));console.log(JSON.stringify({deltaCount:result.deltaCount,emissionSpanMs:span,firstDeltaMs:result.firstDeltaMs,completed:true}));
}
main().catch(e=>{console.error(e.message);process.exitCode=1});
