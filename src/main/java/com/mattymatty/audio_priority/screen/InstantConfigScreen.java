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

public class InstantConfigScreen extends Screen {

    protected final Screen parent;

    public InstantConfigScreen(Screen parent) {
        super(Text.literal("Sound Categories allowed to bypass Priorities"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        assert this.client != null;

        ClickableListWidget listWidget = new ClickableListWidget(this.client, this.width, this.height - 32, 32, 25, 310);

        List<SoundCategory> soundCategories = Arrays.stream(SoundCategory.values()).filter(soundCategory -> soundCategory != SoundCategory.MASTER).toList();


        ClickableWidget btn = CyclingButtonWidget.onOffBuilder(Configs.getInstance().instantCategories.contains(SoundCategory.MASTER.getName()))
                .build(0, 0, 310, 20, Text.translatable("soundCategory." + SoundCategory.MASTER.getName())
                        , (button, value) -> {
                            if (value)
                                Configs.getInstance().instantCategories.add(SoundCategory.MASTER.getName());
                            else
                                Configs.getInstance().instantCategories.remove(SoundCategory.MASTER.getName());
                        });

        btn.active = false;

        listWidget.addEntry(btn);

        for (int i = 0; i < soundCategories.size(); i += 2) {
            SoundCategory category = soundCategories.get(i);
            SoundCategory category2 = i < soundCategories.size() -1 ? soundCategories.get(i + 1) : null;
            ClickableWidget widget1;
            ClickableWidget widget2;

            widget1 = CyclingButtonWidget.onOffBuilder(Configs.getInstance().instantCategories.contains(category.getName()))
                    .build(0, 0, 150, 20, Text.translatable("soundCategory." + category.getName())
                            , (button, value) -> {
                                if (value)
                                    Configs.getInstance().instantCategories.add(category.getName());
                                else
                                    Configs.getInstance().instantCategories.remove(category.getName());
                            });

            if (category2 != null){
                widget2 = CyclingButtonWidget.onOffBuilder(Configs.getInstance().instantCategories.contains(category2.getName()))
                        .build(160, 0, 150, 20, Text.translatable("soundCategory." + category2.getName())
                                , (button, value) -> {
                                    if (value)
                                        Configs.getInstance().instantCategories.add(category2.getName());
                                    else
                                        Configs.getInstance().instantCategories.remove(category2.getName());
                                });

                listWidget.addEntry(widget1, widget2);
            }else {

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
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow( this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
    }
}
