package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewConfigMigration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.creativecore.common.config.key.ConfigKey;
import team.creative.creativecore.common.gui.GuiControl;
import team.creative.creativecore.common.gui.GuiParent;

@Mixin(targets = "team.creative.creativecore.common.config.converation.ConfigTypeConveration$7")
public abstract class PreviewRendererConfigWidthMixin {
   @Inject(method = "createControls", at = @At("RETURN"))
   private void littleperformanceimprovements$expandPreviewRenderer(GuiParent parent, ConfigKey key, CallbackInfo info) {
      if (PreviewConfigMigration.isRendererKey(key.name)) {
         GuiControl control = parent.get("data");
         if (control != null) {
            control.setExpandableX();
         }
      }
   }
}
