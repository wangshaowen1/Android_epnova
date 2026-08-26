#!/bin/zsh

set -e

DEVICE="${1:-}"
PACKAGE_NAME="com.kuaigeglobal.epnova"

if [[ -n "$DEVICE" ]]; then
    DEVICE_DESCRIPTION="$DEVICE"
else
    DEVICE_DESCRIPTION="the default ADB device"
fi

adb_shell() {
    if [[ -n "$DEVICE" ]]; then
        adb -s "$DEVICE" shell "$@"
    else
        adb shell "$@"
    fi
}

if ! command -v adb >/dev/null 2>&1; then
    echo "[ERROR] adb was not found. Please add Android SDK platform-tools to PATH."
    read -r "?Press Enter to close..."
    exit 1
fi

trap 'echo "[ERROR] ADB command failed. Make sure ${DEVICE_DESCRIPTION} is available."; read -r "?Press Enter to close..."' ERR

echo "[1/3] Enabling Firebase Analytics debug mode..."
adb_shell setprop debug.firebase.analytics.app "$PACKAGE_NAME"

echo "[2/3] Current Firebase Analytics debug package:"
adb_shell getprop debug.firebase.analytics.app

echo "[3/3] Force-stopping $PACKAGE_NAME..."
adb_shell am force-stop "$PACKAGE_NAME"

echo "Done. Please launch the app again."
read -r "?Press Enter to close..."
