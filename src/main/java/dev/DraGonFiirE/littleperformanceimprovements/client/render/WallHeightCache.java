package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.*;
import team.creative.littletiles.common.math.box.*;
import team.creative.littletiles.common.math.box.collection.*;
/** Cache only immutable footprint data; returned geometry always belongs to the caller. */
public final class WallHeightCache {
 private static final ThreadLocal<State> FACES=ThreadLocal.withInitial(State::new), BOXES=ThreadLocal.withInitial(State::new);
 private static final int LIMIT=250000;
 private static final class State { int[] key; List<int[]> data; }
 private static int min(LittleBox b,int a){return a==0?b.minX:a==1?b.minY:b.minZ;}
 private static int max(LittleBox b,int a){return a==0?b.maxX:a==1?b.maxY:b.maxZ;}
 private static int[] key(LittleBoxes boxes,int axis){
  int size=boxes.all().size();if(size>LIMIT)return null;
  int[] key=new int[3+size*4];key[0]=axis;key[1]=boxes.grid.count;key[2]=size;int i=3,u=(axis+1)%3,v=(axis+2)%3;
  for(var b:boxes.all()){if(b.getClass()!=LittleBox.class)return null;key[i++]=min(b,u);key[i++]=max(b,u);key[i++]=min(b,v);key[i++]=max(b,v);}return key;
 }
 private static boolean matches(State s,LittleBoxes boxes,int axis){
  if(s.key==null||s.key[0]!=axis||s.key[1]!=boxes.grid.count||s.key[2]!=boxes.all().size())return false;
  int i=3,u=(axis+1)%3,v=(axis+2)%3;
  for(var b:boxes.all())if(b.getClass()!=LittleBox.class||s.key[i++]!=min(b,u)||s.key[i++]!=max(b,u)||s.key[i++]!=min(b,v)||s.key[i++]!=max(b,v))return false;
  return true;
 }
 public static List<int[]> faces(LittleBoxes boxes,int axis,int low,int high){
  var s=FACES.get();if(!matches(s,boxes,axis))return null;
  var result=new ArrayList<int[]>(s.data.size());for(var f:s.data){var c=f.clone();if(c[0]==axis)c[2]=c[1]==0?low:high;else if(c[3]==axis){c[5]=low;c[6]=high;}else{c[7]=low;c[8]=high;}result.add(c);}return result;
 }
 public static void rememberFaces(LittleBoxes boxes,int axis,List<int[]> faces){
  var s=FACES.get();s.key=faces.size()>LIMIT?null:key(boxes,axis);s.data=null;if(s.key==null)return;
  s.data=new ArrayList<>(faces.size());for(var f:faces)s.data.add(Arrays.copyOf(f,9));
 }
 public static LittleBoxes boxes(LittleBoxes raw,int axis,int low,int high){
  var s=BOXES.get();if(!matches(s,raw,axis))return null;
  var result=new LittleBoxesSimple(raw.pos,raw.grid);for(var b:s.data){int[] c=b.clone();c[axis]=low;c[axis+3]=high;result.add(new LittleBox(c[0],c[1],c[2],c[3],c[4],c[5]));}return result;
 }
 public static void rememberBoxes(LittleBoxes raw,int axis,LittleBoxes flat){
  var s=BOXES.get();s.key=flat.all().size()>LIMIT?null:key(raw,axis);s.data=null;if(s.key==null)return;
  s.data=new ArrayList<>(flat.all().size());for(var b:flat.all())s.data.add(new int[]{b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ});
 }
}
