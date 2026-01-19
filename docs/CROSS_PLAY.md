# 統合版 (Bedrock) 対応ガイド

このサーバーは **Geyser** と **Floodgate** を使用することで、スマホ・Switch・PS4/5・Xbox などの統合版Minecraft（Bedrock Edition）からの接続に対応できます。

## 1. 必要なプラグイン
以下の2つのプラグインを `plugins` フォルダに入れます。
（デプロイ用スクリプト `scripts/gcp-startup.sh` には既にダウンロードコマンドが含まれていますが、手動の場合は以下を行ってください）

- **Geyser**: Java版と統合版の通信を翻訳するコアプラグイン
- **Floodgate**: Java版アカウントを持っていない統合版プレイヤーがログインできるようにする認証プラグイン

```bash
# プラグインディレクトリへ移動
cd /opt/minecraft/plugins

# ダウンロード
curl -o Geyser-Spigot.jar -L "https://download.geysermc.org/v2/projects/geyser/versions/latest/builds/latest/downloads/spigot"
curl -o Floodgate-Spigot.jar -L "https://download.geysermc.org/v2/projects/floodgate/versions/latest/builds/latest/downloads/spigot"

# サーバー再起動
sudo systemctl restart minecraft
```

## 2. ポート開放 (UDP 19132)
統合版は Java版の `25565 (TCP)` とは異なり、**`19132 (UDP)`** を使用します。
このポートをファイアウォールで開放する必要があります。

### GCPの場合 (手元のPCで実行)
```powershell
gcloud compute firewall-rules create geyser-port --allow udp:19132 --target-tags=minecraft-server
```

### Xserver VPS / ConoHa 等の場合
VPSの管理画面にある「ファイアウォール」または「セキュリティグループ」設定で、以下のルールを追加してください。
- プロトコル: **UDP**
- ポート番号: **19132**
- 接続元: **全て (0.0.0.0/0)**

## 3. 接続方法
- **サーバーアドレス**: Java版と同じIPアドレス
- **ポート**: `19132` (デフォルト)

## 補足: スキンについて
Floodgateにより、統合版のスキンがJava版にも自動的に反映されます。
