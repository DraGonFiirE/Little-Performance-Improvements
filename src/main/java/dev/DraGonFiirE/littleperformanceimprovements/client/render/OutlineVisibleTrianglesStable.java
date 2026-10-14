package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.system.MemoryStack;

public final class OutlineVisibleTrianglesStable {
   private static int select;
   private static int prefix;
   private static int scatter;
   private static int draw;
   private static int flags;
   private static int groups;
   private static int command;
   private static int capacity;
   private static boolean failed;
   private static final String SELECT = "#version 430\nlayout(local_size_x=256) in;\nlayout(std430,binding=0) readonly buffer InputVertices { vec4 vertices[]; };\nlayout(std430,binding=1) writeonly buffer OutputIndices { uint indices[]; };\nlayout(std430,binding=2) buffer Command { uint count; uint instances; uint firstIndex; uint baseVertex; uint baseInstance; };\nuniform mat4 ModelViewMat;\nuniform mat4 ProjMat;\nuniform vec2 ScreenSize;\nuniform mat4 RoundoffMat;\nuniform uint TriangleCount;\nlayout(std430,binding=3) writeonly buffer Flags { uint flags[]; };\nlayout(std430,binding=4) writeonly buffer Groups { uint groups[]; };\nshared uint alive[256];\nvec2 errorBound(vec4 clip,vec3 position){\n vec4 e=RoundoffMat*vec4(abs(position),1.0);\n if(clip.w<=e.w)return vec2(1e30);\n return (e.xy+abs(clip.xy/clip.w)*e.w)/(clip.w-e.w)*ScreenSize*0.5;\n}\nvoid main(){\n uint lane=gl_LocalInvocationID.x,q=gl_GlobalInvocationID.x;bool keep=q<TriangleCount;\n if(keep){\n  uint b=q*4u;\n  vec4 a=ProjMat*ModelViewMat*vec4(vertices[b].xyz,1);\n  vec4 c=ProjMat*ModelViewMat*vec4(vertices[b+1u].xyz,1);\n  vec4 d=ProjMat*ModelViewMat*vec4(vertices[b+2u].xyz,1);\n  if(min(a.w,min(c.w,d.w))>0.01){\n   vec2 p=(a.xy/a.w*0.5+0.5)*ScreenSize;\n   vec2 r=(c.xy/c.w*0.5+0.5)*ScreenSize;\n   vec2 s=(d.xy/d.w*0.5+0.5)*ScreenSize;\n   vec2 padding=vec2(0.02)+max(errorBound(a,vertices[b].xyz),max(errorBound(c,vertices[b+1u].xyz),errorBound(d,vertices[b+2u].xyz)));\n   vec2 lo=min(p,min(r,s))-padding,hi=max(p,max(r,s))+padding;\n   keep=!any(greaterThan(ceil(lo-0.5),floor(hi-0.5)))&&all(greaterThanEqual(hi,vec2(0)))&&all(lessThanEqual(lo,ScreenSize));\n  }\n }\n alive[lane]=keep?1u:0u;barrier();\n for(uint delta=1u;delta<256u;delta*=2u){uint add=lane>=delta?alive[lane-delta]:0u;barrier();alive[lane]+=add;barrier();}\n if(lane==255u)groups[gl_WorkGroupID.x]=alive[255];\n if(q<TriangleCount)flags[q]=alive[lane]|(keep?0x80000000u:0u);\n}\n\n";
   private static final String PREFIX = "#version 430\nlayout(local_size_x=256) in;\nlayout(std430,binding=2) buffer Command { uint count; uint instances; uint firstIndex; uint baseVertex; uint baseInstance; };\nlayout(std430,binding=4) buffer Groups { uint groups[]; };\nuniform uint GroupCount;\nshared uint sums[256];\nvoid main(){\n uint lane=gl_LocalInvocationID.x,span=(GroupCount+255u)/256u,start=lane*span,end=min(start+span,GroupCount),sum=0u;\n for(uint i=start;i<end;i++)sum+=groups[i];\n sums[lane]=sum;barrier();\n for(uint d=1u;d<256u;d*=2u){uint add=lane>=d?sums[lane-d]:0u;barrier();sums[lane]+=add;barrier();}\n uint offset=lane==0u?0u:sums[lane-1u];\n for(uint i=start;i<end;i++){uint n=groups[i];groups[i]=offset;offset+=n;}\n if(lane==255u){count=sums[255]*3u;instances=1u;firstIndex=0u;baseVertex=0u;baseInstance=0u;}\n}\n\n";
   private static final String SCATTER = "#version 430\nlayout(local_size_x=256) in;\nlayout(std430,binding=1) writeonly buffer OutputIndices { uint indices[]; };\nlayout(std430,binding=3) readonly buffer Flags { uint flags[]; };\nlayout(std430,binding=4) readonly buffer Groups { uint groups[]; };\nuniform uint TriangleCount;\nvoid main(){uint q=gl_GlobalInvocationID.x;if(q>=TriangleCount)return;uint f=flags[q];if((f&0x80000000u)==0u)return;uint at=(groups[gl_WorkGroupID.x]+(f&0x7fffffffu)-1u)*3u;indices[at]=q*4u;indices[at+1u]=q*4u+1u;indices[at+2u]=q*4u+2u;}\n\n";
   private static final String VERTEX = "#version 150\nin vec3 Position;\nuniform mat4 ModelViewMat;\nuniform mat4 ProjMat;\nuniform float ReverseDepthScale;\nflat out uint SurfaceId;\nvoid main(){\n    gl_Position=ProjMat*ModelViewMat*vec4(Position,1.0);\n    // GL_ZERO_TO_ONE lets hardware rasterize reverse depth directly, preserving\n    // its precision and early depth rejection without a geometry shader.\n    gl_Position.z=ReverseDepthScale;SurfaceId=uint(gl_VertexID/4)*2u+1u;\n}\n\n";
   private static final String FRAGMENT = "#version 150\nflat in uint SurfaceId;out uint fragSurface;void main(){fragSurface=SurfaceId;}\n";

