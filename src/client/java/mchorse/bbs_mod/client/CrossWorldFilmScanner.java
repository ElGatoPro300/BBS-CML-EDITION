package mchorse.bbs_mod.client;

import mchorse.bbs_mod.film.CrossWorldFilmEntry;

import net.minecraft.client.MinecraftClient;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelSummary;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CrossWorldFilmScanner
{
    private static final String FILMS_EXTENSION = ".dat";

    public static CompletableFuture<List<CrossWorldFilmEntry>> scanAsync()
    {
        MinecraftClient client = MinecraftClient.getInstance();
        LevelStorage storage = client.getLevelStorage();
        LevelStorage.LevelList levelList = storage.getLevelList();

        return storage.loadSummaries(levelList).thenApply((summaries) ->
        {
            List<LevelSummary> sortedSummaries = new ArrayList<>(summaries);

            sortedSummaries.sort(Comparator.comparingLong(LevelSummary::getLastPlayed).reversed());

            Map<String, LevelStorage.LevelSave> saves = new HashMap<>();

            for (LevelStorage.LevelSave save : levelList.levels())
            {
                saves.put(save.getRootPath(), save);
            }

            List<CrossWorldFilmEntry> entries = new ArrayList<>();

            for (LevelSummary summary : sortedSummaries)
            {
                String worldFolder = summary.getName();
                String worldLabel = summary.getDisplayName();
                long lastPlayed = summary.getLastPlayed();
                LevelStorage.LevelSave save = saves.get(worldFolder);

                if (save == null)
                {
                    continue;
                }

                File filmsFolder = save.path().resolve("bbs/films").toFile();
                int beforeCount = entries.size();

                if (filmsFolder.isDirectory())
                {
                    CrossWorldFilmScanner.collectFilms(entries, worldFolder, worldLabel, lastPlayed, filmsFolder, "");
                }

                if (entries.size() == beforeCount)
                {
                    entries.add(new CrossWorldFilmEntry(worldFolder, worldLabel, "", lastPlayed));
                }
            }

            return entries;
        });
    }

    private static void collectFilms(List<CrossWorldFilmEntry> entries, String worldFolder, String worldLabel, long lastPlayed, File folder, String prefix)
    {
        File[] children = folder.listFiles();

        if (children == null)
        {
            return;
        }

        for (File child : children)
        {
            String name = child.getName();

            if (child.isFile() && name.endsWith(FILMS_EXTENSION) && !name.startsWith("_"))
            {
                String filmId = prefix + name.substring(0, name.length() - FILMS_EXTENSION.length());

                entries.add(new CrossWorldFilmEntry(worldFolder, worldLabel, filmId, lastPlayed));
            }
            else if (child.isDirectory() && !name.startsWith("_"))
            {
                File[] nested = child.listFiles();
                String nestedPrefix = prefix + name + "/";

                if (nested == null || nested.length == 0)
                {
                    entries.add(new CrossWorldFilmEntry(worldFolder, worldLabel, nestedPrefix, lastPlayed));
                }
                else
                {
                    CrossWorldFilmScanner.collectFilms(entries, worldFolder, worldLabel, lastPlayed, child, nestedPrefix);
                }
            }
        }
    }
}
