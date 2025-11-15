@echo off
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%

set DEFAULT_JVM_OPTS=-Xmx64m -Xms64m

if exist "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" (
    set CLASSPATH=%APP_HOME%\gradle\wrapper\gradle-wrapper.jar
) else (
    echo ERROR: gradle-wrapper.jar not found
    exit /b 1
)

"%JAVA_HOME%\bin\java.exe" %DEFAULT_JVM_OPTS% -classpath %CLASSPATH% org.gradle.wrapper.GradleWrapperMain %*
