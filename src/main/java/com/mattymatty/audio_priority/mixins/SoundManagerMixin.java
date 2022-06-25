package com.mattymatty.audio_priority.mixins;

import com.mattymatty.audio_priority.Configs;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.client.sound.SoundSystem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SoundManager.class)
public class SoundManagerMixin {

    @Redirect(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/sound/SoundSystem;play(Lnet/minecraft/client/sound/SoundInstance;)V"))
    void schedule_for_now(SoundSystem instance, SoundInstance sound){
        //allow specific sound categories to be played outside the tick ( bypassing the priority queue )
        if (Configs.instantCategories.contains(sound.getCategory()))
            instance.play(sound);
        else
            //otherwise force them to use the priority queue
            instance.play(sound, 0);
    }
}
