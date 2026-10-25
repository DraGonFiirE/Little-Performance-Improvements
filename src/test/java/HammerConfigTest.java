import com.google.gson.JsonParser;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewConfigMigration;
import dev.DraGonFiirE.littleperformanceimprovements.mixin.LittleConfigRenderingMixin;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HammerConfigTest {
   @Test void migratesShapeSettingsWithoutChangingIndependentColors() {
      var json = JsonParser.parseString("""
         {"shapeRenderer":"Texture","shapeColor":123,"blueprintColor":456,"hammerColor":789}
         """).getAsJsonObject();
      PreviewConfigMigration.migrate(json);
      assertEquals("Texture", json.get("chiselRenderer").getAsString());
      assertEquals(123, json.get("chiselColor").getAsInt());
      assertEquals(456, json.get("blueprintColor").getAsInt());
      assertEquals(789, json.get("hammerColor").getAsInt());
      assertFalse(json.has("shapeRenderer"));
      assertFalse(json.has("shapeColor"));
      String saved = json.toString();
      PreviewConfigMigration.migrate(json);
      assertEquals(saved, json.toString());
   }

   @Test void explicitChiselSettingsWinOverBothLegacyFormats() {
      var json = JsonParser.parseString("""
         {"previewRenderer":"Surface","shapeRenderer":"Texture","chiselRenderer":"Default",
          "previewColor":1,"shapeColor":2,"chiselColor":3}
         """).getAsJsonObject();
      PreviewConfigMigration.migrate(json);
      assertEquals("Default", json.get("chiselRenderer").getAsString());
      assertEquals(3, json.get("chiselColor").getAsInt());
      assertEquals("Surface", json.get("blueprintRenderer").getAsString());
      assertFalse(json.has("hammerColor"));
      assertTrue(PreviewConfigMigration.isColorKey("hammerColor"));
      assertFalse(PreviewConfigMigration.isRendererKey("hammerRenderer"));
      assertFalse(PreviewConfigMigration.isColorKey("shapeColor"));
   }

   @Test void hammerColorIsIndependentAndRendererIsNotConfigurable() {
      var config = new LittleConfigRenderingMixin() {};
      assertNotSame(config.hammerColor, config.chiselColor);
      assertNotSame(config.hammerColor, config.blueprintColor);
      assertEquals(255, config.hammerColor.getRed());
      assertThrows(NoSuchFieldException.class, () -> LittleConfigRenderingMixin.class.getField("hammerRenderer"));
   }
}
