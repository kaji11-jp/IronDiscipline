#!/bin/bash
# ===========================================
#  IronDiscipline 自動アップデートスクリプト
# ===========================================
# GitHubから最新のビルド(IronDiscipline-latest.jar)をダウンロードして
# サーバーを再起動します。

GITHUB_USER="kaji11-jp"
GITHUB_REPO="IronDiscipline"
JAR_NAME="IronDiscipline-latest.jar"
DOWNLOAD_URL="https://github.com/${GITHUB_USER}/${GITHUB_REPO}/releases/download/latest/${JAR_NAME}"
PLUGIN_DIR="/opt/minecraft/plugins"

echo "======================================"
echo "  [自動アップデート] IronDiscipline"
echo "======================================"

# 1. 権限チェック
if [ "$EUID" -ne 0 ]; then
  echo "エラー: root権限で実行してください (sudo bash update-server.sh)"
  exit 1
fi

# 2. 最新版のダウンロード
echo "[1/3] GitHubから最新版をダウンロード中..."
echo "URL: $DOWNLOAD_URL"

cd $PLUGIN_DIR
# バックアップ
if [ -f "$JAR_NAME" ]; then
    mv "$JAR_NAME" "${JAR_NAME}.bak"
fi

# ダウンロード (curl -L でリダイレクト追従)
if curl -L -o "$JAR_NAME" "$DOWNLOAD_URL"; then
    echo "ダウンロード成功！"
    rm -f "${JAR_NAME}.bak"
    # 古いバージョン名のjarがあれば削除
    rm -f IronDiscipline-1.0.0.jar
else
    echo "ダウンロード失敗。バックアップを復元します。"
    if [ -f "${JAR_NAME}.bak" ]; then
        mv "${JAR_NAME}.bak" "$JAR_NAME"
    fi
    exit 1
fi

# 3. サーバー再起動
echo "[2/3] サーバーを再起動中..."
systemctl restart minecraft

echo "[3/3] 完了！"
echo "サーバーログを確認: journalctl -u minecraft -f"
