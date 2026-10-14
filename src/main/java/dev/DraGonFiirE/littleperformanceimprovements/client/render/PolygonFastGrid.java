package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import team.creative.littletiles.common.grid.LittleGrid;
public final class PolygonFastGrid {
 public static int convert(LittleGrid grid,double position){
  double scaled=position*grid.count;
  if(!Double.isFinite(scaled)||Math.abs(scaled)>100_000_000)return grid.toGrid(position);
  int floor=(int)scaled;if(scaled<floor)floor--;
  // Native rounds to five decimal places before flooring. Only values just
  // below the next integer can change cell; leave that boundary to native.
  if(scaled-floor>0.99998)return grid.toGrid(position);
  return floor;
 }
 public static String test(){
  java.util.Random random=new java.util.Random(7162);int checked=0;
  for(int count:new int[]{1,4,16,32}){LittleGrid grid=LittleGrid.get(count);
   for(int i=0;i<250000;i++){
    double q=(random.nextDouble()-0.5)*200000;
    if(i%2==0)q=random.nextInt(200000)-100000+(random.nextDouble()-0.5)*0.0001;
    double position=q/count;
    if(convert(grid,position)!=grid.toGrid(position))throw new AssertionError("Grid mismatch "+count+" "+position);checked++;
   }
  }
  return "PASS: "+checked+" grid rounding comparisons, including cell boundaries";
 }
}
