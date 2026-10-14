package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexBuffer;
import dev.DraGonFiirE.littleperformanceimprovements.LittlePerformanceImprovements;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.client.ClientHooks;
import net.neoforged.neoforge.client.RenderTypeHelper;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import team.creative.creativecore.client.render.model.box.CreativeBakedBoxItemLayer;
import team.creative.creativecore.client.render.model.layer.CreativeBakedItemLayer;
import team.creative.creativecore.client.render.model.layer.CreativeBakedItemLayer.CreativeBakedItemLayerWrapper;
import team.creative.littletiles.LittleTilesRegistry;
import team.creative.littletiles.api.common.tool.ILittleTool;
import team.creative.littletiles.client.render.item.LittleModelItemTiles;

/** GPU caches the miniature content model drawn over blueprint icons. */
public final class BlueprintGuiIconCache {
   private static final int INITIAL_BUFFER_SIZE = 16 * 1024;
   private static final Matrix3f FLAT_ITEM_NORMALS_UNDER_3D_LIGHTING = createFlatItemNormalCompensation();
   private static final Map<ItemStack, CachedIcon> CACHE = new WeakHashMap<>();
   private static final Map<ItemStack, CachedBoxIcon> BOX_LAYER_CACHE = new WeakHashMap<>();
   private static final Map<ItemStack, CachedBoxIcon> HELD_BOX_LAYER_CACHE = new java.util.LinkedHashMap<>(8, 0.75F, true);
   // Strong, bounded entries allow deterministic deletion of GPU resources.
   // WeakHashMap silently discarded values without closing their buffers.
   private static final Map<IconKey, CachedBoxIcon> LAYERED_ICON_CACHE = new java.util.LinkedHashMap<>(16, 0.75F, true);
   private static int layeredLowResolutionThreshold = Integer.MIN_VALUE;

   private BlueprintGuiIconCache() {
   }

   private static Matrix3f createFlatItemNormalCompensation() {
      float eighthTurn = (float) (Math.PI / 8.0D);
      Matrix4f flatLighting = new Matrix4f()
            .rotationY(-eighthTurn)
            .rotateX((float) (Math.PI * 3.0D / 4.0D));
      Matrix4f threeDimensionalLighting = new Matrix4f()
            .scaling(1.0F, -1.0F, 1.0F)
            .rotateYXZ(1.0821041F, 3.2375858F, 0.0F)
            .rotateYXZ(-eighthTurn, (float) (Math.PI * 3.0D / 4.0D), 0.0F);
      return new Matrix3f(threeDimensionalLighting)
            .mul(new Matrix3f(flatLighting).invert());
   }

