@echo off
echo 배달 자동 콜 앱 설치 시작...

echo 1. Gradle 빌드 중...
call gradlew assembleDebug

if %errorlevel% neq 0 (
    echo 빌드 실패! Android Studio에서 확인해주세요.
    pause
    exit /b 1
)

echo 2. APK 파일 생성 완료!
echo 위치: app\build\outputs\apk\debug\app-debug.apk

echo 3. 폰 연결 확인 중...
adb devices

echo 4. 앱 설치 중...
adb install -r app\build\outputs\apk\debug\app-debug.apk

if %errorlevel% eq 0 (
    echo 설치 완료! 폰에서 "배달 자동 콜" 앱을 확인하세요.
) else (
    echo 설치 실패! USB 디버깅을 확인해주세요.
)

pause