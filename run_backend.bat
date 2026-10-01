@echo off
set "JAVA_HOME=C:\Users\Admin\.jdks\ms-17.0.20.1"
set "PATH=%JAVA_HOME%\bin;%PATH%"
cd /d "D:\IdeaProjects\CvAdvisorPlatform"
echo Starting Spring Boot with JAVA_HOME=%JAVA_HOME% > server_start.log
gradlew.bat bootRun >> server_start.log 2>&1