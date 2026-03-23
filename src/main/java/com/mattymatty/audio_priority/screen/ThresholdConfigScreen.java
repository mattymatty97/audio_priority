package com.mattymatty.audio_priority.screen;

import com.mattymatty.audio_priority.Configs;
import com.mattymatty.audio_priority.client.AudioPriority;
import com.mattymatty.audio_priority.widget.ClickableListWidget;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;

public class ThresholdConfigScreen extends Screen {
    static final Component TITLE = Component.literal("Thresholds");
    public final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);

    protected final Screen parent;

    public ThresholdConfigScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.layout.addTitleHeader(TITLE, this.font);

        ClickableListWidget listWidget = new ClickableListWidget(this.minecraft, this.width, this.height - 64, 32, 25, 310);

        List<SoundSource> soundCategories = Arrays.stream(SoundSource.values()).filter(soundCategory -> soundCategory != SoundSource.MASTER && soundCategory != SoundSource.UI).toList();

        for (int i = 0; i < soundCategories.size(); i += 2) {
            SoundSource category = soundCategories.get(i);
            SoundSource category2 = i < soundCategories.size() -1 ? soundCategories.get(i + 1) : null;
            ThresholdSlider slider1;
            ThresholdSlider slider2;

            slider1 = new ThresholdSlider(0, 0, 150, 20, Component.translatable("soundCategory." + category.getName()), Configs.getInstance().maxPercentPerCategory.getOrDefault(category.getName(), 0.1f), (d) ->
                    Configs.getInstance().maxPercentPerCategory.put(category.getName(), (float)Math.clamp(d, 0, 1))
            );

            if (category2 != null){
                slider2 = new ThresholdSlider(160, 0, 150, 20, Component.translatable("soundCategory." + category2.getName()), Configs.getInstance().maxPercentPerCategory.getOrDefault(category2.getName(), 0.1f), (d) ->
                        Configs.getInstance().maxPercentPerCategory.put(category2.getName(), (float)Math.clamp(d, 0, 1))
                );
                listWidget.addEntry(slider1, slider2);
            }else{

                listWidget.addEntry(slider1);
            }

        }

        listWidget.addEntry();

        listWidget.addEntry(
                new DuplicatesSlider(0, 0, 310, 20, Component.literal("Max Duplicated Sounds By Pos"), Configs.getInstance().maxDuplicatedSoundsByPos, 50, (d) ->
                Configs.getInstance().maxDuplicatedSoundsByPos = Math.max(1, d)));

        listWidget.addEntry(
                new DuplicatesSlider(0, 0, 310, 20, Component.literal("Max Duplicated Sounds By Id"), Configs.getInstance().maxDuplicatedSoundsById , 200, (d) ->
                Configs.getInstance().maxDuplicatedSoundsById = Math.max(1, d)));

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

    private static class ThresholdSlider extends AbstractSliderButton {

        private final Consumer<Double> callback;
        protected final Component label;

        public ThresholdSlider(int x, int y, int width, int height, Component label, double value, Consumer<Double> callback) {
            super(x, y, width, height, CommonComponents.EMPTY, value);
            this.callback = callback;
            this.label = label;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            Component text = (float) this.value <= 0 ? CommonComponents.OPTION_OFF : Component.literal((int) (this.value * 100.0) + "%");
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

        public DuplicatesSlider(int x, int y, int width, int height, Component label, int value, int max, Consumer<Integer> callback) {
            super(x, y, width, height, label, ((double)value / max), null);
            this.max = max;
            this.callback = callback;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            Component text = Component.literal(Math.max((int)(this.value * (double)max), 1) + " sounds");
            this.setMessage(this.label.copy().append(": ").append(text));
        }

        @Override
        protected void applyValue() {
            this.callback.accept((int)( this.value * (double)max ));
        }
    }

}
