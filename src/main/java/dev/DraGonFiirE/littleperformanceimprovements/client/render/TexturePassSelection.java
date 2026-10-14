package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.*;
import net.minecraft.world.inventory.InventoryMenu;
import java.nio.*;
import java.util.*;
import java.lang.reflect.Field;
/** Build-time material classification; unknown and mixed-alpha ranges retain both passes. */
public final class TexturePassSelection {
 private static final Map<TextureDrawLayout,byte[]> CACHE=Collections.synchronizedMap(new WeakHashMap<>());
 private static final ThreadLocal<Integer> PASS=ThreadLocal.withInitial(()->0);
 public static long wholeThreshold(){return PASS.get()==0?97L:101L;}
 public static void pass(int mask){PASS.set(mask);}
 public static boolean accept(TextureDrawLayout layout,int index){int pass=PASS.get();if(pass==0)return true;byte[] masks=CACHE.get(layout);return masks==null || index>=masks.length || (masks[index]&pass)!=0;}
 private static Object field(Object o,String name)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(o);}
 public static TextureDrawLayout register(TextureDrawLayout layout,MeshData mesh){
  if(layout==null||CACHE.containsKey(layout))return layout;
  try{
   if(mesh.drawState().format().getVertexSize()!=24)return layout;
   TextureAtlas atlas=(TextureAtlas)Minecraft.getInstance().getTextureManager().getTexture(InventoryMenu.BLOCK_ATLAS);
   List<TextureAtlasSprite> sprites=new ArrayList<>(atlas.getTextures().values());
   List<TextureAtlasSprite>[] bins=new List[4096];
   for(TextureAtlasSprite s:sprites)for(int y=Math.max(0,(int)(s.getV0()*64));y<=Math.min(63,(int)(s.getV1()*64));y++)for(int x=Math.max(0,(int)(s.getU0()*64));x<=Math.min(63,(int)(s.getU1()*64));x++){int i=y*64+x;if(bins[i]==null)bins[i]=new ArrayList<>();bins[i].add(s);}
   Map<TextureAtlasSprite,Integer> flags=new IdentityHashMap<>();
   ByteBuffer v=mesh.vertexBuffer().duplicate().order(ByteOrder.nativeOrder());Object[] ranges=(Object[])field(layout,"ranges");byte[] masks=new byte[ranges.length];
   for(int i=0;i<ranges.length;i++){
    if((i&255)==0)OutlineWorkChunks.check();int first=(int)field(ranges[i],"firstQuad"),count=(int)field(ranges[i],"quadCount"),mask=0;
    for(int q=first;q<first+count&&mask!=3;q++){
     int off=q*96,minAlpha=255,maxAlpha=0;float u0=Float.POSITIVE_INFINITY,u1=Float.NEGATIVE_INFINITY,v0=u0,v1=u1;
     for(int j=0;j<4;j++){int k=off+j*24,a=v.get(k+23)&255;minAlpha=Math.min(minAlpha,a);maxAlpha=Math.max(maxAlpha,a);float u=v.getFloat(k+12),vv=v.getFloat(k+16);u0=Math.min(u0,u);u1=Math.max(u1,u);v0=Math.min(v0,vv);v1=Math.max(v1,vv);}
     if(maxAlpha==0)continue;if(maxAlpha<255){mask|=2;continue;}
     float u=(u0+u1)*.5f,vv=(v0+v1)*.5f;int spriteMask=3;
     if(Float.isFinite(u)&&Float.isFinite(vv)&&u>=0&&u<1&&vv>=0&&vv<1){var bin=bins[(int)(vv*64)*64+(int)(u*64)];if(bin!=null)for(TextureAtlasSprite s:bin)if(u0>=s.getU0()&&u1<=s.getU1()&&v0>=s.getV0()&&v1<=s.getV1()){
      spriteMask=flags.computeIfAbsent(s,TexturePassSelection::classify);break;
     }}
     mask|=spriteMask;if(minAlpha<255&&(spriteMask!=0))mask|=2;
    }masks[i]=(byte)mask;
   }CACHE.put(layout,masks);
  }catch(java.util.concurrent.CancellationException e){throw e;}catch(Exception e){System.err.println("Texture pass classification fallback: "+e);}
  return layout;
 }
 private static int classify(TextureAtlasSprite sprite){
  // Include all animation frames and mip levels. Bilinear filtering across
  // cutouts can produce partial alpha, so those sprites keep both passes.
  boolean zero=false,full=false,partial=false;
  for(var image:sprite.contents().byMipLevel){if(image==null)return 3;for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){int a=image.getPixelRGBA(x,y)>>>24;if(a==0)zero=true;else if(a==255)full=true;else partial=true;}}
  return (full?1:0)|((partial||(zero&&full))?2:0);
 }
}
