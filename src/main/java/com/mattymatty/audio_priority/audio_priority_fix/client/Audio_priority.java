package com.mattymatty.audio_priority.audio_priority_fix.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Environment(EnvType.CLIENT)
public class Audio_priority implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();
    @Override
    public void onInitializeClient() {
        LOGGER.info("Audio Priority Loaded!");
    }
}
