package dev.DraGonFiirE.littleperformanceimprovements.client.render;

import dev.DraGonFiirE.littleperformanceimprovements.LittlePerformanceImprovements;
import dev.DraGonFiirE.littleperformanceimprovements.performance.HeldPreviewBuild;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.FaceInfo;
import net.minecraft.client.renderer.FaceInfo.VertexInfo;
import team.creative.creativecore.client.render.box.RenderBox;
import team.creative.creativecore.client.render.face.RenderBoxFace;
import team.creative.creativecore.client.render.face.RenderBoxFaceSpecial;
import team.creative.creativecore.common.util.math.base.Axis;
import team.creative.creativecore.common.util.math.base.Facing;
import team.creative.creativecore.common.util.math.box.BoxCorner;
import team.creative.creativecore.common.util.math.geo.NormalPlaneF;
import team.creative.creativecore.common.util.math.geo.VectorFan;
import team.creative.creativecore.common.util.math.vec.Vec3f;
import team.creative.littletiles.client.render.tile.LittleRenderBox;
import team.creative.littletiles.client.render.tile.LittleRenderBoxTransformable;
import team.creative.littletiles.common.grid.LittleGrid;
import team.creative.littletiles.common.math.box.LittleBox;
import team.creative.littletiles.common.math.box.LittleTransformableBox;
import team.creative.littletiles.common.math.box.LittleTransformableBox.VectorFanFaceCache;
import team.creative.littletiles.common.math.face.LittleFace;

public final class BlueprintSurfaceFaceFilter {
   private static final int MAX_INDEXED_FACE_CELLS = 256;
   private static final RenderBoxFaceSpecial EMPTY_CACHED_FACE = new RenderBoxFaceSpecial(List.of(), 1.0F);

   private BlueprintSurfaceFaceFilter() {
   }

   public static List<RenderBox> retainAirTouching(List<RenderBox> boxes, LittleGrid grid) {
      HeldPreviewBuild.check();
      if (boxes.size() < 2) {
         return boxes;
      }

      List<LittleRenderBox> renderBoxes = new ArrayList<>(boxes.size());

      for (RenderBox box : boxes) {
         if (box instanceof LittleRenderBox little && little.box != null) {
            renderBoxes.add(little);
         }

         HeldPreviewBuild.check();
      }

      if (renderBoxes.size() < 2) {
         return boxes;
      }


      BlueprintSurfaceFaceFilter.FaceIndex index = new BlueprintSurfaceFaceFilter.FaceIndex(Math.max(1, grid.count / 8), renderBoxes.size());

      for (LittleRenderBox box : renderBoxes) {
         index.add(box);
         HeldPreviewBuild.check();
      }

      index.finishBuild();

      int adjacentCuts = 0;
      int projectedTransformableCuts = 0;
      int filteredFaces = 0;
      long exactOrdinaryFaces = 0L;
      long oppositeQueries = 0L;
      long emptyOppositeQueries = 0L;
      long ordinaryFallbackCovered = 0L;
      long ordinaryUnionFaces = 0L;
      long generatedFaces = 0L;
      long transformableOwnerFaces = 0L;
      long projectedQueries = 0L;

      for (LittleRenderBox renderBox : renderBoxes) {
         boolean transformableOwner = renderBox instanceof LittleRenderBoxTransformable;
         int renderBoxId = index.id(renderBox);
         LittleBox owner = renderBox.box;

         for (Facing facing : Facing.VALUES) {
            if (transformableOwner) {
               transformableOwnerFaces++;
            }

            if (!transformableOwner && index.isExactOrdinaryCovered(renderBoxId, facing)) {
               renderBox.setFace(facing, EMPTY_CACHED_FACE);
               adjacentCuts++;
               filteredFaces++;
               exactOrdinaryFaces++;
            } else {
               oppositeQueries++;
               List<LittleRenderBox> candidates = index.findOpposite(owner, facing);
               if (candidates.isEmpty()) {
                  emptyOppositeQueries++;
               }

               if (!transformableOwner) {
                  boolean fullyCovered = false;
                  Axis one = facing.one();
                  Axis two = facing.two();

                  for (LittleRenderBox candidate : candidates) {
                     if (candidate != renderBox && !(candidate instanceof LittleRenderBoxTransformable)) {
                        LittleBox candidateBox = candidate.box;
                        if (candidateBox.getMin(one) == owner.getMin(one)
                           && candidateBox.getMax(one) == owner.getMax(one)
                           && candidateBox.getMin(two) == owner.getMin(two)
                           && candidateBox.getMax(two) == owner.getMax(two)) {
                           fullyCovered = true;
                           break;
                        }
                     }
                  }

                  if (fullyCovered) {
                     renderBox.setFace(facing, EMPTY_CACHED_FACE);
                     adjacentCuts++;
                     filteredFaces++;
                     ordinaryFallbackCovered++;
                     continue;
                  }
               }

               if (!transformableOwner && !index.hasTransformables() && !candidates.isEmpty()) {
                  BlueprintSurfaceFaceFilter.OrdinaryClip ordinaryClip = clipOrdinaryFace(owner, facing, candidates, renderBox);
                  if (ordinaryClip.touched()) {
                     if (ordinaryClip.exposed().isEmpty()) {
                        renderBox.setFace(facing, EMPTY_CACHED_FACE);
                     } else {
                        renderBox.setFace(facing, new RenderBoxFaceSpecial(normalizeForPreview(ordinaryClip.exposed(), owner), 1.0F));
                     }

                     adjacentCuts += ordinaryClip.cutCount();
                     filteredFaces++;
                     ordinaryUnionFaces++;
                     continue;
                  }
               }

               List<LittleRenderBox> projectedCandidates = List.of();
               if (candidates.isEmpty()) {
                  if (transformableOwner || !index.hasTransformables()) {
                     continue;
                  }

                  projectedQueries++;
                  projectedCandidates = index.findProjectedTransformables(owner, facing);
                  if (projectedCandidates.isEmpty()) {
                     continue;
                  }
               }

               LittleFace face = owner.generateFace(grid, facing);
               if (face != null) {
                  generatedFaces++;
                  boolean checkedAny = false;
                  if (projectedCandidates.isEmpty()) {
                     for (LittleRenderBox candidate : candidates) {
                        if (candidate != renderBox) {
                           if (candidate instanceof LittleRenderBoxTransformable transform) {
                              Facing contact = facing.opposite();
                              if (!transformableOwner && !index.wasTransformAxisVisible(transform, contact)
                                 || transformableOwner && !candidate.shouldRenderFace(contact)) {
                                 continue;
                              }
                           }

                           candidate.box.fill(face);
                           checkedAny = true;
                           adjacentCuts++;
                        }
                     }
                  }

                  if (!transformableOwner && index.hasTransformables()) {
                     if (projectedCandidates.isEmpty()) {
                        projectedQueries++;
                        projectedCandidates = index.findProjectedTransformables(face);
                     }

                     for (LittleRenderBox candidate : projectedCandidates) {
                        if (candidate != renderBox && cutProjectedTransformable(face, (LittleRenderBoxTransformable)candidate)) {
                           checkedAny = true;
                           projectedTransformableCuts++;
                        }
                     }
                  }

                  if (checkedAny && face.isPartiallyFilled()) {
                     List<VectorFan> exposed = face.generateFans();
                     if (transformableOwner) {
                        exposed = clipTransformableAgainstOrdinaryVolumes(exposed, renderBox, index);
                     }

                     if (exposed.isEmpty()) {
                        renderBox.setFace(facing, EMPTY_CACHED_FACE);
                     } else if (transformableOwner) {
                        renderBox.setFace(facing, new RenderBoxFaceSpecial(exposed, (float)grid.pixelLength));
                     } else {
                        renderBox.setFace(facing, new RenderBoxFaceSpecial(normalizeForPreview(exposed, owner), 1.0F));
                     }

                     filteredFaces++;
                  }
               }
            }
         }

         HeldPreviewBuild.check();
      }


      BlueprintSurfaceFaceFilter.SharedFaceSuppression sharedSuppression = suppressFullySharedFaces(renderBoxes, grid, index);




      return boxes;
   }

