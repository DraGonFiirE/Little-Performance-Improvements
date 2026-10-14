package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.*;
import team.creative.littletiles.common.math.box.*;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
/** Exact exterior of axis-aligned boxes, clipping faces against spatially nearby boxes. */
public final class PrimitiveBendingExterior {

 public static List<int[]> faces(LittleBoxes source){
  var boxes=CurveBoxSamples.collect(source);if(boxes==null)return null;
  for(var b:boxes)for(int axis=0;axis<3;axis++)if(max(b,axis)-min(b,axis)!=max(boxes.get(0),axis)-min(boxes.get(0),axis))return null;
  int[] direction=new int[3],run=new int[boxes.size()];int runCount=1;
  for(int i=1;i<boxes.size();i++){
   boolean turn=false;for(int axis=0;axis<3;axis++){int d=Integer.compare(min(boxes.get(i),axis),min(boxes.get(i-1),axis));if(d!=0&&direction[axis]!=0&&direction[axis]!=d)turn=true;}
   if(turn){if(++runCount>2)return null;Arrays.fill(direction,0);}
   else for(int axis=0;axis<3;axis++){int d=Integer.compare(min(boxes.get(i),axis),min(boxes.get(i-1),axis));if(d!=0)direction[axis]=d;}
   run[i]=runCount-1;
  }
  // Index cross-run contacts along the axis with the largest spread.
  int broadAxis=0;long spread=-1;
  for(int axis=0;axis<3;axis++){int lo=Integer.MAX_VALUE,hi=Integer.MIN_VALUE;for(var box:boxes){lo=Math.min(lo,min(box,axis));hi=Math.max(hi,max(box,axis));}if((long)hi-lo>spread){spread=(long)hi-lo;broadAxis=axis;}}
  final int broad=broadAxis;Integer[] order=new Integer[boxes.size()];for(int i=0;i<order.length;i++)order[i]=i;
  Arrays.sort(order,Comparator.comparingInt(i->min(boxes.get(i),broad)));int[] keys=new int[order.length];for(int i=0;i<order.length;i++)keys[i]=min(boxes.get(order[i]),broad);
  int width=boxes.isEmpty()?0:max(boxes.get(0),broad)-min(boxes.get(0),broad);
  int[][] before=new int[3][boxes.size()],after=new int[3][boxes.size()];
  for(int axis=0;axis<3;axis++)for(int start=0;start<boxes.size();){int end=start+1;while(end<boxes.size()&&run[end]==run[start]&&min(boxes.get(end),axis)==min(boxes.get(start),axis))end++;for(int i=start;i<end;i++){before[axis][i]=start>0&&run[start-1]==run[start]?start-1:-1;after[axis][i]=end<boxes.size()&&run[end]==run[start]?end:boxes.size();}start=end;}
  ArrayList<int[]> result=new ArrayList<>();int[] seen=new int[boxes.size()];it.unimi.dsi.fastutil.ints.IntArrayList neighbors=new it.unimi.dsi.fastutil.ints.IntArrayList(),cross=new it.unimi.dsi.fastutil.ints.IntArrayList();FaceClipBuffer pieces=new FaceClipBuffer();
  for(int index=0;index<boxes.size();index++){
   var box=boxes.get(index);neighbors.clear();cross.clear();
   int left=0,right=keys.length;long lower=(long)min(box,broad)-width;while(left<right){int mid=(left+right)>>>1;if(keys[mid]<lower)left=mid+1;else right=mid;}
   for(int k=left;k<keys.length&&keys[k]<=max(box,broad);k++){int other=order[k];if(run[other]==run[index])continue;var c=boxes.get(other);if(c.maxX>=box.minX&&c.minX<=box.maxX&&c.maxY>=box.minY&&c.minY<=box.maxY&&c.maxZ>=box.minZ&&c.minZ<=box.maxZ)cross.add(other);}

   if(index>0&&run[index-1]==run[index])neighbors.add(index-1);if(index+1<boxes.size()&&run[index+1]==run[index])neighbors.add(index+1);
   for(int axis=0;axis<3;axis++)for(int sign=0;sign<2;sign++){
    neighbors.clear();if(index>0&&run[index-1]==run[index])neighbors.add(index-1);if(index+1<boxes.size()&&run[index+1]==run[index])neighbors.add(index+1);
    if(before[axis][index]>=0&&before[axis][index]!=index-1)neighbors.add(before[axis][index]);
    if(after[axis][index]<boxes.size()&&after[axis][index]!=index+1)neighbors.add(after[axis][index]);
    neighbors.addAll(cross);
    int a=(axis+1)%3,b=(axis+2)%3,plane=sign==0?min(box,axis):max(box,axis);
    boolean covered=false;
    for(int ni=0;ni<neighbors.size();ni++){int other=neighbors.getInt(ni);var cover=boxes.get(other);int low=min(cover,axis),high=max(cover,axis);
     boolean hides=sign==1?low<=plane&&(high>plane||(high==plane&&other<index)):high>=plane&&(low<plane||(low==plane&&other<index));
     if(hides&&min(cover,a)<=min(box,a)&&max(cover,a)>=max(box,a)&&min(cover,b)<=min(box,b)&&max(cover,b)>=max(box,b)){covered=true;break;}
    }
    if(covered)continue;
    pieces.begin(min(box,a),max(box,a),min(box,b),max(box,b));
    for(int ni=0;ni<neighbors.size();ni++){int other=neighbors.getInt(ni);var cover=boxes.get(other);int low=min(cover,axis),high=max(cover,axis);
     boolean hides=sign==1?low<=plane&&(high>plane||(high==plane&&other<index)):high>=plane&&(low<plane||(low==plane&&other<index));
     if(!hides||max(cover,a)<=min(box,a)||min(cover,a)>=max(box,a)||max(cover,b)<=min(box,b)||min(cover,b)>=max(box,b))continue;
     pieces.subtract(min(cover,a),max(cover,a),min(cover,b),max(cover,b));if(pieces.size==0)break;
    }
    for(int pi=0;pi<pieces.size;pi+=4)result.add(new int[]{axis,sign,plane,a,b,pieces.values[pi],pieces.values[pi+1],pieces.values[pi+2],pieces.values[pi+3]});
   }
  }
  return result;
 }
 private static int min(LittleBox b,int axis){return axis==0?b.minX:axis==1?b.minY:b.minZ;}
 private static int max(LittleBox b,int axis){return axis==0?b.maxX:axis==1?b.maxY:b.maxZ;}
}
