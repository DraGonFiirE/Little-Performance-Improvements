package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import java.io.File;
import net.minecraft.core.HolderLookup;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import team.creative.creativecore.Side;
import team.creative.creativecore.common.config.event.ConfigEventHandler;
import dev.DraGonFiirE.littleperformanceimprovements.config.InitialBoxCountMigration;

@Mixin(ConfigEventHandler.class)
public abstract class InitialBoxCountConfigMixin {
    @Shadow @Final private File CONFIG_DIRECTORY;

    @Inject(method = "load(Lnet/minecraft/core/HolderLookup$Provider;Ljava/lang/String;Lteam/creative/creativecore/Side;)V", at = @At("HEAD"), require = 1)
    private void littleperformanceimprovements$initializeBoxCounts(HolderLookup.Provider provider,
            String mod, Side side, CallbackInfo callback) {
        if (!side.isClient() || !"littletiles".equals(mod)) return;
        try {
            InitialBoxCountMigration.apply(CONFIG_DIRECTORY.toPath());
        } catch (Exception exception) {
            org.apache.logging.log4j.LogManager.getLogger("LittlePerformanceImprovements")
                    .error("Could not apply initial LT box counts; leaving normal config loading intact", exception);
        }
    }
}
