package com.mattymatty.audio_priority.widget;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ClickableWidget;

import java.util.List;

public class ListWidgetEntry extends AbstractListWidgetEntry {

    private final ClickableWidget widget;

    private final int dx;
    private final int dy;

    public ListWidgetEntry(ClickableWidget widget) {
        this.widget = widget;
        this.dx = widget.getX();
        this.dy = widget.getY();
    }

    @Override
    public List<? extends Selectable> selectableChildren() {
        return List.of();
    }

    @Override
    public List<? extends Element> children() {
        return List.of(widget);
    }

    @Override
    public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
        widget.setX(x + entryWidth / 2 - 155 + this.dx);
        widget.setY(y + this.dy);
        widget.render(context, mouseX, mouseY, tickDelta);
    }
}
