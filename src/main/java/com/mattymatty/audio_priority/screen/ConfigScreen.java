package com.mattymatty.audio_priority.screen;

import com.mattymatty.audio_priority.Configs;
import com.mattymatty.audio_priority.client.AudioPriority;
import java.io.IOException;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.GridLayout;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public class ConfigScreen extends Screen {

    private static final Component TITLE = Component.literal("Audio Engine Tweaks");
    protected final Screen parent;
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 61, 33);

    public ConfigScreen(Screen parent) {
        this(parent, Component.literal("Audio Engine Tweaks Configs"));
    }

    public ConfigScreen(Screen parent, Component title) {
        super(title);
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(TITLE, this.font);
        GridLayout gridLayout = new GridLayout();
        gridLayout.defaultCellSetting().paddingHorizontal(4).paddingBottom(4).alignHorizontallyCenter();
        GridLayout.RowHelper rowHelper = gridLayout.createRowHelper(1);
        rowHelper.addChild(Button.builder(
                CategoryConfigScreen.TITLE,
                button -> this.minecraft.setScreen(new CategoryConfigScreen(this)))
                .build());
        rowHelper.addChild(Button.builder(
                ThresholdConfigScreen.TITLE,
                button -> this.minecraft.setScreen(new ThresholdConfigScreen(this)))
                .build());
        rowHelper.addChild(Button.builder(
                InstantConfigScreen.TITLE,
                button -> this.minecraft.setScreen(new InstantConfigScreen(this)))
                .build());
        rowHelper.addChild(Button.builder(
                SoundListConfigScreen.TITLE,
                button -> this.minecraft.setScreen(new SoundListConfigScreen(this)))
                .build());

        this.layout.addToContents(gridLayout);
        this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).width(200).build());
        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
    }

    @Override
    protected void repositionElements() {
        this.layout.arrangeElements();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.parent);
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
