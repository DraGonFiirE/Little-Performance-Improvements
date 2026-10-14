package dev.DraGonFiirE.littleperformanceimprovements.performance;

import dev.DraGonFiirE.littleperformanceimprovements.LittlePerformanceImprovements;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.CornerBoundaryPreview;
import java.lang.reflect.Method;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import team.creative.littletiles.client.tool.shaper.ShapeSelection;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;

public final class ShapePreviewTransform {
   private static final String ROTATION_DATA = "littleextratiles_rotation";
   private static volatile boolean resolved;
   private static volatile boolean failureLogged;
   private static Method applyMethod;

   private ShapePreviewTransform() {
   }

   public static String cacheKey(ItemStack stack) {
      CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
      CompoundTag rotation = data.getUnsafe().getCompound("littleextratiles_rotation");
      return rotation.isEmpty() ? "" : Integer.toHexString(rotation.hashCode());
   }

   public static LittleBoxes apply(ItemStack stack, ShapeSelection selection, LittleBoxes boxes, boolean lines, String source) {
      Method method = resolve();
      if (method != null && boxes != null) {
         LittleBoxes var10000;
         try {
            var10000 = method.invoke(null, stack, selection, boxes, lines, source) instanceof LittleBoxes result ? result : boxes;
         } catch (ReflectiveOperationException | LinkageError error) {
            logFailure(error);
            return CornerBoundaryPreview.apply(boxes, source);
         }

         return CornerBoundaryPreview.apply(var10000, source);
      } else {
         return CornerBoundaryPreview.apply(boxes, source);
      }
   }

   private static Method resolve() {
      if (resolved) {
         return applyMethod;
      }

      synchronized (ShapePreviewTransform.class) {
         if (resolved) {
            return applyMethod;
         }

         try {
            Class<?> filter = Class.forName(
               "dev.DraGonFiirE.littleextratiles.rotation.RotationLockShapeFilter", false, ShapePreviewTransform.class.getClassLoader()
            );
            applyMethod = filter.getMethod("apply", ItemStack.class, ShapeSelection.class, LittleBoxes.class, boolean.class, String.class);
         } catch (ClassNotFoundException ignored) {
            applyMethod = null;
         } catch (ReflectiveOperationException | LinkageError error) {
            logFailure(error);
         }

         resolved = true;
         return applyMethod;
      }
   }

   private static void logFailure(Throwable error) {
      if (!failureLogged) {
         failureLogged = true;
         LittlePerformanceImprovements.LOGGER.warn("Could not apply the optional LittleExtraTiles shape transform", error);
      }
   }
}
