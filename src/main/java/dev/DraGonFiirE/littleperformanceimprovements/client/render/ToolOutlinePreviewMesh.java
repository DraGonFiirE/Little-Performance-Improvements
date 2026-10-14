package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.DraGonFiirE.littleperformanceimprovements.LittlePerformanceImprovements;
import dev.DraGonFiirE.littleperformanceimprovements.performance.PreviewBuildCancellation;
import java.util.ArrayList;
import java.util.List;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.littletiles.client.render.mc.MeshDataExtender;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.render.overlay.PreviewRenderer.BoxRenderResult;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;

public final class ToolOutlinePreviewMesh {

   private ToolOutlinePreviewMesh() {
   }

   public static BoxRenderResult build(PreviewRenderer renderer, PoseStack pose, LittleBoxes resultBoxes, LittleBoxes displayBoxes) {
      BoxRenderResult var10000 = FastChiselOutline.build(renderer, resultBoxes, displayBoxes);
      if (var10000 != null) {
         return var10000;
      }

      List<RenderBox> raw = new ArrayList<>();
      int checked = 0;

      for (LittleBox box : displayBoxes.all()) {
         if ((checked++ & 0xFF) == 0) {
            PreviewBuildCancellation.check();
         }

         RenderBox renderBox = box.getRenderingBox(displayBoxes.getGrid());
         if (renderBox != null) {
            raw.add(renderBox);
         }
      }

      List<RenderBox> boundary;
      try {
         boundary = SimpleBlueprintOutline.retainSurfaceOutline(raw, displayBoxes.getGrid());
      } catch (Throwable throwable) {
         LittlePerformanceImprovements.LOGGER.error("Failed to derive tool Outline from exact Surface geometry; using native box edges", throwable);
         raw.clear();

         for (LittleBox box : displayBoxes.all()) {
            RenderBox renderBox = box.getRenderingBox(displayBoxes.getGrid());
            if (renderBox != null) {
               raw.add(renderBox);
            }
         }

         boundary = SimpleBlueprintOutline.retainAirTouching(raw, displayBoxes.getGrid().count);
      }

      if (boundary.isEmpty()) {
         return ChiselOutlineDepth.register(new BoxRenderResult(resultBoxes, resultBoxes.pos, null, null), renderer, displayBoxes);
      }

      ByteBufferBuilder backing = renderer.createBuffer();

      try {
         BufferBuilder builder = renderer.createBuilder(backing, true);
         checked = 0;

         for (RenderBox box : boundary) {
            if ((checked++ & 0xFF) == 0) {
               PreviewBuildCancellation.check();
            }

            renderer.buildBox(pose, box, builder, 255, true, false);
         }

         MeshData expandedMesh = builder.build();
         if (expandedMesh == null) {
            backing.close();
            return ChiselOutlineDepth.register(new BoxRenderResult(resultBoxes, resultBoxes.pos, null, null), renderer, displayBoxes);
         }

         BlueprintPreviewMeshCompactor.CompactedMesh exact = BlueprintPreviewMeshCompactor.compactGeneratedLines(expandedMesh);
         if (exact != null) {
            MeshData exactMesh = exact.mesh();

            expandedMesh.close();
            backing.close();
            if (exactMesh instanceof MeshDataExtender persistent) {
               persistent.keepAlive(true);
            }

            return ChiselOutlineDepth.register(new BoxRenderResult(resultBoxes, resultBoxes.pos, exact.buffers(), exactMesh), renderer, displayBoxes);
         }

         LittlePerformanceImprovements.LOGGER
            .warn(
               "Outline endpoint conversion rejected; retaining expanded line mesh (boxes={}, vertices={})",
               boundary.size(),
               expandedMesh.drawState().vertexCount()
            );
         if (expandedMesh instanceof MeshDataExtender persistent) {
            persistent.keepAlive(true);
         }

         var10000 = new BoxRenderResult(resultBoxes, resultBoxes.pos, backing, expandedMesh);
      } catch (Throwable error) {
         backing.close();
         throw error;
      }

      return ChiselOutlineDepth.register(var10000, renderer, displayBoxes);
   }

}
