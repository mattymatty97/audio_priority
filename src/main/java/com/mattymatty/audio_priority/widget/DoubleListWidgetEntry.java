package com.mattymatty.audio_priority.widget;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.List;

public class DoubleListWidgetEntry extends AbstractListWidgetEntry {

    private final ClickableWidget first;
    private final ClickableWidget second;
    private final int firstDx;
    private final int secondDx;
    private final int firstDy;
    private final int secondDy;

    public DoubleListWidgetEntry(ClickableWidget first, ClickableWidget second) {
        assert first != null;
        assert second != null;
        this.first = first;
        this.firstDx = first.getX();
        this.firstDy = first.getY();
        this.second = second;
        this.secondDx = second.getX();
        this.secondDy = second.getY();
    }

    @Override
    public List<? extends Selectable> selectableChildren() {
        return List.of();
    }

    @Override
    public List<? extends Element> children() {
        return List.of(first,second);
    }

    @Override
    public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
        first.setX(x + entryWidth / 2 - 155 + this.firstDx);
        first.setY(y + this.firstDy);
        first.render(context, mouseX, mouseY, tickDelta);
        second.setX(x + entryWidth / 2 - 155 + this.secondDx);
        second.setY(y + this.secondDy);
        second.render(context, mouseX, mouseY, tickDelta);
    }
}