   private OutlineVisibleTrianglesStable() {
   }

   private static int shader(int var0, String var1) {
      int var2 = GL43C.glCreateShader(var0);
      GL43C.glShaderSource(var2, var1);
      GL43C.glCompileShader(var2);
      if (GL43C.glGetShaderi(var2, 35713) == 0) {
         String var3 = GL43C.glGetShaderInfoLog(var2);
         GL43C.glDeleteShader(var2);
         throw new IllegalStateException(var3);
      } else {
         return var2;
      }
   }

   private static int program(String var0, String var1, String var2) {
      int var3 = GL43C.glCreateProgram();

      try {
         for (int var4 = 0; var4 < 3; var4++) {
            String var5 = new String[]{var0, var1, var2}[var4];
            if (var5 != null) {
               int var6 = shader(new int[]{37305, 35633, 35632}[var4], var5);
               GL43C.glAttachShader(var3, var6);
               GL43C.glDeleteShader(var6);
            }
         }

         if (var1 != null) {
            GL43C.glBindAttribLocation(var3, 0, "Position");
         }

         GL43C.glLinkProgram(var3);
         if (GL43C.glGetProgrami(var3, 35714) == 0) {
            throw new IllegalStateException(GL43C.glGetProgramInfoLog(var3));
         } else {
            return var3;
         }
      } catch (Throwable var7) {
         GL43C.glDeleteProgram(var3);
         throw var7;
      }
   }

