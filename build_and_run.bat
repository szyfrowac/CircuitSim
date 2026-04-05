@echo off
echo ============================================
echo  Digital Circuit Simulator - Build & Run
echo ============================================

REM Create output directory
if not exist out mkdir out

echo Compiling...
javac -d out -sourcepath src src\Main.java src\model\Wire.java src\model\GateVisual.java src\controller\CircuitEngine.java src\view\StatusPanel.java src\view\TruthTableDialog.java src\view\ToolPanel.java src\view\CircuitPanel.java src\view\MainFrame.java

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
