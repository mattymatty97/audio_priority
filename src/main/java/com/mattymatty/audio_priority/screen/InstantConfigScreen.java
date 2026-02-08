package com.mattymatty.audio_priority.screen;

import com.mattymatty.audio_priority.Configs;
import com.mattymatty.audio_priority.client.AudioPriority;
import com.mattymatty.audio_priority.widget.ClickableListWidget;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;

public class InstantConfigScreen extends Screen {

    static final Component TITLE = Component.literal("Instant Categories");
    public final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
    protected final Screen parent;

    public InstantConfigScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(TITLE, this.font);
        ClickableListWidget listWidget = new ClickableListWidget(this.minecraft, this.width, this.layout.getContentHeight(), this.layout.getHeaderHeight(), 25, 310);

        List<SoundSource> soundCategories = Arrays.stream(SoundSource.values()).filter(soundCategory -> soundCategory != SoundSource.MASTER && soundCategory != SoundSource.UI).toList();

        for (int i = 0; i < soundCategories.size(); i += 2) {
            SoundSource category = soundCategories.get(i);
            SoundSource category2 = i < soundCategories.size() - 1 ? soundCategories.get(i + 1) : null;
            AbstractWidget widget1;
            AbstractWidget widget2;

            widget1 = CycleButton.onOffBuilder(Configs.getInstance().instantCategories.contains(category.getName()))
                    .create(0, 0, 150, 20, Component.translatable("soundCategory." + category.getName())
                            , (button, value) -> {
                                if (value)
                                    Configs.getInstance().instantCategories.add(category.getName());
                                else
                                    Configs.getInstance().instantCategories.remove(category.getName());
                            });

            if (category2 != null) {
                widget2 = CycleButton.onOffBuilder(Configs.getInstance().instantCategories.contains(category2.getName()))
                        .create(160, 0, 150, 20, Component.translatable("soundCategory." + category2.getName())
                                , (button, value) -> {
                                    if (value)
                                        Configs.getInstance().instantCategories.add(category2.getName());
                                    else
                                        Configs.getInstance().instantCategories.remove(category2.getName());
                                });

                listWidget.addEntry(widget1, widget2);
            } else {

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
