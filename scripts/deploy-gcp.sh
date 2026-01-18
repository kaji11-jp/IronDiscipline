#!/bin/bash
# ===========================================
#  IronDiscipline GCP デプロイスクリプト
# ===========================================

set -e

# 設定
PROJECT_ID="${GCP_PROJECT:-your-project-id}"
ZONE="${GCP_ZONE:-asia-northeast1-b}"
INSTANCE_NAME="irondiscipline-mc"
MACHINE_TYPE="e2-medium"  # 2 vCPU, 4GB RAM

echo "======================================"
echo "  IronDiscipline GCP デプロイ"
echo "======================================"

# 1. ビルド
echo "[1/5] プラグインをビルド中..."
mvn clean package -q

# 2. GCE インスタンス作成
echo "[2/5] GCE インスタンスを作成中..."
gcloud compute instances create $INSTANCE_NAME \
    --project=$PROJECT_ID \
    --zone=$ZONE \
    --machine-type=$MACHINE_TYPE \
    --image-family=ubuntu-2204-lts \
    --image-project=ubuntu-os-cloud \
    --boot-disk-size=30GB \
    --tags=minecraft-server \
    --metadata-from-file startup-script=scripts/gcp-startup.sh

# 3. ファイアウォールルール
echo "[3/5] ファイアウォールルールを設定中..."
gcloud compute firewall-rules create minecraft-server \
    --project=$PROJECT_ID \
    --allow tcp:25565,udp:25565 \
    --target-tags=minecraft-server \
    --description="Minecraft Server Port" \
    2>/dev/null || echo "ファイアウォールルールは既に存在します"

# 4. 外部IPを取得
echo "[4/5] IPアドレスを取得中..."
sleep 10
EXTERNAL_IP=$(gcloud compute instances describe $INSTANCE_NAME \
    --project=$PROJECT_ID \
    --zone=$ZONE \
    --format='get(networkInterfaces[0].accessConfigs[0].natIP)')

echo ""
echo "======================================"
echo "  デプロイ完了！"
echo "======================================"
echo ""
echo "サーバーIP: $EXTERNAL_IP:25565"
echo ""
echo "サーバーの準備には数分かかります。"
echo "ログを確認: gcloud compute ssh $INSTANCE_NAME --zone=$ZONE --command='sudo journalctl -u minecraft -f'"
echo ""
