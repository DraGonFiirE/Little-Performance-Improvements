package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.collection.*;
/** Union an extruded wall's cross-section once, then restore block-split height. */
public final class DefaultWallExtrusion {
 public static LittleBoxes build(LittleBoxes raw){
  int[] first=null;boolean[] common={true,true,true};
  for(var b:raw.all()){
   if(b.getClass()!=LittleBox.class)return null;
   int[] r={b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ};
   if(first==null)first=r;else for(int a=0;a<3;a++)common[a]&=first[a]==r[a]&&first[a+3]==r[a+3];
  }
  if(first==null)return raw;int axis=-1;for(int a=0;a<3;a++)if(common[a]&&first[a+3]-first[a]>1&&(axis<0||first[a+3]-first[a]>first[axis+3]-first[axis]))axis=a;
  if(axis<0)return null;
  var fast=FlatWallBlockUnion.build(raw,axis,first[axis],first[axis+3]);if(fast!=null)return fast;
  var flat=new LittleBoxesSimple(raw.pos,raw.grid);
  for(var b:raw.all()){int[] r={b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ};r[axis]=0;r[axis+3]=1;flat.add(new LittleBox(r[0],r[1],r[2],r[3],r[4],r[5]));}
  var union=BlockDefaultUnion.build(flat);var result=new LittleBoxesSimple(raw.pos,raw.grid);
  for(var b:union.all()){int[] r={b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ};r[axis]=first[axis];r[axis+3]=first[axis+3];result.add(new LittleBox(r[0],r[1],r[2],r[3],r[4],r[5]));}
  return DefaultCylinderBlocks.split(result);
 }
}