   private static BlueprintSurfaceFaceFilter.OrdinaryClip clipOrdinaryFace(
      LittleBox owner, Facing facing, List<LittleRenderBox> candidates, LittleRenderBox ownerRenderBox
   ) {
      HeldPreviewBuild.check();
      Axis one = facing.one();
      Axis two = facing.two();
      List<BlueprintSurfaceFaceFilter.FaceRect> exposed = new ArrayList<>();
      exposed.add(new BlueprintSurfaceFaceFilter.FaceRect(owner.getMin(one), owner.getMin(two), owner.getMax(one), owner.getMax(two)));
      boolean touched = false;
      int cutCount = 0;

      for (LittleRenderBox candidate : candidates) {
         if (candidate != ownerRenderBox && !(candidate instanceof LittleRenderBoxTransformable)) {
            LittleBox box = candidate.box;
            int cutMinOne = Math.max(owner.getMin(one), box.getMin(one));
            int cutMinTwo = Math.max(owner.getMin(two), box.getMin(two));
            int cutMaxOne = Math.min(owner.getMax(one), box.getMax(one));
            int cutMaxTwo = Math.min(owner.getMax(two), box.getMax(two));
            if (cutMinOne < cutMaxOne && cutMinTwo < cutMaxTwo) {
               touched = true;
               cutCount++;
               List<BlueprintSurfaceFaceFilter.FaceRect> next = new ArrayList<>(exposed.size() + 3);

               for (BlueprintSurfaceFaceFilter.FaceRect rect : exposed) {
                  subtract(rect, cutMinOne, cutMinTwo, cutMaxOne, cutMaxTwo, next);
               }

               exposed = next;
               if (exposed.isEmpty()) {
                  break;
               }

               HeldPreviewBuild.check();
            }
         }
      }

      if (!touched) {
         return BlueprintSurfaceFaceFilter.OrdinaryClip.UNTOUCHED;
      }

      mergeAdjacentRectangles(exposed);
      List<VectorFan> fans = new ArrayList<>(exposed.size());

      for (BlueprintSurfaceFaceFilter.FaceRect rect : exposed) {
         fans.add(rectangleFan(owner.get(facing), facing, rect));
         HeldPreviewBuild.check();
      }

      return new BlueprintSurfaceFaceFilter.OrdinaryClip(true, cutCount, fans);
   }

   private static void subtract(
      BlueprintSurfaceFaceFilter.FaceRect rect, int cutMinOne, int cutMinTwo, int cutMaxOne, int cutMaxTwo, List<BlueprintSurfaceFaceFilter.FaceRect> output
   ) {
      int minOne = Math.max(rect.minOne(), cutMinOne);
      int minTwo = Math.max(rect.minTwo(), cutMinTwo);
      int maxOne = Math.min(rect.maxOne(), cutMaxOne);
      int maxTwo = Math.min(rect.maxTwo(), cutMaxTwo);
      if (minOne < maxOne && minTwo < maxTwo) {
         if (rect.minOne() < minOne) {
            output.add(new BlueprintSurfaceFaceFilter.FaceRect(rect.minOne(), rect.minTwo(), minOne, rect.maxTwo()));
         }

         if (maxOne < rect.maxOne()) {
            output.add(new BlueprintSurfaceFaceFilter.FaceRect(maxOne, rect.minTwo(), rect.maxOne(), rect.maxTwo()));
         }

         if (rect.minTwo() < minTwo) {
            output.add(new BlueprintSurfaceFaceFilter.FaceRect(minOne, rect.minTwo(), maxOne, minTwo));
         }

         if (maxTwo < rect.maxTwo()) {
            output.add(new BlueprintSurfaceFaceFilter.FaceRect(minOne, maxTwo, maxOne, rect.maxTwo()));
         }
      } else {
         output.add(rect);
      }
   }

   private static void mergeAdjacentRectangles(List<BlueprintSurfaceFaceFilter.FaceRect> rectangles) {
      boolean merged;
      label55:
      do {
         merged = false;

         for (int first = 0; first < rectangles.size(); first++) {
            BlueprintSurfaceFaceFilter.FaceRect a = rectangles.get(first);

            for (int second = first + 1; second < rectangles.size(); second++) {
               BlueprintSurfaceFaceFilter.FaceRect b = rectangles.get(second);
               BlueprintSurfaceFaceFilter.FaceRect joined = null;
               if (a.minTwo() != b.minTwo() || a.maxTwo() != b.maxTwo() || a.maxOne() != b.minOne() && b.maxOne() != a.minOne()) {
                  if (a.minOne() == b.minOne() && a.maxOne() == b.maxOne() && (a.maxTwo() == b.minTwo() || b.maxTwo() == a.minTwo())) {
                     joined = new BlueprintSurfaceFaceFilter.FaceRect(
                        a.minOne(), Math.min(a.minTwo(), b.minTwo()), a.maxOne(), Math.max(a.maxTwo(), b.maxTwo())
                     );
                  }
               } else {
                  joined = new BlueprintSurfaceFaceFilter.FaceRect(Math.min(a.minOne(), b.minOne()), a.minTwo(), Math.max(a.maxOne(), b.maxOne()), a.maxTwo());
               }

               if (joined != null) {
                  rectangles.set(first, joined);
                  rectangles.remove(second);
                  merged = true;
                  continue label55;
               }
            }
         }
      } while (merged);
   }

   private static VectorFan rectangleFan(int origin, Facing facing, BlueprintSurfaceFaceFilter.FaceRect rect) {
      FaceInfo info = FaceInfo.fromFacing(facing.toVanilla());
      Vec3f[] points = new Vec3f[4];

      for (int vertex = 0; vertex < 4; vertex++) {
         VertexInfo corner = info.getVertexInfo(vertex);
         points[vertex] = new Vec3f(
            rectangleCoordinate(corner.xFace, origin, facing, rect),
            rectangleCoordinate(corner.yFace, origin, facing, rect),
            rectangleCoordinate(corner.zFace, origin, facing, rect)
         );
      }

      return new VectorFan(points);
   }

   private static float rectangleCoordinate(int faceIndex, int origin, Facing facing, BlueprintSurfaceFaceFilter.FaceRect rect) {
      Facing corner = Facing.get(faceIndex);
      if (corner.axis == facing.one()) {
         return corner.positive ? rect.maxOne() : rect.minOne();
      } else if (corner.axis == facing.two()) {
         return corner.positive ? rect.maxTwo() : rect.minTwo();
      } else {
         return origin;
      }
   }

   private static BlueprintSurfaceFaceFilter.SharedFaceSuppression suppressFullySharedFaces(
      List<LittleRenderBox> boxes, LittleGrid grid, BlueprintSurfaceFaceFilter.FaceIndex index
   ) {
      HeldPreviewBuild.check();
      int transformableAxisFaces = 0;
      int mixedSideFaces = 0;

      for (LittleRenderBox renderBox : boxes) {
         if (renderBox instanceof LittleRenderBoxTransformable transformable) {
            for (Facing facing : Facing.VALUES) {
               for (LittleRenderBox candidate : index.findExactOpposite(renderBox.box, facing)) {
                  if (candidate != renderBox) {
                     if (candidate instanceof LittleRenderBoxTransformable other) {
                        if (index.id(renderBox) < index.id(candidate) && ExactTransformableContact.matches(transformable, other, facing)) {
                           retainOnlyTiltedFans(transformable, facing, grid);
                           retainOnlyTiltedFans(other, facing.opposite(), grid);
                           transformableAxisFaces += 2;
                        }
                     } else if (facing != Facing.UP && facing != Facing.DOWN && index.wasTransformAxisVisible(transformable, facing)) {
                        candidate.setFace(facing.opposite(), EMPTY_CACHED_FACE);
                        mixedSideFaces++;
                     }
                  }
               }
            }

            HeldPreviewBuild.check();
         }
      }

      int tiltedFaces = suppressCoincidentTiltedFaces(boxes, grid);
      int exactMixedFaces = suppressExactMixedContacts(boxes, grid, index);
      return new BlueprintSurfaceFaceFilter.SharedFaceSuppression(transformableAxisFaces, mixedSideFaces, tiltedFaces, exactMixedFaces);
   }

