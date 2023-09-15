package com.mattymatty.audio_priority.mixins.accessors;

import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(SoundManager.class)
public interface SoundManagerAccessor {
    @Accessor
    Map<Identifier, WeightedSoundSet> getSounds();
}
