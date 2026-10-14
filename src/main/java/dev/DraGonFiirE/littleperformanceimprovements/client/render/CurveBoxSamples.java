package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import java.util.*;
import team.creative.littletiles.common.math.box.*;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;
public final class CurveBoxSamples {
 public static ArrayList<LittleBox> collect(LittleBoxes source){
  int capacity=16;while(capacity<source.size()*2L&&capacity<(1<<29))capacity<<=1;
  int[] table=new int[capacity];var boxes=new ArrayList<LittleBox>(source.size());
  for(var b:source.all()){
   if(b.getClass()!=LittleBox.class)return null;
   if(b.minX>=b.maxX||b.minY>=b.maxY||b.minZ>=b.maxZ)continue;
   long h=b.minX*0x9E3779B97F4A7C15L^b.minY*0xC2B2AE3D27D4EB4FL^b.minZ*0x165667B19E3779F9L^b.maxX*0x85EBCA77C2B2AE63L^b.maxY*0x27D4EB2F165667C5L^b.maxZ*0x94D049BB133111EBL;
   h^=h>>>33;h*=0xff51afd7ed558ccdL;h^=h>>>33;int slot=(int)h&(capacity-1);
   while(table[slot]!=0){var p=boxes.get(table[slot]-1);if(p.minX==b.minX&&p.minY==b.minY&&p.minZ==b.minZ&&p.maxX==b.maxX&&p.maxY==b.maxY&&p.maxZ==b.maxZ)break;slot=(slot+1)&(capacity-1);}
   if(table[slot]==0){boxes.add(b);table[slot]=boxes.size();}
  }return boxes;
 }
}
