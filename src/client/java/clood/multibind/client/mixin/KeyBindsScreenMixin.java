package clood.multibind.client.mixin;

import clood.multibind.MultiBindableKey;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.options.controls.KeyBindsList;
import net.minecraft.client.gui.screens.options.controls.KeyBindsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(KeyBindsScreen.class)
public abstract class KeyBindsScreenMixin {
  @Shadow
  private KeyMapping selectedKey;
  @Shadow
  private long lastKeySelection;
  @Shadow
  private KeyBindsList keyBindsList;

  @Unique
  private boolean multibind$addMode;

  @Inject(method = "mouseClicked", at = @At("RETURN"))
  private void multibind$afterMouseClick(MouseButtonEvent event, boolean handled, CallbackInfoReturnable<Boolean> cir) {
    if (this.selectedKey != null) {
      this.multibind$addMode = (event.modifiers() & InputConstants.MOD_SHIFT) != 0;
    }
  }

  @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
  private void multibind$handleKeyPress(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
    if (this.selectedKey == null) {
      return;
    }

    InputConstants.Key inputKey = InputConstants.getKey(event);
    MultiBindableKey binding = (MultiBindableKey) this.selectedKey;

    if (inputKey.getType() == InputConstants.Type.KEYSYM && inputKey.getValue() == InputConstants.KEY_ESCAPE) {
      binding.multibind$replaceKey(InputConstants.UNKNOWN);
      binding.multibind$clearAdditionalKeys();
    } else if (this.multibind$addMode) {
      binding.multibind$toggleAdditionalKey(inputKey);
    } else {
      binding.multibind$replaceKey(inputKey);
    }

    this.multibind$addMode = false;
    this.selectedKey = null;
    this.lastKeySelection = System.currentTimeMillis();
    this.keyBindsList.resetMappingAndUpdateButtons();
    cir.setReturnValue(true);
  }

  @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
  private void multibind$handleMouseClick(MouseButtonEvent event, boolean handled,
      CallbackInfoReturnable<Boolean> cir) {
    if (this.selectedKey == null) {
      return;
    }

    InputConstants.Key inputKey = InputConstants.Type.MOUSE.getOrCreate(event.button());
    MultiBindableKey binding = (MultiBindableKey) this.selectedKey;

    if (this.multibind$addMode) {
      binding.multibind$toggleAdditionalKey(inputKey);
    } else {
      binding.multibind$replaceKey(inputKey);
    }

    this.multibind$addMode = false;
    this.selectedKey = null;
    this.lastKeySelection = System.currentTimeMillis();
    this.keyBindsList.resetMappingAndUpdateButtons();
    cir.setReturnValue(true);
  }
}