@echo off
REM ===========================================
REM  IronDiscipline GCP アップロードスクリプト
REM ===========================================

echo ======================================
echo   IronDiscipline GCP Upload
echo ======================================

REM プロジェクトIDを設定
set /p PROJECT_ID="GCPプロジェクトID: "
set BUCKET=gs://%PROJECT_ID%-minecraft

echo.
echo [1/4] プラグインをビルド中...
call mvn clean package -q
if errorlevel 1 (
    echo ビルド失敗！
    pause
    exit /b 1
)

echo [2/4] GCSバケットを作成中...
call gsutil mb -l asia-northeast1 %BUCKET% 2>nul

echo [3/4] ファイルをアップロード中...
call gsutil cp target\IronDiscipline-1.0.0.jar %BUCKET%/plugins/
call gsutil cp plugins\IronDiscipline\config.yml %BUCKET%/plugins/IronDiscipline/

echo [4/4] アップロード完了！

echo.
echo ======================================
echo   アップロード完了
echo ======================================
echo.
echo バケット: %BUCKET%
echo.
echo GCEインスタンスで以下を実行:
echo   gsutil cp %BUCKET%/plugins/IronDiscipline-1.0.0.jar /opt/minecraft/plugins/
echo   sudo systemctl restart minecraft
echo.

pause
