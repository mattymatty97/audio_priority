package com.mattymatty.audio_priority.screen;

import com.mattymatty.audio_priority.Configs;
import com.mattymatty.audio_priority.client.AudioPriority;
import com.mattymatty.audio_priority.widget.ClickableListWidget;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;

public class CategoryConfigScreen extends Screen {
    static final Component TITLE = Component.literal("Sound Category Priorities");
    public final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    protected final Screen parent;

    public CategoryConfigScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(TITLE, this.font);

        ClickableListWidget listWidget = new ClickableListWidget(this.minecraft, this.width, this.height - 64, 32, 25, 310);

        List<SoundSource> soundCategories = Arrays.stream(SoundSource.values()).filter(soundCategory -> soundCategory != SoundSource.MASTER && soundCategory != SoundSource.UI).toList();

        final int count = soundCategories.size() + 1;

        for (int i = 0; i < soundCategories.size(); i += 2) {
            SoundSource category = soundCategories.get(i);
            SoundSource category2 = i < soundCategories.size() -1 ? soundCategories.get(i + 1) : null;
            AbstractWidget widget1;
            AbstractWidget widget2;

            widget1 = CycleButton.builder(v -> Component.literal(v.toString()),Configs.getInstance()
                            .categoryClasses.getOrDefault(category.getName(),SoundSource.values().length))
                    .withValues(IntStream.range(0, count - 1).boxed().toList())
                    .create(0, 0, 150, 20,
                            Component.translatable("soundCategory." + category.getName())
                            , (button, value) -> Configs.getInstance().categoryClasses.put(category.getName(), value));

            if (category2 != null){
                widget2 = CycleButton.builder(v -> Component.literal(v.toString()), Configs.getInstance()
                                .categoryClasses.getOrDefault(category2.getName(),SoundSource.values().length))
                        .withValues(IntStream.range(0, count - 1).boxed().toList())
                        .create(160, 0, 150, 20,
                                Component.translatable("soundCategory." + category2.getName())
                                , (button, value) -> Configs.getInstance().categoryClasses.put(category2.getName(), value));
                listWidget.addEntry(widget1, widget2);
            }else{
                listWidget.addEntry(widget1);
            }
        }

        this.layout.addToContents(listWidget);

        this.layout.addToFooter(Button.builder(CommonComponents.GUI_DONE, button -> this.onClose()).width(200).build());

        this.layout.visitWidgets(this::addRenderableWidget);
        this.repositionElements();
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
