package com.mattymatty.audio_priority.screen;

import com.mattymatty.audio_priority.Configs;
import com.mattymatty.audio_priority.client.AudioPriority;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.sound.SoundCategory;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

import java.io.IOException;
import java.util.function.Consumer;

public class ThresholdConfigScreen extends Screen {

    protected final Screen origin;
    protected final Screen parent;

    public ThresholdConfigScreen(Screen parent, Screen origin) {
        super(Text.literal("Thresholds"));
        this.origin = origin;
        this.parent = parent;
    }

    @Override
    protected void init() {
        assert this.client != null;
        ThresholdSlider slider = new ThresholdSlider(this.width / 2 - 155, this.height / 6 - 12, 310, 20, Text.translatable("soundCategory." + SoundCategory.MASTER.getName()), Configs.getInstance().maxPercentPerCategory.getOrDefault(SoundCategory.MASTER.getName(), 0d), (d) ->
                Configs.getInstance().maxPercentPerCategory.put(SoundCategory.MASTER.getName(), d)
        );
        slider.active = false;
        this.addDrawableChild(slider);
        int i = 2;
        for (SoundCategory category : SoundCategory.values()) {
            if (category == SoundCategory.MASTER) continue;
            int j = this.width / 2 - 155 + i % 2 * 160;
            int k = this.height / 6 - 12 + 24 * (i >> 1);
            this.addDrawableChild(new ThresholdSlider(j, k, 150, 20, Text.translatable("soundCategory." + category.getName()), Configs.getInstance().maxPercentPerCategory.getOrDefault(category.getName(), 0.1d), (d) ->
                    Configs.getInstance().maxPercentPerCategory.put(category.getName(), d)
            ));
            ++i;
        }

        i += i % 2;
        i += 2;

        int j = this.width / 2 - 155 + i % 2 * 160;
        int k = this.height / 6 - 12 + 24 * (i >> 1);

        this.addDrawableChild(new DuplicatesSlider(j, k, 310, 20, Text.literal("Max Duplicated Sounds By Pos"), Configs.getInstance().maxDuplicatedSoundsByPos, 50, (d) ->
                Configs.getInstance().maxDuplicatedSoundsByPos = Math.max(1, d))
        );

        this.addDrawableChild(new DuplicatesSlider(j, k + 25, 310, 20, Text.literal("Max Duplicated Sounds By Id"), Configs.getInstance().maxDuplicatedSoundsById , 200, (d) ->
                Configs.getInstance().maxDuplicatedSoundsById = Math.max(1, d))
        );


        this.addDrawableChild(
                ButtonWidget.builder(ScreenTexts.BACK, button -> this.client.setScreen(this.parent))
                        .dimensions(this.width / 2 - 105, (int) (this.height * 0.9), 100, 20).build());
        this.addDrawableChild(
                ButtonWidget.builder( ScreenTexts.DONE, button -> this.client.setScreen(this.origin))
                        .dimensions(this.width / 2 + 5, (int) (this.height * 0.9), 100, 20).build());
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
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
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
            Text text = (float) this.value == (float) this.getYImage() ? ScreenTexts.OFF : Text.literal((int) (this.value * 100.0) + "%");
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
