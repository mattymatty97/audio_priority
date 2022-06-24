package com.mattymatty.audio_priority.audio_priority_fix.mixins.accessors;

import net.minecraft.client.sound.SoundEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(SoundEngine.class)
public interface SoundEngineAccessor {
    @Accessor
    SoundEngine.SourceSet getStreamingSources();
    @Accessor
    SoundEngine.SourceSet getStaticSources();
}
