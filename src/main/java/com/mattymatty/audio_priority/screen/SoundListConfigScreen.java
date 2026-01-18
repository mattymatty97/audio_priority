package com.mattymatty.audio_priority.screen;

import com.google.common.collect.ImmutableList;
import com.mattymatty.audio_priority.Configs;
import com.mattymatty.audio_priority.client.AudioPriority;
import joptsimple.internal.Strings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class SoundListConfigScreen extends Screen {
    protected final Screen parent;

    protected EditBox searchBox;
    protected SoundListWidget soundList;

    public SoundListConfigScreen(Screen parent) {
        super(Component.literal("Sound List"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        assert this.minecraft != null;
        this.searchBox = new EditBox(this.font, this.width / 2 - 100, 22, 200, 20, this.searchBox, Component.literal("Search Sound"));
        this.searchBox.setResponder(search -> this.soundList.showSearch(search));
        this.soundList = new SoundListWidget(this.minecraft, this.width, this.height - 32 - 48, 48, this.font.lineHeight * 2 + 8, 470);
        this.soundList.showSearch(this.searchBox.getValue());
        this.addWidget(this.searchBox);
        this.addRenderableWidget(this.soundList);
        this.addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> this.minecraft.setScreen(this.parent)).bounds(this.width / 2 - 100, (this.height) - 28, 200, 20).build());
        this.setInitialFocus(this.searchBox);
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        this.soundList.renderWidget(context, mouseX, mouseY, delta);
        this.searchBox.render(context, mouseX, mouseY, delta);
        context.drawCenteredString(this.font, this.title, this.width / 2, 8, 16777215);

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
}
