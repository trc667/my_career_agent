@echo off
chcp 65001 >nul
set JAVA_TOOL_OPTIONS=-Dfile.encoding=UTF-8
call mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=dev,console"
pause
