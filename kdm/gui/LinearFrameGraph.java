package kdm.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import kdm.data.*;

/**
 * abstract graph where each horizontal pixel represents a fixed number of frames; multiple LinearFrameGraphs
 * will typically not align properly unless the data frames and time matches
 */
public abstract class LinearFrameGraph extends Graph
{
   /** first visible tick */
   protected int iVizStart = 0;

   /** num pixels between each bar */
   protected int hgap = 1;

   /** width of rendered bar in pixels */
   protected int hbar = 7;

   /** half the bar width */
   protected int wbar2 = hbar / 2;

   protected transient TimeX mouseTime = null;
   protected transient ValueY mouseValue = null;

   /** default constructor (no data) */
   public LinearFrameGraph()
   {
      this(null);
   }

   /** construct graph with data */
   public LinearFrameGraph(Sequence data)
   {
      super(data);
   }

   public void setBarSize(int _wbar, int _wgap)
   {
      hbar = _wbar;
      wbar2 = hbar / 2;
      hgap = _wgap;
   }

   public TimeMarker.Units getUnits()
   {
      return TimeMarker.Units.Index;
   }

   public long getStart()
   {
      return getStartIndex();
   }

   public long getStartTime()
   {
      return data.getStartMS();
   }
   
   public long getEndTime()
   {
      return data.getEndMS();
   }

   public long getEnd()
   {
      return data.length();
   }
   
   @Override
   public int getXFromIndex(int ix)
   {
      int nBars = getNumVizBars();
      int nFrames = getNumVizFrames();
      assert(nBars >= 1 || nFrames >= 1);
      double fpb = (double)nFrames / nBars;
      double bar = (ix-iVizStart) / fpb;
      return (int)Math.round(bar * (hgap+hbar));  
   }
   
   @Override
   public int getXFromTime(long ms)
   {
      return getXFromIndex(data.getClosestIndex(ms));
   }
   
   public void setOffset(long offset)
   {
      iVizStart = (int)offset;
      fireGraphVizChanged();
      repaint();
   }

   public boolean setBounds(long start, long end)
   {
      assert (start == 0 && end == data.length());
      return true;
   }
   
   @Override
   public long getNumVisibleMS()
   {      
      if (iVizStart >= data.length()) return 0;
      int nFrames = getNumVizFrames();
      if (nFrames < 1) return 0;            
      long ms1 = data.getTimeMS(iVizStart);
      long ms2 = data.getTimeMS(Math.min(iVizStart + nFrames, data.length()) - 1);
      return ms2 - ms1 + getBarWidthMS();
   }

   /** @return number of frames visible given the current virtual width */
   public int getNumVizFrames()
   {
      if (vgraphw < 1) return 0;
      int w = getWidth();
      double frac = (double)w / Math.max(w, vgraphw);
      return (int)Math.ceil(data.length() * frac);
   }

   /** @return number of bars visible given the current virtual width */
   public int getNumVizBars()
   {
      return (int)Math.ceil((double)getWidth() / (hbar + hgap));
   }
   
   @Override   
   public int getIndexFromX(int x, boolean bClip)
   {
      assert(data!=null);
      int nBars = getNumVizBars();
      int nFrames = getNumVizFrames();
      if (nBars < 1 || nFrames < 1) return 0;
      double fpb = (double)nFrames / nBars;
      double fpx = fpb / (hbar+hgap);
      int ix = (int)Math.round(iVizStart + x*fpx);
      if (bClip)
      {
         if (ix<0) ix = 0;
         int n = data.length();
         if (ix>n) ix = n;
      }
      return ix;
   }
   
   @Override
   public ArrayList<TimeX> getGridTimes()
   {
      // TODO Auto-generated method stub
      return null;
   }  

   /** @return index of the visible bar at the given x coordinate */
   protected int getBar(int x)
   {
      return x / (hbar + hgap);
   }
   
   /** @return width in pixels of visible bar */
   public int getBarWidth(){ return hbar; }
   
   /** @return width in pixels of gap between bars */
   public int getBarGap(){ return hgap; }

   /** @return ms per bar */
   public long getBarWidthMS()
   {
      int nBars = getNumVizBars();
      int nFrames = getNumVizFrames();
      if (nBars < 1 || nFrames < 1) return 0;
      double fpb = (double)nFrames / nBars;
      return Math.round(data.getPeriod() * fpb * 1000.0);
   }

   /**
    * Returns a ValueY structure representing the value at the given y coordinate.
    */
   protected ValueY getValueY(int y)
   {
      assert false;
      return null;
   }
   
   public void setTime(TimeX tx)
   {
      if (tx == null) mouseTime = null;
      else
      {
         mouseTime = new TimeX(tx);

         // update index since it may have come from a different component
         if (data != null)
         {
            long ms = tx.time;
            if (ms < data.getStartMS() || ms > data.getEndMS()) mouseTime.index = -1;
            else mouseTime.index = data.getClosestIndex(ms);
         }
         else mouseTime.index = -1;
      }

      repaint();
   }

   public void mouseMoved(MouseEvent e)
   {
      mouseTime = getTimeX(e.getX());
      mouseValue = getValueY(e.getY());
      fireGraphMouseMoved(mouseTime, mouseValue);
      repaint();
   }

   public void mouseExited(MouseEvent e)
   {
      mouseTime = null;
      mouseValue = null;
      fireGraphMouseMoved(null, null);
      repaint();
   }
}
