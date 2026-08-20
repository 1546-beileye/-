#!/usr/bin/env bash
# 一键构建 APK：自动检测/安装 JDK 与 Android SDK，然后出 debug 包
set -e

echo "== AgentSkills APK 构建脚本 =="

# 1. JDK
if ! command -v java >/dev/null 2>&1; then
  echo "[1/4] 安装 JDK 17..."
  if command -v apt-get >/dev/null 2>&1; then
    sudo apt-get update -y && sudo apt-get install -y openjdk-17-jdk
  elif command -v yum >/dev/null 2>&1; then
    sudo yum install -y java-17-openjdk-devel
  else
    echo "无法自动安装 JDK，请手动安装 JDK 17 后重试。"; exit 1
  fi
else
  echo "[1/4] JDK 已安装: $(java -version 2>&1 | head -1)"
fi

# 2. Android SDK
SDK_ROOT="${ANDROID_HOME:-$HOME/Android/Sdk}"
if [ ! -d "$SDK_ROOT/platforms" ]; then
  echo "[2/4] 安装 Android SDK 到 $SDK_ROOT ..."
  mkdir -p "$SDK_ROOT/cmdline-tools"
  CMDTOOLS_ZIP=/tmp/cmdline-tools.zip
  curl -fsSL -o "$CMDTOOLS_ZIP" \
    "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
  unzip -qo "$CMDTOOLS_ZIP" -d "$SDK_ROOT/cmdline-tools"
  mv "$SDK_ROOT/cmdline-tools/cmdline-tools" "$SDK_ROOT/cmdline-tools/latest"
  yes | "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" \
    --licenses >/dev/null 2>&1 || true
  "$SDK_ROOT/cmdline-tools/latest/bin/sdkmanager" \
    "platform-tools" "platforms;android-35" "build-tools;35.0.0"
  echo "export ANDROID_HOME=$SDK_ROOT" >> ~/.bashrc
else
  echo "[2/4] Android SDK 已存在: $SDK_ROOT"
fi
export ANDROID_HOME="$SDK_ROOT"

# 3. Gradle wrapper
if [ ! -f gradlew ]; then
  echo "[3/4] 生成 gradle wrapper..."
  gradle wrapper --gradle-version 8.9 || \
    curl -fsSL -o gradle-wrapper.jar \
      "https://raw.githubusercontent.com/gradle/gradle/v8.9.0/gradle/wrapper/gradle-wrapper.jar"
fi

# 4. 构建
echo "[4/4] 构建 debug APK..."
./gradlew assembleDebug --no-daemon
echo "== 完成：app/build/outputs/apk/debug/app-debug.apk =="
