package com.mattymatty.audio_priority.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalFloatRef;
import com.mattymatty.audio_priority.Configs;
import com.mattymatty.audio_priority.client.AudioPriority;
import com.mattymatty.audio_priority.exceptions.SoundPoolException;
import com.mattymatty.audio_priority.mixins.accessors.SoundEngineAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.*;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
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


@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {

    @Unique
    private final Object memoryLock = new Object();

    @Unique
    Map<Integer, Set<SoundInstance>> soundsPerTick = new TreeMap<>();

    @Shadow
    private int ticks;

    @Shadow
    @Final
    private Map<SoundInstance, Integer> soundStartTicks;

    @Shadow
    @Final
    private SoundEngine soundEngine;

    @Shadow
    @Final
    private Map<SoundInstance, Channel.SourceManager> sources;

    @Unique
    private static int sound_comparator(SoundInstance sound) {
        Vec3d playerPos = null;
        Entity client = MinecraftClient.getInstance().player;
        if (client != null) {
            playerPos = client.getEntityPos();
        }

        int category = Configs.getInstance().categoryClasses.getOrDefault(sound.getCategory().getName(), SoundCategory.values().length);

        int tie_break = 1;

        if (playerPos != null) {
            //nearest sounds get a higher priority
            tie_break *= (int)playerPos.distanceTo(new Vec3d(sound.getX(), sound.getY(), sound.getZ()));
        }

        return category * 10000 + Math.min(tie_break, 9999);
    }

    @Shadow
    public abstract SoundSystem.PlayResult play(SoundInstance sound);

    @Shadow
    public abstract void play(SoundInstance sound, int delay);

    @Unique
    private Set<SoundInstance> getSoundList(int tick) {
        return soundsPerTick.computeIfAbsent(tick, k -> new LinkedHashSet<>());
    }

    @WrapOperation(method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;join()Ljava/lang/Object;"))
    Object onAcquireSourceManager(CompletableFuture<Object> instance, Operation<Object> original, SoundInstance sound) {
        Object ret = original.call(instance);
        //thorw an exception instead of just a log message ( allows me to skip successive play calls instead of spamming the logs )
        if (ret == null)
            throw new SoundPoolException();

        return ret;
    }

    @ModifyArg(method = "tick()V", at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;forEach(Ljava/util/function/Consumer;)V"))
    Consumer<SoundInstance> next_tick_play(Consumer<SoundInstance> action) {
        //do not play the sound immediately but schedule it to the current tick ( force it to use priority system )
        return (sound) -> this.play(sound, -1);
    }

    @Inject(method = "play(Lnet/minecraft/client/sound/SoundInstance;I)V", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"))
    void schedule_play(SoundInstance sound, int delay, CallbackInfo ci) {
        //append scheduled sounds to my queue
        this.getSoundList(this.ticks + delay).add(sound);
    }

    @Inject(method = "tick()V", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.AFTER))
    void schedule_repeating_play(CallbackInfo ci,@Local SoundInstance soundInstance) {
        //append scheduled sounds to my queue
        this.getSoundList(this.ticks + soundInstance.getRepeatDelay()).add(soundInstance);
    }

    //use my queue to play sounds, ordering them with the priorities
    @Inject(method = "tick()V", at = @At(value = "INVOKE", target = "Ljava/util/Set;iterator()Ljava/util/Iterator;", ordinal = 1, shift = At.Shift.BEFORE), cancellable = true)
    void play_current_tick_sounds(CallbackInfo ci) {
        //list due tick values
        Set<Integer> tickKeys = soundsPerTick.keySet().stream().filter(i -> i < this.ticks).collect(Collectors.toSet());
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
                this.soundStartTicks.remove(soundInstance);
            }
        } catch (SoundPoolException ex) {
            //this should not be called anymore cause the play method now uses a threshold to decide whenever to actually play a sound or skip it
            AudioPriority.LOGGER.warn("Sound pool full, Skipped {} sound events", total - count);
            //remove all missing from vanilla queue ( full skip )
            instances.forEach(soundStartTicks::remove);
        }

        //remove due ticks from sound queue
        for (Integer key : tickKeys) {
            soundsPerTick.remove(key).clear();
        }
        //do not run vanilla code
        ci.cancel();
    }

    //decide if to actually play or not a sound
    @Inject(cancellable = true, method = "play(Lnet/minecraft/client/sound/SoundInstance;)Lnet/minecraft/client/sound/SoundSystem$PlayResult;", at = @At(value = "INVOKE_ASSIGN", ordinal = 0, target = "Lnet/minecraft/client/sound/Sound;isStreamed()Z"))
    void should_play_sound(SoundInstance sound, CallbackInfoReturnable<SoundSystem.PlayResult> cir, @Local(ordinal = 2) LocalFloatRef volume) {
        if (sound == null)
            return;
        if (sound.getSound() == null)
            return;

        SoundEngine.SourceSet streamingSources = ((SoundEngineAccessor) this.soundEngine).getStreamingSources();
        SoundEngine.SourceSet staticSources = ((SoundEngineAccessor) this.soundEngine).getStaticSources();

        float volumeMultiplier = Configs.getInstance().soundVolumes.getOrDefault(sound.getId().toString(), 1f);
        //WTF the mappings use inverted naming for some reason (@see Lnet/minecraft/client/sound/SoundEngine;createSource(Lnet/minecraft/client/sound/SoundEngine$RunMode;)Lnet/minecraft/client/sound/Source;)
        SoundEngine.SourceSet sourceSet = (sound.getSound().isStreamed()) ? staticSources : streamingSources;
        if (volumeMultiplier <= 0f || !should_play(sound, sourceSet)) {
            cir.setReturnValue(SoundSystem.PlayResult.STARTED_SILENTLY);
            cir.cancel();
        }else{
            volume.set(volume.get() * volumeMultiplier);
        }
    }

    //all maps get reset each sound engine tick
    @Unique
    private boolean should_play(SoundInstance sound, SoundEngine.SourceSet dest) {
        if (sound == null)
            return false;

        //sounds that can be played outside the tick need to skip the duplication check
        if (!Configs.getInstance().instantCategories.contains(sound.getCategory().getName())) {
            var id = sound.getId();
            var here = new Vec3i((int) sound.getX(), (int) sound.getY(), (int) sound.getZ());
            var positionCount = 0;
            var identifierCount = 0;

            synchronized (memoryLock) {
                //get duplicate map for this sound location ( Block Position )
                for (var pair : this.sources.entrySet()) {
                    var soundInstance = pair.getKey();
                    var manager = pair.getValue();

                    if (manager.isStopped())
                        continue;

                    if (soundInstance.getId() != id)
                        continue;

                    identifierCount++;

                    if (here.equals(new Vec3i((int)soundInstance.getX(), (int)soundInstance.getY(), (int)soundInstance.getZ())))
                        positionCount++;
                }
            }

            AudioPriority.LOGGER.warn("Sound {} at Pos {}\nlocation: {}\ncount: {}",
                    id, here, positionCount, identifierCount);

            //if there are too many duplicated sounds skip playing them
            if (positionCount >= Configs.getInstance().maxDuplicatedSoundsByPos) {
                AudioPriority.LOGGER.debug("Duplicated Sound {} at {} {} {}, Skipped",
                        sound.getId(),
                        sound.getX(),
                        sound.getY(),
                        sound.getZ());
                return false;
            }

            if (identifierCount >= Configs.getInstance().maxDuplicatedSoundsById) {
                AudioPriority.LOGGER.debug("Duplicated Sound Id {}, Skipped",
                        sound.getId());
                return false;
            }

        }

        int sound_count = dest.getSourceCount();
        int max_count = dest.getMaxSourceCount();
        float percentage = Configs.getInstance().maxPercentPerCategory.getOrDefault(sound.getCategory().getName(), 0.1f);
        // check the sound pool fill level and compare it to the threshold for the current category
        boolean ret = (sound_count < (max_count) * percentage);
        if (!ret) {
            AudioPriority.LOGGER.debug("Sound pool level {}% too high for {} sounds, Skipped",
                    (sound_count / (float) max_count) * 100,
                    sound.getCategory().getName());
        }
        return ret;
    }

}
