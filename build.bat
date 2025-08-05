@echo off
chcp 65001 > nul
cls

echo ===============================
echo  Delivery Auto Call APK Builder  
echo ===============================
echo.

echo [1] Checking Gradle...
if not exist gradlew.bat (
    echo ERROR: gradlew.bat not found!
    echo Please install Android Studio first.
    pause
    exit /b 1
)

echo [2] Building APK... (This may take 2-5 minutes)
echo Please wait...
gradlew.bat assembleDebug

if %errorlevel% neq 0 (
    echo.
    echo ===== BUILD FAILED! =====
    echo Please check:
    echo 1. Java JDK installed
    echo 2. Internet connection
    echo 3. Android SDK installed
    echo.
    pause
    exit /b 1
)

echo.
echo ===== BUILD SUCCESS! =====
echo.
echo APK file created at:
echo %cd%\app\build\outputs\apk\debug\app-debug.apk
echo.
echo Next steps:
echo 1. Send APK file via KakaoTalk
echo 2. Download on phone
echo 3. Install (allow unknown sources)
echo 4. Set permissions (accessibility is most important!)
echo.

if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo Opening APK folder...
    explorer app\build\outputs\apk\debug
) else (
    echo APK file not found! Build may have failed.
)

echo.
echo Press any key to exit...
pause > nul