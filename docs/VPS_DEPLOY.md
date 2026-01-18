# 汎用VPSデプロイガイド (Xserver, ConoHa, Linode等)

このプロジェクトは、Google Cloud Platform以外の一般的なVPS（Virtual Private Server）でも簡単に動作させることができます。

## 対応VPSの例
- **Xserver VPS** (日本・高速・安定)
- **ConoHa VPS** (日本・使いやすい)
- **Linode / DigitalOcean / Vultr** (海外・安価)

## 1. サーバーの準備

### 推奨スペック
- **OS**: Ubuntu 22.04 LTS または 24.04 LTS
- **CPU**: 2コア以上
- **メモリ**: 4GB以上 (推奨8GB)

### 手順
1. VPSの管理画面からインスタンス（サーバー）を作成します。
2. OSは **Ubuntu** を選択してください。
3. `root` パスワードを設定するか、SSH鍵を登録します。

## 2. セットアップスクリプトの実行

サーバーにSSH接続し、以下のコマンドを実行するだけで環境構築が完了します。

# 1. SSH接続 (PowerShell / Terminal)
ssh root@<サーバーIPアドレス>

# 2. スクリプトのダウンロードと実行
curl -O https://raw.githubusercontent.com/kaji11-jp/IronDiscipline/main/scripts/setup-ubuntu.sh
sudo bash setup-ubuntu.sh

# ビルド
mvn clean package

# アップロード
scp target/IronDiscipline-1.0.0.jar root@<サーバーIP>:/opt/minecraft/plugins/

### FileZilla / WinSCP を使う場合
1. ホスト: `<サーバーIP>`, ユーザー: `root`, パスワード: `(設定したもの)` で接続
2. `/opt/minecraft/plugins/` に `.jar` ファイルをドラッグ＆ドロップ

最後にサーバーを再起動して反映：
```bash
ssh root@<サーバーIP> "systemctl restart minecraft"
```

## 4. ポート開放 (必要に応じて)

多くのVPSはデフォルトで全ポート開放されていますが、Xserver VPSなどの一部では管理画面でファイアウォール設定が必要です。

**開放が必要なポート:**
- TCP: `25565` (Java版)
- UDP: `19132` (統合版/スマホ - Geyser使用時)
        
## 5. Discord連携設定

1. コンフィグを開く
```bash
nano /opt/minecraft/plugins/IronDiscipline/config.yml
```
2. `bot_token` などを入力して保存 (`Ctrl+S`, `Ctrl+X`)
3. 再起動: `systemctl restart minecraft`