   private static int suppressExactMixedContacts(List<LittleRenderBox> boxes, LittleGrid grid, BlueprintSurfaceFaceFilter.FaceIndex index) {
      HeldPreviewBuild.check();
      int changedFaces = 0;

      for (LittleRenderBox renderBox : boxes) {
         if (renderBox instanceof LittleRenderBoxTransformable transformable) {
            for (Facing facing : Facing.VALUES) {
               List<LittleRenderBox> candidates = index.findOpposite(transformable.box, facing);
               boolean hasOrdinary = false;

               for (LittleRenderBox candidate : candidates) {
                  if (candidate != transformable && !(candidate instanceof LittleRenderBoxTransformable)) {
                     hasOrdinary = true;
                     break;
                  }
               }

               if (hasOrdinary) {
                  BlueprintSurfaceFaceFilter.TransformFaceSnapshot snapshot = snapshotFace(transformable, facing, grid);
                  if (snapshot != null) {
                     List<VectorFan> retained = new ArrayList<>(snapshot.fans.size());
                     boolean changed = false;

                     for (VectorFan fan : snapshot.fans) {
                        if (!isAxisAligned(fan)) {
                           retained.add(fan);
                        } else {
                           List<VectorFan> fragments = List.of(fan);
                           Iterator source = candidates.iterator();

                           while (true) {
                              if (source.hasNext()) {
                                 LittleRenderBox candidate = (LittleRenderBox)source.next();
                                 if (candidate == transformable || candidate instanceof LittleRenderBoxTransformable) {
                                    continue;
                                 }

                                 List<VectorFan> next = new ArrayList<>();

                                 for (VectorFan fragment : fragments) {
                                    next.addAll(subtractBoxFromTiltedFan(fragment, candidate.box));
                                 }

                                 if (next.size() != fragments.size() || next.size() == 1 && next.getFirst() != fan) {
                                    changed = true;
                                 }

                                 fragments = next;
                                 if (!fragments.isEmpty()) {
                                    continue;
                                 }
                              }

                              retained.addAll(fragments);
                              break;
                           }
                        }
                     }

                     if (changed) {
                        transformable.setFace(facing, retained.isEmpty() ? EMPTY_CACHED_FACE : new RenderBoxFaceSpecial(retained, snapshot.scale));
                        changedFaces++;
                     }
                  }
               }
            }

            HeldPreviewBuild.check();
         }
      }

      for (LittleRenderBox ownerRenderBox : boxes) {
         if (!(ownerRenderBox instanceof LittleRenderBoxTransformable)) {
            LittleBox owner = ownerRenderBox.box;

            for (Facing facing : Facing.VALUES) {
               List<LittleRenderBox> candidates = index.findOpposite(owner, facing);
               List<VectorFan> transformCutters = new ArrayList<>();

               for (LittleRenderBox candidate : candidates) {
                  if (candidate instanceof LittleRenderBoxTransformable transform) {
                     Facing contact = facing.opposite();
                     VectorFanFaceCache cache = transform.getFaceCache(contact);
                     if (cache != null && cache.hasAxisStrip() && index.wasTransformAxisVisible(transform, contact)) {
                        transformCutters.addAll(cache.axisStrips);
                     }
                  }
               }

               if (!transformCutters.isEmpty()) {
                  LittleFace face = owner.generateFace(grid, facing);
                  if (face != null) {
                     for (LittleRenderBox candidate : candidates) {
                        if (candidate != ownerRenderBox && !(candidate instanceof LittleRenderBoxTransformable)) {
                           candidate.box.fill(face);
                        }
                     }

                     List<VectorFan> exposed = face.generateFans();

                     for (VectorFan cutter : transformCutters) {
                        List<VectorFan> next = new ArrayList<>();

                        for (VectorFan source : exposed) {
                           next.addAll(subtractConvexFan(source, cutter));
                        }

                        exposed = next;
                        if (exposed.isEmpty()) {
                           break;
                        }
                     }

                     ownerRenderBox.setFace(facing, exposed.isEmpty() ? EMPTY_CACHED_FACE : new RenderBoxFaceSpecial(normalizeForPreview(exposed, owner), 1.0F));
                     changedFaces++;
                  }
               }
            }

            HeldPreviewBuild.check();
         }
      }

      return changedFaces;
   }

   private static int suppressCoincidentTiltedFaces(List<LittleRenderBox> boxes, LittleGrid grid) {
      HeldPreviewBuild.check();
      Map<BlueprintSurfaceFaceFilter.TiltedFanKey, List<BlueprintSurfaceFaceFilter.TiltedFanRef>> byVertices = new HashMap<>();
      List<BlueprintSurfaceFaceFilter.TransformFaceSnapshot> snapshots = new ArrayList<>();

      for (LittleRenderBox renderBox : boxes) {
         if (renderBox instanceof LittleRenderBoxTransformable transformable) {
            for (Facing facing : Facing.VALUES) {
               BlueprintSurfaceFaceFilter.TransformFaceSnapshot snapshot = snapshotFace(transformable, facing, grid);
               if (snapshot != null) {
                  snapshots.add(snapshot);

                  for (int fanIndex = 0; fanIndex < snapshot.fans.size(); fanIndex++) {
                     VectorFan fan = snapshot.fans.get(fanIndex);
                     if (!isAxisAligned(fan)) {
                        BlueprintSurfaceFaceFilter.TiltedFanKey key = BlueprintSurfaceFaceFilter.TiltedFanKey.of(fan);
                        List<BlueprintSurfaceFaceFilter.TiltedFanRef> bucket = byVertices.computeIfAbsent(key, ignored -> new ArrayList<>(2));
                        Vec3f normal = fan.createPlane().normal;

                        for (BlueprintSurfaceFaceFilter.TiltedFanRef other : bucket) {
                           if (other.snapshot.owner != transformable && opposingNormals(normal, other.normal)) {
                              snapshot.removed[fanIndex] = true;
                              other.snapshot.removed[other.fanIndex] = true;
                           }
                        }

                        bucket.add(new BlueprintSurfaceFaceFilter.TiltedFanRef(snapshot, fanIndex, normal.copy()));
                     }
                  }
               }
            }

            HeldPreviewBuild.check();
         }
      }

      int removed = 0;

      for (BlueprintSurfaceFaceFilter.TransformFaceSnapshot snapshot : snapshots) {
         boolean changed = false;

         for (int fanIndex = 0; fanIndex < snapshot.fans.size(); fanIndex++) {
            if (snapshot.removed[fanIndex]) {
               changed = true;
               removed++;
            }
         }

         if (changed) {
            List<VectorFan> retained = new ArrayList<>(snapshot.fans.size());

            for (int fanIndex = 0; fanIndex < snapshot.fans.size(); fanIndex++) {
               if (!snapshot.removed[fanIndex]) {
                  retained.add(snapshot.fans.get(fanIndex));
               }
            }

            snapshot.owner.setFace(snapshot.facing, retained.isEmpty() ? EMPTY_CACHED_FACE : new RenderBoxFaceSpecial(retained, snapshot.scale));
            HeldPreviewBuild.check();
         }
      }

      return removed;
   }

