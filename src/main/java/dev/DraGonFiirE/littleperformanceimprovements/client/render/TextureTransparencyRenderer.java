package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.world.inventory.InventoryMenu;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import java.nio.*;
import static org.lwjgl.opengl.GL40C.*;

/** Opaque depth plus weighted blended transparency; no camera-dependent sorting. */
public final class TextureTransparencyRenderer {
    private static int framebuffer, depth, opaque, accumulation, revealage, vao, program, composite, width, height;
    private static long lastResolve;
    private static VertexBuffer lastBuffer;
    private static TextureDrawLayout lastLayout;
    private static float lastAlpha, lastNear;
    private static final Matrix4f lastView = new Matrix4f(), lastProjection = new Matrix4f();
    static final String VERTEX = """
        #version 330 core
        layout(location=0) in vec3 Position;
        layout(location=1) in vec2 UV0;
        layout(location=2) in vec4 Color;
        uniform mat4 ModelViewMat, ProjMat;
        out vec2 uv;
        out vec4 tint;
        out float viewDepth;
        void main() {
            vec4 view = ModelViewMat * vec4(Position, 1.0);
            gl_Position = ProjMat * view;
            uv = UV0; tint = Color; viewDepth = -view.z;
        }
        """;
    static final String FRAGMENT = """
        #version 330 core
        in vec2 uv; in vec4 tint; in float viewDepth;
        uniform sampler2D Atlas;
        uniform float NearClip;
        uniform int TransparentPass;
        layout(location=0) out vec4 color;
        layout(location=1) out vec4 reveal;
        void main() {
            if (viewDepth < NearClip) discard;
            vec4 material = texture(Atlas, uv) * tint;
            float a = clamp(material.a, 0.0, 1.0);
            if (a <= 0.0) discard;
            if (TransparentPass == 0) {
                if (a < 0.999) discard;
                color = vec4(material.rgb, 1.0); reveal = vec4(0.0);
            } else {
                if (a >= 0.999) discard;
                // Bounded weight avoids half-float overflow on layered models.
                float weight = clamp(8.0 / (1.0 + viewDepth * 0.05), 0.1, 8.0);
                color = vec4(material.rgb * a, a) * weight;
                reveal = vec4(a);
            }
        }
        """;
    static final String FULLSCREEN = """
        #version 330 core
        out vec2 uv;
        void main() {
            vec2 p = vec2(gl_VertexID == 1 ? 3.0 : -1.0, gl_VertexID == 2 ? 3.0 : -1.0);
            uv = p * 0.5 + 0.5; gl_Position = vec4(p, 0.0, 1.0);
        }
        """;
    static final String COMPOSITE = """
        #version 330 core
        in vec2 uv;
        uniform sampler2D Opaque, Accumulation, Revealage;
        uniform float PreviewAlpha;
        out vec4 color;
        void main() {
            vec4 base = texture(Opaque, uv);
            vec4 accum = texture(Accumulation, uv);
            float remaining = clamp(texture(Revealage, uv).r, 0.0, 1.0);
            float a = 1.0 - remaining;
            vec3 rgb = accum.rgb / max(accum.a, 0.00001);
            float combinedAlpha = a + base.a * remaining;
            if (combinedAlpha <= 0.0) discard;
            color = vec4((rgb * a + base.rgb * base.a * remaining) / combinedAlpha,
                         combinedAlpha * PreviewAlpha);
        }
        """;

