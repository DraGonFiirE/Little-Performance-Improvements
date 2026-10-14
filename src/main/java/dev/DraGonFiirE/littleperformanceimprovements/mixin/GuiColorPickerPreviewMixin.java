package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import dev.DraGonFiirE.littleperformanceimprovements.access.PreviewColorPickerAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import team.creative.creativecore.common.gui.GuiParent;
import team.creative.creativecore.common.gui.control.simple.GuiColorPicker;
import team.creative.creativecore.common.gui.control.simple.GuiColoredSteppedSlider;
import team.creative.creativecore.common.util.type.Color;

@Mixin(GuiColorPicker.class)
public abstract class GuiColorPickerPreviewMixin extends GuiParent implements PreviewColorPickerAccess {
   @Shadow
   protected GuiColoredSteppedSlider a;
   @Shadow
   public Color color;

   protected GuiColorPickerPreviewMixin(String name) {
      super(name);
   }

   @Override
   public void littleperformanceimprovements$hideAlphaChannel() {
      if (this.a != null && this.a.getParent() instanceof GuiParent alphaRow && alphaRow.getParent() instanceof GuiParent sliderColumn) {
         sliderColumn.remove(alphaRow);
         this.color.setAlpha(255);
         this.a = null;
      }
   }
}
