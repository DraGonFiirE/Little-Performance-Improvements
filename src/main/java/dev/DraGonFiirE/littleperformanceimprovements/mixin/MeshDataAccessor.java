package dev.DraGonFiirE.littleperformanceimprovements.mixin;

import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.ByteBufferBuilder.Result;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MeshData.class)
public interface MeshDataAccessor {
   @Accessor("indexBuffer")
   void littleperformanceimprovements$setIndexBuffer(Result var1);
}
