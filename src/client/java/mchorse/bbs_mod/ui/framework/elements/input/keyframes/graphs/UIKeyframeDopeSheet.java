package mchorse.bbs_mod.ui.framework.elements.input.keyframes.graphs;

import com.mojang.blaze3d.systems.RenderSystem;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.camera.utils.TimeUtils;
import mchorse.bbs_mod.data.types.MapType;
import mchorse.bbs_mod.graphics.window.Window;
import mchorse.bbs_mod.ui.forms.editors.utils.UIStructureOverlayPanel;
import mchorse.bbs_mod.ui.framework.UIContext;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframeSheet;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.UIKeyframes;
import mchorse.bbs_mod.ui.framework.elements.overlay.UIOverlay;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes.IKeyframeShapeRenderer;
import mchorse.bbs_mod.ui.framework.elements.input.keyframes.shapes.KeyframeShapeRenderers;
import mchorse.bbs_mod.ui.framework.elements.utils.FontRenderer;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.utils.Area;
import mchorse.bbs_mod.ui.utils.Scale;
import mchorse.bbs_mod.ui.utils.Scroll;
import mchorse.bbs_mod.ui.utils.ScrollDirection;
import mchorse.bbs_mod.ui.utils.icons.Icon;
import mchorse.bbs_mod.ui.utils.icons.Icons;
import mchorse.bbs_mod.utils.CollectionUtils;
import mchorse.bbs_mod.utils.MathUtils;
import mchorse.bbs_mod.utils.Pair;
import mchorse.bbs_mod.utils.colors.Colors;
import mchorse.bbs_mod.utils.keyframes.Keyframe;
import mchorse.bbs_mod.utils.keyframes.KeyframeShape;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class UIKeyframeDopeSheet implements IUIKeyframeGraph
{
    private static final int LEVEL_INDENT = 8;
    private static final int TRACK_LINE_HALF_HEIGHT = 1;

    private UIKeyframes keyframes;

    private List<UIKeyframeSheet> sheets = new ArrayList<>();
    private UIKeyframeSheet lastSheet;

    private Scroll dopeSheet;
    private Scroll sidebarScrollbar;
    private double trackHeight;
    private int sidebarScroll;
    private int sidebarScrollMax;
    private boolean sidebarDragging;
    private float sidebarDragRatio;

    public static IKeyframeShapeRenderer renderShape(Keyframe frame, UIContext context, BufferBuilder builder, Matrix4f matrix, int x, int y, int offset, int c)
    {
        KeyframeShape keyframeShape = frame.getShape();
        IKeyframeShapeRenderer shape = KeyframeShapeRenderers.SHAPES.get(keyframeShape);

        shape.renderKeyframe(context, builder, matrix, x, y, offset, c);

        return shape;
    }

    public UIKeyframeDopeSheet(UIKeyframes keyframes)
    {
        this.keyframes = keyframes;
        this.dopeSheet = new Scroll(this.keyframes.area);
        this.sidebarScrollbar = new Scroll(new Area(), 1, ScrollDirection.HORIZONTAL);

        this.setTrackHeight(16);
    }

    public double getTrackHeight()
    {
        return this.trackHeight;
    }

    public void setTrackHeight(double height)
    {
        this.trackHeight = MathUtils.clamp(height, 8D, 100D);
        this.dopeSheet.scrollSpeed = (int) this.trackHeight * 2;
        this.dopeSheet.scrollSize = (int) this.trackHeight * this.sheets.size() + TOP_MARGIN;

        this.dopeSheet.clamp();
    }

    private String getDisplayTitle(String title)
    {
        int limit = BBSSettings.editorReplayEditorTitleLimit.get();

        if (limit <= 0)
        {
            return title;
        }

        return title.length() > limit ? title.substring(0, limit) + "..." : title;
    }

    private String getSidebarTitle(String title)
    {
        return this.sidebarScrollMax > 0 ? title : this.getDisplayTitle(title);
    }

    private String getEffectiveSidebarTitle(UIKeyframeSheet sheet)
    {
        if (sheet == null)
        {
            return "";
        }

        if (sheet.groupHeader)
        {
            return sheet.title.get();
        }

        return sheet.title.get();
    }

    /* Graphing */

    public Scroll getYAxis()
    {
        return this.dopeSheet;
    }

    public int getDopeSheetY()
    {
        return this.keyframes.area.y + TOP_MARGIN - (int) this.dopeSheet.getScroll();
    }

    public int getDopeSheetY(int sheet)
    {
        return this.getDopeSheetY() + sheet * (int) this.trackHeight;
    }

    public int getDopeSheetY(UIKeyframeSheet sheet)
    {
        return this.getDopeSheetY(this.sheets.indexOf(sheet));
    }

    /**
     * Whether given mouse coordinates are near the given point?
     */
    public static boolean isNear(double x, double y, int mouseX, int mouseY, boolean checkOnlyX)
    {
        if (checkOnlyX)
        {
            return Math.pow(mouseX - x, 2) < 25D;
        }

        return Math.pow(mouseX - x, 2) + Math.pow(mouseY - y, 2) < 25D;
    }

    /* Sheet management */

    @Override
    public void resetView()
    {
        this.keyframes.resetViewX();
    }

    @Override
    public UIKeyframeSheet getLastSheet()
    {
        return this.lastSheet == null ? CollectionUtils.getSafe(this.sheets, 0) : this.lastSheet;
    }

    @Override
    public List<UIKeyframeSheet> getSheets()
    {
        return this.sheets;
    }

    public void removeAllSheets()
    {
        this.sheets.clear();
    }

    public void addSheet(UIKeyframeSheet sheet)
    {
        this.sheets.add(sheet);
    }

    /* Selection */

    @Override
    public void selectByX(int mouseX)
    {
        for (int i = 0; i < sheets.size(); i++)
        {
            UIKeyframeSheet sheet = sheets.get(i);

            if (sheet.groupHeader)
            {
                continue;
            }

            List keyframes = sheet.channel.getKeyframes();

            for (int j = 0; j < keyframes.size(); j++)
            {
                Keyframe keyframe = (Keyframe) keyframes.get(j);
                int x = this.keyframes.toGraphX(keyframe.getTick());
                int y = this.getDopeSheetY(i) + (int) this.trackHeight / 2;

                if (this.isNear(x, y, mouseX, 0, true))
                {
                    sheet.selection.add(j);
                }
            }
        }

        this.pickSelected();
    }

    @Override
    public void selectInArea(Area area)
    {
        List<UIKeyframeSheet> sheets = this.getSheets();

        for (int i = 0; i < sheets.size(); i++)
        {
            UIKeyframeSheet sheet = sheets.get(i);
            List keyframes = sheet.channel.getKeyframes();

            for (int j = 0; j < keyframes.size(); j++)
            {
                Keyframe keyframe = (Keyframe) keyframes.get(j);
                int x = this.keyframes.toGraphX(keyframe.getTick());
                int y = this.getDopeSheetY(i) + (int) this.trackHeight / 2;

                if (area.isInside(x, y))
                {
                    sheet.selection.add(j);
                }
            }
        }

        this.pickSelected();
    }

    @Override
    public UIKeyframeSheet getSheet(int mouseY)
    {
        int dopeSheetY = this.getDopeSheetY();
        int index = (mouseY - dopeSheetY) / (int) this.trackHeight;

        return CollectionUtils.getSafe(this.sheets, index);
    }

    @Override
    public boolean addKeyframe(int mouseX, int mouseY)
    {
        float tick = (float) this.keyframes.fromGraphX(mouseX);
        UIKeyframeSheet sheet = this.getSheet(mouseY);

        if (!Window.isShiftPressed())
        {
            tick = Math.round(tick);
        }

        if (sheet != null && !sheet.groupHeader)
        {
            this.addKeyframe(sheet, tick, null);
        }

        return sheet != null && !sheet.groupHeader;
    }

    @Override
    public Pair<Keyframe, KeyframeType> findKeyframe(int mouseX, int mouseY)
    {
        UIKeyframeSheet sheet = this.getSheet(mouseY);

        if (sheet == null || sheet.groupHeader)
        {
            return null;
        }

        List keyframes = sheet.channel.getKeyframes();
        int i = this.sheets.indexOf(sheet);

        for (int j = 0; j < keyframes.size(); j++)
        {
            Keyframe keyframe = (Keyframe) keyframes.get(j);
            int x = this.keyframes.toGraphX(keyframe.getTick());
            int y = this.getDopeSheetY(i) + (int) this.trackHeight / 2;

            if (this.isNear(x, y, mouseX, mouseY, false))
            {
                return new Pair<>(keyframe, KeyframeType.REGULAR);
            }
        }

        return null;
    }

    @Override
    public void onCallback(Keyframe keyframe)
    {
        UIKeyframeSheet sheet = this.getSheet(keyframe);

        if (sheet != null)
        {
            this.lastSheet = sheet;
        }
    }

    @Override
    public void pickKeyframe(Keyframe keyframe)
    {
        this.keyframes.pickKeyframe(keyframe);
    }

    @Override
    public void selectKeyframe(Keyframe keyframe)
    {
        this.clearSelection();

        UIKeyframeSheet sheet = this.getSheet(keyframe);

        if (sheet != null)
        {
            sheet.selection.add(keyframe);
            this.pickKeyframe(keyframe);

            double x = keyframe.getTick();
            int y = (int) (this.sheets.indexOf(sheet) * this.trackHeight) + TOP_MARGIN;

            this.keyframes.getXAxis().shiftIntoMiddle(x);
            this.dopeSheet.scrollTo((int) (y - (this.dopeSheet.area.h - this.trackHeight) / 2));
        }
    }

    @Override
    public void resize()
    {
        this.dopeSheet.clamp();
    }

    /* Input handling */

    @Override
    public boolean mouseClicked(UIContext context)
    {
        if (this.handleSidebarScrollbarClick(context))
        {
            return true;
        }

        if (context.mouseButton == 0 && this.keyframes.area.isInside(context))
        {
            UIKeyframeSheet sheet = this.getSheet(context.mouseY);

            if (sheet != null)
            {
                FontRenderer font = context.batcher.getFont();
                String title = this.getEffectiveSidebarTitle(sheet);
                String displayTitle = this.getSidebarTitle(title);
                Icon arrow = sheet.groupHeader
                    ? (sheet.groupKey != null && (sheet.groupKey.endsWith("__world__") || sheet.groupKey.endsWith("__model__")) ? (sheet.groupExpanded ? Icons.UNCOLLAPSED : Icons.COLLAPSED) : (sheet.groupExpanded ? Icons.ARROW_DOWN : Icons.ARROW_RIGHT))
                    : (sheet.toggleExpanded != null ? (sheet.expanded ? Icons.UNCOLLAPSED : Icons.COLLAPSED) : null);

                int left = this.keyframes.area.x + sheet.level * LEVEL_INDENT - this.sidebarScroll;
                if (sheet.groupHeader && (sheet.groupKey == null || (!sheet.groupKey.endsWith("__world__") && !sheet.groupKey.endsWith("__model__"))))
                {
                    left += 4;
                }
                int iconWidth = 2 + (arrow != null ? arrow.w + 4 : 0);
                int clickableWidth = Math.min(SIDEBAR_WIDTH - sheet.level * LEVEL_INDENT, iconWidth + font.getWidth(displayTitle) + 6);
                clickableWidth = Math.max(0, clickableWidth);

                if (context.mouseX >= left && context.mouseX <= left + clickableWidth)
                {
                    if (sheet.groupHeader && sheet.toggleGroup != null)
                    {
                        sheet.toggleGroup.run();
                        return true;
                    }
                    else if (!sheet.groupHeader && sheet.toggleExpanded != null)
                    {
                        sheet.toggleExpanded.run();
                        return true;
                    }
                }
            }
        }

        return this.dopeSheet.mouseClicked(context);
    }

    @Override
    public void mouseReleased(UIContext context)
    {
        this.dopeSheet.mouseReleased(context);
        this.sidebarScrollbar.mouseReleased(context);
        this.sidebarDragging = false;
    }

    @Override
    public void mouseScrolled(UIContext context)
    {
        Area area = this.keyframes.area;
        boolean inSidebar = area.isInside(context) && context.mouseX < area.x + SIDEBAR_WIDTH;

        if (inSidebar)
        {
            this.updateSidebarScrollLimits(context);
        }

        if (inSidebar && (context.mouseWheelHorizontal != 0 || context.mouseWheel != 0D))
        {
            if (this.sidebarScrollMax <= 0)
            {
                return;
            }

            double wheel = context.mouseWheelHorizontal != 0 ? context.mouseWheelHorizontal : context.mouseWheel;
            float sensitivity = BBSSettings.scrollingSensitivityHorizontal.get();
            int delta = (int) Math.round(25F * sensitivity * wheel);

            if (delta == 0)
            {
                delta = wheel > 0 ? 1 : -1;
            }

            this.sidebarScrollbar.scrollBy(-delta);
            this.sidebarScroll = (int) Math.round(this.sidebarScrollbar.getScroll());

            return;
        }

        if (context.mouseWheelHorizontal != 0)
        {
            double offsetX = (25F * BBSSettings.scrollingSensitivityHorizontal.get() * context.mouseWheelHorizontal) / this.keyframes.getXAxis().getZoom();

            this.keyframes.getXAxis().setShift(this.keyframes.getXAxis().getShift() - offsetX);
        }
        else if (Window.isShiftPressed())
        {
            this.dopeSheet.mouseScroll(context);
        }
        else if (Window.isAltPressed())
        {
            this.setTrackHeight(this.trackHeight - context.mouseWheel);
        }
        else if (context.mouseWheel != 0D)
        {
            this.keyframes.getXAxis().zoomAnchor(Scale.getAnchorX(context, this.keyframes.area), Math.copySign(this.keyframes.getXAxis().getZoomFactor(), context.mouseWheel));
        }
    }

    @Override
    public void handleMouse(UIContext context, int lastX, int lastY)
    {
        this.dopeSheet.drag(context);

        if (this.sidebarDragging)
        {
            this.scrollSidebarToMouse(context.mouseX);
        }

        this.sidebarScroll = (int) Math.round(this.sidebarScrollbar.getScroll());

        if (this.keyframes.isNavigating())
        {
            int mouseX = context.mouseX;
            int mouseY = context.mouseY;
            double offset = (mouseX - lastX) / this.keyframes.getXAxis().getZoom();

            this.keyframes.getXAxis().setShift(this.keyframes.getXAxis().getShift() - offset);
            this.dopeSheet.scrollBy(-(mouseY - lastY));
        }
    }

    @Override
    public void dragKeyframes(UIContext context, Pair<Keyframe, KeyframeType> type, int originalX, int originalY, float originalT, Object originalV)
    {
        float offset = (float) (this.keyframes.fromGraphX(originalX) - originalT);
        float tick = (float) this.keyframes.fromGraphX(context.mouseX) - offset;

        if (!Window.isShiftPressed())
        {
            tick = Math.round(this.keyframes.fromGraphX(context.mouseX) - offset);
        }

        this.setTick(tick, false);
        this.keyframes.triggerChange();
    }

    /* Rendering */

    @Override
    public void render(UIContext context)
    {
        this.renderGrid(context);
        this.renderGraph(context);
    }

    /**
     * Render grid that allows easier to see where are specific ticks
     */
    protected void renderGrid(UIContext context)
    {
        /* Draw horizontal grid */
        Area area = this.keyframes.area;
        int mult = this.keyframes.getXAxis().getMult();
        int hx = this.keyframes.getDuration() / mult;
        int ht = (int) this.keyframes.fromGraphX(area.x);

        for (int j = Math.max(ht / mult, 0); j <= hx; j++)
        {
            int x = this.keyframes.toGraphX(j * mult);

            if (x >= area.ex())
            {
                break;
            }

            if (x < area.x + SIDEBAR_WIDTH)
            {
                continue;
            }

            String label = TimeUtils.formatTime(j * mult);

            context.batcher.box(x, area.y, x + 1, area.ey(), Colors.setA(Colors.WHITE, 0.25F));
            context.batcher.text(label, x + 4, area.y + 2);
        }

        /* Render where the keyframe will be duplicated or added */
        if (!area.isInside(context))
        {
            return;
        }

        if (this.keyframes.isStacking())
        {
            List<UIKeyframeSheet> sheets = new ArrayList<>();
            float currentTick = (float) this.keyframes.fromGraphX(context.mouseX);

            for (UIKeyframeSheet sheet : this.getSheets())
            {
                if (sheet.selection.hasAny())
                {
                    sheets.add(sheet);
                }
            }

            for (UIKeyframeSheet current : sheets)
            {
                List<Keyframe> selected = current.selection.getSelected();
                float mmin = Integer.MAX_VALUE;
                float mmax = Integer.MIN_VALUE;

                for (Keyframe keyframe : selected)
                {
                    mmin = Math.min(keyframe.getTick(), mmin);
                    mmax = Math.max(keyframe.getTick(), mmax);
                }

                float length = mmax - mmin + this.keyframes.getStackOffset();
                int times = (int) Math.max(1, Math.ceil((currentTick - mmax) / length));
                float x = 0;

                for (int i = 0; i < times; i++)
                {
                    for (Keyframe keyframe : selected)
                    {
                        float tick = mmax + this.keyframes.getStackOffset() + (keyframe.getTick() - mmin) + x;

                        this.renderPreviewKeyframe(context, current, tick, Colors.YELLOW);
                    }

                    x += length;
                }
            }
        }
        else if (Window.isCtrlPressed())
        {
            UIKeyframeSheet sheet = this.getSheet(context.mouseY);

            if (sheet != null)
            {
                float tick = (float) this.keyframes.fromGraphX(context.mouseX);

                if (!Window.isShiftPressed())
                {
                    tick = Math.round(tick);
                }

                this.renderPreviewKeyframe(context, sheet, tick, Colors.WHITE);
            }
        }
        else if (Window.isAltPressed() && !Window.isShiftPressed())
        {
            List<UIKeyframeSheet> sheets = new ArrayList<>();

            for (UIKeyframeSheet sheet : this.getSheets())
            {
                if (sheet.selection.hasAny())
                {
                    sheets.add(sheet);
                }
            }

            if (sheets.size() == 1)
            {
                UIKeyframeSheet current = sheets.get(0);
                UIKeyframeSheet hovered = this.getSheet(context.mouseY);

                if (hovered == null || current.channel.getFactory() != hovered.channel.getFactory())
                {
                    return;
                }

                List<Keyframe> selected = current.selection.getSelected();

                for (int i = 0; i < selected.size(); i++)
                {
                    Keyframe first = selected.get(0);
                    Keyframe keyframe = selected.get(i);

                    this.renderPreviewKeyframe(context, hovered, Math.round(this.keyframes.fromGraphX(context.mouseX)) + (keyframe.getTick() - first.getTick()), Colors.YELLOW);
                }
            }
            else
            {
                float min = Float.MAX_VALUE;

                for (UIKeyframeSheet sheet : sheets)
                {
                    List<Keyframe> selected = sheet.selection.getSelected();

                    for (Keyframe keyframe : selected)
                    {
                        min = Math.min(min, keyframe.getTick());
                    }
                }

                for (UIKeyframeSheet sheet : sheets)
                {
                    List<Keyframe> selected = sheet.selection.getSelected();

                    for (int i = 0; i < selected.size(); i++)
                    {
                        Keyframe keyframe = selected.get(i);

                        this.renderPreviewKeyframe(context, sheet, Math.round(this.keyframes.fromGraphX(context.mouseX)) + (keyframe.getTick() - min), Colors.YELLOW);
                    }
                }
            }
        }
    }

    private void renderPreviewKeyframe(UIContext context, UIKeyframeSheet sheet, double tick, int color)
    {
        int x = this.keyframes.toGraphX(tick);
        int y = this.getDopeSheetY(sheet) + (int) this.trackHeight / 2;
        float a = (float) Math.sin(context.getTickTransition() / 2D) * 0.1F + 0.5F;

        context.batcher.box(x - 3, y - 3, x + 3, y + 3, Colors.setA(color, a));
    }

    /**
     * Render the graph
     */
    @SuppressWarnings({"rawtypes", "IntegerDivisionInFloatingPointContext"})
    protected void renderGraph(UIContext context)
    {
        if (this.sheets.isEmpty())
        {
            return;
        }

        this.dopeSheet.scrollSize = (int) this.trackHeight * this.sheets.size() + TOP_MARGIN;

        Area area = this.keyframes.area;
        this.updateSidebarScrollLimits(context);
        BufferBuilder builder = Tessellator.getInstance().getBuffer();
        Matrix4f matrix = context.batcher.getContext().getMatrices().peek().getPositionMatrix();

        int sidebarX = area.x - this.sidebarScroll;

        for (int i = 0; i < this.sheets.size(); i++)
        {
            int y = this.getDopeSheetY(i);

            if (y + this.trackHeight < area.y || y > area.ey())
            {
                continue;
            }

            UIKeyframeSheet sheet = this.sheets.get(i);
            List keyframes = sheet.channel.getKeyframes();

            boolean hover = area.isInside(context) && context.mouseY >= y && context.mouseY < y + this.trackHeight;
            int my = y + (int) this.trackHeight / 2;
            int cc = Colors.setA(sheet.color, hover ? 0.8F : 0.35F);

            if (sheet.groupHeader)
            {
                FontRenderer font = context.batcher.getFont();
                String title = this.getEffectiveSidebarTitle(sheet);
                String displayTitle = this.getSidebarTitle(title);

                Icon arrow = sheet.groupExpanded ? Icons.ARROW_DOWN : Icons.ARROW_RIGHT;
                int iconX = sidebarX + 6 + sheet.level * LEVEL_INDENT;

                if (sheet.groupKey != null && (sheet.groupKey.endsWith("__world__") || sheet.groupKey.endsWith("__model__")))
                {
                    arrow = sheet.groupExpanded ? Icons.UNCOLLAPSED : Icons.COLLAPSED;
                    iconX = sidebarX + 2 + sheet.level * LEVEL_INDENT;
                }

                int iconY = my - arrow.h / 2;
                int textX = iconX + arrow.w + 4;
                int textY = my - font.getHeight() / 2;
                int textW = font.getWidth(displayTitle);

                context.batcher.clip(area.x, y, SIDEBAR_WIDTH, (int) this.trackHeight, context);
                context.batcher.icon(arrow, iconX, iconY);
                context.batcher.textShadow(displayTitle, textX, textY);
                context.batcher.unclip(context);

                continue;
            }

            /* Render track bars (horizontal lines) */
            builder.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);

            int startX = area.x;
            int endX = area.ex();

            startX += SIDEBAR_WIDTH;

            context.batcher.fillRect(builder, matrix, startX, my - TRACK_LINE_HALF_HEIGHT, endX - startX, TRACK_LINE_HALF_HEIGHT * 2, cc, cc, cc, cc);

            if (sheet.separator)
            {
                int c = Colors.setA(sheet.color, 0F);
                int sepStartX = area.x;

                sepStartX += SIDEBAR_WIDTH;

                /* Render separator */
                context.batcher.fillRect(builder, matrix, sepStartX, y, endX - sepStartX, (int) this.trackHeight, c | Colors.A25, c | Colors.A25, c, c);
            }

            /* Render bars indicating same values */
            for (int j = 1; j < keyframes.size(); j++)
            {
                Keyframe previous = (Keyframe) keyframes.get(j - 1);
                Keyframe frame = (Keyframe) keyframes.get(j);
                int c = Colors.YELLOW | Colors.A25;
                int xx = this.keyframes.toGraphX(previous.getTick());
                int xxx = this.keyframes.toGraphX(frame.getTick());

                xx = Math.max(xx, area.x + SIDEBAR_WIDTH);
                xxx = Math.max(xxx, area.x + SIDEBAR_WIDTH);

                if (previous.getFactory().compare(previous.getValue(), frame.getValue()))
                {
                    if (xxx > xx)
                    {
                        context.batcher.fillRect(builder, matrix, xx, my - TRACK_LINE_HALF_HEIGHT, xxx - xx, TRACK_LINE_HALF_HEIGHT * 2, c, c, c, c);
                    }
                }

                if (Math.abs(xxx - xx) < 5)
                {
                    if (xx >= area.x + SIDEBAR_WIDTH)
                    {
                        c = Colors.YELLOW | Colors.A50;

                        context.batcher.fillRect(builder, matrix, xx - 2, my + 5, xxx - xx + 4, 2, c, c, c, c);
                    }
                }
            }

            /* Draw keyframe handles (outer) */
            int forcedIndex = 0;

            for (int j = 0; j < keyframes.size(); j++)
            {
                Keyframe frame = (Keyframe) keyframes.get(j);
                float tick = frame.getTick();
                int x1 = this.keyframes.toGraphX(tick);
                int x2 = this.keyframes.toGraphX(tick + frame.getDuration());

                /* Render custom duration markers */
                if (x1 != x2)
                {
                    int rx1 = x1;
                    int rx2 = x2;

                    rx1 = Math.max(x1, area.x + SIDEBAR_WIDTH);
                    rx2 = Math.max(x2, area.x + SIDEBAR_WIDTH);

                    if (rx2 > rx1)
                    {
                        int y1 = my - 8 + (forcedIndex % 2 == 1 ? -4 : 0);
                        int color = sheet.selection.has(j) ? Colors.WHITE :  Colors.setA(Colors.mulRGB(sheet.color, 0.9F), 0.75F);

                        if (rx1 == x1) context.batcher.fillRect(builder, matrix, rx1, y1 - 2, 1, 5, color, color, color, color);
                        if (rx2 == x2) context.batcher.fillRect(builder, matrix, rx2, y1 - 2, 1, 5, color, color, color, color);
                        context.batcher.fillRect(builder, matrix, rx1, y1, rx2 - rx1, 1, color, color, color, color);
                    }

                    forcedIndex += 1;
                }

                if (x1 < area.x + SIDEBAR_WIDTH)
                {
                    continue;
                }

                boolean isPointHover = this.isNear(this.keyframes.toGraphX(frame.getTick()), my, context.mouseX, context.mouseY, Window.isAltPressed() && Window.isShiftPressed());
                boolean toRemove = Window.isCtrlPressed() && isPointHover;

                if (this.keyframes.isSelecting())
                {
                    isPointHover = isPointHover || this.keyframes.getGrabbingArea(context).isInside(x1, my);
                }

                int kc = frame.getColor() != null ? frame.getColor().getRGBColor() | Colors.A100 : sheet.color;
                int c = (sheet.selection.has(j) || isPointHover ? Colors.WHITE : kc) | Colors.A100;

                if (toRemove)
                {
                    c = Colors.RED | Colors.A100;
                }

                int offset = toRemove ? 4 : 3;

                renderShape(frame, context, builder, matrix, x1, my, offset, c);
            }

            /* Render keyframe handles (inner) */
            for (int j = 0; j < keyframes.size(); j++)
            {
                Keyframe frame = (Keyframe) keyframes.get(j);
                int mx = this.keyframes.toGraphX(frame.getTick());

                if (mx < area.x + SIDEBAR_WIDTH)
                {
                    continue;
                }

                int c = sheet.selection.has(j) ? Colors.ACTIVE : 0;
                int mc = c | Colors.A100;
                IKeyframeShapeRenderer shapeResult = renderShape(frame, context, builder, matrix, mx, my, 2, mc);

                shapeResult.renderKeyframeBackground(context, builder, matrix, mx, my, 2, mc);
            }

            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionColorProgram);
            BufferRenderer.drawWithGlobalProgram(builder.end());

            FontRenderer font = context.batcher.getFont();
            String displayTitle = this.getSidebarTitle(this.getEffectiveSidebarTitle(sheet));

            Icon icon = sheet.getIcon();
            Icon arrow = sheet.toggleExpanded != null ? (sheet.expanded ? Icons.UNCOLLAPSED : Icons.COLLAPSED) : null;

            int iconWidth = 2 + sheet.level * LEVEL_INDENT + (arrow != null ? arrow.w + 4 : 0);
            if (icon != null) iconWidth += icon.w + 4;
            int lw = font.getWidth(displayTitle);

            int totalWidth = iconWidth + lw + 10;

            int c1 = hover ? Colors.setA(sheet.color, 0.66F) : 0x00000000;
            int c2 = hover ? Colors.setA(sheet.color, 0.24F) : 0x00000000;

            c1 = hover ? Colors.setA(sheet.color, 0.58F) : 0x00000000;
            c2 = hover ? Colors.setA(sheet.color, 0.18F) : 0x00000000;

            context.batcher.box(area.x, y, area.x + 2, y + (int) this.trackHeight, sheet.color | Colors.A100);

            context.batcher.gradientHBox(area.x, y, area.x + SIDEBAR_WIDTH, y + (int) this.trackHeight, c1, c2);

            context.batcher.clip(area.x, y, SIDEBAR_WIDTH, (int) this.trackHeight, context);

            if (arrow != null)
            {
                context.batcher.icon(arrow, sidebarX + 4 + sheet.level * LEVEL_INDENT, my - arrow.h / 2);
            }

            int currentX = sidebarX + 4 + sheet.level * LEVEL_INDENT + (arrow != null ? arrow.w + 4 : 0);

            if (icon != null)
            {
                context.batcher.icon(icon, currentX, my - icon.h / 2);
                currentX += icon.w + 4;
            }

            if (hover)
            {
                context.batcher.textShadow(displayTitle, currentX, my - font.getHeight() / 2);
            }
            else
            {
                context.batcher.textShadow(displayTitle, currentX, my - font.getHeight() / 2, Colors.WHITE & 0xeeffffff);
            }

            context.batcher.unclip(context);
        }
    }

    @Override
    public void postRender(UIContext context)
    {
        this.dopeSheet.renderScrollbar(context.batcher);
        this.renderSidebarScrollbar(context);
    }

    private void renderSidebarScrollbar(UIContext context)
    {
        Area area = this.keyframes.area;

        this.updateSidebarScrollLimits(context);
        this.updateSidebarScrollbarArea(area);

        int barHeight = this.sidebarScrollbar.getScrollbarWidth();
        int y = area.ey() - barHeight;
        int trackX = area.x;
        int trackW = SIDEBAR_WIDTH;

        context.batcher.box(trackX, y, trackX + trackW, y + barHeight, Colors.A50);

        if (this.sidebarScrollMax <= 0)
        {
            Scroll.bar(context.batcher, trackX, y, trackX + trackW, y + barHeight, BBSSettings.scrollbarShadow.get());
            return;
        }

        Area knob = this.sidebarScrollbar.getScrollbarArea();
        Scroll.bar(context.batcher, knob.x, knob.y, knob.ex(), knob.ey(), BBSSettings.scrollbarShadow.get());
    }

    private void updateSidebarScrollLimits(UIContext context)
    {
        FontRenderer font = context.batcher.getFont();
        int maxWidth = SIDEBAR_WIDTH;

        for (UIKeyframeSheet sheet : this.sheets)
        {
            String title = this.getEffectiveSidebarTitle(sheet);
            int titleWidth = font.getWidth(title);

            if (sheet.groupHeader)
            {
                Icon arrow = sheet.groupExpanded ? Icons.ARROW_DOWN : Icons.ARROW_RIGHT;
                int base = 6 + sheet.level * LEVEL_INDENT;

                if (sheet.groupKey != null && (sheet.groupKey.endsWith("__world__") || sheet.groupKey.endsWith("__model__")))
                {
                    arrow = sheet.groupExpanded ? Icons.UNCOLLAPSED : Icons.COLLAPSED;
                    base = 2 + sheet.level * LEVEL_INDENT;
                }

                int width = base + arrow.w + 4 + titleWidth + 4;
                maxWidth = Math.max(maxWidth, width);

                continue;
            }

            Icon arrow = sheet.toggleExpanded != null ? (sheet.expanded ? Icons.UNCOLLAPSED : Icons.COLLAPSED) : null;
            Icon icon = sheet.getIcon();
            int iconWidth = 2 + sheet.level * LEVEL_INDENT + (arrow != null ? arrow.w + 4 : 0) + (icon != null ? icon.w + 4 : 0);
            int totalWidth = iconWidth + titleWidth + 10;

            maxWidth = Math.max(maxWidth, totalWidth);
        }

        this.sidebarScrollMax = Math.max(0, maxWidth - SIDEBAR_WIDTH);
        this.sidebarScroll = Math.max(0, Math.min(this.sidebarScrollMax, this.sidebarScroll));
        this.sidebarScrollbar.scrollSize = SIDEBAR_WIDTH + this.sidebarScrollMax;
        this.sidebarScrollbar.setScroll(this.sidebarScroll);
    }

    private void updateSidebarScrollbarArea(Area area)
    {
        int barHeight = this.sidebarScrollbar.getScrollbarWidth();

        this.sidebarScrollbar.area.set(area.x, area.ey() - barHeight, SIDEBAR_WIDTH, barHeight);
    }

    /* State recovery */

    @Override
    public void saveState(MapType extra)
    {
        extra.putDouble("track_height", this.trackHeight);
        extra.putDouble("scroll", this.dopeSheet.getScroll());
    }

    @Override
    public void restoreState(MapType extra)
    {
        this.setTrackHeight(extra.getDouble("track_height"));
        this.dopeSheet.setScroll(extra.getDouble("scroll"));
    }

    private boolean handleSidebarScrollbarClick(UIContext context)
    {
        if (!this.keyframes.area.isInside(context) || this.sidebarScrollMax <= 0)
        {
            return false;
        }

        this.updateSidebarScrollLimits(context);
        this.updateSidebarScrollbarArea(this.keyframes.area);

        if (!this.sidebarScrollbar.area.isInside(context.mouseX, context.mouseY))
        {
            return false;
        }

        Area knob = this.sidebarScrollbar.getScrollbarArea();

        if (knob.w <= 0)
        {
            return false;
        }

        if (knob.isInside(context.mouseX, context.mouseY))
        {
            this.sidebarDragRatio = (context.mouseX - knob.x) / (float) knob.w;
            this.sidebarDragRatio = MathUtils.clamp(this.sidebarDragRatio, 0F, 1F);
        }
        else
        {
            this.sidebarDragRatio = 0.5F;
            this.scrollSidebarToMouse(context.mouseX);
        }

        this.sidebarDragging = true;

        return true;
    }

    private void scrollSidebarToMouse(int mouseX)
    {
        int trackX = this.sidebarScrollbar.area.x;
        int trackW = this.sidebarScrollbar.area.w;
        int knobW = Math.max(1, this.sidebarScrollbar.getScrollbar());
        int maxOffset = Math.max(1, trackW - knobW);
        int minMouse = trackX + Math.round(knobW * this.sidebarDragRatio);
        int maxMouse = trackX + trackW - Math.round(knobW * (1F - this.sidebarDragRatio));
        int clampedMouse = Math.max(minMouse, Math.min(maxMouse, mouseX));
        float progress = (clampedMouse - (trackX + knobW * this.sidebarDragRatio)) / (float) maxOffset;

        progress = MathUtils.clamp(progress, 0F, 1F);
        this.sidebarScrollbar.setScroll(progress * this.sidebarScrollMax);
        this.sidebarScroll = (int) Math.round(this.sidebarScrollbar.getScroll());
    }
}
