package com.mattymatty.audio_priority.widget;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import org.jspecify.annotations.NonNull;

public class ClickableListWidget extends ContainerObjectSelectionList<ClickableListWidget.AbstractListWidgetEntry> {

    private final int spacebarPositionX;
    private final int rowWidth;

    public ClickableListWidget(Minecraft minecraftClient, int width, int height, int top_padding, int itemHeight, int rowWidth) {
        super(minecraftClient, width, height, top_padding, itemHeight);
        this.rowWidth = rowWidth;
        this.spacebarPositionX = (width / 2) + (rowWidth / 2) + 10;
    }

    public int addEntry(AbstractWidget...widgets){
        return super.addEntry(new ListWidgetEntry(widgets));
    }

    @Override
    public int addEntry(AbstractListWidgetEntry entry){
        return super.addEntry(entry);
    }

    @Override
    public int scrollBarX() {
        return spacebarPositionX;
    }

    @Override
    public int getRowWidth() {
        return rowWidth;
    }
    

    public abstract static class AbstractListWidgetEntry extends Entry<AbstractListWidgetEntry> {}

    public class ListWidgetEntry extends AbstractListWidgetEntry {

        private final List<WidgetHolder> widgets = new LinkedList<>();

        public ListWidgetEntry(AbstractWidget...widgets) {
            Arrays.stream(widgets).map(WidgetHolder::new).forEach(this.widgets::add);
        }

        @Override
        public @NonNull List<? extends NarratableEntry> narratables() {
            return List.of();
        }

        @Override
        public @NonNull List<? extends GuiEventListener> children() {
            return widgets.stream().map(WidgetHolder::getWidget).toList();
        }

        @Override
        public void renderContent(@NonNull GuiGraphics context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            for (WidgetHolder holder : widgets) {
                holder.widget.setX(getX() + ClickableListWidget.this.rowWidth / 2 - 155 + holder.getDx());
                holder.widget.setY(getY() + holder.getDy());
                holder.widget.render(context, mouseX, mouseY, deltaTicks);
            }
        }

        public static class WidgetHolder{
            private final AbstractWidget widget;

            private final int dx;
            private final int dy;

            public AbstractWidget getWidget() {
                return widget;
            }

            public int getDx() {
                return dx;
            }

            public int getDy() {
                return dy;
            }

            public WidgetHolder(AbstractWidget widget) {
                this.widget = widget;
                this.dx = widget.getX();
                this.dy = widget.getY();
            }
        }
    }
}
