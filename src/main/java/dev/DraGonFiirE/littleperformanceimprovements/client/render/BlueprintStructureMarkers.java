package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode;
import dev.DraGonFiirE.littleperformanceimprovements.access.PreviewRenderingConfig;
import team.creative.littletiles.LittleTiles;
import net.minecraft.client.renderer.GameRenderer;
import org.lwjgl.opengl.GL11;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import team.creative.littletiles.common.block.little.tile.group.LittleGroup;
import team.creative.littletiles.common.math.vec.LittleVec;
import team.creative.littletiles.common.placement.box.LittlePlaceBoxRelative;
import java.util.*;

/** Structure handles are overlays, never blueprint surfaces or occluders. */
public final class BlueprintStructureMarkers {
    private static final Map<MeshData, List<RenderBox>> MARKERS = Collections.synchronizedMap(new WeakHashMap<>());

    public static List<RenderBox> geometry(LittleGroup group, LittleVec offset) {
        List<RenderBox> result = new ArrayList<>();
        collect(group, offset, result, false);
        return result;
    }

    public static void register(MeshData mesh, LittleGroup group, LittleVec offset) {
        if (mesh == null) return;
        List<RenderBox> markers = new ArrayList<>();
        collect(group, offset, markers, true);
        if (!markers.isEmpty()) MARKERS.put(mesh, List.copyOf(markers));
    }

    private static void collect(LittleGroup group, LittleVec offset, List<RenderBox> result, boolean markers) {
        if (!markers)
            for (var tile : group) tile.addPlaceBoxes(group.getGrid(), result, offset);
        if (group.hasStructure()) {
            var specials = group.getStructureType().getSpecialBoxes(group);
            if (specials != null) for (var special : specials)
                if ((special instanceof LittlePlaceBoxRelative) == markers)
                    result.add(special.getRenderBox(group.getGrid(), offset));
        }
        for (LittleGroup child : group.children.all()) collect(child, offset, result, markers);
    }

    static List<RenderBox> markers(MeshData mesh) {
        return MARKERS.get(mesh);
    }

    public static void draw(MeshData mesh, PreviewRendererMode mode) {
        if (mode != PreviewRendererMode.Default && mode != PreviewRendererMode.Surface) return;
        if (mode == PreviewRendererMode.Surface && BlueprintSurfaceMarkers.wasDrawn()) return;
        List<RenderBox> markers = MARKERS.get(mesh);
        if (markers == null) return;
        var shader = RenderSystem.getShader();
        float[] color = RenderSystem.getShaderColor().clone();
        boolean offset = GL11.glIsEnabled(GL11.GL_POLYGON_OFFSET_FILL);
        float factor = GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_FACTOR);
        float units = GL11.glGetFloat(GL11.GL_POLYGON_OFFSET_UNITS);
        boolean write = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        try {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            // Surface handles use the configured steady opacity, not LT's animated Default alpha.
            float alpha = mode == PreviewRendererMode.Surface
                ? (float)Math.clamp(((PreviewRenderingConfig)(Object)LittleTiles.CONFIG.rendering)
                    .littleperformanceimprovements$getPreviewSurfaceAlpha(), 0.0, 1.0)
                : color[3];
            RenderSystem.setShaderColor(1, 1, 1, alpha);
            RenderSystem.depthMask(false);
            // Give handles priority over coincident preview/world faces, including grazing angles.
            GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
            GL11.glPolygonOffset(-2, -4);
            var builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (RenderBox box : markers) box.renderPreview(PreviewRenderer.EMPTY, builder, 255);
            MeshData overlay = builder.build();
            if (overlay != null) BufferUploader.drawWithShader(overlay);
        } finally {
            GL11.glPolygonOffset(factor, units);
            if (!offset) GL11.glDisable(GL11.GL_POLYGON_OFFSET_FILL);
            RenderSystem.depthMask(write);
            RenderSystem.setShader(() -> shader);
            RenderSystem.setShaderColor(color[0], color[1], color[2], color[3]);
        }
    }

    private BlueprintStructureMarkers() {}
}
