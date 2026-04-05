#!/bin/bash
echo "============================================"
echo " Digital Circuit Simulator - Build & Run"
echo "============================================"

mkdir -p out

echo "Compiling..."
javac -d out -sourcepath srcfix \
    srcfix/Main.java \
    srcfix/model/Wire.java \
    srcfix/model/GateVisual.java \
    srcfix/controller/CircuitEngine.java \
    srcfix/view/StatusPanel.java \
    srcfix/view/TruthTableDialog.java \
    srcfix/view/ToolPanel.java \
    srcfix/view/CircuitPanel.java \
    srcfix/view/MainFrame.java

if [ $? -ne 0 ]; then
    echo ""
    echo "BUILD FAILED. Make sure JDK is installed (sudo apt install default-jdk)"
    exit 1
fi

echo "Build successful! Launching..."
java -cp out Main
