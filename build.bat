@echo off
REM 构建模块（使用 Gradle Wrapper，需先执行过 gradle wrapper）
cd /d %~dp0
call gradlew.bat assembleDebug --no-daemon
echo.
echo APK: %~dp0app\build\outputs\apk\debug\app-debug.apk
pause
