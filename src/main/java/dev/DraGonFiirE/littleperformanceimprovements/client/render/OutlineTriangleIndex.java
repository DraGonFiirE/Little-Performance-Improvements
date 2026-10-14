package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import java.nio.IntBuffer;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL33C;
import org.lwjgl.opengl.GL43C;
import org.lwjgl.system.MemoryStack;

public final class OutlineTriangleIndex {
   private static int compute;
   private static int indices;
   private static int flag;
   private static int capacity;
   private static int cachedBuffer;
   private static int cachedVertices;
   private static boolean dirty = true;
   private static boolean triangles;
   private static final String SOURCE = "#version 430\nlayout(local_size_x=256) in;\nlayout(std430,binding=0) readonly buffer InputVertices { vec4 vertices[]; };\nlayout(std430,binding=1) writeonly buffer TriangleIndices { uint indices[]; };\nlayout(std430,binding=2) buffer Validation { uint invalid; };\nuniform uint QuadCount;\nshared uint bad;\nvoid main(){\n  uint lane=gl_LocalInvocationID.x,q=gl_GlobalInvocationID.x;\n  if(lane==0u)bad=0u;barrier();\n  if(q<QuadCount){\n    uint base=q*4u;\n    vec3 a=vertices[base].xyz,c=vertices[base+2u].xyz,d=vertices[base+3u].xyz;\n    // Only exact repeated positions qualify. No tolerance or size threshold.\n    if(!all(equal(a,c))&&!all(equal(a,d))&&!all(equal(c,d)))atomicOr(bad,1u);\n    indices[q*3u]=base;indices[q*3u+1u]=base+1u;indices[q*3u+2u]=base+2u;\n  }\n  barrier();if(lane==0u&&bad!=0u)atomicOr(invalid,1u);\n}\n";

   private OutlineTriangleIndex() {
   }

   private static void initialize() {
      if (compute == 0) {
         int var0 = GL43C.glCreateShader(37305);
         int var1 = 0;

         try {
            GL43C.glShaderSource(
               var0,
               "#version 430\nlayout(local_size_x=256) in;\nlayout(std430,binding=0) readonly buffer InputVertices { vec4 vertices[]; };\nlayout(std430,binding=1) writeonly buffer TriangleIndices { uint indices[]; };\nlayout(std430,binding=2) buffer Validation { uint invalid; };\nuniform uint QuadCount;\nshared uint bad;\nvoid main(){\n  uint lane=gl_LocalInvocationID.x,q=gl_GlobalInvocationID.x;\n  if(lane==0u)bad=0u;barrier();\n  if(q<QuadCount){\n    uint base=q*4u;\n    vec3 a=vertices[base].xyz,c=vertices[base+2u].xyz,d=vertices[base+3u].xyz;\n    // Only exact repeated positions qualify. No tolerance or size threshold.\n    if(!all(equal(a,c))&&!all(equal(a,d))&&!all(equal(c,d)))atomicOr(bad,1u);\n    indices[q*3u]=base;indices[q*3u+1u]=base+1u;indices[q*3u+2u]=base+2u;\n  }\n  barrier();if(lane==0u&&bad!=0u)atomicOr(invalid,1u);\n}\n"
            );
            GL43C.glCompileShader(var0);
            if (GL43C.glGetShaderi(var0, 35713) == 0) {
               throw new IllegalStateException(GL43C.glGetShaderInfoLog(var0));
            }

            var1 = GL43C.glCreateProgram();
            GL43C.glAttachShader(var1, var0);
            GL43C.glLinkProgram(var1);
            if (GL43C.glGetProgrami(var1, 35714) == 0) {
               throw new IllegalStateException(GL43C.glGetProgramInfoLog(var1));
            }

            compute = var1;
            var1 = 0;
            indices = GL43C.glGenBuffers();
            flag = GL43C.glGenBuffers();
         } finally {
            GL43C.glDeleteShader(var0);
            if (var1 != 0) {
               GL43C.glDeleteProgram(var1);
            }
         }
      }
   }

