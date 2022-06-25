package com.mattymatty.audio_priority.audio_priority_fix;

import net.minecraft.sound.SoundCategory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Configs {
    public static final Map<SoundCategory, Integer> categoryClasses = new HashMap<>();
    public static final Map<Integer, Float> maxPercentPerCategory = new HashMap<>();
    public static final Set<SoundCategory> instantCategories = new HashSet<>();

    public static Integer maxDuplicatedSounds;

    static {
        categoryClasses.put(SoundCategory.MASTER,0);
        categoryClasses.put(SoundCategory.VOICE,0);
        categoryClasses.put(SoundCategory.PLAYERS,1);
        categoryClasses.put(SoundCategory.HOSTILE,2);
        categoryClasses.put(SoundCategory.BLOCKS,3);
        categoryClasses.put(SoundCategory.MUSIC,4);
        categoryClasses.put(SoundCategory.RECORDS,4);
        categoryClasses.put(SoundCategory.NEUTRAL,5);
        categoryClasses.put(SoundCategory.WEATHER,6);
        categoryClasses.put(SoundCategory.AMBIENT,6);


        maxPercentPerCategory.put(0,1f);
        maxPercentPerCategory.put(1,0.95f);
        maxPercentPerCategory.put(2,0.9f);
        maxPercentPerCategory.put(3,0.8f);
        maxPercentPerCategory.put(4,0.7f);
        maxPercentPerCategory.put(5,0.6f);
        maxPercentPerCategory.put(6,0.5f);

        instantCategories.add(SoundCategory.MASTER);
        instantCategories.add(SoundCategory.MUSIC);

        maxDuplicatedSounds = 5;
    }

}
