package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import dev.DraGonFiirE.littleperformanceimprovements.blueprint.BlueprintPerformance;
import net.minecraft.world.item.ItemStack;

/** Geometry optimization must not be disabled by the item placeholder setting. */
public final class FullDetailPreviewCompaction {
    private FullDetailPreviewCompaction() {}

    public static boolean eligible(ItemStack stack) {
        // Small previews retain their inexpensive sequential upload. Existing
        // callers additionally require a full-detail preview; no setting or
        // geometry is changed here.
        return BlueprintPerformance.boxCount(stack) >= 4096;
    }
}
