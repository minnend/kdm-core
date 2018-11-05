package kdm.gui;

import java.awt.*;
import java.util.*;

import kdm.data.*;
import kdm.util.*;

/**
 * graph where each horizontal pixel corresponds to a fixed number of frames and each vertical pixel
 * corresponds to a fixed value
 */
public abstract class ValueFrameGraph extends LinearFrameGraph
{
   public static final String KeyFrameRange = "VFG.FrameRange";
   // TODO !! should have separate classes for value and time vs. frame and bring them together in the final
   // graph; this should cut down on redundancy

   /** behavior for aligning the vertical scale */
   public static enum VScale {
      None, Top, Middle, Bottom, Fixed
   }

   /** number of pixels between horizontal grid lines */
   protected int nVGridSpace = 48;
   protected boolean bFixedScale = false;
   protected Sequence seqViz;

   protected VScale vscale = VScale.None;

   /** if VScale!=None, fix the specified position to this value */
   protected double fixedValueBase;
   
   /** if VScale==Fixed, this is the max value (i.e., the top of the range) */
   protected double fixedValueTop;
   
   /** value corresponding to y=0 */
   protected double valueBase;

   /** value represented by one vertical pixel */
   protected double valueStep;

   /** total value between horizontal grid lines */
   protected double gridVScale;

   protected ArrayList<FrameGraphOverlay> overlays;
   protected Color colorGrid = Library.makeGray(48);
   protected boolean bRenderVerticalGridLines = true;
   protected boolean bRenderHorizontalGridLines = true;
   
   protected ArrayList<HorzMarker> horzMarkers;

   public ValueFrameGraph(Sequence data)
   {
      super(data);
      overlays = new ArrayList<FrameGraphOverlay>();
      horzMarkers = new ArrayList<HorzMarker>();
   }

   public void addOverlay(FrameGraphOverlay overlay)
   {
      if (!overlays.contains(overlay)){
         overlays.add(overlay);
         bRedraw = true;
         repaint();
      }
   }

   public void removeOverlay(FrameGraphOverlay overlay)
   {
      if (overlays.remove(overlay)){
         bRedraw = true;
         repaint();
      }
   }

   public void clearVScale()
   {
      if (vscale == VScale.None) return;
      vscale = VScale.None;
      bRedraw = true;
      repaint();
   }

   public void setVScale(VScale vscale, double v)
   {
      if (vscale != this.vscale || fixedValueBase != v){
         this.vscale = vscale;
         fixedValueBase = v;
         bRedraw = true;
         repaint();
      }
   }
   
