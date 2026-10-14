package dev.DraGonFiirE.littleperformanceimprovements.client.tooltip;

import dev.DraGonFiirE.littleperformanceimprovements.blueprint.BlueprintPerformance;
import dev.DraGonFiirE.littleperformanceimprovements.mixin.PatchedDataComponentMapAccessor;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import team.creative.littletiles.LittleTilesRegistry;
import team.creative.littletiles.api.common.tool.ILittleTool;
import team.creative.littletiles.common.item.ItemLittleBlueprint;

public final class LittleToolTooltipCache {
   private static LittleToolTooltipCache.Entry cached;

   private LittleToolTooltipCache() {
   }

   public static boolean supports(ItemStack stack) {
      return stack != null && !stack.isEmpty() && stack.getItem() instanceof ILittleTool;
   }

   public static List<Component> lookup(ItemStack stack, Player player, TooltipFlag flag) {
      LittleToolTooltipCache.Entry entry = cached;
      int state = state(flag);
      int fingerprint = fingerprint(stack);
      Object screen = Minecraft.getInstance().screen;
      return entry != null
            && entry.stack().get() == stack
            && entry.player() == player
            && entry.screen() == screen
            && entry.state() == state
            && entry.fingerprint() == fingerprint
         ? new ArrayList<>(entry.lines())
         : null;
   }

   public static void store(ItemStack stack, Player player, TooltipFlag flag, List<Component> lines) {
      if (supports(stack) && lines != null) {
         cached = new LittleToolTooltipCache.Entry(
            new WeakReference<>(stack), player, Minecraft.getInstance().screen, state(flag), fingerprint(stack), List.copyOf(lines)
         );
      }
   }

   public static void clear() {
      cached = null;
   }

   private static int state(TooltipFlag flag) {
      int state = flag.isAdvanced() ? 1 : 0;
      state = 31 * state + (flag.isCreative() ? 1 : 0);
      state = 31 * state + (Screen.hasShiftDown() ? 1 : 0);
      state = 31 * state + (Screen.hasControlDown() ? 1 : 0);
      return 31 * state + (Screen.hasAltDown() ? 1 : 0);
   }

   private static int fingerprint(ItemStack stack) {
      int hash = 31 + stack.getCount();
      if (stack.getComponents() instanceof PatchedDataComponentMap components) {
         Reference2ObjectMap<DataComponentType<?>, Optional<?>> patch = ((PatchedDataComponentMapAccessor)(Object)components).littleperformanceimprovements$getPatch();
         hash = 31 * hash + patch.size();

         for (Reference2ObjectMap.Entry<DataComponentType<?>, Optional<?>> entry : patch.reference2ObjectEntrySet()) {
            hash = 31 * hash + System.identityHashCode(entry.getKey());
            hash = 31 * hash + System.identityHashCode(entry.getValue().orElse(null));
         }
      }

      CustomData vanillaData = stack.get(DataComponents.CUSTOM_DATA);
      if (vanillaData != null) {
         hash = 31 * hash + vanillaData.hashCode();
      }

      CustomData littleData = stack.get(LittleTilesRegistry.DATA);
      if (littleData == null) {
         return hash;
      }

      if (!(stack.getItem() instanceof ItemLittleBlueprint)) {
         return 31 * hash + littleData.hashCode();
      }

      CompoundTag content = BlueprintPerformance.content(stack);
      hash = 31 * hash + content.getInt("tiles");
      hash = 31 * hash + content.getInt("boxes");
      if (content.contains("s")) {
         hash = 31 * hash + content.getCompound("s").getString("n").hashCode();
      }

      return hash;
   }

   private record Entry(WeakReference<ItemStack> stack, Player player, Object screen, int state, int fingerprint, List<Component> lines) {
   }
}
