package com.mattymatty.audio_priority.mixins.accessors;

import com.mojang.blaze3d.audio.Library;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Library.class)
public interface SoundEngineAccessor {
    @Accessor
    Library.ChannelPool getStaticChannels();

    @Accessor
    Library.ChannelPool getStreamingChannels();
}
