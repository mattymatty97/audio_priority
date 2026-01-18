package com.mattymatty.audio_priority.mixins;

import com.mattymatty.audio_priority.screen.ConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.SoundOptionsScreen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(SoundOptionsScreen.class)
public abstract class SoundOptionsScreenMixin extends OptionsSubScreen {
    public SoundOptionsScreenMixin(Screen parent, Options gameOptions, Component title) {
        super(parent, gameOptions, title);
    }

    @Override
    protected void addFooter() {
        Minecraft mc = Minecraft.getInstance();
        LinearLayout directionalLayoutWidget = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
        directionalLayoutWidget.addChild(Button.builder(Component.literal("Audio Priorities"), button -> mc.setScreen(new ConfigScreen((SoundOptionsScreen)(Object)(this)))).build());
        directionalLayoutWidget.addChild(Button.builder(CommonComponents.GUI_DONE, buttonWidget -> this.onClose()).build());
    }

}
