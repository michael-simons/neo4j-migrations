@Echo off

setlocal

@REM Set the current directory to the installation directory
call :getCurrentBatch INSTALLDIR1
set INSTALLDIR=%INSTALLDIR1%
set INSTALLDIR=%INSTALLDIR:~0,-10%

if exist "%INSTALLDIR%\jre\bin\java.exe" (
 set JAVA_CMD="%INSTALLDIR%\jre\bin\java.exe"
) else (
 @REM Use JAVA_HOME if it is set
 if "%JAVA_HOME%"=="" (
  set JAVA_CMD=java
 ) else (
  set JAVA_CMD="%JAVA_HOME%\bin\java.exe"
 )
)

if "%JAVA_ARGS%"=="" (
  set JAVA_ARGS=
)

%JAVA_CMD% %JAVA_ARGS% -cp "%CLASSPATH%;%INSTALLDIR%\lib\*" ac.simons.neo4j.migrations.cli.MigrationsCli %*

@REM Exit using the same code returned from Java
EXIT /B %ERRORLEVEL%

:getCurrentBatch
    set "%~1=%~f0"
    goto :eof