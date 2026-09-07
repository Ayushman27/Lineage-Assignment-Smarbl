@echo off
REM ============================================================================
REM run.bat — Windows run script for Expression Parser & Lineage Graph
REM
REM Usage:
REM   run.bat <input.json> validate
REM   run.bat <input.json> upstream   <nodeId>
REM   run.bat <input.json> downstream <nodeId>
REM   run.bat <input.json> paths      <fromId> <toId>
REM
REM Examples:
REM   run.bat sample.json validate
REM   run.bat sample.json upstream 7
REM   run.bat sample.json downstream 1
REM   run.bat sample.json paths 7 1
REM ============================================================================

set JAR=target\expression-parser-lineage-1.0.0.jar

if "%~1"=="" goto usage
if "%~2"=="" goto usage

if not exist "%JAR%" (
    echo JAR not found. Running setup first...
    call setup.bat
    if errorlevel 1 exit /b 1
)

java -jar "%JAR%" %*
goto end

:usage
echo.
echo Usage:
echo   run.bat ^<input.json^> validate
echo   run.bat ^<input.json^> upstream   ^<nodeId^>
echo   run.bat ^<input.json^> downstream ^<nodeId^>
echo   run.bat ^<input.json^> paths      ^<fromId^> ^<toId^>
echo.
echo Examples:
echo   run.bat sample.json validate
echo   run.bat sample.json upstream 7
echo   run.bat sample.json downstream 1
echo   run.bat sample.json paths 7 1
echo.
exit /b 1

:end
