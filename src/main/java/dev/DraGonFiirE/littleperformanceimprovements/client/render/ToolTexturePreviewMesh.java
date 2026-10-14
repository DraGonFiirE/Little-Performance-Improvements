package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat.Mode;
import dev.DraGonFiirE.littleperformanceimprovements.LittlePerformanceImprovements;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import team.creative.creativecore.client.render.box.QuadGeneratorContext;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.mc.ColorUtils;
import team.creative.littletiles.client.render.mc.MeshDataExtender;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.client.render.overlay.PreviewRenderer.BoxRenderResult;
import team.creative.littletiles.common.block.little.element.LittleElement;
import team.creative.littletiles.common.block.little.tile.LittleTile;
import team.creative.littletiles.common.math.box.collection.LittleBoxes;

public final class ToolTexturePreviewMesh {
   private ToolTexturePreviewMesh() {
   }

   public static BoxRenderResult build(PreviewRenderer var0, LittleBoxes var1, ItemStack var2) {
      ArrayList var3 = new ArrayList();
      LittleTile var4 = new LittleTile(LittleElement.getOrDefault(var2), var1.all());
      var4.addRenderingBoxes(var1.grid, var3);
      SphereDirectFaces.retain(var3, var1.grid, var1);
      ByteBufferBuilder var5 = var0.createBuffer();

      BoxRenderResult var10000;
      try {
         BufferBuilder var6 = new BufferBuilder(var5, Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
         QuadGeneratorContext var7 = new QuadGeneratorContext();
         RandomSource var8 = RandomSource.create();
         Minecraft var9 = Minecraft.getInstance();
         BlockPos var10 = BlockPos.ZERO;
         ModelData var11 = var0.level().getModelData(var10);
         HashMap<net.minecraft.world.level.block.state.BlockState, BakedModel> var12 = new HashMap<>();
         HashMap<net.minecraft.world.level.block.state.BlockState, ModelData> var13 = new HashMap<>();
         HashMap var14 = new HashMap();
         HashMap var15 = new HashMap();

         for (RenderBox var17 : (Iterable<RenderBox>)(Iterable<?>)(var3)) {
            if (var17.state != null) {
               BakedModel var18 = var12.computeIfAbsent(var17.state, state -> var9.getBlockRenderer().getBlockModel(state));
               ModelData var19 = var13.computeIfAbsent(var17.state, state -> var18.getModelData(var0.level(), var10, state, var11));
               List var20 = (List)var14.get(var17.state);
               if (var20 == null) {
                  var8.setSeed(var17.state.getSeed(var10));
                  ArrayList var21 = new ArrayList();

                  for (RenderType var23 : var18.getRenderTypes(var17.state, var8, var19)) {
                     var21.add(var23);
                  }

                  var20 = List.copyOf(var21);
                  var14.put(var17.state, var20);
               }

               TextureFaceCells var42 = new TextureFaceCells(var17);
               if (!var42.empty()) {
                  int var43 = var42.minX;
                  int var44 = var42.minY;
                  int var24 = var42.minZ;
                  int var25 = var42.maxX;
                  int var26 = var42.maxY;
                  int var27 = var42.maxZ;

                  for (int var28 = var43; var28 <= var25; var28++) {
                     for (int var29 = var44; var29 <= var26; var29++) {
                        for (int var30 = var42.nextZ(var28, var29, var24); var30 <= var27; var30 = var42.nextZ(var28, var29, var30 + 1)) {
                           BlockPos var31 = new BlockPos(var28, var29, var30);
                           var8.setSeed(var17.state.getSeed(var10));

                           for (RenderType var33 : (Iterable<RenderType>)(Iterable<?>)(var20)) {
                              for (Facing var37 : Facing.VALUES) {
                                 if (var17.shouldRenderFace(var37)) {
                                    List var38 = ChiselTransformTextures.quads(
                                       var17, var7, var0.level(), var10, var31, var17.state, var18, var19, var37, var33, var8, true, -1
                                    );
                                    appendQuads(var0, var6, var17, var38, var9, var15, var10);
                                 }
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         MeshData var40 = var6.build();
         if (var40 == null) {
            var5.close();
            return SphereModeSharing.compact(new BoxRenderResult(var1, var1.pos, null, null), false, true);
         }

         if (var40 instanceof MeshDataExtender var41) {
            var41.keepAlive(true);
         }

         var10000 = new BoxRenderResult(var1, var1.pos, var5, var40);
      } catch (Throwable var39) {
         var5.close();
         LittlePerformanceImprovements.LOGGER.error("Failed to build a material-textured tool preview; using Surface mesh", var39);
         return SphereModeSharing.compact(var0.buildBoxes(PreviewRenderer.EMPTY, var1, false), false, true);
      }

      return SphereModeSharing.compact(var10000, false, true);
   }

   private static void appendQuads(
      PreviewRenderer renderer,
      BufferBuilder builder,
      RenderBox box,
      List<BakedQuad> quads,
      Minecraft minecraft,
      Map<BlockState, Map<Integer, Integer>> tintCache,
      BlockPos origin
   ) {
      for (BakedQuad quad : quads) {
         int[] vertices = quad.getVertices();
         int stride = vertices.length / 4;
         if (stride >= 6) {
            float shade = ChiselStableTextureShade.shade(quad, vertices, stride);
            int tintRed = 255;
            int tintGreen = 255;
            int tintBlue = 255;
            int tintAlpha = 255;
            if (box.color != -1) {
               tintRed = ColorUtils.red(box.color);
               tintGreen = ColorUtils.green(box.color);
               tintBlue = ColorUtils.blue(box.color);
               tintAlpha = ColorUtils.alpha(box.color);
            } else if (quad.isTinted()) {
               int tintIndex = quad.getTintIndex();
               int modelTint = tintCache.computeIfAbsent(box.state, ignored -> new HashMap<>())
                  .computeIfAbsent(tintIndex, ignored -> minecraft.getBlockColors().getColor(box.state, renderer.level(), origin, tintIndex));
               if (modelTint != -1) {
                  tintRed = modelTint >>> 16 & 0xFF;
                  tintGreen = modelTint >>> 8 & 0xFF;
                  tintBlue = modelTint & 0xFF;
               }
            }

            for (int vertex = 0; vertex < 4; vertex++) {
               int offset = vertex * stride;
               int bakedColor = vertices[offset + 3];
               int red = Math.round((bakedColor & 0xFF) * tintRed / 255.0F * shade);
               int green = Math.round((bakedColor >>> 8 & 0xFF) * tintGreen / 255.0F * shade);
               int blue = Math.round((bakedColor >>> 16 & 0xFF) * tintBlue / 255.0F * shade);
               int alpha = (bakedColor >>> 24 & 0xFF) * tintAlpha / 255;
               builder.addVertex(Float.intBitsToFloat(vertices[offset]), Float.intBitsToFloat(vertices[offset + 1]), Float.intBitsToFloat(vertices[offset + 2]))
                  .setUv(Float.intBitsToFloat(vertices[offset + 4]), Float.intBitsToFloat(vertices[offset + 5]))
                  .setColor(red, green, blue, alpha);
            }
         }
      }
   }

}
