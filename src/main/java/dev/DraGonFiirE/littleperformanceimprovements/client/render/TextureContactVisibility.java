package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.client.render.face.RenderBoxFace;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.littletiles.common.grid.LittleGrid;
/** Remove buried translucent faces without deleting opaque surfaces behind them. */
public final class TextureContactVisibility {
 public static List<RenderBox> retainAirTouching(List<RenderBox> boxes,LittleGrid grid){
  List<RenderBox> opaque=new ArrayList<>(boxes.size()),volumes=new ArrayList<>(boxes.size());
  Map<BlockState,Boolean> solidModels=new HashMap<>();
  for(RenderBox box:boxes){
   if(box.state==null)continue;
   boolean solidModel=solidModels.computeIfAbsent(box.state,state->{
    var model=Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
    var layers=model.getRenderTypes(state,RandomSource.create(0),ModelData.EMPTY);
    boolean any=false;for(RenderType layer:layers){any=true;if(layer!=RenderType.solid())return false;}return any;
   });
   boolean isOpaque=solidModel && box.state.canOcclude() && (box.color>>>24)==255;
   if(isOpaque)opaque.add(box);
   // Alpha-tinted solid-texture tiles still have a complete physical surface.
   // Glass may also join its own faces. Leaf/cutout models that do not
   // support self-face suppression keep their independent layers.
   boolean joinsOwnFaces=true;
   if(!solidModel) for(var side:net.minecraft.core.Direction.values())
    if(!box.state.skipRendering(box.state,side)){joinsOwnFaces=false;break;}
   if((solidModel || joinsOwnFaces) && (box.color>>>24)!=0)volumes.add(box);
  }
  if(volumes.size()==opaque.size()){BlueprintSurfaceFaceFilter.retainAirTouching(opaque,grid);return boxes;}
  TextureInteriorFaces interiors=new TextureInteriorFaces(volumes,opaque);
  RenderBoxFace[] original=snapshot(opaque);
  BlueprintSurfaceFaceFilter.retainAirTouching(opaque,grid);
  RenderBoxFace[] opaqueOnly=snapshot(opaque);
  restore(opaque,original);
  try{
   // Includes exact partial contacts and transformed-face clipping. Internal
   // block-boundary caps of translucent tiles disappear along with contacts
   // against opaque frames. No coordinate rounding or depth bias is used.
   BlueprintSurfaceFaceFilter.retainAirTouching(volumes,grid);
  }finally{
   // Transparency must never erase an opaque face visible through the tile.
   restore(opaque,opaqueOnly);
  }
  interiors.apply(grid);
  return boxes;
 }
 private static RenderBoxFace[] snapshot(List<RenderBox> boxes){
  RenderBoxFace[] faces=new RenderBoxFace[boxes.size()*6];int i=0;
  for(RenderBox box:boxes)for(Facing side:Facing.VALUES)faces[i++]=box.getFace(side);return faces;
 }
 private static void restore(List<RenderBox> boxes,RenderBoxFace[] faces){int i=0;for(RenderBox box:boxes)for(Facing side:Facing.VALUES)box.setFace(side,faces[i++]);}
}
