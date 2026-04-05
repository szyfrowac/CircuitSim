#!/bin/bash
echo "============================================"
echo " Digital Circuit Simulator - Build & Run"
echo "============================================"

mkdir -p out

echo "Compiling..."
javac -d out -sourcepath src \
    src/Main.java \
    src/model/Wire.java \
    src/model/GateVisual.java \
    src/controller/CircuitEngine.java \
    src/view/StatusPanel.java \
    src/view/TruthTableDialog.java \
    src/view/ToolPanel.java \
    src/view/CircuitPanel.java \
    src/view/MainFrame.java

if [ $? -ne 0 ]; then
    echo ""
    echo "BUILD FAILED. Make sure JDK is installed (sudo apt install default-jdk)"
    exit 1
fi

echo "Build successful! Launching..."
java -cp out Main
