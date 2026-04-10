package mchorse.bbs_mod.graphics.window;

import mchorse.bbs_mod.data.DataToString;
import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.data.types.ListType;
import mchorse.bbs_mod.data.types.MapType;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MemoryUtil;
import com.mojang.blaze3d.platform.InputConstants;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Window
{
    private static int verticalScroll;
    private static long lastScroll;

    public static com.mojang.blaze3d.platform.Window getWindow()
    {
        return Minecraft.getInstance().getWindow();
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
        return GLFW.glfwGetMouseButton(getWindow().handle(), mouse) == GLFW.GLFW_PRESS;
    }

    public static boolean isCtrlPressed()
    {
        return InputConstants.isKeyDown(getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL) || InputConstants.isKeyDown(getWindow(), GLFW.GLFW_KEY_RIGHT_CONTROL);
    }

    public static boolean isShiftPressed()
    {
        return InputConstants.isKeyDown(getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT) || InputConstants.isKeyDown(getWindow(), GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    public static boolean isAltPressed()
    {
        return InputConstants.isKeyDown(getWindow(), GLFW.GLFW_KEY_LEFT_ALT) || InputConstants.isKeyDown(getWindow(), GLFW.GLFW_KEY_RIGHT_ALT);
    }

    public static boolean isKeyPressed(int key)
    {
        return InputConstants.isKeyDown(getWindow(), key);
    }

    public static String getClipboard()
    {
        try
        {
            String string = GLFW.glfwGetClipboardString(getWindow().handle());

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

            GLFW.glfwSetClipboardString(getWindow().handle(), buffer);

            MemoryUtil.memFree(buffer);
        }
        else
        {
            GLFW.glfwSetClipboardString(getWindow().handle(), string);
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
        GLFW.glfwSetCursorPos(getWindow().handle(), x, y);
    }
}
