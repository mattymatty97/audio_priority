package com.mattymatty.audio_priority.screen;

import com.mattymatty.audio_priority.Configs;
import com.mattymatty.audio_priority.client.AudioPriority;
import com.mattymatty.audio_priority.widget.ClickableListWidget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

public class ThresholdConfigScreen extends Screen {

    protected final Screen parent;

    public ThresholdConfigScreen(Screen parent) {
        super(Text.literal("Thresholds"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        assert this.client != null;

        ClickableListWidget listWidget = new ClickableListWidget(this.client, this.width, this.height - 64, 32, 25, 310);

        ThresholdSlider slider = new ThresholdSlider(0, 0, 310, 20, Text.translatable("soundCategory." + SoundCategory.MASTER.getName()), Configs.getInstance().maxPercentPerCategory.getOrDefault(SoundCategory.MASTER.getName(), 0f), (d) ->
                Configs.getInstance().maxPercentPerCategory.put(SoundCategory.MASTER.getName(), (float)(double)d)
        );
        slider.active = false;

        listWidget.addEntry(slider);

        List<SoundCategory> soundCategories = Arrays.stream(SoundCategory.values()).filter(soundCategory -> soundCategory != SoundCategory.MASTER).toList();

        for (int i = 0; i < soundCategories.size(); i += 2) {
            SoundCategory category = soundCategories.get(i);
            SoundCategory category2 = i < soundCategories.size() -1 ? soundCategories.get(i + 1) : null;
            ThresholdSlider slider1;
            ThresholdSlider slider2;

            slider1 = new ThresholdSlider(0, 0, 150, 20, Text.translatable("soundCategory." + category.getName()), Configs.getInstance().maxPercentPerCategory.getOrDefault(category.getName(), 0.1f), (d) ->
                    Configs.getInstance().maxPercentPerCategory.put(category.getName(), (float)(double)d)
            );

            if (category2 != null){
                slider2 = new ThresholdSlider(160, 0, 150, 20, Text.translatable("soundCategory." + category2.getName()), Configs.getInstance().maxPercentPerCategory.getOrDefault(category2.getName(), 0.1f), (d) ->
                        Configs.getInstance().maxPercentPerCategory.put(category2.getName(), (float)(double)d)
                );
                listWidget.addEntry(slider1, slider2);
            }else{

                listWidget.addEntry(slider1);
            }

        }

        listWidget.addEntry();

        listWidget.addEntry(
                new DuplicatesSlider(0, 0, 310, 20, Text.literal("Max Duplicated Sounds By Pos"), Configs.getInstance().maxDuplicatedSoundsByPos, 50, (d) ->
                Configs.getInstance().maxDuplicatedSoundsByPos = Math.max(1, d)));

        listWidget.addEntry(
                new DuplicatesSlider(0, 0, 310, 20, Text.literal("Max Duplicated Sounds By Id"), Configs.getInstance().maxDuplicatedSoundsById , 200, (d) ->
                Configs.getInstance().maxDuplicatedSoundsById = Math.max(1, d)));

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

    private static class ThresholdSlider extends SliderWidget {

        private final Consumer<Double> callback;
        protected final Text label;

        public ThresholdSlider(int x, int y, int width, int height, Text label, double value, Consumer<Double> callback) {
            super(x, y, width, height, label, value);
            this.callback = callback;
            this.label = label;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            Text text = (float) this.value <= 0 ? ScreenTexts.OFF : Text.literal((int) (this.value * 100.0) + "%");
            this.setMessage(this.label.copy().append(": ").append(text));
        }

        @Override
        protected void applyValue() {
            callback.accept(this.value);
        }

    }

    private static class DuplicatesSlider extends ThresholdSlider {

        private final int max;
        private final Consumer<Integer> callback;

        public DuplicatesSlider(int x, int y, int width, int height, Text label, int value, int max, Consumer<Integer> callback) {
            super(x, y, width, height, label, ((double)value / max), null);
            this.max = max;
            this.callback = callback;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            Text text = Text.literal(Math.max((int)(this.value * (double)max), 1) + " sounds");
            this.setMessage(this.label.copy().append(": ").append(text));
        }

        @Override
        protected void applyValue() {
            this.callback.accept((int)( this.value * (double)max ));
        }
    }

}
