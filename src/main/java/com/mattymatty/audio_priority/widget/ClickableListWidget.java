package com.mattymatty.audio_priority.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ElementListWidget;

public class ClickableListWidget extends ElementListWidget<AbstractListWidgetEntry> {

    private final int spacebarPositionX;
    private final int rowWidth;

    public ClickableListWidget(MinecraftClient minecraftClient, int width, int height, int top_padding, int itemHeight, int rowWidth) {
        super(minecraftClient, width, height, top_padding, itemHeight);
        this.rowWidth = rowWidth;
        this.spacebarPositionX = (width / 2) + (rowWidth / 2) + 10;
    }

    @Override
    public int addEntry(AbstractListWidgetEntry entry){
        return super.addEntry(entry);
    }

    @Override
    public int getScrollbarX() {
        return spacebarPositionX;
    }

    @Override
    public int getRowWidth() {
        return rowWidth;
    }
}
