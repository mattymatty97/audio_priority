package com.mattymatty.audio_priority.screen;

import com.mattymatty.audio_priority.Configs;
import com.mattymatty.audio_priority.client.AudioPriority;
import java.io.IOException;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {

    protected final Screen parent;

    public ConfigScreen(Screen parent) {
        this(parent, Component.literal("Audio Engine Tweaks Configs"));
    }

    public ConfigScreen(Screen parent, Component title) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        assert this.minecraft != null;
        super.init();
        this.addRenderableWidget(Button.builder(Component.literal("Sound Category Priorities"), button -> this.minecraft.setScreen(new CategoryConfigScreen(this)))
                .bounds(this.width / 2 - 75, this.height / 6 + 48 - 6, 150, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Thresholds"), button -> this.minecraft.setScreen(new ThresholdConfigScreen(this)))
                .bounds(this.width / 2 - 75, this.height / 6 + 72 - 6, 150, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Instant Categories"), button -> this.minecraft.setScreen(new InstantConfigScreen(this)))
                .bounds(this.width / 2 - 75, this.height / 6 + 96 - 6, 150, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Sound List"), button -> this.minecraft.setScreen(new SoundListConfigScreen(this)))
                .bounds(this.width / 2 - 75, this.height / 6 + 120 - 6, 150, 20).build());
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.minecraft.setScreen(this.parent))
                .bounds(this.width / 2 - 100, this.height / 6 + 168, 200, 20).build());
    }


    @Override
    public void removed() {
        try {
            Configs.saveConfig();
        } catch (IOException e) {
            AudioPriority.LOGGER.error("Exception Saving Config file");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
    }
}