   public void setVScale(VScale vscale, double vBase, double vTop)
   {
      assert(vscale == VScale.Fixed);
      if (vscale != this.vscale || fixedValueBase != vBase || fixedValueTop != vTop){
         this.vscale = vscale;
         fixedValueBase = vBase;
         fixedValueTop = vTop;
         bRedraw = true;
         repaint();
      }
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

   /** @return y coordinate from a real value */
   protected int getYFromValue(double v)
   {
      int y = (int)Math.round((v - valueBase) / valueStep);
      return getHeight() - y;
   }

   /** @return cached visualized sequence; generate it if necessary */
   protected Sequence getSeqViz()
   {
      if (seqViz == null) seqViz = genSeqViz();
      return seqViz;
   }

   /** @return accumulate data for each vizible bar (may be multiple frames per bar) */
   protected Sequence genSeqViz()
   {
      if (data == null) return null;
      int nBars = getNumVizBars();
      int nFrames = getNumVizFrames();
      if (nBars < 1 || nFrames < 1) return null;
      int T = data.length();
      Sequence seq = new Sequence();
      double fpb = (double)nFrames / nBars;
      for(int i = 0; i <= nBars; i++){
         int f1 = (int)Math.round(iVizStart + i * fpb);
         if (f1 >= T) break;
         int f2 = (int)Math.round(iVizStart + (i + 1) * fpb);
         f2 = Math.min(f2, T);
         FeatureVec fvNow = getBarData(f1, f2);
         fvNow.setMeta(KeyFrameRange, new Point(f1, f2));
         seq.add(fvNow, data.getTimeMS(f1));
         if (f2 >= T) break;
      }
      return seq;
   }

   /** @return aggregate data for the ix-th visible bar; default implementation simply averages */
   protected FeatureVec getBarData(int f1, int f2)
   {
      FeatureVec ret = new FeatureVec(data.get(f1));
      for(int i = f1 + 1; i < f2; i++)
         ret._add(data.get(i));
      if (f2 - f1 > 1) ret._div(f2 - f1);
      return ret;
   }

   /** Update the vertical scale parameters using the current data and parameters */
   protected final void calcVScale()
   {
      if (data == null){
         valueBase = 0; // value corresponding to y=0
         valueStep = 2.0 / getHeight();
         gridVScale = nVGridSpace * valueStep;
         return;
      }

      // determine scale for graph
      int nd = data.getNumDims();
      int iStart = bFixedScale ? 0 : iVizStart;
      int nViz = bFixedScale ? data.length() : getNumVizFrames();
      double[] minmax = getMinMax(iStart, nViz);

      double eps = 0.000001;
      if (vscale == VScale.Top){
         minmax[1] = fixedValueBase;
         minmax[0] = Math.min(minmax[0], minmax[1]);
      }
      else if (vscale == VScale.Middle){
         double half = Math.max(minmax[1] - fixedValueBase, fixedValueBase - minmax[0]);
         minmax[0] = fixedValueBase - half;
         minmax[1] = fixedValueBase + half;
      }
      else if (vscale == VScale.Bottom){
         minmax[0] = fixedValueBase;
         minmax[1] = Math.max(minmax[0], minmax[1]);
      }
      else if (vscale == VScale.Fixed){
         minmax[0] = fixedValueBase;
         minmax[1] = fixedValueTop;
      }

      double valueHeight = minmax[1] - minmax[0] + eps;
      double vgap = 8;
      valueStep = valueHeight / Math.max(1, getHeight() - vgap * 2);
      valueBase = minmax[0] - vgap * valueStep;
      gridVScale = nVGridSpace * valueStep;
   }

   /** Default implementation returns the min/max value across all dimensions and overlay */
   protected double[] getMinMax(int iStart, int nViz)
   {
      FeatureVec[] fv = data.getMinMax(iStart, nViz);
      double[] minmax = new double[] { fv[0].min(), fv[1].max() };
      for(FrameGraphOverlay overlay : overlays){
         double[] mnx = overlay.getMinMax(iStart, nViz);
         if (mnx[0] < minmax[0]) minmax[0] = mnx[0];
         if (mnx[1] > minmax[1]) minmax[1] = mnx[1];
      }
      return minmax;
   }

   @Override
   protected ValueY getValueY(int y)
   {
      double v = valueBase + valueStep * (getHeight() - y);
      return new ValueY(y, v);
   }

   @Override
   public ArrayList<ValueY> getGridValues()
   {
      if (gridVScale == 0) calcVScale();
      ArrayList<ValueY> grid = new ArrayList<ValueY>();
      double v = Math.ceil(valueBase / gridVScale) * gridVScale;
      Dimension dims = getSize();

      while(true){
         int y = (int)Math.round((v - valueBase) / valueStep);
         if (y > dims.height) break;
         grid.add(new ValueY(y, v));
         v += gridVScale;
      }

      return grid;
   }

   @Override
   public long getTimeFromX(int x)
   {
      if ((seqViz == null || seqViz.length() == 0)) seqViz = genSeqViz();
      if (seqViz == null) return 0;
      int iBar = getBar(x);
      if (iBar >= seqViz.length()){
         iBar = seqViz.length() - 1;
         long ms = seqViz.getTimeMS(iBar);
         int dx = x - iBar * (hgap + hbar);
         double mspx = (double)getNumVisibleMS() / getWidth();
         return Math.round(ms + dx * mspx);
      }
      return seqViz.getTimeMS(iBar);
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
         if (gridx != null){
            for(TimeX tx : gridx){
               if (tx == null) continue;
               g.drawLine(tx.x, 0, tx.x, dims.height);
            }
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

   /** Render the horizontal markers in this component */
   protected void renderHorzMarkers(Graphics2D g)
   {
      if (vgraphw == 0) return;
      Dimension dim = getSize();
      Stroke stroke = g.getStroke();
      for(HorzMarker hm : horzMarkers)
      {
         int y = getYFromValue(hm.value);
         if (y < 0 || y>=dim.height) continue;
         
         g.setColor(hm.color);         
         g.setStroke(hm.stroke);
         g.drawLine(0, y, dim.width, y);
      }
      g.setStroke(stroke);
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
}
