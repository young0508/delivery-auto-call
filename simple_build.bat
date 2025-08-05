@echo off
echo ==========================================
echo  Android APK Simple Builder
echo ==========================================
echo.

echo This requires Android Studio to be installed.
echo.
echo Alternative methods:
echo.
echo 1. Install Android Studio:
echo    https://developer.android.com/studio
echo.
echo 2. Open this project folder in Android Studio
echo.
echo 3. Click Build -^> Build Bundle(s) / APK(s) -^> Build APK(s)
echo.
echo 4. APK will be created in:
echo    app\build\outputs\apk\debug\app-debug.apk
echo.
echo ==========================================
echo.

echo Would you like to open Android Studio download page?
set /p choice="Type Y to open browser, or any key to exit: "

if /i "%choice%"=="y" (
    start https://developer.android.com/studio
)

echo.
echo Press any key to exit...
pause > nul