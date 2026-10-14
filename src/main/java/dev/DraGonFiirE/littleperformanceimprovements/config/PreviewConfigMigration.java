package dev.DraGonFiirE.littleperformanceimprovements.config;
import com.google.gson.JsonObject;
/** Upgrade old shared settings without overwriting independently chosen values. */
public final class PreviewConfigMigration {
 private PreviewConfigMigration() {}
 public static void migrate(JsonObject json) {
  if(json == null)return;
  split(json,"previewRenderer","blueprintRenderer","shapeRenderer");
  split(json,"previewColor","blueprintColor","shapeColor");
 }
 private static void split(JsonObject json,String old,String blueprint,String shape) {
  if(!json.has(old))return;
  var value=json.get(old);
  if(!json.has(blueprint))json.add(blueprint,value.deepCopy());
  if(!json.has(shape))json.add(shape,value.deepCopy());
  json.remove(old);
 }
 public static boolean isColorKey(Object name) {return "blueprintColor".equals(name)||"shapeColor".equals(name);}
 public static boolean isRendererKey(Object name) {return "blueprintRenderer".equals(name)||"shapeRenderer".equals(name);}
}
