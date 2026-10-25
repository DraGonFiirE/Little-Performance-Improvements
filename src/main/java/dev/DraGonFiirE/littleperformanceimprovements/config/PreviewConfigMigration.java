package dev.DraGonFiirE.littleperformanceimprovements.config;
import com.google.gson.JsonObject;
/** Upgrade old shared settings without overwriting independently chosen values. */
public final class PreviewConfigMigration {
 private PreviewConfigMigration() {}
 public static void migrate(JsonObject json) {
  if(json == null)return;
  rename(json,"shapeRenderer","chiselRenderer");
  rename(json,"shapeColor","chiselColor");
  split(json,"previewRenderer","blueprintRenderer","chiselRenderer");
  split(json,"previewColor","blueprintColor","chiselColor");
 }
 private static void rename(JsonObject json,String old,String current) {
  if(!json.has(old))return;
  if(!json.has(current))json.add(current,json.get(old).deepCopy());
  json.remove(old);
 }
 private static void split(JsonObject json,String old,String blueprint,String shape) {
  if(!json.has(old))return;
  var value=json.get(old);
  if(!json.has(blueprint))json.add(blueprint,value.deepCopy());
  if(!json.has(shape))json.add(shape,value.deepCopy());
  json.remove(old);
 }
 public static boolean isColorKey(Object name) {return "blueprintColor".equals(name)||"chiselColor".equals(name)||"hammerColor".equals(name);}
 public static boolean isRendererKey(Object name) {return "blueprintRenderer".equals(name)||"chiselRenderer".equals(name);}
}