   private static BlueprintSurfaceFaceFilter.TransformFaceSnapshot snapshotFace(LittleRenderBoxTransformable box, Facing facing, LittleGrid grid) {
      RenderBoxFace face = box.getFace(facing);
      List<VectorFan> fans = new ArrayList<>();
      float scale;
      if (face.hasCachedFans()) {
         fans.addAll(face.getCachedFans());
         scale = face.getScale();
      } else {
         VectorFanFaceCache cache = box.getFaceCache(facing);
         if (cache == null) {
            return null;
         }

         if (face.shouldRender() && cache.hasAxisStrip()) {
            fans.addAll(cache.axisStrips);
         }

         if (cache.hasTiltedStripsRendering()) {
            cache.collectAllTiltedStripsRendering(fans);
         }

         scale = (float)grid.pixelLength;
      }

      return fans.isEmpty() ? null : new BlueprintSurfaceFaceFilter.TransformFaceSnapshot(box, facing, fans, scale);
   }

   private static boolean opposingNormals(Vec3f first, Vec3f second) {
      float firstLength = (float)Math.sqrt(first.x * first.x + first.y * first.y + first.z * first.z);
      float secondLength = (float)Math.sqrt(second.x * second.x + second.y * second.y + second.z * second.z);
      if (!(firstLength <= 5.0E-4F) && !(secondLength <= 5.0E-4F)) {
         float cosine = (first.x * second.x + first.y * second.y + first.z * second.z) / (firstLength * secondLength);
         return cosine < -0.999F;
      } else {
         return false;
      }
   }

   private static void retainOnlyTiltedFans(LittleRenderBoxTransformable box, Facing facing, LittleGrid grid) {
      RenderBoxFace original = box.getFace(facing);
      List<VectorFan> tilted = new ArrayList<>();
      float scale;
      if (original.hasCachedFans()) {
         for (VectorFan fan : original.getCachedFans()) {
            if (!isAxisAligned(fan)) {
               tilted.add(fan);
            }
         }

         scale = original.getScale();
      } else {
         VectorFanFaceCache cache = box.getFaceCache(facing);
         if (cache != null && cache.hasTiltedStripsRendering()) {
            cache.collectAllTiltedStripsRendering(tilted);
         }

         scale = (float)grid.pixelLength;
      }

      box.setFace(facing, tilted.isEmpty() ? EMPTY_CACHED_FACE : new RenderBoxFaceSpecial(tilted, scale));
   }

   private static boolean isAxisAligned(VectorFan fan) {
      Vec3f normal = fan.createPlane().normal;
      int activeAxes = (Math.abs(normal.x) > 5.0E-4F ? 1 : 0) + (Math.abs(normal.y) > 5.0E-4F ? 1 : 0) + (Math.abs(normal.z) > 5.0E-4F ? 1 : 0);
      return activeAxes <= 1;
   }

   private static List<VectorFan> clipTransformableAgainstOrdinaryVolumes(
      List<VectorFan> source, LittleRenderBox owner, BlueprintSurfaceFaceFilter.FaceIndex index
   ) {
      HeldPreviewBuild.check();
      List<VectorFan> result = new ArrayList<>(source.size());

      for (VectorFan fan : source) {
         Vec3f normal = fan.createPlane().normal;
         int activeAxes = (Math.abs(normal.x) > 5.0E-4F ? 1 : 0) + (Math.abs(normal.y) > 5.0E-4F ? 1 : 0) + (Math.abs(normal.z) > 5.0E-4F ? 1 : 0);
         if (activeAxes <= 1) {
            result.add(fan);
         } else {
            List<VectorFan> fragments = new ArrayList<>();
            fragments.add(fan);
            Iterator var9 = index.findOrdinaryVolumes(fan).iterator();

            while (true) {
               if (var9.hasNext()) {
                  LittleRenderBox candidate = (LittleRenderBox)var9.next();
                  if (candidate == owner) {
                     continue;
                  }

                  List<VectorFan> next = new ArrayList<>();

                  for (VectorFan fragment : fragments) {
                     next.addAll(subtractBoxFromTiltedFan(fragment, candidate.box));
                  }

                  fragments = next;
                  if (!fragments.isEmpty()) {
                     continue;
                  }
               }

               result.addAll(fragments);
               HeldPreviewBuild.check();
               break;
            }
         }
      }

      return result;
   }

   private static List<VectorFan> subtractBoxFromTiltedFan(VectorFan fan, LittleBox box) {
      HeldPreviewBuild.check();
      NormalPlaneF plane = fan.createPlane();
      Axis dropped = dominantAxis(plane.normal);
      Axis one = dropped.one();
      Axis two = dropped.two();
      List<Vec3f> section = boxPlaneSection(box, plane);
      if (section.size() < 3) {
         return List.of(fan);
      }

      section = sectionHull(section, one, two);
      if (section.size() < 3) {
         return List.of(fan);
      }

      List<List<Vec3f>> inside = new ArrayList<>();
      inside.add(copyPoints(fan));
      List<List<Vec3f>> outside = new ArrayList<>();
      int edge = 0;

      while (edge < section.size() && !inside.isEmpty()) {
         Vec3f start = section.get(edge);
         Vec3f end = section.get((edge + 1) % section.size());
         List<List<Vec3f>> nextInside = new ArrayList<>();

         for (List<Vec3f> polygon : inside) {
            List<Vec3f> keptInside = clipHalfPlane(polygon, start, end, one, two, true);
            List<Vec3f> keptOutside = clipHalfPlane(polygon, start, end, one, two, false);
            if (keptInside.size() >= 3) {
               nextInside.add(keptInside);
            }

            if (keptOutside.size() >= 3) {
               outside.add(keptOutside);
            }
         }

         inside = nextInside;
         edge++;
         HeldPreviewBuild.check();
      }

      if (outside.isEmpty()) {
         return inside.isEmpty() ? List.of(fan) : List.of();
      }

      List<VectorFan> result = new ArrayList<>(outside.size());

      for (List<Vec3f> polygon : outside) {
         result.add(new VectorFan((Vec3f[])polygon.toArray(Vec3f[]::new)));
         HeldPreviewBuild.check();
      }

      return result;
   }

   private static List<VectorFan> subtractConvexFan(VectorFan source, VectorFan cutter) {
      HeldPreviewBuild.check();
      NormalPlaneF sourcePlane = source.createPlane();
      int point = 0;

      while (point < cutter.count()) {
         if (Math.abs(signedDistance(sourcePlane, cutter.get(point))) > 0.001F) {
            return List.of(source);
         }

         point++;
         HeldPreviewBuild.check();
      }

      Axis dropped = dominantAxis(sourcePlane.normal);
      Axis one = dropped.one();
      Axis two = dropped.two();
      List<Vec3f> section = copyPoints(cutter);
      if (polygonArea(section, one, two) < 0.0F) {
         Collections.reverse(section);
      }

      List<List<Vec3f>> inside = new ArrayList<>();
      inside.add(copyPoints(source));
      List<List<Vec3f>> outside = new ArrayList<>();
      int edge = 0;

      while (edge < section.size() && !inside.isEmpty()) {
         Vec3f start = section.get(edge);
         Vec3f end = section.get((edge + 1) % section.size());
         List<List<Vec3f>> nextInside = new ArrayList<>();

         for (List<Vec3f> polygon : inside) {
            List<Vec3f> keptInside = clipHalfPlane(polygon, start, end, one, two, true);
            List<Vec3f> keptOutside = clipHalfPlane(polygon, start, end, one, two, false);
            if (keptInside.size() >= 3) {
               nextInside.add(keptInside);
            }

            if (keptOutside.size() >= 3) {
               outside.add(keptOutside);
            }
         }

         inside = nextInside;
         edge++;
         HeldPreviewBuild.check();
      }

      if (outside.isEmpty()) {
         return inside.isEmpty() ? List.of(source) : List.of();
      }

      List<VectorFan> result = new ArrayList<>(outside.size());

      for (List<Vec3f> polygon : outside) {
         result.add(new VectorFan((Vec3f[])polygon.toArray(Vec3f[]::new)));
         HeldPreviewBuild.check();
      }

      return result;
   }

