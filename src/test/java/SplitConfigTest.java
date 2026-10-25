import com.google.gson.*;
import dev.DraGonFiirE.littleperformanceimprovements.config.PreviewConfigMigration;
public class SplitConfigTest {
 static void check(boolean b){if(!b)throw new AssertionError();}
 @org.junit.jupiter.api.Test public void regression(){
 JsonObject old=JsonParser.parseString("{\"previewRenderer\":\"Outline\",\"previewColor\":-240,\"previewOutlineThickness\":10}").getAsJsonObject();PreviewConfigMigration.migrate(old);
 check(old.get("blueprintRenderer").getAsString().equals("Outline")&&old.get("chiselRenderer").getAsString().equals("Outline"));check(old.get("blueprintColor").getAsInt()==-240&&old.get("chiselColor").getAsInt()==-240);check(!old.has("previewRenderer")&&!old.has("previewColor")&&old.get("previewOutlineThickness").getAsInt()==10);
 JsonObject mixed=JsonParser.parseString("{\"previewRenderer\":\"Outline\",\"chiselRenderer\":\"Texture\",\"previewColor\":-240,\"blueprintColor\":123}").getAsJsonObject();PreviewConfigMigration.migrate(mixed);check(mixed.get("chiselRenderer").getAsString().equals("Texture")&&mixed.get("blueprintRenderer").getAsString().equals("Outline")&&mixed.get("blueprintColor").getAsInt()==123&&mixed.get("chiselColor").getAsInt()==-240);
 String saved=mixed.toString();PreviewConfigMigration.migrate(mixed);check(saved.equals(mixed.toString()));JsonObject fresh=new JsonObject();PreviewConfigMigration.migrate(fresh);check(fresh.size()==0);
 check(PreviewConfigMigration.isColorKey("blueprintColor")&&PreviewConfigMigration.isColorKey("chiselColor")&&!PreviewConfigMigration.isColorKey("previewColor"));check(PreviewConfigMigration.isRendererKey("blueprintRenderer")&&PreviewConfigMigration.isRendererKey("chiselRenderer")&&!PreviewConfigMigration.isRendererKey("previewRenderer"));
 System.out.println("PASS: migration preserves legacy values, independent overrides, unrelated settings, defaults and repeat loads; both GUI key pairs recognized.");}
}
