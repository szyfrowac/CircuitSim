# CircuitSim - Digital Circuit Simulator

A Java Swing-based digital combinational logic simulator with interactive wiring, live simulation, XML save/load, and truth-table generation.

## Requirements

- Java JDK 11 or later
- `javac` and `java` available in your PATH

Check installation:

```bash
javac -version
java -version
```

## Quick Start

### Linux / macOS

```bash
chmod +x build_and_run.sh
./build_and_run.sh
```

### Windows

Run:

```bat
build_and_run.bat
```

or double-click `build_and_run.bat`.

## Manual Build and Run

From the project root:

```bash
javac -d out -sourcepath srcfix \
    srcfix/Main.java \
    srcfix/model/Wire.java \
    srcfix/model/GateVisual.java \
    srcfix/controller/CircuitEngine.java \
    srcfix/controller/CircuitXmlIO.java \
    srcfix/view/UiScale.java \
    srcfix/view/StatusPanel.java \
    srcfix/view/TruthTableDialog.java \
    srcfix/view/ToolPanel.java \
    srcfix/view/CircuitPanel.java \
    srcfix/view/MainFrame.java

java -cp out Main
```

## What You Can Build

- Combinational circuits using AND, OR, NOT, XOR, SWITCH, and LED
- Branching wires by connecting into an existing wire
- Truth tables for all switch combinations
- XML project save/load

## Components

| Component | Inputs | Outputs | Notes |
|---|---:|---:|---|
| AND | 2 | 1 | Output is HIGH only if both inputs are HIGH |
| OR | 2 | 1 | Output is HIGH if any input is HIGH |
| NOT | 1 | 1 | Output is inverse of input |
| XOR | 2 | 1 | Output is HIGH if inputs differ |
| SWITCH | 0 | 1 | Click to toggle; closed = 1, open = 0 |
| LED | 1 | 1* | Visual indicator; glows when driven HIGH |

`*` LED internally has an output node ID for model compatibility, but is used as an indicator.

## How to Use

### 1) Place components

1. Choose a gate/tool from the left panel.
2. A floating preview follows the mouse on the circuit panel.
3. Click to place and fix the component at that location.
4. Press `Esc` to cancel tool mode.

### 2) Create wires (port-to-port interaction)

1. Click a port to start a wire.
2. Move the mouse to see the live wire preview.
3. Click another port to terminate the wire.
4. You can also click an existing wire segment to insert a junction and branch.

### 3) Connect by node numbers (optional)

- Enter node IDs in the "Connect Nodes" section and press Connect.
- Reverse order is auto-corrected where possible.

### 4) Simulate

- Click SWITCH components to toggle logic values.
- LED updates live according to circuit state.

### 5) Edit

- Select a gate and press `Delete` to remove it.
- Right-click a wire to remove it.
- Right-click during wire creation to cancel pending wire.

### 6) Analyze and verify

- `Tools -> Verification` checks for short-circuit conditions.
- `Tools -> Truth Table Generator` enumerates all switch combinations.

### 7) Save and load

- `File -> Save` / `File -> Save As...` writes XML.
- `File -> Open` restores gates, wires, and manual signals.

## Wiring Rules and Safety Checks

- Preferred direction: output/junction -> input/junction
- Direct same-gate self-loop is blocked (a gate output cannot wire to its own input)
- Duplicate wires are blocked
- Invalid directions are rejected with a connect error

## Visual Semantics

- Wire color:
    - Green = HIGH (1)
    - Gray = LOW (0)
- LED:
    - Green glow = HIGH
    - Dark red = LOW

## Simulation Rules

- SWITCH output is seeded directly from state:
    - closed -> 1
    - open -> 0
- Signals propagate iteratively through gates and wires
- If conflicting drivers force different values on the same node, simulation reports short circuit
- Undriven LED input defaults to 0

## Project Structure

```text
.
├── README.md
├── build_and_run.sh
├── build_and_run.bat
└── srcfix/
        ├── Main.java
        ├── controller/
        │   ├── CircuitEngine.java
        │   └── CircuitXmlIO.java
        ├── model/
        │   ├── GateVisual.java
        │   └── Wire.java
        └── view/
                ├── MainFrame.java
                ├── CircuitPanel.java
                ├── ToolPanel.java
                ├── StatusPanel.java
                ├── TruthTableDialog.java
                └── UiScale.java
```

## Troubleshooting

### Build fails with `javac: command not found`

- Install JDK 11+ and ensure `javac` is in PATH.

### VS Code shows package/import errors

- Ensure Java source root points to `srcfix`.
- If needed, run `Java: Clean Java Language Server Workspace` and reload VS Code.

### Menu text is not readable

- The app sets menu text color explicitly in `MainFrame` UI defaults.
- Restart the app after pulling latest changes.

## Current Limitations

- Combinational logic only (no sequential elements like flip-flops)
- Undo/Redo and Zoom are placeholders
- Branching from wire-to-wire works via inserted junction nodes, but junctions are implicit (not separate visible components)