   private static float polygonArea(List<Vec3f> polygon, Axis one, Axis two) {
      float area = 0.0F;

      for (int point = 0; point < polygon.size(); point++) {
         Vec3f current = polygon.get(point);
         Vec3f next = polygon.get((point + 1) % polygon.size());
         area += current.get(one) * next.get(two) - next.get(one) * current.get(two);
      }

      return area * 0.5F;
   }

   private static List<Vec3f> boxPlaneSection(LittleBox box, NormalPlaneF plane) {
      Vec3f[] corners = new Vec3f[8];

      for (int corner = 0; corner < 8; corner++) {
         corners[corner] = new Vec3f((corner & 1) == 0 ? box.minX : box.maxX, (corner & 2) == 0 ? box.minY : box.maxY, (corner & 4) == 0 ? box.minZ : box.maxZ);
      }

      List<Vec3f> section = new ArrayList<>(8);

      for (int first = 0; first < corners.length; first++) {
         for (int bit : new int[]{1, 2, 4}) {
            int second = first ^ bit;
            if (second > first) {
               Vec3f a = corners[first];
               Vec3f b = corners[second];
               float aDistance = signedDistance(plane, a);
               float bDistance = signedDistance(plane, b);
               if (Math.abs(aDistance) <= 5.0E-4F) {
                  addSectionPoint(section, a.copy());
               }

               if (Math.abs(bDistance) <= 5.0E-4F) {
                  addSectionPoint(section, b.copy());
               }

               if (!(aDistance * bDistance >= 0.0F)) {
                  float progress = aDistance / (aDistance - bDistance);
                  addSectionPoint(section, new Vec3f(a.x + (b.x - a.x) * progress, a.y + (b.y - a.y) * progress, a.z + (b.z - a.z) * progress));
               }
            }
         }
      }

      return section;
   }

   private static float signedDistance(NormalPlaneF plane, Vec3f point) {
      return (point.x - plane.origin.x) * plane.normal.x + (point.y - plane.origin.y) * plane.normal.y + (point.z - plane.origin.z) * plane.normal.z;
   }

   private static Axis dominantAxis(Vec3f normal) {
      float x = Math.abs(normal.x);
      float y = Math.abs(normal.y);
      float z = Math.abs(normal.z);
      if (x >= y && x >= z) {
         return Axis.X;
      } else {
         return y >= z ? Axis.Y : Axis.Z;
      }
   }

   private static List<Vec3f> copyPoints(VectorFan fan) {
      List<Vec3f> result = new ArrayList<>(fan.count());

      for (int index = 0; index < fan.count(); index++) {
         result.add(fan.get(index).copy());
      }

      return result;
   }

   private static List<Vec3f> clipHalfPlane(List<Vec3f> polygon, Vec3f edgeStart, Vec3f edgeEnd, Axis one, Axis two, boolean keepInside) {
      List<Vec3f> result = new ArrayList<>(polygon.size() + 1);
      Vec3f previous = polygon.getLast();
      float previousSide = edgeSide(edgeStart, edgeEnd, previous, one, two);
      boolean previousKept = keepInside ? previousSide >= -5.0E-4F : previousSide <= 5.0E-4F;

      for (Vec3f current : polygon) {
         float currentSide = edgeSide(edgeStart, edgeEnd, current, one, two);
         boolean currentKept = keepInside ? currentSide >= -5.0E-4F : currentSide <= 5.0E-4F;
         if (currentKept != previousKept) {
            float denominator = previousSide - currentSide;
            float progress = Math.abs(denominator) <= 5.0E-4F ? 0.0F : previousSide / denominator;
            result.add(
               new Vec3f(
                  previous.x + (current.x - previous.x) * progress,
                  previous.y + (current.y - previous.y) * progress,
                  previous.z + (current.z - previous.z) * progress
               )
            );
         }

         if (currentKept) {
            result.add(current.copy());
         }

         previous = current;
         previousSide = currentSide;
         previousKept = currentKept;
      }

      removeDuplicatePolygonPoints(result);
      return result;
   }

   private static float edgeSide(Vec3f start, Vec3f end, Vec3f point, Axis one, Axis two) {
      return (end.get(one) - start.get(one)) * (point.get(two) - start.get(two)) - (end.get(two) - start.get(two)) * (point.get(one) - start.get(one));
   }

   private static void removeDuplicatePolygonPoints(List<Vec3f> points) {
      for (int index = points.size() - 1; index >= 0 && points.size() > 1; index--) {
         Vec3f current = points.get(index);
         Vec3f previous = points.get((index + points.size() - 1) % points.size());
         if (current.epsilonEquals(previous, 5.0E-4F)) {
            points.remove(index);
         }
      }
   }

   private static boolean cutProjectedTransformable(LittleFace face, LittleRenderBoxTransformable transformable) {
      if (!(transformable.box instanceof LittleTransformableBox source)) {
         return false;
      } else {
         Vec3f[] var19 = source.getTiltedCorners();
         float offsetX = transformable.minX * face.grid.count - source.minX;
         float offsetY = transformable.minY * face.grid.count - source.minY;
         float offsetZ = transformable.minZ * face.grid.count - source.minZ;
         if (offsetX != 0.0F || offsetY != 0.0F || offsetZ != 0.0F) {
            for (Vec3f corner : var19) {
               corner.x += offsetX;
               corner.y += offsetY;
               corner.z += offsetZ;
            }
         }

         List<Vec3f> section = new ArrayList<>(8);
         BoxCorner[] topology = BoxCorner.values();

         for (int first = 0; first < topology.length; first++) {
            for (int second = first + 1; second < topology.length; second++) {
               BoxCorner aCorner = topology[first];
               BoxCorner bCorner = topology[second];
               int differences = (aCorner.x != bCorner.x ? 1 : 0) + (aCorner.y != bCorner.y ? 1 : 0) + (aCorner.z != bCorner.z ? 1 : 0);
               if (differences == 1) {
                  Vec3f a = var19[first];
                  Vec3f b = var19[second];
                  float aDistance = a.get(face.facing.axis) - face.origin;
                  float bDistance = b.get(face.facing.axis) - face.origin;
                  if (Math.abs(aDistance) < 5.0E-4F) {
                     addSectionPoint(section, a.copy());
                  }

                  if (Math.abs(bDistance) < 5.0E-4F) {
                     addSectionPoint(section, b.copy());
                  }

                  if (!(aDistance * bDistance >= 0.0F)) {
                     float progress = aDistance / (aDistance - bDistance);
                     addSectionPoint(section, new Vec3f(a.x + (b.x - a.x) * progress, a.y + (b.y - a.y) * progress, a.z + (b.z - a.z) * progress));
                  }
               }
            }
         }

         List<Vec3f> hull = sectionHull(section, face.one, face.two);
         if (hull.size() < 3) {
            return false;
         }

         float normal = crossOnAxis(hull.get(0), hull.get(1), hull.get(2), face.facing.axis);
         if (normal * face.facing.opposite().offset() < 0.0F) {
            Collections.reverse(hull);
         }

         face.cut(List.of(new VectorFan((Vec3f[])hull.toArray(Vec3f[]::new))));
         return true;
      }
   }

   private static void addSectionPoint(List<Vec3f> points, Vec3f candidate) {
      for (Vec3f point : points) {
         if (point.epsilonEquals(candidate, 5.0E-4F)) {
            return;
         }
      }

      points.add(candidate);
   }

   private static List<Vec3f> sectionHull(List<Vec3f> points, Axis one, Axis two) {
      if (points.size() < 4) {
         return points;
      }

      points.sort((a, b) -> {
         int compareOne = Float.compare(a.get(one), b.get(one));
         return compareOne != 0 ? compareOne : Float.compare(a.get(two), b.get(two));
      });
      List<Vec3f> hull = new ArrayList<>(points.size() * 2);

      for (Vec3f point : points) {
         while (hull.size() >= 2 && cross2d(hull.get(hull.size() - 2), hull.getLast(), point, one, two) <= 5.0E-4F) {
            hull.removeLast();
         }

         hull.add(point);
      }

      int lowerSize = hull.size();

      for (int index = points.size() - 2; index >= 0; index--) {
         Vec3f point = points.get(index);

         while (hull.size() > lowerSize && cross2d(hull.get(hull.size() - 2), hull.getLast(), point, one, two) <= 5.0E-4F) {
            hull.removeLast();
         }

         hull.add(point);
      }

      if (hull.size() > 1) {
         hull.removeLast();
      }

      return hull;
   }

