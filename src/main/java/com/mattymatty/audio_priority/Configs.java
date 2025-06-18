package com.mattymatty.audio_priority;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.sound.SoundCategory;

import java.io.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Configs implements Serializable {

    private static Configs instance = new Configs();
    public final Map<String, Integer> categoryClasses = new HashMap<>();
    public final Map<String, Float> maxPercentPerCategory = new HashMap<>();
    public final Set<String> instantCategories = new HashSet<>();
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public final Set<String> mutedSounds = new HashSet<>();
    public final Map<String, Float> soundVolumes = new HashMap<>();
    public Integer maxDuplicatedSoundsByPos;
    public Integer maxDuplicatedSoundsById;

    Configs() {
        categoryClasses.put(SoundCategory.MASTER.getName(), 0);
        categoryClasses.put(SoundCategory.VOICE.getName(), 0);
        categoryClasses.put(SoundCategory.PLAYERS.getName(), 6);
        categoryClasses.put(SoundCategory.HOSTILE.getName(), 5);
        categoryClasses.put(SoundCategory.BLOCKS.getName(), 6);
        categoryClasses.put(SoundCategory.MUSIC.getName(), 3);
        categoryClasses.put(SoundCategory.RECORDS.getName(), 3);
        categoryClasses.put(SoundCategory.NEUTRAL.getName(), 2);
        categoryClasses.put(SoundCategory.WEATHER.getName(), 1);
        categoryClasses.put(SoundCategory.AMBIENT.getName(), 1);


        maxPercentPerCategory.put(SoundCategory.MASTER.getName(), 1f);
        maxPercentPerCategory.put(SoundCategory.VOICE.getName(), 1f);
        maxPercentPerCategory.put(SoundCategory.PLAYERS.getName(), 0.95f);
        maxPercentPerCategory.put(SoundCategory.HOSTILE.getName(), 0.9f);
        maxPercentPerCategory.put(SoundCategory.BLOCKS.getName(), 0.8f);
        maxPercentPerCategory.put(SoundCategory.MUSIC.getName(), 0.7f);
        maxPercentPerCategory.put(SoundCategory.RECORDS.getName(), 0.7f);
        maxPercentPerCategory.put(SoundCategory.NEUTRAL.getName(), 0.6f);
        maxPercentPerCategory.put(SoundCategory.WEATHER.getName(), 0.5f);
        maxPercentPerCategory.put(SoundCategory.AMBIENT.getName(), 0.5f);

        instantCategories.add(SoundCategory.MASTER.getName());
        instantCategories.add(SoundCategory.MUSIC.getName());

        maxDuplicatedSoundsByPos = 5;
        maxDuplicatedSoundsById = 50;
    }

    public static Configs getInstance() {
        return instance;
    }

    public static void saveConfig() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        mapper.writerWithDefaultPrettyPrinter().writeValue(FabricLoader.getInstance().getConfigDir().resolve("audio_engine.json").toFile(), instance);
    }

    public static void loadConfig() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        instance = mapper.readerFor(Configs.class).readValue(FabricLoader.getInstance().getConfigDir().resolve("audio_engine.json").toFile());

        for (String id : instance.mutedSounds){
            instance.soundVolumes.put(id, 0f);
        }

    }

}
