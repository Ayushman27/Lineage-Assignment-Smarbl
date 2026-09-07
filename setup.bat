@echo off
REM ============================================================================
REM setup.bat — Windows setup script for Expression Parser & Lineage Graph
REM
REM Usage: setup.bat
REM ============================================================================

echo ==================================================
echo  Expression Parser ^& Lineage Graph — Setup
echo ==================================================
echo.

REM Check for Java
java -version >nul 2>&1
if errorlevel 1 (
    echo ERROR: Java is not installed or not on PATH.
    echo        Please install Java 11 or later.
    exit /b 1
)
echo [OK] Java found:
java -version 2>&1 | findstr /i "version"

REM Try to find Maven
set MVN_CMD=
if exist "C:\apache-maven-3.9.14\bin\mvn.cmd" set MVN_CMD=C:\apache-maven-3.9.14\bin\mvn.cmd
if exist "C:\apache-maven-3.9.6\bin\mvn.cmd"  set MVN_CMD=C:\apache-maven-3.9.6\bin\mvn.cmd

where mvn >nul 2>&1
if not errorlevel 1 set MVN_CMD=mvn

if "%MVN_CMD%"=="" (
    echo ERROR: Maven not found. Please install Maven 3.6+ or set MVN_CMD.
    exit /b 1
)
echo [OK] Maven found: %MVN_CMD%
echo.

echo Running: %MVN_CMD% clean package -q
"%MVN_CMD%" clean package -q
if errorlevel 1 (
    echo.
    echo ERROR: Build failed. Run with 'mvn clean package' for details.
    exit /b 1
)

echo.
echo ==================================================
echo  Setup complete!
echo.
echo  Run the application:
echo    run.bat sample.json validate
echo    run.bat sample.json upstream 7
echo    run.bat sample.json downstream 1
echo    run.bat sample.json paths 7 1
echo.
echo  Run tests:
echo    %MVN_CMD% test
echo ==================================================
