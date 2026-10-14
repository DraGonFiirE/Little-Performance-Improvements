package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.function.Consumer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL33C;

/** Screen-resolution, color-and-depth cache of an unchanged full-detail GUI mesh.
 * All raw GL state changed here is restored before returning to Minecraft.
 * No geometry simplification is involved. Held items explicitly opt in to perspective.
 */
public final class BlueprintInventoryRaster implements AutoCloseable {
    private final float[] bounds = {Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY,
            Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY};
    private final boolean animated;
    private final boolean allowPerspective;
    private final Matrix4f lastModel = new Matrix4f(), lastProjection = new Matrix4f();
    private final Matrix4f lastTransform = new Matrix4f();
    private int lastX, lastY;
    private float[] lastColor;
    private int[] lastViewport;
    private int framebuffer, color, depth, program, vao, width, height;
    private long refreshed;
    private boolean valid;

    public BlueprintInventoryRaster(ByteBuffer vertices, int stride, int count, boolean animated) {
        this(vertices, stride, count, animated, false);
    }

    public BlueprintInventoryRaster(ByteBuffer vertices, int stride, int count, boolean animated, boolean allowPerspective) {
        this.animated = animated;
        this.allowPerspective = allowPerspective;
        for (int v = 0; v < count; v++)
            for (int axis = 0; axis < 3; axis++) {
                float p = vertices.getFloat(v * stride + axis * Float.BYTES);
                bounds[axis] = Math.min(bounds[axis], p);
                bounds[axis + 3] = Math.max(bounds[axis + 3], p);
            }
    }

