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
    public final Map<SoundCategory, Integer> categoryClasses = new HashMap<>();
    public final Map<SoundCategory, Double> maxPercentPerCategory = new HashMap<>();
    public final Set<SoundCategory> instantCategories = new HashSet<>();
    public Integer maxDuplicatedSounds;

    Configs() {
        categoryClasses.put(SoundCategory.MASTER, 0);
        categoryClasses.put(SoundCategory.VOICE, 0);
        categoryClasses.put(SoundCategory.PLAYERS, 1);
        categoryClasses.put(SoundCategory.HOSTILE, 2);
        categoryClasses.put(SoundCategory.BLOCKS, 3);
        categoryClasses.put(SoundCategory.MUSIC, 4);
        categoryClasses.put(SoundCategory.RECORDS, 4);
        categoryClasses.put(SoundCategory.NEUTRAL, 5);
        categoryClasses.put(SoundCategory.WEATHER, 6);
        categoryClasses.put(SoundCategory.AMBIENT, 6);


        maxPercentPerCategory.put(SoundCategory.MASTER, 1d);
        maxPercentPerCategory.put(SoundCategory.VOICE, 1d);
        maxPercentPerCategory.put(SoundCategory.PLAYERS, 0.95d);
        maxPercentPerCategory.put(SoundCategory.HOSTILE, 0.9d);
        maxPercentPerCategory.put(SoundCategory.BLOCKS, 0.8d);
        maxPercentPerCategory.put(SoundCategory.MUSIC, 0.7d);
        maxPercentPerCategory.put(SoundCategory.RECORDS, 0.7d);
        maxPercentPerCategory.put(SoundCategory.NEUTRAL, 0.6d);
        maxPercentPerCategory.put(SoundCategory.WEATHER, 0.5d);
        maxPercentPerCategory.put(SoundCategory.AMBIENT, 0.5d);

        instantCategories.add(SoundCategory.MASTER);
        instantCategories.add(SoundCategory.MUSIC);

        maxDuplicatedSounds = 5;
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
