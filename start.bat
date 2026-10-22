@echo off
setlocal EnableExtensions
chcp 65001 >nul
cd /d "%~dp0"
if defined DOCUAGENT_JAVA_HOME (
  set "JAVA_HOME=%DOCUAGENT_JAVA_HOME%"
) else if exist "E:\jdk17\bin\java.exe" (
  set "JAVA_HOME=E:\jdk17"
)
if not exist "%JAVA_HOME%\bin\java.exe" (
  echo [ERROR] 请将 JAVA_HOME 或 DOCUAGENT_JAVA_HOME 设置为 JDK 17 根目录。
  exit /b 1
)
"%JAVA_HOME%\bin\java.exe" -version 2>&1 | findstr /R /C:"version.*17[.]" >nul
if errorlevel 1 (
  echo [ERROR] 本项目要求 JDK 17，请检查 JAVA_HOME。
  exit /b 1
)
if not defined AI_API_KEY if not exist ".local\application-secrets.properties" (
  echo [ERROR] 请先按 README 配置 AI_API_KEY 和数据库密码。
  exit /b 1
)
if not exist "target\DocuAgent-0.0.1-SNAPSHOT.jar" (
  echo [BUILD] 首次运行，正在构建应用...
  where mvn.cmd >nul 2>nul
  if errorlevel 1 (
    call mvnw.cmd -DskipTests package
  ) else (
    call mvn.cmd -DskipTests package
  )
  if errorlevel 1 exit /b 1
)
if not defined APP_PORT set "APP_PORT=18081"
echo [START] http://localhost:%APP_PORT%/index.html
echo [INFO] 关闭服务请按 Ctrl+C。数据库和Redis需预先启动。
"%JAVA_HOME%\bin\java.exe" -jar target\DocuAgent-0.0.1-SNAPSHOT.jar
exit /b %errorlevel%
