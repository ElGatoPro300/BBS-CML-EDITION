package mchorse.bbs_mod.ui.utils;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.BBSSettings;
import mchorse.bbs_mod.utils.OS;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvents;

import java.io.File;
import java.io.IOException;

public class UIUtils
{
    /**
     * Open web link (in default web browser)
     */
    public static boolean openWebLink(String address)
    {
        if (OS.CURRENT == OS.WINDOWS)
        {
            return runSysCommand("rundll32", "url.dll,FileProtocolHandler", address);
        }
        else if (OS.CURRENT == OS.MACOS)
        {
            return runSysCommand("open", address);
        }

        return runSysCommand("kde-open", address)
            || runSysCommand("gnome-open", address)
            || runSysCommand("xdg-open", address);
    }

    /**
     * Open a folder (in default file browser)
     */
    public static boolean openFolder(File folder)
    {
        try
        {
            String path = folder.getAbsolutePath();

            if (OS.CURRENT == OS.WINDOWS)
            {
                if (focusExistingExplorerWindow(path))
                {
                    return true;
                }

                return runSysCommand("explorer", path);
            }
            else if (OS.CURRENT == OS.MACOS)
            {
                return runSysCommand("open", path);
            }

            return runSysCommand("kde-open", path)
                || runSysCommand("gnome-open", path)
                || runSysCommand("xdg-open", path);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return false;
    }

    private static boolean focusExistingExplorerWindow(String path)
    {
        try
        {
            File canonical = new File(path).getCanonicalFile();
            String normalized = canonical.getAbsolutePath().replace("'", "''").replace("\\", "\\\\");
            String script = "$target=[System.IO.Path]::GetFullPath('" + normalized + "').TrimEnd('\\');"
                + "$shell=New-Object -ComObject Shell.Application;"
                + "foreach($window in @($shell.Windows())){"
                + "try{"
                + "$openPath=$null;"
                + "if($null -ne $window.Document -and $null -ne $window.Document.Folder -and $null -ne $window.Document.Folder.Self){"
                + "$openPath=[System.IO.Path]::GetFullPath($window.Document.Folder.Self.Path).TrimEnd('\\');"
                + "}"
                + "elseif($window.LocationURL){"
                + "$url=$window.LocationURL -replace '^file:///','' -replace '/','\\';"
                + "$url=[System.Uri]::UnescapeDataString($url);"
                + "$openPath=[System.IO.Path]::GetFullPath($url).TrimEnd('\\');"
                + "}"
                + "if($null -ne $openPath -and [string]::Equals($openPath,$target,[System.StringComparison]::OrdinalIgnoreCase)){"
                + "if($window.Visible -eq $false){$window.Visible=$true};"
                + "$window.Activate();"
                + "exit 0"
                + "}"
                + "}catch{}"
                + "};"
                + "exit 1";
            Process process = Runtime.getRuntime().exec(new String[] {"powershell.exe", "-NoProfile", "-Command", script});

            if (process.waitFor() == 0)
            {
                return true;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return false;
    }

    private static boolean runSysCommand(String... command)
    {
        try
        {
            Process p = Runtime.getRuntime().exec(command);

            if (p == null)
            {
                return false;
            }

            try
            {
                return p.exitValue() == 0;
            }
            catch (IllegalThreadStateException e)
            {
                return true;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();

            return false;
        }
    }

    public static void playClick()
    {
        playClick(1F);
    }

    public static void playClick(float pitch)
    {
        if (BBSSettings.clickSound.get())
        {
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(BBSMod.CLICK, pitch));
        }
        else
        {
            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, pitch));
        }
    }
}