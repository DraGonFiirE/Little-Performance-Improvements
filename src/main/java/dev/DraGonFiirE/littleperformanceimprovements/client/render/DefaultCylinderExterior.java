package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.*;
import com.mojang.blaze3d.vertex.*;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.render.mc.MeshDataExtender;
import team.creative.littletiles.common.math.box.*;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
/** Exterior-only Default drag mesh, with real-block grid strokes and native pulse. */
public final class DefaultCylinderExterior {
 public static PreviewRenderer.BoxRenderResult build(PreviewRenderer renderer,LittleBoxes boxes){
  if(boxes!=null){var result=CylinderDragExterior.build(renderer,boxes);DefaultCylinderGrid2.mark(result.data());return result;}
  var faces=CylinderDragExterior.faces(boxes);if(faces==null)return IndexedDefaultBoxMesh.build(renderer,boxes,false);
  var buffer=renderer.createBuffer();try{var builder=renderer.createBuilder(buffer,false);float scale=(float)boxes.grid.pixelLength;
   for(var f:faces)quad(builder,f,f[5],f[6],f[7],f[8],scale,0);
   // Grid strokes follow block planes; no boundaries from the mesher's row decomposition.
   int grid=boxes.grid.count;var strokes=new LinkedHashMap<String,List<float[]>>();
   for(var f:faces)for(int direction=0;direction<2;direction++){
    int lo=f[direction==0?5:7],hi=f[direction==0?6:8];long start=((long)Math.floorDiv(lo,grid)+1)*grid;
    for(long plane=start;plane<hi;plane+=grid){String key=f[0]+","+f[1]+","+f[2]+","+f[3]+","+f[4]+","+direction+","+plane;
     strokes.computeIfAbsent(key,k->new ArrayList<>()).add(new float[]{direction==0?f[7]:f[5],direction==0?f[8]:f[6]});
    }
    // Include a grid plane coinciding with a rectangle boundary; merge duplicates below.
    for(int plane:new int[]{lo,hi})if(Math.floorMod(plane,grid)==0){String key=f[0]+","+f[1]+","+f[2]+","+f[3]+","+f[4]+","+direction+","+plane;strokes.computeIfAbsent(key,k->new ArrayList<>()).add(new float[]{direction==0?f[7]:f[5],direction==0?f[8]:f[6]});}
   }
   float half=grid/512f;
   for(var entry:strokes.entrySet()){String[] k=entry.getKey().split(",");int[] f=new int[9];for(int i=0;i<5;i++)f[i]=Integer.parseInt(k[i]);int direction=Integer.parseInt(k[5]);float plane=Float.parseFloat(k[6]);var spans=entry.getValue();spans.sort(Comparator.comparingDouble(a->a[0]));float lo=spans.getFirst()[0],hi=spans.getFirst()[1];
    for(int i=1;i<=spans.size();i++){if(i<spans.size()&&spans.get(i)[0]<=hi){hi=Math.max(hi,spans.get(i)[1]);continue;}
     quad(builder,f,direction==0?plane-half:lo,direction==0?plane+half:hi,direction==0?lo:plane-half,direction==0?hi:plane+half,scale,0.00005f);
     if(i<spans.size()){lo=spans.get(i)[0];hi=spans.get(i)[1];}
    }
   }
   var mesh=builder.build();if(mesh instanceof MeshDataExtender ext)ext.keepAlive(true);return new PreviewRenderer.BoxRenderResult(boxes,boxes.pos,buffer,mesh);
  }catch(Throwable e){buffer.close();throw e;}
 }
 private static void quad(BufferBuilder builder,int[] f,float loA,float hiA,float loB,float hiB,float scale,float offset){int axis=f[0],a=f[3],b=f[4];boolean forward=((a+1)%3==b)==(f[1]==1);
  for(int j=0;j<4;j++){int corner=forward?j:(4-j)%4;float av=(corner==1||corner==2?hiA:loA)*scale,bv=(corner>=2?hiB:loB)*scale,p=f[2]*scale+(f[1]==1?offset:-offset);builder.addVertex(axis==0?p:a==0?av:bv,axis==1?p:a==1?av:bv,axis==2?p:a==2?av:bv).setColor(-1);}
 }
}
