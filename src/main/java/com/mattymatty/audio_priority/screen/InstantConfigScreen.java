package com.mattymatty.audio_priority.screen;

import com.mattymatty.audio_priority.Configs;
import com.mattymatty.audio_priority.client.AudioPriority;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ScreenTexts;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.LiteralText;
import net.minecraft.text.TranslatableText;

import java.io.IOException;
import java.util.List;

public class InstantConfigScreen extends Screen {

    protected final Screen origin;
    protected final Screen parent;

    public InstantConfigScreen(Screen parent, Screen origin) {
        super(new LiteralText("Sound Categories allowed to bypass Priorities"));
        this.origin = origin;
        this.parent = parent;
    }

    @Override
    protected void init() {
        assert this.client != null;

        CyclingButtonWidget<String> btn = CyclingButtonWidget.builder(LiteralText::new)
                .values(List.of(Boolean.FALSE.toString(), Boolean.TRUE.toString()))
                .initially(Boolean.toString(Configs.getInstance().instantCategories.contains(SoundCategory.MASTER.getName())))
                .build(this.width / 2 - 155, this.height / 6 - 12, 310, 20, new TranslatableText("soundCategory." + SoundCategory.MASTER.getName())
                        , (button, value) -> {
                            if (Boolean.parseBoolean(value))
                                Configs.getInstance().instantCategories.add(SoundCategory.MASTER.getName());
                            else
                                Configs.getInstance().instantCategories.remove(SoundCategory.MASTER.getName());
                        });

        btn.active = false;


        this.addDrawableChild(btn);

        int i = 2;
        for (SoundCategory category : SoundCategory.values()) {
            if (category == SoundCategory.MASTER) continue;
            int j = this.width / 2 - 155 + i % 2 * 160;
            int k = this.height / 6 - 12 + 24 * (i >> 1);
            this.addDrawableChild(CyclingButtonWidget.builder(LiteralText::new)
                    .values(List.of(Boolean.FALSE.toString(), Boolean.TRUE.toString()))
                    .initially(Boolean.toString(Configs.getInstance().instantCategories.contains(category.getName())))
                    .build(j, k, 150, 20, new TranslatableText("soundCategory." + category.getName())
                            , (button, value) -> {
                                if (Boolean.parseBoolean(value))
                                    Configs.getInstance().instantCategories.add(category.getName());
                                else
                                    Configs.getInstance().instantCategories.remove(category.getName());
                            }));
            ++i;
        }

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
}
