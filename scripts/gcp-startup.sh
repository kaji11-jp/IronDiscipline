#!/bin/bash
# GCP インスタンス起動時のスタートアップスクリプト

set -e

# ログ設定
exec > >(tee /var/log/minecraft-setup.log)
exec 2>&1

echo "====== Minecraft Server Setup ======"

# パッケージインストール
apt-get update
apt-get install -y openjdk-17-jre-headless screen curl jq unzip

# Minecraft ユーザー作成
useradd -r -m -d /opt/minecraft minecraft || true

# ディレクトリ作成
mkdir -p /opt/minecraft/{plugins,world}
cd /opt/minecraft

# Paper MCダウンロード
VERSION="1.21.1"
echo "Paper MC $VERSION をダウンロード中..."
BUILD=$(curl -s "https://api.papermc.io/v2/projects/paper/versions/$VERSION/builds" | jq -r '.builds[-1].build')
curl -o paper.jar -L "https://api.papermc.io/v2/projects/paper/versions/$VERSION/builds/$BUILD/downloads/paper-$VERSION-$BUILD.jar"

# EULA同意
echo "eula=true" > eula.txt

# server.properties
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

# LuckPermsダウンロード
echo "LuckPerms をダウンロード中..."
LUCKPERMS_URL="https://download.luckperms.net/1552/bukkit/loader/LuckPerms-Bukkit-5.4.145.jar"
curl -o plugins/LuckPerms.jar -L "$LUCKPERMS_URL"

# TODO: IronDiscipline.jar をGCSからダウンロード
# gsutil cp gs://YOUR_BUCKET/IronDiscipline-1.0.0.jar plugins/

# 権限設定
chown -R minecraft:minecraft /opt/minecraft

# systemdサービス作成
cat > /etc/systemd/system/minecraft.service << 'EOF'
[Unit]
Description=Minecraft Server
After=network.target

[Service]
User=minecraft
WorkingDirectory=/opt/minecraft
ExecStart=/usr/bin/java -Xms3G -Xmx3G -XX:+UseG1GC -XX:+ParallelRefProcEnabled -XX:MaxGCPauseMillis=200 -jar paper.jar nogui
ExecStop=/bin/kill -SIGTERM $MAINPID
Restart=on-failure
RestartSec=10

[Install]
WantedBy=multi-user.target
EOF

# サービス有効化・起動
systemctl daemon-reload
systemctl enable minecraft
systemctl start minecraft

echo "====== Setup Complete ======"
echo "Server starting..."
