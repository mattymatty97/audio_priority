package com.mattymatty.audio_priority.screen;

import com.google.common.collect.ImmutableList;
import com.mattymatty.audio_priority.Configs;
import com.mattymatty.audio_priority.mixins.accessors.SoundManagerAccessor;
import joptsimple.internal.Strings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
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

public class SoundListConfigScreen extends Screen {
    protected final Screen parent;

    protected TextFieldWidget searchBox;
    protected SoundListWidget soundList;

    private static final Text DEFAULT = Text.translatable("options.gamma.default");

    public SoundListConfigScreen(Screen parent) {
        super(Text.literal("Sound List"));
        this.parent = parent;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return this.searchBox.charTyped(chr, modifiers);
    }

    @Override
    protected void init() {
        super.init();
        assert this.client != null;
        this.searchBox = new TextFieldWidget(this.textRenderer, this.width / 2 - 100, 22, 200, 20, this.searchBox, Text.translatable("gui.recipebook.search_hint"));
        this.searchBox.setChangedListener(search -> this.soundList.showSearch(search));
        this.soundList = new SoundListWidget(this.client, this.width, this.height - 32 - 48, 48, this.textRenderer.fontHeight * 2 + 8, 300);
        this.soundList.showSearch(this.searchBox.getText());
        this.addSelectableChild(this.searchBox);
        this.addDrawableChild(this.soundList);
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> this.client.setScreen(this.parent)).dimensions(this.width / 2 - 100, (this.height) - 28, 200, 20).build());
        this.setInitialFocus(this.searchBox);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers) || this.searchBox.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        //this.renderBackground(context, mouseX, mouseY, delta);

        this.soundList.renderWidget(context, mouseX, mouseY, delta);
        this.searchBox.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 8, 16777215);

    }

    public abstract static class AbstractSoundEntryWidget extends ElementListWidget.Entry<AbstractSoundEntryWidget> {
        public abstract boolean shouldShow(String search);
    }


    public class SoundWidgetEntry extends AbstractSoundEntryWidget implements Comparable<SoundWidgetEntry> {

        private final Identifier identifier;

        private final Text ruleName;
        private final Text ruleSubtitle;
        private final Text name;
        private final Text subtitle;
        protected final List<ClickableWidget> children = new LinkedList<>();
        private final VolumeSlider volumeSlider;
        private final int sliderOffset;

        public SoundWidgetEntry(Text name, Identifier identifier) {
            super();
            this.identifier = identifier;
            MutableText text = Text.literal(identifier.getPath());
            MutableText text2 = null;
            if (name != null) {
                text2 = Text.literal("( ");
                text2.append(name);
                text2.append(Text.literal(" )"));
            }
            this.ruleName = text;
            this.ruleSubtitle = name;
            assert SoundListConfigScreen.this.client != null;
            this.name = this.ruleName;
            this.subtitle = text2;

            int fontHeight = SoundListConfigScreen.this.client.textRenderer.fontHeight;

            sliderOffset = Math.max(0, fontHeight + 2 - 10);

            float value = Configs.getInstance().soundVolumes.getOrDefault(identifier.toString(), 1f);

            this.volumeSlider = new VolumeSlider( 0, 0, 100, 20, name, value, (newValue) -> {
                    if (newValue < 0d)
                        newValue = 0d;
                    if (Math.abs(newValue - 1d) < 0.01d){
                        Configs.getInstance().soundVolumes.remove(identifier.toString());
                    }else{
                        Configs.getInstance().soundVolumes.put(identifier.toString(), (float)(double)newValue);
                    }
                }
            );

            this.children.add(this.volumeSlider);
        }

        @Override
        public List<? extends Element> children() {
            return this.children;
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return this.children;
        }

        protected void drawName(DrawContext context, int textOffset, int x, int y) {
            assert SoundListConfigScreen.this.client != null;
            List<Text> texts = new LinkedList<>();
            if (this.name != null) {
                texts.add(this.name);
            }
            if (this.subtitle != null) {
                texts.add(this.subtitle.copy().formatted(Formatting.GRAY));
            }
            int index = 0;

            int textEnd = x + textOffset;

            TextRenderer renderer = SoundListConfigScreen.this.client.textRenderer;

            int offset = 2;

            if (texts.size() == 1){
                offset = renderer.fontHeight / 2 + 3;
            }
            TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;
            for (Text text : texts) {
                int textWidth = renderer.getWidth(text);
                context.drawTextWithShadow(textRenderer, text, textEnd - textWidth, y + offset + index * (textRenderer.fontHeight + 1),  Colors.WHITE);
                index++;
            }
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            this.drawName(context, entryWidth - this.volumeSlider.getWidth() - 10, x, y);
            this.volumeSlider.setX(x + entryWidth - this.volumeSlider.getWidth() + 1);
            this.volumeSlider.setY(y + sliderOffset);
            this.volumeSlider.render(context, mouseX, mouseY, tickDelta);
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
        public int compareTo(@NotNull SoundListConfigScreen.SoundWidgetEntry o) {
            int ret = Boolean.compare(this.getStatus(), o.getStatus());
            if (ret == 0)
                return this.getRuleName().getString().compareTo(o.getRuleName().getString());
            return ret;
        }
    }

    public class SoundNamespaceWidget extends AbstractSoundEntryWidget {
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
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            assert SoundListConfigScreen.this.client != null;
            context.drawCenteredTextWithShadow(SoundListConfigScreen.this.client.textRenderer, this.name, x + entryWidth / 2, y + 5,  Colors.WHITE);
        }

        @Override
        public List<? extends Element> children() {
            return ImmutableList.of();
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return ImmutableList.of(new Selectable() {
                @Override
                public Selectable.SelectionType getType() {
                    return Selectable.SelectionType.HOVERED;
                }

                @Override
                public void appendNarrations(NarrationMessageBuilder builder) {
                    builder.put(NarrationPart.TITLE, SoundNamespaceWidget.this.name);
                }
            });
        }
    }


    public class SoundListWidget extends ElementListWidget<AbstractSoundEntryWidget> {
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

        private void showSearch(String search) {
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
    }

    private static class VolumeSlider extends SliderWidget {

        private final Consumer<Double> callback;

        public VolumeSlider(int x, int y, int width, int height, Text label, double value, Consumer<Double> callback) {
            super(x, y, width, height, label, value);
            this.callback = callback;
            this.updateMessage();
        }

        @Override
        protected void updateMessage() {
            if (this.value <= 0)
                this.setMessage(ScreenTexts.OFF);
            else if (Math.abs(this.value - 1d) < 0.01d)
                this.setMessage(DEFAULT);
            else
                this.setMessage(Text.literal((int) (this.value * 100.0) + "%"));
        }

        @Override
        protected void applyValue() {
            callback.accept(Math.max(0d, this.value));
        }

    }
}
