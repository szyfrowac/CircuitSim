@echo off
echo ============================================
echo  Digital Circuit Simulator - Build & Run
echo ============================================

REM Create output directory
if not exist out mkdir out

echo Compiling...
javac -d out -sourcepath srcfix srcfix\Main.java srcfix\model\Wire.java srcfix\model\GateVisual.java srcfix\controller\CircuitEngine.java srcfix\view\StatusPanel.java srcfix\view\TruthTableDialog.java srcfix\view\ToolPanel.java srcfix\view\CircuitPanel.java srcfix\view\MainFrame.java

if %ERRORLEVEL% NEQ 0 (
    echo.
    echo BUILD FAILED. Make sure JDK is installed and 'javac' is in PATH.
    pause
    exit /b 1
)

echo Build successful!
echo Launching...
java -cp out Main
pause
