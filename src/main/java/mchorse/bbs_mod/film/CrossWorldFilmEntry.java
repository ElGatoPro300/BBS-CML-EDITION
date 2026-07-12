package mchorse.bbs_mod.film;

public class CrossWorldFilmEntry
{
    public final String worldFolder;
    public final String worldLabel;
    public final String filmId;
    public final long lastPlayed;

    public CrossWorldFilmEntry(String worldFolder, String worldLabel, String filmId)
    {
        this(worldFolder, worldLabel, filmId, 0L);
    }

    public CrossWorldFilmEntry(String worldFolder, String worldLabel, String filmId, long lastPlayed)
    {
        this.worldFolder = worldFolder;
        this.worldLabel = worldLabel;
        this.filmId = filmId;
        this.lastPlayed = lastPlayed;
    }

    public String encodeKey()
    {
        return this.worldFolder + "\0" + this.filmId;
    }

    public static CrossWorldFilmEntry decodeKey(String key)
    {
        int separator = key.indexOf('\0');

        if (separator < 0)
        {
            return null;
        }

        return new CrossWorldFilmEntry(key.substring(0, separator), key.substring(0, separator), key.substring(separator + 1));
    }

    public String getDisplayLabel()
    {
        String filmLabel = this.filmId.replace('/', ' ').trim();

        if (filmLabel.isEmpty())
        {
            filmLabel = this.filmId;
        }

        return this.worldLabel + " — " + filmLabel;
    }
}
