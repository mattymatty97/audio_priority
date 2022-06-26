package com.mattymatty.audio_priority.client;

import com.mattymatty.audio_priority.Configs;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

@Environment(EnvType.CLIENT)
public class AudioPriority implements ClientModInitializer {
    public static final Logger LOGGER = LogManager.getLogger();

    @Override
    public void onInitializeClient() {
        LOGGER.info("Audio Priority Loaded!");
        try{
            Configs.loadConfig();
        }catch (IOException ex){
            LOGGER.warn("Missing Config File - creating default one");
            try {
                Configs.saveConfig();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
