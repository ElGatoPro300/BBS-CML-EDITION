package mchorse.bbs_mod.ui.dashboard.textures;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.BBSSettings;
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
import mchorse.bbs_mod.ui.framework.elements.buttons.UIButton;
import mchorse.bbs_mod.ui.framework.elements.buttons.UIIcon;
import mchorse.bbs_mod.ui.framework.elements.input.UIColor;
import mchorse.bbs_mod.ui.forms.editors.utils.UIFormRenderer;
import mchorse.bbs_mod.ui.framework.elements.input.UITrackpad;
import mchorse.bbs_mod.ui.utils.UI;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.Direction;
import mchorse.bbs_mod.utils.StringUtils;
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
    private static final int SIDE_PANEL_WIDTH = 186;

    public UITrackpad brightness;
    public UITrackpad brush;
    public UIElement headerToolbar;
    public UIElement sidePanel;
    public UIElement colorTabContent;
    public UIElement paletteTabContent;
    public UIElement mediaTabContent;
    public UIButton tabColor;
    public UIButton tabPalette;
    public UIButton tabImages;
    public UIButton tabLayers;
    public UIButton primarySlot;
    public UIButton secondarySlot;
    public UIElement layerRow;
    public UIElement imageRow;

    public UIColor primary;
    public UIColor secondary;
    public UITextureInlineColorPicker fixedColorPicker;

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
    private boolean editingPrimary = true;
    private boolean topTabColor = true;
    private boolean bottomTabLayers = true;

    public UITexturePainter(Consumer<Link> saveCallback)
    {
        this.brightness = new UITrackpad();
        this.brightness.limit(0, 1).setValue(0.7);
        this.brightness.tooltip(UIKeys.TEXTURES_VIEWER_BRIGHTNESS, Direction.TOP);
        this.brightness.w(100);

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

        this.primary = new UIColor((c) -> {}).noLabel();
        this.primary.direction(Direction.BOTTOM).h(20);
        this.secondary = new UIColor((c) -> {}).noLabel();
        this.secondary.direction(Direction.BOTTOM).wh(20, 20);

        this.primary.setColor(Colors.WHITE);
        this.secondary.setColor(0);

        this.toolBrush = new UIIcon(Icons.BRUSH, (b) -> this.setActiveTool(UIPixelsEditor.Tool.BRUSH))
        {
            @Override
            protected void renderSkin(UIContext context)
            {
                super.renderSkin(context);

                if (this.isActive())
                {
                    context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xff000000 | BBSSettings.primaryColor.get());
                }
            }
        };
        this.toolEraser = new UIIcon(Icons.ERASER, (b) -> this.setActiveTool(UIPixelsEditor.Tool.ERASER))
        {
            @Override
            protected void renderSkin(UIContext context)
            {
                super.renderSkin(context);

                if (this.isActive())
                {
                    context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xff000000 | BBSSettings.primaryColor.get());
                }
            }
        };
        this.toolPick = new UIIcon(Icons.DROPPER, (b) -> this.setActiveTool(UIPixelsEditor.Tool.PICK))
        {
            @Override
            protected void renderSkin(UIContext context)
            {
                super.renderSkin(context);

                if (this.isActive())
                {
                    context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xff000000 | BBSSettings.primaryColor.get());
                }
            }
        };
        this.toolFill = new UIIcon(Icons.DROP, (b) -> this.setActiveTool(UIPixelsEditor.Tool.FILL))
        {
            @Override
            protected void renderSkin(UIContext context)
            {
                super.renderSkin(context);

                if (this.isActive())
                {
                    context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), 0xff000000 | BBSSettings.primaryColor.get());
                }
            }
        };

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

        this.headerToolbar = UI.row(
            0,
            this.toolBrush,
            this.toolEraser,
            this.toolPick,
            this.toolFill.marginRight(8),
            this.main.undo,
            this.main.redo,
            this.main.resize,
            this.main.extract,
            this.main.save
        );
        this.headerToolbar.row().preferred(0);
        this.updateToolButtons();

        this.add(this.main);
        this.setupSidePanel();

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

    private void setupSidePanel()
    {
        this.sidePanel = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                this.area.render(context.batcher, Colors.A25);
                context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A50);
                super.render(context);
            }
        };
        this.sidePanel.relative(this).x(1F, -SIDE_PANEL_WIDTH).y(0).w(SIDE_PANEL_WIDTH).h(1F);

        this.fixedColorPicker = new UITextureInlineColorPicker((color) ->
        {
            if (this.editingPrimary)
            {
                this.primary.setColor(color);
            }
            else
            {
                this.secondary.setColor(color);
            }

            this.updateColorSlots();
        });
        this.fixedColorPicker.setup(0, 0);
        this.fixedColorPicker.relative(this.sidePanel).xy(8, 34).w(1F, -16);
        this.fixedColorPicker.h(164);

        this.tabColor = new UIButton(IKey.constant("COLOR"), (b) -> this.setTopTab(true));
        this.tabPalette = new UIButton(IKey.constant("PALETA"), (b) -> this.setTopTab(false));
        this.tabColor.relative(this.sidePanel).xy(8, 8).w(0.5F, -10).h(20);
        this.tabPalette.relative(this.sidePanel).x(0.5F, 2).y(8).w(0.5F, -10).h(20);

        this.colorTabContent = new UIElement();
        this.colorTabContent.relative(this.sidePanel).xy(8, 34).w(1F, -16).h(168);
        this.fixedColorPicker.relative(this.colorTabContent).x(0).y(0).w(1F).h(164);

        this.primarySlot = new UIButton(IKey.EMPTY, (b) -> this.setEditingPrimary(true))
        {
            @Override
            protected void renderSkin(UIContext context)
            {
                super.renderSkin(context);

                if (UITexturePainter.this.editingPrimary)
                {
                    int outline = 0xff000000 | BBSSettings.primaryColor.get();
                    context.batcher.outline(this.area.x - 1, this.area.y - 1, this.area.ex() + 1, this.area.ey() + 1, outline);
                }
            }
        };
        this.primarySlot.wh(12, 12).tooltip(IKey.constant("Color primario"), Direction.TOP);
        this.primarySlot.relative(this.colorTabContent).xy(6, 6);

        this.secondarySlot = new UIButton(IKey.EMPTY, (b) -> this.setEditingPrimary(false))
        {
            @Override
            protected void renderSkin(UIContext context)
            {
                super.renderSkin(context);

                if (!UITexturePainter.this.editingPrimary)
                {
                    int outline = 0xff000000 | BBSSettings.primaryColor.get();
                    context.batcher.outline(this.area.x - 1, this.area.y - 1, this.area.ex() + 1, this.area.ey() + 1, outline);
                }
            }
        };
        this.secondarySlot.wh(12, 12).tooltip(IKey.constant("Color secundario"), Direction.TOP);
        this.secondarySlot.relative(this.colorTabContent).xy(12, 12);

        this.colorTabContent.add(this.fixedColorPicker, this.secondarySlot, this.primarySlot);

        this.paletteTabContent = new UIElement();
        this.paletteTabContent.relative(this.sidePanel).xy(8, 34).w(1F, -16).h(56);

        UIElement paletteRowOne = new UIElement();
        UIElement paletteRowTwo = new UIElement();
        paletteRowOne.relative(this.paletteTabContent).xy(0, 0).w(1F).h(18).row(2).resize();
        paletteRowTwo.relative(this.paletteTabContent).xy(0, 20).w(1F).h(18).row(2).resize();

        int[] swatches = new int[] {
            0x000000, 0xffffff, 0x8f3f20, 0xd87f33, 0xff0000, 0xff55ff,
            0x00aa00, 0x55ffff, 0x3c44aa, 0x8932b8, 0xa0a0a0, 0x5a5a5a
        };

        for (int i = 0; i < swatches.length; i++)
        {
            final int color = swatches[i];
            UIButton swatch = new UIButton(IKey.EMPTY, (b) ->
            {
                if (this.editingPrimary)
                {
                    this.primary.setColor(color);
                }
                else
                {
                    this.secondary.setColor(color);
                }

                this.fixedColorPicker.setColor(this.getActiveColor());
                this.updateColorSlots();
            });

            swatch.color(color).background(true).wh(18, 18).tooltip(IKey.constant(String.format("#%06X", color)), Direction.TOP);

            if (i < 6)
            {
                paletteRowOne.add(swatch);
            }
            else
            {
                paletteRowTwo.add(swatch);
            }
        }

        this.paletteTabContent.add(paletteRowOne, paletteRowTwo);

        this.brush.relative(this.sidePanel).xy(8, 206).w(1F, -16);
        this.brightness.relative(this.sidePanel).xy(8, 230).w(1F, -16);

        this.tabImages = new UIButton(IKey.constant("IMAGENES"), (b) -> this.setBottomTab(false));
        this.tabLayers = new UIButton(IKey.constant("CAPAS"), (b) -> this.setBottomTab(true));
        this.tabImages.relative(this.sidePanel).x(8).y(1F, -54).w(0.5F, -10).h(18);
        this.tabLayers.relative(this.sidePanel).x(0.5F, 2).y(1F, -54).w(0.5F, -10).h(18);

        this.mediaTabContent = new UIElement();
        this.mediaTabContent.relative(this.sidePanel).x(8).y(1F, -34).w(1F, -16).h(24);

        this.imageRow = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                this.area.render(context.batcher, Colors.A50);
                context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A75);
                context.batcher.icon(Icons.IMAGE, Colors.WHITE, this.area.x + 8, this.area.my(), 0F, 0.5F);
                context.batcher.text("imagen", this.area.x + 24, this.area.my(11), 0xdadada, false);

                super.render(context);
            }
        };
        this.imageRow.relative(this.mediaTabContent).xy(0, 0).w(1F).h(22);

        this.layerRow = new UIElement()
        {
            @Override
            public void render(UIContext context)
            {
                this.area.render(context.batcher, Colors.A50);
                context.batcher.outline(this.area.x, this.area.y, this.area.ex(), this.area.ey(), Colors.A75);
                context.batcher.icon(Icons.IMAGE, Colors.WHITE, this.area.x + 8, this.area.my(), 0F, 0.5F);

                String label = "layer";
                Link texture = UITexturePainter.this.main.getTexture();

                if (texture != null)
                {
                    String fileName = StringUtils.fileName(texture.path);

                    if (fileName != null && !fileName.isEmpty())
                    {
                        label = fileName;
                    }
                }

                context.batcher.text(label, this.area.x + 24, this.area.my(11), 0xdadada, false);

                super.render(context);
            }
        };
        this.layerRow.relative(this.mediaTabContent).xy(0, 0).w(1F).h(22);

        this.mediaTabContent.add(this.imageRow, this.layerRow);
        this.sidePanel.add(
            this.tabColor,
            this.tabPalette,
            this.colorTabContent,
            this.paletteTabContent,
            this.brush,
            this.brightness,
            this.tabImages,
            this.tabLayers,
            this.mediaTabContent
        );
        this.fixedColorPicker.setColor(this.primary.picker.color.getRGBColor());
        this.setTopTab(true);
        this.setBottomTab(true);
        this.updateColorSlots();
        this.add(this.sidePanel);
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
        this.fixedColorPicker.setColor(this.getActiveColor());
        this.updateColorSlots();
    }

    private int getActiveColor()
    {
        return this.editingPrimary ? this.primary.picker.color.getRGBColor() : this.secondary.picker.color.getRGBColor();
    }

    private Color getActiveBrushColor()
    {
        return this.editingPrimary ? this.primary.picker.color : this.secondary.picker.color;
    }

    private void setEditingPrimary(boolean editingPrimary)
    {
        this.editingPrimary = editingPrimary;
        this.fixedColorPicker.setColor(this.getActiveColor());
        this.updateColorSlots();
    }

    private void updateColorSlots()
    {
        this.primarySlot.color(this.primary.picker.color.getRGBColor()).background(true);
        this.secondarySlot.color(this.secondary.picker.color.getRGBColor()).background(true);
    }

    private void setTopTab(boolean color)
    {
        this.topTabColor = color;
        this.colorTabContent.setVisible(color);
        this.paletteTabContent.setVisible(!color);

        this.tabColor.background(color).textColor(color ? Colors.WHITE : 0xb0b0b0, false);
        this.tabPalette.background(!color).textColor(color ? 0xb0b0b0 : Colors.WHITE, false);
    }

    private void setBottomTab(boolean layers)
    {
        this.bottomTabLayers = layers;
        this.layerRow.setVisible(layers);
        this.imageRow.setVisible(!layers);

        this.tabLayers.background(layers).textColor(layers ? Colors.WHITE : 0xb0b0b0, false);
        this.tabImages.background(!layers).textColor(layers ? 0xb0b0b0 : Colors.WHITE, false);
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
                if (this.editingPrimary)
                {
                    this.primary.setColor(color.getRGBColor());
                }
                else
                {
                    this.secondary.setColor(color.getRGBColor());
                }

                this.fixedColorPicker.setColor(this.getActiveColor());
                this.updateColorSlots();
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

            editor.fillColor(pixel, this.getActiveBrushColor(), Window.isShiftPressed());
        }
    }

    private void configureEditor(UITextureEditor editor)
    {
        editor
            .colorSupplier(this::getActiveBrushColor)
            .backgroundSupplier(() -> (float) this.brightness.getValue())
            .onPickColor((color) ->
            {
                if (this.editingPrimary)
                {
                    this.primary.setColor(color.getRGBColor());
                }
                else
                {
                    this.secondary.setColor(color.getRGBColor());
                }

                this.fixedColorPicker.setColor(this.getActiveColor());
                this.updateColorSlots();
            })
            .onFillColor((pixel, replace) -> editor.fillColor(pixel, this.getActiveBrushColor(), replace))
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
        boolean sidePanelVisible = !this.modelPreviewArea.isVisible() && this.reference == null;
        this.sidePanel.setVisible(sidePanelVisible);

        if (this.modelPreviewArea.isVisible())
        {
            this.main.relative(this).xy(0, 0).w(0.5F, -4).h(1F);

            if (this.reference != null)
            {
                this.reference.setVisible(false);
            }

            this.modelPreviewArea.relative(this).x(0.5F, 4).y(6).w(0.5F, -10).h(1F, -12);

            return;
        }

        if (this.reference == null)
        {
            if (sidePanelVisible)
            {
                this.main.relative(this).xy(0, 0).w(1F, -(SIDE_PANEL_WIDTH + 4)).h(1F);
                this.sidePanel.relative(this).x(1F, -SIDE_PANEL_WIDTH).y(0).w(SIDE_PANEL_WIDTH).h(1F);
            }
            else
            {
                this.main.relative(this).xy(0, 0).w(1F).h(1F);
            }
        }
        else
        {
            this.main.relative(this).xy(0, 0).w(0.5F).h(1F);
            this.reference.relative(this).xy(0.5F, 0).w(0.5F).h(1F);
            this.reference.setVisible(true);
        }
    }

    public UIElement getHeaderToolbar()
    {
        return this.headerToolbar;
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
