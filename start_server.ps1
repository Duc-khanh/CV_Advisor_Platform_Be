$env:JAVA_HOME = 'C:\Users\Admin\.jdks\ms-17.0.20.1'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Set-Location 'D:\IdeaProjects\CvAdvisorPlatform'
./gradlew.bat bootRun > 'D:\IdeaProjects\CvAdvisorPlatform\server_start.log' 2>&1
