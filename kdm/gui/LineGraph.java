package kdm.gui;

import kdm.data.*;
import kdm.io.DateExtracter;
import kdm.util.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.geom.*;

import javax.swing.*;

import java.util.*;
import java.text.*;
import java.io.*;

/**
 * A line graph that can represent multiple series.
 */
public class LineGraph extends LinearTimeGraph implements ActionListener
{
   public static final int PREF_HEIGHT = 400;

   public static final String sFixedScale = "Fixed Scale";
   public static final String sCrosshairs = "Crosshairs";
   public static final String sAntiAlias = "Anti-Alias";

   protected double valueBase; // value corresponding to y=0
   protected double valueStep; // value represented by one vertical pixel
   protected int nVGridSpace = 32; // approx. number of pixels between horizontal grid lines
   protected double gridVScale; // total value between horizontal grid lines

   protected double globalMin = Double.NaN;
   protected double globalMax = Double.NaN;

   protected Color colorGrid;
   protected Color colorHighlight;
   protected BufferedImage buf;

   protected boolean bRenderVerticalGridLines = true;
   protected boolean bRenderHorizontalGridLines = true;
   protected boolean bRenderMouseOverFrame = true;
   protected boolean bRenderMouseOverData = true;
   protected boolean bRenderMouseOverCrossHair = true;

   protected boolean bFixedScale = false;
   protected boolean bAntiAlias = false;

   protected ArrayList<VertMarker> vertMarkers;
   protected ArrayList<HorzMarker> horzMarkers;

   public LineGraph()
   {
      this(null);
      setOpaque(true);
   }

   public LineGraph(Sequence data)
   {
      super(data);
      setOpaque(true);

      vertMarkers = new ArrayList<VertMarker>();
      horzMarkers = new ArrayList<HorzMarker>();

      // for printing
      // setBackground(Color.white);
      // colorGrid = Library.makeGray(240);

      // for screen display
      setBackground(Color.black);
      colorGrid = Library.makeGray(60);

      setForeground(Color.yellow);
      colorMouse = Color.lightGray;
      colorHighlight = new Color(180, 190, 220);

      setData(data);
   }

   public void setVGridSpace(int v)
   {
      nVGridSpace = v;
      bRedraw = true;
      repaint();
   }

   public void setFixedScale(boolean b)
   {
      if (b != bFixedScale){
         bFixedScale = b;
         bRedraw = true;
         repaint();
      }
   }

   public void setData(Sequence data)
   {
      bWaitRedraw = true;
      super.setData(data);
      bWaitRedraw = false;

      calcVScale();
      calcHScale();
      repaint();
      fireDataChanged();
   }

   public void setRenderGridLines(boolean bVert, boolean bHorz)
   {
      bRenderHorizontalGridLines = bHorz;
      bRenderVerticalGridLines = bVert;
      repaint();
   }

   public void setRenderMouseInfo(boolean bCross, boolean bFrame, boolean bData)
   {
      bRenderMouseOverCrossHair = bCross;
      bRenderMouseOverFrame = bFrame;
      bRenderMouseOverData = bData;
      repaint();
   }

   public JPopupMenu appendPopup(JPopupMenu menu)
   {
      menu.addSeparator();

      JCheckBoxMenuItem mi = new JCheckBoxMenuItem(sFixedScale, bFixedScale);
      mi.addActionListener(this);
      menu.add(mi);

      mi = new JCheckBoxMenuItem(sCrosshairs, bRenderMouseOverCrossHair);
      mi.addActionListener(this);
      menu.add(mi);

      mi = new JCheckBoxMenuItem(sAntiAlias, bAntiAlias);
      mi.addActionListener(this);
      menu.add(mi);

      return menu;
   }

   public int getYFromValue(double value)
   {
      int y = (int)Math.round((value - valueBase) / valueStep);
      return getHeight() - y;
   }

   public int getYFromIndex(int ix, int d)
   {
      return getYFromValue(data.get(ix).get(d));
   }

