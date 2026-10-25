package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.vertex.*;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import org.lwjgl.opengl.GL11;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.littletiles.client.render.overlay.PreviewRenderer;
import java.util.List;

/** Draw handles into Surface's nearest-face target before its single alpha composite. Render thread only. */
public final class BlueprintSurfaceMarkers {
    private static List<RenderBox> active, uploaded;
    private static VertexBuffer buffer;
    private static boolean drawn;

    public static void begin(MeshData mesh, PreviewRendererMode mode) {
        active = mode == PreviewRendererMode.Surface ? BlueprintStructureMarkers.markers(mesh) : null;
        drawn = false;
    }
    public static boolean wasDrawn() { return drawn; }
    public static void end() { active = null; drawn = false; }

    public static void draw(ShaderProgram shader, float alpha) {
        if (active == null || active.isEmpty()) return;
        // A cached Surface image already contains the depth-tested handle. Do not
        // redraw the old overlay on top merely because geometry was reused.
        if (shader == null) { drawn = true; return; }
        if (uploaded != active) {
            if (buffer != null) buffer.close();
            buffer = null;
            uploaded = null;
            var builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            for (RenderBox box : active) box.renderPreview(PreviewRenderer.EMPTY, builder, 255);
            MeshData mesh = builder.build();
            if (mesh == null) return;
            buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
            buffer.bind();
            buffer.upload(mesh);
            uploaded = active;
        }
        // Same world-depth test and nearest-surface depth buffer as the blueprint.
        // Only coincident surfaces give priority to the handle.
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_POLYGON_OFFSET_FILL);
        GL11.glPolygonOffset(-2, -4);
        shader.getUniformSafe("ColorModulator").setVector(1f, 1f, 1f, alpha);
        shader.getUniformSafe("PerTilePulse").setInt(1);
        shader.getUniformSafe("HighlightThinFeatures").setInt(0);
        shader.bind();
        buffer.bind();
        buffer.draw();
        drawn = true;
    }

    private BlueprintSurfaceMarkers() {}
}
