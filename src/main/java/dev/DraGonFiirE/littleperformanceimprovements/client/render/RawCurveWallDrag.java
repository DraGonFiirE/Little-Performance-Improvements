package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import team.creative.littletiles.common.placement.shape.type.LittleShapeCurveWall;
import team.creative.littletiles.common.placement.shape.config.InterpolationAxisThicknessConfig;
import team.creative.littletiles.client.tool.shaper.ShapeSelection;
import team.creative.littletiles.common.math.box.collection.*;
public final class RawCurveWallDrag extends LittleShapeCurveWall {
 public static LittleBoxes sample(ShapeSelection s,InterpolationAxisThicknessConfig c){var cached=CachedCurveWallSegments.sample(s,c);if(cached!=null)return cached;var result=new LittleBoxesSimple(s.pos,s.grid);new RawCurveWallDrag().build(result,s,c);return result;}
}
