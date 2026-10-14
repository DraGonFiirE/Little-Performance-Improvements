import java.nio.file.*;
import com.google.gson.*;
import dev.DraGonFiirE.littleperformanceimprovements.config.InitialBoxCountMigration;
public class MigrationTest {
 static void check(boolean ok) { if(!ok) throw new AssertionError(); }
 @org.junit.jupiter.api.Test public void regression(@org.junit.jupiter.api.io.TempDir Path base) throws Exception {
  Files.createDirectories(base);
  Path existing=Files.createTempDirectory(base,"existing-");
  Path file=existing.resolve("littletiles-client.json");
  String before="{\"building\":{\"lowResolutionBoxCount\":10,\"maxSavedActions\":64},\"rendering\":{\"itemLowResolutionBoxCount\":20,\"shapeRenderer\":\"Outline\"}}";
  Files.writeString(file,before); InitialBoxCountMigration.apply(existing);
  JsonObject root=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
  check(root.getAsJsonObject("building").get("lowResolutionBoxCount").getAsInt()==1000000);
  check(root.getAsJsonObject("rendering").get("itemLowResolutionBoxCount").getAsInt()==1000000);
  check(root.getAsJsonObject("building").get("maxSavedActions").getAsInt()==64);
  check(root.getAsJsonObject("rendering").get("shapeRenderer").getAsString().equals("Outline"));
  check(Files.readString(existing.resolve("littletiles-client.before-lpi-box-counts.json.bak")).equals(before));
  Files.writeString(file,before); InitialBoxCountMigration.apply(existing); check(Files.readString(file).equals(before));
  Files.delete(file); InitialBoxCountMigration.apply(existing); check(!Files.exists(file));
  Path fresh=Files.createTempDirectory(base,"fresh-"); InitialBoxCountMigration.apply(fresh);
  check(JsonParser.parseString(Files.readString(fresh.resolve("littletiles-client.json"))).getAsJsonObject().getAsJsonObject("building").get("lowResolutionBoxCount").getAsInt()==1000000);
  Path bad=Files.createTempDirectory(base,"invalid-"); Files.writeString(bad.resolve("littletiles-client.json"),"broken");
  try { InitialBoxCountMigration.apply(bad); throw new AssertionError(); } catch(JsonParseException | IllegalStateException expected) {}
  check(Files.readString(bad.resolve("littletiles-client.json")).equals("broken")); check(!Files.exists(bad.resolve("littleperformanceimprovements-box-counts-v1.done")));
  System.out.println("PASS: existing/fresh configs; unrelated fields and backup; later edits and deletion preserved; malformed config untouched.");
 }
}
