package com.mattymatty.audio_priority.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.mattymatty.audio_priority.Configs;
import com.mattymatty.audio_priority.client.AudioPriority;
import com.mattymatty.audio_priority.exceptions.SoundPoolException;
import com.mattymatty.audio_priority.mixins.accessors.SoundEngineAccessor;
import com.mojang.blaze3d.audio.Library;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.ChannelAccess;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.core.Vec3i;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;


@Mixin(SoundEngine.class)
public abstract class SoundSystemMixin {

    @Unique
    private final Object memoryLock = new Object();

    @Unique
    Map<Integer, Set<SoundInstance>> soundsPerTick = new TreeMap<>();

    @Shadow
    private int tickCount;

    @Shadow
    @Final
    private Map<SoundInstance, Integer> queuedSounds;

    @Shadow
    @Final
    private Library library;

    @Shadow
    @Final
    private Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel;

    @Unique
    private static int sound_comparator(SoundInstance sound) {
        Vec3 playerPos = null;
        int category   = 0;
        Entity client = Minecraft.getInstance().player;
        if (client != null) {
            playerPos = client.position();
        }

        final SoundSource source = sound.getSource();

        if (source != SoundSource.MASTER && source != SoundSource.UI)
            category = Configs.getInstance().categoryClasses.getOrDefault(sound.getSource().getName(), SoundSource.values().length);

        int tie_break = 1;

        if (playerPos != null) {
            //nearest sounds get a higher priority
            tie_break *= (int)playerPos.distanceTo(new Vec3(sound.getX(), sound.getY(), sound.getZ()));
        }

        return category * 10000 + Math.min(tie_break, 9999);
    }

    @Shadow
    public abstract SoundEngine.PlayResult play(SoundInstance sound);

    @Shadow
    public abstract void playDelayed(SoundInstance sound, int delay);

    @Unique
    private Set<SoundInstance> getSoundList(int tick) {
        return soundsPerTick.computeIfAbsent(tick, k -> new LinkedHashSet<>());
    }

