package controller;

import model.GateVisual;
import model.Wire;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

public class CircuitXmlIO {

    public static final class CircuitData {
        private final List<GateVisual> gates;
        private final List<Wire> wires;
        private final Map<Integer, Integer> manualSignals;

        public CircuitData(List<GateVisual> gates, List<Wire> wires, Map<Integer, Integer> manualSignals) {
            this.gates = gates;
            this.wires = wires;
            this.manualSignals = manualSignals;
        }

        public List<GateVisual> getGates() {
            return gates;
        }

        public List<Wire> getWires() {
            return wires;
        }

        public Map<Integer, Integer> getManualSignals() {
            return manualSignals;
        }
    }

    private CircuitXmlIO() {}

    public static void save(File file,
                            List<GateVisual> gates,
                            List<Wire> wires,
                            Map<Integer, Integer> manualSignals,
                            IntFunction<Point> nodePositionResolver) throws Exception {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.newDocument();

        Element root = doc.createElement("circuit");
        root.setAttribute("version", "1");
        doc.appendChild(root);

        Element componentsEl = doc.createElement("components");
        root.appendChild(componentsEl);
        for (GateVisual gate : gates) {
            Element compEl = doc.createElement("component");
            compEl.setAttribute("id", String.valueOf(gate.getComponentId()));
            compEl.setAttribute("type", gate.getType());
            compEl.setAttribute("x", String.valueOf(gate.getX()));
            compEl.setAttribute("y", String.valueOf(gate.getY()));
            compEl.setAttribute("width", String.valueOf(gate.getWidth()));
            compEl.setAttribute("height", String.valueOf(gate.getHeight()));
            compEl.setAttribute("switchClosed", String.valueOf(gate.isClosed()));
            compEl.setAttribute("ledOn", String.valueOf(gate.isOn()));

            Element nodesEl = doc.createElement("nodes");
            for (Integer inId : gate.getInputNodeIds()) {
                Element nodeEl = doc.createElement("node");
                nodeEl.setAttribute("role", "input");
                nodeEl.setAttribute("id", String.valueOf(inId));
                Point p = nodePositionResolver.apply(inId);
                if (p != null) {
                    nodeEl.setAttribute("x", String.valueOf(p.x));
                    nodeEl.setAttribute("y", String.valueOf(p.y));
                }
                nodesEl.appendChild(nodeEl);
            }

            Element outNodeEl = doc.createElement("node");
            outNodeEl.setAttribute("role", "output");
            outNodeEl.setAttribute("id", String.valueOf(gate.getOutputNodeId()));
            Point p = nodePositionResolver.apply(gate.getOutputNodeId());
            if (p != null) {
                outNodeEl.setAttribute("x", String.valueOf(p.x));
                outNodeEl.setAttribute("y", String.valueOf(p.y));
            }
            nodesEl.appendChild(outNodeEl);

            compEl.appendChild(nodesEl);
            componentsEl.appendChild(compEl);
        }

        Element wiresEl = doc.createElement("wires");
        root.appendChild(wiresEl);
        for (Wire wire : wires) {
            Element wireEl = doc.createElement("wire");
            wireEl.setAttribute("fromNode", String.valueOf(wire.getFromNode()));
            wireEl.setAttribute("toNode", String.valueOf(wire.getToNode()));

            Point from = nodePositionResolver.apply(wire.getFromNode());
            Point to = nodePositionResolver.apply(wire.getToNode());
            if (from != null) {
                wireEl.setAttribute("startX", String.valueOf(from.x));
                wireEl.setAttribute("startY", String.valueOf(from.y));
            }
            if (to != null) {
                wireEl.setAttribute("endX", String.valueOf(to.x));
                wireEl.setAttribute("endY", String.valueOf(to.y));
            }

            wiresEl.appendChild(wireEl);
        }

        Element signalsEl = doc.createElement("manualSignals");
        root.appendChild(signalsEl);
        for (Map.Entry<Integer, Integer> entry : manualSignals.entrySet()) {
            Element signalEl = doc.createElement("signal");
            signalEl.setAttribute("nodeId", String.valueOf(entry.getKey()));
            signalEl.setAttribute("value", String.valueOf(entry.getValue()));
            signalsEl.appendChild(signalEl);
        }

        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
        transformer.transform(new DOMSource(doc), new StreamResult(file));
    }

