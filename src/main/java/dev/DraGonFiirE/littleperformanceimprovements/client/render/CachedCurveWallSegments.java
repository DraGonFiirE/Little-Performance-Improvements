package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.*;import team.creative.creativecore.common.util.math.vec.Vec2d;import team.creative.creativecore.common.util.math.interpolation.*;import team.creative.creativecore.common.util.math.vec.VectorUtils;import team.creative.littletiles.client.tool.shaper.ShapeSelection;import team.creative.littletiles.common.placement.shape.config.InterpolationAxisThicknessConfig;import team.creative.littletiles.common.math.box.*;import team.creative.littletiles.common.math.box.collection.*;import team.creative.littletiles.common.math.vec.LittleVec;
/** Reuse unchanged curve segments; cache only coordinates, never mutable boxes or GPU meshes. */
public final class CachedCurveWallSegments {
 private static final ThreadLocal<State> CACHE=ThreadLocal.withInitial(State::new);
 private static final class State {double[][] keys=new double[0][];int[][] values=new int[0][];}
 public static LittleBoxes sample(ShapeSelection selection,InterpolationAxisThicknessConfig config){
  var result=new LittleBoxesSimple(selection.pos,selection.grid);var bound=selection.getOverallBox();var axis=config.axis;var u=axis.one();var v=axis.two();var points=new ArrayList<Vec2d>();double half=selection.grid.halfPixelLength;
  for(var point:selection)points.add(new Vec2d(point.getVanillaGrid(u)+half,point.getVanillaGrid(v)+half));
  int grow=Math.max(0,config.thickness-1);if(points.size()<=1){bound.growCentered(grow);result.add(bound);return result;}
  Interpolation<Vec2d> interpolation;var values=(Vec2d[])points.toArray(Vec2d[]::new);if(config.interpolation==team.creative.littletiles.common.placement.shape.type.LittleShapeCurve.ShapeInterpolation.HERMITE)interpolation=new HermiteInterpolation<Vec2d>(values);else if(config.interpolation==team.creative.littletiles.common.placement.shape.type.LittleShapeCurve.ShapeInterpolation.CUBIC)interpolation=new CubicInterpolation<Vec2d>(values);else interpolation=new LinearInterpolation<Vec2d>(values);
  int n=points.size()-1;var state=CACHE.get();if(state.keys.length!=n){state.keys=new double[n][];state.values=new int[n][];}
  double originU=VectorUtils.get(u,selection.pos),originV=VectorUtils.get(v,selection.pos),step=1.0/n;int stored=0;
  for(int segment=0;segment<n;segment++){
   int first=Math.max(0,segment-2),last=Math.min(points.size()-1,segment+3);double[] key=new double[6+(last-first+1)*2];key[0]=selection.grid.count;key[1]=originU;key[2]=originV;key[3]=config.interpolation.ordinal();key[4]=n;key[5]=segment;
   for(int j=first,k=6;j<=last;j++){key[k++]=points.get(j).x;key[k++]=points.get(j).y;}
   int[] samples=Arrays.equals(key,state.keys[segment])?state.values[segment]:null;
   if(samples==null){var middle=interpolation.valueAt(step*(segment+0.5));double distance=points.get(segment).distance(middle)+middle.distance(points.get(segment+1));int count=(int)Math.ceil(distance/selection.grid.pixelLength*2);if(count>1000000)return null;samples=new int[count*2];double substep=step/(count-1);for(int j=0;j<count;j++){var point=interpolation.valueAt(step*segment+substep*j);point.sub(new Vec2d(originU,originV));samples[j*2]=selection.grid.toGrid(point.x);samples[j*2+1]=selection.grid.toGrid(point.y);}state.keys[segment]=key;state.values[segment]=samples;}
   stored+=samples.length;
   for(int j=0;j<samples.length;j+=2){var location=new LittleVec(0,0,0);location.set(u,samples[j]);location.set(v,samples[j+1]);var box=new LittleBox(location);box.setMin(axis,bound.getMin(axis));box.setMax(axis,bound.getMax(axis));box.growCentered(grow);result.add(box);}
  }
  if(stored>2000000){state.keys=new double[0][];state.values=new int[0][];}
  return result;
 }
}
