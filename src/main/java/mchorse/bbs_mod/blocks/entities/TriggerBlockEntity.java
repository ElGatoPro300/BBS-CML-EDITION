package mchorse.bbs_mod.blocks.entities;

import mchorse.bbs_mod.BBSMod;
import mchorse.bbs_mod.blocks.entities.ModelBlockEntity;
import mchorse.bbs_mod.data.DataStorageUtils;
import mchorse.bbs_mod.settings.values.core.ValueList;
import mchorse.bbs_mod.settings.values.misc.ValueVector3f;
import mchorse.bbs_mod.settings.values.numeric.ValueBoolean;
import mchorse.bbs_mod.triggers.Trigger;
import mchorse.bbs_mod.events.TriggerBlockEntityUpdateCallback;
import mchorse.bbs_mod.forms.FormUtils;
import mchorse.bbs_mod.forms.forms.Form;
import mchorse.bbs_mod.morphing.Morph;
import mchorse.bbs_mod.network.ServerNetwork;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.List;

public class TriggerBlockEntity extends BlockEntity
{
    public final ValueList<Trigger> left = new ValueList<Trigger>("left")
    {
        @Override
        protected Trigger create(String id)
        {
            return new Trigger(id);
        }
    };

    public final ValueList<Trigger> right = new ValueList<Trigger>("right")
    {
        @Override
        protected Trigger create(String id)
        {
            return new Trigger(id);
        }
    };

    public final ValueBoolean collidable = new ValueBoolean("collidable", false);
    public final ValueVector3f pos1 = new ValueVector3f("pos1", new Vector3f(0, 0, 0));
    public final ValueVector3f pos2 = new ValueVector3f("pos2", new Vector3f(1, 1, 1));

    public TriggerBlockEntity(BlockPos pos, BlockState state)
    {
        super(BBSMod.TRIGGER_BLOCK_ENTITY, pos, state);
    }

    public void trigger(ServerPlayerEntity player, boolean rightClick)
    {
        List<Trigger> triggers = rightClick ? this.right.getList() : this.left.getList();
        
        for (Trigger trigger : triggers)
        {
            String type = trigger.type.get();
            
            if (type.equals("command"))
            {
                String cmd = trigger.command.get();
                
                if (!cmd.isEmpty())
                {
                    try
                    {
                        player.getServer().getCommandManager().executeWithPrefix(player.getCommandSource().withLevel(2), cmd);
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }
            }
            else if (type.equals("form"))
            {
                Form form = trigger.form.get();

                ServerNetwork.sendMorphToTracked(player, form);
                Morph.getMorph(player).setForm(FormUtils.copy(form));
            }
            else if (type.equals("block"))
            {
                int x = trigger.x.get();
                int y = trigger.y.get();
                int z = trigger.z.get();
                Form form = trigger.blockForm.get();
                
                BlockPos pos = new BlockPos(x, y, z);
                
                if (this.world.isChunkLoaded(pos))
                {
                    BlockEntity be = this.world.getBlockEntity(pos);
                    
                    if (be instanceof ModelBlockEntity modelBlock)
                    {
                        modelBlock.getProperties().setForm(FormUtils.copy(form));
                        modelBlock.markDirty();
                        this.world.updateListeners(pos, this.world.getBlockState(pos), this.world.getBlockState(pos), 3);
                    }
                }
            }
        }
    }
    
    public static void tick(World world, BlockPos pos, BlockState state, TriggerBlockEntity blockEntity)
    {
        TriggerBlockEntityUpdateCallback.EVENT.invoker().update(blockEntity);
    }

    @Override
    public void readNbt(NbtCompound nbt)
    {
        super.readNbt(nbt);
        
        if (nbt.contains("Left")) this.left.fromData(DataStorageUtils.fromNbt(nbt.get("Left")));
        if (nbt.contains("Right")) this.right.fromData(DataStorageUtils.fromNbt(nbt.get("Right")));
        if (nbt.contains("Collidable")) this.collidable.set(nbt.getBoolean("Collidable"));
        if (nbt.contains("Pos1")) this.pos1.fromData(DataStorageUtils.fromNbt(nbt.get("Pos1")));
        if (nbt.contains("Pos2")) this.pos2.fromData(DataStorageUtils.fromNbt(nbt.get("Pos2")));
    }

    @Override
    public void writeNbt(NbtCompound nbt)
    {
        super.writeNbt(nbt);
        
        nbt.put("Left", DataStorageUtils.toNbt(this.left.toData()));
        nbt.put("Right", DataStorageUtils.toNbt(this.right.toData()));
        nbt.putBoolean("Collidable", this.collidable.get());
        nbt.put("Pos1", DataStorageUtils.toNbt(this.pos1.toData()));
        nbt.put("Pos2", DataStorageUtils.toNbt(this.pos2.toData()));
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket()
    {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt()
    {
        return this.createNbt();
    }
}
