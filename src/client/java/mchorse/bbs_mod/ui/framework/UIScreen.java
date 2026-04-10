package mchorse.bbs_mod.ui.framework;

import mchorse.bbs_mod.BBSModClient;
import mchorse.bbs_mod.importers.IImportPathProvider;
import mchorse.bbs_mod.importers.ImporterContext;
import mchorse.bbs_mod.importers.Importers;
import mchorse.bbs_mod.importers.types.IImporter;
import mchorse.bbs_mod.ui.UIKeys;
import mchorse.bbs_mod.ui.utils.IFileDropListener;
import mchorse.bbs_mod.ui.utils.UIUtils;
import mchorse.bbs_mod.utils.FFMpegUtils;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class UIScreen extends Screen implements IFileDropListener
{
    private UIBaseMenu menu;
    private UIRenderingContext context;

    private int lastGuiScale;

    public static void open(UIBaseMenu menu)
    {
        Minecraft.getInstance().setScreen(new UIScreen(Component.empty(), menu));
    }

    public static UIBaseMenu getCurrentMenu()
    {
        Screen currentScreen = Minecraft.getInstance().screen;

        if (currentScreen instanceof UIScreen uiScreen)
        {
            return uiScreen.menu;
        }

        return null;
    }

    public UIScreen(Component title, UIBaseMenu menu)
    {
        super(title);

        Minecraft mc = Minecraft.getInstance();

        this.menu = menu;
        this.context = new UIRenderingContext(new GuiGraphics(mc, new GuiRenderState(), 0, 0));

        this.menu.context.setup(this.context);
    }

    public UIBaseMenu getMenu()
    {
        return this.menu;
    }

    public void update()
    {
        this.menu.update();
    }

    public void renderInWorld(WorldRenderContext context)
    {
        this.menu.renderInWorld(context);
    }

    /* @Override */
    public void filesDragged(List<Path> paths)
    {
        /* super.filesDragged(paths); */

        String[] filePaths = new String[paths.size()];
        int i = 0;

        for (Path path : paths)
        {
            filePaths[i] = path.toAbsolutePath().toString();

            i += 1;
        }

        this.acceptFilePaths(filePaths);
    }

    @Override
    public void removed()
    {
        Minecraft.getInstance().options.guiScale().set(this.lastGuiScale);
        Minecraft.getInstance().resizeDisplay();

        super.removed();

        this.menu.onClose(null);

        if (this.menu.canHideHUD())
        {
            Minecraft.getInstance().options.hideGui = false;
        }
    }

    @Override
    public void added()
    {
        this.lastGuiScale = Minecraft.getInstance().options.guiScale().get();

        Minecraft.getInstance().options.guiScale().set(BBSModClient.getGUIScale());
        Minecraft.getInstance().resizeDisplay();

        super.added();

        this.menu.onOpen(null);

        if (this.menu.canHideHUD())
        {
            Minecraft.getInstance().options.hideGui = true;
        }
    }

    @Override
    public boolean isPauseScreen()
    {
        return this.menu.canPause();
    }

    @Override
    protected void init()
    {
        super.init();

        this.menu.resize(this.width, this.height);
    }

    @Override
    public void resize(int width, int height)
    {
        super.resize(width, height);

        this.menu.resize(width, height);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent click, boolean doubleClick)
    {
        return this.menu.mouseClicked((int) click.x(), (int) click.y(), click.button());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount)
    {
        return this.menu.mouseScrolled((int) mouseX, (int) mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent click)
    {
        return this.menu.mouseReleased((int) click.x(), (int) click.y(), click.button());
    }

    @Override
    public boolean keyPressed(KeyEvent input)
    {
        return this.menu.handleKey(input.key(), input.scancode(), GLFW.GLFW_PRESS, input.modifiers());
    }

    @Override
    public boolean keyReleased(KeyEvent input)
    {
        return this.menu.handleKey(input.key(), input.scancode(), GLFW.GLFW_RELEASE, input.modifiers());
    }

    @Override
    public boolean charTyped(CharacterEvent input)
    {
        this.menu.handleTextInput((char) input.codepoint());

        return true;
    }

    @Override
    public void renderBackground(GuiGraphics context, int mouseX, int mouseY, float delta)
    {}

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta)
    {
        super.render(context, mouseX, mouseY, delta);

        this.context = new UIRenderingContext(context);
        this.menu.context.setup(this.context);
        this.menu.context.setTransition(Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
        this.menu.renderMenu(this.context, mouseX, mouseY);
        this.menu.context.render.executeRunnables();
    }

    @Override
    public void acceptFilePaths(String[] paths)
    {
        if (this.menu != null)
        {
            if (!FFMpegUtils.checkFFMPEG())
            {
                this.menu.context.notifyError(UIKeys.IMPORTER_FFMPEG_NOTIFICATION);

                return;
            }

            File directory = null;
            boolean open = true;

            for (IImportPathProvider provider : this.menu.getRoot().getChildren(IImportPathProvider.class))
            {
                directory = provider.getImporterPath();

                if (directory != null)
                {
                    open = false;

                    break;
                }
            }

            List<File> files = new ArrayList<>();

            for (String path : paths)
            {
                File file = new File(path);

                if (file.exists())
                {
                    files.add(file);
                }
            }

            ImporterContext context = new ImporterContext(files, directory);

            for (IImporter importer : Importers.getImporters())
            {
                if (importer.canImport(context))
                {
                    importer.importFiles(context);

                    if (open)
                    {
                        UIUtils.openFolder(context.getDestination(importer));
                    }

                    this.menu.context.notifySuccess(UIKeys.IMPORTER_SUCCESS_NOTIFICATION.format(importer.getName()));

                    return;
                }
            }
        }
    }
}