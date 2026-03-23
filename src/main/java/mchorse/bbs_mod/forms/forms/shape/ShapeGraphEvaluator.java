package mchorse.bbs_mod.forms.forms.shape;

import mchorse.bbs_mod.forms.forms.shape.nodes.*;
import mchorse.bbs_mod.utils.math.Noise;
import mchorse.bbs_mod.utils.colors.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ShapeGraphEvaluator
{
    public final List<IrisShaderNode> irisNodes = new ArrayList<>();
    public final List<IrisAttributeNode> irisAttributeNodes = new ArrayList<>();
    private final Map<Integer, ShapeNode> nodes = new HashMap<>();
    private final Map<Integer, Map<Integer, ShapeConnection>> inputs = new HashMap<>();
    private OutputNode output;
    private Noise noiseGen;

    public ShapeGraphEvaluator(ShapeFormGraph graph)
    {
        for (ShapeNode node : graph.nodes)
        {
            this.nodes.put(node.id, node);
            if (node instanceof OutputNode)
            {
                this.output = (OutputNode) node;
            }
            if (node instanceof IrisShaderNode)
            {
                this.irisNodes.add((IrisShaderNode) node);
            }
            if (node instanceof IrisAttributeNode)
            {
                this.irisAttributeNodes.add((IrisAttributeNode) node);
            }
        }

        for (ShapeConnection c : graph.connections)
        {
            this.inputs.computeIfAbsent(c.inputNodeId, k -> new HashMap<>()).put(c.inputIndex, c);
        }
        
        this.noiseGen = new Noise(0);
    }

    public double compute(double x, double y, double z, double time)
    {
        return this.evaluate(this.output, 0, x, y, z, time);
    }

    public int computeColor(double x, double y, double z, double time)
    {
        int color = -1;

        if (this.output != null && this.hasInput(this.output.id, 1))
        {
            color = (int) this.evaluate(this.output, 1, x, y, z, time);
        }

        return color;
    }

    private double evaluate(ShapeNode node, int outputIndex, double x, double y, double z, double time)
    {
        if (node instanceof ValueNode) return ((ValueNode) node).value;
        if (node instanceof TimeNode) return time;
        if (node instanceof CoordinateNode)
        {
            if (outputIndex == 0) return x;
            if (outputIndex == 1) return y;
            if (outputIndex == 2) return z;
            return 0;
        }
        if (node instanceof ColorNode) return ((ColorNode) node).color.getARGBColor();
        
        if (node instanceof MixColorNode)
        {
            int c1 = (int) this.getInput(node.id, 0, x, y, z, time);
            int c2 = (int) this.getInput(node.id, 1, x, y, z, time);
            double factor = this.getInput(node.id, 2, x, y, z, time);
            
            Color color1 = new Color().set(c1);
            Color color2 = new Color().set(c2);
            
            float r = (float) (color1.r + (color2.r - color1.r) * factor);
            float g = (float) (color1.g + (color2.g - color1.g) * factor);
            float b = (float) (color1.b + (color2.b - color1.b) * factor);
            float a = (float) (color1.a + (color2.a - color1.a) * factor);
            
            return new Color(r, g, b, a).getARGBColor();
        }
        
        if (node instanceof MathNode)
        {
            double a = this.getInput(node.id, 0, x, y, z, time);
            double b = this.getInput(node.id, 1, x, y, z, time);
            int op = ((MathNode) node).operation;
            
            if (op == 0) return a + b;
            if (op == 1) return a - b;
            if (op == 2) return a * b;
            if (op == 3) return b == 0 ? 0 : a / b;
            if (op == 4) return b == 0 ? 0 : a % b;
            if (op == 5) return Math.min(a, b);
            if (op == 6) return Math.max(a, b);
            if (op == 7) return Math.pow(a, b);
            if (op == 8) // Custom
            {
                MathNode math = (MathNode) node;
                
                if (math.compiled == null) return 0;
                
                MathNode.parser.setValue("a", a);
                MathNode.parser.setValue("b", b);
                
                try
                {
                    return math.compiled.get();
                }
                catch (Exception e)
                {
                    return 0;
                }
            }
            
            return 0;
        }

        if (node instanceof VectorMathNode)
        {
            VectorMathNode vn = (VectorMathNode) node;
            int op = vn.operation;

            double ax = this.getInput(node.id, 0, x, y, z, time);
            double ay = this.getInput(node.id, 1, x, y, z, time);
            double az = this.getInput(node.id, 2, x, y, z, time);
            double bx = this.getInput(node.id, 3, x, y, z, time);
            double by = this.getInput(node.id, 4, x, y, z, time);
            double bz = this.getInput(node.id, 5, x, y, z, time);

            if (op == 0) // Add
            {
                if (outputIndex == 0) return ax + bx;
                if (outputIndex == 1) return ay + by;
                if (outputIndex == 2) return az + bz;
            }
            if (op == 1) // Sub
            {
                if (outputIndex == 0) return ax - bx;
                if (outputIndex == 1) return ay - by;
                if (outputIndex == 2) return az - bz;
            }
            if (op == 2) // Mul
            {
                if (outputIndex == 0) return ax * bx;
                if (outputIndex == 1) return ay * by;
                if (outputIndex == 2) return az * bz;
            }
            if (op == 3) // Div
            {
                if (outputIndex == 0) return bx == 0 ? 0 : ax / bx;
                if (outputIndex == 1) return by == 0 ? 0 : ay / by;
                if (outputIndex == 2) return bz == 0 ? 0 : az / bz;
            }
            if (op == 4) // Cross
            {
                if (outputIndex == 0) return ay * bz - az * by;
                if (outputIndex == 1) return az * bx - ax * bz;
                if (outputIndex == 2) return ax * by - ay * bx;
            }
            if (op == 5) // Project
            {
                double dot = ax * bx + ay * by + az * bz;
                double lenSq = bx * bx + by * by + bz * bz;
                double scale = lenSq == 0 ? 0 : dot / lenSq;
                if (outputIndex == 0) return bx * scale;
                if (outputIndex == 1) return by * scale;
                if (outputIndex == 2) return bz * scale;
            }
            if (op == 6) // Reflect
            {
                double dot = ax * bx + ay * by + az * bz;
                // Assume b is normal? If not, we should normalize it. Blender assumes normalized normal.
                // But let's be safe and normalize B logic if needed.
                // Blender Reflect: I - 2 * dot(N, I) * N. (I=Incident=A, N=Normal=B)
                if (outputIndex == 0) return ax - 2 * dot * bx;
                if (outputIndex == 1) return ay - 2 * dot * by;
                if (outputIndex == 2) return az - 2 * dot * bz;
            }
            if (op == 7) // Dot
            {
                return ax * bx + ay * by + az * bz;
            }
            if (op == 8) // Distance
            {
                double dx = ax - bx;
                double dy = ay - by;
                double dz = az - bz;
                return Math.sqrt(dx * dx + dy * dy + dz * dz);
            }
            if (op == 9) // Length
            {
                return Math.sqrt(ax * ax + ay * ay + az * az);
            }
            if (op == 10) // Scale
            {
                // Use bx as scale factor
                if (outputIndex == 0) return ax * bx;
                if (outputIndex == 1) return ay * bx;
                if (outputIndex == 2) return az * bx;
            }
            if (op == 11) // Normalize
            {
                double len = Math.sqrt(ax * ax + ay * ay + az * az);
                if (len == 0) return 0;
                if (outputIndex == 0) return ax / len;
                if (outputIndex == 1) return ay / len;
                if (outputIndex == 2) return az / len;
            }
            if (op == 12) // Abs
            {
                if (outputIndex == 0) return Math.abs(ax);
                if (outputIndex == 1) return Math.abs(ay);
                if (outputIndex == 2) return Math.abs(az);
            }
            if (op == 13) // Min
            {
                if (outputIndex == 0) return Math.min(ax, bx);
                if (outputIndex == 1) return Math.min(ay, by);
                if (outputIndex == 2) return Math.min(az, bz);
            }
            if (op == 14) // Max
            {
                if (outputIndex == 0) return Math.max(ax, bx);
                if (outputIndex == 1) return Math.max(ay, by);
                if (outputIndex == 2) return Math.max(az, bz);
            }
            if (op == 15) // Floor
            {
                if (outputIndex == 0) return Math.floor(ax);
                if (outputIndex == 1) return Math.floor(ay);
                if (outputIndex == 2) return Math.floor(az);
            }
            if (op == 16) // Ceil
            {
                if (outputIndex == 0) return Math.ceil(ax);
                if (outputIndex == 1) return Math.ceil(ay);
                if (outputIndex == 2) return Math.ceil(az);
            }
            if (op == 17) // Fraction
            {
                if (outputIndex == 0) return ax - Math.floor(ax);
                if (outputIndex == 1) return ay - Math.floor(ay);
                if (outputIndex == 2) return az - Math.floor(az);
            }
            if (op == 18) // Modulo
            {
                if (outputIndex == 0) return bx == 0 ? 0 : ax % bx;
                if (outputIndex == 1) return by == 0 ? 0 : ay % by;
                if (outputIndex == 2) return bz == 0 ? 0 : az % bz;
            }
            if (op == 19) // Snap
            {
                if (outputIndex == 0) return bx == 0 ? ax : Math.floor(ax / bx) * bx;
                if (outputIndex == 1) return by == 0 ? ay : Math.floor(ay / by) * by;
                if (outputIndex == 2) return bz == 0 ? az : Math.floor(az / bz) * bz;
            }
            if (op == 20) // Sin
            {
                if (outputIndex == 0) return Math.sin(ax);
                if (outputIndex == 1) return Math.sin(ay);
                if (outputIndex == 2) return Math.sin(az);
            }
            if (op == 21) // Cos
            {
                if (outputIndex == 0) return Math.cos(ax);
                if (outputIndex == 1) return Math.cos(ay);
                if (outputIndex == 2) return Math.cos(az);
            }
            if (op == 22) // Tan
            {
                if (outputIndex == 0) return Math.tan(ax);
                if (outputIndex == 1) return Math.tan(ay);
                if (outputIndex == 2) return Math.tan(az);
            }

            return 0;
        }
        
        if (node instanceof NoiseNode)
        {
            NoiseNode n = (NoiseNode) node;
            this.noiseGen.setSeed(n.seed);
            
            double nx = this.getInput(n.id, 0, x, y, z, time);
            double ny = this.getInput(n.id, 1, x, y, z, time);
            double nz = this.getInput(n.id, 2, x, y, z, time);
            double scale = this.getInput(n.id, 3, x, y, z, time);
            
            if (!this.hasInput(n.id, 0)) nx = x;
            if (!this.hasInput(n.id, 1)) ny = y;
            if (!this.hasInput(n.id, 2)) nz = z;
            if (!this.hasInput(n.id, 3)) scale = 1;
            
            return this.noiseGen.noise(nx * scale, ny * scale, nz * scale);
        }

        if (node instanceof FlowNoiseNode)
        {
            FlowNoiseNode n = (FlowNoiseNode) node;
            this.noiseGen.setSeed(n.seed);
            
            double nx = this.hasInput(n.id, 0) ? this.getInput(n.id, 0, x, y, z, time) : x;
            double ny = this.hasInput(n.id, 1) ? this.getInput(n.id, 1, x, y, z, time) : y;
            double nz = this.hasInput(n.id, 2) ? this.getInput(n.id, 2, x, y, z, time) : z;
            double scale = this.getInput(n.id, 3, x, y, z, time);
            double speed = this.getInput(n.id, 4, x, y, z, time);
            double taper = this.getInput(n.id, 5, x, y, z, time);
            
            if (scale == 0 && !this.hasInput(n.id, 3)) scale = 1;
            if (speed == 0 && !this.hasInput(n.id, 4)) speed = 1;
            
            return this.noiseGen.noise(nx * scale, ny * scale - time * speed, nz * scale) + ny * taper;
        }

        if (node instanceof VoronoiNode)
        {
            VoronoiNode n = (VoronoiNode) node;
            this.noiseGen.setSeed(n.seed);
            
            double vx = this.hasInput(n.id, 0) ? this.getInput(n.id, 0, x, y, z, time) : x;
            double vy = this.hasInput(n.id, 1) ? this.getInput(n.id, 1, x, y, z, time) : y;
            double vz = this.hasInput(n.id, 2) ? this.getInput(n.id, 2, x, y, z, time) : z;
            double scale = this.getInput(n.id, 3, x, y, z, time);
            
            if (scale == 0 && !this.hasInput(n.id, 3)) scale = 1;
            
            return this.noiseGen.voronoi(vx * scale, vy * scale, vz * scale);
        }

        if (node instanceof TriggerNode)
        {
            TriggerNode n = (TriggerNode) node;
            
            double val = this.hasInput(n.id, 0) ? this.getInput(n.id, 0, x, y, z, time) : time;
            double target = this.getInput(n.id, 1, x, y, z, time);
            double range = this.getInput(n.id, 2, x, y, z, time);
            
            if (n.mode == 0) return val > target ? 1 : 0; // GREATER
            if (n.mode == 1) return val < target ? 1 : 0; // LESS
            if (n.mode == 2) return Math.abs(val - target) < (range == 0 ? 0.0001 : range) ? 1 : 0; // EQUAL
            if (n.mode == 3) return Math.abs(val - target) >= (range == 0 ? 0.0001 : range) ? 1 : 0; // NOT_EQUAL
            if (n.mode == 4) // PULSE
            {
                if (target <= 0) target = 1;
                return (val % target) < (range <= 0 ? target / 2 : range) ? 1 : 0;
            }
            
            return 0;
        }
        
        if (node instanceof OutputNode)
        {
            if (outputIndex == 1)
            {
                return this.getInput(node.id, 1, x, y, z, time);
            }
            return this.getInput(node.id, 0, x, y, z, time);
        }
        
        return 0;
    }
    
    public double evaluateInput(int nodeId, int index, double x, double y, double z, double time)
    {
        return this.getInput(nodeId, index, x, y, z, time);
    }

    private boolean hasInput(int nodeId, int index)
    {
        return this.inputs.containsKey(nodeId) && this.inputs.get(nodeId).containsKey(index);
    }

    private double getInput(int nodeId, int index, double x, double y, double z, double time)
    {
        if (this.hasInput(nodeId, index))
        {
            ShapeConnection c = this.inputs.get(nodeId).get(index);
            return this.evaluate(this.nodes.get(c.outputNodeId), c.outputIndex, x, y, z, time);
        }
        return 0;
    }
}
