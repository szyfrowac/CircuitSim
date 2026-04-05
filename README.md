# Digital Circuit Simulator

A Java Swing-based combinational logic circuit simulator.

## Requirements
- Java JDK 11 or later
- Download from: https://adoptium.net or https://www.oracle.com/java/

## How to Run

### Windows
Double-click `build_and_run.bat`
OR in Command Prompt:
```
build_and_run.bat
```

### Linux / macOS
```bash
chmod +x build_and_run.sh
./build_and_run.sh
```

---

## Features

### Components
| Component | Description |
|-----------|-------------|
| AND | 2-input AND gate |
| OR  | 2-input OR gate  |
| NOT | 1-input NOT gate |
| XOR | 2-input XOR gate |
| SWITCH | Interactive toggle; open → VCC (1), closed → VCC (1) |
| LED | Lights green when input = 1, dark red when input = 0 |

### How to Use

1. **Add a gate**: Click a button in the left panel (e.g. AND), then click on the canvas.
2. **Read node numbers**: Each terminal shows a number (yellow) next to the dot.
3. **Connect nodes**: Enter two node numbers in "Connect Nodes" → Connect.
4. **Toggle switch**: Left-click any SWITCH on the canvas.
5. **Delete gate**: Select it (left-click) → press Delete, or use toolbar button.
6. **Delete wire**: Right-click on a wire.
7. **Manual signal**: Enter node + value (0/1) in "Input Signals" → Apply All.
8. **Save/Load**: File → Save / Open (.circuit files).
9. **Truth Table**: Tools → Truth Table Generator.
10. **Verify**: Tools → Verification (detects short circuits).

### Node Wiring Rules
- Output nodes are on the **right** side of each gate.
- Input nodes are on the **left** side.
- Connect an output node → an input node to form a wire.
- Wires are drawn with right-angle routing and colour: **green = HIGH (1)**, grey = LOW (0).

### Simulation Rules
- Open SWITCH output → pulled to VCC (1).
- Open LED input → pulled to GND (0).
- Short circuit: two gates driving the same node to different values → red status bar alert.

---

## Project Structure

```
src/
├── Main.java                    Entry point
├── model/
│   ├── GateVisual.java          Gate data model + serialization
│   └── Wire.java                Wire (from-node → to-node)
├── controller/
│   └── CircuitEngine.java       Simulation, truth table, verification
└── view/
    ├── MainFrame.java           Top-level JFrame + menus + toolbar
    ├── CircuitPanel.java        Canvas — drawing + interaction
    ├── ToolPanel.java           Left sidebar
    ├── StatusPanel.java         Bottom status bar
    └── TruthTableDialog.java    Truth table popup
```
