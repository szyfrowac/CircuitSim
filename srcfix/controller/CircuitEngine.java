package controller;

import model.GateVisual;
import model.Wire;

import java.util.*;

/**
 * Stateless simulation engine.
 */
public class CircuitEngine {

    // ─────────────────────────────────────────────────────────────────────────
    // SIMULATE
    // Returns nodeId→signal map, or null on short-circuit.
    // ─────────────────────────────────────────────────────────────────────────
    public static Map<Integer,Integer> simulate(
            List<GateVisual> gates,
            List<Wire>       wires,
            Map<Integer,Integer> manualInputs) {

        Map<Integer,Integer> signals = new HashMap<>(manualInputs);

        // Seed switches: closed=1, open=0
        for (GateVisual g : gates) {
            if (g.getType().equals("SWITCH")) {
                signals.put(g.getOutputNodeId(), g.isClosed() ? 1 : 0);
            }
        }

        // Iterative propagation (handles any gate ordering)
        boolean changed = true;
        int iter = 0;
        while (changed && iter < 1000) {
            changed = false;
            iter++;

            // Gate computation
            for (GateVisual g : gates) {
                if (g.getType().equals("SWITCH")) continue;

                // All inputs must be resolved
                boolean allReady = true;
                for (int id : g.getInputNodeIds()) {
                    if (!signals.containsKey(id)) { allReady = false; break; }
                }
                if (!allReady) continue;

                int computed = computeGate(g, signals);
                int outId    = g.getOutputNodeId();
                Integer prev = signals.get(outId);
                if (prev != null && prev != computed) return null; // SHORT CIRCUIT
                if (prev == null) { signals.put(outId, computed); changed = true; }
            }

            // Wire propagation
            for (Wire w : wires) {
                Integer val = signals.get(w.getFromNode());
                if (val == null) continue;
                Integer prev = signals.get(w.getToNode());
                if (prev != null && prev != val) return null; // SHORT CIRCUIT
                if (prev == null) { signals.put(w.getToNode(), val); changed = true; }
            }
        }

        // Pull undriven LED inputs to GND
        for (GateVisual g : gates) {
            if (g.getType().equals("LED")) {
                for (int id : g.getInputNodeIds()) signals.putIfAbsent(id, 0);
            }
        }

        return signals;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // VERIFY
    // ─────────────────────────────────────────────────────────────────────────
    public static String verify(List<GateVisual> gates, List<Wire> wires) {
        // Check if any output node is driven by more than one gate
        Map<Integer,Integer> driverCount = new HashMap<>();
        for (GateVisual g : gates) {
            int out = g.getOutputNodeId();
            driverCount.merge(out, 1, Integer::sum);
        }

        List<String> issues = new ArrayList<>();
        for (Map.Entry<Integer,Integer> e : driverCount.entrySet()) {
            if (e.getValue() > 1) {
                issues.add("Short circuit: node " + e.getKey() + " driven by " + e.getValue() + " gates.");
            }
        }

        // Try a full simulation to catch wire-propagated shorts
        Map<Integer,Integer> simResult = simulate(gates, wires, new HashMap<>());
        if (simResult == null) issues.add("Short circuit detected during simulation (conflicting wire values).");

        if (issues.isEmpty()) return "Circuit OK — no short circuits detected.";
        return String.join("\n", issues);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // TRUTH TABLE
    // ─────────────────────────────────────────────────────────────────────────
    public static List<String[]> truthTable(List<GateVisual> gates, List<Wire> wires) {
        List<GateVisual> switches = new ArrayList<>();
        List<GateVisual> leds     = new ArrayList<>();
        for (GateVisual g : gates) {
            if (g.getType().equals("SWITCH")) switches.add(g);
            if (g.getType().equals("LED"))    leds.add(g);
        }

        int n = switches.size();

        // Header row
        List<String[]> table = new ArrayList<>();
        String[] header = new String[n + leds.size()];
        for (int i = 0; i < n; i++)
            header[i] = "SW" + (i+1) + " (node " + switches.get(i).getOutputNodeId() + ")";
        for (int i = 0; i < leds.size(); i++)
            header[n+i] = "LED" + (i+1) + " (node " + leds.get(i).getInputNodeIds().get(0) + ")";
        table.add(header);

        if (n == 0) return table; // no switches → nothing to enumerate

        // Enumerate all 2^n switch combinations
        int combos = 1 << n;
        for (int combo = 0; combo < combos; combo++) {
            // Clone gates (counter-safe) and set switch states
            List<GateVisual> cloned = cloneGates(gates);
            List<GateVisual> cSwitches = new ArrayList<>();
            for (GateVisual g : cloned) if (g.getType().equals("SWITCH")) cSwitches.add(g);

            for (int i = 0; i < n; i++) {
                boolean wantClosed = ((combo >> (n - 1 - i)) & 1) == 1;
                // Clones start as open (isClosed=false); toggle if we want closed
                if (wantClosed) cSwitches.get(i).toggleSwitch();
            }

            Map<Integer,Integer> result = simulate(cloned, wires, new HashMap<>());

            String[] row = new String[n + leds.size()];
            for (int i = 0; i < n; i++)
                row[i] = ((combo >> (n - 1 - i)) & 1) == 1 ? "1" : "0";

            List<GateVisual> cLeds = new ArrayList<>();
            for (GateVisual g : cloned) if (g.getType().equals("LED")) cLeds.add(g);

            for (int i = 0; i < cLeds.size(); i++) {
                if (result == null) { row[n+i] = "SHORT"; continue; }
                int ledIn = cLeds.get(i).getInputNodeIds().get(0);
                row[n+i] = result.getOrDefault(ledIn, 0) == 1 ? "1" : "0";
            }
            table.add(row);
        }
        return table;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private static int computeGate(GateVisual g, Map<Integer,Integer> signals) {
        List<Integer> ins = g.getInputNodeIds();
        int a = signals.getOrDefault(ins.get(0), 0);
        switch (g.getType()) {
            case "NOT": return a == 0 ? 1 : 0;
            case "LED": return a;
            case "AND": { int b = signals.getOrDefault(ins.get(1), 0); return a & b; }
            case "OR":  { int b = signals.getOrDefault(ins.get(1), 0); return a | b; }
            case "XOR": { int b = signals.getOrDefault(ins.get(1), 0); return a ^ b; }
            default:    return 0;
        }
    }

    /** Clone gate list without touching the global node counter. */
    private static List<GateVisual> cloneGates(List<GateVisual> orig) {
        List<GateVisual> out = new ArrayList<>();
        for (GateVisual g : orig) out.add(GateVisual.deserialize(g.serialize()));
        return out;
    }
}