   private static void initialize() {
      if (draw == 0) {
         try {
            select = program(
               "#version 430\nlayout(local_size_x=256) in;\nlayout(std430,binding=0) readonly buffer InputVertices { vec4 vertices[]; };\nlayout(std430,binding=1) writeonly buffer OutputIndices { uint indices[]; };\nlayout(std430,binding=2) buffer Command { uint count; uint instances; uint firstIndex; uint baseVertex; uint baseInstance; };\nuniform mat4 ModelViewMat;\nuniform mat4 ProjMat;\nuniform vec2 ScreenSize;\nuniform mat4 RoundoffMat;\nuniform uint TriangleCount;\nlayout(std430,binding=3) writeonly buffer Flags { uint flags[]; };\nlayout(std430,binding=4) writeonly buffer Groups { uint groups[]; };\nshared uint alive[256];\nvec2 errorBound(vec4 clip,vec3 position){\n vec4 e=RoundoffMat*vec4(abs(position),1.0);\n if(clip.w<=e.w)return vec2(1e30);\n return (e.xy+abs(clip.xy/clip.w)*e.w)/(clip.w-e.w)*ScreenSize*0.5;\n}\nvoid main(){\n uint lane=gl_LocalInvocationID.x,q=gl_GlobalInvocationID.x;bool keep=q<TriangleCount;\n if(keep){\n  uint b=q*4u;\n  vec4 a=ProjMat*ModelViewMat*vec4(vertices[b].xyz,1);\n  vec4 c=ProjMat*ModelViewMat*vec4(vertices[b+1u].xyz,1);\n  vec4 d=ProjMat*ModelViewMat*vec4(vertices[b+2u].xyz,1);\n  if(min(a.w,min(c.w,d.w))>0.01){\n   vec2 p=(a.xy/a.w*0.5+0.5)*ScreenSize;\n   vec2 r=(c.xy/c.w*0.5+0.5)*ScreenSize;\n   vec2 s=(d.xy/d.w*0.5+0.5)*ScreenSize;\n   vec2 padding=vec2(0.02)+max(errorBound(a,vertices[b].xyz),max(errorBound(c,vertices[b+1u].xyz),errorBound(d,vertices[b+2u].xyz)));\n   vec2 lo=min(p,min(r,s))-padding,hi=max(p,max(r,s))+padding;\n   keep=!any(greaterThan(ceil(lo-0.5),floor(hi-0.5)))&&all(greaterThanEqual(hi,vec2(0)))&&all(lessThanEqual(lo,ScreenSize));\n  }\n }\n alive[lane]=keep?1u:0u;barrier();\n for(uint delta=1u;delta<256u;delta*=2u){uint add=lane>=delta?alive[lane-delta]:0u;barrier();alive[lane]+=add;barrier();}\n if(lane==255u)groups[gl_WorkGroupID.x]=alive[255];\n if(q<TriangleCount)flags[q]=alive[lane]|(keep?0x80000000u:0u);\n}\n\n",
               null,
               null
            );
            prefix = program(
               "#version 430\nlayout(local_size_x=256) in;\nlayout(std430,binding=2) buffer Command { uint count; uint instances; uint firstIndex; uint baseVertex; uint baseInstance; };\nlayout(std430,binding=4) buffer Groups { uint groups[]; };\nuniform uint GroupCount;\nshared uint sums[256];\nvoid main(){\n uint lane=gl_LocalInvocationID.x,span=(GroupCount+255u)/256u,start=lane*span,end=min(start+span,GroupCount),sum=0u;\n for(uint i=start;i<end;i++)sum+=groups[i];\n sums[lane]=sum;barrier();\n for(uint d=1u;d<256u;d*=2u){uint add=lane>=d?sums[lane-d]:0u;barrier();sums[lane]+=add;barrier();}\n uint offset=lane==0u?0u:sums[lane-1u];\n for(uint i=start;i<end;i++){uint n=groups[i];groups[i]=offset;offset+=n;}\n if(lane==255u){count=sums[255]*3u;instances=1u;firstIndex=0u;baseVertex=0u;baseInstance=0u;}\n}\n\n",
               null,
               null
            );
            scatter = program(
               "#version 430\nlayout(local_size_x=256) in;\nlayout(std430,binding=1) writeonly buffer OutputIndices { uint indices[]; };\nlayout(std430,binding=3) readonly buffer Flags { uint flags[]; };\nlayout(std430,binding=4) readonly buffer Groups { uint groups[]; };\nuniform uint TriangleCount;\nvoid main(){uint q=gl_GlobalInvocationID.x;if(q>=TriangleCount)return;uint f=flags[q];if((f&0x80000000u)==0u)return;uint at=(groups[gl_WorkGroupID.x]+(f&0x7fffffffu)-1u)*3u;indices[at]=q*4u;indices[at+1u]=q*4u+1u;indices[at+2u]=q*4u+2u;}\n\n",
               null,
               null
            );
            draw = program(
               null,
               "#version 150\nin vec3 Position;\nuniform mat4 ModelViewMat;\nuniform mat4 ProjMat;\nuniform float ReverseDepthScale;\nflat out uint SurfaceId;\nvoid main(){\n    gl_Position=ProjMat*ModelViewMat*vec4(Position,1.0);\n    // GL_ZERO_TO_ONE lets hardware rasterize reverse depth directly, preserving\n    // its precision and early depth rejection without a geometry shader.\n    gl_Position.z=ReverseDepthScale;SurfaceId=uint(gl_VertexID/4)*2u+1u;\n}\n\n",
               "#version 150\nflat in uint SurfaceId;out uint fragSurface;void main(){fragSurface=SurfaceId;}\n"
            );
            flags = GL43C.glGenBuffers();
            groups = GL43C.glGenBuffers();
            command = GL43C.glGenBuffers();
            GL43C.glBindBuffer(37074, command);
            GL43C.glBufferData(37074, 20L, 35048);
         } catch (Throwable var1) {
            release();
            throw var1;
         }
      }
   }