   public Point getGraphPoint(int ix, int d, double timeStep)
   {
      long ms = data.getTimeMS(ix);
      int x = getXFromTime(ms, timeStep);
      int y = (int)Math.round((data.get(ix).get(d) - valueBase) / valueStep);
      y = getHeight() - y;
      return new Point(x, y);
   }

   /**
    * Returns a ValueY structure representing the value at the given y coordinate.
    */
   protected ValueY getValueY(int y)
   {
      double v = valueBase + valueStep * (getHeight() - y);
      return new ValueY(y, v);
   }

   /**
    * Computes the locations of each horizontal (value axis) grid line including the screen coordinates
    * (relative to 0) and the actual value at that level.
    */
   public ArrayList<ValueY> getGridValues()
   {
      if (gridVScale < 1e-9) calcVScale();
      ArrayList<ValueY> grid = new ArrayList<ValueY>();
      double v = Math.ceil(valueBase / gridVScale) * gridVScale;
      Dimension dims = getSize();

      while(true){
         int y = (int)Math.round((v - valueBase) / valueStep);
         if (y > dims.height) break;
         grid.add(new ValueY(y, v));
         v += gridVScale;
         if (grid.size() > 100){
            System.err.printf("Error: too many horizontal grid lines (%d)\n y=%d  height=%d  v=%f  gridVScale=%f  base=%f  step=%f\n",
                  grid.size(), y, dims.height, v, gridVScale, valueBase, valueStep);
            assert false;
            grid.clear();
            break;
         }
      }

      return grid;
   }

   /**
    * Uses screen coordinates returned by getGridTimes to draw the actual vertical lines
    */
   protected void renderGrid(Graphics g)
   {
      g.setColor(colorGrid);
      Dimension dims = getSize();

      // render vertical lines
      if (bRenderVerticalGridLines){
         ArrayList<TimeX> gridx = getGridTimes();
         for(TimeX tx : gridx){
            if (tx == null) continue;
            g.drawLine(tx.x, 0, tx.x, dims.height);
         }
      }

      // render horizontal lines
      if (bRenderHorizontalGridLines){
         ArrayList<ValueY> gridy = getGridValues();
         for(ValueY vy : gridy){
            int y = dims.height - vy.y;
            g.drawLine(0, y, dims.width, y);
         }
      }
   }

