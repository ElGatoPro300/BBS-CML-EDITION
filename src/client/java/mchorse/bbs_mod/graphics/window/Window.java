package mchorse.bbs_mod.graphics.window;

import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Window
{
    private static int verticalScroll;
    private static long lastScroll;
    private static long arrowCursor = -1L;
    private static long activeCursor = -1L;

    public static net.minecraft.client.util.Window getWindow()
    {
        return MinecraftClient.getInstance().getWindow();
    }

    public static void setVerticalScroll(int scroll)
    {
        verticalScroll = scroll;
        lastScroll = System.currentTimeMillis();
    }

    public static int getVerticalScroll()
    {
        if (lastScroll + 5 < System.currentTimeMillis())
        {
            return 0;
        }

        return verticalScroll;
    }

    public static boolean isMouseButtonPressed(int mouse)
    {
        return GLFW.glfwGetMouseButton(getWindow().getHandle(), mouse) == GLFW.GLFW_PRESS;
    }

    public static boolean isCtrlPressed()
    {
        return InputUtil.isKeyPressed(getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL) || InputUtil.isKeyPressed(getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    public static boolean isShiftPressed()
    {
        return InputUtil.isKeyPressed(getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) || InputUtil.isKeyPressed(getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public static boolean isAltPressed()
    {
        return InputUtil.isKeyPressed(getWindow(), GLFW.GLFW_KEY_LEFT_ALT) || InputUtil.isKeyPressed(getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);
    }

    public static boolean isKeyPressed(int key)
    {
        return InputUtil.isKeyPressed(getWindow(), key);
    }

    public static String getClipboard()
    {
        try
        {
            String string = GLFW.glfwGetClipboardString(getWindow().getHandle());

            return string == null ? "" : string;
        }
        catch (Exception e)
        {}

        return "";
    }

    public static MapType getClipboardMap()
    {
        return DataToString.mapFromString(getClipboard());
    }

    /**
     * Get a data map from clipboard with verification key.
     */
    public static MapType getClipboardMap(String verificationKey)
    {
        MapType data = DataToString.mapFromString(getClipboard());

        return data != null && data.getBool(verificationKey) ? data : null;
    }

    public static ListType getClipboardList()
    {
        return DataToString.listFromString(getClipboard());
    }

    public static void setClipboard(String string)
    {
        if (string.length() > 1024)
        {
            byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length + 1);

            buffer.put(bytes);
            buffer.put((byte) 0);
            buffer.flip();

            GLFW.glfwSetClipboardString(getWindow().getHandle(), buffer);

            MemoryUtil.memFree(buffer);
        }
        else
        {
            GLFW.glfwSetClipboardString(getWindow().getHandle(), string);
        }
    }

    public static void setClipboard(BaseType data)
    {
        if (data != null)
        {
            setClipboard(DataToString.toString(data, true));
        }
    }

    /**
     * Save given data to clipboard with a verification key that could be
     * used in {@link #getClipboardMap(String)} to decode data.
     */
    public static void setClipboard(MapType data, String verificationKey)
    {
        if (data != null)
        {
            data.putBool(verificationKey, true);
        }

        setClipboard(data);
    }

    public static void moveCursor(int x, int y)
    {
        GLFW.glfwSetCursorPos(getWindow().getHandle(), x, y);
    }

    public static void setCursorDefault()
    {
        setCursor(GLFW.GLFW_ARROW_CURSOR);
    }

    private static void setCursor(int type)
    {
        long cursor = getOrCreateCursor(type);
        if (cursor != 0L && activeCursor != cursor)
        {
            GLFW.glfwSetCursor(getWindow().getHandle(), cursor);
            activeCursor = cursor;
        }
    }

    private static long getOrCreateCursor(int type)
    {
        if (type == GLFW.GLFW_ARROW_CURSOR)
        {
            if (arrowCursor == -1L)
            {
                arrowCursor = GLFW.glfwCreateStandardCursor(type);
            }

            return arrowCursor;
        }

        return 0L;
    }
}
