package clood.multibind.client.gui;

import clood.multibind.MultiBindableKey;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AdditionalKeybindsScreen extends Screen {
  private static final int PADDING = 10;
  private static final int BUTTON_HEIGHT = 20;
  private static final int KEY_LIST_HEIGHT = 120;
  
  private final Screen parent;
  private final KeyMapping keyBinding;
  private final MultiBindableKey binding;
  private final List<InputConstants.Key> additionalKeys;
  
  private int listTop;
  private int listBottom;
  private boolean addingNewKey = false;

  public AdditionalKeybindsScreen(Screen parent, KeyMapping keyBinding) {
    super(Component.literal("Additional Keybinds: " + keyBinding.getName()));
    this.parent = parent;
    this.keyBinding = keyBinding;
    this.binding = (MultiBindableKey) keyBinding;
    this.additionalKeys = new ArrayList<>(this.binding.multibind$getAdditionalKeys());
  }

  @Override
  protected void init() {
    this.clearWidgets();
    
    int contentWidth = Math.min(300, this.width - PADDING * 2);
    int contentLeft = (this.width - contentWidth) / 2;
    int y = PADDING * 2;
    
    // Top buttons: Add Key, Clear All, Done
    int buttonWidth = (contentWidth - PADDING) / 3;
    
    this.addRenderableWidget(
        new net.minecraft.client.gui.components.Button.Builder(Component.literal("Add Key"), button -> {
          this.addingNewKey = true;
        })
            .pos(contentLeft, y)
            .width(buttonWidth)
            .build());
    
    this.addRenderableWidget(
        new net.minecraft.client.gui.components.Button.Builder(Component.literal("Clear All"), button -> {
          this.binding.multibind$clearAdditionalKeys();
          this.additionalKeys.clear();
          this.init();
        })
            .pos(contentLeft + buttonWidth + PADDING / 2, y)
            .width(buttonWidth)
            .build());
    
    this.addRenderableWidget(
        new net.minecraft.client.gui.components.Button.Builder(Component.literal("Done"), button -> this.onClose())
            .pos(contentLeft + buttonWidth * 2 + PADDING, y)
            .width(buttonWidth)
            .build());
    
    this.listTop = y + BUTTON_HEIGHT + PADDING;
    this.listBottom = this.height - PADDING - BUTTON_HEIGHT;
  }

  @Override
  public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
    super.render(graphics, mouseX, mouseY, delta);
    
    int contentWidth = Math.min(300, this.width - PADDING * 2);
    int contentLeft = (this.width - contentWidth) / 2;
    
    // Draw instruction
    String instruction = this.addingNewKey ? "Press a key to add..." : "Click a key to remove it";
    graphics.drawCenteredString(this.font, instruction, this.width / 2, this.listTop - 15, 0xCCCCCC);
    
    // Draw list background
    graphics.fill(contentLeft, this.listTop, contentLeft + contentWidth, this.listBottom, 0x8B000000);
    
    // Draw additional keys as clickable text
    int keyY = this.listTop + PADDING;
    for (InputConstants.Key key : this.additionalKeys) {
      String keyName = key.getDisplayName().getString();
      
      boolean isHovered = mouseX >= contentLeft + PADDING && mouseX <= contentLeft + contentWidth - PADDING &&
                          mouseY >= keyY && mouseY <= keyY + 15;
      
      int color = isHovered ? 0xFF4BA05B : 0xFFAAAAAA;
      graphics.drawCenteredString(this.font, keyName, this.width / 2, keyY, color);
      
      keyY += 20;
    }
  }

  @Override
  public boolean keyPressed(KeyEvent event) {
    if (this.addingNewKey) {
      InputConstants.Key inputKey = InputConstants.getKey(event);
      if (inputKey.getType() == InputConstants.Type.KEYSYM && inputKey.getValue() == InputConstants.KEY_ESCAPE) {
        this.addingNewKey = false;
        return true;
      }
      this.binding.multibind$toggleAdditionalKey(inputKey);
      this.additionalKeys.clear();
      this.additionalKeys.addAll(this.binding.multibind$getAdditionalKeys());
      this.addingNewKey = false;
      this.init();
      return true;
    }
    return super.keyPressed(event);
  }

  @Override
  public boolean mouseClicked(MouseButtonEvent event, boolean handled) {
    if (this.addingNewKey) {
      InputConstants.Key inputKey = InputConstants.Type.MOUSE.getOrCreate(event.button());
      this.binding.multibind$toggleAdditionalKey(inputKey);
      this.additionalKeys.clear();
      this.additionalKeys.addAll(this.binding.multibind$getAdditionalKeys());
      this.addingNewKey = false;
      this.init();
      return true;
    }
    
    // Check if clicking on a key in the list
    int contentWidth = Math.min(300, this.width - PADDING * 2);
    int contentLeft = (this.width - contentWidth) / 2;
    
    int keyY = this.listTop + PADDING;
    for (InputConstants.Key key : this.additionalKeys) {
      if (event.x() >= contentLeft + PADDING && event.x() <= contentLeft + contentWidth - PADDING &&
          event.y() >= keyY && event.y() <= keyY + 15) {
        this.binding.multibind$toggleAdditionalKey(key);
        this.additionalKeys.remove(key);
        this.init();
        return true;
      }
      keyY += 20;
    }
    
    return super.mouseClicked(event, handled);
  }
  
  @Override
  public void onClose() {
    this.minecraft.setScreen(this.parent);
  }
}
