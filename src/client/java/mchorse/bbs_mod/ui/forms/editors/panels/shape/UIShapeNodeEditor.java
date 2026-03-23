package mchorse.bbs_mod.ui.forms.editors.panels.shape;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.forms.forms.shape.ShapeConnection;
import mchorse.bbs_mod.forms.forms.shape.ShapeFormGraph;
import mchorse.bbs_mod.forms.forms.shape.nodes.BumpNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.ColorNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.CommentNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.CoordinateNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.MathNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.MixColorNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.NoiseNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.FlowNoiseNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.IrisAttributeNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.IrisShaderNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.TriggerNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.OutputNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.ShapeNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.TimeNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.ValueNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.VectorMathNode;
import mchorse.bbs_mod.forms.forms.shape.nodes.VoronoiNode;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIColorOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UINumberOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIPromptOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UITextareaOverlayPanel;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.utils.UI;

import mchorse.bbs_mod.ui.utils.context.ContextMenuManager;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.ui.utils.presets.UICopyPasteController;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.presets.PresetManager;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class UIShapeNodeEditor extends UIElement
{
    private ShapeFormGraph graph;
    
    private float scale = 1F;
    private float translateX = 0;
    private float translateY = 0;
    
    private int lastMouseX;
    private int lastMouseY;
    private boolean dragging;
    
    private ShapeNode draggingNode;
    private float draggingNodeX;
    private float draggingNodeY;
    private int draggingMouseX;
    private int draggingMouseY;

    private int draggingConnectionNode = -1;
    private int draggingConnectionIndex = -1;
    private boolean draggingConnectionInput = false;

    private final Set<ShapeNode> selection = new HashSet<>();
    private final Set<ShapeConnection> selectedConnections = new HashSet<>();
    private final Map<ShapeNode, Vector2f> initialPositions = new HashMap<>();
    private boolean selecting;
    private int selectingX;
    private int selectingY;
    private static MapType clipboard;

    private UIElement toolbar;
    private UIIcon presets;
    private UICopyPasteController copyPaste;

    public UIShapeNodeEditor()
    {
        super();
        
        this.copyPaste = new UICopyPasteController(PresetManager.SHAPE_GRAPHS, "ShapeGraph");
        this.copyPaste.supplier(this::createData);
        this.copyPaste.consumer(this::pasteData);

        this.toolbar = UI.row(0, 0);
        this.toolbar.relative(this).x(10).y(10).w(100).h(20);
        
        this.presets = new UIIcon(Icons.SAVED, (b) -> this.openPresets());
        this.toolbar.add(this.presets);
        this.toolbar.add(new UIIcon(Icons.REFRESH, (b) -> this.resetView()));
        
        this.add(this.toolbar);
    }
    
    private void openPresets()
    {
        // Implementation assumed
    }
    
    private void resetView()
    {
        this.scale = 1F;
        this.translateX = 0;
        this.translateY = 0;
    }
    
    private MapType createData()
    {
        if (this.selection.isEmpty() && this.selectedConnections.isEmpty()) return null;
        
        MapType data = new MapType();
        ListType nodesList = new ListType();
        ListType connectionsList = new ListType();
        
        for (ShapeNode node : this.selection)
        {
            MapType nodeData = new MapType();
            node.toData(nodeData);
            nodesList.add(nodeData);
        }
        
        // Add selected connections or connections between selected nodes
        Set<ShapeConnection> connectionsToAdd = new HashSet<>(this.selectedConnections);
        
        if (this.graph != null)
        {
            for (ShapeConnection c : this.graph.connections)
            {
                boolean inputSelected = false;
                boolean outputSelected = false;
                
                for (ShapeNode node : this.selection)
                {
                    if (node.id == c.inputNodeId) inputSelected = true;
                    if (node.id == c.outputNodeId) outputSelected = true;
                }
                
                if (inputSelected && outputSelected)
                {
                    connectionsToAdd.add(c);
                }
            }
        }
        
        for (ShapeConnection c : connectionsToAdd)
        {
            MapType cData = new MapType();
            c.toData(cData);
            connectionsList.add(cData);
        }
        
        data.put("nodes", nodesList);
        data.put("connections", connectionsList);
        
        return data;
    }
    
    private void pasteData(MapType data, int mouseX, int mouseY)
    {
        if (this.graph == null) return;
        
        MapType map = data;
        ListType nodesList = map.getList("nodes");
        ListType connectionsList = map.getList("connections");
        
        Map<Integer, Integer> idMap = new HashMap<>();
        
        this.selection.clear();
        this.selectedConnections.clear();
        
        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        
        // Calculate center
        for (BaseType element : nodesList)
        {
            if (element instanceof MapType)
            {
                MapType nodeData = (MapType) element;
                float x = nodeData.getFloat("x");
                float y = nodeData.getFloat("y");
                
                if (x < minX) minX = x;
                if (y < minY) minY = y;
            }
        }
        
        float graphX = (-this.translateX + mouseX - this.area.x) / this.scale;
        float graphY = (-this.translateY + mouseY - this.area.y) / this.scale;
        
        for (BaseType element : nodesList)
        {
            if (element instanceof MapType)
            {
                MapType nodeData = (MapType) element;
                String type = nodeData.getString("type");
                ShapeNode node = this.graph.createNode(type);
                
                if (node != null)
                {
                    node.fromData(nodeData);
                    
                    int oldId = node.id;
                    node.id = 0;
                    this.graph.addNode(node);
                    idMap.put(oldId, node.id);
                    
                    node.x = graphX + (node.x - minX);
                    node.y = graphY + (node.y - minY);
                    
                    this.selection.add(node);
                }
            }
        }
        
        for (BaseType element : connectionsList)
        {
            if (element instanceof MapType)
            {
                ShapeConnection c = new ShapeConnection();
                c.fromData((MapType) element);
                
                if (idMap.containsKey(c.inputNodeId) && idMap.containsKey(c.outputNodeId))
                {
                    this.graph.connect(idMap.get(c.outputNodeId), c.outputIndex, idMap.get(c.inputNodeId), c.inputIndex);
                }
            }
        }
    }
    
    private void copyNodes()
    {
        MapType data = this.createData();
        
        if (data != null)
        {
            clipboard = data;
        }
    }
    
    private void pasteNodes(int mx, int my)
    {
        if (clipboard != null)
        {
            this.pasteData(clipboard, mx, my);
        }
    }

    public void setGraph(ShapeFormGraph graph)
    {
        this.graph = graph;
    }

    @Override
    protected boolean subMouseClicked(UIContext context)
    {
        if (this.area.isInside(context))
        {
            int mx = context.mouseX;
            int my = context.mouseY;
            
            if (this.graph != null)
            {
                // Check sockets first
                for (int i = this.graph.nodes.size() - 1; i >= 0; i--)
                {
                    ShapeNode node = this.graph.nodes.get(i);
                    int nx = (int) (this.area.x + this.translateX + node.x * this.scale);
                    int ny = (int) (this.area.y + this.translateY + node.y * this.scale);
                    int w = (int) (120 * this.scale);
                    
                    // Outputs
                    List<String> outputs = node.getOutputs();
                    for (int j = 0; j < outputs.size(); j++)
                    {
                        int sx = nx + w;
                        int sy = ny + (int) ((30 + j * 20) * this.scale);
                        
                        if (Math.abs(mx - sx) < Math.max(10 * this.scale, 6F) && Math.abs(my - sy) < Math.max(10 * this.scale, 6F))
                        {
                            this.draggingConnectionNode = node.id;
                            this.draggingConnectionIndex = j;
                            this.draggingConnectionInput = false;
                            return true;
                        }
                    }
                    
                    // Inputs
                    List<String> inputs = node.getInputs();
                    for (int j = 0; j < inputs.size(); j++)
                    {
                        int sx = nx;
                        int sy = ny + (int) ((30 + j * 20) * this.scale);
                        
                        if (Math.abs(mx - sx) < Math.max(10 * this.scale, 6F) && Math.abs(my - sy) < Math.max(10 * this.scale, 6F))
                        {
                            this.draggingConnectionNode = node.id;
                            this.draggingConnectionIndex = j;
                            this.draggingConnectionInput = true;
                            return true;
                        }
                    }
                }

                // Check nodes
                for (int i = this.graph.nodes.size() - 1; i >= 0; i--)
                {
                    ShapeNode node = this.graph.nodes.get(i);
                    int nx = (int) (this.area.x + this.translateX + node.x * this.scale);
                    int ny = (int) (this.area.y + this.translateY + node.y * this.scale);
                    
                    int max = Math.max(node.getInputs().size(), node.getOutputs().size());
                    int w = (int) (120 * this.scale);
                    int h = (int) ((35 + max * 20) * this.scale);

                    if (mx >= nx && mx <= nx + w && my >= ny && my <= ny + h)
                    {
                        if (context.mouseButton == 0)
                        {
                            if (Window.isCtrlPressed())
                            {
                                if (this.selection.contains(node)) this.selection.remove(node);
                                else this.selection.add(node);
                            }
                            else if (Window.isShiftPressed())
                            {
                                this.selection.add(node);
                            }
                            else
                            {
                                if (!this.selection.contains(node))
                                {
                                    this.selection.clear();
                                    this.selectedConnections.clear();
                                    this.selection.add(node);
                                }
                            }
                            
                            this.draggingNode = node;
                            this.draggingNodeX = node.x;
                            this.draggingNodeY = node.y;
                            this.draggingMouseX = mx;
                            this.draggingMouseY = my;
                            this.lastMouseX = mx;
                            this.lastMouseY = my;
                            
                            this.initialPositions.clear();
                            for (ShapeNode selected : this.selection)
                            {
                                this.initialPositions.put(selected, new Vector2f(selected.x, selected.y));
                            }
                            
                            return true;
                        }
                        else if (context.mouseButton == 1)
                        {
                            this.lastMouseX = mx;
                            this.lastMouseY = my;

                            if (!this.selection.contains(node))
                            {
                                this.selection.clear();
                                this.selectedConnections.clear();
                                this.selection.add(node);
                            }
                            
                            this.openNodeContextMenu(context, node);
                            return true;
                        }
                    }
                }
                
                // Check connections
                for (ShapeConnection c : this.graph.connections)
                {
                    if (this.isOverConnection(mx, my, c))
                    {
                        if (context.mouseButton == 0)
                        {
                            if (Window.isCtrlPressed())
                            {
                                if (this.selectedConnections.contains(c)) this.selectedConnections.remove(c);
                                else this.selectedConnections.add(c);
                            }
                            else if (Window.isShiftPressed())
                            {
                                this.selectedConnections.add(c);
                            }
                            else
                            {
                                this.selection.clear();
                                this.selectedConnections.clear();
                                this.selectedConnections.add(c);
                            }
                            return true;
                        }
                    }
                }
            }

            if (context.mouseButton == 2 || context.mouseButton == 0)
            {
                if (context.mouseButton == 0 && Window.isShiftPressed())
                {
                    this.selecting = true;
                    this.selectingX = mx;
                    this.selectingY = my;
                }
                else
                {
                    this.lastMouseX = mx;
                    this.lastMouseY = my;
                    this.dragging = true;
                }
                
                // Clear selection if not adding
                if (!Window.isShiftPressed() && !Window.isCtrlPressed())
                {
                    this.selection.clear();
                    this.selectedConnections.clear();
                }
                
                return true;
            }
            
            if (context.mouseButton == 1)
            {
                this.lastMouseX = mx;
                this.lastMouseY = my;
                this.openContextMenu(context);
            }
        }

        return super.subMouseClicked(context);
    }
    
    private void openContextMenu(UIContext context)
    {
        ContextMenuManager menu = new ContextMenuManager();
        
        if (!this.selection.isEmpty() || !this.selectedConnections.isEmpty())
        {
            menu.action(Icons.REMOVE, UIKeys.GENERAL_REMOVE, Colors.NEGATIVE, () -> this.removeSelection());
        }
        
        if (!this.selection.isEmpty())
        {
            menu.action(Icons.COPY, IKey.raw("Copy Nodes"), () -> this.copyNodes());
        }
        
        if (clipboard != null)
        {
            menu.action(Icons.PASTE, IKey.raw("Paste Nodes"), () -> this.pasteNodes(this.lastMouseX, this.lastMouseY));
        }
        
        menu.action(Icons.ADD, IKey.raw("Add Node"), () -> this.openNodeAddMenu(context));
        
        context.replaceContextMenu(menu.create());
    }
    
    private void openNodeAddMenu(UIContext context)
    {
        ContextMenuManager menu = new ContextMenuManager();
        
        menu.action(Icons.DOWNLOAD, IKey.raw("Output"), () -> this.addNode("output"));
        menu.action(Icons.ALL_DIRECTIONS, IKey.raw("Coordinate"), () -> this.addNode("coordinate"));
        menu.action(Icons.GEAR, IKey.raw("Math"), () -> this.addNode("math"));
        menu.action(Icons.ALL_DIRECTIONS, IKey.raw("Vector Math"), () -> this.addNode("vector_math"));
        menu.action(Icons.MAXIMIZE, IKey.raw("Value"), () -> this.addNode("value"));
        menu.action(Icons.TIME, IKey.raw("Time"), () -> this.addNode("time"));
        menu.action(Icons.MATERIAL, IKey.raw("Color"), () -> this.addNode("color"));
        menu.action(Icons.REFRESH, IKey.raw("Mix Color"), () -> this.addNode("mix_color"));
        menu.action(Icons.EDIT, IKey.raw("Comment"), () -> this.addNode("comment"));
        menu.action(Icons.SOUND, IKey.raw("Noise"), () -> this.addNode("noise"));
        menu.action(Icons.SOUND, IKey.raw("Voronoi"), () -> this.addNode("voronoi"));
        menu.action(Icons.SOUND, IKey.raw("Flow Noise"), () -> this.addNode("flow_noise"));
        menu.action(Icons.GEAR, IKey.raw("Trigger"), () -> this.addNode("trigger"));
        menu.action(Icons.UPLOAD, IKey.raw("Bump"), () -> this.addNode("bump"));
        menu.action(Icons.GLOBE, IKey.raw("Iris Shader"), () -> this.addNode("iris_shader"));
        menu.action(Icons.VISIBLE, IKey.raw("Iris Attribute"), () -> this.addNode("iris_attribute"));
        
        context.replaceContextMenu(menu.create());
    }
    
    private void addNode(String type)
    {
        if (this.graph == null) return;
        
        ShapeNode node = this.graph.createNode(type);
        
        if (node != null)
        {
            node.x = (this.lastMouseX - this.area.x - this.translateX) / this.scale;
            node.y = (this.lastMouseY - this.area.y - this.translateY) / this.scale;
            
            this.graph.addNode(node);
        }
    }
    
    private void removeSelection()
    {
        for (ShapeNode node : this.selection)
        {
            this.graph.removeNode(node);
        }
        
        this.graph.connections.removeAll(this.selectedConnections);
        
        this.selection.clear();
        this.selectedConnections.clear();
    }
    
    private void removeNode(ShapeNode node)
    {
        this.graph.removeNode(node);
        this.selection.remove(node);
    }
    
    private void openNodeContextMenu(UIContext context, ShapeNode node)
    {
        ContextMenuManager menu = new ContextMenuManager();

        if (node instanceof MathNode)
        {
            MathNode math = (MathNode) node;
            ContextMenuManager op = new ContextMenuManager();

            op.action(Icons.ADD, IKey.raw("Add"), () -> math.operation = 0);
            op.action(Icons.REMOVE, IKey.raw("Sub"), () -> math.operation = 1);
            op.action(Icons.CLOSE, IKey.raw("Mul"), () -> math.operation = 2);
            op.action(Icons.MAXIMIZE, IKey.raw("Div"), () -> math.operation = 3);
            op.action(Icons.REFRESH, IKey.raw("Mod"), () -> math.operation = 4);
            op.action(Icons.DOWNLOAD, IKey.raw("Min"), () -> math.operation = 5);
            op.action(Icons.UPLOAD, IKey.raw("Max"), () -> math.operation = 6);
            op.action(Icons.MORE, IKey.raw("Pow"), () -> math.operation = 7);
            op.action(Icons.EDIT, IKey.raw("Custom..."), () -> {
                math.operation = 8;
                
                UIPromptOverlayPanel panel = new UIPromptOverlayPanel(IKey.raw("Edit Expression"), IKey.raw("Enter Molang expression"), (s) -> math.setExpression(s));
                panel.text.setText(math.expression);
                UIOverlay.addOverlay(context, panel);
            });

            menu.action(Icons.GEAR, IKey.raw("Operation"), () -> context.replaceContextMenu(op.create()));
            
            if (math.operation == 8)
            {
                menu.action(Icons.EDIT, IKey.raw("Edit Expression"), () -> {
                    UIPromptOverlayPanel panel = new UIPromptOverlayPanel(IKey.raw("Edit Expression"), IKey.raw("Enter Molang expression"), (s) -> math.setExpression(s));
                    panel.text.setText(math.expression);
                    UIOverlay.addOverlay(context, panel);
                });
            }
        }
        else if (node instanceof VectorMathNode)
        {
            VectorMathNode math = (VectorMathNode) node;
            ContextMenuManager op = new ContextMenuManager();

            op.action(Icons.ADD, IKey.raw("Add"), () -> math.operation = 0);
            op.action(Icons.REMOVE, IKey.raw("Sub"), () -> math.operation = 1);
            op.action(Icons.CLOSE, IKey.raw("Mul"), () -> math.operation = 2);
            op.action(Icons.MAXIMIZE, IKey.raw("Div"), () -> math.operation = 3);
            op.action(Icons.CLOSE, IKey.raw("Cross"), () -> math.operation = 4);
            op.action(Icons.DOWNLOAD, IKey.raw("Project"), () -> math.operation = 5);
            op.action(Icons.REFRESH, IKey.raw("Reflect"), () -> math.operation = 6);
            op.action(Icons.VISIBLE, IKey.raw("Dot"), () -> math.operation = 7);
            op.action(Icons.ALL_DIRECTIONS, IKey.raw("Distance"), () -> math.operation = 8);
            op.action(Icons.ALL_DIRECTIONS, IKey.raw("Length"), () -> math.operation = 9);
            op.action(Icons.MAXIMIZE, IKey.raw("Scale"), () -> math.operation = 10);
            op.action(Icons.ALL_DIRECTIONS, IKey.raw("Normalize"), () -> math.operation = 11);
            op.action(Icons.GEAR, IKey.raw("Abs"), () -> math.operation = 12);
            op.action(Icons.DOWNLOAD, IKey.raw("Min"), () -> math.operation = 13);
            op.action(Icons.UPLOAD, IKey.raw("Max"), () -> math.operation = 14);
            op.action(Icons.GEAR, IKey.raw("Floor"), () -> math.operation = 15);
            op.action(Icons.GEAR, IKey.raw("Ceil"), () -> math.operation = 16);
            op.action(Icons.GEAR, IKey.raw("Fract"), () -> math.operation = 17);
            op.action(Icons.REFRESH, IKey.raw("Modulo"), () -> math.operation = 18);
            op.action(Icons.GEAR, IKey.raw("Snap"), () -> math.operation = 19);
            op.action(Icons.GEAR, IKey.raw("Sin"), () -> math.operation = 20);
            op.action(Icons.GEAR, IKey.raw("Cos"), () -> math.operation = 21);
            op.action(Icons.GEAR, IKey.raw("Tan"), () -> math.operation = 22);

            menu.action(Icons.GEAR, IKey.raw("Operation"), () -> context.replaceContextMenu(op.create()));
        }
        else if (node instanceof ValueNode)
        {
            ValueNode valueNode = (ValueNode) node;
            menu.action(Icons.EDIT, IKey.raw("Edit Value"), () -> {
                UINumberOverlayPanel panel = new UINumberOverlayPanel(IKey.raw("Edit Value"), IKey.raw("Enter a new value"), (v) -> valueNode.value = v.floatValue());
                panel.value.setValue((double) valueNode.value);
                UIOverlay.addOverlay(context, panel);
            });
        }
        else if (node instanceof ColorNode)
        {
            ColorNode colorNode = (ColorNode) node;
            menu.action(Icons.MATERIAL, IKey.raw("Edit Color"), () -> {
                UIColorOverlayPanel panel = new UIColorOverlayPanel(IKey.raw("Edit Color"), (c) -> colorNode.color.set(c));
                panel.picker.editAlpha = true;
                panel.picker.setColor(colorNode.color.getARGBColor());
                UIOverlay.addOverlay(context, panel, 250, 160);
            });
        }
        else if (node instanceof CommentNode)
        {
            CommentNode commentNode = (CommentNode) node;
            menu.action(Icons.EDIT, IKey.raw("Edit Title"), () -> {
                UIPromptOverlayPanel panel = new UIPromptOverlayPanel(IKey.raw("Edit Title"), IKey.raw("Enter title"), (s) -> commentNode.title = s);
                panel.text.setText(commentNode.title);
                UIOverlay.addOverlay(context, panel);
            });
            menu.action(Icons.EDIT, IKey.raw("Edit Comment"), () -> {
                UITextareaOverlayPanel panel = new UITextareaOverlayPanel(IKey.raw("Edit Comment"), IKey.raw("Enter comment"), (s) -> commentNode.comment = s);
                panel.text.setText(commentNode.comment);
                UIOverlay.addOverlay(context, panel);
            });
        }
        else if (node instanceof IrisShaderNode)
        {
            IrisShaderNode irisNode = (IrisShaderNode) node;
            menu.action(Icons.EDIT, IKey.raw("Edit Uniform"), () -> {
                UIPromptOverlayPanel panel = new UIPromptOverlayPanel(IKey.raw("Edit Uniform"), IKey.raw("Enter uniform name"), (s) -> irisNode.uniform = s);
                panel.text.setText(irisNode.uniform);
                UIOverlay.addOverlay(context, panel);
            });
        }
        else if (node instanceof IrisAttributeNode)
        {
            IrisAttributeNode attrNode = (IrisAttributeNode) node;
            ContextMenuManager attrMenu = new ContextMenuManager();
            
            for (IrisAttributeNode.Attribute attr : IrisAttributeNode.Attribute.values())
            {
                attrMenu.action(Icons.GEAR, IKey.raw(attr.name()), () -> attrNode.attribute = attr);
            }
            
            menu.action(Icons.GEAR, IKey.raw("Attribute: " + attrNode.attribute.name()), () -> context.replaceContextMenu(attrMenu.create()));
        }
        
        menu.action(Icons.REMOVE, UIKeys.GENERAL_REMOVE, Colors.NEGATIVE, () -> this.removeNode(node));
        
        if (!this.selection.isEmpty())
        {
            menu.action(Icons.COPY, IKey.raw("Copy Nodes"), () -> this.copyNodes());
        }
        
        if (clipboard != null)
        {
            menu.action(Icons.PASTE, IKey.raw("Paste Nodes"), () -> this.pasteNodes(this.lastMouseX, this.lastMouseY));
        }
        
        context.replaceContextMenu(menu.create());
    }
    
    @Override
    protected boolean subMouseReleased(UIContext context)
    {
        if (this.draggingConnectionNode != -1)
        {
             if (this.graph != null)
             {
                 int mx = context.mouseX;
                 int my = context.mouseY;
                 
                 for (int i = this.graph.nodes.size() - 1; i >= 0; i--)
                 {
                    ShapeNode node = this.graph.nodes.get(i);
                    int nx = (int) (this.area.x + this.translateX + node.x * this.scale);
                    int ny = (int) (this.area.y + this.translateY + node.y * this.scale);
                    int w = (int) (120 * this.scale);
                    
                    if (!this.draggingConnectionInput)
                    {
                        // Connecting output to input
                        List<String> inputs = node.getInputs();
                        for (int j = 0; j < inputs.size(); j++)
                        {
                            int sx = nx;
                            int sy = ny + (int) ((30 + j * 20) * this.scale);
                            
                            if (Math.abs(mx - sx) < Math.max(10 * this.scale, 6F) && Math.abs(my - sy) < Math.max(10 * this.scale, 6F))
                            {
                                this.graph.connect(this.draggingConnectionNode, this.draggingConnectionIndex, node.id, j);
                                break;
                            }
                        }
                    }
                    else
                    {
                        // Connecting input to output
                        List<String> outputs = node.getOutputs();
                        for (int j = 0; j < outputs.size(); j++)
                        {
                            int sx = nx + w;
                            int sy = ny + (int) ((30 + j * 20) * this.scale);
                            
                            if (Math.abs(mx - sx) < Math.max(10 * this.scale, 6F) && Math.abs(my - sy) < Math.max(10 * this.scale, 6F))
                            {
                                this.graph.connect(node.id, j, this.draggingConnectionNode, this.draggingConnectionIndex);
                                break;
                            }
                        }
                    }
                 }
             }
             
             this.draggingConnectionNode = -1;
             this.draggingConnectionIndex = -1;
             this.draggingConnectionInput = false;
             return true;
        }
        
        if (this.selecting)
        {
            this.selectNodes(this.selectingX, this.selectingY, context.mouseX, context.mouseY, Window.isCtrlPressed());
            this.selecting = false;
            return true;
        }

        this.draggingNode = null;
        this.dragging = false;
        return super.subMouseReleased(context);
    }
    
    @Override
    protected boolean subKeyPressed(UIContext context)
    {
        if (this.area.isInside(context))
        {
            if (Window.isCtrlPressed())
            {
                if (context.getKeyCode() == GLFW.GLFW_KEY_C)
                {
                    this.copyNodes();
                    return true;
                }
                else if (context.getKeyCode() == GLFW.GLFW_KEY_V)
                {
                    this.pasteNodes(this.lastMouseX, this.lastMouseY);
                    return true;
                }
            }
            
            if (context.getKeyCode() == GLFW.GLFW_KEY_DELETE || context.getKeyCode() == GLFW.GLFW_KEY_BACKSPACE)
            {
                if (!this.selection.isEmpty() || !this.selectedConnections.isEmpty())
                {
                    this.removeSelection();
                    return true;
                }
            }
        }
        
        return super.subKeyPressed(context);
    }
    
    private void selectNodes(int x1, int y1, int x2, int y2, boolean add)
    {
        if (this.graph == null) return;
        
        int minX = Math.min(x1, x2);
        int minY = Math.min(y1, y2);
        int maxX = Math.max(x1, x2);
        int maxY = Math.max(y1, y2);
        
        if (!add)
        {
            this.selection.clear();
        }
        
        for (ShapeNode node : this.graph.nodes)
        {
            int nx = (int) (this.area.x + this.translateX + node.x * this.scale);
            int ny = (int) (this.area.y + this.translateY + node.y * this.scale);
            
            int max = Math.max(node.getInputs().size(), node.getOutputs().size());
            int w = (int) (120 * this.scale);
            int h = (int) ((35 + max * 20) * this.scale);
            
            if (nx + w > minX && nx < maxX && ny + h > minY && ny < maxY)
            {
                this.selection.add(node);
            }
        }
    }

    @Override
    protected boolean subMouseScrolled(UIContext context)
    {
        if (this.area.isInside(context))
        {
            float oldScale = this.scale;
            this.scale += Math.copySign(0.1F, context.mouseWheel);
            this.scale = Math.max(0.1F, Math.min(this.scale, 4F));
            
            float newScale = this.scale;
            float scaleFactor = newScale / oldScale;
            
            // Zoom towards mouse
            this.translateX = context.mouseX - (context.mouseX - this.translateX) * scaleFactor;
            this.translateY = context.mouseY - (context.mouseY - this.translateY) * scaleFactor;
            
            return true;
        }
        return super.subMouseScrolled(context);
    }

    @Override
    public void render(UIContext context)
    {
        if (this.dragging)
        {
            this.translateX += context.mouseX - this.lastMouseX;
            this.translateY += context.mouseY - this.lastMouseY;
            
            this.lastMouseX = context.mouseX;
            this.lastMouseY = context.mouseY;
        }
        
        if (this.draggingNode != null)
        {
            float dx = (context.mouseX - this.draggingMouseX) / this.scale;
            float dy = (context.mouseY - this.draggingMouseY) / this.scale;
            
            for (ShapeNode node : this.selection)
            {
                Vector2f pos = this.initialPositions.get(node);
                if (pos != null)
                {
                    node.x = pos.x + dx;
                    node.y = pos.y + dy;
                }
            }
        }
        
        if (!Window.isMouseButtonPressed(0) && !Window.isMouseButtonPressed(2))
        {
            this.dragging = false;
        }

        this.renderBackground(context);

        if (this.graph != null)
        {
             for (ShapeConnection c : this.graph.connections)
             {
                 drawConnection(context, c);
             }
             
             if (this.draggingConnectionNode != -1)
             {
                 drawDraggingConnection(context);
             }

             for (ShapeNode node : this.graph.nodes)
             {
                 drawNode(context, node);
             }
        }
        
        if (this.selecting)
        {
            context.batcher.normalizedBox(this.selectingX, this.selectingY, context.mouseX, context.mouseY, Colors.setA(Colors.ACTIVE, 0.25F));
        }

        super.render(context);
    }
    
    private void renderBackground(UIContext context)
    {
        context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A75 | 0x181818);
        
        context.batcher.clip(this.area, context);
        
        int size = 20;
        int color = Colors.A25 | 0xFFFFFF;
        
        float sc = this.scale;
        float ox = this.translateX % (size * sc);
        float oy = this.translateY % (size * sc);
        
        if (ox < 0) ox += size * sc;
        if (oy < 0) oy += size * sc;
        
        // Draw grid dots
        for (float x = ox; x < this.area.w; x += size * sc)
        {
            for (float y = oy; y < this.area.h; y += size * sc)
            {
                context.batcher.box(this.area.x + x - 1, this.area.y + y - 1, this.area.x + x + 1, this.area.y + y + 1, color);
            }
        }
        
        context.batcher.unclip(context);
    }

    private void drawNode(UIContext context, ShapeNode node)
    {
         int x = (int) (this.area.x + this.translateX + node.x * this.scale);
         int y = (int) (this.area.y + this.translateY + node.y * this.scale);
         
         if (node instanceof CommentNode)
         {
             CommentNode comment = (CommentNode) node;
             int w = (int) (comment.width * this.scale);
             int h = (int) (comment.height * this.scale);
             
             context.batcher.box(x, y, x + w, y + h, Colors.A50 | 0x000000);
             context.batcher.outline(x, y, x + w, y + h, Colors.A50 | 0xFFFFFF);
             
             context.batcher.text(comment.title, x + 5, y + 5);
             if (!comment.comment.isEmpty())
             {
                 context.batcher.text(comment.comment, x + 5, y + 20);
             }
             return;
         }
         
         List<String> inputs = node.getInputs();
         List<String> outputs = node.getOutputs();
         int max = Math.max(inputs.size(), outputs.size());
         
         int w = (int) (120 * this.scale);
         int h = (int) ((35 + max * 20) * this.scale);
         int headerH = (int) (20 * this.scale);
         
         // Shadow
         context.batcher.dropShadow(x, y, x + w, y + h, (int) (10 * this.scale), Colors.A50 | 0x000000, 0);
         
         if (this.selection.contains(node))
         {
             context.batcher.outline(x - 1, y - 1, x + w + 1, y + h + 1, Colors.WHITE);
             context.batcher.outline(x - 2, y - 2, x + w + 2, y + h + 2, Colors.WHITE);
         }
         
         // Header
         int headerColor = Colors.A100 | 0x444444;
         
         if (node instanceof OutputNode) headerColor = Colors.A100 | 0xFF5555;
         else if (node instanceof MathNode) headerColor = Colors.A100 | 0x5555FF;
         else if (node instanceof ValueNode || node instanceof TimeNode || node instanceof CoordinateNode || node instanceof ColorNode) headerColor = Colors.A100 | 0x55FF55;
         else if (node instanceof TriggerNode) headerColor = Colors.A100 | 0xFF55FF;
         else if (node instanceof NoiseNode || node instanceof VoronoiNode || node instanceof FlowNoiseNode) headerColor = Colors.A100 | 0xFFAA00;
         else if (node instanceof BumpNode) headerColor = Colors.A100 | 0xFF55AA;
         
         context.batcher.box(x, y, x + w, y + headerH, headerColor);
         context.batcher.outline(x, y, x + w, y + headerH, 0xFF222222);
         
         // Body
         context.batcher.box(x, y + headerH, x + w, y + h, Colors.A75 | 0x222222);
         context.batcher.outline(x, y + headerH, x + w, y + h, 0xFF000000);
         
         // Title
         String title = node.getType();
         
         if (node instanceof MathNode)
        {
            MathNode math = (MathNode) node;
            String[] ops = {"+", "-", "*", "/", "%", "min", "max", "^"};
            int op = math.operation;
            
            if (op == 8)
            {
                title += " (" + math.expression + ")";
            }
            else if (op >= 0 && op < ops.length)
            {
                title += " (" + ops[op] + ")";
            }
        }
         else if (node instanceof ValueNode)
        {
            title += ": " + ((ValueNode) node).value;
        }
        else if (node instanceof ColorNode)
        {
            title += ": " + Integer.toHexString(((ColorNode) node).color.getARGBColor()).toUpperCase();
        }
        else if (node instanceof NoiseNode)
        {
            title += " (" + ((NoiseNode) node).seed + ")";
        }
        else if (node instanceof FlowNoiseNode)
        {
            title += " (" + ((FlowNoiseNode) node).seed + ")";
        }
        else if (node instanceof VoronoiNode)
        {
            title += " (" + ((VoronoiNode) node).seed + ")";
        }
        else if (node instanceof TriggerNode)
        {
            int mode = ((TriggerNode) node).mode;
            String label = "";

            if (mode == 0) label = "Greater";
            else if (mode == 1) label = "Less";
            else if (mode == 2) label = "Equal";
            else if (mode == 3) label = "Not Equal";
            else if (mode == 4) label = "Pulse";

            title += " (" + label + ")";
        }
        
        context.batcher.text(title, x + 5, y + 6);
        
        if (node instanceof ColorNode)
        {
             int c = ((ColorNode) node).color.getARGBColor();
             context.batcher.box(x + 5, y + 25, x + w - 5, y + 45, c);
             context.batcher.outline(x + 5, y + 25, x + w - 5, y + 45, 0xFF000000);
        }
        
        float socketSize = Math.max(10 * this.scale, 6F);
         
         for (int i = 0; i < inputs.size(); i++)
         {
             int sy = y + (int) ((30 + i * 20) * this.scale);
             
             this.drawSocket(context, x, sy, Colors.WHITE);
             context.batcher.text(inputs.get(i), x + (int)(8 * this.scale), sy - 4);
         }
         
         for (int i = 0; i < outputs.size(); i++)
         {
             int sy = y + (int) ((30 + i * 20) * this.scale);
             
             this.drawSocket(context, x + w, sy, Colors.WHITE);
             String label = outputs.get(i);
             context.batcher.text(label, x + w - (int)(8 * this.scale) - context.batcher.getFont().getWidth(label), sy - 4);
         }
    }
    
    private void drawConnection(UIContext context, ShapeConnection c)
    {
        ShapeNode out = findNode(c.outputNodeId);
        ShapeNode in = findNode(c.inputNodeId);
        
        if (out != null && in != null)
        {
             int x1 = (int) (this.area.x + this.translateX + out.x * this.scale + 120 * this.scale);
             int y1 = (int) (this.area.y + this.translateY + out.y * this.scale + (30 + c.outputIndex * 20) * this.scale);
             
             int x2 = (int) (this.area.x + this.translateX + in.x * this.scale);
             int y2 = (int) (this.area.y + this.translateY + in.y * this.scale + (30 + c.inputIndex * 20) * this.scale);
             
             int color = this.selectedConnections.contains(c) ? Colors.A100 | Colors.ACTIVE : Colors.WHITE;
             
             drawBezier(context, x1, y1, x2, y2, color, 2F * this.scale);
        }
    }
    
    private void drawDraggingConnection(UIContext context)
    {
        ShapeNode node = findNode(this.draggingConnectionNode);
        if (node != null)
        {
             if (!this.draggingConnectionInput)
            {
                int x1 = (int) (this.area.x + this.translateX + node.x * this.scale + 120 * this.scale);
                int y1 = (int) (this.area.y + this.translateY + node.y * this.scale + (30 + this.draggingConnectionIndex * 20) * this.scale);
                
                drawBezier(context, x1, y1, context.mouseX, context.mouseY, Colors.WHITE, 2F * this.scale);
            }
            else
            {
                int x1 = (int) (this.area.x + this.translateX + node.x * this.scale);
                int y1 = (int) (this.area.y + this.translateY + node.y * this.scale + (30 + this.draggingConnectionIndex * 20) * this.scale);
                
                drawBezier(context, context.mouseX, context.mouseY, x1, y1, Colors.WHITE, 2F * this.scale);
            }
        }
    }

    private void drawSocket(UIContext context, int x, int y, int color)
    {
        context.batcher.flush();

        float radius = Math.max(5 * this.scale, 3F);
        int segments = 32;

        Matrix4f matrix4f = context.batcher.getContext().getMatrices().peek().getPositionMatrix();
        BufferBuilder builder = Tessellator.getInstance().getBuffer();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        // Border
        builder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix4f, x, y, 0F).color(0xFF000000).next();

        for (int i = 0; i <= segments; i++)
        {
            double a = i / (double) segments * Math.PI * 2;
            builder.vertex(matrix4f, (float) (x + Math.cos(a) * (radius + 1.5F)), (float) (y + Math.sin(a) * (radius + 1.5F)), 0F).color(0xFF000000).next();
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());

        // Fill
        builder.begin(VertexFormat.DrawMode.TRIANGLE_FAN, VertexFormats.POSITION_COLOR);
        builder.vertex(matrix4f, x, y, 0F).color(color).next();

        for (int i = 0; i <= segments; i++)
        {
            double a = i / (double) segments * Math.PI * 2;
            builder.vertex(matrix4f, (float) (x + Math.cos(a) * radius), (float) (y + Math.sin(a) * radius), 0F).color(color).next();
        }

        BufferRenderer.drawWithGlobalProgram(builder.end());
    }

    private void drawBezier(UIContext context, int x1, int y1, int x2, int y2, int color, float thickness)
    {
        int segments = 24;
        float dist = Math.max(Math.abs(x2 - x1) / 2F, 50F * this.scale);
        
        float px = x1;
        float py = y1;
        
        for (int i = 1; i <= segments; i++)
        {
            float t = i / (float) segments;
            float t1 = 1 - t;
            
            float c0 = t1 * t1 * t1;
            float c1 = 3 * t1 * t1 * t;
            float c2 = 3 * t1 * t * t;
            float c3 = t * t * t;
            
            float x = c0 * x1 + c1 * (x1 + dist) + c2 * (x2 - dist) + c3 * x2;
            float y = c0 * y1 + c1 * y1 + c2 * y2 + c3 * y2;
            
            context.batcher.line(px, py, x, y, thickness, color);
            
            px = x;
            py = y;
        }
    }
    
    private boolean isOverConnection(int mx, int my, ShapeConnection c)
    {
        ShapeNode out = findNode(c.outputNodeId);
        ShapeNode in = findNode(c.inputNodeId);
        
        if (out != null && in != null)
        {
             int x1 = (int) (this.area.x + this.translateX + out.x * this.scale + 120 * this.scale);
             int y1 = (int) (this.area.y + this.translateY + out.y * this.scale + (30 + c.outputIndex * 20) * this.scale);
             
             int x2 = (int) (this.area.x + this.translateX + in.x * this.scale);
             int y2 = (int) (this.area.y + this.translateY + in.y * this.scale + (30 + c.inputIndex * 20) * this.scale);
             
             return isOverBezier(mx, my, x1, y1, x2, y2);
        }
        
        return false;
    }

    private boolean isOverBezier(int mx, int my, int x1, int y1, int x2, int y2)
    {
        int segments = 24;
        float dist = Math.max(Math.abs(x2 - x1) / 2F, 50F * this.scale);
        
        float px = x1;
        float py = y1;
        
        float threshold = 5F * this.scale;
        
        for (int i = 1; i <= segments; i++)
        {
            float t = i / (float) segments;
            float t1 = 1 - t;
            
            float c0 = t1 * t1 * t1;
            float c1 = 3 * t1 * t1 * t;
            float c2 = 3 * t1 * t * t;
            float c3 = t * t * t;
            
            float x = c0 * x1 + c1 * (x1 + dist) + c2 * (x2 - dist) + c3 * x2;
            float y = c0 * y1 + c1 * y1 + c2 * y2 + c3 * y2;
            
            // Check distance to line segment (px, py) -> (x, y)
            if (distanceToSegment(mx, my, px, py, x, y) < threshold)
            {
                return true;
            }
            
            px = x;
            py = y;
        }
        
        return false;
    }
    
    private float distanceToSegment(float x, float y, float x1, float y1, float x2, float y2)
    {
        float A = x - x1;
        float B = y - y1;
        float C = x2 - x1;
        float D = y2 - y1;
        
        float dot = A * C + B * D;
        float len_sq = C * C + D * D;
        float param = -1;
        
        if (len_sq != 0)
        {
            param = dot / len_sq;
        }
        
        float xx, yy;
        
        if (param < 0)
        {
            xx = x1;
            yy = y1;
        }
        else if (param > 1)
        {
            xx = x2;
            yy = y2;
        }
        else
        {
            xx = x1 + param * C;
            yy = y1 + param * D;
        }
        
        float dx = x - xx;
        float dy = y - yy;
        
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
    
    private ShapeNode findNode(int id)
    {
        if (this.graph == null) return null;
        
        for (ShapeNode node : this.graph.nodes)
        {
            if (node.id == id) return node;
        }
        
        return null;
    }
}
