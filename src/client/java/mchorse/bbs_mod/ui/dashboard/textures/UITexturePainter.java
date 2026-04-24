package mchorse.bbs_mod.ui.dashboard.textures;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.graphics.texture.Texture;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.l10n.keys.IKey;
import mchorse.bbs_mod.resources.Link;
import mchorse.bbs_mod.ui.Keys;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.UIElement;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.forms.editors.utils.UIFormRenderer;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.colors.Color;
import mchorse.bbs_mod.utils.resources.Pixels;
import mchorse.bbs_mod.utils.colors.Colors;
import org.joml.Vector2i;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class UITexturePainter extends UIElement
{
    private static final int DIVIDER_BAR_HEIGHT = 30;

    public UITrackpad brightness;
    public UITrackpad brush;
    public UIElement dividerBar;

    public UIColor primary;
    public UIColor secondary;

    public UITextureEditor main;
    public UITextureEditor reference;
    public UIElement modelPreviewArea;
    public UIFormRenderer modelPreview;
    public UIIcon toolBrush;
    public UIIcon toolEraser;
    public UIIcon toolPick;
    public UIIcon toolFill;

    private Supplier<Form> formPreviewSupplier;
    private final Set<Link> touchedPreviewTextures = new HashSet<>();
    private UIPixelsEditor.Tool activeTool = UIPixelsEditor.Tool.BRUSH;

    public UITexturePainter(Consumer<Link> saveCallback)
    {
        this.brightness = new UITrackpad();
        this.brightness.limit(0, 1).setValue(0.7);
        this.brightness.tooltip(UIKeys.TEXTURES_VIEWER_BRIGHTNESS, Direction.TOP);
        this.brightness.w(100);
        this.brightness.forcedLabel(UIKeys.TEXTURES_VIEWER_BRIGHTNESS);

        this.brush = new UITrackpad((v) ->
        {
            int brushSize = Math.max(1, v.intValue());

            this.main.setBrushSize(brushSize);

            if (this.reference != null)
            {
                this.reference.setBrushSize(brushSize);
            }
        });
        this.brush.integer().limit(1, 32, true).setValue(1);
        this.brush.tooltip(UIKeys.TEXTURES_BRUSH_SIZE, Direction.TOP);
        this.brush.w(100);
        this.brush.forcedLabel(UIKeys.TEXTURES_BRUSH_SIZE);

        this.dividerBar = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                this.area.render(context.batcher, Colors.CONTROL_BAR);
                context.batcher.box(this.area.x, this.area.y, this.area.ex(), this.area.y + 1, Colors.A50);
                context.batcher.box(this.area.x, this.area.ey() - 1, this.area.ex(), this.area.ey(), Colors.A75);

                super.render(context);
            }
        };
        this.dividerBar.relative(this).xy(0, 0).w(1F).h(DIVIDER_BAR_HEIGHT).row(4).resize().padding(5);

        this.primary = new UIColor((c) -> {}).noLabel();
        this.primary.direction(Direction.RIGHT).w(20);
        this.secondary = new UIColor((c) -> {}).noLabel();
        this.secondary.direction(Direction.RIGHT).w(20);

        this.primary.setColor(0);
        this.secondary.setColor(Colors.WHITE);

        this.toolBrush = new UIIcon(Icons.SPRAY, (b) -> this.setActiveTool(UIPixelsEditor.Tool.BRUSH));
        this.toolEraser = new UIIcon(Icons.REMOVE, (b) -> this.setActiveTool(UIPixelsEditor.Tool.ERASER));
        this.toolPick = new UIIcon(Icons.PICKAXE, (b) -> this.setActiveTool(UIPixelsEditor.Tool.PICK));
        this.toolFill = new UIIcon(Icons.BUCKET, (b) -> this.setActiveTool(UIPixelsEditor.Tool.FILL));

        this.toolBrush.tooltip(UIKeys.GENERAL_EDIT, Direction.TOP);
        this.toolEraser.tooltip(UIKeys.TEXTURE_EDITOR_ERASE, Direction.TOP);
        this.toolPick.tooltip(UIKeys.TEXTURES_KEYS_PICK, Direction.TOP);
        this.toolFill.tooltip(UIKeys.TEXTURES_KEYS_FILL, Direction.TOP);

        this.main = new UITextureEditor().saveCallback(saveCallback);
        this.configureEditor(this.main);
        this.main.full(this);
        this.main.undo.removeFromParent();
        this.main.redo.removeFromParent();
        this.main.resize.removeFromParent();
        this.main.extract.removeFromParent();
        this.main.save.removeFromParent();

        this.dividerBar.add(
            this.toolBrush,
            this.toolEraser,
            this.toolPick,
            this.toolFill.marginRight(8),
            this.main.undo,
            this.main.redo,
            this.main.resize,
            this.main.extract,
            this.main.save.marginRight(8),
            this.primary,
            this.secondary.marginRight(8),
            this.brush,
            this.brightness
        );
        this.updateToolButtons();

        this.add(this.main, this.dividerBar);

        this.modelPreviewArea = new UIElement();
        this.modelPreview = new UIFormRenderer();
        this.modelPreview.grid = false;
        this.modelPreview.setDistance(14);
        this.modelPreview.setPosition(0F, 1F, 0F);
        this.modelPreview.setRotation(34F, 8F);
        this.modelPreview.relative(this.modelPreviewArea).full(this.modelPreviewArea);
        this.modelPreviewArea.add(this.modelPreview);
        this.modelPreviewArea.setVisible(false);
        this.add(this.modelPreviewArea);

        IKey category = UIKeys.TEXTURES_KEYS_CATEGORY;

        this.keys().register(Keys.PIXEL_SWAP, this::swapColors).inside().category(category);
        this.keys().register(Keys.PIXEL_PICK, this::pickColor).inside().category(category);
        this.keys().register(Keys.PIXEL_FILL, this::fillColor).inside().category(category);
    }

    public UITexturePainter withFormPreview(Supplier<Form> supplier)
    {
        this.formPreviewSupplier = supplier;
        this.modelPreviewArea.setVisible(supplier != null);
        this.refreshModelPreview();
        this.updateEditorsLayout();
        this.resize();

        return this;
    }

    private void swapColors()
    {
        int swap = this.primary.picker.color.getRGBColor();

        this.primary.setColor(this.secondary.picker.color.getRGBColor());
        this.secondary.setColor(swap);
    }

    private UITextureEditor getHoverEditor(UIContext context)
    {
        return this.main.area.isInside(context) ? this.main : (this.reference != null && this.reference.area.isInside(context) ? this.reference : null);
    }

    private void pickColor()
    {
        UIContext context = this.getContext();
        UITextureEditor editor = this.getHoverEditor(context);

        if (editor != null)
        {
            Vector2i pixel = editor.getHoverPixel(context.mouseX, context.mouseY);
            Color color = editor.getPixels().getColor(pixel.x, pixel.y);

            if (color != null)
            {
                this.primary.setColor(color.getRGBColor());
            }
        }
    }

    private void fillColor()
    {
        UIContext context = this.getContext();
        UITextureEditor editor = this.getHoverEditor(context);

        if (editor != null)
        {
            Vector2i pixel = editor.getHoverPixel(context.mouseX, context.mouseY);

            editor.fillColor(pixel, this.primary.picker.color, Window.isShiftPressed());
        }
    }

    private void configureEditor(UITextureEditor editor)
    {
        editor
            .colorSupplier(() -> this.primary.picker.color)
            .backgroundSupplier(() -> (float) this.brightness.getValue())
            .onPickColor((color) -> this.primary.setColor(color.getRGBColor()))
            .onFillColor((pixel, replace) -> editor.fillColor(pixel, this.primary.picker.color, replace))
            .setTool(this.activeTool)
            .useExternalToolbar();
        editor.setBrushSize((int) this.brush.getValue());
    }

    private void setActiveTool(UIPixelsEditor.Tool tool)
    {
        this.activeTool = tool == null ? UIPixelsEditor.Tool.BRUSH : tool;

        this.main.setTool(this.activeTool);

        if (this.reference != null)
        {
            this.reference.setTool(this.activeTool);
        }

        this.updateToolButtons();
    }

    private void updateToolButtons()
    {
        this.toolBrush.active(this.activeTool == UIPixelsEditor.Tool.BRUSH);
        this.toolEraser.active(this.activeTool == UIPixelsEditor.Tool.ERASER);
        this.toolPick.active(this.activeTool == UIPixelsEditor.Tool.PICK);
        this.toolFill.active(this.activeTool == UIPixelsEditor.Tool.FILL);
    }

    public void fillTexture(Link current)
    {
        this.main.fillTexture(current);
        this.main.setEditing(true);
        this.refreshModelPreview();
    }

    private void refreshModelPreview()
    {
        if (this.formPreviewSupplier == null)
        {
            this.modelPreview.form = null;

            return;
        }

        Form source = this.formPreviewSupplier.get();
        this.modelPreview.form = source == null ? null : FormUtils.copy(source);
    }

    private void updateEditorsLayout()
    {
        if (this.modelPreviewArea.isVisible())
        {
            this.main.relative(this).xy(0, DIVIDER_BAR_HEIGHT + 2).w(0.5F, -4).h(1F, -(DIVIDER_BAR_HEIGHT + 2));

            if (this.reference != null)
            {
                this.reference.setVisible(false);
            }

            this.modelPreviewArea.relative(this).x(0.5F, 4).y(DIVIDER_BAR_HEIGHT + 8).w(0.5F, -10).h(1F, -(DIVIDER_BAR_HEIGHT + 18));

            return;
        }

        if (this.reference == null)
        {
            this.main.relative(this).xy(0, DIVIDER_BAR_HEIGHT + 2).w(1F).h(1F, -(DIVIDER_BAR_HEIGHT + 2));
        }
        else
        {
            this.main.relative(this).xy(0, DIVIDER_BAR_HEIGHT + 2).w(0.5F).h(1F, -(DIVIDER_BAR_HEIGHT + 2));
            this.reference.relative(this).xy(0.5F, DIVIDER_BAR_HEIGHT + 2).w(0.5F).h(1F, -(DIVIDER_BAR_HEIGHT + 2));
            this.reference.setVisible(true);
        }
    }

    private void updateLiveModelPreviewTexture()
    {
        if (!this.modelPreviewArea.isVisible() || this.formPreviewSupplier == null)
        {
            return;
        }

        Link textureLink = this.main.getTexture();
        Pixels pixels = this.main.getPixels();

        if (textureLink == null || pixels == null)
        {
            return;
        }

        Texture texture = BBSModClient.getTextures().getTexture(textureLink);

        if (texture == null)
        {
            return;
        }

        pixels.rewindBuffer();
        texture.bind();
        texture.updateTexture(pixels);
        this.touchedPreviewTextures.add(textureLink);
    }

    public void discardPreviewTextureChanges()
    {
        for (Link link : this.touchedPreviewTextures)
        {
            BBSModClient.getTextures().delete(link);
        }

        this.touchedPreviewTextures.clear();
    }

    @Override
    public void render(UIContext context)
    {
        this.updateEditorsLayout();
        this.updateLiveModelPreviewTexture();

        if (this.modelPreviewArea.isVisible())
        {
            this.modelPreviewArea.area.render(context.batcher, Colors.A25);
            context.batcher.outline(this.modelPreviewArea.area.x, this.modelPreviewArea.area.y, this.modelPreviewArea.area.ex(), this.modelPreviewArea.area.ey(), Colors.A50);
        }

        super.render(context);

        UITextureEditor editor = this.getHoverEditor(context);

        if (editor != null)
        {
            Vector2i pixel = editor.getHoverPixel(context.mouseX, context.mouseY);
            Color color = editor.getPixels().getColor(pixel.x, pixel.y);

            int r = 0;
            int g = 0;
            int b = 0;
            int a = 0;

            if (color != null)
            {
                r = (int) Math.floor(color.r * 255);
                g = (int) Math.floor(color.g * 255);
                b = (int) Math.floor(color.b * 255);
                a = (int) Math.floor(color.a * 255);
            }

            String[] information = {
                editor.getPixels().width + "x" + editor.getPixels().height + " (" + pixel.x + ", " + pixel.y + ")",
                "\u00A7cR\u00A7aG\u00A79B\u00A7rA (" + r + ", " + g + ", " + b + ", " + a + ")",
                "Brush " + editor.getBrushSize() + "x" + editor.getBrushSize(),
            };

            int x = this.area.x + 10;
            int y = this.area.ey() - context.batcher.getFont().getHeight() - 10 - (information.length - 1)* 14;

            for (String line : information)
            {
                context.batcher.textCard(line, x, y);

                y += 14;
            }
        }
    }
}