   private static void matrix(int var0, String var1, FloatBuffer var2) {
      GL43C.glUniformMatrix4fv(GL43C.glGetUniformLocation(var0, var1), false, var2);
   }

   public static boolean draw(int var0, int var1, int var2, int var3) {
      if (!failed && var1 >= 1048576) {
         int var4 = GL43C.glGetUniformLocation(var2, "ReverseDepthScale");
         if (var4 < 0) {
            return false;
         }

         int var5 = GL43C.glGetInteger(35725);
         int var6 = GL43C.glGetInteger(37075);
         int var7 = GL43C.glGetInteger(36675);
         int var8 = GL43C.glGetInteger(34965);

         try (MemoryStack var9 = MemoryStack.stackPush()) {
            IntBuffer var10 = var9.mallocInt(5);
            LongBuffer var11 = var9.mallocLong(5);
            LongBuffer var12 = var9.mallocLong(5);

            for (int var13 = 0; var13 < 5; var13++) {
               var10.put(var13, GL43C.glGetIntegeri(37075, var13));
               var11.put(var13, GL43C.glGetInteger64i(37076, var13));
               var12.put(var13, GL43C.glGetInteger64i(37077, var13));
            }

            try {
               try {
                  initialize();
               } catch (RuntimeException var32) {
                  failed = true;
                  System.err.println("[LittlePerformanceImprovements] Outline coverage compaction unavailable: " + var32);
                  return false;
               }

               int var35 = var1 / 4;
               int var36 = (var35 + 255) / 256;
               if (capacity < var1) {
                  GL43C.glBindBuffer(37074, flags);
                  GL43C.glBufferData(37074, var1, 35048);
                  GL43C.glBindBuffer(37074, groups);
                  GL43C.glBufferData(37074, var36 * 4L, 35048);
                  capacity = var1;
               }

               FloatBuffer var15 = var9.mallocFloat(16);
               FloatBuffer var16 = var9.mallocFloat(16);
               IntBuffer var17 = var9.mallocInt(4);
               GL43C.glGetUniformfv(var2, GL43C.glGetUniformLocation(var2, "ModelViewMat"), var15);
               GL43C.glGetUniformfv(var2, GL43C.glGetUniformLocation(var2, "ProjMat"), var16);
               GL43C.glGetIntegerv(2978, var17);
               float var18 = GL43C.glGetUniformf(var2, var4);
               GL43C.glBindBufferBase(37074, 0, var0);
               GL43C.glBindBufferBase(37074, 1, var3);
               GL43C.glBindBufferBase(37074, 2, command);
               GL43C.glBindBufferBase(37074, 3, flags);
               GL43C.glBindBufferBase(37074, 4, groups);
               GL43C.glUseProgram(select);
               FloatBuffer var19 = var9.mallocFloat(16);

               for (int var20 = 0; var20 < 4; var20++) {
                  for (int var21 = 0; var21 < 4; var21++) {
                     double var22 = 0.0;

                     for (int var24 = 0; var24 < 4; var24++) {
                        var22 += Math.abs((double)var16.get(var24 * 4 + var21) * var15.get(var20 * 4 + var24));
                     }

                     var19.put(var20 * 4 + var21, (float)(var22 * 4.0E-6));
                  }
               }

               matrix(select, "RoundoffMat", var19);
               matrix(select, "ModelViewMat", var15);
               matrix(select, "ProjMat", var16);
               GL43C.glUniform2f(GL43C.glGetUniformLocation(select, "ScreenSize"), var17.get(2), var17.get(3));
               GL43C.glUniform1ui(GL43C.glGetUniformLocation(select, "TriangleCount"), var35);
               GL43C.glDispatchCompute(var36, 1, 1);
               GL43C.glMemoryBarrier(8192);
               GL43C.glUseProgram(prefix);
               GL43C.glUniform1ui(GL43C.glGetUniformLocation(prefix, "GroupCount"), var36);
               GL43C.glDispatchCompute(1, 1, 1);
               GL43C.glMemoryBarrier(8192);
               GL43C.glUseProgram(scatter);
               GL43C.glUniform1ui(GL43C.glGetUniformLocation(scatter, "TriangleCount"), var35);
               GL43C.glDispatchCompute(var36, 1, 1);
               GL43C.glMemoryBarrier(8258);
               GL43C.glUseProgram(draw);
               matrix(draw, "ModelViewMat", var15);
               matrix(draw, "ProjMat", var16);
               GL43C.glUniform1f(GL43C.glGetUniformLocation(draw, "ReverseDepthScale"), var18);
               GL43C.glBindBuffer(34963, var3);
               GL43C.glBindBuffer(36671, command);
               GL43C.glDrawElementsIndirect(4, 5125, 0L);
            } finally {
               int var26 = 0;

               while (true) {
                  if (var26 >= 5) {
                     GL43C.glBindBuffer(37074, var6);
                     GL43C.glBindBuffer(36671, var7);
                     GL43C.glBindBuffer(34963, var8);
                     GL43C.glUseProgram(var5);
                     break;
                  } else {
                     if (var10.get(var26) != 0 && var12.get(var26) > 0L) {
                        GL43C.glBindBufferRange(37074, var26, var10.get(var26), var11.get(var26), var12.get(var26));
                     } else {
                        GL43C.glBindBufferBase(37074, var26, var10.get(var26));
                     }

                     var26++;
                  }
               }
            }
         }

         return true;
      } else {
         return false;
      }
   }

   public static void release() {
      for (int var3 : new int[]{select, prefix, scatter, draw}) {
         if (var3 != 0) {
            GL43C.glDeleteProgram(var3);
         }
      }

      for (int var7 : new int[]{flags, groups, command}) {
         if (var7 != 0) {
            GL43C.glDeleteBuffers(var7);
         }
      }

      capacity = 0;
      command = 0;
      groups = 0;
      flags = 0;
      draw = 0;
      scatter = 0;
      prefix = 0;
      select = 0;
   }
}
