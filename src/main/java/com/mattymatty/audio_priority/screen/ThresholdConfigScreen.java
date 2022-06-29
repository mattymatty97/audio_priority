package com.mattymatty.audio_priority.screen;

import com.mattymatty.audio_priority.Configs;
import com.mattymatty.audio_priority.client.AudioPriority;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.util.math.MatrixStack;
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
        this.addDrawableChild(new DuplicatesSlider(j, k, 310, 20, Text.literal("Max Duplicated Sounds"), Configs.getInstance().maxDuplicatedSounds / 50d, (d) ->
                Configs.getInstance().maxDuplicatedSounds = Math.max(1, (int) (d * 50))
        ));

        this.addDrawableChild(new ButtonWidget(this.width / 2 - 105, (int) (this.height * 0.9), 100, 20, ScreenTexts.BACK, button -> this.client.setScreen(this.parent)));
        this.addDrawableChild(new ButtonWidget(this.width / 2 + 5, (int) (this.height * 0.9), 100, 20, ScreenTexts.DONE, button -> this.client.setScreen(this.origin)));

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
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        this.renderBackground(matrices);
        DrawableHelper.drawCenteredText(matrices, this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);

        super.render(matrices, mouseX, mouseY, delta);
    }

    private static class ThresholdSlider extends SliderWidget {

        private final Consumer<Double> callback;

        public ThresholdSlider(int x, int y, int width, int height, Text text, double value, Consumer<Double> callback) {
            super(x, y, width, height, text, value);
            this.callback = callback;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            Text text = (float) this.value == (float) this.getYImage(false) ? ScreenTexts.OFF : Text.literal((int) (this.value * 100.0) + "%");
            this.setMessage(this.getMessage().copy().append(": ").append(text));
        }

        @Override
        protected void applyValue() {
            callback.accept(this.value);
        }


    }

    private static class DuplicatesSlider extends ThresholdSlider {

        public DuplicatesSlider(int x, int y, int width, int height, Text text, double value, Consumer<Double> callback) {
            super(x, y, width, height, text, value, callback);
        }

        @Override
        protected void updateMessage() {
            Text text = Text.literal(Math.max((int) (this.value * 50d), 1) + " sounds");
            this.setMessage(this.getMessage().copy().append(": ").append(text));
        }


    }
}
