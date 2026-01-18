# IronDiscipline (鉄の規律)

Minecraftサーバー用 総合管理・規律維持プラグイン。
軍隊・刑務所RPサーバー向けに設計されています。

## 機能

- **階級システム**: 階級による権限管理、セットアップ機能
- **PTS (Permission to Speak)**: 下士官の発言許可システム
- **Discord連携**:
  - アカウント連携 (`/link`)
  - ロール・ニックネーム同期
  - 通達システム
  - サーバーステータス表示
- **警告・処分システム**:
  - `/warn` で警告蓄積
  - 一定数で自動隔離・Kick
  - `/jail` 隔離システム
- **試験システム**: GUIを使用した昇進試験
- **勤務時間管理**: オンライン時間の記録

## 必要要件

- Java 17+
- Paper / Spigot 1.20+
- MySQL または H2 Database (デフォルト)

## ビルド

```bash
mvn clean package
```

## デプロイ

このプロジェクトは、**Google Cloud Platform (GCP)** や **Xserver VPS** などの一般的なVPSで動作するように設計されています。

### 1. GCP (Google Cloud Platform)
[GCPデプロイガイド (Docs)](docs/GCP_DEPLOY.md) を参照してください。専用スクリプトで数分で構築できます。

### 2. 一般的なVPS (Xserver, ConoHaなど)
[汎用VPSデプロイガイド (Docs)](docs/VPS_DEPLOY.md) を参照してください。Ubuntu環境であれば、汎用スクリプトで一発セットアップ可能です。

## Discord連携設定

`config.yml` に Discord Bot Token 等を設定してください。

```yaml
discord:
  enabled: true
  bot_token: "YOUR_TOKEN"
  guild_id: "YOUR_GUILD_ID"
  notification_channel_id: "YOUR_CHANNEL_ID"
```

## ライセンス

MIT License