   private static float cross2d(Vec3f a, Vec3f b, Vec3f c, Axis one, Axis two) {
      return (b.get(one) - a.get(one)) * (c.get(two) - a.get(two)) - (b.get(two) - a.get(two)) * (c.get(one) - a.get(one));
   }

   private static float crossOnAxis(Vec3f a, Vec3f b, Vec3f c, Axis axis) {
      float abX = b.x - a.x;
      float abY = b.y - a.y;
      float abZ = b.z - a.z;
      float acX = c.x - a.x;
      float acY = c.y - a.y;
      float acZ = c.z - a.z;

      return switch (axis) {
         case X -> abY * acZ - abZ * acY;
         case Y -> abZ * acX - abX * acZ;
         case Z -> abX * acY - abY * acX;
      };
   }

   private static List<VectorFan> normalizeForPreview(List<VectorFan> fans, LittleBox owner) {
      float sizeX = owner.maxX - owner.minX;
      float sizeY = owner.maxY - owner.minY;
      float sizeZ = owner.maxZ - owner.minZ;
      List<VectorFan> normalized = new ArrayList<>(fans.size());

      for (VectorFan source : fans) {
         VectorFan fan = source.copy();

         for (int i = 0; i < fan.count(); i++) {
            fan.get(i).x = (fan.get(i).x - owner.minX) / sizeX;
            fan.get(i).y = (fan.get(i).y - owner.minY) / sizeY;
            fan.get(i).z = (fan.get(i).z - owner.minZ) / sizeZ;
         }

         normalized.add(fan);
      }

      return normalized;
   }

   private static final class FaceIndex {
      private final int cellSize;
      private final IdentityHashMap<LittleRenderBox, Integer> boxIds;
      private final ArrayList<LittleRenderBox> boxesById;
      private final int[] visitMarks;
      private final boolean[] exactOrdinaryCovered;
      private final boolean[] originalTransformAxisVisible;
      private Long2LongOpenHashMap exactOrdinaryFaces;
      private int visitMark;
      private boolean hasTransformables;
      private final ArrayList<LittleRenderBox> queryResult = new ArrayList<>();
      private final ArrayList<LittleRenderBox> filteredResult = new ArrayList<>();
      private final Int2ObjectOpenHashMap<IntArrayList>[] planes;
      private final Int2ObjectOpenHashMap<Long2ObjectOpenHashMap<IntArrayList>>[] cells;
      private final Int2ObjectOpenHashMap<IntArrayList>[] broad;
      private final Long2ObjectOpenHashMap<IntArrayList>[] transformProjectionCells;
      private final IntArrayList[] broadTransformProjections;
      private final Map<BlueprintSurfaceFaceFilter.VolumeCellKey, IntArrayList> ordinaryVolumeCells = new HashMap<>();
      private final IntArrayList broadOrdinaryVolumes = new IntArrayList();
      private long candidateVisits;
      private long uniqueCandidates;
      private long queryCount;

      FaceIndex(int cellSize, int expectedBoxes) {
         this.cellSize = cellSize;
         this.boxIds = new IdentityHashMap<>(expectedBoxes);
         this.boxesById = new ArrayList<>(expectedBoxes);
         this.visitMarks = new int[expectedBoxes];
         this.exactOrdinaryCovered = new boolean[Math.multiplyExact(expectedBoxes, Facing.VALUES.length)];
         this.originalTransformAxisVisible = new boolean[Math.multiplyExact(expectedBoxes, Facing.VALUES.length)];
         this.exactOrdinaryFaces = new Long2LongOpenHashMap(Math.max(16, Math.multiplyExact(expectedBoxes, 3)));
         int facings = Facing.VALUES.length;
         this.planes = new Int2ObjectOpenHashMap[facings];
         this.cells = new Int2ObjectOpenHashMap[facings];
         this.broad = new Int2ObjectOpenHashMap[facings];
         this.transformProjectionCells = new Long2ObjectOpenHashMap[facings];
         this.broadTransformProjections = new IntArrayList[facings];
         int facing = 0;

         while (facing < facings) {
            this.planes[facing] = new Int2ObjectOpenHashMap<>();
            this.cells[facing] = new Int2ObjectOpenHashMap<>();
            this.broad[facing] = new Int2ObjectOpenHashMap<>();
            this.transformProjectionCells[facing] = new Long2ObjectOpenHashMap<>();
            facing++;
            HeldPreviewBuild.check();
         }
      }

      void add(LittleRenderBox renderBox) {
         int boxId = this.boxIds.size();
         this.boxIds.put(renderBox, boxId);
         this.boxesById.add(renderBox);
         LittleBox box = renderBox.box;
         boolean ordinary = !(renderBox instanceof LittleRenderBoxTransformable);
         if (ordinary) {
            this.addOrdinaryVolume(boxId, box);
            this.addExactOrdinaryFaces(renderBox, box, boxId);
         } else {
            this.hasTransformables = true;
            LittleRenderBoxTransformable transformable = (LittleRenderBoxTransformable)renderBox;

            for (Facing facing : Facing.VALUES) {
               VectorFanFaceCache faceCache = transformable.getFaceCache(facing);
               this.originalTransformAxisVisible[boxId * Facing.VALUES.length + facing.ordinal()] = faceCache != null
                  && faceCache.hasAxisStrip()
                  && transformable.getFace(facing).shouldRender();
            }
         }

         for (Facing facing : Facing.VALUES) {
            Axis one = facing.one();
            Axis two = facing.two();
            int plane = box.get(facing);
            int minOne = box.getMin(one);
            int maxOne = box.getMax(one);
            int minTwo = box.getMin(two);
            int maxTwo = box.getMax(two);
            int facingIndex = facing.ordinal();
            this.planes[facingIndex].computeIfAbsent(plane, ignored -> new IntArrayList()).add(boxId);
            int minCellOne = Math.floorDiv(minOne, this.cellSize);
            int maxCellOne = Math.floorDiv(maxOne - 1, this.cellSize);
            int minCellTwo = Math.floorDiv(minTwo, this.cellSize);
            int maxCellTwo = Math.floorDiv(maxTwo - 1, this.cellSize);
            long count = (long)(maxCellOne - minCellOne + 1) * (maxCellTwo - minCellTwo + 1);
            if (count > 256L) {
               this.broad[facingIndex].computeIfAbsent(plane, ignored -> new IntArrayList()).add(boxId);
               if (renderBox instanceof LittleRenderBoxTransformable) {
                  IntArrayList projected = this.broadTransformProjections[facingIndex];
                  if (projected == null) {
                     this.broadTransformProjections[facingIndex] = projected = new IntArrayList();
                  }

                  projected.add(boxId);
               }
            } else {
               for (int oneCell = minCellOne; oneCell <= maxCellOne; oneCell++) {
                  for (int twoCell = minCellTwo; twoCell <= maxCellTwo; twoCell++) {
                     this.cells[facingIndex]
                        .computeIfAbsent(plane, ignored -> new Long2ObjectOpenHashMap<>())
                        .computeIfAbsent(packedCell(oneCell, twoCell), ignored -> new IntArrayList())
                        .add(boxId);
                  }
               }

               if (renderBox instanceof LittleRenderBoxTransformable) {
                  for (int oneCell = minCellOne; oneCell <= maxCellOne; oneCell++) {
                     for (int twoCell = minCellTwo; twoCell <= maxCellTwo; twoCell++) {
                        this.transformProjectionCells[facingIndex].computeIfAbsent(packedCell(oneCell, twoCell), ignored -> new IntArrayList()).add(boxId);
                     }
                  }
               }
            }
         }
      }

