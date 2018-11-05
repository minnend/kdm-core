package kdm.gui;

import kdm.data.*;
import kdm.io.*;
import kdm.io.Def.*;
import kdm.util.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import static javax.swing.SwingConstants.*;
import static kdm.gui.GridFlexLayout.*;

/**
 * This component holds a set of graphs along with the time legend, event bar, scroll bars, and zoom buttons.
 */
public class GraphScrollPane extends JPanel implements GraphListener, ActionListener, AdjustmentListener,
      ComponentListener
{
   protected final static boolean bSlider = false; // TODO

   /** scrollbar for panning across time, each unit is 100ms */
   protected JScrollBar hsb;
   protected JDirSlider slider;

   protected JPanel pgcont, gpanel;
   protected JButton btZoomIn, btZoomOut;
   protected TimeLegend timeLegend;
   protected long start, end;
   protected int vwidth = 0;
   protected Vector<GraphListener> gListeners;
   protected Vector<Graph> otherGraphs;
   protected GraphComplexLite[] gcl;
   protected Graph masterGraph;
   protected ZoomMethod zoom;
   protected boolean bRedrawing = false;

   protected GraphScrollPane(GridFlexLayout gfl, GraphComplexLite[] gcl, boolean bControls, boolean bZoom,
         Graph masterGraph)
   {
      super(new BorderLayout());

      this.gcl = gcl;
      this.masterGraph = masterGraph;
      start = end = 0;
      gListeners = new Vector<GraphListener>();
      otherGraphs = new Vector<Graph>();

      // setup the scrollbar
      if (bControls)
      {
         hsb = new JScrollBar(JScrollBar.HORIZONTAL, 0, 0, 0, 0);
         hsb.addAdjustmentListener(this);

         slider = new JDirSlider(0, 0, 0, 0, JDirSlider.HORIZONTAL);
         slider.addAdjustmentListener(this);

         if (bZoom)
         {
            // setup the zoom buttons
            btZoomOut = new JButton("-");
            btZoomOut.setMargin(new Insets(0, 4, 0, 4));
            btZoomOut.setToolTipText("Zoom Out");
            btZoomOut.addActionListener(this);
            btZoomIn = new JButton("+");
            btZoomIn.setMargin(new Insets(0, 4, 0, 4));
            btZoomIn.setToolTipText("Zoom In");
            btZoomIn.addActionListener(this);
         }
      }

      // create the graph panel and box
      pgcont = new JPanel();
      pgcont.setLayout(new BorderLayout());
      gpanel = new JPanel();
      gpanel.setBackground(Color.lightGray);
      if (gfl != null) gpanel.setLayout(gfl);
      else gpanel.setLayout(new BoxLayout(gpanel, BoxLayout.Y_AXIS));
      JScrollPane vsp = new JScrollPane(gpanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
      pgcont.add(vsp, BorderLayout.CENTER);

      if (bControls)
      {
         // create the scroll / zoom panel
         JPanel sp = new JPanel();
         sp.setLayout(new BorderLayout());
         sp.add(bSlider ? slider : hsb, BorderLayout.CENTER);
         JPanel p = new JPanel();
         if (bZoom)
         {
            FlowLayout flow = new FlowLayout(FlowLayout.RIGHT);
            flow.setHgap(2);
            flow.setVgap(1);
            p.setLayout(flow);
            p.add(btZoomOut);
            p.add(btZoomIn);
            sp.add(p, BorderLayout.EAST);
         }
         add(sp, BorderLayout.SOUTH);
      }

      // add the components to this container
      add(pgcont, BorderLayout.CENTER);

      addComponentListener(this);
   }

   public void addOtherGraph(Graph g)
   {
      if (!otherGraphs.contains(g))
      {
         g.setShowAll(false);
         g.setVirtualWidth(vwidth);
         otherGraphs.add(g);
      }
   }

   public void removeOtherGraph(Graph g)
   {
      otherGraphs.remove(g);
   }

   public void clearOtherGraphs()
   {
      otherGraphs.clear();
   }

   public void addEventBar(EventBar eb)
   {
      if (!gListeners.contains(eb)) gListeners.add(eb);
   }

   public void addGraphListener(GraphListener gl)
   {
      if (gListeners.contains(gl)) return;
      if (gl instanceof EventBar) addEventBar((EventBar)gl);
      else gListeners.add(gl);
   }

   /**
    * @return layout based on the list of component definitions
    */
   public static GridFlexLayout constructGFL(ArrayList<DefComp> compdef)
   {
      int nGraphs = 0;
      for(DefComp comp : compdef)
         if (comp.location == DefComp.Location.Graph) nGraphs++;

      GridFlexLayout gfl = new GridFlexLayout(nGraphs, 1, 0, 1);

      int iRow = 0;
      for(DefComp comp : compdef)
      {
         if (comp.location != DefComp.Location.Graph) continue;
         if (comp.bAbsHeight)
         {
            if (comp.height < 0) continue;
            gfl.setRow(iRow, Style.fixed, comp.height);
         }
         else gfl.setRow(iRow, Style.fill, (float)comp.height / 100.0f);
         iRow++;
      }

      return gfl;
   }

   /** update the start/end time of this scroll pane based on the current state of the graphs */
   public void updateTimes()
   {
      start = Long.MAX_VALUE;
      end = Long.MIN_VALUE;
      for(int i = 0; i < gcl.length; i++)
      {
         if (gcl[i].getGraph().getData() == null) continue;
         start = Math.min(start, gcl[i].getGraph().getStart());
         end = Math.max(end, gcl[i].getGraph().getEnd());
      }

      // set the new time bounds
      for(int i = 0; i < gcl.length; i++)
         gcl[i].getGraph().setBounds(start, end);

      updateScrollbar();
   }

   /**
    * Create a GSP for the given graphs
    * 
    * @param gcl graph to display
    * @param bTimeLegend display a time legend?
    * @param bControls display a scrollbar and zoom buttons?
    * @param bZoom display zoom buttons
    * @param gfl layout to use (if null, one will be generated)
    * @param zoom zoom behavior
    * @return true if successful
    */
   public static GraphScrollPane construct(GraphComplexLite gcl, boolean bTimeLegend, boolean bControls,
         boolean bZoom, GridFlexLayout gfl, ZoomMethod zoom)
   {
      return construct(new GraphComplexLite[] { gcl }, gcl, bTimeLegend, bControls, bZoom, gfl, zoom);
   }

   /**
    * Create a GSP for the given graphs
    * 
    * @param gcl graphs to display
    * @param bTimeLegend display a time legend?
    * @param bControls display a scrollbar and zoom buttons?
    * @param bZoom display zoom buttons
    * @param gfl layout to use (if null, one will be generated)
    * @param zoom zoom behavior, null => no zoom buttons
    * @return true if successful
    */
   public static GraphScrollPane construct(GraphComplexLite[] gcl, Graph masterGraph, boolean bTimeLegend,
         boolean bControls, boolean bZoom, GridFlexLayout gfl, ZoomMethod zoom)
   {
      if (gfl == null)
      {
         gfl = new GridFlexLayout(gcl.length, 1);
         gfl.setRows(Style.fill);
      }

      GraphScrollPane gsp = new GraphScrollPane(gfl, gcl, bControls, bZoom, masterGraph);
      gsp.zoom = zoom;
      gsp.setBackground(new Color(210, 230, 250));
      if (bTimeLegend)
      {
         gsp.timeLegend = new TimeLegend(masterGraph);
         gsp.addBorderComponent(gsp.timeLegend, BorderLayout.NORTH);
         gsp.timeLegend.setBorder(BorderFactory
               .createMatteBorder(0, 1, 0, 0, gsp.timeLegend.getBackground()));
      }

      // setup and add all of the components
      for(int i = 0; i < gcl.length; i++)
      {
         gcl[i].setShowAll(false);
         gsp.add(gcl[i]);
         if (bTimeLegend) gsp.timeLegend.addGraph(gcl[i]);
      }
      gsp.updateTimes();
      return gsp;
   }

   public TimeLegend getTimeLegend()
   {
      return timeLegend;
   }

   /**
    * update the scrollbars with the current scroll pane time; note: don't call this directly, it is called
    * automatically by updateTimes()
    */
   protected void updateScrollbar()
   {
      int sbEnd = zoom.graph2sb(end - start);
      int extent = (int)Math.round(zoom.getSBExtent(end - start));

      if (slider == null && hsb == null) return;
      int v = bSlider ? slider.getValue() : hsb.getValue();
      v = Math.max(0, Math.min(v, sbEnd - extent));

      if (bSlider) slider.setMinMax(0, sbEnd, extent);
      else
      {
         hsb.setUnitIncrement(Math.max(1, extent / 20));
         hsb.setBlockIncrement(Math.max(10, extent / 4));
         hsb.setValues(v, extent, 0, sbEnd - 1);
      }
   }

   public long getStart()
   {
      return start;
   }

   public long getEnd()
   {
      return end;
   }

   public int getVWidth()
   {
      return vwidth;
   }

   public void addBorderComponent(JComponent comp, String border)
   {
      comp.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, comp.getBackground()));
      pgcont.add(comp, border);
   }

   public void removeBorderComponent(Component comp)
   {
      pgcont.remove(comp);
   }

   public Component add(Component comp)
   {
      if (comp instanceof Graph)
      {
         ((Graph)comp).addGraphListener(this);
         return gpanel.add(comp);
      }
      else return super.add(comp);
   }

   public Component add(Component comp, int index)
   {
      if (comp instanceof Graph)
      {
         ((Graph)comp).addGraphListener(this);
         return gpanel.add(comp, index);
      }
      else return super.add(comp, index);
   }

   public void graphMouseMoved(Graph graph, TimeX timex, ValueY valuey)
   {
      Component[] gkids = gpanel.getComponents();
      for(Component kid : gkids)
      {
         if (kid instanceof Graph)
         {
            Graph g = ((Graph)kid).getGraph();
            if (g != graph) g.setTime(timex);
         }
      }
      for(GraphListener gl : gListeners)
         gl.graphMouseMoved(graph, timex, valuey);
   }
   
   public void graphDataChanged(Graph graph)
   {
      // nothing to do
   }

   public void graphVizChanged(Graph _graph)
   {
      if (bRedrawing) return;
      bRedrawing = true;

      int w = (masterGraph != null ? masterGraph.getGraph().getWidth() : 0);
      vwidth = zoom.calcVWidth(w);

      // enable/disable the zoom buttons as appropriate
      if (btZoomOut != null)
      {
         btZoomOut.setEnabled(zoom.canZoomOut());
         btZoomIn.setEnabled(zoom.canZoomIn());
      }

      // notify child graphs of the change
      for(Component kid : gpanel.getComponents())
      {
         if (!(kid instanceof Graph)) continue;
         Graph graph = ((Graph)kid).getGraph();
         graph.setVirtualWidth(vwidth);
      }

      // notify other graphs of the change
      for(Graph g : otherGraphs)
         g.setVirtualWidth(vwidth);

      // may need to recalc sb info (e.g., extent)
      updateTimes();
      bRedrawing = false;
   }

   public void actionPerformed(ActionEvent e)
   {
      Object src = e.getSource();
      Component[] gkids = gpanel.getComponents();
      if (btZoomOut == src)
      {
         zoom.zoomOut();
         updateScrollbar();
         graphVizChanged(null);

      }
      else if (btZoomIn == src)
      {
         zoom.zoomIn();
         updateScrollbar();
         graphVizChanged(null);
      }
   }

   public void adjustmentValueChanged(AdjustmentEvent e)
   {
      // notify graphs of the change
      long ixms = zoom.sb2graph(bSlider ? slider.getValue() : hsb.getValue());
      for(Component kid : gpanel.getComponents())
      {
         if (!(kid instanceof Graph)) continue;
         Graph graph = ((Graph)kid).getGraph();
         graph.setOffset(ixms);
      }

      // notify other graphs as well
      for(Graph g : otherGraphs)
         g.setOffset(ixms);
   }

   public void componentHidden(ComponentEvent e)
   {}

   public void componentMoved(ComponentEvent e)
   {}

   public void componentResized(ComponentEvent e)
   {
      // if this scroll pane changes, we need to update the vwidth of child graphs
      graphVizChanged(null);
   }

   public void componentShown(ComponentEvent e)
   {}
}
