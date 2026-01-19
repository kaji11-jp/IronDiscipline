# IronDiscipline GCP デプロイガイド

## 前提条件

1. [Google Cloud SDK](https://cloud.google.com/sdk/docs/install) インストール済み
2. GCPプロジェクト作成済み
3. 課金有効化済み

## 方法1: 簡単デプロイ（推奨）

### 1. プラグインをビルド

```powershell
mvn clean package
```

### 2. GCSバケットにアップロード

```bash
# バケット作成
gsutil mb gs://irondiscipline-server

# JARアップロード
gsutil cp target/IronDiscipline-latest.jar gs://irondiscipline-server/
gsutil cp plugins/LuckPerms*.jar gs://irondiscipline-server/
```

### 3. GCEインスタンス作成

```bash
gcloud compute instances create irondiscipline-mc \
    --zone=asia-northeast1-b \
    --machine-type=e2-medium \
    --image-family=ubuntu-2204-lts \
    --image-project=ubuntu-os-cloud \
    --boot-disk-size=30GB \
    --tags=minecraft-server \
    --metadata-from-file startup-script=scripts/gcp-startup.sh
```

### 4. ファイアウォール設定

```bash
gcloud compute firewall-rules create minecraft-port \
    --allow tcp:25565,udp:25565 \
    --target-tags=minecraft-server
```

### 5. 接続

```bash
# IP確認
gcloud compute instances describe irondiscipline-mc --zone=asia-northeast1-b \
    --format='get(networkInterfaces[0].accessConfigs[0].natIP)'
```

Minecraftで `<IP>:25565` に接続！

---

## 方法2: Docker（上級者向け）

```bash
# SSH接続
gcloud compute ssh irondiscipline-mc --zone=asia-northeast1-b

# Docker インストール
sudo apt-get update && sudo apt-get install -y docker.io docker-compose

# コンテナ起動
docker-compose up -d
```

---

## 料金目安（東京リージョン）

| マシンタイプ | RAM | 月額（概算） |
|-------------|-----|-------------|
| e2-micro | 1GB | 無料枠内 |
| e2-small | 2GB | ~$15 |
| e2-medium | 4GB | ~$30 |

---

## Discord Bot設定

1. サーバー起動後、config.ymlを編集：

```bash
gcloud compute ssh irondiscipline-mc --zone=asia-northeast1-b
sudo nano /opt/minecraft/plugins/IronDiscipline/config.yml
```

2. Discord設定を入力：

```yaml
discord:
  enabled: true
  bot_token: "YOUR_BOT_TOKEN"
  notification_channel_id: "CHANNEL_ID"
  guild_id: "SERVER_ID"
```

3. サーバー再起動：

```bash
sudo systemctl restart minecraft
```

---

## 便利コマンド

```bash
# ログ確認
sudo journalctl -u minecraft -f

# サーバー停止
sudo systemctl stop minecraft

# サーバー起動
sudo systemctl start minecraft

# コンソール接続
screen -r minecraft
```
