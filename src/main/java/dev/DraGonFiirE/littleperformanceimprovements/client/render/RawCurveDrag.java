package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import team.creative.littletiles.common.placement.shape.type.LittleShapeCurve;
import team.creative.littletiles.common.placement.shape.config.InterpolationThicknessConfig;
import team.creative.littletiles.client.tool.shaper.ShapeSelection;
import team.creative.littletiles.common.math.box.collection.*;
public final class RawCurveDrag extends LittleShapeCurve {
 public static LittleBoxes sample(ShapeSelection s,InterpolationThicknessConfig c){var result=new LittleBoxesSimple(s.pos,s.grid);new RawCurveDrag().build(result,s,c);return result;}
}
