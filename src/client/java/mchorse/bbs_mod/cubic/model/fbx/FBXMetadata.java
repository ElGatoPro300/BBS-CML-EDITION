package mchorse.bbs_mod.cubic.model.fbx;

import org.lwjgl.assimp.AIMetaData;
import org.lwjgl.assimp.AIMetaDataEntry;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.Assimp;

public class FBXMetadata
{
    public int upAxis = 1; /* Default to Y-up */
    public int originalUpAxis = 1;
    public int frontAxis = 2; /* Default to Z-front */
    public int coordAxis = 0; /* Default to X-coord */
    public double unitScaleFactor = 1.0;

    public FBXMetadata(AIScene scene)
    {
        AIMetaData metadata = scene.mMetaData();

        if (metadata != null)
        {
            System.out.println("[FBXMetadata] Found metadata properties: " + metadata.mNumProperties());

            for (int i = 0; i < metadata.mNumProperties(); i++)
            {
                String key = metadata.mKeys().get(i).dataString();
                AIMetaDataEntry entry = metadata.mValues().get(i);

                if (key.equals("UpAxis"))
                {
                    this.upAxis = getInt(entry);
                    System.out.println(" -> UpAxis: " + this.upAxis);
                }
                else if (key.equals("OriginalUpAxis"))
                {
                    this.originalUpAxis = getInt(entry);
                    System.out.println(" -> OriginalUpAxis: " + this.originalUpAxis);
                }
                else if (key.equals("FrontAxis"))
                {
                    this.frontAxis = getInt(entry);
                    System.out.println(" -> FrontAxis: " + this.frontAxis);
                }
                else if (key.equals("CoordAxis"))
                {
                    this.coordAxis = getInt(entry);
                    System.out.println(" -> CoordAxis: " + this.coordAxis);
                }
                else if (key.equals("UnitScaleFactor"))
                {
                    this.unitScaleFactor = getDouble(entry);
                    System.out.println(" -> UnitScaleFactor: " + this.unitScaleFactor);
                }
            }
        }
        else
        {
            System.out.println("[FBXMetadata] No metadata found in this scene! Using defaults.");
        }
    }

    private int getInt(AIMetaDataEntry entry)
    {
        if (entry.mType() == Assimp.AI_INT32)
        {
            return entry.mData(4).asIntBuffer().get(0);
        }
        else if (entry.mType() == Assimp.AI_DOUBLE)
        {
            return (int) entry.mData(8).asDoubleBuffer().get(0);
        }
        return 0;
    }

    private double getDouble(AIMetaDataEntry entry)
    {
        if (entry.mType() == Assimp.AI_DOUBLE)
        {
            return entry.mData(8).asDoubleBuffer().get(0);
        }
        else if (entry.mType() == Assimp.AI_INT32)
        {
            return (int) entry.mData(4).asIntBuffer().get(0);
        }
        return 0;
    }
}