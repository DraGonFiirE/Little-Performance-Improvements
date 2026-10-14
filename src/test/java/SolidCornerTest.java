import java.util.*;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.*;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.SimpleOutlineEdges.Point;
public class SolidCornerTest {
 static Point p(double x,double y,double z){return new Point(x,y,z);}
 static final Point MID=p(1,.5,1),DIR=p(0,1,0);
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);}
 static List<Point[]> mesh(double x){
  List<Point[]> out=new ArrayList<>();int[][] faces={{0,1,3,2},{4,5,7,6},{0,1,5,4},{2,3,7,6},{0,2,6,4},{1,3,7,5}};
  Point[] points=new Point[8];for(int i=0;i<8;i++){double a=x+(i&1),b=(i>>1)&1,c=(i>>2)&1;points[i]=p(a,b,c+a);}
  for(int[] f:faces)out.add(new Point[]{points[f[0]],points[f[1]],points[f[2]],points[f[3]]});return out;
 }
 @org.junit.jupiter.api.Test public void regression(){
  var s=new OutlineSolidUnion();s.box(0,0,0,1,1,1);s.build();check(s.corner(MID,DIR),"convex corner retained");
  s=new OutlineSolidUnion();s.box(0,0,0,2,1,1);s.box(0,0,1,1,1,2);s.build();check(s.corner(MID,DIR),"concave corner retained");
  s=new OutlineSolidUnion();s.box(0,0,0,1,1,1);s.box(1,0,0,2,1,1);s.build();check(!s.corner(MID,DIR),"flat joined surface is not a corner");check(!s.corner(p(1,.5,.5),DIR),"buried interface is not a corner");
  s=new OutlineSolidUnion();s.mesh(mesh(0));s.build();check(s.corner(p(1,.5,2),DIR),"sloped mesh corner retained");
  s=new OutlineSolidUnion();s.mesh(mesh(0));s.mesh(mesh(1));s.build();check(!s.corner(p(1,.5,2),DIR),"joined sloped meshes form a flat surface");
  System.out.println("PASS: solid-union convex/concave corners, flat joins, buried interfaces, sloped meshes");
 }
}
