package dev.DraGonFiirE.littleperformanceimprovements.mixin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import team.creative.littletiles.common.placement.shape.type.LittleShapeBlob;
import team.creative.littletiles.common.placement.shape.type.LittleShapeSphere;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
import team.creative.littletiles.client.tool.shaper.ShapeSelection;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.BlobSphereGeometry;
@Mixin(LittleShapeBlob.class)
public abstract class BlobShapeCacheMixin {
 @Redirect(method="get",at=@At(value="INVOKE",target="Lteam/creative/littletiles/common/placement/shape/type/LittleShapeSphere;build(Lteam/creative/littletiles/client/tool/shaper/ShapeSelection;Ljava/lang/Object;)Lteam/creative/littletiles/common/math/box/collection/LittleBoxes;"),require=1)
 private LittleBoxes littleperformanceimprovements$filledBlob(LittleShapeSphere sphere,ShapeSelection selection,Object config){return BlobSphereGeometry.buildForBlob(sphere,selection,config);}
}
