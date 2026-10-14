package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.*;
import net.minecraft.core.BlockPos;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.collection.*;
/** Union in block-sized bit rows, emitted as disjoint maximal boxes. */
public final class BlockDefaultUnion {
 public static LittleBoxes build(LittleBoxes raw){
  int g=raw.grid.count;if(g>64)return SparseDefaultUnion.build(raw);
  var blocks=new LinkedHashMap<BlockPos,long[]>();var seen=new HashSet<LittleBox>();
  for(var b:raw.all())if(seen.add(b)){
   for(int bx=Math.floorDiv(b.minX,g);bx<=Math.floorDiv(b.maxX-1,g);bx++)for(int by=Math.floorDiv(b.minY,g);by<=Math.floorDiv(b.maxY-1,g);by++)for(int bz=Math.floorDiv(b.minZ,g);bz<=Math.floorDiv(b.maxZ-1,g);bz++){
    var rows=blocks.computeIfAbsent(new BlockPos(bx,by,bz),k->new long[g*g]);
    int x0=Math.max(0,b.minX-bx*g),x1=Math.min(g,b.maxX-bx*g),y0=Math.max(0,b.minY-by*g),y1=Math.min(g,b.maxY-by*g),z0=Math.max(0,b.minZ-bz*g),z1=Math.min(g,b.maxZ-bz*g);long mask=mask(x0,x1);
    for(int z=z0;z<z1;z++)for(int y=y0;y<y1;y++)rows[z*g+y]|=mask;
   }
  }
  var result=new LittleBoxesSimple(raw.pos,raw.grid);
  for(var entry:blocks.entrySet()){
   var p=entry.getKey();var rows=entry.getValue();
   for(int z=0;z<g;z++)for(int y=0;y<g;y++)while(rows[z*g+y]!=0){
    int x=Long.numberOfTrailingZeros(rows[z*g+y]);int endX=x+Long.numberOfTrailingZeros(~(rows[z*g+y]>>>x));endX=Math.min(endX,g);long m=mask(x,endX);
    int endY=y+1;while(endY<g&&(rows[z*g+endY]&m)==m)endY++;
    int endZ=z+1;outer:while(endZ<g){for(int yy=y;yy<endY;yy++)if((rows[endZ*g+yy]&m)!=m)break outer;endZ++;}
    for(int zz=z;zz<endZ;zz++)for(int yy=y;yy<endY;yy++)rows[zz*g+yy]&=~m;
    result.add(new LittleBox(p.getX()*g+x,p.getY()*g+y,p.getZ()*g+z,p.getX()*g+endX,p.getY()*g+endY,p.getZ()*g+endZ));
   }
  }return result;
 }
 private static long mask(int lo,int hi){return (-1L>>> (64-(hi-lo)))<<lo;}
}
