#!/bin/bash
# ===========================================
#  Ubuntu/Debian 汎用セットアップスクリプト
# ===========================================
# 対応: Xserver VPS, ConoHa VPS, Linode, DigitalOcean etc.
# 動作環境: Ubuntu 22.04 LTS / 24.04 LTS (Debian 12も可)

set -e

# ルート権限チェック
if [ "$EUID" -ne 0 ]; then
  echo "エラー: このスクリプトは root 権限で実行してください (sudo bash setup-ubuntu.sh)"
  exit 1
fi

echo "====== Minecraft Server Setup (Generic VPS) ======"

# 1. パッケージ更新 & ツールインストール
echo "[1/6] パッケージを更新中..."
apt-get update
# Java 21 (Paper 1.21+用), screen, curl, jq
apt-get install -y openjdk-21-jre-headless screen curl jq unzip

# 2. ユーザー作成
echo "[2/6] Minecraftユーザーを作成中..."
useradd -r -m -d /opt/minecraft minecraft || true

# 3. ディレクトリ準備
echo "[3/6] ディレクトリを作成中..."
mkdir -p /opt/minecraft/{plugins,world}
cd /opt/minecraft

# 4. Paper MC ダウンロード
VERSION="1.21.1" # 必要に応じて変更
echo "[4/6] Paper MC $VERSION をダウンロード中..."
BUILD=$(curl -s "https://api.papermc.io/v2/projects/paper/versions/$VERSION/builds" | jq -r '.builds[-1].build')
curl -o paper.jar -L "https://api.papermc.io/v2/projects/paper/versions/$VERSION/builds/$BUILD/downloads/paper-$VERSION-$BUILD.jar"

# EULA同意
echo "eula=true" > eula.txt

# server.properties (基本設定)
cat > server.properties << 'EOF'
server-port=25565
motd=\u00a76\u00a7l[Iron Discipline] \u00a7f\u00a7l\u9244\u306e\u898f\u5f8b
max-players=50
online-mode=true
enable-command-block=false
spawn-protection=0
allow-flight=true
view-distance=10
simulation-distance=8
EOF

# 5. 基本プラグイン導入
echo "[5/6] 基本プラグインを導入中..."
# LuckPerms
curl -o plugins/LuckPerms.jar -L "https://download.luckperms.net/1552/bukkit/loader/LuckPerms-Bukkit-5.4.145.jar"

# 6. サービス化 (systemd)
echo "[6/6] 自動起動設定を作成中..."
cat > /etc/systemd/system/minecraft.service << 'EOF'
[Unit]
Description=Minecraft Server
After=network.target

[Service]
User=minecraft
WorkingDirectory=/opt/minecraft
ExecStart=/usr/bin/java -Xms4G -Xmx4G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -jar paper.jar nogui
ExecStop=/bin/kill -SIGTERM $MAINPID
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# 権限修正
chown -R minecraft:minecraft /opt/minecraft

# サービス起動
systemctl daemon-reload
systemctl enable minecraft
systemctl start minecraft

echo ""
echo "======================================"
echo "  セットアップ完了！"
echo "======================================"
echo "サーバーIP: $(curl -s ifconfig.me):25565"
echo ""
echo "【次のステップ】"
echo "1. ローカルでビルドした 'IronDiscipline-1.0.0.jar' をSFTP等でアップロード"
echo "   場所: /opt/minecraft/plugins/"
echo "2. サーバーを再起動: sudo systemctl restart minecraft"
echo "3. ログ確認: sudo journalctl -u minecraft -f"
echo ""
