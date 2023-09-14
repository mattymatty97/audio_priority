package com.mattymatty.audio_priority;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
    public final Map<String, Double> maxPercentPerCategory = new HashMap<>();
    public final Set<String> instantCategories = new HashSet<>();
    public Integer maxDuplicatedSoundsByPos;
    public Integer maxDuplicatedSoundsById;

    Configs() {
        categoryClasses.put(SoundCategory.MASTER.getName(), 0);
        categoryClasses.put(SoundCategory.VOICE.getName(), 0);
        categoryClasses.put(SoundCategory.PLAYERS.getName(), 1);
        categoryClasses.put(SoundCategory.HOSTILE.getName(), 2);
        categoryClasses.put(SoundCategory.BLOCKS.getName(), 3);
        categoryClasses.put(SoundCategory.MUSIC.getName(), 4);
        categoryClasses.put(SoundCategory.RECORDS.getName(), 4);
        categoryClasses.put(SoundCategory.NEUTRAL.getName(), 5);
        categoryClasses.put(SoundCategory.WEATHER.getName(), 6);
        categoryClasses.put(SoundCategory.AMBIENT.getName(), 6);


        maxPercentPerCategory.put(SoundCategory.MASTER.getName(), 1d);
        maxPercentPerCategory.put(SoundCategory.VOICE.getName(), 1d);
        maxPercentPerCategory.put(SoundCategory.PLAYERS.getName(), 0.95d);
        maxPercentPerCategory.put(SoundCategory.HOSTILE.getName(), 0.9d);
        maxPercentPerCategory.put(SoundCategory.BLOCKS.getName(), 0.8d);
        maxPercentPerCategory.put(SoundCategory.MUSIC.getName(), 0.7d);
        maxPercentPerCategory.put(SoundCategory.RECORDS.getName(), 0.7d);
        maxPercentPerCategory.put(SoundCategory.NEUTRAL.getName(), 0.6d);
        maxPercentPerCategory.put(SoundCategory.WEATHER.getName(), 0.5d);
        maxPercentPerCategory.put(SoundCategory.AMBIENT.getName(), 0.5d);

        instantCategories.add(SoundCategory.MASTER.getName());
        instantCategories.add(SoundCategory.MUSIC.getName());

        maxDuplicatedSoundsByPos = 5;
        maxDuplicatedSoundsById = 50;
    }

    public static Configs getInstance() {
        return instance;
    }

    public static void saveConfig() throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(FabricLoader.getInstance().getConfigDir().resolve("audio_engine.json").toFile())) {
            writer.write(gson.toJson(getInstance()));
        }
    }

    public static void loadConfig() throws FileNotFoundException {
        Gson gson = new Gson();
        instance = gson.fromJson(new FileReader(FabricLoader.getInstance().getConfigDir().resolve("audio_engine.json").toFile()), Configs.class);
    }

}
