package kdm.gui;

/** abstract base class for all zoom methods */
public abstract class ZoomMethod
{
   public abstract boolean zoomIn();
   public abstract boolean zoomOut();
   public abstract double getZoom();
   
   /** @return virtual width for the given component width and current zoom level */
   public abstract int calcVWidth(int w);
   
   /** change zoom so that graph is w pixels wide */
   public abstract void fit(int w);
 
   /** @return graph time/index from scrollbar location */
   public abstract long sb2graph(int x);
   
   /** @return scrollbar location from graph time/index */
   public abstract int graph2sb(long x);
   
   /** Reset the zoom data (e.g., in response to data change) */
   public abstract void reset();
   
   /**
    * Compute the scrollbar extent from the graph width
    * @param w width of the graph (may be ms or frames, etc.)
    * @return scrollbar extent
    */
   public abstract int getSBExtent(long w);
   
   public boolean canZoomIn(){ return true; }
   public boolean canZoomOut(){ return true; }
}
