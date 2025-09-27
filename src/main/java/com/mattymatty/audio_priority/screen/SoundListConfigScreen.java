package com.mattymatty.audio_priority.screen;

import com.google.common.collect.ImmutableList;
import com.mattymatty.audio_priority.Configs;
import joptsimple.internal.Strings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.*;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class SoundListConfigScreen extends Screen {
    protected final Screen parent;

    protected TextFieldWidget searchBox;
    protected SoundListWidget soundList;

    public SoundListConfigScreen(Screen parent) {
        super(Text.literal("Sound List"));
        this.parent = parent;
    }
/*
    @Override
    public boolean charTyped(char chr, int modifiers) {
        return this.searchBox.charTyped(chr, modifiers);
    }
*/
    @Override
    protected void init() {
        super.init();
        assert this.client != null;
        this.searchBox = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 22, 200, 20, this.searchBox, Text.translatable("gui.recipebook.search_hint"));
        this.searchBox.setChangedListener(search -> this.soundList.showSearch(search));
        this.soundList = new SoundListWidget(this.client, this.width, this.height - 32 - 48, 48, this.textRenderer.fontHeight * 2 + 8, 300);
        this.soundList.showSearch(this.searchBox.getText());
        this.addSelectableChild(this.searchBox);
        this.addDrawableChild(this.soundList);
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> this.client.setScreen(this.parent)).dimensions(this.width / 2 - 100, (this.height) - 28, 200, 20).build());
        this.setInitialFocus(this.searchBox);
    }
/*
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers) || this.searchBox.keyPressed(keyCode, scanCode, modifiers);
    }
*/
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        //this.renderBackground(context, mouseX, mouseY, delta);

        this.soundList.renderWidget(context, mouseX, mouseY, delta);
        this.searchBox.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 16777215);

    }


}
