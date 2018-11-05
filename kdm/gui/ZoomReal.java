package kdm.gui;

/** continuous zoom (i.e., zoom value is a real number) */
public class ZoomReal extends ZoomMethod
{
   protected Graph graph;
   protected double zoom = 1.0;
   protected double zf;

   public ZoomReal(Graph graph)
   {
      this(graph, 1.5);      
   }
   
   public ZoomReal(Graph graph, double zoomFactor)
   {
      this.graph = graph;
      zf = zoomFactor; 
   }
   
   @Override
   public void reset()
   {
      zoom = 1.0;
   }
   
   @Override
   public void fit(int w)
   {
      zoom = (double)w / Math.max(1, graph.getWidth());
   }
   
   @Override
   public boolean zoomIn()
   {
      zoom *= zf;
      return true;
   }

   @Override
   public boolean zoomOut()
   {
      if (zoom <= 1.0) return false;
      zoom = Math.max(1.0, zoom/zf);
      return true;
   }

   @Override
   public double getZoom()
   {
      return zoom;
   }   
   
   @Override
   public long sb2graph(int x)
   {
      return x*100L;
   }
      
   @Override
   public int graph2sb(long x)
   {
      return (int)(x/100L);
   }
   
   @Override
   public int getSBExtent(long w)
   {
      return (int)Math.round(graph2sb(w)/zoom);
   }

   @Override
   public int calcVWidth(int w)
   {
      return (int)Math.round(w * zoom);
   }
}
