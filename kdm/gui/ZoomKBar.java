package kdm.gui;

/** zoom always allows an integral number of bars */
public class ZoomKBar extends ZoomMethod
{
   protected LinearFrameGraph graph;
   protected int zoom = 1;
   
   public ZoomKBar(LinearFrameGraph graph)
   {
      this.graph = graph;
   }

   @Override
   public void reset()
   {
      zoom = 1;
   }
   
   @Override
   public void fit(int w)
   {
      // TODO
      assert false : "not yet implemented";
   }
   
   @Override
   public int getSBExtent(long w)
   {
      return graph.getNumVizFrames();
   }

   @Override
   public double getZoom()
   {
      System.err.printf("zoom: %d\n", zoom);
      return zoom;
   }

   @Override
   public int graph2sb(long x)
   {
      return (int)x;
   }

   @Override
   public long sb2graph(int x)
   {
      return x;
   }

   @Override
   public boolean zoomIn()
   {
      zoom--;
      if (zoom >= 1) return true;
      zoom = 1;
      return false;
   }

   @Override
   public boolean zoomOut()
   {
      zoom++;
      int N = graph.getNumFrames();
      if (zoom <= N) return true;
      zoom = N;
      return false;
   }

   @Override
   public int calcVWidth(int w)
   {
      int hgap = graph.getBarGap();
      int hbar = graph.getBarWidth();
      return hgap + (hgap+hbar)*graph.getNumFrames()/zoom;
   }


}
