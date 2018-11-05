package kdm.gui;

import kdm.data.*;
import kdm.util.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.util.*;
import java.io.*;

/**
 * Abstract base class of all GUI graph components
 */
public abstract class Graph extends JMyComponent implements ComponentListener, MouseMotionListener,
      Configurable
{
   /** color to draw mouse location lines (eg, for time and value) */
   protected Color colorMouse = Library.makeGray(160);

   /** the dataset vizualized by this graph */
   protected Sequence data;

   /** some objects will want to know when the graph vizualization changes */
   protected ArrayList<GraphListener> glist;

   /** always show all data? */
   protected boolean bShowAll = true;

   /** do we need to redraw the display (vs. restore from buffer)? */
   protected boolean bRedraw = true;

   /** Override the bRedraw signal (used by subclasses) */
   protected boolean bWaitRedraw = false;

   /** title of this graph */
   protected String graphName;

   /** virtual width of component in pixels (affects zoom) */
   protected int vgraphw;

   /** if true, all dims are displayed with standard colors (from Library.generateColors) */
   protected boolean bAutoColor = true;

   /** color of graph for each dimension (null to omit) */
   protected Color[] dimColor;

   /** list of areas to highlight */
   protected transient ArrayList<TimeMarker> highlights;

   // //////////////////////////////////////////////////////////

   public Graph()
   {
      this(null);
   }

   public Graph(Sequence data)
   {
      setOpaque(true);
      glist = new ArrayList<GraphListener>();
      highlights = new ArrayList<TimeMarker>();
      addComponentListener(this);
      if (!(this instanceof GraphWrapper)){
         // mouse listener added by JMyComponent
         addMouseMotionListener(this);
         setData(data);
      }
   }

   public void setData(Sequence _data)
   {
      data = _data;
      if (bAutoColor){
         if (data != null) dimColor = Library.generateColors(data.getNumDims());
         else dimColor = null;
      }
      bRedraw = true;
      highlights.clear();
      fireDataChanged();
      fireGraphVizChanged();
      repaint();
   }

   public String getGraphName()
   {
      return graphName;
   }

   public void setGraphName(String graphName)
   {
      this.graphName = graphName;
   }

   public int getNumFrames()
   {
      return data.length();
   }

   /** @return units used by this graph (time or frames) */
   public abstract TimeMarker.Units getUnits();

   /**
    * @return start time (ms or index) of data represented by this graph; return value should match either
    *         getStartTime or getStartIndex
    */
   public abstract long getStart();

   /** @return start time (ms) of data represented by this graph */
   public abstract long getStartTime();

   /** @return start index (frame number) of data represented by this graph */
   public long getStartIndex()
   {
      return 0;
   }

   /** @return end time (ms or index) of data represented by this graph */
   public abstract long getEnd();

   /** change the offset that controls which portion of the data is displayed */
   public abstract void setOffset(long offset);

   /**
    * Set new bounds for this graph
    * 
    * @param start start time in ms since epoch
    * @param end end time in ms
    * @return true if successfully changed
    */
   public abstract boolean setBounds(long start, long end);

   public void setAutoColors(boolean b)
   {
      if (b == bAutoColor) return;
      bAutoColor = b;
      if (bAutoColor){
         if (data != null) dimColor = Library.generateColors(data.getNumDims());
         else dimColor = null;
         repaint();
      }
   }

   public void clearDimColors()
   {
      bAutoColor = false;
      if (data == null) dimColor = null;
      else dimColor = new Color[data.getNumDims()];
   }

   public void setDimColor(int i, Color color)
   {      
      if (bAutoColor) clearDimColors();      
      if (dimColor == null || i >= dimColor.length){
         Color[] dc = dimColor;
         dimColor = new Color[Math.max(i + 1, data==null?0:data.getNumDims())];
         if (!bAutoColor && dc!=null) Library.copy(dc, dimColor, 0, 0, Math.min(dc.length, dimColor.length));
      }
      dimColor[i] = color;      
      bAutoColor = false;
      repaint();
   }

   public void setDimColor(Color[] colors)
   {      
      if (dimColor == null)
         dimColor = new Color[Math.max(colors.length, data==null?0:data.getNumDims())];         
      int i, n = (int)Math.min(colors.length, dimColor.length);      
      for(i = 0; i < n; i++)
         dimColor[i] = colors[i];
      for(; i < dimColor.length; i++)
         dimColor[i] = null;
      bAutoColor = false;      
      repaint();
   }

   /**
    * Set the time step so that the graph requires the given number of pixels
    * 
    * @param w width of graph after adjustment
    * @return true if time step changes
    */
   public boolean setVirtualWidth(int w)
   {
      if (w == vgraphw) return false;
      vgraphw = w;
      fireGraphVizChanged();
      repaint();
      return true;
   }

   public int getVirtualWidth()
   {
      return vgraphw;
   }

   @Override
   public JPopupMenu buildPopup(boolean bAppend)
   {
      JPopupMenu menu = new JPopupMenu();
      JMenuItem mi = menu.add(graphName != null ? graphName : "Unknown");
      mi.setEnabled(false);
      if (bAppend) appendPopup(menu);
      return menu;
   }

   public void setShowAll(boolean b)
   {
      bShowAll = b;
      if (bShowAll) vgraphw = getWidth();
      repaint();
   }

   public boolean config(ConfigHelper chelp, String sKey, String sVal)
   {
      return false;
   }
   
   /** Called by config manager after configuration has completed */
   public boolean finalSetup()
   {
      return true;
   }

   public boolean config(File fBase, String s)
   {
      // grrr: no multiple inheritance in java
      ConfigHelper chelp = new ConfigHelper(fBase);
      return chelp.config(s, this);
   }

   public void addGraphListener(GraphListener gl)
   {
      if (!getGraph().glist.contains(gl)) getGraph().glist.add(gl);
   }

   public void removeGraphListener(GraphListener gl)
   {
      getGraph().glist.remove(gl);
   }

   public Graph getGraph()
   {
      return this;
   }

   public Sequence getData()
   {
      return data;
   }

   public abstract ArrayList<TimeX> getGridTimes();

   public abstract ArrayList<ValueY> getGridValues();

   public abstract long getNumVisibleMS();

   public abstract void setTime(TimeX tx);

   public TimeX getTimeX(int x)
   {
      long time = getTimeFromX(x);
      return new TimeX(x, getIndexFromX(x, false), time - getTimeFromX(0), time);
   }

   /** @return x coordinate of the given time in ms */
   public abstract int getXFromTime(long ms);

   /** @return x coordinate of the given data frame */
   public abstract int getXFromIndex(int ix);

   /** @return time index that corresponds to the given x coordinate */
   public abstract long getTimeFromX(int x);

   /** @return (nearest) data index to the given x coordinate */
   public abstract int getIndexFromX(int x, boolean bClip);

   public void highlight(TimeMarker tm)
   {
      clearHighlights();
      addHighlight(tm);
   }

   public void addHighlight(TimeMarker tm)
   {
      if (tm == null) return;
      bRedraw = true;
      highlights.add(tm);
      repaint();
   }

   public void clearHighlights()
   {
      if (!highlights.isEmpty()){
         bRedraw = true;
         highlights.clear();
         repaint();
      }
   }

   public void mouseDragged(MouseEvent e)
   {
      mouseMoved(e);
   }

   public void mouseMoved(MouseEvent e)
   {}

   public void componentHidden(ComponentEvent e)
   {}

   public void componentMoved(ComponentEvent e)
   {}

   public void componentShown(ComponentEvent e)
   {}

   public void componentResized(ComponentEvent e)
   {
      // no need to notify & repaint wrappers
      if (getGraph() == this){
         if (bShowAll) vgraphw = getWidth();
         fireGraphVizChanged();
         repaint();
      }
   }

   public void fireGraphMouseMoved(TimeX mouseTime, ValueY mouseValue)
   {
      for(GraphListener gl : glist)
         gl.graphMouseMoved(this, mouseTime, mouseValue);
   }

   protected void fireGraphVizChanged()
   {
      bRedraw = true;
      for(GraphListener gl : glist)
         gl.graphVizChanged(this);
   }

   protected void fireDataChanged()
   {
      for(GraphListener gl : glist)
         gl.graphDataChanged(this);
   }

   /**
    * If this graph is displayed in a separate frame (e.g., a map), then the JPanel returned here will be
    * placed below the graph in the frame.
    * 
    * @return panel to place below graph in the external frame
    */
   public JPanel getLowerFramePanel()
   {
      return null;
   }
}