    public static CircuitData load(File file) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(file);
        doc.getDocumentElement().normalize();

        List<GateVisual> gates = new ArrayList<>();
        List<Wire> wires = new ArrayList<>();
        Map<Integer, Integer> manualSignals = new HashMap<>();

        NodeList componentNodes = doc.getElementsByTagName("component");
        int maxNodeId = 0;
        int maxComponentId = 0;
        for (int i = 0; i < componentNodes.getLength(); i++) {
            Node n = componentNodes.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;

            Element compEl = (Element) n;
            String type = compEl.getAttribute("type");
            int x = parseIntAttr(compEl, "x", 0);
            int y = parseIntAttr(compEl, "y", 0);
            int componentId = parseIntAttr(compEl, "id", i + 1);
            boolean switchClosed = Boolean.parseBoolean(compEl.getAttribute("switchClosed"));

            List<Integer> inputIds = new ArrayList<>();
            int outputId = 0;

            NodeList nodes = compEl.getElementsByTagName("node");
            for (int j = 0; j < nodes.getLength(); j++) {
                Node node = nodes.item(j);
                if (node.getNodeType() != Node.ELEMENT_NODE) continue;
                Element nodeEl = (Element) node;

                int nodeId = parseIntAttr(nodeEl, "id", 0);
                String role = nodeEl.getAttribute("role");
                if ("output".equals(role)) outputId = nodeId;
                else inputIds.add(nodeId);

                if (nodeId > maxNodeId) maxNodeId = nodeId;
            }

            GateVisual gate = GateVisual.deserialize(buildSerializedGate(type, componentId, x, y, switchClosed, inputIds, outputId));
            gate.setComponentId(componentId);
            gate.setPosition(x, y);
            gate.setClosed(switchClosed);

            gates.add(gate);
            if (componentId > maxComponentId) maxComponentId = componentId;
        }

        NodeList wireNodes = doc.getElementsByTagName("wire");
        for (int i = 0; i < wireNodes.getLength(); i++) {
            Node n = wireNodes.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element wireEl = (Element) n;

            int from = parseIntAttr(wireEl, "fromNode", 0);
            int to = parseIntAttr(wireEl, "toNode", 0);
            wires.add(new Wire(from, to));

            if (from > maxNodeId) maxNodeId = from;
            if (to > maxNodeId) maxNodeId = to;
        }

        NodeList signalNodes = doc.getElementsByTagName("signal");
        for (int i = 0; i < signalNodes.getLength(); i++) {
            Node n = signalNodes.item(i);
            if (n.getNodeType() != Node.ELEMENT_NODE) continue;
            Element sigEl = (Element) n;

            int nodeId = parseIntAttr(sigEl, "nodeId", 0);
            int val = parseIntAttr(sigEl, "value", 0);
            manualSignals.put(nodeId, val == 0 ? 0 : 1);
            if (nodeId > maxNodeId) maxNodeId = nodeId;
        }

        GateVisual.bumpCounterAbove(maxNodeId);
        GateVisual.bumpComponentCounterAbove(maxComponentId);

        return new CircuitData(gates, wires, manualSignals);
    }

    private static int parseIntAttr(Element e, String attr, int fallback) {
        try {
            String v = e.getAttribute(attr);
            if (v == null || v.isEmpty()) return fallback;
            return Integer.parseInt(v);
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private static String buildSerializedGate(String type,
                                              int componentId,
                                              int x,
                                              int y,
                                              boolean switchClosed,
                                              List<Integer> inputIds,
                                              int outputId) {
        StringBuilder sb = new StringBuilder();
        sb.append(type).append(',').append(componentId).append(',').append(x).append(',').append(y).append(',').append(switchClosed);
        for (Integer id : inputIds) sb.append(",I").append(id);
        sb.append(",O").append(outputId);
        return sb.toString();
    }
}
