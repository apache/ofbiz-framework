@echo off
if "%~1" == "17" (
    echo 17
   set /p JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.5.8-hotspot"

   pathed /remove "C:\Program Files\Eclipse Adoptium\jdk-11.0.17.8-hotspot\bin" /user
   pathed /remove "n:\bin" /user

   pathed /add       "C:\Program Files\Eclipse Adoptium\jdk-17.0.5.8-hotspot\bin" /user

) else if "%~1" == "11" (
    echo 11
   set /p JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-11.0.17.8-hotspot"

   pathed /remove  "C:\Program Files\Eclipse Adoptium\jdk-17.0.5.8-hotspot\bin" /user
   pathed /remove "n:\bin" /user

   pathed /add        "C:\Program Files\Eclipse Adoptium\jdk-11.0.17.8-hotspot\bin" /user

) else if "%~1" == "n" (
    echo N
   set /p JAVA_HOME="n:\"

   pathed /remove  "C:\Program Files\Eclipse Adoptium\jdk-17.0.5.8-hotspot\bin" /user
   pathed /remove "C:\Program Files\Eclipse Adoptium\jdk-11.0.17.8-hotspot\bin" /user

   pathed /add        "n:\bin"  /user

) else (
    echo Syntax error
)
echo -------------
echo %java_home%
pathed /slim /user
