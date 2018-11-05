package kdm.gui;

import java.awt.*;
import java.util.*;
import kdm.data.*;
import kdm.util.Library;

/**
 * A parallel coordinate view component
 */
public class ParallelCoordinateView extends JMyComponent
{
   public static final int xpad = 2;
   public static final int ypad = 2;
   public final static Stroke stroke = new BasicStroke(2.0f);
   public final static Stroke stroke1 = new BasicStroke(1.0f);

   protected ArrayList<FeatureVec> data;
   protected int nDims;
   protected double xmin[], xmax[], xrange[];
   protected Color c0, c1;
   protected int iHighlight;
   protected boolean bFixedRange;

   public ParallelCoordinateView()
   {
      this(new ArrayList<FeatureVec>());
   }

   public ParallelCoordinateView(ArrayList<FeatureVec> _data)
   {
      setBackground(Color.black);
      // cNormal = new Color(0.08f, 0.4f, 0.15f);
      // cHighlight = Color.green;
      c0 = Color.blue;
      c1 = Color.red;
      iHighlight = -1;
      bFixedRange = false;
      setData(_data);
   }

   public void setFixedRange(boolean b)
   {
      bFixedRange = b;
   }

   public void setFixedRange(ArrayList<FeatureVec> _data)
   {
      data = _data;
      bFixedRange = false;
      if (data != null && !data.isEmpty()) nDims = data.get(0).getNumDims();
      else nDims = -1;
      iHighlight = -1;
      calcStats();

      bFixedRange = true;
      data = null;
      repaint();
   }

   public void setData(ArrayList<FeatureVec> _data)
   {
      data = _data;
      if (data != null && !data.isEmpty()) nDims = data.get(0).getNumDims();
      else nDims = -1;
      iHighlight = -1;
      calcStats();
      repaint();
   }

   public void clear()
   {
      nDims = -1;
      xmin = xmax = xrange = null;
      data.clear();
      repaint();
   }

   public void add(FeatureVec fv)
   {
      assert (nDims <= 0 || fv.getNumDims() == nDims);
      if (data == null) data = new ArrayList<FeatureVec>();
      data.add(fv);
      if (nDims <= 0) nDims = fv.getNumDims();
      updateStats(fv);
      repaint();
   }

   public void calcStats()
   {
      if (bFixedRange) return;
      if (data == null || data.isEmpty()) clear();
      else
      {
         FeatureVec fv = data.get(0);
         xmin = fv.toArray();
         xmax = fv.toArray();
         xrange = new double[nDims];
         for(int i = 1; i < data.size(); i++)
            updateStats(data.get(i));
      }
   }

   public void setHighlight(int _iHighlight)
   {
      if (iHighlight != _iHighlight)
      {
         iHighlight = _iHighlight;
         repaint();
      }
   }

   public void updateStats(FeatureVec fv)
   {
      if (bFixedRange) return;
      for(int d = 0; d < nDims; d++)
      {
         double x = fv.get(d);
         if (x < xmin[d]) xmin[d] = x;
         if (x > xmax[d]) xmax[d] = x;
         xrange[d] = xmax[d] - xmin[d];
      }
   }

   protected void transform(FeatureVec fv, int w, int[] xc)
   {
      for(int d = 0; d < nDims; d++)
      {
         xc[d] = (int)Math.round(xpad + (fv.get(d) - xmin[d]) / xrange[d] * w);
      }
   }

   /**
    * Calculate the color for the given parameters
    * 
    * @param ix index of data point to calc color for
    * @param bHighlight is this a highlight or normal color?
    * @return color for this index
    */
   protected Color calcColor(int ix, boolean bHighlight)
   {
      Color c;
      if (c1 == null) c = c0;
      else
      {
         float f0[] = c0.getColorComponents(null);
         float f1[] = c1.getColorComponents(null);

         int n = data.size() - 1;
         for(int i = 0; i < f0.length; i++)
            f0[i] += (f1[i] - f0[i]) * ix / n;

         c = new Color(f0[0], f0[1], f0[2]);
      }
      if (!bHighlight) c = c.darker().darker();
      return c;
   }

   public void setColor(Color c)
   {
      c0 = c;
      c1 = null;
      repaint();
   }

   public void setColors(Color _c0, Color _c1)
   {
      c0 = _c0;
      c1 = _c1;
      repaint();
   }

   @Override
   public void paintComponent(Graphics2D g, int w, int h)
   {
      g.setColor(getBackground());
      g.fillRect(0, 0, w, h);

      if (nDims <= 0 || w <= 2 * xpad || h <= 2 * ypad) return;

      // calc coordinate positions
      int[] yc = new int[nDims];
      double dy = (double)(h - ypad * 2) / (nDims - 1);
      double fy = (double)ypad;
      for(int i = 0; i < nDims; i++)
      {
         yc[i] = (int)Math.round(fy);
         fy += dy;
      }

      // now draw the data
      Library.setAntiAlias(g, true);
      g.setStroke(stroke);
      int nData = data.size();
      int[] xc = new int[nDims];
      for(int i = 0; i < nData; i++)
      {
         if (i == iHighlight) continue;
         g.setColor(calcColor(i, false));
         transform(data.get(i), w - 2 * xpad, xc);
         g.drawPolyline(xc, yc, nDims);
      }

      // draw highlighted value last
      if (iHighlight >= 0)
      {
         g.setColor(calcColor(iHighlight, true));
         transform(data.get(iHighlight), w - 2 * xpad, xc);
         g.drawPolyline(xc, yc, nDims);
      }

      // draw the coordinate bars
      g.setColor(Color.darkGray);
      g.setStroke(stroke1);
      for(int i = 0; i < nDims; i++)
         g.drawLine(0, yc[i], w, yc[i]);

      Library.setAntiAlias(g, false);
   }

}
