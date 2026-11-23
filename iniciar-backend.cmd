@echo off
REM Sobe a API Spring Boot na porta 8080 (Windows: PowerShell ou CMD).
REM Usa um JDK 21 SOMENTE para este processo, sem alterar o JAVA_HOME do sistema
REM (util para quem tem outro Java configurado para Eclipse, Fluig etc.).
REM Para usar um JDK especifico, defina JAVA21_HOME antes de rodar.
REM O arquivo .env da raiz e lido pelo proprio Spring (application.properties).
setlocal

if defined JAVA21_HOME (
  set "JAVA_HOME=%JAVA21_HOME%"
  goto :rodar
)

for /d %%D in (
  "%ProgramFiles%\Java\jdk-21*"
  "%ProgramFiles%\Eclipse Adoptium\jdk-21*"
  "%ProgramFiles%\Microsoft\jdk-21*"
  "%ProgramFiles%\Zulu\zulu-21*"
  "%ProgramFiles%\Amazon Corretto\jdk21*"
) do set "JAVA_HOME=%%~fD"

:rodar
if not exist "%JAVA_HOME%\bin\java.exe" (
  echo Nao encontrei um JDK 21. Instale o Java 21 ou defina JAVA21_HOME com a pasta do JDK.
  exit /b 1
)
echo Usando JDK: %JAVA_HOME%

cd /d "%~dp0backend"
call mvnw.cmd spring-boot:run %*
