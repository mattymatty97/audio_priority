package com.mattymatty.audio_priority.screen;

import com.mattymatty.audio_priority.Configs;
import com.mattymatty.audio_priority.client.AudioPriority;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.screen.ScreenTexts;

import java.io.IOException;

public class ConfigScreen extends Screen {

    protected final Screen parent;

    public ConfigScreen(Screen parent) {
        this(parent, Text.literal("Audio Engine Tweaks Configs"));
    }

    public ConfigScreen(Screen parent, Text title) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        assert this.client != null;
        super.init();
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Sound Category Priorities"), button -> this.client.setScreen(new CategoryConfigScreen(this)))
                .dimensions(this.width / 2 - 75, this.height / 6 + 48 - 6, 150, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Thresholds"), button -> this.client.setScreen(new ThresholdConfigScreen(this)))
                .dimensions(this.width / 2 - 75, this.height / 6 + 72 - 6, 150, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Instant Categories"), button -> this.client.setScreen(new InstantConfigScreen(this)))
                .dimensions(this.width / 2 - 75, this.height / 6 + 96 - 6, 150, 20).build());
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Muted Sounds"), button -> this.client.setScreen(new MuteConfigScreen(this)))
                .dimensions(this.width / 2 - 75, this.height / 6 + 120 - 6, 150, 20).build());
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> this.client.setScreen(this.parent))
                .dimensions(this.width / 2 - 100, this.height / 6 + 168, 200, 20).build());
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }
}
