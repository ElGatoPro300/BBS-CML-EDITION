package mchorse.bbs_mod.film.replays;

import mchorse.bbs_mod.data.types.BaseType;
import mchorse.bbs_mod.settings.values.core.ValueList;
import mchorse.bbs_mod.utils.CollectionUtils;

public class Replays extends ValueList<Replay>
{
    public Replays(String id)
    {
        super(id);
    }

    @Override
    public void fromData(BaseType data)
    {
        super.fromData(data);

        java.util.List<Replay> allReplays = this.getList();
        java.util.Set<String> existingUUIDs = new java.util.HashSet<>();
        
        /* Collect existing UUIDs */
        for (Replay r : allReplays)
        {
            existingUUIDs.add(r.uuid.get());
        }

        /* Identify old groups and their members */
        java.util.Map<String, java.util.List<Replay>> oldGroups = new java.util.LinkedHashMap<>();
        
        for (Replay replay : allReplays)
        {
            String groupName = replay.group.get();

            if (!groupName.isEmpty() && !existingUUIDs.contains(groupName))
            {
                oldGroups.computeIfAbsent(groupName, k -> new java.util.ArrayList<>()).add(replay);
            }
        }

        /* If no old groups found, exit early */
        if (oldGroups.isEmpty())
        {
            return;
        }

        /* Reconstruct list with new Group Replays and contiguous members */
        java.util.List<Replay> newList = new java.util.ArrayList<>();
        java.util.Set<Replay> processed = new java.util.HashSet<>();
        java.util.Map<String, Replay> createdGroups = new java.util.HashMap<>();

        for (Replay replay : allReplays)
        {
            if (processed.contains(replay))
            {
                continue;
            }

            String groupName = replay.group.get();
            boolean isOldGroupMember = !groupName.isEmpty() && !existingUUIDs.contains(groupName);

            if (isOldGroupMember)
            {
                /* We encountered the first member of an old group */
                if (!createdGroups.containsKey(groupName))
                {
                    /* Create the Group Replay */
                    Replay groupReplay = new Replay(String.valueOf(allReplays.size() + createdGroups.size()));
                    groupReplay.isGroup.set(true);
                    groupReplay.label.set(groupName);
                    /* Ensure unique UUID if by chance it collides (unlikely but safe) */
                    while (existingUUIDs.contains(groupReplay.uuid.get()))
                    {
                        groupReplay.uuid.set(java.util.UUID.randomUUID().toString());
                    }
                    existingUUIDs.add(groupReplay.uuid.get());
                    
                    createdGroups.put(groupName, groupReplay);
                    newList.add(groupReplay);

                    /* Add all members of this group immediately */
                    java.util.List<Replay> members = oldGroups.get(groupName);
                    if (members != null)
                    {
                        for (Replay member : members)
                        {
                            member.group.set(groupReplay.uuid.get());
                            newList.add(member);
                            processed.add(member);
                        }
                    }
                }
            }
            else
            {
                /* Normal replay or already valid group member */
                newList.add(replay);
                processed.add(replay);
            }
        }

        /* Update the main list */
        this.list.clear();
        this.list.addAll(newList);
        this.sync();
    }

    public Replay addReplay()
    {
        Replay replay = new Replay(String.valueOf(this.list.size()));

        this.preNotify();
        this.add(replay);
        this.postNotify();

        return replay;
    }

    public void remove(Replay replay)
    {
        int index = CollectionUtils.getIndex(this.list, replay);

        if (CollectionUtils.inRange(this.list, index))
        {
            this.preNotify();
            this.list.remove(index);
            this.sync();
            this.postNotify();
        }
    }

    @Override
    protected Replay create(String id)
    {
        return new Replay(id);
    }
}