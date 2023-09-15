package com.mattymatty.audio_priority.mixins;

import com.mattymatty.audio_priority.mixins.accessors.GameOptionsScreenAccessor;
import com.mattymatty.audio_priority.mixins.accessors.ScreenAccessor;
import com.mattymatty.audio_priority.screen.ConfigScreen;
import net.minecraft.client.gui.screen.option.SoundOptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(SoundOptionsScreen.class)
public class SoundOptionsScreenMixin {
    @ModifyArgs(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/ButtonWidget$Builder;dimensions(IIII)Lnet/minecraft/client/gui/widget/ButtonWidget$Builder;"))
    void resize_done_button(Args args){
        args.set(0, ((ScreenAccessor)this).getWidth() / 2 + 5);
        args.set(2, 100);
    }

    @Inject(method = "init", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/option/SoundOptionsScreen;addDrawableChild(Lnet/minecraft/client/gui/Element;)Lnet/minecraft/client/gui/Element;"))
    void add_config_menu(CallbackInfo ci){
        ((SoundOptionsScreen)(Object)this).addDrawableChild(ButtonWidget.builder(Text.literal("Audio Priorities"),button -> ((ScreenAccessor)this).getClient().setScreen(new ConfigScreen((SoundOptionsScreen)(Object)(this)))).dimensions(((ScreenAccessor)this).getWidth() / 2 - 105, (((ScreenAccessor)this).getHeight() - 27), 100, 20 ).build());
    }

}
