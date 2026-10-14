import dev.DraGonFiirE.littleperformanceimprovements.client.render.SimpleOutlineEdges;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.SimpleOutlineEdges.*;
import java.util.*;
public class SimpleOutlineTest {
 static Point p(double x,double y){return new Point(x,y,0);}
 static void rect(SimpleOutlineEdges e,double x,double y,double w,double h){e.polygon(new Point[]{p(x,y),p(x+w,y),p(x+w,y+h),p(x,y+h)});}
 static List<Segment> finish(SimpleOutlineEdges e){List<Segment> result=new ArrayList<>();e.finish(result::add);return result;}
 static void require(boolean b,String s){if(!b)throw new AssertionError(s);}
 @org.junit.jupiter.api.Test public void regression(){
  SimpleOutlineEdges e=new SimpleOutlineEdges();rect(e,0,0,1,1);require(finish(e).size()==4,"single face boundary");
  e=new SimpleOutlineEdges();rect(e,0,0,1,2);rect(e,1,0,1,1);rect(e,1,1,1,1);require(finish(e).size()==4,"T junction seams cancel");
  e=new SimpleOutlineEdges();rect(e,0,0,3,1);rect(e,0,2,3,1);rect(e,0,1,1,1);rect(e,2,1,1,1);require(finish(e).size()==8,"hole contour retained");
  e=new SimpleOutlineEdges();rect(e,0,0,1,1);e.polygon(new Point[]{p(0,1),p(1,1),p(1,0),p(0,0)});require(finish(e).size()==4,"duplicate reversed face preserves boundary once");
  e=new SimpleOutlineEdges();rect(e,0,0,1,1);e.polygon(new Point[]{new Point(0,0,0),new Point(1,0,0),new Point(1,0,1),new Point(0,0,1)});require(finish(e).size()==7,"noncoplanar crease retained once");
  e=new SimpleOutlineEdges();e.polygon(new Point[]{new Point(0,0,0),new Point(1,0,1),new Point(1,1,2),new Point(0,1,1)});require(finish(e).size()==4,"sloped geometry retained");
  long start=System.nanoTime();e=new SimpleOutlineEdges();
  for(int x=0;x<640;x++)for(int y=0;y<625;y++)rect(e,x,y,1,1);
  require(finish(e).size()==4,"400k coplanar tiles reduce to outer boundary");
  System.out.printf(java.util.Locale.ROOT,"PASS: boundaries, T-junctions, holes, reversed duplicates, creases, sloped faces; 400k face sweep %.2f seconds%n",(System.nanoTime()-start)/1e9);
 }
}
