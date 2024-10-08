package com.mattymatty.audio_priority.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ElementListWidget;

public class ClickableListWidget extends ElementListWidget<AbstractListWidgetEntry> {

    private final int spacebarPositionX;

    public ClickableListWidget(MinecraftClient minecraftClient, int width, int height, int top_padding, int itemHeight, int spacebarPositionX) {
        super(minecraftClient, width, height, top_padding, itemHeight);
        this.spacebarPositionX = spacebarPositionX;
    }

    @Override
    public int addEntry(AbstractListWidgetEntry entry){
        return super.addEntry(entry);
    }

    @Override
    public int getScrollbarX() {
        return spacebarPositionX;
    }
}
