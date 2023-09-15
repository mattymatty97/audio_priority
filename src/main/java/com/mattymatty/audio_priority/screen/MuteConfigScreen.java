package com.mattymatty.audio_priority.screen;

import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mattymatty.audio_priority.Configs;
import com.mattymatty.audio_priority.mixins.accessors.SoundManagerAccessor;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.screen.world.EditGameRulesScreen;
import net.minecraft.client.gui.widget.*;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameRules;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class MuteConfigScreen extends Screen {
    protected final Screen parent;

    public MuteConfigScreen(Screen parent) {
        super(Text.literal("Muted Sounds"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        super.init();
        assert this.client != null;
        SoundListWidget soundListWidget = new SoundListWidget();
        this.addDrawableChild(soundListWidget);
        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, button -> this.client.setScreen(this.parent)).dimensions(this.width / 2 - 100, (this.height) - 28, 200, 20 ).build());
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta)  {
        this.renderBackground(context);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    @Environment(EnvType.CLIENT)
    public abstract static class AbstractSoundEntryWidget extends ElementListWidget.Entry<AbstractSoundEntryWidget> {

        public AbstractSoundEntryWidget() {}
    }

    @Environment(EnvType.CLIENT)
    public class SoundWidgetEntry extends AbstractSoundEntryWidget implements Comparable<SoundWidgetEntry> {

        private final Identifier identifier;

        private final Text ruleName;
        private final List<OrderedText> name;
        private final List<OrderedText> subtitle;
        protected final List<ClickableWidget> children = Lists.<ClickableWidget>newArrayList();
        private final CyclingButtonWidget<Boolean> toggleButton;

        public SoundWidgetEntry(Text name, Identifier identifier) {
            super();
            this.identifier = identifier;
            MutableText text = Text.literal(identifier.getPath());
            MutableText text2 = null;
            if (name!=null) {
                text2 = Text.literal("( ");
                text2.append(name);
                text2.append(Text.literal(" )"));
            }
            this.ruleName = text;
            assert MuteConfigScreen.this.client != null;
            this.name = MuteConfigScreen.this.client.textRenderer.wrapLines(this.ruleName, 200);
            if (text2 != null){
                this.subtitle = MuteConfigScreen.this.client.textRenderer.wrapLines(text2, 200);
            }else{
                this.subtitle = Collections.emptyList();
            }
            this.toggleButton = CyclingButtonWidget.onOffBuilder(!Configs.getInstance().mutedSounds.contains(identifier.toString()))
                    .omitKeyText()
                    .narration(button -> button.getGenericNarrationMessage().append("\n").append(this.ruleName))
                    .build(10, 5, 44, 20, name, (button, value) -> {
                        if(!value)
                            Configs.getInstance().mutedSounds.add(identifier.toString());
                        else
                            Configs.getInstance().mutedSounds.remove(identifier.toString());
                    });
            this.children.add(this.toggleButton);
        }

        @Override
        public List<? extends Element> children() {
            return this.children;
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return this.children;
        }

        protected void drawName(DrawContext context, int x, int y) {
            assert MuteConfigScreen.this.client != null;
            List<OrderedText> texts = new LinkedList<>();
            if (!this.name.isEmpty()) {
                texts.add(this.name.get(0));
            }
            if (this.name.size() >= 2) {
                texts.add(this.name.get(1));
            }
            if (!this.subtitle.isEmpty()) {
                texts.add(this.subtitle.get(0));
            }
            if (this.subtitle.size() >= 2) {
                texts.add(this.subtitle.get(1));
            }
            int index = 0;
            for (OrderedText text : texts){
                context.drawText(MuteConfigScreen.this.client.textRenderer, text, x, y + index * 10, 16777215, false);
                index++;
            }
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            this.drawName(context, x - 30, y);
            this.toggleButton.setX(x + entryWidth - 45);
            this.toggleButton.setY(y);
            this.toggleButton.render(context, mouseX, mouseY, tickDelta);
        }



        public Text getRuleName() {
            return ruleName;
        }

        public boolean getStatus(){
            return !Configs.getInstance().mutedSounds.contains(this.identifier.toString());
        }

        @Override
        public int compareTo(@NotNull MuteConfigScreen.SoundWidgetEntry o) {
            int ret = Boolean.compare(this.getStatus(), o.getStatus());
            if (ret == 0)
                return this.getRuleName().getString().compareTo(o.getRuleName().getString());
            return ret;
        }
    }

    @Environment(EnvType.CLIENT)
    public class SoundNamespaceWidget extends AbstractSoundEntryWidget {
        final Text name;

        public SoundNamespaceWidget(Text text) {
            super();
            this.name = text;
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            assert MuteConfigScreen.this.client != null;
            context.drawCenteredTextWithShadow(MuteConfigScreen.this.client.textRenderer, this.name, x + entryWidth / 2, y + 5, 16777215);
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

    @Environment(EnvType.CLIENT)
    public class SoundListWidget extends ElementListWidget<AbstractSoundEntryWidget> {
        public SoundListWidget() {
            super(MuteConfigScreen.this.client, MuteConfigScreen.this.width, MuteConfigScreen.this.height, 43, MuteConfigScreen.this.height - 32, 44);
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
                                this.addEntry(
                                        MuteConfigScreen.this.new SoundNamespaceWidget(
                                                Text.literal(entry.getKey().toUpperCase()).formatted(Formatting.BOLD, Formatting.YELLOW)
                                        )
                                );
                                entry.getValue()
                                        .stream()
                                        .sorted()
                                        .forEach(this::addEntry);
                            }
                    );
        }

    }
}
