package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.DraGonFiirE.littleperformanceimprovements.access.PreviewRenderingConfig;
import foundry.veil.api.client.render.VeilRenderSystem;
import foundry.veil.api.client.render.shader.program.ShaderProgram;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL33C;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.common.util.type.Color;
import team.creative.littletiles.LittleTiles;
import java.util.List;

/** Colors Default blueprint faces inside structure handles without changing their geometry or alpha. */
public final class BlueprintMarkerTint {
    private static final ResourceLocation SHADER = ResourceLocation.fromNamespaceAndPath("littleperformanceimprovements", "blueprint_marker_tint");
    private static int buffer, texture;
    private static List<RenderBox> uploaded;

    public static boolean draw(MeshData mesh, VertexBuffer vertices) {
        List<RenderBox> markers = BlueprintStructureMarkers.markers(mesh);
        if (markers == null || markers.isEmpty()) return false;
        ShaderProgram shader = VeilRenderSystem.setShader(SHADER);
        if (shader == null || !shader.isValid()) return false;
        Color color = ((PreviewRenderingConfig)(Object)LittleTiles.CONFIG.rendering).littleperformanceimprovements$getPreviewColor();
        if (color == null) color = Color.WHITE;
        int active = GL33C.glGetInteger(GL33C.GL_ACTIVE_TEXTURE);
        int program = GL33C.glGetInteger(GL33C.GL_CURRENT_PROGRAM);
        int previousBuffer = GL33C.glGetInteger(GL33C.GL_TEXTURE_BUFFER);
        RenderSystem.activeTexture(GL33C.GL_TEXTURE12);
        int previousTexture = GL33C.glGetInteger(GL33C.GL_TEXTURE_BINDING_BUFFER);
        try {
            if (buffer == 0) buffer = GL33C.glGenBuffers();
            if (texture == 0) texture = GL33C.glGenTextures();
            GL33C.glBindBuffer(GL33C.GL_TEXTURE_BUFFER, buffer);
            GL33C.glBindTexture(GL33C.GL_TEXTURE_BUFFER, texture);
            if (uploaded != markers) {
                float[] data = new float[markers.size() * 12];
                int index = 0;
                for (RenderBox box : markers) {
                    data[index++] = box.minX; data[index++] = box.minY; data[index++] = box.minZ; data[index++] = 0;
                    data[index++] = box.maxX; data[index++] = box.maxY; data[index++] = box.maxZ; data[index++] = 0;
                    data[index++] = ((box.color >>> 16) & 255) / 255f;
                    data[index++] = ((box.color >>> 8) & 255) / 255f;
                    data[index++] = (box.color & 255) / 255f;
                    data[index++] = 1;
                }
                GL33C.glBufferData(GL33C.GL_TEXTURE_BUFFER, data, GL33C.GL_STATIC_DRAW);
                GL33C.glTexBuffer(GL33C.GL_TEXTURE_BUFFER, GL33C.GL_RGBA32F, buffer);
                uploaded = markers;
            }
            shader.setDefaultUniforms(VertexFormat.Mode.QUADS);
            shader.getUniformSafe("ColorModulator").setVector(color.getRedDecimal(), color.getGreenDecimal(), color.getBlueDecimal(), RenderSystem.getShaderColor()[3]);
            shader.getUniformSafe("MarkerCount").setInt(markers.size());
            shader.bind();
            GL33C.glUniform1i(GL33C.glGetUniformLocation(shader.getProgram(), "MarkerData"), 12);
            RenderSystem.activeTexture(GL33C.GL_TEXTURE12);
            GL33C.glBindTexture(GL33C.GL_TEXTURE_BUFFER, texture);
            vertices.bind();
            vertices.draw();
            return true;
        } finally {
            ShaderProgram.unbind();
            GL33C.glUseProgram(program);
            RenderSystem.activeTexture(GL33C.GL_TEXTURE12);
            GL33C.glBindTexture(GL33C.GL_TEXTURE_BUFFER, previousTexture);
            GL33C.glBindBuffer(GL33C.GL_TEXTURE_BUFFER, previousBuffer);
            RenderSystem.activeTexture(active);
        }
    }

    private BlueprintMarkerTint() {}
}
