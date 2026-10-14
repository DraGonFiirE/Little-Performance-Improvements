package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import net.minecraft.client.Minecraft;
/** Additional appearance key for the shared resolved Surface target. */
public final class SurfaceResolveKey {
 private static int red,green,blue;private static boolean shade,thin,textured,valid;private static Object level;
 public static boolean same(float r,float g,float b,boolean s,boolean t,boolean texture){
  Object world=Minecraft.getInstance().level;int rr=Float.floatToIntBits(r),gg=Float.floatToIntBits(g),bb=Float.floatToIntBits(b);
  boolean same=valid&&level==world&&red==rr&&green==gg&&blue==bb&&shade==s&&thin==t&&textured==texture;
  valid=true;level=world;red=rr;green=gg;blue=bb;shade=s;thin=t;textured=texture;return same;
 }
}
