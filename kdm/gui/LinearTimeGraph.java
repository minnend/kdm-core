package kdm.gui;

import kdm.data.*;
import kdm.util.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.SwingUtilities;

/* abstract linear time graph; each horizontal pixel represents a fixed amount of time */
public abstract class LinearTimeGraph extends Graph
{
   /** time in ms of left edge of virtual graph comp */
   protected long timeCompStart;

   /** time in ms of right edge of virtual graph comp */
   protected long timeCompEnd;

   /** temporal offset of display from start of data in ms */
   protected long timeOffset;
         
   /** approx. number of pixels between vertical grid lines */
   protected int nHGridSpace;

   /** number of data points between vertical grid lines */
   protected int gridHScale;

   protected transient TimeX mouseTime = null;
   protected transient ValueY mouseValue = null;

   public LinearTimeGraph()
   {
      this(null);
   }

   public LinearTimeGraph(Sequence _data)
   {
      super(_data);      
      nHGridSpace = 96;
   }
   
   public void setData(Sequence _data)
   {
      if (_data != null)
      {
         data = _data;
         if (dimColor != null)
         {
            Color[] dc = dimColor;         
            dimColor = new Color[data.getNumDims()];
            int nMin = Math.min(dc.length, dimColor.length);
            Library.copy(dc, dimColor, 0, 0, nMin);
         }
         else{
            dimColor = new Color[data.getNumDims()];
            if (data.getNumDims() == 1) dimColor[0] = getForeground();
         }
         timeCompStart = data.getStartMS();
         timeCompEnd = data.getEndMS();
      }
      else
      {
         data = null;
         timeCompStart = -1;
         timeCompEnd = -1;
      }      
      timeOffset = 0;
      super.setData(_data);
   }
   
   /** @return width of component in ms */
   public long getWidthMS()
   {
      return timeCompEnd - timeCompStart;
   }

   public TimeMarker.Units getUnits(){ return TimeMarker.Units.Time; }
   
   public long getStart()
   {
      return getStartTime();
   }
   
   public long getStartTime(){ return timeCompStart; }

   public long getEnd()
   {
      return timeCompEnd;
   }
   
   public void setOffset(long offset)
   {
      timeOffset = offset;
      fireGraphVizChanged();
      repaint();
   }
   
   public boolean setBounds(long start, long end)
   {
      timeCompStart = start;
      timeCompEnd = end;      
      fireGraphVizChanged();
      return true;
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

   public double getGridHScale()
   {
      return gridHScale;
   }

   /** @return ms per pixel of virtual component */
   public double calcTimeStep()
   {
      return (double)getWidthMS() / vgraphw;
   }

   @Override
   public int getXFromIndex(int ix)
   {
      return getXFromIndex(ix, calcTimeStep());
   }
      
   public int getXFromIndex(int ix, double timeStep)
   {
      long ms = data.getTimeMS(ix);
      return getXFromTime(ms, timeStep);
   }
   
   @Override
   public int getXFromTime(long ms)
   {
      return getXFromTime(ms, calcTimeStep());
   }

   
   public int getXFromTime(long ms, double timeStep)
   {      
      long time0 = timeCompStart + timeOffset;
      return (int)Math.round((ms - time0) / timeStep);
   }
   
   /**
    * Computes the number of ms represented across this component at its current size.
    */
   public long getNumVisibleMS()
   {
      // System.err.printf(" fig.getNumVis: w=%d\n", getWidth());
      return (long)Math.ceil(getWidth() * calcTimeStep());
   }

   /**
    * @return index of the (closest) data point to the given x screen coordinate.
    */
   public int getIndexFromX(int x, boolean bClip)
   {
      if (data == null) return -1;
      double timeStep = calcTimeStep();
      long ms = Math.round(timeStep * x) + timeCompStart + timeOffset;
      int i = data.getClosestIndex(ms);
      if (bClip) i = (int)Math.max(0, Math.min(i, data.length() - 1));
      return i;
   }
   
   @Override
   public long getTimeFromX(int x)
   {
      return timeCompStart + timeOffset + Math.round(calcTimeStep() * x);
   }

   protected ValueY getValueY(int y)
   {
      return null;
   }
   
   /**
    * Computes the locations of each vertical (time axis) grid line including the screen coordinate (relative
    * to 0), the time in seconds, and the real calendar date and time.
    */
   public ArrayList<TimeX> getGridTimes()
   {
      // TODO
      return new ArrayList<TimeX>();
   }

   protected void calcHScale()
   {
      double ms = nHGridSpace * calcTimeStep(); // ms between grid lines
      if (data == null) gridHScale = 10;
      else gridHScale = (int)Math.max(Math.round(ms / (1000.0 * data.getPeriod())), 1);
   }
   
   public void componentResized(ComponentEvent e)
   {
      super.componentResized(e);
      calcHScale();
   }

   public void mouseClicked(MouseEvent e)
   {
      // TODO: for debugging only
      if (data!=null && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2)
      {
         System.err.printf("%s: [%s]->[%s] (%s)  (w=%d vw=%d)\n", getClass().getName(), Library.formatTime(timeCompStart), Library
               .formatTime(timeCompEnd), Library.formatDuration(getWidthMS()), getWidth(), vgraphw);
         System.err.printf(" data: %d frames %dD [%s]  data-offset: %s  viz-offset: %d\n", data.length(),
               data.getNumDims(), Library.formatTime(data.getStartMS()), Library.formatDuration(data
                     .getStartMS() - timeCompStart), timeOffset);
      }
      super.mouseClicked(e);
   }

   public void mouseMoved(MouseEvent e)
   {
      mouseTime = getTimeX(e.getX());
      mouseValue = getValueY(e.getY());
      repaint();
      fireGraphMouseMoved(mouseTime, mouseValue);
   }
   
   public void mouseExited(MouseEvent e)
   {
      mouseTime = null;
      mouseValue = null;
      repaint();
      fireGraphMouseMoved(mouseTime, mouseValue);
   }
}
