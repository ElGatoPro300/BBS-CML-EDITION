package mchorse.bbs_mod.camera.utils;

import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.utils.StringUtils;

public class TimeUtils
{
    public static int toTick(float seconds)
    {
        return (int) (seconds * 20);
    }

    public static float toSeconds(float tick)
    {
        return tick / 20F;
    }

    public static String formatTime(double ticks)
    {
        if (BBSSettings.editorSeconds.get())
        {
            long seconds = (long) (ticks / 20D);
            int milliseconds = (int) (ticks % 20 == 0 ? 0 : ticks % 20 * 5D);

            return seconds + "." + StringUtils.leftPad(String.valueOf(milliseconds), 2, "0");
        }
        else if (BBSSettings.editorFrames.get())
        {
            int fps = BBSSettings.videoSettings.frameRate.get();
            int frame = (int) Math.round(ticks / 20.0 * fps);

            return String.valueOf(frame);
        }

        return String.valueOf((int) ticks);
    }

    public static double toTime(double ticks)
    {
        if (BBSSettings.editorSeconds.get())
        {
            return ticks / 20D;
        }
        else if (BBSSettings.editorFrames.get())
        {
            return ticks / 20D * BBSSettings.videoSettings.frameRate.get();
        }

        return ticks;
    }

    public static double fromTime(double time)
    {
        if (BBSSettings.editorSeconds.get())
        {
            return Math.round(time * 20D);
        }
        else if (BBSSettings.editorFrames.get())
        {
            return Math.round(time / BBSSettings.videoSettings.frameRate.get() * 20D);
        }

        return (int) time;
    }
}