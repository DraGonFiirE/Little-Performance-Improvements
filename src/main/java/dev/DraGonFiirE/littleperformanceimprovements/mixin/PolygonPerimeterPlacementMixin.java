package dev.DraGonFiirE.littleperformanceimprovements.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import team.creative.littletiles.client.tool.shaper.LittleToolShaper;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.PolygonPerimeterPreview;
@Mixin(value=LittleToolShaper.class,remap=false)
public class PolygonPerimeterPlacementMixin {
 @Redirect(method="interact",at=@At(value="INVOKE",target="Lteam/creative/littletiles/client/tool/shaper/LittleToolShaper;getShapeResult()Lteam/creative/littletiles/client/render/overlay/PreviewRenderer$BoxRenderResult;"),require=1)
 private PreviewRenderer.BoxRenderResult completePolygonForPlacement(LittleToolShaper tool){return PolygonPerimeterPreview.placement(tool.getShapeResult());}
}
