package com.mattymatty.audio_priority.widget;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ClickableListWidget extends ElementListWidget<ClickableListWidget.AbstractListWidgetEntry> {

    private final int spacebarPositionX;
    private final int rowWidth;

    public ClickableListWidget(MinecraftClient minecraftClient, int width, int height, int top_padding, int itemHeight, int rowWidth) {
        super(minecraftClient, width, height, top_padding, itemHeight);
        this.rowWidth = rowWidth;
        this.spacebarPositionX = (width / 2) + (rowWidth / 2) + 10;
    }

    public int addEntry(ClickableWidget...widgets){
        return super.addEntry(new ClickableListWidget.ListWidgetEntry(widgets));
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
    

    public abstract class AbstractListWidgetEntry extends Entry<AbstractListWidgetEntry> {}

    public class ListWidgetEntry extends AbstractListWidgetEntry {

        private final List<WidgetHolder> widgets = new LinkedList<>();

        public ListWidgetEntry(ClickableWidget...widgets) {
            Arrays.stream(widgets).map(WidgetHolder::new).forEach(this.widgets::add);
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return List.of();
        }

        @Override
        public List<? extends Element> children() {
            return widgets.stream().map(WidgetHolder::getWidget).toList();
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            for (WidgetHolder holder : widgets) {
                holder.widget.setX(getX() + ClickableListWidget.this.rowWidth / 2 - 155 + holder.getDx());
                holder.widget.setY(getY() + holder.getDy());
                holder.widget.render(context, mouseX, mouseY, deltaTicks);
            }
        }

        public static class WidgetHolder{
            private final ClickableWidget widget;

            private final int dx;
            private final int dy;

            public ClickableWidget getWidget() {
                return widget;
            }

            public int getDx() {
                return dx;
            }

            public int getDy() {
                return dy;
            }

            public WidgetHolder(ClickableWidget widget) {
                this.widget = widget;
                this.dx = widget.getX();
                this.dy = widget.getY();
            }
        }
    }
}