   /**
    * Draws the actual graph, including grid lines
    */
   @Override
   public void paintComponent(Graphics2D g, int cw, int ch)
   {
      double timeStep = calcTimeStep();

      if (buf == null || (bRedraw && !bWaitRedraw)){
         calcVScale();
         if (buf == null) buf = Library.getGC().createCompatibleImage(cw, ch);
         Graphics2D gb = buf.createGraphics();

         // clear the background
         gb.setColor(getBackground());
         gb.fillRect(0, 0, cw, ch);

         // render the highlight if we have one
         if (data != null && highlights.size() > 0){
            gb.setColor(colorHighlight);
            for(TimeMarker highlight : highlights){
               if (highlight.isIndex()){
                  int a, b;
                  if (highlight.getStartIndex() < data.length()) a = getGraphPoint(
                        highlight.getStartIndex(), 0, timeStep).x;
                  else a = getGraphPoint(data.length() - 1, 0, timeStep).x + 1;
                  if (highlight.getStopIndex() < data.length()) b = getGraphPoint(highlight.getStopIndex(),
                        0, timeStep).x;
                  else b = getGraphPoint(data.length() - 1, 0, timeStep).x + 1;
                  gb.fillRect(a, 0, b - a + 1, ch);
               }
               else{
                  int a = getXFromTime(highlight.getStartTime(), timeStep);
                  int b = getXFromTime(highlight.getStopTime(), timeStep);
                  gb.fillRect(a, 0, b - a + 1, ch);
               }
            }
         }

         // render the grid and mouse info
         renderGrid(gb);

         // render the markers (horz/vert lines on graph)
         renderVertMarkers(gb);
         renderHorzMarkers(gb);

         // render the graph
         renderData(gb);

         // we no longer need to redraw the buffer
         bRedraw = false;
         gb.dispose();
      }
      g.drawImage(buf, 0, 0, null);

      // render the volatile mouse info
      if (bRenderMouseOverCrossHair && mouseValue != null){
         g.setColor(colorMouse);
         g.drawLine(0, mouseValue.y, cw, mouseValue.y);
      }

      if (mouseTime != null){
         if (bRenderMouseOverCrossHair){
            g.setColor(colorMouse);
            g.drawLine(mouseTime.x, 0, mouseTime.x, ch);
         }

         if (data != null){
            // render value(s) at this point
            if (mouseTime.index >= 0 && mouseTime.index < data.length()){
               FontMetrics fm = g.getFontMetrics();
               int sh = fm.getHeight();

               if (bRenderMouseOverData){
                  FeatureVec fv = data.get(mouseTime.index);
                  Vector<StringColorWidth> vs = new Vector<StringColorWidth>();
                  int sw = 0;
                  for(int i = 0; i < fv.getNumDims(); i++){
                     if (dimColor[i] == null) continue;
                     String s = PrettyPrint.printDouble("%.6f", fv.get(i));
                     int w = fm.stringWidth(s);
                     vs.add(new StringColorWidth(s, dimColor[i], w));
                     if (w > sw) sw = w;
                  }

                  sw += 4;
                  int h = vs.size() * sh + 2;
                  g.setColor(getBackground());
                  int x = cw - sw - 6;
                  int y = ch - h - 4;
                  if (mouseTime != null && mouseValue != null && mouseTime.x > x - 12
                        && mouseValue.y > y - 16) y = 4;
                  g.fillRect(x, y, sw, h);
                  g.setColor(Color.darkGray);
                  g.drawRect(x, y, sw, h);
                  for(int i = 0; i < vs.size(); i++){
                     StringColorWidth scw = vs.get(i);
                     g.setColor(scw.color);
                     g.drawString(scw.s, x + (sw - scw.w) / 2, y - 2 + sh * (i + 1));
                  }
               }

               // render the frame number in the bottom-left
               if (bRenderMouseOverFrame){
                  String s = "" + mouseTime.index;
                  int sw = fm.stringWidth(s);
                  g.setColor(getBackground());
                  g.fillRect(2, ch - sh - 4, sw + 4, sh);
                  g.setColor(Color.darkGray);
                  g.drawRect(2, ch - sh - 4, sw + 4, sh);
                  g.setColor(Color.lightGray);
                  g.drawString(s, 4, ch - 7);
               }
            }
         }
      }
   }

   /** Render the vertical markers in this component */
   protected void renderVertMarkers(Graphics2D g)
   {
      if (vgraphw == 0) return;
      double timeStep = calcTimeStep();
      Dimension dim = getSize();
      Stroke stroke = g.getStroke();
      synchronized (vertMarkers){
         for(VertMarker vm : vertMarkers){
            int x = getXFromTime(vm.ms, timeStep);
            if (x < 0 || x >= dim.width) continue;

            g.setColor(vm.color);
            g.setStroke(vm.stroke);
            g.drawLine(x, 0, x, dim.height);
         }
      }
      g.setStroke(stroke);
   }

   /** Render the horizontal markers in this component */
   protected void renderHorzMarkers(Graphics2D g)
   {
      if (vgraphw == 0) return;
      Dimension dim = getSize();
      Stroke stroke = g.getStroke();
      for(HorzMarker hm : horzMarkers){
         int y = getYFromValue(hm.value);
         if (y < 0 || y >= dim.height) continue;

         g.setColor(hm.color);
         g.setStroke(hm.stroke);
         g.drawLine(0, y, dim.width, y);
      }
      g.setStroke(stroke);
   }

