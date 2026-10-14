package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import team.creative.creativecore.common.util.math.base.Axis;
import team.creative.littletiles.client.tool.shaper.ShapeSelection;
import team.creative.littletiles.common.placement.shape.LittleShape;
import team.creative.littletiles.common.placement.shape.type.LittleShapeCylinder;
import team.creative.littletiles.common.placement.shape.config.*;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.collection.*;
public final class FastCylinderDrag {
 public static LittleBoxes build(LittleShape shape,ShapeSelection selection,LittleShapeConfig config){
  if(shape.getClass()!=LittleShapeCylinder.class||!(config instanceof AxisHollowThicknessShapeConfig c))return shape.build(selection,config);
  LittleBox b=selection.getOverallBox();Axis axis=c.axis;
  int u=axis==Axis.X?b.maxY-b.minY:b.maxX-b.minX;
  int v=axis==Axis.Z?b.maxY-b.minY:b.maxZ-b.minZ;
  int[] rows=CylinderDragRows.build(u,v,c.hollow,c.thickness);
  LittleBoxesSimple boxes=new LittleBoxesSimple(selection.pos,selection.grid);
  for(int i=0;i<rows.length;i+=4){int a=rows[i],lo=rows[i+1],z=rows[i+2],hi=rows[i+3];
   if(axis==Axis.X)boxes.add(new LittleBox(b.minX,b.minY+a,b.minZ+lo,b.maxX,b.minY+z,b.minZ+hi));
   else if(axis==Axis.Z)boxes.add(new LittleBox(b.minX+a,b.minY+lo,b.minZ,b.minX+z,b.minY+hi,b.maxZ));
   else boxes.add(new LittleBox(b.minX+a,b.minY,b.minZ+lo,b.minX+z,b.maxY,b.minZ+hi));
  }
  return boxes;
 }
}