    @WrapOperation(method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;join()Ljava/lang/Object;"))
    Object onAcquireSourceManager(CompletableFuture<Object> instance, Operation<Object> original, SoundInstance sound) {
        Object ret = original.call(instance);
        //thorw an exception instead of just a log message ( allows me to skip successive play calls instead of spamming the logs )
        if (ret == null)
            throw new SoundPoolException();

        return ret;
    }

    @ModifyArg(method = "tickInGameSound()V", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;forEach(Ljava/util/function/Consumer;)V"))
    Consumer<SoundInstance> next_tick_play(Consumer<SoundInstance> action) {
        //do not play the sound immediately but schedule it to the current tick ( force it to use priority system )
        return (sound) -> this.playDelayed(sound, -1);
    }

    @Inject(method = "playDelayed(Lnet/minecraft/client/resources/sounds/SoundInstance;I)V", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    void schedule_play(SoundInstance sound, int delay, CallbackInfo ci) {
        //append scheduled sounds to my queue
        this.getSoundList(this.tickCount + delay).add(sound);
    }

    @Inject(method = "tickInGameSound()V", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.AFTER))
    void schedule_repeating_play(CallbackInfo ci,@Local SoundInstance soundInstance) {
        //append scheduled sounds to my queue
        this.getSoundList(this.tickCount + soundInstance.getDelay()).add(soundInstance);
    }

    //use my queue to play sounds, ordering them with the priorities
    @Inject(method = "tickInGameSound()V", at = @At(value = "INVOKE", target = "Ljava/util/Set;iterator()Ljava/util/Iterator;", ordinal = 1, shift = At.Shift.BEFORE), cancellable = true)
    void play_current_tick_sounds(CallbackInfo ci) {
        //list due tick values
        Set<Integer> tickKeys = soundsPerTick.keySet().stream().filter(i -> i < this.tickCount).collect(Collectors.toSet());
        //get all sounds to be played and order them
        List<SoundInstance> instances = soundsPerTick.entrySet().stream()
                .filter(e -> tickKeys.contains(e.getKey()))
                .flatMap(e -> e.getValue().stream())
                .distinct()
                .sorted(Comparator.comparingInt(SoundSystemMixin::sound_comparator))
                .toList();

        long total = instances.size();
        long count = 0;

        Iterator<SoundInstance> iterator = instances.iterator();

        try {
            while (iterator.hasNext()) {
                SoundInstance soundInstance = iterator.next();
                this.play(soundInstance);
                if (soundInstance instanceof TickableSoundInstance) {
                    ((TickableSoundInstance) soundInstance).tick();
                }
                count++;
                //remove them from vanilla queue too
                this.queuedSounds.remove(soundInstance);
            }
        } catch (SoundPoolException ex) {
            //this should not be called anymore cause the play method now uses a threshold to decide whenever to actually play a sound or skip it
            AudioPriority.LOGGER.warn("Sound pool full, Skipped {} sound events", total - count);
            //remove all missing from vanilla queue ( full skip )
            instances.forEach(queuedSounds::remove);
        }

        //remove due ticks from sound queue
        for (Integer key : tickKeys) {
            soundsPerTick.remove(key).clear();
        }
        //do not run vanilla code
        ci.cancel();
    }

    //decide if to actually play or not a sound
    @Inject(cancellable = true, method = "play(Lnet/minecraft/client/resources/sounds/SoundInstance;)Lnet/minecraft/client/sounds/SoundEngine$PlayResult;", at = @At(value = "INVOKE_ASSIGN", ordinal = 0, target = "Lnet/minecraft/client/resources/sounds/Sound;shouldStream()Z"))
    void should_play_sound(SoundInstance sound, CallbackInfoReturnable<SoundEngine.PlayResult> cir, @Local(ordinal = 1) LocalFloatRef volume) {
        if (sound == null)
            return;
        if (sound.getSound() == null)
            return;

        Library.ChannelPool streamingSources = ((SoundEngineAccessor) this.library).getStaticChannels();
        Library.ChannelPool staticSources = ((SoundEngineAccessor) this.library).getStreamingChannels();

        float volumeMultiplier = Configs.getInstance().soundVolumes.getOrDefault(sound.getIdentifier().toString(), 1f);
        //WTF the mappings use inverted naming for some reason (@see Lnet/minecraft/client/sound/SoundEngine;createSource(Lnet/minecraft/client/sound/SoundEngine$RunMode;)Lnet/minecraft/client/sound/Source;)
        Library.ChannelPool sourceSet = (sound.getSound().shouldStream()) ? staticSources : streamingSources;
        if (volumeMultiplier <= 0f || !should_play(sound, sourceSet)) {
            cir.setReturnValue(SoundEngine.PlayResult.STARTED_SILENTLY);
            cir.cancel();
        }else{
            volume.set(volume.get() * volumeMultiplier);
        }
    }

    //all maps get reset each sound engine tick
    @Unique
    private boolean should_play(SoundInstance sound, Library.ChannelPool dest) {
        if (sound == null)
            return false;

        final SoundSource source = sound.getSource();
        //sounds that can be played outside the tick need to skip the duplication check
        if (    source != SoundSource.MASTER &&
                source != SoundSource.UI &&
                !Configs.getInstance().instantCategories.contains(source.getName())
        ) {
            var id = sound.getIdentifier();
            var here = new Vec3i((int) sound.getX(), (int) sound.getY(), (int) sound.getZ());
            var positionCount = 0;
            var identifierCount = 0;

            synchronized (memoryLock) {
                //get duplicate map for this sound location ( Block Position )
                for (var pair : this.instanceToChannel.entrySet()) {
                    var soundInstance = pair.getKey();
                    var manager = pair.getValue();

                    if (manager.isStopped())
                        continue;

                    if (soundInstance.getIdentifier() != id)
                        continue;

                    identifierCount++;

                    if (here.equals(new Vec3i((int)soundInstance.getX(), (int)soundInstance.getY(), (int)soundInstance.getZ())))
                        positionCount++;
                }
            }

            //if there are too many duplicated sounds skip playing them
            if (positionCount >= Configs.getInstance().maxDuplicatedSoundsByPos) {
                AudioPriority.LOGGER.debug("Duplicated Sound {} at {} {} {}, Skipped",
                        sound.getIdentifier(),
                        sound.getX(),
                        sound.getY(),
                        sound.getZ());
                return false;
            }

            if (identifierCount >= Configs.getInstance().maxDuplicatedSoundsById) {
                AudioPriority.LOGGER.debug("Duplicated Sound Id {}, Skipped",
                        sound.getIdentifier());
                return false;
            }

        }

        int sound_count = dest.getUsedCount();
        int max_count = dest.getMaxCount();
        float percentage = 1.f;
        if (source != SoundSource.MASTER && source != SoundSource.UI)
            percentage = Configs.getInstance().maxPercentPerCategory.getOrDefault(sound.getSource().getName(), 0.1f);

        // check the sound pool fill level and compare it to the threshold for the current category
        boolean ret = (sound_count < (max_count) * percentage);
        if (!ret) {
            AudioPriority.LOGGER.debug("Sound pool level {}% too high for {} sounds, Skipped",
                    (sound_count / (float) max_count) * 100,
                    sound.getSource().getName());
        }
        return ret;
    }

}
