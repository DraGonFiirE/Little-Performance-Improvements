package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.collection.*;
/** Keep block boundaries visible without rerunning the native voxel sampler. */
public final class DefaultCylinderBlocks {
 public static LittleBoxes split(LittleBoxes input){
  var result=new LittleBoxesSimple(input.pos,input.grid);int grid=input.grid.count;
  for(var b:input.all()){
   for(int x=b.minX;x<b.maxX;){int endX=end(x,b.maxX,grid);
    for(int y=b.minY;y<b.maxY;){int endY=end(y,b.maxY,grid);
     for(int z=b.minZ;z<b.maxZ;){int endZ=end(z,b.maxZ,grid);result.add(new LittleBox(x,y,z,endX,endY,endZ));z=endZ;}
     y=endY;
    }x=endX;
   }
  }return result;
 }
 private static int end(int value,int max,int grid){return (int)Math.min((long)max,((long)Math.floorDiv(value,grid)+1)*grid);}
}
