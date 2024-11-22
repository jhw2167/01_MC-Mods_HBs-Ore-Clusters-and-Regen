package com.holybuckets.foundation.model;

import com.holybuckets.foundation.HolyBucketsUtility;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ManagedChunkCapabilityProvider implements ICapabilityProvider, INBTSerializable<CompoundTag> {

    public static Capability<ManagedChunk> MANAGED_CHUNK = CapabilityManager.get(new CapabilityToken<ManagedChunk>() { });

    private ManagedChunk managedChunk = null;
    private final LazyOptional<ManagedChunk> optional = LazyOptional.of(this::getManagedChunk);

    public ManagedChunkCapabilityProvider(LevelChunk chunk)
    {
        super();
        String id = HolyBucketsUtility.ChunkUtil.getId(chunk.getPos());
        this.init(chunk.getLevel(), id);
    }

    public ManagedChunk init(LevelAccessor level, String id) {
        if(this.managedChunk == null)
            this.managedChunk = new ManagedChunk(level, id);
        return this.managedChunk;
    }

    public ManagedChunk getManagedChunk() {
        return this.managedChunk;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        if(this.managedChunk == null)
            return tag;

        tag.put("managedChunk", this.managedChunk.serializeNBT());
        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
    CompoundTag tag = nbt.getCompound("managedChunk");
        if(this.managedChunk == null)
            this.managedChunk = new ManagedChunk(tag);
        this.managedChunk.deserializeNBT(tag);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> capability, @Nullable Direction direction) {
        return getCapability(capability);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
        if(cap == MANAGED_CHUNK) {
            return optional.cast();
        }
        return ICapabilityProvider.super.getCapability(cap);
    }
}
