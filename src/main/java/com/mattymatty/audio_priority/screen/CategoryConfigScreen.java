package com.mattymatty.audio_priority.screen;

import com.mattymatty.audio_priority.Configs;
import com.mattymatty.audio_priority.client.AudioPriority;
import com.mattymatty.audio_priority.widget.ClickableListWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class CategoryConfigScreen extends Screen {
    protected final Screen parent;

    public CategoryConfigScreen(Screen parent) {
        super(Text.literal("Sound Category Priorities"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        assert this.client != null;

        ClickableListWidget listWidget = new ClickableListWidget(this.client, this.width, this.height - 64, 32, 25, 310);

        List<SoundCategory> soundCategories = Arrays.stream(SoundCategory.values()).filter(soundCategory -> soundCategory != SoundCategory.MASTER).toList();

        int count = soundCategories.size() + 1;

        listWidget.addEntry(
                CyclingButtonWidget.builder(v -> Text.literal(v.toString()), Configs.getInstance().categoryClasses.getOrDefault(SoundCategory.MASTER.getName(),0))
                        .values(IntStream.range(0, count - 1).boxed().toList())
                        .build(0, 0, 310, 20,
                                Text.translatable("soundCategory." + SoundCategory.MASTER.getName())
                                , (button, value) -> Configs.getInstance().categoryClasses.put(SoundCategory.MASTER.getName(), value))
        );

        for (int i = 0; i < soundCategories.size(); i += 2) {
            SoundCategory category = soundCategories.get(i);
            SoundCategory category2 = i < soundCategories.size() -1 ? soundCategories.get(i + 1) : null;
            ClickableWidget widget1;
            ClickableWidget widget2;

            widget1 = CyclingButtonWidget.builder(v -> Text.literal(v.toString()),Configs.getInstance()
                            .categoryClasses.getOrDefault(category.getName(),SoundCategory.values().length))
                    .values(IntStream.range(0, count - 1).boxed().toList())
                    .build(0, 0, 150, 20,
                            Text.translatable("soundCategory." + category.getName())
                            , (button, value) -> Configs.getInstance().categoryClasses.put(category.getName(), value));

            if (category2 != null){
                widget2 = CyclingButtonWidget.builder(v -> Text.literal(v.toString()), Configs.getInstance()
                                .categoryClasses.getOrDefault(category2.getName(),SoundCategory.values().length))
                        .values(IntStream.range(0, count - 1).boxed().toList())
                        .build(160, 0, 150, 20,
                                Text.translatable("soundCategory." + category2.getName())
                                , (button, value) -> Configs.getInstance().categoryClasses.put(category2.getName(), value));
                listWidget.addEntry(widget1, widget2);
            }else{
                listWidget.addEntry(widget1);
            }
        }

        this.addDrawableChild(listWidget);

        this.addDrawableChild(
                ButtonWidget.builder( ScreenTexts.DONE, button -> this.client.setScreen(this.parent))
                        .dimensions(this.width / 2 - 100, this.height- 28, 200, 20).build());
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta)  {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
    }

}
