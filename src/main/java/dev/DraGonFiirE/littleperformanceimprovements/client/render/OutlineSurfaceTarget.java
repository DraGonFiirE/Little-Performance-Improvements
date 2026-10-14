package dev.DraGonFiirE.littleperformanceimprovements.client.render;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL45C;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;
import org.joml.Matrix4fc;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Exact surface triangle IDs with a private, high-precision reverse depth buffer. */
public final class OutlineSurfaceTarget {
 private static int fbo,planes,depth,program,width,height,empty,vertices,resolveProgram,resolveVao;
 private static final Vector3f camera=new Vector3f();
 private static boolean ready,fastReverse;
 private static float depthScale;
 private static boolean valid;
 private static int cachedVertexBuffer,cachedWidth,cachedHeight;
 private static final Matrix4f cachedView=new Matrix4f(),cachedProjection=new Matrix4f();
 private OutlineSurfaceTarget(){}
 private static String source(String suffix){try(var stream=OutlineSurfaceTarget.class.getResourceAsStream("/assets/littleperformanceimprovements/pinwheel/shaders/program/"+suffix)){if(stream==null)throw new IllegalStateException("Missing outline shader "+suffix);return "#version 150\n"+new String(stream.readAllBytes(),StandardCharsets.UTF_8);}catch(IOException e){throw new IllegalStateException(e);}}
 private static int compile(int type,String file){int shader=GL33C.glCreateShader(type);GL33C.glShaderSource(shader,source(file));GL33C.glCompileShader(shader);if(GL33C.glGetShaderi(shader,GL33C.GL_COMPILE_STATUS)==0){String error=GL33C.glGetShaderInfoLog(shader);GL33C.glDeleteShader(shader);throw new IllegalStateException(error);}return shader;}
 private static void initialize(){if(program!=0)return;int vs=0,fs=0,p=0;try{fastReverse=GL.getCapabilities().OpenGL45||GL.getCapabilities().GL_ARB_clip_control;vs=compile(GL33C.GL_VERTEX_SHADER,fastReverse?"outline_surface_fast.vsh":"preview_line.vsh");fs=compile(GL33C.GL_FRAGMENT_SHADER,fastReverse?"outline_surface_fast.fsh":"outline_surface.fsh");p=GL33C.glCreateProgram();GL33C.glAttachShader(p,vs);GL33C.glAttachShader(p,fs);GL33C.glBindAttribLocation(p,0,"Position");GL33C.glBindAttribLocation(p,1,"Color");GL33C.glLinkProgram(p);if(GL33C.glGetProgrami(p,GL33C.GL_LINK_STATUS)==0)throw new IllegalStateException(GL33C.glGetProgramInfoLog(p));program=p;p=0;}finally{if(vs!=0)GL33C.glDeleteShader(vs);if(fs!=0)GL33C.glDeleteShader(fs);if(p!=0)GL33C.glDeleteProgram(p);}}
 private static void allocate(int w,int h){
  if(fbo==0){fbo=GL33C.glGenFramebuffers();planes=GL33C.glGenTextures();depth=GL33C.glGenTextures();}
  GL33C.glBindFramebuffer(GL33C.GL_FRAMEBUFFER,fbo);
  if(width!=w||height!=h){
   GL33C.glBindTexture(GL33C.GL_TEXTURE_2D,planes);GL33C.glTexImage2D(GL33C.GL_TEXTURE_2D,0,GL33C.GL_R32UI,w,h,0,GL33C.GL_RED_INTEGER,GL33C.GL_UNSIGNED_INT,0L);GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D,GL33C.GL_TEXTURE_MIN_FILTER,GL33C.GL_NEAREST);GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D,GL33C.GL_TEXTURE_MAG_FILTER,GL33C.GL_NEAREST);
   GL33C.glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER,GL33C.GL_COLOR_ATTACHMENT0,GL33C.GL_TEXTURE_2D,planes,0);
   GL33C.glBindTexture(GL33C.GL_TEXTURE_2D,depth);GL33C.glTexImage2D(GL33C.GL_TEXTURE_2D,0,GL33C.GL_DEPTH_COMPONENT32F,w,h,0,GL33C.GL_DEPTH_COMPONENT,GL33C.GL_FLOAT,0L);GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D,GL33C.GL_TEXTURE_MIN_FILTER,GL33C.GL_NEAREST);GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D,GL33C.GL_TEXTURE_MAG_FILTER,GL33C.GL_NEAREST);GL33C.glFramebufferTexture2D(GL33C.GL_FRAMEBUFFER,GL33C.GL_DEPTH_ATTACHMENT,GL33C.GL_TEXTURE_2D,depth,0);
   if(GL33C.glCheckFramebufferStatus(GL33C.GL_FRAMEBUFFER)!=GL33C.GL_FRAMEBUFFER_COMPLETE)throw new IllegalStateException("Incomplete outline surface target");width=w;height=h;
  }
 }
 private static void enabled(int cap,boolean value){if(value)GL33C.glEnable(cap);else GL33C.glDisable(cap);}
 public static void capture(Runnable draw,Matrix4fc view,Matrix4fc projection,int vertexBuffer){
  try(var stack=MemoryStack.stackPush()){
   var viewport=stack.mallocInt(4);GL33C.glGetIntegerv(GL33C.GL_VIEWPORT,viewport);
   if(valid&&cachedVertexBuffer==vertexBuffer&&cachedWidth==viewport.get(2)&&cachedHeight==viewport.get(3)&&cachedView.equals(view)&&cachedProjection.equals(projection)){ready=true;return;}
  }
  ready=false;valid=false;
  int oldRead=GL33C.glGetInteger(GL33C.GL_READ_FRAMEBUFFER_BINDING),oldDraw=GL33C.glGetInteger(GL33C.GL_DRAW_FRAMEBUFFER_BINDING),oldProgram=GL33C.glGetInteger(GL33C.GL_CURRENT_PROGRAM),oldFunc=GL33C.glGetInteger(GL33C.GL_DEPTH_FUNC),active=GL33C.glGetInteger(GL33C.GL_ACTIVE_TEXTURE);
  boolean depthTest=GL33C.glIsEnabled(GL33C.GL_DEPTH_TEST),depthWrite=GL33C.glGetBoolean(GL33C.GL_DEPTH_WRITEMASK),blend=GL33C.glIsEnabled(GL33C.GL_BLEND),cull=GL33C.glIsEnabled(GL33C.GL_CULL_FACE),scissor=GL33C.glIsEnabled(GL33C.GL_SCISSOR_TEST),offset=GL33C.glIsEnabled(GL33C.GL_POLYGON_OFFSET_FILL);
  double clearDepth=GL33C.glGetDouble(GL33C.GL_DEPTH_CLEAR_VALUE);GL33C.glActiveTexture(GL33C.GL_TEXTURE0);int texture=GL33C.glGetInteger(GL33C.GL_TEXTURE_BINDING_2D),oldVertices=GL33C.glGetInteger(GL33C.GL_TEXTURE_BINDING_BUFFER);
  try(var stack=MemoryStack.stackPush()){
   var viewport=stack.mallocInt(4);var mask=stack.malloc(4);var color=stack.mallocFloat(4);GL33C.glGetIntegerv(GL33C.GL_VIEWPORT,viewport);GL33C.glGetBooleanv(GL33C.GL_COLOR_WRITEMASK,mask);GL33C.glGetFloatv(GL33C.GL_COLOR_CLEAR_VALUE,color);
   try{
    initialize();allocate(viewport.get(2),viewport.get(3));depthScale=fastReverse?projection.m32()/(projection.m22()-1f):0f;
    if(vertices==0)vertices=GL33C.glGenTextures();GL33C.glBindTexture(GL33C.GL_TEXTURE_BUFFER,vertices);GL33C.glTexBuffer(GL33C.GL_TEXTURE_BUFFER,GL33C.GL_RGBA32F,vertexBuffer);new Matrix4f(view).invert().transformPosition(camera.set(0,0,0));GL33C.glViewport(0,0,width,height);GL33C.glDisable(GL33C.GL_BLEND);GL33C.glDisable(GL33C.GL_CULL_FACE);GL33C.glDisable(GL33C.GL_SCISSOR_TEST);GL33C.glDisable(GL33C.GL_POLYGON_OFFSET_FILL);GL33C.glEnable(GL33C.GL_DEPTH_TEST);GL33C.glDepthFunc(GL33C.GL_GREATER);GL33C.glDepthMask(true);GL33C.glColorMask(true,true,true,true);GL33C.glClearDepth(0);GL33C.glClearColor(0,0,0,0);GL33C.glClearBufferuiv(GL33C.GL_COLOR,0,new int[4]);GL33C.glClear(GL33C.GL_DEPTH_BUFFER_BIT);
    GL33C.glUseProgram(program);var matrix=stack.mallocFloat(16);GL33C.glUniformMatrix4fv(GL33C.glGetUniformLocation(program,"ModelViewMat"),false,view.get(matrix));GL33C.glUniformMatrix4fv(GL33C.glGetUniformLocation(program,"ProjMat"),false,projection.get(matrix));GL33C.glUniform1f(GL33C.glGetUniformLocation(program,"ReverseDepthScale"),depthScale);
    int origin=0,depthMode=0;if(fastReverse){origin=GL33C.glGetInteger(GL45C.GL_CLIP_ORIGIN);depthMode=GL33C.glGetInteger(GL45C.GL_CLIP_DEPTH_MODE);GL45C.glClipControl(origin,GL45C.GL_ZERO_TO_ONE);}
    try{if(!OutlineTriangleIndex.draw(vertexBuffer,program))draw.run();ready=true;valid=true;cachedVertexBuffer=vertexBuffer;cachedWidth=width;cachedHeight=height;cachedView.set(view);cachedProjection.set(projection);}finally{if(fastReverse)GL45C.glClipControl(origin,depthMode);}
   }finally{GL33C.glViewport(viewport.get(0),viewport.get(1),viewport.get(2),viewport.get(3));GL33C.glColorMask(mask.get(0)!=0,mask.get(1)!=0,mask.get(2)!=0,mask.get(3)!=0);GL33C.glClearColor(color.get(0),color.get(1),color.get(2),color.get(3));}
  }finally{
   GL33C.glBindFramebuffer(GL33C.GL_READ_FRAMEBUFFER,oldRead);GL33C.glBindFramebuffer(GL33C.GL_DRAW_FRAMEBUFFER,oldDraw);GL33C.glUseProgram(oldProgram);GL33C.glDepthFunc(oldFunc);GL33C.glDepthMask(depthWrite);GL33C.glClearDepth(clearDepth);enabled(GL33C.GL_DEPTH_TEST,depthTest);enabled(GL33C.GL_BLEND,blend);enabled(GL33C.GL_CULL_FACE,cull);enabled(GL33C.GL_SCISSOR_TEST,scissor);enabled(GL33C.GL_POLYGON_OFFSET_FILL,offset);GL33C.glBindTexture(GL33C.GL_TEXTURE_2D,texture);GL33C.glBindTexture(GL33C.GL_TEXTURE_BUFFER,oldVertices);GL33C.glActiveTexture(active);
  }
 }
 /** Transfer the captured surface depth to Minecraft's target without drawing its geometry again. */
 public static boolean resolve(Matrix4fc view,Matrix4fc projection){
  if(!ready)return false;
  int oldProgram=GL33C.glGetInteger(GL33C.GL_CURRENT_PROGRAM),oldVao=GL33C.glGetInteger(GL33C.GL_VERTEX_ARRAY_BINDING),active=GL33C.glGetInteger(GL33C.GL_ACTIVE_TEXTURE);
  GL33C.glActiveTexture(GL33C.GL_TEXTURE2);int oldDepth=GL33C.glGetInteger(GL33C.GL_TEXTURE_BINDING_2D),oldSampler=GL33C.glGetIntegeri(GL33C.GL_SAMPLER_BINDING,2);
  try(var binding=bind();var stack=MemoryStack.stackPush()){
   if(resolveProgram==0){
    int v=compile(GL33C.GL_VERTEX_SHADER,"outline_depth_resolve.vsh"),f=0,p=0;
    try{f=compile(GL33C.GL_FRAGMENT_SHADER,"outline_depth_resolve.fsh");p=GL33C.glCreateProgram();GL33C.glAttachShader(p,v);GL33C.glAttachShader(p,f);GL33C.glLinkProgram(p);if(GL33C.glGetProgrami(p,GL33C.GL_LINK_STATUS)==0)throw new IllegalStateException(GL33C.glGetProgramInfoLog(p));resolveProgram=p;p=0;resolveVao=GL33C.glGenVertexArrays();}
    finally{GL33C.glDeleteShader(v);if(f!=0)GL33C.glDeleteShader(f);if(p!=0)GL33C.glDeleteProgram(p);}
   }
   GL33C.glUseProgram(resolveProgram);var matrix=stack.mallocFloat(16);GL33C.glUniformMatrix4fv(GL33C.glGetUniformLocation(resolveProgram,"ModelViewMat"),false,view.get(matrix));GL33C.glUniformMatrix4fv(GL33C.glGetUniformLocation(resolveProgram,"ProjMat"),false,projection.get(matrix));
   GL33C.glUniform1f(GL33C.glGetUniformLocation(resolveProgram,"ReverseDepthScale"),depthScale);
   var config=(dev.DraGonFiirE.littleperformanceimprovements.access.PreviewRenderingConfig)(Object)team.creative.littletiles.LittleTiles.CONFIG.rendering;
   GL33C.glUniform1f(GL33C.glGetUniformLocation(resolveProgram,"OutlineWidth"),(float)Math.min(16.0,Math.max(1.0,config.littleperformanceimprovements$getPreviewOutlineThickness())));GL33C.glUniform1i(GL33C.glGetUniformLocation(resolveProgram,"OutlineSurface"),0);GL33C.glUniform1i(GL33C.glGetUniformLocation(resolveProgram,"OutlineVertices"),1);GL33C.glUniform1i(GL33C.glGetUniformLocation(resolveProgram,"OutlineReverseDepth"),2);
   GL33C.glActiveTexture(GL33C.GL_TEXTURE2);GL33C.glBindSampler(2,0);GL33C.glBindTexture(GL33C.GL_TEXTURE_2D,depth);GL33C.glBindVertexArray(resolveVao);GL33C.glDrawArrays(GL33C.GL_TRIANGLES,0,3);
  }finally{GL33C.glUseProgram(oldProgram);GL33C.glBindVertexArray(oldVao);GL33C.glActiveTexture(GL33C.GL_TEXTURE2);GL33C.glBindTexture(GL33C.GL_TEXTURE_2D,oldDepth);GL33C.glBindSampler(2,oldSampler);GL33C.glActiveTexture(active);}
  ready=true;return true;
 }
 public static final class Binding implements AutoCloseable{
  private final int active,texture,sampler,vertexTexture,vertexSampler;
  public final float cameraX,cameraY,cameraZ;
  private Binding(int a,int t,int s,int v,int vs){active=a;texture=t;sampler=s;vertexTexture=v;vertexSampler=vs;cameraX=camera.x;cameraY=camera.y;cameraZ=camera.z;}
  public void close(){GL33C.glActiveTexture(GL33C.GL_TEXTURE1);GL33C.glBindTexture(GL33C.GL_TEXTURE_BUFFER,vertexTexture);GL33C.glBindSampler(1,vertexSampler);GL33C.glActiveTexture(GL33C.GL_TEXTURE0);GL33C.glBindTexture(GL33C.GL_TEXTURE_2D,texture);GL33C.glBindSampler(0,sampler);GL33C.glActiveTexture(active);ready=false;}
 }
 public static Binding bind(){
  int a=GL33C.glGetInteger(GL33C.GL_ACTIVE_TEXTURE);GL33C.glActiveTexture(GL33C.GL_TEXTURE0);int t=GL33C.glGetInteger(GL33C.GL_TEXTURE_BINDING_2D),s=GL33C.glGetIntegeri(GL33C.GL_SAMPLER_BINDING,0);GL33C.glBindSampler(0,0);
  if(ready)GL33C.glBindTexture(GL33C.GL_TEXTURE_2D,planes);else{if(empty==0){empty=GL33C.glGenTextures();GL33C.glBindTexture(GL33C.GL_TEXTURE_2D,empty);GL33C.glTexImage2D(GL33C.GL_TEXTURE_2D,0,GL33C.GL_R32UI,1,1,0,GL33C.GL_RED_INTEGER,GL33C.GL_UNSIGNED_INT,new int[1]);GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D,GL33C.GL_TEXTURE_MIN_FILTER,GL33C.GL_NEAREST);GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D,GL33C.GL_TEXTURE_MAG_FILTER,GL33C.GL_NEAREST);}else GL33C.glBindTexture(GL33C.GL_TEXTURE_2D,empty);}
  GL33C.glActiveTexture(GL33C.GL_TEXTURE1);int v=GL33C.glGetInteger(GL33C.GL_TEXTURE_BINDING_BUFFER),vs=GL33C.glGetIntegeri(GL33C.GL_SAMPLER_BINDING,1);GL33C.glBindSampler(1,0);GL33C.glBindTexture(GL33C.GL_TEXTURE_BUFFER,vertices);GL33C.glActiveTexture(GL33C.GL_TEXTURE0);
  return new Binding(a,t,s,v,vs);
 }
 public static void invalidate(){ready=false;valid=false;OutlineTriangleIndex.invalidate();}
 public static void release(){OutlineTriangleIndex.release();if(fbo!=0)GL33C.glDeleteFramebuffers(fbo);for(int t:new int[]{planes,depth,empty,vertices})if(t!=0)GL33C.glDeleteTextures(t);if(program!=0)GL33C.glDeleteProgram(program);if(resolveProgram!=0)GL33C.glDeleteProgram(resolveProgram);if(resolveVao!=0)GL33C.glDeleteVertexArrays(resolveVao);fbo=planes=depth=empty=vertices=program=resolveProgram=resolveVao=width=height=0;ready=false;valid=false;}
}


