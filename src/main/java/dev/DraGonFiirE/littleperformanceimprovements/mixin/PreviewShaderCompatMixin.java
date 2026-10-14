package dev.DraGonFiirE.littleperformanceimprovements.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.phys.Vec3;
import team.creative.littletiles.client.tool.LittleTool;
import team.creative.littletiles.client.render.overlay.*;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.ShaderPreviewPass;
@Mixin(PreviewManager.class)
public abstract class PreviewShaderCompatMixin {
 @Redirect(method={"tick","drawNonHighlight"},at=@At(value="INVOKE",target="Lteam/creative/littletiles/client/tool/LittleTool;render(Lteam/creative/littletiles/client/render/overlay/PreviewRenderer;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/phys/Vec3;Z)V"),require=2)
 private void littleperformanceimprovements$shaderPreview(LittleTool tool,PreviewRenderer renderer,PoseStack pose,Vec3 camera,boolean highlight){ShaderPreviewPass.render(tool,renderer,pose,camera,highlight);}
}
