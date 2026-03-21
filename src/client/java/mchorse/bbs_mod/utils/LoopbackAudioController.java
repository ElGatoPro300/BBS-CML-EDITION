package mchorse.bbs_mod.utils;

public class LoopbackAudioController
{
    private static boolean captureRequested;
    private static long loopbackDevice;

    public static synchronized void requestCapture(boolean value)
    {
        captureRequested = value;
    }

    public static synchronized boolean isCaptureRequested()
    {
        return captureRequested;
    }

    public static synchronized void setLoopbackDevice(long value)
    {
        loopbackDevice = value;
    }

    public static synchronized long getLoopbackDevice()
    {
        return loopbackDevice;
    }
}
