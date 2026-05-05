package clood.multibind.client.mixin;

import clood.multibind.MultiBindableKey;
import clood.multibind.MultibindBindings;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Consumer;

@Mixin(KeyMapping.class)
public abstract class KeyMappingMixin implements MultiBindableKey {
  @Shadow
  @Final
  private static Map<String, KeyMapping> ALL;
  @Shadow
  @Final
  private String name;
  @Shadow
  @Final
  private InputConstants.Key defaultKey;
  @Shadow
  private InputConstants.Key key;

  @Shadow
  public abstract void setKey(InputConstants.Key key);

  @Unique
  private final LinkedHashSet<InputConstants.Key> multibind$additionalKeys = new LinkedHashSet<>();

  @Inject(method = "<init>(Ljava/lang/String;Lcom/mojang/blaze3d/platform/InputConstants$Type;ILnet/minecraft/client/KeyMapping$Category;I)V", at = @At("TAIL"))
  private void multibind$loadAdditionalKeys(String name, InputConstants.Type type, int keyCode,
      KeyMapping.Category category, int order, CallbackInfo ci) {
    for (String keyName : MultibindBindings.INSTANCE.getAdditionalKeyNames(this.name)) {
      InputConstants.Key additionalKey = InputConstants.getKey(keyName);
      if (additionalKey != InputConstants.UNKNOWN) {
        multibind$additionalKeys.add(additionalKey);
      }
    }
  }

  @Inject(method = "setKey", at = @At("TAIL"))
  private void multibind$clearExtrasOnReplace(InputConstants.Key key, CallbackInfo ci) {
    multibind$clearAdditionalKeys();
  }

  @Inject(method = "matches", at = @At("HEAD"), cancellable = true)
  private void multibind$matchesKey(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
    InputConstants.Key inputKey = InputConstants.getKey(event);
    if (multibind$matches(inputKey)) {
      cir.setReturnValue(true);
    }
  }

  @Inject(method = "matchesMouse", at = @At("HEAD"), cancellable = true)
  private void multibind$matchesMouse(MouseButtonEvent event, CallbackInfoReturnable<Boolean> cir) {
    InputConstants.Key inputKey = InputConstants.Type.MOUSE.getOrCreate(event.button());
    if (multibind$matches(inputKey)) {
      cir.setReturnValue(true);
    }
  }

  @Inject(method = "same", at = @At("HEAD"), cancellable = true)
  private void multibind$same(KeyMapping other, CallbackInfoReturnable<Boolean> cir) {
    MultiBindableKey otherBinding = (MultiBindableKey) (Object) other;
    if (multibind$sharesAnyKey(otherBinding)) {
      cir.setReturnValue(true);
    }
  }

  @Inject(method = "forAllKeyMappings", at = @At("HEAD"))
  private static void multibind$forAllKeyMappings(InputConstants.Key key, Consumer<KeyMapping> consumer,
      CallbackInfo ci) {
    for (KeyMapping mapping : ALL.values()) {
      MultiBindableKey multibindBinding = (MultiBindableKey) (Object) mapping;
      if (multibindBinding.multibind$getAdditionalKeys().contains(key)) {
        consumer.accept(mapping);
      }
    }
  }

  @Inject(method = "getTranslatedKeyMessage", at = @At("RETURN"), cancellable = true)
  private void multibind$displayAdditionalKeys(CallbackInfoReturnable<Component> cir) {
    Component display = multibind$buildDisplayMessage();
    if (display != null) {
      cir.setReturnValue(display);
    }
  }

  @Inject(method = "isDefault", at = @At("HEAD"), cancellable = true)
  private void multibind$isDefault(CallbackInfoReturnable<Boolean> cir) {
    if (!multibind$additionalKeys.isEmpty()) {
      cir.setReturnValue(false);
    }
  }

  @Override
  public InputConstants.Key multibind$getPrimaryKey() {
    return key;
  }

  @Override
  public Collection<InputConstants.Key> multibind$getAdditionalKeys() {
    return multibind$additionalKeys;
  }

  @Override
  public void multibind$replaceKey(InputConstants.Key key) {
    this.setKey(key);
  }

  @Override
  public void multibind$toggleAdditionalKey(InputConstants.Key key) {
    if (key == InputConstants.UNKNOWN || key.equals(this.key)) {
      return;
    }

    if (!multibind$additionalKeys.add(key)) {
      multibind$additionalKeys.remove(key);
    }

    MultibindBindings.INSTANCE.setAdditionalKeyNames(name, multibind$serializeAdditionalKeys());
  }

  @Override
  public void multibind$clearAdditionalKeys() {
    if (multibind$additionalKeys.isEmpty()) {
      return;
    }

    multibind$additionalKeys.clear();
    MultibindBindings.INSTANCE.setAdditionalKeyNames(name, multibind$serializeAdditionalKeys());
  }

  @Unique
  private boolean multibind$matches(InputConstants.Key inputKey) {
    return this.key.equals(inputKey) || multibind$additionalKeys.contains(inputKey);
  }

  @Unique
  private boolean multibind$sharesAnyKey(MultiBindableKey otherBinding) {
    if (otherBinding == null) {
      return false;
    }

    InputConstants.Key otherPrimary = otherBinding.multibind$getPrimaryKey();
    Collection<InputConstants.Key> otherAdditional = otherBinding.multibind$getAdditionalKeys();

    if (this.key.equals(otherPrimary) || multibind$additionalKeys.contains(otherPrimary)) {
      return true;
    }

    for (InputConstants.Key otherKey : otherAdditional) {
      if (this.key.equals(otherKey) || multibind$additionalKeys.contains(otherKey)) {
        return true;
      }
    }

    return false;
  }

  @Unique
  private Component multibind$buildDisplayMessage() {
    LinkedHashSet<InputConstants.Key> keys = new LinkedHashSet<>();
    if (!this.key.equals(InputConstants.UNKNOWN)) {
      keys.add(this.key);
    }
    keys.addAll(multibind$additionalKeys);

    if (keys.isEmpty()) {
      return null;
    }

    if (keys.size() == 1) {
      return keys.iterator().next().getDisplayName();
    }

    MutableComponent message = Component.empty();
    boolean first = true;
    for (InputConstants.Key binding : keys) {
      if (!first) {
        message.append(Component.literal(", "));
      }
      message.append(binding.getDisplayName());
      first = false;
    }
    return message;
  }

  @Unique
  private Collection<String> multibind$serializeAdditionalKeys() {
    LinkedHashSet<String> serialized = new LinkedHashSet<>();
    for (InputConstants.Key additionalKey : multibind$additionalKeys) {
      serialized.add(additionalKey.getName());
    }
    return serialized;
  }
}