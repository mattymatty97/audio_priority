package com.mattymatty.audio_priority.mixins.accessors;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Screen.class)
public interface ScreenAccessor {
    @Accessor
    int getWidth();
    @Accessor
    int getHeight();

    @Accessor
    MinecraftClient getClient();

}