    public static boolean draw(VertexBuffer buffer, VertexBuffer interior, float alpha, float near, TextureDrawLayout layout) {
        Minecraft mc = Minecraft.getInstance();
        int w = mc.getWindow().getWidth(), h = mc.getWindow().getHeight();
        if (w <= 0 || h <= 0) return false;
        int read = glGetInteger(GL_READ_FRAMEBUFFER_BINDING), draw = glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING);
        int oldProgram = glGetInteger(GL_CURRENT_PROGRAM), oldVao = glGetInteger(GL_VERTEX_ARRAY_BINDING);
        int active = glGetInteger(GL_ACTIVE_TEXTURE), depthFunc = glGetInteger(GL_DEPTH_FUNC);
        int cullMode = glGetInteger(GL_CULL_FACE_MODE);
        boolean depthTest = glIsEnabled(GL_DEPTH_TEST), depthWrite = glGetBoolean(GL_DEPTH_WRITEMASK);
        boolean cull = glIsEnabled(GL_CULL_FACE), scissor = glIsEnabled(GL_SCISSOR_TEST), offset = glIsEnabled(GL_POLYGON_OFFSET_FILL);
        int[] textures = new int[3], viewport = new int[4]; glGetIntegerv(GL_VIEWPORT, viewport);
        boolean[] blending = new boolean[2]; int[][] blends = new int[2][6];
        int[] blendKeys = {GL_BLEND_SRC_RGB, GL_BLEND_DST_RGB, GL_BLEND_SRC_ALPHA, GL_BLEND_DST_ALPHA, GL_BLEND_EQUATION_RGB, GL_BLEND_EQUATION_ALPHA};
        for (int i=0;i<2;i++) { blending[i]=glIsEnabledi(GL_BLEND,i); for(int j=0;j<6;j++) blends[i][j]=glGetIntegeri(blendKeys[j],i); }
        for (int i=0;i<3;i++) { glActiveTexture(GL_TEXTURE0+i); textures[i]=glGetInteger(GL_TEXTURE_BINDING_2D); }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            ByteBuffer masks = stack.malloc(8);
            glGetBooleani_v(GL_COLOR_WRITEMASK,0,masks.slice(0,4));
            glGetBooleani_v(GL_COLOR_WRITEMASK,1,masks.slice(4,4));
            try {
            glColorMaski(0,true,true,true,true); glColorMaski(1,true,true,true,true);
            ensure(w,h);
            Matrix4f view = RenderSystem.getModelViewMatrix(), projection = RenderSystem.getProjectionMatrix();
            long now = System.nanoTime();
            boolean same = lastView.equals(view) && lastProjection.equals(projection);
            boolean reuse = buffer == lastBuffer && layout == lastLayout && alpha == lastAlpha && near == lastNear
                && now-lastResolve < (same ? 100_000_000L : 8_000_000L);
            glDisable(GL_SCISSOR_TEST); glDisable(GL_POLYGON_OFFSET_FILL);
            if (!reuse) {
                // Copy world depth, then opaque preview faces add their own depth.
                glBindFramebuffer(GL_READ_FRAMEBUFFER, read); glBindFramebuffer(GL_DRAW_FRAMEBUFFER, framebuffer);
                glBlitFramebuffer(0,0,w,h,0,0,w,h,GL_DEPTH_BUFFER_BIT,GL_NEAREST);
                glBindFramebuffer(GL_FRAMEBUFFER, framebuffer); glViewport(0,0,w,h);
                glDrawBuffer(GL_COLOR_ATTACHMENT0); glDisablei(GL_BLEND,0);
                glClearBufferfv(GL_COLOR,0,stack.floats(0,0,0,0));
                glEnable(GL_DEPTH_TEST); glDepthFunc(GL_LESS); glDepthMask(true);
                // Marked previews can surround the camera. The filtered surface must
                // remain visible from either side in both material passes.
                glDisable(GL_CULL_FACE);
                glUseProgram(program);
                glUniformMatrix4fv(glGetUniformLocation(program,"ModelViewMat"),false,view.get(stack.mallocFloat(16)));
                glUniformMatrix4fv(glGetUniformLocation(program,"ProjMat"),false,projection.get(stack.mallocFloat(16)));
                glUniform1f(glGetUniformLocation(program,"NearClip"),near);
                glUniform1i(glGetUniformLocation(program,"Atlas"),0);
                glActiveTexture(GL_TEXTURE0); glBindTexture(GL_TEXTURE_2D,mc.getTextureManager().getTexture(InventoryMenu.BLOCK_ATLAS).getId());
                glUniform1i(glGetUniformLocation(program,"TransparentPass"),0);
                TexturePassSelection.pass(1);
                try { submit(buffer,layout); } finally { TexturePassSelection.pass(0); }
                glDrawBuffers(stack.ints(GL_COLOR_ATTACHMENT1,GL_COLOR_ATTACHMENT2));
                glClearBufferfv(GL_COLOR,0,stack.floats(0,0,0,0));
                glClearBufferfv(GL_COLOR,1,stack.floats(1,1,1,1));
                glDepthMask(false);
                glEnablei(GL_BLEND,0); glEnablei(GL_BLEND,1);
                glBlendEquationi(0,GL_FUNC_ADD); glBlendEquationi(1,GL_FUNC_ADD);
                glBlendFunci(0,GL_ONE,GL_ONE); glBlendFunci(1,GL_ZERO,GL_ONE_MINUS_SRC_ALPHA);
                glUniform1i(glGetUniformLocation(program,"TransparentPass"),1);
                TexturePassSelection.pass(2);
                try { submit(buffer,layout); } finally { TexturePassSelection.pass(0); }
                lastBuffer=buffer; lastLayout=layout; lastAlpha=alpha; lastNear=near;
                lastView.set(view); lastProjection.set(projection); lastResolve=now;
            }
            glBindFramebuffer(GL_DRAW_FRAMEBUFFER,draw); glViewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            glDisable(GL_DEPTH_TEST); glDepthMask(false); glDisable(GL_CULL_FACE);
            glEnablei(GL_BLEND,0); glBlendEquationi(0,GL_FUNC_ADD);
            glBlendFuncSeparatei(0,GL_SRC_ALPHA,GL_ONE_MINUS_SRC_ALPHA,GL_ONE,GL_ONE_MINUS_SRC_ALPHA);
            glUseProgram(composite);
            bind(0,opaque,"Opaque"); bind(1,accumulation,"Accumulation"); bind(2,revealage,"Revealage");
            glUniform1f(glGetUniformLocation(composite,"PreviewAlpha"),alpha);
            glBindVertexArray(vao); glDrawArrays(GL_TRIANGLES,0,3);
            return true;
            } finally {
                for(int i=0;i<2;i++)glColorMaski(i,masks.get(i*4)!=0,masks.get(i*4+1)!=0,masks.get(i*4+2)!=0,masks.get(i*4+3)!=0);
            }
        } finally {
            glBindFramebuffer(GL_READ_FRAMEBUFFER,read); glBindFramebuffer(GL_DRAW_FRAMEBUFFER,draw);
            glViewport(viewport[0],viewport[1],viewport[2],viewport[3]);
            glUseProgram(oldProgram); glBindVertexArray(oldVao);
            for(int i=0;i<3;i++){glActiveTexture(GL_TEXTURE0+i);glBindTexture(GL_TEXTURE_2D,textures[i]);} glActiveTexture(active);
            glDepthFunc(depthFunc); glDepthMask(depthWrite); glCullFace(cullMode);
            enable(GL_DEPTH_TEST,depthTest); enable(GL_CULL_FACE,cull); enable(GL_SCISSOR_TEST,scissor); enable(GL_POLYGON_OFFSET_FILL,offset);
            for(int i=0;i<2;i++){int[] b=blends[i];glBlendFuncSeparatei(i,b[0],b[1],b[2],b[3]);glBlendEquationSeparatei(i,b[4],b[5]);if(blending[i])glEnablei(GL_BLEND,i);else glDisablei(GL_BLEND,i);}
        }
    }
    private static void submit(VertexBuffer buffer, TextureDrawLayout layout) {
        buffer.bind();
        // Transparent ranges must not act as occluders for other ranges.
        if(layout==null || !layout.drawVisible(buffer,false)) buffer.draw();
    }
    private static void bind(int unit,int texture,String name){glActiveTexture(GL_TEXTURE0+unit);glBindTexture(GL_TEXTURE_2D,texture);glUniform1i(glGetUniformLocation(composite,name),unit);}
    private static void enable(int state,boolean value){if(value)glEnable(state);else glDisable(state);}
    private static void ensure(int w,int h) {
        if(program==0){program=link(VERTEX,FRAGMENT);composite=link(FULLSCREEN,COMPOSITE);vao=glGenVertexArrays();}
        if(framebuffer!=0 && width==w && height==h)return;
        if(framebuffer!=0){glDeleteFramebuffers(framebuffer);glDeleteRenderbuffers(depth);glDeleteTextures(opaque);glDeleteTextures(accumulation);glDeleteTextures(revealage);}
        width=w;height=h;lastBuffer=null;lastResolve=0;
        framebuffer=glGenFramebuffers();glBindFramebuffer(GL_FRAMEBUFFER,framebuffer);
        opaque=attachment(GL_COLOR_ATTACHMENT0,GL_RGBA8,w,h);
        accumulation=attachment(GL_COLOR_ATTACHMENT1,GL_RGBA16F,w,h);
        revealage=attachment(GL_COLOR_ATTACHMENT2,GL_R16F,w,h);
        int previous=glGetInteger(GL_RENDERBUFFER_BINDING);
        depth=glGenRenderbuffers();glBindRenderbuffer(GL_RENDERBUFFER,depth);glRenderbufferStorage(GL_RENDERBUFFER,GL_DEPTH_COMPONENT24,w,h);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER,GL_DEPTH_ATTACHMENT,GL_RENDERBUFFER,depth);glBindRenderbuffer(GL_RENDERBUFFER,previous);
        if(glCheckFramebufferStatus(GL_FRAMEBUFFER)!=GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Texture transparency framebuffer incomplete");
    }
    private static int attachment(int slot,int format,int w,int h){
        int t=glGenTextures();glBindTexture(GL_TEXTURE_2D,t);glTexImage2D(GL_TEXTURE_2D,0,format,w,h,0,GL_RGBA,GL_FLOAT,(ByteBuffer)null);
        glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MIN_FILTER,GL_NEAREST);glTexParameteri(GL_TEXTURE_2D,GL_TEXTURE_MAG_FILTER,GL_NEAREST);
        glFramebufferTexture2D(GL_FRAMEBUFFER,slot,GL_TEXTURE_2D,t,0);return t;
    }
    static int link(String vertex,String fragment){
        int v=shader(GL_VERTEX_SHADER,vertex),f=shader(GL_FRAGMENT_SHADER,fragment),p=glCreateProgram();
        glAttachShader(p,v);glAttachShader(p,f);glLinkProgram(p);glDeleteShader(v);glDeleteShader(f);
        if(glGetProgrami(p,GL_LINK_STATUS)==0)throw new IllegalStateException(glGetProgramInfoLog(p));return p;
    }
    private static int shader(int type,String source){int s=glCreateShader(type);glShaderSource(s,source);glCompileShader(s);if(glGetShaderi(s,GL_COMPILE_STATUS)==0)throw new IllegalStateException(glGetShaderInfoLog(s));return s;}
}