      void finishBuild() {
         this.exactOrdinaryFaces = null;
      }

      boolean isExactOrdinaryCovered(int boxId, Facing facing) {
         return boxId >= 0 && this.exactOrdinaryCovered[boxId * Facing.VALUES.length + facing.ordinal()];
      }

      private void addExactOrdinaryFaces(LittleRenderBox renderBox, LittleBox box, int boxId) {
         for (Facing facing : Facing.VALUES) {
            long key = exactFaceKey(box, facing);
            long packed = this.exactOrdinaryFaces.get(key);
            Facing opposite = facing.opposite();
            long oppositeSlot = opposite.positive ? packed & 4294967295L : packed >>> 32;
            if (oppositeSlot != 0L) {
               int oppositeId = (int)oppositeSlot - 1;
               LittleRenderBox candidate = this.boxesById.get(oppositeId);
               if (isExactFaceMatch(box, facing, candidate.box)) {
                  this.exactOrdinaryCovered[boxId * Facing.VALUES.length + facing.ordinal()] = true;
                  this.exactOrdinaryCovered[oppositeId * Facing.VALUES.length + opposite.ordinal()] = true;
               }
            }

            long ownSlot = boxId + 1L & 4294967295L;
            if (facing.positive) {
               if ((packed & 4294967295L) == 0L) {
                  this.exactOrdinaryFaces.put(key, packed & -4294967296L | ownSlot);
               }
            } else if (packed >>> 32 == 0L) {
               this.exactOrdinaryFaces.put(key, packed & 4294967295L | ownSlot << 32);
            }
         }
      }

      private static boolean isExactFaceMatch(LittleBox owner, Facing facing, LittleBox candidate) {
         Axis one = facing.one();
         Axis two = facing.two();
         return candidate.get(facing.opposite()) == owner.get(facing)
            && candidate.getMin(one) == owner.getMin(one)
            && candidate.getMax(one) == owner.getMax(one)
            && candidate.getMin(two) == owner.getMin(two)
            && candidate.getMax(two) == owner.getMax(two);
      }

      private static long exactFaceKey(LittleBox box, Facing facing) {
         Axis one = facing.one();
         Axis two = facing.two();
         long hash = -7046029254386353131L ^ facing.axis.ordinal();
         hash = mixExactFace(hash, box.get(facing));
         hash = mixExactFace(hash, box.getMin(one));
         hash = mixExactFace(hash, box.getMax(one));
         hash = mixExactFace(hash, box.getMin(two));
         return mixExactFace(hash, box.getMax(two));
      }

      private static long mixExactFace(long hash, int value) {
         long mixed = value * -7046029254386353131L;
         mixed ^= mixed >>> 30;
         mixed *= -4658895280553007687L;
         mixed ^= mixed >>> 27;
         mixed *= -7723592293110705685L;
         mixed ^= mixed >>> 31;
         hash ^= mixed;
         hash = Long.rotateLeft(hash, 27);
         return hash * 5L + 1390208809L;
      }

