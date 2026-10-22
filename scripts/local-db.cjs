// 仅供本机维护/验收脚本复用，不输出或保存连接凭证。
const fs=require('node:fs'),path=require('node:path'),{execFileSync}=require('node:child_process');
const root=path.resolve(__dirname,'..');
function value(key){
  const local=path.join(root,'.local/application-secrets.properties');
  if(key==='password'&&process.env.DB_PASSWORD===undefined&&fs.existsSync(local)){
    const secret=fs.readFileSync(local,'utf8').match(/^spring\.datasource\.password=(.*)$/m);
    if(secret){const value=secret[1].replace(/\r$/,'');return value.match(/^\$\{DB_PASSWORD:(.*)\}$/)?.[1]??value;}
  }
  const config=fs.readFileSync(path.join(root,'src/main/resources/application.yml'),'utf8');
  const match=config.match(new RegExp('^    '+key+':\\s*([^\\r\\n]+)','m'));
  if(!match)throw new Error('缺少本地数据库配置');
  let text=match[1].trim();
  text=text.startsWith('"')||text.startsWith("'")?text.slice(1,text.indexOf(text[0],1)):text.split(/\s+#/)[0];
  const env=text.match(/^\$\{([^:}]+)(?::([^}]*))?\}$/);
  return env?(process.env[env[1]]??env[2]):text;
}
function query(sql, database=process.env.DB_NAME||'docuagent'){
  if(!/^[a-zA-Z0-9_]+$/.test(database))throw new Error('数据库名无效');
  return execFileSync(process.env.MYSQL_EXE||'D:/MYSQL/mysql-8.0.34-winx64/bin/mysql.exe',
    ['--host=127.0.0.1','--port=3306','--user='+value('username'),'--default-character-set=utf8mb4','--batch','--raw','--skip-column-names',database],
    {input:sql,env:{...process.env,MYSQL_PWD:value('password')},encoding:'utf8',maxBuffer:64*1024*1024}).trim();
}
module.exports={root,query,value};