   public static boolean render(
         PoseStack outerPose,
         ItemStack blueprint,
         Function<ItemStack, ItemStack> contentFactory,
         BiConsumer<ItemDisplayContext, PoseStack> prepareRenderer) {
      CustomData data = blueprint.get(LittleTilesRegistry.DATA);
      CachedIcon cached = CACHE.get(blueprint);
      CompoundTag currentData = data == null ? null : data.getUnsafe();
      if (cached != null && !java.util.Objects.equals(cached.dataSnapshot, currentData)) {
         cached.close();
         CACHE.remove(blueprint);
         cached = null;
      }

      if (cached == null) {
         cached = build(blueprint, data, contentFactory, prepareRenderer);
         if (cached == null)
            return false;
         CACHE.put(blueprint, cached);
      }

      Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix())
            .mul(outerPose.last().pose());
      for (CachedLayer layer : cached.layers)
         layer.draw(modelView);
      return true;
   }

   /** GPU-caches the dynamic box layer used by LittleTiles pre225 and newer. */
   public static boolean renderBoxLayer(
         PoseStack outerPose,
         CreativeBakedBoxItemLayer boxLayer,
         ItemRenderer itemRenderer,
         ItemStack blueprint,
         ItemDisplayContext context,
         boolean leftHand,
         int packedLight,
         int packedOverlay) {
      CustomData data = blueprint.get(LittleTilesRegistry.DATA);
      CompoundTag currentData = data == null ? null : data.getUnsafe();
      boolean gui = context == ItemDisplayContext.GUI;
      int effectiveLight = gui ? LightTexture.FULL_BRIGHT : packedLight;
      Map<ItemStack, CachedBoxIcon> cache = gui
            ? BOX_LAYER_CACHE : HELD_BOX_LAYER_CACHE;
      CachedBoxIcon cached = cache.get(blueprint);
      if (cached != null && !cached.matches(
            currentData, context, leftHand, effectiveLight, packedOverlay)) {
         cached.close();
         cache.remove(blueprint);
         cached = null;
      }

      if (cached == null) {
         cached = buildBoxLayer(
               boxLayer, itemRenderer, blueprint, context, leftHand,
               effectiveLight, packedOverlay, currentData);
         if (cached == null)
            return false;
         cache.put(blueprint, cached);
         if (!gui && cache.size() > 4) {
            var oldest = cache.entrySet().iterator();
            oldest.next().getValue().close();
            oldest.remove();
         }
      }

      Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix())
            .mul(outerPose.last().pose());
      for (CachedLayer layer : cached.layers)
         layer.draw(modelView);
      return true;
   }

   /**
    * Collapses pre225's separately flushed parchment and dynamic tile layers
    * into one atlas-backed VBO and therefore one draw call per GUI item.
    */
   public static boolean renderLayeredIcon(
         PoseStack outerPose,
         List<CreativeBakedItemLayer> sourceLayers,
         ItemRenderer itemRenderer,
         ItemStack blueprint,
         ItemDisplayContext context,
         boolean leftHand,
         int packedOverlay) {
      int threshold = team.creative.littletiles.LittleTiles.CONFIG.rendering.itemLowResolutionBoxCount;
      if (threshold != layeredLowResolutionThreshold) {
         for (CachedBoxIcon icon : LAYERED_ICON_CACHE.values())
            icon.close();
         LAYERED_ICON_CACHE.clear();
         layeredLowResolutionThreshold = threshold;
      }
      CustomData data = blueprint.get(LittleTilesRegistry.DATA);
      CompoundTag currentData = data == null ? null : data.getUnsafe();
      // Inventory clicks create new ItemStack objects but share their immutable
      // component payload. Key by that identity without hashing the huge NBT.
      IconKey key = new IconKey(currentData, blueprint.getItem(), sourceLayers,
            context, leftHand, packedOverlay);
      CachedBoxIcon cached = LAYERED_ICON_CACHE.get(key);
      if (cached != null && !cached.matches(currentData, context, leftHand,
            LightTexture.FULL_BRIGHT, packedOverlay)) {
         cached.close();
         LAYERED_ICON_CACHE.remove(key);
         cached = null;
      }

      if (cached == null) {
         // Server inventory synchronization can decode an identical blueprint
         // into a new component object. On an identity miss only, compare the
         // exact content and transfer ownership of the existing GPU resources.
         // Rekey rather than alias: eviction must never close a shared VBO.
         IconKey equivalent = null;
         for (IconKey candidate : LAYERED_ICON_CACHE.keySet()) {
            if (key.sameContent(candidate)) {
               equivalent = candidate;
               break;
            }
         }
         if (equivalent != null) {
            CachedBoxIcon previous = LAYERED_ICON_CACHE.remove(equivalent);
            cached = new CachedBoxIcon(currentData, context, leftHand,
                  LightTexture.FULL_BRIGHT, packedOverlay, previous.layers);
            LAYERED_ICON_CACHE.put(key, cached);
         }
      }

      if (cached == null) {
         cached = buildLayeredIcon(
               sourceLayers,
               itemRenderer,
               blueprint,
               context,
               leftHand,
               packedOverlay,
               currentData);
         if (cached == null)
            return false;
         LAYERED_ICON_CACHE.put(key, cached);
         if (LAYERED_ICON_CACHE.size() > 32) {
            var oldest = LAYERED_ICON_CACHE.entrySet().iterator();
            oldest.next().getValue().close();
            oldest.remove();
         }
      }

      Matrix4f modelView = new Matrix4f(RenderSystem.getModelViewMatrix())
            .mul(outerPose.last().pose());
      for (CachedLayer layer : cached.layers)
         layer.draw(modelView);
      return true;
   }

   private static CachedBoxIcon buildLayeredIcon(
         List<CreativeBakedItemLayer> sourceLayers,
         ItemRenderer itemRenderer,
         ItemStack blueprint,
         ItemDisplayContext context,
         boolean leftHand,
         int packedOverlay,
         CompoundTag dataIdentity) {
      CreativeBakedItemLayerWrapper background = null;
      CreativeBakedBoxItemLayer boxes = null;
      for (CreativeBakedItemLayer sourceLayer : sourceLayers) {
         CreativeBakedItemLayer resolved = sourceLayer.resolve(blueprint);
         if (resolved instanceof CreativeBakedItemLayerWrapper wrapper)
            background = wrapper;
         else if (resolved instanceof CreativeBakedBoxItemLayer boxLayer
               && boxLayer.model() instanceof LittleModelItemTiles)
            boxes = boxLayer;
         else
            return null;
      }
      if (background == null || boxes == null)
         return null;

      net.minecraft.util.RandomSource random = net.minecraft.util.RandomSource.create();
      random.setSeed(42L);
      List<net.minecraft.client.renderer.block.model.BakedQuad> opaqueQuads =
            boxes.getBakedQuads(blueprint, Sheets.cutoutBlockSheet(), random, false);
      List<net.minecraft.client.renderer.block.model.BakedQuad> translucentQuads = Collections.emptyList();
      if (boxes.model().hasTranslucentLayer(blueprint)) {
         random.setSeed(42L);
         RenderType translucent = RenderTypeHelper.getEntityRenderType(RenderType.translucent(), true);
         translucentQuads = boxes.getBakedQuads(blueprint, translucent, random, true);
      }
      if (ILittleTool.hasData(blueprint) && opaqueQuads.isEmpty() && translucentQuads.isEmpty())
         return null;

      PoseStack backgroundPose = new PoseStack();
      BakedModel backgroundModel = ClientHooks.handleCameraTransforms(
            backgroundPose, background.model, context, leftHand);
      backgroundPose.translate(-0.5F, -0.5F, -0.5F);
      // CreativeBakedItemLayerWrapper normally switches only the flat
      // parchment layer to GUI-flat lighting. The combined one-draw cache must
      // stay under GUI-3D lighting for the tile geometry, so rotate just the
      // parchment normals to produce the identical flat-light dot products.
      Matrix3f compensatedNormal = new Matrix3f(FLAT_ITEM_NORMALS_UNDER_3D_LIGHTING)
            .mul(backgroundPose.last().normal());
      backgroundPose.last().normal().set(compensatedNormal);
      List<BakedModel> backgroundPasses = backgroundModel.getRenderPasses(blueprint, true);
      RenderType combinedType = null;
      for (BakedModel pass : backgroundPasses) {
         List<RenderType> renderTypes = pass.getRenderTypes(blueprint, true);
         if (renderTypes.size() != 1)
            return null;
         RenderType passType = renderTypes.getFirst();
         if (combinedType == null)
            combinedType = passType;
         else if (combinedType != passType)
            return null;
      }
      if (combinedType == null)
         return null;

      List<CachedLayer> completedLayers = new ArrayList<>();
      try (ByteBufferBuilder backing = new ByteBufferBuilder(INITIAL_BUFFER_SIZE)) {
         BufferBuilder builder = new BufferBuilder(backing, combinedType.mode(), combinedType.format());

         for (BakedModel pass : backgroundPasses)
            itemRenderer.renderModelLists(
                  pass,
                  blueprint,
                  LightTexture.FULL_BRIGHT,
                  packedOverlay,
                  backgroundPose,
                  builder);

         PoseStack boxPose = new PoseStack();
         boxPose.translate(0.0F, 0.0F, 0.5F);
         boxes.block().applyTransform(context, boxPose, leftHand);
         boxPose.translate(-0.5F, -0.5F, -0.5F);
         ItemStack neutralTint = new ItemStack(LittleTilesRegistry.ITEM_TILES.value());
         itemRenderer.renderQuadList(
               boxPose,
               builder,
               opaqueQuads,
               neutralTint,
               LightTexture.FULL_BRIGHT,
               packedOverlay);
         boolean cacheOpaqueImage = opaqueQuads.size() >= 4096;
         if (!cacheOpaqueImage)
            itemRenderer.renderQuadList(
                  boxPose, builder, translucentQuads, neutralTint,
                  LightTexture.FULL_BRIGHT, packedOverlay);

         MeshData mesh = builder.build();
         if (mesh == null)
            return null;
         BlueprintInventoryRaster raster = null;
         // Transparent geometry keeps its original draw order and depth tests
         // in a separate VBO, after the cached opaque color/depth image.
         if (cacheOpaqueImage) {
            var sprites = new java.util.IdentityHashMap<net.minecraft.client.renderer.texture.TextureAtlasSprite, Boolean>();
            boolean animated = false;
            for (var quad : opaqueQuads) {
               if (sprites.put(quad.getSprite(), Boolean.TRUE) == null
                     && quad.getSprite().contents().getUniqueFrames().limit(2).count() > 1)
                  animated = true;
            }
            // Background resource packs can also contain animated sprites.
            for (BakedModel pass : backgroundPasses)
               for (var quad : pass.getQuads(null, null, random))
                  if (quad.getSprite().contents().getUniqueFrames().limit(2).count() > 1)
                     animated = true;
            raster = new BlueprintInventoryRaster(mesh.vertexBuffer(),
                  mesh.drawState().format().getVertexSize(), mesh.drawState().vertexCount(), animated);

         }
         VertexBuffer vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
         vertexBuffer.bind();
         vertexBuffer.upload(mesh);
         VertexBuffer.unbind();
         completedLayers.add(new CachedLayer(combinedType, vertexBuffer, raster));
         if (cacheOpaqueImage && !translucentQuads.isEmpty()) {
            BufferBuilder transparentBuilder = new BufferBuilder(backing, combinedType.mode(), combinedType.format());
            itemRenderer.renderQuadList(boxPose, transparentBuilder, translucentQuads,
                  neutralTint, LightTexture.FULL_BRIGHT, packedOverlay);
            MeshData transparentMesh = transparentBuilder.build();
            if (transparentMesh != null) {
               VertexBuffer transparentBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
               transparentBuffer.bind();
               transparentBuffer.upload(transparentMesh);
               VertexBuffer.unbind();
               completedLayers.add(new CachedLayer(combinedType, transparentBuffer));
            }
         }
         return new CachedBoxIcon(
               dataIdentity,
               context,
               leftHand,
               LightTexture.FULL_BRIGHT,
               packedOverlay,
               completedLayers);
      } catch (Throwable throwable) {
         for (CachedLayer layer : completedLayers)
            layer.close();
         LittlePerformanceImprovements.LOGGER.warn("Could not cache a combined pre225 blueprint GUI icon", throwable);
         return null;
      }
   }

   private static CachedBoxIcon buildBoxLayer(
         CreativeBakedBoxItemLayer boxLayer,
         ItemRenderer itemRenderer,
         ItemStack blueprint,
         ItemDisplayContext context,
         boolean leftHand,
         int packedLight,
         int packedOverlay,
         CompoundTag dataIdentity) {
      PoseStack localPose = new PoseStack();
      localPose.translate(0.0F, 0.0F, 0.5F);
      boxLayer.block().applyTransform(context, localPose, leftHand);
      localPose.translate(-0.5F, -0.5F, -0.5F);

      List<CachedLayer> layers = new ArrayList<>();
      try {
         RenderType cutout = Sheets.cutoutBlockSheet();
         CachedLayer opaque = buildBoxRenderLayer(
               boxLayer, itemRenderer, blueprint, localPose, cutout,
               packedLight, packedOverlay, context != ItemDisplayContext.GUI);
         if (opaque != null)
            layers.add(opaque);

         if (boxLayer.model().hasTranslucentLayer(blueprint)) {
            RenderType translucent = RenderTypeHelper.getEntityRenderType(RenderType.translucent(), true);
            CachedLayer transparent = buildBoxRenderLayer(
                  boxLayer, itemRenderer, blueprint, localPose, translucent,
                  packedLight, packedOverlay, false);
            if (transparent != null)
               layers.add(transparent);
         }
      } catch (Throwable throwable) {
         for (CachedLayer layer : layers)
            layer.close();
         LittlePerformanceImprovements.LOGGER.warn("Could not cache a pre225 blueprint GUI box layer", throwable);
         return null;
      }

      // The asynchronous LT item cache can be temporarily empty. Render the
      // normal path for that frame and retry instead of caching an empty icon.
      if (layers.isEmpty())
         return null;
      return new CachedBoxIcon(
            dataIdentity, context, leftHand, packedLight, packedOverlay, layers);
   }

   private static CachedLayer buildBoxRenderLayer(
         CreativeBakedBoxItemLayer boxLayer,
         ItemRenderer itemRenderer,
         ItemStack stack,
         PoseStack pose,
         RenderType renderType,
         int packedLight,
         int packedOverlay,
         boolean cacheHeldImage) {
      try (ByteBufferBuilder backing = new ByteBufferBuilder(INITIAL_BUFFER_SIZE)) {
         BufferBuilder builder = new BufferBuilder(backing, renderType.mode(), renderType.format());
         boxLayer.renderModelLists(
               itemRenderer,
               stack,
               renderType,
               packedLight,
               packedOverlay,
               pose,
               builder);
         MeshData mesh = builder.build();
         if (mesh == null)
            return null;

         // Retain exact perspective and depth; changes to the hand transform
         // refresh the image. Periodic refresh also tracks animated atlas and
         // lightmap content. Translucent geometry keeps its normal draw path.
         BlueprintInventoryRaster raster = cacheHeldImage && mesh.drawState().vertexCount() >= 16384
               ? new BlueprintInventoryRaster(mesh.vertexBuffer(), mesh.drawState().format().getVertexSize(),
                     mesh.drawState().vertexCount(), true, true) : null;
         VertexBuffer vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
         vertexBuffer.bind();
         vertexBuffer.upload(mesh);
         VertexBuffer.unbind();
         return new CachedLayer(renderType, vertexBuffer, raster);
      }
   }

   private static CachedIcon build(
         ItemStack blueprint,
         CustomData data,
         Function<ItemStack, ItemStack> contentFactory,
         BiConsumer<ItemDisplayContext, PoseStack> prepareRenderer) {
      ItemStack content = contentFactory.apply(blueprint);
      if (content.isEmpty())
         return null;

      Minecraft minecraft = Minecraft.getInstance();
      ItemRenderer itemRenderer = minecraft.getItemRenderer();
      BakedModel model = itemRenderer.getModel(content, null, null, 0);
      PoseStack localPose = new PoseStack();
      prepareRenderer.accept(ItemDisplayContext.GUI, localPose);
      model = ClientHooks.handleCameraTransforms(localPose, model, ItemDisplayContext.GUI, false);
      localPose.translate(-0.5F, -0.5F, -0.5F);

      List<CachedLayer> layers = new ArrayList<>();
      try {
         for (BakedModel pass : model.getRenderPasses(content, true)) {
            for (RenderType renderType : pass.getRenderTypes(content, true)) {
               CachedLayer layer = buildLayer(itemRenderer, pass, content, localPose, renderType);
               if (layer != null)
                  layers.add(layer);
            }
         }
      } catch (Throwable throwable) {
         for (CachedLayer layer : layers)
            layer.close();
         LittlePerformanceImprovements.LOGGER.warn("Could not cache a blueprint GUI content icon", throwable);
         return null;
      }

      // LittleTiles' asynchronous item cache returns no quads until it is ready.
      // Do not remember that temporary empty result; the next frame will retry.
      if (layers.isEmpty())
         return null;
      return new CachedIcon(data == null ? null : data.copyTag(), layers);
   }

   private static CachedLayer buildLayer(
         ItemRenderer itemRenderer,
         BakedModel model,
         ItemStack stack,
         PoseStack pose,
         RenderType renderType) {
      try (ByteBufferBuilder backing = new ByteBufferBuilder(INITIAL_BUFFER_SIZE)) {
         BufferBuilder builder = new BufferBuilder(backing, renderType.mode(), renderType.format());
         itemRenderer.renderModelLists(
               model, stack, 15728880, OverlayTexture.NO_OVERLAY, pose, builder);
         MeshData mesh = builder.build();
         if (mesh == null)
            return null;

         VertexBuffer vertexBuffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
         vertexBuffer.bind();
         vertexBuffer.upload(mesh);
         VertexBuffer.unbind();
         return new CachedLayer(renderType, vertexBuffer);
      }
   }

   public static void clear() {
      for (CachedIcon icon : CACHE.values())
         icon.close();
      CACHE.clear();
      for (CachedBoxIcon icon : BOX_LAYER_CACHE.values())
         icon.close();
      BOX_LAYER_CACHE.clear();
      for (CachedBoxIcon icon : HELD_BOX_LAYER_CACHE.values())
         icon.close();
      HELD_BOX_LAYER_CACHE.clear();
      for (CachedBoxIcon icon : LAYERED_ICON_CACHE.values())
         icon.close();
      LAYERED_ICON_CACHE.clear();
   }

   private record IconKey(Object data, Object item, Object model, Object context,
         boolean leftHand, int overlay) {
      private boolean sameContent(IconKey other) {
         return item == other.item && model == other.model && context == other.context
               && leftHand == other.leftHand && overlay == other.overlay
               && java.util.Objects.equals(data, other.data);
      }

      @Override public int hashCode() {
         int hash = System.identityHashCode(data);
         hash = 31 * hash + System.identityHashCode(item);
         hash = 31 * hash + System.identityHashCode(model);
         hash = 31 * hash + System.identityHashCode(context);
         return 31 * (31 * hash + Boolean.hashCode(leftHand)) + overlay;
      }

      @Override public boolean equals(Object other) {
         return other instanceof IconKey key && data == key.data && item == key.item
               && model == key.model && context == key.context
               && leftHand == key.leftHand && overlay == key.overlay;
      }
   }

   private record CachedIcon(CompoundTag dataSnapshot, List<CachedLayer> layers) implements AutoCloseable {
      @Override
      public void close() {
         for (CachedLayer layer : this.layers)
            layer.close();
      }
   }

   private record CachedBoxIcon(
         CompoundTag dataIdentity,
         ItemDisplayContext context,
         boolean leftHand,
         int packedLight,
         int packedOverlay,
         List<CachedLayer> layers) implements AutoCloseable {
      private boolean matches(
            CompoundTag dataIdentity,
            ItemDisplayContext context,
            boolean leftHand,
            int packedLight,
            int packedOverlay) {
         return this.dataIdentity == dataIdentity
               && this.context == context
               && this.leftHand == leftHand
               && this.packedLight == packedLight
               && this.packedOverlay == packedOverlay;
      }

      @Override
      public void close() {
         for (CachedLayer layer : this.layers)
            layer.close();
      }
   }

   private static final class CachedLayer implements AutoCloseable {
      private final RenderType renderType;
      private final VertexBuffer vertexBuffer;
      private BlueprintInventoryRaster raster;

      CachedLayer(RenderType renderType, VertexBuffer vertexBuffer) {
         this(renderType, vertexBuffer, null);
      }

      CachedLayer(RenderType renderType, VertexBuffer vertexBuffer, BlueprintInventoryRaster raster) {
         this.renderType = renderType;
         this.vertexBuffer = vertexBuffer;
         this.raster = raster;
      }

      public void draw(Matrix4f modelView) {
         this.renderType.setupRenderState();
         try {
            if (this.raster != null && RenderSystem.getShader() != null) {
               try {
                  if (this.raster.draw(modelView, RenderSystem.getProjectionMatrix(),
                        RenderSystem.getShaderColor(), projection -> {
                           this.vertexBuffer.bind();
                           this.vertexBuffer.drawWithShader(modelView, projection, RenderSystem.getShader());
                           VertexBuffer.unbind();
                        }))
                     return;
               } catch (RuntimeException error) {
                  this.raster.close();
                  this.raster = null;
                  LittlePerformanceImprovements.LOGGER.warn(
                        "Inventory image cache unavailable; retaining full-detail GPU mesh", error);
               }
            }
            this.vertexBuffer.bind();
            if (RenderSystem.getShader() != null)
               this.vertexBuffer.drawWithShader(
                     modelView, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
         } finally {
            VertexBuffer.unbind();
            this.renderType.clearRenderState();
         }
      }

      @Override
      public void close() {
         if (this.raster != null)
            this.raster.close();
         this.vertexBuffer.close();
      }
   }
}
