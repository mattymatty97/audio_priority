package com.mattymatty.audio_priority.screen;

import com.mattymatty.audio_priority.Configs;
import com.mattymatty.audio_priority.client.AudioPriority;
import com.mattymatty.audio_priority.widget.SoundListWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.io.IOException;

public class SoundListConfigScreen extends Screen {
    protected final Screen parent;

    protected TextFieldWidget searchBox;
    protected SoundListWidget soundList;

    public SoundListConfigScreen(Screen parent) {
        super(Text.literal("Sound List"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        assert this.client != null;
        this.searchBox = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 22, 200, 20, this.searchBox, Text.literal("Search Sound"));
        this.searchBox.setChangedListener(search -> this.soundList.showSearch(search));
        this.soundList = new SoundListWidget(this.client, this.width, this.height - 32, 48, this.textRenderer.fontHeight * 2 + 8, 470);
        this.soundList.showSearch(this.searchBox.getText());
        this.addSelectableChild(this.searchBox);
        this.addDrawableChild(this.soundList);
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> this.client.setScreen(this.parent)).dimensions(this.width / 2 - 100, (this.height) - 28, 200, 20).build());
        this.setInitialFocus(this.searchBox);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        //this.soundList.renderButton(context, mouseX, mouseY, delta);
        this.searchBox.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 16777215);

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
