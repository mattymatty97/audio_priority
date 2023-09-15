package com.mattymatty.audio_priority.mixins;

import com.google.common.collect.Multimap;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mattymatty.audio_priority.Configs;
import com.mattymatty.audio_priority.client.AudioPriority;
import com.mattymatty.audio_priority.exceptions.SoundPoolException;
import com.mattymatty.audio_priority.mixins.accessors.SoundEngineAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.*;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;


@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {
    @Unique
    private final Map<Vec3d, Map<Identifier, AtomicInteger>> playedByPos = new HashMap<>();
    @Unique
    private final Map<Identifier, AtomicInteger> playedByIdentifier = new HashMap<>();
    @Unique
    Map<Integer, Set<SoundInstance>> soundsPerTick = new TreeMap<>();
    @Shadow
    private int ticks;
    @Shadow
    @Final
    private Map<SoundInstance, Integer> startTicks;
    @Shadow
    @Final
    private SoundEngine soundEngine;

    @Unique
    private static int sound_comparator(SoundInstance sound) {
        Vec3d playerPos = null;
        Entity client = MinecraftClient.getInstance().player;
        if (client != null) {
            playerPos = client.getPos();
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
    public abstract void play(SoundInstance sound);

    @Shadow
    public abstract void play(SoundInstance sound, int delay);

    @Shadow @Final private Multimap<SoundCategory, SoundInstance> sounds;

    @Shadow @Final private Map<SoundInstance, Integer> soundEndTicks;

    @Unique
    private Set<SoundInstance> getSoundList(int tick) {
        return soundsPerTick.computeIfAbsent(tick, k -> new LinkedHashSet<>());
    }

    //thorw an exception instead of just a log message ( allows me to skip successive play calls instead of spamming the logs )
    @WrapOperation(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;join()Ljava/lang/Object;"))
    Object except_on_full_sound_pool(CompletableFuture<Object> instance, Operation<Object> original) {
        Object ret = original.call(instance);
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

    //use my queue to play sounds ordering them with the priorities
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
                this.startTicks.remove(soundInstance);
            }
        } catch (SoundPoolException ex) {
            //this should not be called anymore cause the play method now uses a threshold to decide whenever to actually play a sound or skip it
            AudioPriority.LOGGER.warn("Sound pool full, Skipped {} sound events", total - count);
            //remove all missing from vanilla queue ( full skip )
            instances.forEach(startTicks::remove);
        }

        //remove due ticks from sound queue
        for (Integer key : tickKeys) {
            soundsPerTick.remove(key).clear();
        }
        //do not run vanilla code
        ci.cancel();
    }

    @Inject(method = "stopAll", at = @At("HEAD"))
    void stopAll(CallbackInfo ci){
        //clear the duplication list
        playedByPos.forEach((k, m) -> m.clear()); //try and clear also potential memory leaks
        playedByPos.clear();

        playedByIdentifier.clear();
    }

    @Inject(method = "tick()V", at = @At(value = "INVOKE", target = "Ljava/util/Map;remove(Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.AFTER))
    void tickStoppedPlaying(CallbackInfo ci, @Local SoundInstance sound){
        this.stopped_playing(sound);
    }

    @Inject(method = "stop(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At(value = "HEAD"))
    void stop_sound(SoundInstance sound, CallbackInfo ci){
        this.stopped_playing(sound);
    }

    //decide if to actually play or not a sound
    @Inject(cancellable = true, method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At(value = "INVOKE_ASSIGN", ordinal = 0, target = "Lnet/minecraft/client/sound/Sound;isStreamed()Z"))
    void should_play_sound(SoundInstance sound, CallbackInfo ci) {
        SoundEngine.SourceSet streamingSources = ((SoundEngineAccessor) this.soundEngine).getStreamingSources();
        SoundEngine.SourceSet staticSources = ((SoundEngineAccessor) this.soundEngine).getStaticSources();
        if (!should_play(sound, (sound.getSound().isStreamed()) ? /*WTF Mojang inverts the naming later*/ staticSources : streamingSources)) {
            ci.cancel();
        }
    }

    //all maps get reset each sound engine tick
    @Unique
    private boolean should_play(SoundInstance sound, SoundEngine.SourceSet dest) {
        //if sound is muted skip it
        if (Configs.getInstance().mutedSounds.contains(sound.getId().toString()))
            return false;

        //sounds that can be played outside the tick need to skip the duplication check
        if (!Configs.getInstance().instantCategories.contains(sound.getCategory().getName())) {
            //get duplicate map for this sound location ( Block Position )
            Map<Identifier, AtomicInteger> played_sounds = this.playedByPos.computeIfAbsent(
                    new Vec3d(Math.round(sound.getX()), Math.round(sound.getY()), Math.round(sound.getZ())),
                    (i) -> new HashMap<>());

            AtomicInteger posCount = played_sounds.computeIfAbsent(sound.getId(), (i) -> new AtomicInteger());

            AtomicInteger idCount = this.playedByIdentifier.computeIfAbsent(sound.getId(), (i) -> new AtomicInteger());

            //if there are too many duplicated sounds skip playing them
            if (posCount.getAndIncrement() > Configs.getInstance().maxDuplicatedSoundsByPos) {
                AudioPriority.LOGGER.debug("Duplicated Sound {} at {} {} {}, Skipped",
                        sound.getSound().getIdentifier(),
                        sound.getX(),
                        sound.getY(),
                        sound.getZ());
                return false;
            }

            if (idCount.getAndIncrement() > Configs.getInstance().maxDuplicatedSoundsById) {
                AudioPriority.LOGGER.debug("Duplicated Sound Id {}, Skipped",
                        sound.getId());
                return false;
            }
        }

        int sound_count = dest.getSourceCount();
        int max_count = dest.getMaxSourceCount();
        double percentage = Configs.getInstance().maxPercentPerCategory.getOrDefault(sound.getCategory().getName(), 0.1d);
        // check the sound pool fill level and compare it to the threshold for the current category
        boolean ret = (sound_count < (max_count) * percentage);
        if (!ret) {
            AudioPriority.LOGGER.debug("Sound pool level {}% too high for {} sounds, Skipped",
                    (sound_count / (float) max_count) * 100,
                    sound.getCategory().getName());
        }
        return ret;
    }

    @Unique
    private void stopped_playing(SoundInstance sound){
        Vec3d pos = new Vec3d(Math.round(sound.getX()), Math.round(sound.getY()), Math.round(sound.getZ()));
        Map<Identifier, AtomicInteger> played_sounds = this.playedByPos.get(pos);
        if (played_sounds != null) {
            AtomicInteger posCount = played_sounds.get(sound.getId());
            if (posCount != null && posCount.decrementAndGet() <= 0) {
                played_sounds.remove(sound.getId());
            }
            if (played_sounds.isEmpty())
                this.playedByPos.remove(pos);
        }

        AtomicInteger idCount = this.playedByIdentifier.get(sound.getId());
        if (idCount != null && idCount.decrementAndGet() <= 0)
            this.playedByIdentifier.remove(sound.getId());
    }

}
