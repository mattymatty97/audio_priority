package com.mattymatty.audio_priority.screen;

import com.google.common.collect.ImmutableList;
import com.mattymatty.audio_priority.Configs;
import com.mattymatty.audio_priority.mixins.accessors.SoundManagerAccessor;
import joptsimple.internal.Strings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.*;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.Consumer;

public class SoundListWidget extends ElementListWidget<SoundListWidget.AbstractSoundEntryWidget> {
    private static final Text DEFAULT = Text.translatable("options.gamma.default");

    private final int spacebarPositionX;
    private final int rowWidth;
    List<AbstractSoundEntryWidget> sounds = new LinkedList<>();

    @Override
    public int getScrollbarX() {
        return spacebarPositionX;
    }

    @Override
    public int getRowWidth() {
        return rowWidth;
    }

    public SoundListWidget(
            MinecraftClient client,
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

        ((SoundManagerAccessor) this.client.getSoundManager()).getSounds().forEach((key, value) -> {
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
                                    Text.literal(entry.getKey().toUpperCase()).formatted(Formatting.BOLD, Formatting.YELLOW)
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

        private final Text ruleName;
        private final Text ruleSubtitle;
        private final Text name;
        private final Text subtitle;
        protected final List<ClickableWidget> children = new LinkedList<>();
        private final VolumeSlider    volumeSlider;
        private final ButtonWidget    resetButton;
        private final int yOffset;

        public SoundWidgetEntry(Text subtitle, Identifier identifier) {
            super();
            this.identifier = identifier;
            MutableText text = Text.literal(identifier.getPath());
            MutableText text2 = null;
            if (subtitle != null) {
                text2 = Text.literal("( ");
                text2.append(subtitle);
                text2.append(Text.literal(" )"));
            }
            this.ruleName = text;
            this.ruleSubtitle = subtitle;
            this.name = this.ruleName;
            this.subtitle = text2;

            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

            yOffset = Math.max(0, textRenderer.fontHeight + 2 - 10);

            float value = Configs.getInstance().soundVolumes.getOrDefault(identifier.toString(), 1f);

            this.volumeSlider = new VolumeSlider(0, 0, 150, 20, value, this::OnValueChanged);

            this.children.add(this.volumeSlider);

            this.resetButton = ButtonWidget
                    .builder(Text.literal("Reset"), button -> OnResetValue())
                    .dimensions(0, 0, 40, 20)
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
            this.volumeSlider.setValue(1d);
            this.resetButton.active = false;
            this.resetButton.setFocused(false);
        }

        @Override
        public List<? extends Element> children() {
            return this.children;
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return this.children;
        }

        protected void drawName(DrawContext context, int textEndX, int y) {
            MinecraftClient mc = MinecraftClient.getInstance();
            List<Text> texts = new LinkedList<>();
            if (this.name != null) {
                texts.add(this.name);
            }
            if (this.subtitle != null) {
                texts.add(this.subtitle.copy().formatted(Formatting.GRAY));
            }
            int index = 0;

            TextRenderer renderer = mc.textRenderer;

            int offset = 2;

            if (texts.size() == 1){
                offset = renderer.fontHeight / 2 + 3;
            }
            TextRenderer textRenderer = mc.textRenderer;
            for (Text text : texts) {
                int textWidth = renderer.getWidth(text);
                context.drawTextWithShadow(textRenderer, text, textEndX - textWidth, y + offset + index * (textRenderer.fontHeight + 1),  Colors.WHITE);
                index++;
            }
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
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

        public Text getRuleName() {
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
        final Text name;

        public SoundNamespaceWidget(Text text) {
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
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float deltaTicks) {
            MinecraftClient mc = MinecraftClient.getInstance();
            context.drawCenteredTextWithShadow(mc.textRenderer, this.name, getX() + this.getWidth()/ 2, getY() + 5,  Colors.WHITE);
        }

        @Override
        public List<? extends Element> children() {
            return ImmutableList.of();
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return ImmutableList.of(new Selectable() {
                @Override
                public SelectionType getType() {
                    return SelectionType.HOVERED;
                }

                @Override
                public void appendNarrations(NarrationMessageBuilder builder) {
                    builder.put(NarrationPart.TITLE, SoundNamespaceWidget.this.name);
                }
            });
        }
    }

    private static class VolumeSlider extends SliderWidget {

        private static final double EXPONENT = 2.0;

        private final Consumer<Double> callback;

        public VolumeSlider(int x, int y, int width, int height, double value, Consumer<Double> callback) {
            super(x, y, width, height, ScreenTexts.EMPTY, volumeToSlider(value));
            this.callback = callback;
            this.updateMessage();
        }

        public void setValue(double value)
        {
            this.value = volumeToSlider(value);
            this.updateMessage();
            applyValue();
        }

        @Override
        protected void updateMessage() {
            double multiplier = sliderToVolume(this.value);
            double decibel = volumeToDb(multiplier);
            if (this.value <= 0)
                this.setMessage(ScreenTexts.OFF);
            else if (Math.abs(decibel) < 0.1d)
                this.setMessage(DEFAULT);
            else
                this.setMessage(Text.literal(String.format("%+.1f dB", decibel)));
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
