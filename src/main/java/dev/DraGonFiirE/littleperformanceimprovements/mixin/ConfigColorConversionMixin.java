package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.access.PreviewColorPickerAccess;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewConfigMigration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.creativecore.common.config.key.ConfigKey;
import team.creative.creativecore.common.gui.GuiParent;
import team.creative.creativecore.common.gui.control.simple.GuiColorPicker;

@Mixin(targets = "team.creative.creativecore.common.config.converation.ConfigTypeConveration$9", remap = false)
public abstract class ConfigColorConversionMixin {
   @Inject(method = "createControls", at = @At("RETURN"))
   private void littleperformanceimprovements$configurePreviewColor(GuiParent parent, ConfigKey key, CallbackInfo info) {
      if (PreviewConfigMigration.isColorKey(key.name)) {
         GuiColorPicker picker = parent.get("color", GuiColorPicker.class);
         if (picker instanceof PreviewColorPickerAccess access) {
            access.littleperformanceimprovements$hideAlphaChannel();
         }
      }
   }
}
