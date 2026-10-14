package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import java.lang.reflect.Field;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.creative.creativecore.common.config.core.IConfigRegistry;
import team.creative.creativecore.common.config.holder.ConfigHolderObject;
import team.creative.littletiles.common.config.LittleConfigRendering;

/** Keeps the injected preview-renderer fields together in the LT config GUI. */
@Mixin(ConfigHolderObject.class)
public abstract class ConfigHolderObjectOrderingMixin {
   @Inject(method = "collectFields", at = @At("RETURN"))
   private static void littleperformanceimprovements$orderPreviewRendererFields(
         Class<?> type,
         List<Field> fields,
         IConfigRegistry registry,
         CallbackInfoReturnable<List<Field>> callback) {
      if (type != LittleConfigRendering.class)
         return;

      int heldIndex = littleperformanceimprovements$find(fields, "heldItemSkipBoxCount");
      if (heldIndex >= 0 && littleperformanceimprovements$find(fields, "itemLowResolutionBoxCount") >= 0) {
         Field held = fields.remove(heldIndex);
         int itemIndex = littleperformanceimprovements$find(fields, "itemLowResolutionBoxCount");
         fields.add(itemIndex + 1, held);
      }

      // LittleTiles' old boolean/width pair has been superseded by the
      // renderer enum and its mode-specific settings. Excluding the inherited
      // fields here removes them from both config serialization and the
      // generated /cmdclientconfig page.
      littleperformanceimprovements$remove(fields, "previewLines");
      littleperformanceimprovements$remove(fields, "previewLineThickness");

      String[] names = {
            "blueprintRenderer",
            "blueprintColor",
            "shapeRenderer",
            "shapeColor",
            "previewOutlineThickness",
            "previewOutlineAlpha",
            "previewSurfaceAlpha",
            "previewTextureAlpha"
      };
      Field[] ordered = new Field[names.length];
      for (int index = 0; index < names.length; index++) {
         int fieldIndex = littleperformanceimprovements$find(fields, names[index]);
         if (fieldIndex < 0)
            return;
         ordered[index] = fields.remove(fieldIndex);
      }

      int anchor = littleperformanceimprovements$find(fields, "enableRandomDisplayTick");
      if (anchor < 0)
         anchor = fields.size();
      for (int index = 0; index < ordered.length; index++)
         fields.add(anchor + index, ordered[index]);
   }

   @Inject(method = "load", at = @At("HEAD"))
   private void littleperformanceimprovements$migratePreviewSettings(
         net.minecraft.core.HolderLookup.Provider provider, boolean loadDefault, boolean ignoreRestart,
         com.google.gson.JsonObject json, team.creative.creativecore.Side side,
         org.spongepowered.asm.mixin.injection.callback.CallbackInfo callback) {
      if (((ConfigHolderObject)(Object)this).object instanceof LittleConfigRendering)
         dev.DraGonFiirE.littleperformanceimprovements.config.PreviewConfigMigration.migrate(json);
   }

   @Unique
   private static int littleperformanceimprovements$find(List<Field> fields, String name) {
      for (int index = 0; index < fields.size(); index++)
         if (fields.get(index).getName().equals(name))
            return index;
      return -1;
   }

   @Unique
   private static void littleperformanceimprovements$remove(List<Field> fields, String name) {
      int index = littleperformanceimprovements$find(fields, name);
      if (index >= 0)
         fields.remove(index);
   }
}
