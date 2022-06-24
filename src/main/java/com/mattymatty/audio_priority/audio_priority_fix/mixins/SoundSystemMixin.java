package com.mattymatty.audio_priority.audio_priority_fix.mixins;

import com.google.common.collect.Multimap;
import com.mattymatty.audio_priority.audio_priority_fix.mixins.accessors.SoundEngineAccessor;
import com.mattymatty.audio_priority.audio_priority_fix.exceptions.SoundPoolException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.*;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;


@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {
    @Shadow private int ticks;

    @Shadow public abstract void play(SoundInstance sound);

    @Shadow @Final private Map<SoundInstance, Integer> startTicks;

    @Shadow public abstract void play(SoundInstance sound, int delay);

    @Shadow @Final private static Logger LOGGER;
    @Shadow @Final private SoundEngine soundEngine;
    @Shadow @Final private Multimap<SoundCategory, SoundInstance> sounds;
    Map<Integer, Set<SoundInstance>> soundsPerTick = new TreeMap<>();

    private Set<SoundInstance> getSoundList(int tick){
        return soundsPerTick.computeIfAbsent(tick, k -> new LinkedHashSet<>());
    }

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;I)V", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    void schedule_play(SoundInstance sound, int delay, CallbackInfo ci){
        this.getSoundList(this.ticks + delay).add(sound);
    }

    @ModifyArg(method = "tick()V", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;forEach(Ljava/util/function/Consumer;)V"))
    Consumer<SoundInstance> next_tick_play(Consumer<SoundInstance> action){
        return (sound) -> this.play(sound, 0);
    }


    @Inject(method = "tick()V", at=@At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    void schedule_repeating_play(CallbackInfo ci, Iterator iterator, Map.Entry entry, Channel.SourceManager sourceManager, SoundInstance soundInstance){
        this.getSoundList(this.ticks + soundInstance.getRepeatDelay()).add(soundInstance);
    }

    @Inject(method = "tick()V", at=@At(value = "INVOKE", target = "Ljava/util/Set;iterator()Ljava/util/Iterator;", ordinal = 1, shift = At.Shift.BEFORE), cancellable = true)
    void play_current_tick_sounds(CallbackInfo ci){
        Set<Integer> tickKeys = soundsPerTick.keySet().stream().filter(i -> i < this.ticks).collect(Collectors.toSet());
        List<SoundInstance> instances = soundsPerTick.entrySet().stream()
                .filter(e -> tickKeys.contains(e.getKey()))
                .flatMap(e -> e.getValue().stream())
                .distinct()
                .sorted(Comparator.comparing(SoundSystemMixin::sound_comparator))
                .toList();
        Iterator<SoundInstance> iterator = instances.iterator();

        int count = 0;
        try {
            while (iterator.hasNext()) {
                SoundInstance soundInstance = iterator.next();
                if (soundInstance instanceof TickableSoundInstance) {
                    ((TickableSoundInstance) soundInstance).tick();
                }
                this.play(soundInstance);
                count++;
                this.startTicks.remove(soundInstance);
            }
        }catch ( SoundPoolException ex){
            SoundSystemMixin.LOGGER.warn("Sound pool full, Skipped {} sound events", instances.size() - count);
            instances.forEach(startTicks::remove);
        }
        for (Integer key : tickKeys) {
            soundsPerTick.remove(key);
        }
        ci.cancel();
    }


    @Inject(cancellable = true, method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at=@At(value = "INVOKE_ASSIGN",ordinal = 0, target = "Lnet/minecraft/client/sound/Sound;isStreamed()Z"))
    void should_play_sound(SoundInstance sound, CallbackInfo ci){
        SoundEngine.SourceSet streamingSources = ((SoundEngineAccessor) this.soundEngine).getStreamingSources();
        SoundEngine.SourceSet staticSources = ((SoundEngineAccessor) this.soundEngine).getStaticSources();
        if (!should_play(sound,(sound.getSound().isStreamed())?staticSources:streamingSources)){
            SoundSystemMixin.LOGGER.warn("Sound pool level too high for {} sounds, Skipped", sound.getCategory().getName());
            ci.cancel();
        }
    }

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At(value = "INVOKE", target = "Lorg/apache/logging/log4j/Logger;warn(Ljava/lang/String;)V"))
    void except_on_full_sound_pool(SoundInstance sound, CallbackInfo ci){
        throw new SoundPoolException();
    }


    private static int sound_comparator(SoundInstance sound){
        Vec3d playerPos = null;
        Entity client = MinecraftClient.getInstance().player;
        if (client != null){
            playerPos = client.getPos();
        }
        int category = 0;
        switch (sound.getCategory()){
            case MASTER:
            case VOICE:
                break;
            case PLAYERS:
                category = 1;
                break;
            case HOSTILE:
                category = 2;
                break;
            case BLOCKS:
                category = 3;
                break;
            case MUSIC:
            case RECORDS:
                category = 4;
                break;
            case NEUTRAL:
                category = 5;
                break;
            case WEATHER:
            case AMBIENT:
                category = 6;
                break;
            default:
                category = 7;
        }

        int tie_break = 1;

        if (playerPos != null){
            //nearest sounds get a higher priority
            tie_break *= playerPos.distanceTo(new Vec3d(sound.getX(),sound.getY(),sound.getZ()));
        }

        return category*10000 + Math.min(tie_break,9999);
    }


    private static boolean should_play(SoundInstance sound, SoundEngine.SourceSet dest){
        int comparator = sound_comparator(sound);
        int sound_count = dest.getSourceCount();
        int max_count = dest.getMaxSourceCount();
        if (comparator < 10000 ) // category 0
            return true;         // always
        if (comparator < 20000 ) // category 1
            return (sound_count < (max_count)*(95f/100)); //95%
        if (comparator < 30000 ) //category 2
            return (sound_count < (max_count)*(9f/10));   //90%
        if (comparator < 40000 ) //category 3
            return (sound_count < (max_count)*(8f/10));   //80%
        if (comparator < 50000 ) //category 4
            return (sound_count < (max_count)*(5f/10));   //50%
        if (comparator < 60000 ) //category 5
            return (sound_count < (max_count)*(4f/10));   //40%
        if (comparator < 70000 ) //category 6
            return (sound_count < (max_count)*(2f/10));   //40%
        // no category
        return (sound_count < (max_count)*(1f/10)); //10%
    }

}
