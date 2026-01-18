# IronDiscipline Minecraft Server
FROM eclipse-temurin:17-jre-alpine

LABEL maintainer="IronDiscipline"
LABEL description="Minecraft Server with IronDiscipline Plugin"

# 環境変数
ENV MINECRAFT_VERSION=1.21.1
ENV PAPER_BUILD=latest
ENV MEMORY=2G
ENV SERVER_PORT=25565

# 作業ディレクトリ
WORKDIR /minecraft

# 必要なパッケージ
RUN apk add --no-cache curl jq

# サーバーJARダウンロードスクリプト
COPY scripts/download-paper.sh /minecraft/
RUN chmod +x /minecraft/download-paper.sh

# プラグインコピー
COPY target/IronDiscipline-1.0.0.jar /minecraft/plugins/
COPY plugins/LuckPerms*.jar /minecraft/plugins/

# 設定ファイル
COPY docker/server.properties /minecraft/
COPY docker/eula.txt /minecraft/

# ポート公開
EXPOSE 25565/tcp
EXPOSE 25565/udp

# ボリューム（永続化）
VOLUME ["/minecraft/world", "/minecraft/plugins/IronDiscipline", "/minecraft/plugins/LuckPerms"]

# 起動
COPY scripts/start.sh /minecraft/
RUN chmod +x /minecraft/start.sh

ENTRYPOINT ["/minecraft/start.sh"]
