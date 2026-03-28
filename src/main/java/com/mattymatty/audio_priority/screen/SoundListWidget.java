package com.mattymatty.audio_priority.screen;

import com.google.common.collect.ImmutableList;
import com.mattymatty.audio_priority.Configs;
import com.mattymatty.audio_priority.mixins.accessors.SoundManagerAccessor;
import joptsimple.internal.Strings;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.function.Consumer;

public class SoundListWidget extends ContainerObjectSelectionList<SoundListWidget.AbstractSoundEntryWidget> {
    private static final Component DEFAULT = Component.translatable("options.gamma.default");

    private final int spacebarPositionX;
    private final int rowWidth;
    List<AbstractSoundEntryWidget> sounds = new LinkedList<>();

    @Override
    public int scrollBarX() {
        return spacebarPositionX;
    }

    @Override
    public int getRowWidth() {
        return rowWidth;
    }

    public SoundListWidget(
            Minecraft client,
            int width,
            int height,
            int top,
            int itemHeight,
            int rowWidth
    ) {
        super(client, width, height, top, itemHeight);
        this.rowWidth = rowWidth;
        this.spacebarPositionX = (width / 2) + (rowWidth / 2) + 10;

        final Map<String, List<SoundWidgetEntry>> sound_map = new LinkedHashMap<>();

        ((SoundManagerAccessor) this.minecraft.getSoundManager()).getRegistry().forEach((key, value) -> {
            List<SoundWidgetEntry> elements = sound_map.computeIfAbsent(key.getNamespace(), s -> new LinkedList<>());
            elements.add(new SoundWidgetEntry(value.getSubtitle(), key));
        });

        sound_map.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey((String k1, String k2) -> {
                    if (k1.equals("minecraft"))
                        return -1;
                    else if (k2.equals("minecraft"))
                        return 1;
                    else
                        return k1.compareTo(k2);
                }))
                .forEach(
                        entry -> {
                            SoundNamespaceWidget namespaceWidget = new SoundNamespaceWidget(
                                    Component.literal(entry.getKey().toUpperCase()).withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW)
                            );
                            sounds.add(namespaceWidget);
                            entry.getValue()
                                    .stream()
                                    .sorted()
                                    .forEach(e -> {
                                        sounds.add(e);
                                        namespaceWidget.addSubEntry(e);
                                    });
                        }
                );
        this.showSearch(null);
    }

    public void showSearch(String search) {
        this.clearEntries();
        if (search != null) {
            search = search.toLowerCase(Locale.ROOT);
        }

        for (AbstractSoundEntryWidget soundEntry : sounds) {
            if (soundEntry.shouldShow(search)) {
                this.addEntry(soundEntry);
            }
        }
    }

    public abstract static class AbstractSoundEntryWidget extends Entry<AbstractSoundEntryWidget> {
        public abstract boolean shouldShow(String search);
    }

    public static class SoundWidgetEntry extends AbstractSoundEntryWidget implements Comparable<SoundWidgetEntry> {
        final int spacing = 10;

        private final Identifier identifier;

        private final Component ruleName;
        private final Component ruleSubtitle;
        private final Component name;
        private final Component subtitle;
        protected final List<AbstractWidget> children = new LinkedList<>();
        private final VolumeSlider    volumeSlider;
        private final Button    resetButton;
        private final int yOffset;

        public SoundWidgetEntry(Component subtitle, Identifier identifier) {
            super();
            this.identifier = identifier;
            MutableComponent text = Component.literal(identifier.getPath());
            MutableComponent text2 = null;
            if (subtitle != null) {
                text2 = Component.literal("( ");
                text2.append(subtitle);
                text2.append(Component.literal(" )"));
            }
            this.ruleName = text;
            this.ruleSubtitle = subtitle;
            this.name = this.ruleName;
            this.subtitle = text2;

            Font textRenderer = Minecraft.getInstance().font;

            yOffset = Math.max(0, textRenderer.lineHeight + 2 - 10);

            float value = Configs.getInstance().soundVolumes.getOrDefault(identifier.toString(), 1f);

            this.volumeSlider = new VolumeSlider(0, 0, 150, 20, value, this::OnValueChanged);

            this.children.add(this.volumeSlider);

            this.resetButton = Button
                    .builder(Component.literal("Reset"), button -> OnResetValue())
                    .bounds(0, 0, 40, 20)
                    .build();

            if (Math.abs(value - 1f) < 0.01d)
            {
                this.resetButton.active = false;
            }

            this.children.add(this.resetButton);
        }

        private void OnValueChanged(double newValue) {
            if (newValue < 0d)
                newValue = 0d;
            if (Math.abs(newValue - 1d) < 0.01d){
                Configs.getInstance().soundVolumes.remove(identifier.toString());
                this.resetButton.active = false;
            }else{
                Configs.getInstance().soundVolumes.put(identifier.toString(), (float)newValue);
                this.resetButton.active = true;
            }
        }

        private void OnResetValue()
        {
            this.volumeSlider.setVolume(1d);
            this.resetButton.active = false;
            this.resetButton.setFocused(false);
        }

        @Override
        public @NonNull List<? extends GuiEventListener> children() {
            return this.children;
        }

        @Override
        public @NonNull List<? extends NarratableEntry> narratables() {
            return this.children;
        }

        protected void drawName(GuiGraphics context, int textEndX, int y) {
            Minecraft mc = Minecraft.getInstance();
            List<Component> texts = new LinkedList<>();
            if (this.name != null) {
                texts.add(this.name);
            }
            if (this.subtitle != null) {
                texts.add(this.subtitle.copy().withStyle(ChatFormatting.GRAY));
            }
            int index = 0;

            Font renderer = mc.font;

            int offset = 2;

            if (texts.size() == 1){
                offset = renderer.lineHeight / 2 + 3;
            }
            Font textRenderer = mc.font;
            for (Component text : texts) {
                int textWidth = renderer.width(text);
                context.drawString(textRenderer, text, textEndX - textWidth, y + offset + index * (textRenderer.lineHeight + 1),  CommonColors.WHITE);
                index++;
            }
        }

        @Override
        public void renderContent(@NonNull GuiGraphics context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            this.drawName(context,getX() + this.getWidth()/2 - spacing, getY());
            this.volumeSlider.setX(getX() + this.getWidth()/2 + spacing);
            this.volumeSlider.setY(getY() + yOffset);
            this.volumeSlider.render(context, mouseX, mouseY, deltaTicks);
            this.resetButton.setX(getX() + this.getWidth()/2 + spacing + this.volumeSlider.getWidth() + spacing);
            this.resetButton.setY(getY() + yOffset);
            this.resetButton.render(context, mouseX, mouseY, deltaTicks);
        }

        public boolean shouldShow(String search) {
            if (Strings.isNullOrEmpty(search))
                return true;
            return this.ruleName.getString().toLowerCase(Locale.ROOT).contains(search) || ( this.ruleSubtitle != null && this.ruleSubtitle.getString().toLowerCase(Locale.ROOT).contains(search) );
        }

        public Component getRuleName() {
            return ruleName;
        }

        public boolean getStatus() {
            return !Configs.getInstance().soundVolumes.containsKey(this.identifier.toString());
        }

        @Override
        public int compareTo(@NotNull SoundWidgetEntry o) {
            int ret = Boolean.compare(this.getStatus(), o.getStatus());
            if (ret == 0)
                return this.getRuleName().getString().compareTo(o.getRuleName().getString());
            return ret;
        }
    }

    public static class SoundNamespaceWidget extends AbstractSoundEntryWidget {
        private final List<AbstractSoundEntryWidget> sub_entries = new LinkedList<>();
        final Component name;

        public SoundNamespaceWidget(Component text) {
            super();
            this.name = text;
        }

        public void addSubEntry(AbstractSoundEntryWidget entry) {
            this.sub_entries.add(entry);
        }

        public boolean shouldShow(String search) {
            return sub_entries.stream().anyMatch(e -> e.shouldShow(search));
        }

        @Override
        public void renderContent(GuiGraphics context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            Minecraft mc = Minecraft.getInstance();
            context.drawCenteredString(mc.font, this.name, getX() + this.getWidth()/ 2, getY() + 5,  CommonColors.WHITE);
        }

        @Override
        public @NonNull List<? extends GuiEventListener> children() {
            return ImmutableList.of();
        }

        @Override
        public @NonNull List<? extends NarratableEntry> narratables() {
            return ImmutableList.of(new NarratableEntry() {
                @Override
                public @NonNull NarrationPriority narrationPriority() {
                    return NarrationPriority.HOVERED;
                }

                @Override
                public void updateNarration(@NonNull NarrationElementOutput builder) {
                    builder.add(NarratedElementType.TITLE, SoundNamespaceWidget.this.name);
                }
            });
        }
    }

    private static class VolumeSlider extends AbstractSliderButton {

        private static final double EXPONENT = 2.0;

        private final Consumer<Double> callback;

        public VolumeSlider(int x, int y, int width, int height, double value, Consumer<Double> callback) {
            super(x, y, width, height, CommonComponents.EMPTY, volumeToSlider(value));
            this.callback = callback;
            this.updateMessage();
        }

        public void setVolume(double volume)
        {
            double newValue = volumeToSlider(volume);
            this.setValue(newValue);
        }

        @Override
        protected void updateMessage() {
            double multiplier = sliderToVolume(this.value);
            double decibel = volumeToDb(multiplier);
            if (this.value <= 0)
                this.setMessage(CommonComponents.OPTION_OFF);
            else if (Math.abs(decibel) < 0.1d)
                this.setMessage(DEFAULT);
            else
                this.setMessage(Component.literal(String.format("%+.1f dB", decibel)));
        }

        @Override
        protected void applyValue() {
            double multiplier = sliderToVolume(this.value);
            double decibel = volumeToDb(multiplier);
            //round to 1 decimal place
            double rounded = Math.round(decibel * 10.0) / 10.0;
            double limited = Math.max(0d, dbToVolume(rounded));

            this.value = volumeToSlider(limited);
            callback.accept(limited);
        }

        // Forward mapping
        public static double sliderToVolume(double slider) {
            if (slider <= 0.0) return 0.0;
            if (slider >= 1.0) return 2.0;

            if (slider <= 0.7) {
                double normalized = slider / 0.7;
                return Math.pow(normalized, EXPONENT);
            } else {
                double t = (slider - 0.7) / 0.3;
                return 1.0 + t; // 1 → 2
            }
        }

        // Inverse mapping: multiplier → slider
        public static double volumeToSlider(double multiplier) {
            if (multiplier <= 0.0) return 0.0;
            if (multiplier >= 2.0) return 1.0;

            if (multiplier <= 1.0) {
                return 0.7 * Math.pow(multiplier, 1.0 / EXPONENT);
            } else {
                return 0.7 + (multiplier - 1.0) * 0.3;
            }
        }

        public static double dbToVolume(double dB) {
            return Math.pow(10, dB / 20.0);
        }

        public static double volumeToDb(double multiplier) {
            return 20.0 * Math.log10(multiplier);
        }
    }
}
