package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.HashSet;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.collection.*;
/** Match LT's overlap-free curve boxes before translucent Default rendering. */
public final class DefaultDragUnion {
 public static LittleBoxes build(LittleBoxes raw){
  var extruded=DefaultWallExtrusion.build(raw);return extruded!=null?extruded:BlockDefaultUnion.build(raw);
 }
}
