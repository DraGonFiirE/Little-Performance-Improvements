package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.model.data.ModelData;
import team.creative.creativecore.client.render.box.*;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.math.geo.VectorFan;
import team.creative.creativecore.common.util.math.vec.Vec3f;
import team.creative.littletiles.client.render.tile.LittleRenderBoxTransformable;
/** Transformed fans must be clipped in block-local coordinates before UV generation. */
public final class ChiselTransformTextures {
 public static List<BakedQuad> quads(RenderBox box,QuadGeneratorContext context,LevelAccessor level,BlockPos pos,BlockPos offset,BlockState state,BakedModel model,ModelData data,Facing facing,RenderType layer,RandomSource random,boolean override,int tint) {
  if(!(box instanceof LittleRenderBoxTransformable)){
   float plane=facing.positive?facing.axis.get(box.maxX,box.maxY,box.maxZ):facing.axis.get(box.minX,box.minY,box.minZ);
   int cell=facing.positive?(int)Math.ceil(plane)-1:(int)Math.floor(plane);
   if(facing.axis.get(offset)!=cell)return List.of();
  }
  if(!(box instanceof LittleRenderBoxTransformable) && !box.getFace(facing).hasCachedFans())return box.getBakedQuad(context,level,pos,offset,state,model,data,facing,layer,random,override,tint);
  var face=box.getFace(facing);List<VectorFan> fans;
  float scale=box.getPreviewScaleX();
  if(face.hasCachedFans()){fans=face.getCachedFans();scale=face.getScale();}
  else {fans=new ArrayList<>();if(box instanceof LittleRenderBoxTransformable transformed){var cache=transformed.getFaceCache(facing);if(cache!=null){if(face.shouldRender()&&cache.hasAxisStrip())fans.addAll(cache.axisStrips);if(cache.hasTiltedStripsRendering())cache.collectAllTiltedStripsRendering(fans);}}}
  if(fans.isEmpty())return List.of();
  boolean cachedOrdinary=face.hasCachedFans() && !(box instanceof LittleRenderBoxTransformable);
  float ox=box.getPreviewOffX(),oy=box.getPreviewOffY(),oz=box.getPreviewOffZ();
  float sx=cachedOrdinary?box.getPreviewScaleX():scale,sy=cachedOrdinary?box.getPreviewScaleY():scale,sz=cachedOrdinary?box.getPreviewScaleZ():scale;
  List<VectorFan> clipped=new ArrayList<>(Math.min(4,fans.size()));
  for(Object o:fans){VectorFan fan=(VectorFan)o;
   if(cachedOrdinary && TextureRectangleClip.append(clipped,fan,sx,sy,sz,ox,oy,oz,offset.getX(),offset.getY(),offset.getZ()))continue;
   List<Vec3f> points=new ArrayList<>();
   for(int i=0;i<fan.count();i++){Vec3f p=fan.get(i);points.add(new Vec3f(p.x*sx+ox-offset.getX(),p.y*sy+oy-offset.getY(),p.z*sz+oz-offset.getZ()));}
   for(int axis=0;axis<3&&!points.isEmpty();axis++){points=clip(points,axis,0,true);points=clip(points,axis,1,false);}
   if(points.size()>=3&&area(points)>1e-16)clipped.add(new VectorFan((Vec3f[])points.toArray(Vec3f[]::new)));
  }
  if(clipped.isEmpty())return List.of();
  RenderBox local=new LocalFace(state,clipped);local.color=box.color;
  List<BakedQuad> result=local.getBakedQuad(context,level,pos,BlockPos.ZERO,state,model,data,facing,layer,random,override,tint);
  for(BakedQuad q:result){int[] v=q.getVertices();int stride=v.length/4;for(int i=0;i<4;i++){int at=i*stride;v[at]=Float.floatToRawIntBits(Float.intBitsToFloat(v[at])+offset.getX());v[at+1]=Float.floatToRawIntBits(Float.intBitsToFloat(v[at+1])+offset.getY());v[at+2]=Float.floatToRawIntBits(Float.intBitsToFloat(v[at+2])+offset.getZ());}}
  return result;
 }
 static class LocalFace extends RenderBox {
  final List<VectorFan> fans;
  LocalFace(BlockState state,List<VectorFan> fans){super(0,0,0,1,1,1,state);this.fans=fans;}
  public Object getRenderQuads(Facing facing){return fans;}
  protected boolean scaleAndOffsetQuads(Facing facing){return false;}
 }
 static float coord(Vec3f p,int a){return a==0?p.x:a==1?p.y:p.z;}
 static List<Vec3f> clip(List<Vec3f> input,int axis,float plane,boolean lower){
  List<Vec3f> result=new ArrayList<>();if(input.isEmpty())return result;Vec3f prev=input.getLast();float pd=coord(prev,axis)-plane;boolean pi=lower?pd>=0:pd<=0;
  for(Vec3f p:input){float d=coord(p,axis)-plane;boolean inside=lower?d>=0:d<=0;if(pi!=inside){float t=pd/(pd-d);result.add(new Vec3f(prev.x+t*(p.x-prev.x),prev.y+t*(p.y-prev.y),prev.z+t*(p.z-prev.z)));}if(inside)result.add(p);prev=p;pd=d;pi=inside;}return result;
 }
 static double area(List<Vec3f> ps){double x=0,y=0,z=0;Vec3f a=ps.getFirst();for(int i=1;i<ps.size()-1;i++){Vec3f b=ps.get(i),c=ps.get(i+1);x+=(b.y-a.y)*(c.z-a.z)-(b.z-a.z)*(c.y-a.y);y+=(b.z-a.z)*(c.x-a.x)-(b.x-a.x)*(c.z-a.z);z+=(b.x-a.x)*(c.y-a.y)-(b.y-a.y)*(c.x-a.x);}return x*x+y*y+z*z;}
}
