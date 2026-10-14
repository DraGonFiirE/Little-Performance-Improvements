package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import team.creative.littletiles.common.math.box.*;import team.creative.littletiles.common.math.box.collection.*;
/** FlatWallBlockUnion has already split both footprint axes at every block boundary. */
public final class WallLayerSplit {
 public static LittleBoxes split(LittleBoxes flat,int axis,int low,int high){
 int grid=flat.grid.count;var result=new LittleBoxesSimple(flat.pos,flat.grid);if(high<=low)return result;
 long count=(long)Math.floorDiv(high-1,grid)-Math.floorDiv(low,grid)+1;if(count>1000000)return DefaultCylinderBlocks.split(flat);
 int[] edges=new int[(int)count+1];edges[0]=low;for(int i=1;i<count;i++)edges[i]=(int)(((long)Math.floorDiv(low,grid)+i)*grid);edges[(int)count]=high;
 for(var b:flat.all())for(int i=0;i<count;i++){
 if(axis==0)result.add(new LittleBox(edges[i],b.minY,b.minZ,edges[i+1],b.maxY,b.maxZ));
 else if(axis==1)result.add(new LittleBox(b.minX,edges[i],b.minZ,b.maxX,edges[i+1],b.maxZ));
 else result.add(new LittleBox(b.minX,b.minY,edges[i],b.maxX,b.maxY,edges[i+1]));
 }return result;
 }
}
