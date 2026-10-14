package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import team.creative.littletiles.common.placement.shape.type.LittleShapeCatenary;
import team.creative.littletiles.common.placement.shape.config.CatenaryConfig;
import team.creative.littletiles.client.tool.shaper.ShapeSelection;
import team.creative.littletiles.common.math.box.collection.*;
public final class RawCatenaryDrag extends LittleShapeCatenary {
 public static LittleBoxes sample(ShapeSelection s,CatenaryConfig c){var result=new LittleBoxesSimple(s.pos,s.grid);new RawCatenaryDrag().build(result,s,c);return result;}
}
