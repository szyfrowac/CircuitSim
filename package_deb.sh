#!/bin/bash
set -euo pipefail

APP_NAME="Digital Circuit Simulator"
PACKAGE_NAME="digital-circuit-simulator"
VERSION="${1:-0.1.0}"
ARCH="all"
ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="$ROOT_DIR/deb-build"
STAGE_DIR="$BUILD_DIR/${PACKAGE_NAME}_${VERSION}_${ARCH}"
APP_DIR="$STAGE_DIR/opt/$PACKAGE_NAME"

rm -rf "$BUILD_DIR"
mkdir -p "$APP_DIR" "$STAGE_DIR/DEBIAN" "$STAGE_DIR/usr/bin" "$STAGE_DIR/usr/share/applications"

echo "Compiling Java sources..."
mkdir -p "$ROOT_DIR/out"
javac -d "$ROOT_DIR/out" -sourcepath "$ROOT_DIR/srcfix" \
    "$ROOT_DIR/srcfix/Main.java" \
    "$ROOT_DIR/srcfix/controller/CircuitEngine.java" \
    "$ROOT_DIR/srcfix/controller/CircuitXmlIO.java" \
    "$ROOT_DIR/srcfix/model/GateVisual.java" \
    "$ROOT_DIR/srcfix/model/Wire.java" \
    "$ROOT_DIR/srcfix/view/CircuitPanel.java" \
    "$ROOT_DIR/srcfix/view/MainFrame.java" \
    "$ROOT_DIR/srcfix/view/StatusPanel.java" \
    "$ROOT_DIR/srcfix/view/ToolPanel.java" \
    "$ROOT_DIR/srcfix/view/TruthTableDialog.java" \
    "$ROOT_DIR/srcfix/view/UiScale.java"

echo "Creating runnable JAR..."
jar --create --file "$APP_DIR/circuitsim.jar" --main-class Main -C "$ROOT_DIR/out" .

install -m 0755 "$ROOT_DIR/packaging/deb/circuitsim" "$STAGE_DIR/usr/bin/circuitsim"
install -m 0644 "$ROOT_DIR/packaging/deb/circuitsim.desktop" "$STAGE_DIR/usr/share/applications/circuitsim.desktop"

sed -e "s/@PACKAGE_NAME@/$PACKAGE_NAME/g" \
    -e "s/@VERSION@/$VERSION/g" \
    -e "s/@ARCH@/$ARCH/g" \
    -e "s/@APP_NAME@/$APP_NAME/g" \
    "$ROOT_DIR/packaging/deb/control" > "$STAGE_DIR/DEBIAN/control"
printf '\n' >> "$STAGE_DIR/DEBIAN/control"

echo "Building .deb package..."
dpkg-deb --build "$STAGE_DIR" "$ROOT_DIR/${PACKAGE_NAME}_${VERSION}_${ARCH}.deb"

echo "Created: $ROOT_DIR/${PACKAGE_NAME}_${VERSION}_${ARCH}.deb"