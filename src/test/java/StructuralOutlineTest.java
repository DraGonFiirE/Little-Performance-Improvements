import dev.DraGonFiirE.littleperformanceimprovements.client.render.SimpleOutlineEdges;
import dev.DraGonFiirE.littleperformanceimprovements.client.render.SimpleOutlineEdges.*;
import java.util.*;
public class StructuralOutlineTest {
 static Point p(double x,double y,double z){return new Point(x,y,z);}
 static void face(SimpleOutlineEdges e,Point... p){e.polygon(p);}
 static List<Segment> finish(SimpleOutlineEdges e){List<Segment> out=new ArrayList<>();e.finish(out::add);return out;}
 static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);}
 static void rect(SimpleOutlineEdges e,double x,double y,double w,double h){face(e,p(x,y,0),p(x+w,y,0),p(x+w,y+h,0),p(x,y+h,0));}
 static boolean seam(List<Segment> edges){return edges.stream().anyMatch(s->Math.abs(s.a().x()-1)<1e-6&&Math.abs(s.b().x()-1)<1e-6&&Math.abs(s.a().y()-s.b().y())>.5);}
 static void slope(SimpleOutlineEdges e,double slope){
  face(e,p(0,0,0),p(1,0,1),p(1,1,1),p(0,1,0));
  face(e,p(1,0,1),p(2,0,1+slope),p(2,1,1+slope),p(1,1,1));
 }
 static void shell(SimpleOutlineEdges e,double x,double end,boolean left,boolean right){
  face(e,p(x,0,0),p(end,0,0),p(end,1,0),p(x,1,0));
  face(e,p(x,0,1),p(end,0,1),p(end,1,1),p(x,1,1));
  face(e,p(x,0,0),p(end,0,0),p(end,0,1),p(x,0,1));
  face(e,p(x,1,0),p(end,1,0),p(end,1,1),p(x,1,1));
  if(left)face(e,p(x,0,0),p(x,1,0),p(x,1,1),p(x,0,1));
  if(right)face(e,p(end,0,0),p(end,1,0),p(end,1,1),p(end,0,1));
 }
 @org.junit.jupiter.api.Test public void regression(){
  var e=new SimpleOutlineEdges();rect(e,0,0,1,1);rect(e,0,0,1,1);rect(e,1,0,1,1);
  check(finish(e).size()==4,"unequal duplicate counts must not leave a flat seam");
  e=new SimpleOutlineEdges();rect(e,0,0,1,2);rect(e,0,0,1,2);rect(e,1,0,1,1);rect(e,1,1,1,1);
  check(finish(e).size()==4,"duplicate faces at a T junction merge into one contour");
  e=new SimpleOutlineEdges();slope(e,1.000002);check(!seam(finish(e)),"float-level normal differences must not leave a flat seam");
  e=new SimpleOutlineEdges();slope(e,1.01);check(seam(finish(e)),"small real change of angle remains a crease");
  e=new SimpleOutlineEdges();shell(e,0,1,true,false);shell(e,1,2,false,true);
  check(finish(e).size()==12,"combined cuboid has twelve continuous structural edges");
  e=new SimpleOutlineEdges();rect(e,0,0,1,1);rect(e,2,0,1,1);
  check(finish(e).size()==8,"collinear merging must not bridge an empty gap");
  System.out.println("PASS: duplicate adjacency, duplicate T junctions, tolerant normals, real shallow creases, merged solid edges, preserved gaps");
 }
}