   /** Render the graph data in this component using the given graphics object */
   protected void renderData(Graphics2D g)
   {
      if (data == null || vgraphw == 0) return;

      if (bAntiAlias) Library.setAntiAlias(g, true);

      int w = getWidth();
      int nFrames = data.length();
      int iVizStart = Math.max(getIndexFromX(0, false) - 1, 0);
      int nd = data.getNumDims();
      double timeStep = calcTimeStep();

      int[] by = new int[nd];
      int bx = getXFromIndex(iVizStart, timeStep);
      int nDimViz = 0;
      for(int d = 0; d < nd; d++)
         if (dimColor[d] != null){
            nDimViz++;
            by[d] = getYFromIndex(iVizStart, d);
         }
      if (nDimViz == 0){
         System.err.printf("Warning: no dimensions are being visualized!\n");
         return;
      }

      // now process the rest of the points
      MyIntList xlist = new MyIntList();
      ArrayList<int[]> ylist = new ArrayList<int[]>();
      xlist.add(bx);
      ylist.add(by.clone());

      int ix = iVizStart + 1;
      while(ix < nFrames && bx < w){
         int ax = bx;
         int ixBStart = ix;
         bx = getXFromIndex(ix++, timeStep);
         while(ix < nFrames && bx == ax)
            bx = getXFromIndex(ix++, timeStep);

         // calc y value of each dimension
         for(int d = 0; d < nd; d++){
            if (dimColor[d] == null) continue;
            if (ix - ixBStart > 1){
               int ymin, ymax;
               ymin = ymax = getYFromIndex(ixBStart, d);
               for(int i = ixBStart + 1; i < ix; i++){
                  int y = getYFromIndex(i, d);
                  if (y < ymin) ymin = y;
                  else if (y > ymax) ymax = y;
               }
               g.setColor(dimColor[d]);
               g.drawLine(ax, ymin, ax, ymax);
            }
            by[d] = getYFromIndex(ix - 1, d);
         }
         xlist.add(bx);
         ylist.add(by.clone());
      }

      // now we actually render the lines
      int nPoints = xlist.size();
      int[] yy = new int[nPoints];
      int[] xx = xlist.toArray();
      for(int d = 0; d < nd; d++){
         if (dimColor[d] == null) continue;
         g.setColor(dimColor[d]);
         for(int i = 0; i < nPoints; i++)
            yy[i] = ylist.get(i)[d];
         g.drawPolyline(xx, yy, nPoints);
      }

      if (bAntiAlias) Library.setAntiAlias(g, false);
   }

   /**
    * Returns the size that the component would like to have based on the dataset (if it exists).
    */
   public Dimension getPreferredSize()
   {
      Dimension dim;
      if (data == null) dim = new Dimension(400, PREF_HEIGHT);
      else{
         int w = (int)Math.ceil((double)data.getLengthMS() / calcTimeStep());
         dim = new Dimension(w, PREF_HEIGHT);
      }
      return dim;
   }

   public Dimension getPreferredScrollableViewportSize()
   {
      int w = (int)Math.ceil((double)getNumVisibleMS() / calcTimeStep());
      return new Dimension(w, PREF_HEIGHT);
   }

