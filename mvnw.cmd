@REM ----------------------------------------------------------------------------
@REM NetShield Maven Wrapper Batch Script
@REM ----------------------------------------------------------------------------
@echo off
set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set MAVEN_CMD=%DIRNAME%apache-maven-3.9.9\bin\mvn.cmd

if exist "%MAVEN_CMD%" (
    "%MAVEN_CMD%" %*
) else (
    mvn %*
)
