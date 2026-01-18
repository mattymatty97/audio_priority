package com.mattymatty.audio_priority.mixins.accessors;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.Identifier;

@Mixin(SoundManager.class)
public interface SoundManagerAccessor {
    @Accessor
    Map<Identifier, WeighedSoundEvents> getRegistry();
}
