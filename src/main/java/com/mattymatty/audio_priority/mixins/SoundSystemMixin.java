package com.mattymatty.audio_priority.mixins;

import com.mattymatty.audio_priority.Configs;
import com.mattymatty.audio_priority.client.AudioPriority;
import com.mattymatty.audio_priority.exceptions.SoundPoolException;
import com.mattymatty.audio_priority.mixins.accessors.SoundEngineAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.*;
import net.minecraft.entity.Entity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;


@Mixin(SoundSystem.class)
public abstract class SoundSystemMixin {
    private final Map<SoundCategory, AtomicInteger> skippedByCategory = new HashMap<>();
    private final Map<Vec3d, Map<Identifier, AtomicInteger>> playedByPos = new HashMap<>();
    Map<Integer, Set<SoundInstance>> soundsPerTick = new TreeMap<>();
    @Shadow
    private int ticks;
    @Shadow
    @Final
    private Map<SoundInstance, Integer> startTicks;
    @Shadow
    @Final
    private SoundEngine soundEngine;

    private static int sound_comparator(SoundInstance sound) {
        Vec3d playerPos = null;
        Entity client = MinecraftClient.getInstance().player;
        if (client != null) {
            playerPos = client.getPos();
        }

        int category = Configs.getInstance().categoryClasses.getOrDefault(sound.getCategory(), Configs.getInstance().categoryClasses.size());

        int tie_break = 1;

        if (playerPos != null) {
            //nearest sounds get a higher priority
            tie_break *= playerPos.distanceTo(new Vec3d(sound.getX(), sound.getY(), sound.getZ()));
        }

        return category * 10000 + Math.min(tie_break, 9999);
    }

    @Shadow
    public abstract void play(SoundInstance sound);

    @Shadow
    public abstract void play(SoundInstance sound, int delay);

    private Set<SoundInstance> getSoundList(int tick) {
        return soundsPerTick.computeIfAbsent(tick, k -> new LinkedHashSet<>());
    }

    //thorw an exception instead of just a log message ( allows me to skip successive play calls instead of spamming the logs )
    @Redirect(method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At(value = "INVOKE", target = "Ljava/util/concurrent/CompletableFuture;join()Ljava/lang/Object;"))
    Object except_on_full_sound_pool(CompletableFuture<Object> instance) {
        Object ret = instance.join();
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

    @Inject(method = "tick()V", at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILEXCEPTION)
    void schedule_repeating_play(CallbackInfo ci, Iterator iterator, Map.Entry entry, Channel.SourceManager sourceManager, SoundInstance soundInstance) {
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
                .sorted(Comparator.comparing(SoundSystemMixin::sound_comparator))
                .toList();

        long total = instances.size();
        long count = 0;

        Iterator<SoundInstance> iterator = instances.iterator();

        try {
            while (iterator.hasNext()) {
                SoundInstance soundInstance = iterator.next();
                if (soundInstance instanceof TickableSoundInstance) {
                    ((TickableSoundInstance) soundInstance).tick();
                }
                this.play(soundInstance);
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

        //log the amount of skipped sounds ( debug only )
        //TODO: remove it as might cause memory leaks
        if (skippedByCategory.size() > 0) {
            for (Map.Entry<SoundCategory, AtomicInteger> entry : skippedByCategory.entrySet()) {
                AudioPriority.LOGGER.debug("Skipped {} sounds for {} category", entry.getValue().get(),
                        entry.getKey().getName());
            }
            skippedByCategory.clear();
        }

        //clear the duplication list
        playedByPos.forEach((k, m) -> m.clear()); //try and clear also potential memory leaks
        playedByPos.clear();

        //remove due ticks from sound queue
        for (Integer key : tickKeys) {
            soundsPerTick.remove(key);
        }
        //do not run vanilla code
        ci.cancel();
    }

    //decide if to actually play or not a sound
    @Inject(cancellable = true, method = "play(Lnet/minecraft/client/sound/SoundInstance;)V", at = @At(value = "INVOKE_ASSIGN", ordinal = 0, target = "Lnet/minecraft/client/sound/Sound;isStreamed()Z"))
    void should_play_sound(SoundInstance sound, CallbackInfo ci) {
        SoundEngine.SourceSet streamingSources = ((SoundEngineAccessor) this.soundEngine).getStreamingSources();
        SoundEngine.SourceSet staticSources = ((SoundEngineAccessor) this.soundEngine).getStaticSources();
        if (!should_play(sound, (sound.getSound().isStreamed()) ?/*WTF Mojang inverts the naming later*/staticSources : streamingSources)) {
            ci.cancel();
        }
    }

    //all maps get reset each sound engine tick
    private boolean should_play(SoundInstance sound, SoundEngine.SourceSet dest) {

        //sounds that can be played outside the tick need to skip the duplication check
        if (!Configs.getInstance().instantCategories.contains(sound.getCategory())) {
            //get duplicate map for this sound location ( Block Position )
            Map<Identifier, AtomicInteger> played_sounds = this.playedByPos.computeIfAbsent(
                    new Vec3d(Math.round(sound.getX()), Math.round(sound.getY()), Math.round(sound.getZ())),
                    (i) -> new HashMap<>());

            AtomicInteger count = played_sounds.computeIfAbsent(sound.getId(), (i) -> new AtomicInteger());

            //if there are too many duplicated sounds skip playing them
            if (count.getAndIncrement() > Configs.getInstance().maxDuplicatedSounds) {
                //if (!played_sounds.add(sound.getId())){
                AudioPriority.LOGGER.debug("Duplicated Sound {} at {} {} {}, Skipped",
                        sound.getId(),
                        sound.getX(),
                        sound.getY(),
                        sound.getZ());
                skippedByCategory.computeIfAbsent(
                        sound.getCategory(),
                        k -> new AtomicInteger()
                ).incrementAndGet();
                return false;
            }
        }

        int sound_count = dest.getSourceCount();
        int max_count = dest.getMaxSourceCount();
        double percentage = Configs.getInstance().maxPercentPerCategory.getOrDefault(sound.getCategory(), 0d);
        // check the sound pool fill level and compare it to the threshold for the current category
        boolean ret = (sound_count < (max_count) * percentage);
        if (!ret) {
            AudioPriority.LOGGER.debug("Sound pool level {}% too high for {} sounds, Skipped",
                    (sound_count / (float) max_count) * 100,
                    sound.getCategory().getName());
            //log the amount of skipped sounds ( debug only )
            //TODO: remove it as might cause memory leaks
            skippedByCategory.computeIfAbsent(
                    sound.getCategory(),
                    k -> new AtomicInteger()
            ).incrementAndGet();
        }
        return ret;
    }
}
