package dev.DraGonFiirE.littleperformanceimprovements.client.render;

public final class PreviewFanTriangulation implements AutoCloseable {
   private static final ThreadLocal<PreviewFanTriangulation.State> STATE = ThreadLocal.withInitial(PreviewFanTriangulation.State::new);
   private final boolean facingMarkers;

   private PreviewFanTriangulation(boolean facingMarkers) {
      this.facingMarkers = facingMarkers;
   }

   public static PreviewFanTriangulation activate() {
      PreviewFanTriangulation.State state = STATE.get();
      state.depth++;
      return new PreviewFanTriangulation(false);
   }

   public static PreviewFanTriangulation activateFacingMarkers() {
      PreviewFanTriangulation.State state = STATE.get();
      state.depth++;
      state.facingMarkerDepth++;
      return new PreviewFanTriangulation(true);
   }

   public static boolean active() {
      PreviewFanTriangulation.State state = STATE.get();
      return state.depth > 0 || state.workerMeshActive;
   }

   public static void workerMeshActive(boolean active) {
      PreviewFanTriangulation.State state = STATE.get();
      state.workerMeshActive = active;
      if (!active && state.depth <= 0) {
         STATE.remove();
      }
   }

   public static boolean workerMeshActive() {
      return STATE.get().workerMeshActive;
   }

   public static boolean facingMarkersActive() {
      return STATE.get().facingMarkerDepth > 0;
   }

   public static void currentFacing(int ordinal) {
      if (active()) {
         STATE.get().currentFacing = ordinal;
      }
   }

   public static int currentFacing() {
      return STATE.get().currentFacing;
   }

   @Override
   public void close() {
      PreviewFanTriangulation.State state = STATE.get();
      if (this.facingMarkers) {
         state.facingMarkerDepth--;
      }

      state.depth--;
      if (state.depth <= 0 && !state.workerMeshActive) {
         STATE.remove();
      }
   }

   private static final class State {
      private int depth;
      private int facingMarkerDepth;
      private int currentFacing = -1;
      private boolean workerMeshActive;
   }
}
