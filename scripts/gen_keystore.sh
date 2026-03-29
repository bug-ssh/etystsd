#!/usr/bin/env bash
# ============================================================
#  一键生成签名 Keystore 并输出 GitHub Secrets 所需的 base64
#  使用方法：bash scripts/gen_keystore.sh
# ============================================================
set -e

KEYSTORE_FILE="release.jks"
KEY_ALIAS="voiceassistant"
STORE_PASS="$(openssl rand -base64 16)"
KEY_PASS="$(openssl rand -base64 16)"

echo "🔐 正在生成签名 Keystore..."

keytool -genkey -v \
  -keystore "$KEYSTORE_FILE" \
  -alias "$KEY_ALIAS" \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass "$STORE_PASS" \
  -keypass "$KEY_PASS" \
  -dname "CN=VoiceAssistant, OU=Mobile, O=VoiceApp, L=China, S=China, C=CN"

echo ""
echo "✅ Keystore 生成完成：$KEYSTORE_FILE"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "📋 请将以下内容分别填入 GitHub Secrets："
echo "   仓库 → Settings → Secrets and variables → Actions"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "Secret 名称: KEYSTORE_BASE64"
echo "Secret 值:"
base64 "$KEYSTORE_FILE"
echo ""
echo "Secret 名称: KEYSTORE_PASSWORD"
echo "Secret 值: $STORE_PASS"
echo ""
echo "Secret 名称: KEY_ALIAS"
echo "Secret 值: $KEY_ALIAS"
echo ""
echo "Secret 名称: KEY_PASSWORD"
echo "Secret 值: $KEY_PASS"
echo ""
echo "⚠️  请妥善保存以上密钥，丢失后无法用同一签名更新 APP"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

# 安全删除明文 jks，base64 已打印到终端
rm -f "$KEYSTORE_FILE"
echo "🧹 本地 jks 文件已删除（已输出 base64）"