      private void addOrdinaryVolume(int boxId, LittleBox box) {
         int minX = Math.floorDiv(box.minX, this.cellSize);
         int maxX = Math.floorDiv(box.maxX - 1, this.cellSize);
         int minY = Math.floorDiv(box.minY, this.cellSize);
         int maxY = Math.floorDiv(box.maxY - 1, this.cellSize);
         int minZ = Math.floorDiv(box.minZ, this.cellSize);
         int maxZ = Math.floorDiv(box.maxZ - 1, this.cellSize);
         long count = (long)(maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
         if (count > 256L) {
            this.broadOrdinaryVolumes.add(boxId);
         } else {
            for (int x = minX; x <= maxX; x++) {
               for (int y = minY; y <= maxY; y++) {
                  for (int z = minZ; z <= maxZ; z++) {
                     this.ordinaryVolumeCells.computeIfAbsent(new BlueprintSurfaceFaceFilter.VolumeCellKey(x, y, z), ignored -> new IntArrayList()).add(boxId);
                  }
               }
            }
         }
      }

      List<LittleRenderBox> findOrdinaryVolumes(VectorFan fan) {
         float minX = Float.POSITIVE_INFINITY;
         float minY = Float.POSITIVE_INFINITY;
         float minZ = Float.POSITIVE_INFINITY;
         float maxX = Float.NEGATIVE_INFINITY;
         float maxY = Float.NEGATIVE_INFINITY;
         float maxZ = Float.NEGATIVE_INFINITY;

         for (int point = 0; point < fan.count(); point++) {
            Vec3f vec = fan.get(point);
            minX = Math.min(minX, vec.x);
            minY = Math.min(minY, vec.y);
            minZ = Math.min(minZ, vec.z);
            maxX = Math.max(maxX, vec.x);
            maxY = Math.max(maxY, vec.y);
            maxZ = Math.max(maxZ, vec.z);
         }

         int minCellX = (int)Math.floor((minX - 5.0E-4F) / this.cellSize);
         int maxCellX = (int)Math.floor((maxX + 5.0E-4F) / this.cellSize);
         int minCellY = (int)Math.floor((minY - 5.0E-4F) / this.cellSize);
         int maxCellY = (int)Math.floor((maxY + 5.0E-4F) / this.cellSize);
         int minCellZ = (int)Math.floor((minZ - 5.0E-4F) / this.cellSize);
         int maxCellZ = (int)Math.floor((maxZ + 5.0E-4F) / this.cellSize);
         List<LittleRenderBox> result = this.beginQuery(this.queryResult);
         this.addUnique(result, this.broadOrdinaryVolumes);

         for (int x = minCellX; x <= maxCellX; x++) {
            for (int y = minCellY; y <= maxCellY; y++) {
               for (int z = minCellZ; z <= maxCellZ; z++) {
                  IntArrayList local = this.ordinaryVolumeCells.get(new BlueprintSurfaceFaceFilter.VolumeCellKey(x, y, z));
                  if (local != null) {
                     this.addUnique(result, local);
                  }
               }
            }
         }

         return result;
      }

      List<LittleRenderBox> findOpposite(LittleBox owner, Facing facing) {
         return this.findOpposite(
            facing.opposite(),
            owner.get(facing),
            owner.getMin(facing.one()),
            owner.getMax(facing.one()),
            owner.getMin(facing.two()),
            owner.getMax(facing.two())
         );
      }

      List<LittleRenderBox> findOpposite(LittleFace face) {
         return this.findOpposite(face.facing.opposite(), face.origin, face.minOne, face.maxOne, face.minTwo, face.maxTwo);
      }

      private List<LittleRenderBox> findOpposite(Facing candidateFacing, int plane, int minOne, int maxOne, int minTwo, int maxTwo) {
         int facingIndex = candidateFacing.ordinal();
         int minCellOne = Math.floorDiv(minOne, this.cellSize);
         int maxCellOne = Math.floorDiv(maxOne - 1, this.cellSize);
         int minCellTwo = Math.floorDiv(minTwo, this.cellSize);
         int maxCellTwo = Math.floorDiv(maxTwo - 1, this.cellSize);
         long count = (long)(maxCellOne - minCellOne + 1) * (maxCellTwo - minCellTwo + 1);
         if (count > 256L) {
            IntArrayList result = this.planes[facingIndex].get(plane);
            return result != null ? this.materialize(result) : List.of();
         }

         List<LittleRenderBox> result = this.beginQuery(this.queryResult);
         IntArrayList broadCandidates = this.broad[facingIndex].get(plane);
         if (broadCandidates != null) {
            this.addUnique(result, broadCandidates);
         }

         Long2ObjectOpenHashMap<IntArrayList> planeCells = this.cells[facingIndex].get(plane);
         if (planeCells == null) {
            return result;
         }

         for (int oneCell = minCellOne; oneCell <= maxCellOne; oneCell++) {
            for (int twoCell = minCellTwo; twoCell <= maxCellTwo; twoCell++) {
               IntArrayList local = planeCells.get(packedCell(oneCell, twoCell));
               if (local != null) {
                  this.addUnique(result, local);
               }
            }
         }

         return result;
      }

      List<LittleRenderBox> findExactOpposite(LittleBox owner, Facing facing) {
         IntArrayList possible = this.planes[facing.opposite().ordinal()].get(owner.get(facing));
         Axis one = facing.one();
         Axis two = facing.two();
         this.filteredResult.clear();
         if (possible == null) {
            return this.filteredResult;
         }

         for (int possibleIndex = 0; possibleIndex < possible.size(); possibleIndex++) {
            LittleRenderBox candidate = this.boxesById.get(possible.getInt(possibleIndex));
            LittleBox box = candidate.box;
            if (box.getMin(one) == owner.getMin(one)
               && box.getMax(one) == owner.getMax(one)
               && box.getMin(two) == owner.getMin(two)
               && box.getMax(two) == owner.getMax(two)) {
               this.filteredResult.add(candidate);
            }
         }

         return this.filteredResult;
      }

      int id(LittleRenderBox box) {
         Integer id = this.boxIds.get(box);
         return id == null ? -1 : id;
      }

      boolean wasTransformAxisVisible(LittleRenderBoxTransformable box, Facing facing) {
         int id = this.id(box);
         return id >= 0 && this.originalTransformAxisVisible[id * Facing.VALUES.length + facing.ordinal()];
      }

      List<LittleRenderBox> findProjectedTransformables(LittleFace face) {
         return this.findProjectedTransformables(face.facing, face.origin, face.one, face.two, face.minOne, face.maxOne, face.minTwo, face.maxTwo);
      }

      List<LittleRenderBox> findProjectedTransformables(LittleBox owner, Facing facing) {
         return this.findProjectedTransformables(
            facing,
            owner.get(facing),
            facing.one(),
            facing.two(),
            owner.getMin(facing.one()),
            owner.getMax(facing.one()),
            owner.getMin(facing.two()),
            owner.getMax(facing.two())
         );
      }

      private List<LittleRenderBox> findProjectedTransformables(Facing facing, int origin, Axis one, Axis two, int minOne, int maxOne, int minTwo, int maxTwo) {
         int facingIndex = facing.ordinal();
         int minCellOne = Math.floorDiv(minOne, this.cellSize);
         int maxCellOne = Math.floorDiv(maxOne - 1, this.cellSize);
         int minCellTwo = Math.floorDiv(minTwo, this.cellSize);
         int maxCellTwo = Math.floorDiv(maxTwo - 1, this.cellSize);
         List<LittleRenderBox> possible = this.beginQuery(this.queryResult);
         IntArrayList broadCandidates = this.broadTransformProjections[facingIndex];
         if (broadCandidates != null) {
            this.addUnique(possible, broadCandidates);
         }

         for (int oneCell = minCellOne; oneCell <= maxCellOne; oneCell++) {
            for (int twoCell = minCellTwo; twoCell <= maxCellTwo; twoCell++) {
               IntArrayList local = this.transformProjectionCells[facingIndex].get(packedCell(oneCell, twoCell));
               if (local != null) {
                  this.addUnique(possible, local);
               }
            }
         }

         this.filteredResult.clear();
         List<LittleRenderBox> result = this.filteredResult;

         for (LittleRenderBox candidate : possible) {
            LittleBox box = candidate.box;
            if (box.getMax(one) > minOne && box.getMin(one) < maxOne && box.getMax(two) > minTwo && box.getMin(two) < maxTwo) {
               int minimum = box.getMin(facing.axis);
               int maximum = box.getMax(facing.axis);
               boolean spansPlane = minimum < origin && maximum > origin;
               if (spansPlane) {
                  result.add(candidate);
               }
            }
         }

         return result;
      }

      private List<LittleRenderBox> beginQuery(ArrayList<LittleRenderBox> result) {
         this.queryCount++;
         result.clear();
         if (++this.visitMark == 0) {
            Arrays.fill(this.visitMarks, 0);
            this.visitMark = 1;
         }

         return result;
      }

      private List<LittleRenderBox> materialize(IntArrayList ids) {
         this.queryResult.clear();

         for (int index = 0; index < ids.size(); index++) {
            this.queryResult.add(this.boxesById.get(ids.getInt(index)));
         }

         return this.queryResult;
      }

      private void addUnique(List<LittleRenderBox> result, IntArrayList candidates) {
         this.candidateVisits = this.candidateVisits + candidates.size();

         for (int candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
            int id = candidates.getInt(candidateIndex);
            if (this.visitMarks[id] != this.visitMark) {
               this.visitMarks[id] = this.visitMark;
               result.add(this.boxesById.get(id));
               this.uniqueCandidates++;
            }
         }
      }

      boolean hasTransformables() {
         return this.hasTransformables;
      }

      long candidateVisits() {
         return this.candidateVisits;
      }

      long uniqueCandidates() {
         return this.uniqueCandidates;
      }

      long queryCount() {
         return this.queryCount;
      }

      private static long packedCell(int one, int two) {
         return (long)one << 32 ^ two & 4294967295L;
      }
   }

   private record FaceRect(int minOne, int minTwo, int maxOne, int maxTwo) {
   }

   private record OrdinaryClip(boolean touched, int cutCount, List<VectorFan> exposed) {
      private static final BlueprintSurfaceFaceFilter.OrdinaryClip UNTOUCHED = new BlueprintSurfaceFaceFilter.OrdinaryClip(false, 0, List.of());
   }

   private record QuantizedPoint(long x, long y, long z) implements Comparable<BlueprintSurfaceFaceFilter.QuantizedPoint> {
      public int compareTo(BlueprintSurfaceFaceFilter.QuantizedPoint other) {
         int compare = Long.compare(this.x, other.x);
         if (compare != 0) {
            return compare;
         }

         compare = Long.compare(this.y, other.y);
         return compare != 0 ? compare : Long.compare(this.z, other.z);
      }
   }

   private record SharedFaceSuppression(int transformableAxisFaces, int mixedSideFaces, int tiltedFaces, int exactMixedFaces) {
   }

   private record TiltedFanKey(List<BlueprintSurfaceFaceFilter.QuantizedPoint> points) {
      private static BlueprintSurfaceFaceFilter.TiltedFanKey of(VectorFan fan) {
         List<BlueprintSurfaceFaceFilter.QuantizedPoint> points = new ArrayList<>(fan.count());

         for (int point = 0; point < fan.count(); point++) {
            Vec3f vector = fan.get(point);
            points.add(new BlueprintSurfaceFaceFilter.QuantizedPoint(quantize(vector.x), quantize(vector.y), quantize(vector.z)));
         }

         points.sort(BlueprintSurfaceFaceFilter.QuantizedPoint::compareTo);
         return new BlueprintSurfaceFaceFilter.TiltedFanKey(List.copyOf(points));
      }

      private static long quantize(float value) {
         return Math.round(value / 5.0E-4F);
      }
   }

   private record TiltedFanRef(BlueprintSurfaceFaceFilter.TransformFaceSnapshot snapshot, int fanIndex, Vec3f normal) {
   }

   private static final class TransformFaceSnapshot {
      private final LittleRenderBoxTransformable owner;
      private final Facing facing;
      private final List<VectorFan> fans;
      private final float scale;
      private final boolean[] removed;

      private TransformFaceSnapshot(LittleRenderBoxTransformable owner, Facing facing, List<VectorFan> fans, float scale) {
         this.owner = owner;
         this.facing = facing;
         this.fans = fans;
         this.scale = scale;
         this.removed = new boolean[fans.size()];
      }
   }

   private record VolumeCellKey(int x, int y, int z) {
   }
}
