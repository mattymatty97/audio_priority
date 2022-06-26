package com.mattymatty.audio_priority.client;

import com.mattymatty.audio_priority.screen.ConfigScreen;
import io.github.prospector.modmenu.api.ConfigScreenFactory;
import io.github.prospector.modmenu.api.ModMenuApi;

public class AudioPriorityModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return parent -> {

            return new ConfigScreen(parent);
        };
    }
}