   private static void prepare(int var0, int var1) {
      if (dirty || cachedBuffer != var0 || cachedVertices != var1) {
         initialize();
         int var2 = GL33C.glGetInteger(35725);
         int var3 = GL43C.glGetInteger(37075);
         int[] var4 = new int[3];
         long[] var5 = new long[3];
         long[] var6 = new long[3];

         for (int var7 = 0; var7 < 3; var7++) {
            var4[var7] = GL43C.glGetIntegeri(37075, var7);
            var5[var7] = GL43C.glGetInteger64i(37076, var7);
            var6[var7] = GL43C.glGetInteger64i(37077, var7);
         }

         try (MemoryStack var18 = MemoryStack.stackPush()) {
            if (capacity < var1) {
               GL43C.glBindBuffer(37074, indices);
               GL43C.glBufferData(37074, var1 * 3L, 35044);
               capacity = var1;
            }

            GL43C.glBindBuffer(37074, flag);
            GL43C.glBufferData(37074, (int[])(new int[]{0}), 35049);
            GL43C.glBindBufferBase(37074, 0, var0);
            GL43C.glBindBufferBase(37074, 1, indices);
            GL43C.glBindBufferBase(37074, 2, flag);
            GL43C.glUseProgram(compute);
            GL43C.glUniform1ui(GL43C.glGetUniformLocation(compute, "QuadCount"), var1 / 4);
            GL43C.glDispatchCompute((var1 / 4 + 255) / 256, 1, 1);
            GL43C.glMemoryBarrier(514);
            GL43C.glBindBuffer(37074, flag);
            IntBuffer var8 = var18.mallocInt(1);
            GL43C.glGetBufferSubData(37074, 0L, var8);
            triangles = var8.get(0) == 0;
            cachedBuffer = var0;
            cachedVertices = var1;
            dirty = false;
         } finally {
            int var11 = 0;

            while (true) {
               if (var11 >= 3) {
                  GL43C.glBindBuffer(37074, var3);
                  GL33C.glUseProgram(var2);
               } else {
                  if (var4[var11] != 0 && var6[var11] > 0L) {
                     GL43C.glBindBufferRange(37074, var11, var4[var11], var5[var11], var6[var11]);
                  } else {
                     GL43C.glBindBufferBase(37074, var11, var4[var11]);
                  }

                  var11++;
               }
            }
         }
      }
   }

   public static boolean draw(int var0, int var1) {
      if (OutlineDepthVertexSharing.draw(var0, var1)) {
         return true;
      }

      int var2 = GL33C.glGetUniformLocation(var1, "OutlineTriangleStep");
      GL33C.glUniform1ui(var2, 1);
      if (GL.getCapabilities().OpenGL43 && GL33C.glGetVertexAttribi(0, 34975) == var0) {
         int var3 = GL33C.glGetInteger(34964);

         long var4;
         try {
            GL33C.glBindBuffer(34962, var0);
            var4 = GL33C.glGetBufferParameteri64(34962, 34660);
         } finally {
            GL33C.glBindBuffer(34962, var3);
         }

         if (var4 % 64L == 0L && var4 / 16L >= 262144L && var4 / 16L <= 2147483647L) {
            int var6 = (int)(var4 / 16L);
            if ((var6 / 4 + 255L) / 256L > GL43C.glGetIntegeri(37310, 0)) {
               return false;
            }

            prepare(var0, var6);
            if (!triangles) {
               return false;
            }

            if (OutlineVisibleTrianglesStable.draw(var0, var6, var1, indices)) {
               return true;
            }

            int var7 = GL33C.glGetInteger(34965);

            try {
               GL33C.glBindBuffer(34963, indices);
               GL33C.glUniform1ui(var2, 2);
               GL33C.glDrawElements(4, var6 / 4 * 3, 5125, 0L);
            } finally {
               GL33C.glBindBuffer(34963, var7);
               GL33C.glUniform1ui(var2, 1);
            }

            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   public static void invalidate() {
      dirty = true;
   }

   public static void release() {
      OutlineVisibleTrianglesStable.release();
      if (compute != 0) {
         GL43C.glDeleteProgram(compute);
      }

      if (indices != 0) {
         GL43C.glDeleteBuffers(indices);
      }

      if (flag != 0) {
         GL43C.glDeleteBuffers(flag);
      }

      cachedVertices = 0;
      cachedBuffer = 0;
      capacity = 0;
      flag = 0;
      indices = 0;
      compute = 0;
      dirty = true;
      triangles = false;
   }
}