   protected void calcVScale()
   {
      if (data == null){
         valueBase = 0; // value corresponding to y=0
         valueStep = 1.0 / Math.max(1, getHeight());
         gridVScale = nVGridSpace * valueStep;
         return;
      }

      // determine scale for graph
      int nd = data.getNumDims();
      int h = getHeight();

      // determine the first & last visible indices
      int iVizStart = getIndexFromX(0, true);
      int iVizStop = getIndexFromX(getWidth(), true);

      // System.err.printf("lg.vscale: D=%d w,h=%d,%d viz: %d -> %d\n", nd, getWidth(), getHeight(),
      // iVizStart, iVizStop);

      // find the first rendered dimension
      int d = 0;
      while(d < nd && dimColor[d] == null)
         d++;
      // System.err.printf(" x=(%d -> %d) index=(%d -> %d) dim=%d ", 0, getWidth()-1, iVizStart, iVizStop, d);

      if (d < nd){ // we might not render any of the dimensions
         double vMin, vMax;
         if (bFixedScale){
            FeatureVec fvMin = data.getMin();
            FeatureVec fvMax = data.getMax();
            globalMin = fvMin.get(d);
            globalMax = fvMax.get(d);
            for(; d < nd; d++){
               if (dimColor[d] == null) continue;
               globalMin = Math.min(globalMin, fvMin.get(d));
               globalMax = Math.max(globalMax, fvMax.get(d));
            }
            vMin = globalMin;
            vMax = globalMax;
         }
         else{
            // seed min/max from this dimension
            vMin = data.get(iVizStart).get(d);
            vMax = vMin;

            // now find the real min/max
            for(; d < nd; d++){
               if (dimColor[d] == null) continue;
               for(int i = iVizStart; i <= iVizStop; i++){
                  double v = data.get(i).get(d);
                  if (v < vMin) vMin = v;
                  if (v > vMax) vMax = v;
               }
            }
         }

         double valueHeight = vMax - vMin + 1e-6;
         valueStep = valueHeight / Math.max(1, h - 16);
         valueBase = vMin - 8.0 * valueStep;

         // System.err.printf(" vheight=%.2f step=%.2f base=%.2f grid=%.2f\n", valueHeight, valueStep,
         // valueBase, gridVScale);
      }
      else{
         valueBase = 0;
         valueStep = 1.0 / getHeight();
      }

      // now figure out the vertical grid scale from these values
      gridVScale = nVGridSpace * valueStep;
   }

   public void setDimColor(int i, Color color)
   {
      super.setDimColor(i, color);
      bRedraw = true;
      globalMin = globalMax = Double.NaN;
   }

   public void setDimColor(Color[] colors)
   {
      super.setDimColor(colors);
      bRedraw = true;
      globalMin = globalMax = Double.NaN;
   }

   public void clearVertMarkers()
   {
      synchronized (vertMarkers){
         vertMarkers.clear();
      }
      bRedraw = true;
      repaint();
   }

   public void addVertLine(long ms, Color color, float thickness)
   {
      synchronized (vertMarkers){
         vertMarkers.add(new VertMarker(ms, color, thickness));
      }
      bRedraw = true;
      repaint();
   }

   public void setVertLine(long ms, Color color, float thickness)
   {
      synchronized (vertMarkers){
         vertMarkers.clear();
      }
      addVertLine(ms, color, thickness);
   }

   public void clearHorzMarkers()
   {
      horzMarkers.clear();
      bRedraw = true;
      repaint();
   }

   public void addHorzLine(double value, Color color, float thickness)
   {
      horzMarkers.add(new HorzMarker(value, color, thickness));
      bRedraw = true;
      repaint();
   }

   public boolean config(ConfigHelper chelp, String sKey, String sVal)
   {
      if (Library.stricmp(sKey, "fixed")){
         bFixedScale = ConfigHelper.isTrueString(sVal);
      }
      else{
         System.err.println("Error: unrecognized LineGraph parameters: " + sKey);
         assert false;
      }
      return true;
   }

   public void actionPerformed(ActionEvent e)
   {
      String cmd = e.getActionCommand();

      if (cmd.equals(sFixedScale)){
         bFixedScale = !bFixedScale;
         fireGraphVizChanged();
         repaint();
      }
      else if (cmd.equals(sCrosshairs)){
         bRenderMouseOverCrossHair = !bRenderMouseOverCrossHair;
         repaint();
      }
      else if (cmd.equals(sAntiAlias)){
         bRedraw = true;
         bAntiAlias = !bAntiAlias;
         repaint();
      }
   }

   public void componentResized(ComponentEvent e)
   {
      buf = null;
      super.componentResized(e);
   }
}

// //////////////////////////////////////////////////////////

/** Simple structure that holds a string, a color, and the string width */
class StringColorWidth
{
   public String s;
   public Color color;
   public int w;

   public StringColorWidth(String _s, Color _color, int _w)
   {
      s = _s;
      color = _color;
      w = _w;
   }
}
