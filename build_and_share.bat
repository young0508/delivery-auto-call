@echo off
echo ===============================
echo  배달 자동 콜 APK 생성 도구
echo ===============================

echo 1. APK 빌드 중...
call gradlew assembleDebug

if %errorlevel% neq 0 (
    echo ❌ 빌드 실패!
    pause
    exit /b 1
)

echo 2. APK 파일 위치:
echo 📁 %cd%\app\build\outputs\apk\debug\app-debug.apk

echo 3. 파일을 카톡으로 전송하세요!
echo    - 위 경로의 app-debug.apk 파일을
echo    - 카톡에서 파일 전송으로 본인에게 보내기

echo 4. 폰에서 설치:
echo    - 카톡에서 APK 다운로드
echo    - "알 수 없는 소스" 허용
echo    - 앱 설치 완료!

echo.
echo ✅ APK 생성 완료!
explorer app\build\outputs\apk\debug

pause