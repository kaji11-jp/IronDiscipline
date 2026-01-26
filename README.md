# IronDiscipline (鉄の規律)

Minecraftサーバー用 総合管理・規律維持プラグイン。
軍隊・刑務所RPサーバー向けに設計されています。

## 機能

- **階級システム**: 階級による権限管理、config.ymlでの完全なカスタマイズが可能
- **PTS (Permission to Speak)**: 下士官の発言許可システム
- **Discord連携**:
  - アカウント連携 (`/link`)
  - ロール・ニックネーム同期
  - 通達システム
  - サーバーステータス表示
- **警告・処分システム**:
  - `/warn` で警告蓄積
  - 一定数で自動隔離・Kick
  - `/jail` 隔離システム (DB保存)
- **試験システム**: GUIを使用した昇進試験
- **勤務時間管理**: オンライン時間の記録
- **メッセージカスタマイズ**: ゲーム内メッセージのほとんどを変更可能

## 必要要件

- Java 17+
- Paper / Spigot 1.20+
- LuckPerms (必須)
- MySQL, SQLite または H2 Database (デフォルト)

## インストール

1. [Releases](https://github.com/kaji11-jp/IronDiscipline/releases) から最新のJARファイルをダウンロードします。
2. サーバーの `plugins` フォルダに配置します。
3. サーバーを起動します。
4. `plugins/IronDiscipline/config.yml` が生成されるので、必要に応じて編集します（データベースやDiscord連携など）。
5. サーバーを再起動または `/iron reload` で設定を反映させます。

## 設定

### データベース設定
デフォルトでは H2 Database (ファイルベース) を使用しますが、大規模サーバーでは MySQL の使用を推奨します。

```yaml
database:
  # タイプ: h2, sqlite, mysql
  type: mysql
  mysql:
    host: localhost
    port: 3306
    database: irondiscipline
    username: root
    password: "password"
```

### Discord連携設定
`config.yml` に Discord Bot Token 等を設定してください。

```yaml
discord:
  enabled: true
  bot_token: "YOUR_TOKEN"
  guild_id: "YOUR_GUILD_ID"
  notification_channel_id: "YOUR_CHANNEL_ID"
```

## コマンド一覧

### 🌐 一般コマンド
| コマンド | 説明 | 権限 |
|---|---|---|
| `/link [コード]` | Discordアカウント連携 | なし |
| `/playtime [top]` | 勤務時間（プレイ時間）を表示 | `iron.playtime.view` |
| `/radio <周波数>` | 無線チャンネルに参加・退出 | `iron.radio.use` |
| `/radiobroadcast <msg>` | 無線で広域放送 | `iron.radio.use` |
| `/warnings [player]` | 自分または他人の警告履歴を表示 | `iron.warn.view` |

### 👮 規律・管理コマンド
| コマンド | 説明 | 権限 |
|---|---|---|
| `/warn <player> <理由>` | 警告を与える（累積で自動処分） | `iron.warn.use` |
| `/unwarn <player>` | 最新の警告を取り消す | `iron.warn.admin` |
| `/clearwarnings <player>` | 警告を全消去する | `iron.warn.admin` |
| `/jail <player> [理由]` | プレイヤーを強制隔離 | `iron.jail.use` |
| `/unjail <player>` | プレイヤーを釈放 | `iron.jail.use` |
| `/setjail` | 隔離場所を現在地に設定 | `iron.jail.admin` |
| `/grant <player> [秒]` | 下士官に発言権(PTS)を付与 | `iron.pts.grant` |
| `/promote <player>` | 階級を昇進させる | `iron.rank.promote` |
| `/demote <player>` | 階級を降格させる | `iron.rank.demote` |
| `/division <set/remove...>` | 部隊配属・除隊管理 | `iron.division.use` |
| `/exam <start/end...>` | 昇進試験の管理 | `iron.exam.use` |
| `/killlog [player] [数]` | PvP詳細ログの確認 | `iron.killlog.view` |
| `/iron reload` | 設定リロード | `iron.admin` |

### 🤖 Discord Bot コマンド（スラッシュコマンド）
| コマンド | 説明 |
|---|---|
| `/link` | アカウント連携（DM・サーバー両対応） |
| `/settings` | Bot設定・ロール紐付け管理 |
| `/panel` | 連携・ロール管理パネルの設置 |
| `/promote, /demote` | 階級操作（Discordから実行可） |
| `/division` | 部隊管理 |
| `/kick, /ban` | 処罰実行 |

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

### 3. 統合版 (スマホ/Switch) 対応
[統合版対応ガイド (Docs)](docs/CROSS_PLAY.md) を参照してください。Geyserを使用してクロスプラットフォームプレイを実現します。

## 🔄 自動アップデート

このプロジェクトは **GitHub Actions** による自動ビルドに対応しています。
`main` ブランチにプッシュされると、自動的に最新版がビルドされ、[Releases](https://github.com/kaji11-jp/IronDiscipline/releases) に `latest` タグとして公開されます。

### サーバーでのアップデート方法
以下のコマンドを1回実行するだけで、最新版への更新と再起動が完了します。

```bash
# SSH接続後
curl -sL https://raw.githubusercontent.com/kaji11-jp/IronDiscipline/main/scripts/update-server.sh | sudo bash
```

## ライセンス

MIT License