    /** Called after the source render type is set up. Callback draws with the supplied projection. */
    public boolean draw(Matrix4f model, Matrix4f projection, float[] tint,
            Consumer<Matrix4f> render) {
        boolean orthographic = projection.m03() == 0 && projection.m13() == 0
                && projection.m23() == 0 && projection.m33() == 1;
        if ((!orthographic && !allowPerspective) || GL33C.glIsEnabled(GL33C.GL_COLOR_LOGIC_OP))
            return false;
        int[] viewport = new int[4];
        GL33C.glGetIntegerv(GL33C.GL_VIEWPORT, viewport);
        if (viewport[2] <= 0 || viewport[3] <= 0)
            return false;
        Matrix4f transform = new Matrix4f(projection).mul(model);
        float minX = Float.POSITIVE_INFINITY, minY = minX;
        float maxX = Float.NEGATIVE_INFINITY, maxY = maxX;
        Vector4f point = new Vector4f();
        for (int corner = 0; corner < 8; corner++) {
            point.set(bounds[(corner & 1) == 0 ? 0 : 3],
                    bounds[(corner & 2) == 0 ? 1 : 4],
                    bounds[(corner & 4) == 0 ? 2 : 5], 1).mul(transform);
            if (point.w <= 0 || !Float.isFinite(point.w))
                return false;
            float x = viewport[0] + (point.x / point.w + 1) * viewport[2] * 0.5f;
            float y = viewport[1] + (point.y / point.w + 1) * viewport[3] * 0.5f;
            if (!Float.isFinite(x) || !Float.isFinite(y))
                return false;
            // A clipped source image cannot be reused after a depth translation.
            if (point.z / point.w <= -1 || point.z / point.w >= 1)
                return false;
            minX = Math.min(minX, x); maxX = Math.max(maxX, x);
            minY = Math.min(minY, y); maxY = Math.max(maxY, y);
        }
        // Perspective images are keyed by the complete pose: offscreen pixels
        // cannot be reused by a later pose, so do not allocate or draw them.
        // All corners have positive w, making these projected bounds conservative.
        if (!orthographic) {
            if (maxX < viewport[0] || minX > viewport[0] + viewport[2]
                    || maxY < viewport[1] || minY > viewport[1] + viewport[3])
                return true;
            minX = Math.max(minX, viewport[0]);
            minY = Math.max(minY, viewport[1]);
            maxX = Math.min(maxX, viewport[0] + viewport[2]);
            maxY = Math.min(maxY, viewport[1] + viewport[3]);
        }
        // Capture the whole GUI icon even at the window edge, so moving it back
        // onscreen never needs another mesh draw just to reveal clipped pixels.
        int x = (int) Math.floor(minX) - 2;
        int y = (int) Math.floor(minY) - 2;
        int w = (int) Math.ceil(maxX) + 2 - x;
        int h = (int) Math.ceil(maxY) + 2 - y;
        float depthOffset = 0;
        boolean reuseTranslation = false;
        if (orthographic && valid && projection.equals(lastProjection) && Arrays.equals(viewport, lastViewport)
                && new Matrix4f(transform).m30(0).m31(0).m32(0)
                      .equals(new Matrix4f(lastTransform).m30(0).m31(0).m32(0))) {
            float dx = (transform.m30() - lastTransform.m30()) * viewport[2] * 0.5f;
            float dy = (transform.m31() - lastTransform.m31()) * viewport[3] * 0.5f;
            // Integer-pixel translations preserve rasterization exactly.
            // Fractional motion, scale and rotation still redraw the source.
            if (Math.abs(dx - Math.round(dx)) < 0.001f && Math.abs(dy - Math.round(dy)) < 0.001f) {
                x = lastX + Math.round(dx); y = lastY + Math.round(dy);
                w = width; h = height;
                depthOffset = (transform.m32() - lastTransform.m32()) * 0.5f;
                reuseTranslation = true;
            }
        }
        // Bound memory and leave enlarged/non-inventory views on the ordinary path.
        int limit = allowPerspective ? 1024 : 512;
        if (w <= 0 || h <= 0 || w > limit || h > limit)
            return false;

        State state = new State();
        try {
            if (program == 0)
                createProgram();
            if (framebuffer == 0 || w != width || h != height)
                createTarget(w, h);
            // Resource creation uses raw texture/program bindings. Restore them
            // before Minecraft's state-cached shader apply runs in the callback.
            state.restore();
            long now = System.nanoTime();
            if (!valid || (!reuseTranslation && !model.equals(lastModel)) || !projection.equals(lastProjection)
                    || !Arrays.equals(tint, lastColor) || !Arrays.equals(viewport, lastViewport)
                    || (animated && now - refreshed >= 50_000_000L)) {
                GL33C.glBindFramebuffer(GL33C.GL_DRAW_FRAMEBUFFER, framebuffer);
                GL33C.glViewport(0, 0, width, height);
                GL33C.glDisable(GL33C.GL_SCISSOR_TEST);
                GL33C.glColorMask(true, true, true, true);
                GL33C.glDepthMask(true);
                GL33C.glClearBufferfv(GL33C.GL_COLOR, 0, new float[] {0, 0, 0, 0});
                GL33C.glClearBufferfv(GL33C.GL_DEPTH, 0, new float[] {1});
                Matrix4f cropped = new Matrix4f().scaling((float) viewport[2] / w,
                        (float) viewport[3] / h, 1);
                cropped.m30((2f * (viewport[0] - x) + viewport[2]) / w - 1);
                cropped.m31((2f * (viewport[1] - y) + viewport[3]) / h - 1);
                cropped.mul(projection);
                try {
                    render.accept(cropped);
                } finally {
                    // ShaderInstance clears its samplers/program through
                    // Minecraft's cached GL wrapper. Preserve those post-draw
                    // bindings rather than silently desynchronizing that cache.
                    state.rememberRendererBindings();
                }
                lastModel.set(model); lastProjection.set(projection);
                lastTransform.set(transform); lastX = x; lastY = y;
                depthOffset = 0;
                lastColor = tint.clone(); lastViewport = viewport.clone();
                refreshed = now; valid = true;
            }
            // Restore destination/scissor before compositing. Replay the captured
            // per-pixel depth, so overlapping GUI contents keep their depth order.
            GL33C.glBindFramebuffer(GL33C.GL_DRAW_FRAMEBUFFER, state.drawFramebuffer);
            GL33C.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            State.enable(GL33C.GL_SCISSOR_TEST, state.scissor);
            GL33C.glColorMask(state.mask[0] != 0, state.mask[1] != 0,
                    state.mask[2] != 0, state.mask[3] != 0);
            GL33C.glDepthMask(state.depthMask);
            GL33C.glDisable(GL33C.GL_CULL_FACE);
            GL33C.glEnable(GL33C.GL_BLEND);
            // Offscreen blending produced premultiplied color.
            GL33C.glBlendEquationSeparate(GL33C.GL_FUNC_ADD, GL33C.GL_FUNC_ADD);
            GL33C.glBlendFuncSeparate(GL33C.GL_ONE, GL33C.GL_ONE_MINUS_SRC_ALPHA,
                    GL33C.GL_ONE, GL33C.GL_ONE_MINUS_SRC_ALPHA);
            GL33C.glUseProgram(program);
            GL33C.glUniform1f(GL33C.glGetUniformLocation(program, "depthOffset"), depthOffset);
            GL33C.glUniform4f(GL33C.glGetUniformLocation(program, "rect"),
                    2f * (x - viewport[0]) / viewport[2] - 1,
                    2f * (y - viewport[1]) / viewport[3] - 1,
                    2f * w / viewport[2], 2f * h / viewport[3]);
            GL33C.glActiveTexture(GL33C.GL_TEXTURE0);
            GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, color);
            GL33C.glActiveTexture(GL33C.GL_TEXTURE1);
            GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, depth);
            GL33C.glBindVertexArray(vao);
            GL33C.glDrawArrays(GL33C.GL_TRIANGLES, 0, 3);
            return true;
        } finally {
            state.restore();
        }
    }

    private void createTarget(int w, int h) {
        destroyTarget();
        width = w; height = h; valid = false;
        framebuffer = GL33C.glGenFramebuffers();
        GL33C.glBindFramebuffer(GL33C.GL_DRAW_FRAMEBUFFER, framebuffer);
        color = texture(GL33C.GL_RGBA8, GL33C.GL_RGBA, GL33C.GL_UNSIGNED_BYTE);
        depth = texture(GL33C.GL_DEPTH_COMPONENT24, GL33C.GL_DEPTH_COMPONENT, GL33C.GL_UNSIGNED_INT);
        GL33C.glFramebufferTexture2D(GL33C.GL_DRAW_FRAMEBUFFER, GL33C.GL_COLOR_ATTACHMENT0,
                GL33C.GL_TEXTURE_2D, color, 0);
        GL33C.glFramebufferTexture2D(GL33C.GL_DRAW_FRAMEBUFFER, GL33C.GL_DEPTH_ATTACHMENT,
                GL33C.GL_TEXTURE_2D, depth, 0);
        if (GL33C.glCheckFramebufferStatus(GL33C.GL_DRAW_FRAMEBUFFER) != GL33C.GL_FRAMEBUFFER_COMPLETE)
            throw new IllegalStateException("Incomplete blueprint inventory framebuffer");
    }

    private int texture(int internal, int format, int type) {
        int id = GL33C.glGenTextures();
        GL33C.glActiveTexture(GL33C.GL_TEXTURE0);
        GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, id);
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_NEAREST);
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_NEAREST);
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_WRAP_S, GL33C.GL_CLAMP_TO_EDGE);
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_WRAP_T, GL33C.GL_CLAMP_TO_EDGE);
        GL33C.glTexImage2D(GL33C.GL_TEXTURE_2D, 0, internal, width, height, 0, format, type, (ByteBuffer) null);
        return id;
    }

    private void createProgram() {
        int vertex = compile(GL33C.GL_VERTEX_SHADER, """
                #version 150
                uniform vec4 rect;
                out vec2 uv;
                void main() {
                    uv = vec2((gl_VertexID << 1) & 2, gl_VertexID & 2);
                    gl_Position = vec4(rect.xy + uv * rect.zw, 0.0, 1.0);
                }
                """);
        int fragment = 0;
        try {
            fragment = compile(GL33C.GL_FRAGMENT_SHADER, """
                    #version 150
                    uniform sampler2D image;
                    uniform sampler2D depths;
                    uniform float depthOffset;
                    in vec2 uv;
                    out vec4 result;
                    void main() {
                        // The oversized triangle covers the rectangle only.
                        if (any(greaterThan(uv, vec2(1.0)))) discard;
                        result = texture(image, uv);
                        if (result.a == 0.0) discard;
                        gl_FragDepth = texture(depths, uv).r + depthOffset;
                    }
                    """);
            program = GL33C.glCreateProgram();
            GL33C.glAttachShader(program, vertex);
            GL33C.glAttachShader(program, fragment);
            GL33C.glLinkProgram(program);
            if (GL33C.glGetProgrami(program, GL33C.GL_LINK_STATUS) == 0)
                throw new IllegalStateException(GL33C.glGetProgramInfoLog(program));
            GL33C.glUseProgram(program);
            GL33C.glUniform1i(GL33C.glGetUniformLocation(program, "image"), 0);
            GL33C.glUniform1i(GL33C.glGetUniformLocation(program, "depths"), 1);
            vao = GL33C.glGenVertexArrays();
        } finally {
            GL33C.glDeleteShader(vertex);
            if (fragment != 0) GL33C.glDeleteShader(fragment);
        }
    }

    private static int compile(int type, String text) {
        int shader = GL33C.glCreateShader(type);
        GL33C.glShaderSource(shader, text);
        GL33C.glCompileShader(shader);
        if (GL33C.glGetShaderi(shader, GL33C.GL_COMPILE_STATUS) == 0) {
            String error = GL33C.glGetShaderInfoLog(shader);
            GL33C.glDeleteShader(shader);
            throw new IllegalStateException(error);
        }
        return shader;
    }

    private void destroyTarget() {
        if (framebuffer != 0) GL33C.glDeleteFramebuffers(framebuffer);
        if (color != 0) GL33C.glDeleteTextures(color);
        if (depth != 0) GL33C.glDeleteTextures(depth);
        framebuffer = color = depth = 0;
    }

    @Override public void close() {
        destroyTarget();
        if (program != 0) GL33C.glDeleteProgram(program);
        if (vao != 0) GL33C.glDeleteVertexArrays(vao);
        program = vao = 0; valid = false;
    }

    private static final class State {
        final int drawFramebuffer = GL33C.glGetInteger(GL33C.GL_DRAW_FRAMEBUFFER_BINDING);
        final int readFramebuffer = GL33C.glGetInteger(GL33C.GL_READ_FRAMEBUFFER_BINDING);
        int program, vao, arrayBuffer, active;
        final int srcRgb = GL33C.glGetInteger(GL33C.GL_BLEND_SRC_RGB);
        final int dstRgb = GL33C.glGetInteger(GL33C.GL_BLEND_DST_RGB);
        final int srcAlpha = GL33C.glGetInteger(GL33C.GL_BLEND_SRC_ALPHA);
        final int dstAlpha = GL33C.glGetInteger(GL33C.GL_BLEND_DST_ALPHA);
        final int eqRgb = GL33C.glGetInteger(GL33C.GL_BLEND_EQUATION_RGB);
        final int eqAlpha = GL33C.glGetInteger(GL33C.GL_BLEND_EQUATION_ALPHA);
        final boolean blend = GL33C.glIsEnabled(GL33C.GL_BLEND);
        final boolean cull = GL33C.glIsEnabled(GL33C.GL_CULL_FACE);
        final boolean scissor = GL33C.glIsEnabled(GL33C.GL_SCISSOR_TEST);
        final boolean depthMask = GL33C.glGetBoolean(GL33C.GL_DEPTH_WRITEMASK);
        final int[] viewport = new int[4], mask = new int[4], textures = new int[2];

        State() {
            GL33C.glGetIntegerv(GL33C.GL_VIEWPORT, viewport);
            GL33C.glGetIntegerv(GL33C.GL_COLOR_WRITEMASK, mask);
            rememberRendererBindings();
        }

        void rememberRendererBindings() {
            program = GL33C.glGetInteger(GL33C.GL_CURRENT_PROGRAM);
            vao = GL33C.glGetInteger(GL33C.GL_VERTEX_ARRAY_BINDING);
            arrayBuffer = GL33C.glGetInteger(GL33C.GL_ARRAY_BUFFER_BINDING);
            active = GL33C.glGetInteger(GL33C.GL_ACTIVE_TEXTURE);
            for (int i = 0; i < 2; i++) {
                GL33C.glActiveTexture(GL33C.GL_TEXTURE0 + i);
                textures[i] = GL33C.glGetInteger(GL33C.GL_TEXTURE_BINDING_2D);
            }
            GL33C.glActiveTexture(active);
        }

        static void enable(int capability, boolean enabled) {
            if (enabled) GL33C.glEnable(capability); else GL33C.glDisable(capability);
        }

        void restore() {
            GL33C.glBindFramebuffer(GL33C.GL_DRAW_FRAMEBUFFER, drawFramebuffer);
            GL33C.glBindFramebuffer(GL33C.GL_READ_FRAMEBUFFER, readFramebuffer);
            GL33C.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            GL33C.glUseProgram(program);
            GL33C.glBindVertexArray(vao);
            GL33C.glBindBuffer(GL33C.GL_ARRAY_BUFFER, arrayBuffer);
            for (int i = 0; i < 2; i++) {
                GL33C.glActiveTexture(GL33C.GL_TEXTURE0 + i);
                GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, textures[i]);
            }
            GL33C.glActiveTexture(active);
            GL33C.glBlendEquationSeparate(eqRgb, eqAlpha);
            GL33C.glBlendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            enable(GL33C.GL_BLEND, blend); enable(GL33C.GL_CULL_FACE, cull);
            enable(GL33C.GL_SCISSOR_TEST, scissor);
            GL33C.glDepthMask(depthMask);
            GL33C.glColorMask(mask[0] != 0, mask[1] != 0, mask[2] != 0, mask[3] != 0);
        }
    }
}
