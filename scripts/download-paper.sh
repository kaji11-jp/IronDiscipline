#!/bin/bash
# Paper MC ダウンロードスクリプト

VERSION=${MINECRAFT_VERSION:-1.21.1}

echo "Fetching Paper build info for version $VERSION..."

# Paper API から最新ビルド取得
BUILD=$(curl -s "https://api.papermc.io/v2/projects/paper/versions/$VERSION/builds" | jq -r '.builds[-1].build')

if [ "$BUILD" == "null" ] || [ -z "$BUILD" ]; then
    echo "Error: Could not fetch build info"
    exit 1
fi

echo "Downloading Paper $VERSION build $BUILD..."

curl -o paper.jar -L "https://api.papermc.io/v2/projects/paper/versions/$VERSION/builds/$BUILD/downloads/paper-$VERSION-$BUILD.jar"

echo "Download complete!"
