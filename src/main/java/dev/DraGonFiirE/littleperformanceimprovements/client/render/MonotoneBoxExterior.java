package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.*;
import team.creative.littletiles.common.math.box.*;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
/** Exact exterior of axis-aligned boxes, clipping faces against spatially nearby boxes. */
public final class MonotoneBoxExterior {

 public static List<int[]> faces(LittleBoxes source){
  var boxes=CurveBoxSamples.collect(source);if(boxes==null)return null;
  for(var b:boxes)for(int axis=0;axis<3;axis++)if(max(b,axis)-min(b,axis)!=max(boxes.get(0),axis)-min(boxes.get(0),axis))return null;
  int[] direction=new int[3];
  for(int i=1;i<boxes.size();i++)for(int axis=0;axis<3;axis++){
   int dl=Integer.compare(min(boxes.get(i),axis),min(boxes.get(i-1),axis));
   int dh=Integer.compare(max(boxes.get(i),axis),max(boxes.get(i-1),axis));
   if(dl!=0&&dh!=0&&dl!=dh)return null;
   int d=dl!=0?dl:dh;if(d!=0){if(direction[axis]!=0&&direction[axis]!=d)return null;direction[axis]=d;}
  }
  int[][] before=new int[3][boxes.size()],after=new int[3][boxes.size()];
  for(int axis=0;axis<3;axis++)for(int start=0;start<boxes.size();){int end=start+1;while(end<boxes.size()&&min(boxes.get(end),axis)==min(boxes.get(start),axis))end++;for(int i=start;i<end;i++){before[axis][i]=start-1;after[axis][i]=end;}start=end;}
  ArrayList<int[]> result=new ArrayList<>();int[] seen=new int[boxes.size()];ArrayList<Integer> neighbors=new ArrayList<>();ArrayList<int[]> pieces=new ArrayList<>(),next=new ArrayList<>();
  for(int index=0;index<boxes.size();index++){
   var box=boxes.get(index);neighbors.clear();
   if(index>0)neighbors.add(index-1);if(index+1<boxes.size())neighbors.add(index+1);
   for(int axis=0;axis<3;axis++)for(int sign=0;sign<2;sign++){
    neighbors.clear();if(index>0)neighbors.add(index-1);if(index+1<boxes.size())neighbors.add(index+1);
    if(before[axis][index]>=0&&before[axis][index]!=index-1)neighbors.add(before[axis][index]);
    if(after[axis][index]<boxes.size()&&after[axis][index]!=index+1)neighbors.add(after[axis][index]);
    int a=(axis+1)%3,b=(axis+2)%3,plane=sign==0?min(box,axis):max(box,axis);
    boolean covered=false;
    for(int other:neighbors){var cover=boxes.get(other);int low=min(cover,axis),high=max(cover,axis);
     boolean hides=sign==1?low<=plane&&(high>plane||(high==plane&&other<index)):high>=plane&&(low<plane||(low==plane&&other<index));
     if(hides&&min(cover,a)<=min(box,a)&&max(cover,a)>=max(box,a)&&min(cover,b)<=min(box,b)&&max(cover,b)>=max(box,b)){covered=true;break;}
    }
    if(covered)continue;
    pieces.clear();pieces.add(new int[]{min(box,a),max(box,a),min(box,b),max(box,b)});
    for(int other:neighbors){var cover=boxes.get(other);int low=min(cover,axis),high=max(cover,axis);
     boolean hides=sign==1?low<=plane&&(high>plane||(high==plane&&other<index)):high>=plane&&(low<plane||(low==plane&&other<index));
     if(!hides||max(cover,a)<=min(box,a)||min(cover,a)>=max(box,a)||max(cover,b)<=min(box,b)||min(cover,b)>=max(box,b))continue;
     next.clear();for(int[] rect:pieces){
      int l=Math.max(rect[0],min(cover,a)),r=Math.min(rect[1],max(cover,a)),d=Math.max(rect[2],min(cover,b)),u=Math.min(rect[3],max(cover,b));
      if(l>=r||d>=u){next.add(rect);continue;}
      if(rect[0]<l)next.add(new int[]{rect[0],l,rect[2],rect[3]});if(r<rect[1])next.add(new int[]{r,rect[1],rect[2],rect[3]});
      if(rect[2]<d)next.add(new int[]{l,r,rect[2],d});if(u<rect[3])next.add(new int[]{l,r,u,rect[3]});
     }
     var swap=pieces;pieces=next;next=swap;if(pieces.isEmpty())break;
    }
    for(int[] p:pieces)result.add(new int[]{axis,sign,plane,a,b,p[0],p[1],p[2],p[3]});
   }
  }
  return result;
 }
 private static int min(LittleBox b,int axis){return axis==0?b.minX:axis==1?b.minY:b.minZ;}
 private static int max(LittleBox b,int axis){return axis==0?b.maxX:axis==1?b.maxY:b.maxZ;}
}
