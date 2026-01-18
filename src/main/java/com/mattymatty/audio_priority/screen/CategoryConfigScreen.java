package com.mattymatty.audio_priority.screen;

import com.mattymatty.audio_priority.Configs;
import com.mattymatty.audio_priority.client.AudioPriority;
import com.mattymatty.audio_priority.widget.ClickableListWidget;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;

public class CategoryConfigScreen extends Screen {
    protected final Screen parent;

    public CategoryConfigScreen(Screen parent) {
        super(Component.literal("Sound Category Priorities"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        assert this.minecraft != null;

        ClickableListWidget listWidget = new ClickableListWidget(this.minecraft, this.width, this.height - 64, 32, 25, 310);

        List<SoundSource> soundCategories = Arrays.stream(SoundSource.values()).filter(soundCategory -> soundCategory != SoundSource.MASTER).toList();

        int count = soundCategories.size() + 1;

        listWidget.addEntry(
                CycleButton.builder(v -> Component.literal(v.toString()), Configs.getInstance().categoryClasses.getOrDefault(SoundSource.MASTER.getName(),0))
                        .withValues(IntStream.range(0, count - 1).boxed().toList())
                        .create(0, 0, 310, 20,
                                Component.translatable("soundCategory." + SoundSource.MASTER.getName())
                                , (button, value) -> Configs.getInstance().categoryClasses.put(SoundSource.MASTER.getName(), value))
        );

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

        this.addRenderableWidget(listWidget);

        this.addRenderableWidget(
                Button.builder( CommonComponents.GUI_DONE, button -> this.minecraft.setScreen(this.parent))
                        .bounds(this.width / 2 - 100, this.height- 28, 200, 20).build());
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

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta)  {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredString(this.font, this.title, this.width / 2, 15, 0xFFFFFF);
    }

}
