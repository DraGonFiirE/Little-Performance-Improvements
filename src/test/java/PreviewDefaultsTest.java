import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import dev.DraGonFiirE.littleperformanceimprovements.mixin.LittleConfigRenderingMixin;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewRendererMode;
class PreviewDefaultsTest {
 @Test void screenshotDefaults() {
  var config = new LittleConfigRenderingMixin() {};
  assertEquals(PreviewRendererMode.Surface, config.blueprintRenderer);
  assertEquals(PreviewRendererMode.Surface, config.chiselRenderer);
  assertEquals(2.0, config.previewOutlineThickness);
  assertEquals(1.0, config.previewOutlineAlpha);
  assertEquals(0.5, config.previewSurfaceAlpha);
  assertEquals(1.0, config.previewTextureAlpha);
  assertNotSame(config.blueprintColor, config.chiselColor);
 }
}
