package com.mattymatty.audio_priority.screen;

import com.mattymatty.audio_priority.Configs;
import com.mattymatty.audio_priority.client.AudioPriority;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.util.*;

public class SoundListConfigScreen extends Screen {
    static final Component TITLE = Component.literal("Sound List");
    public final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this, 8 + 9 + 8 + 20 + 4, 60);
    protected final Screen parent;

    protected EditBox searchBox;
    protected SoundListWidget soundList;

    public SoundListConfigScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        LinearLayout linearLayout = this.layout.addToHeader(LinearLayout.vertical().spacing(4));
        linearLayout.defaultCellSetting().alignHorizontallyCenter();
        linearLayout.addChild(new StringWidget(this.title, this.font));
        LinearLayout linearLayout2 = linearLayout.addChild(LinearLayout.horizontal().spacing(4));

        this.searchBox = linearLayout2.addChild(
                new EditBox(this.font, this.width / 2 - 100, 22, 200, 20, this.searchBox, Component.literal("Search Sound"))
        );
        this.searchBox.setResponder(search -> this.soundList.showSearch(search));
        this.searchBox.setHint(Component.literal("Search...").setStyle(EditBox.SEARCH_HINT_STYLE));

        this.soundList = this.layout.addToContents(new SoundListWidget(this.minecraft, this.width, this.layout.getContentHeight(), this.layout.getHeaderHeight(), this.font.lineHeight * 2 + 8, 470));
        this.soundList.showSearch(this.searchBox.getValue());

        this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).width(200).build());

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();

        this.setInitialFocus(this.searchBox);
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
