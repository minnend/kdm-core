package kdm.gui;

import kdm.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EtchedBorder;

import java.util.*;

/**
 * GUI component that renders a set of ranges with colors attached (in the range.payload field).
 */
public class ColoredRangeBar extends JMyComponent
{
   public static enum Highlight {
      None, Top, Bottom, Both
   }

   protected Range rBar;
   protected ArrayList<Range> ranges;
   protected ArrayList<Range> highlights;
   protected Highlight highlight = Highlight.None;
   protected Color cHighlight = Color.yellow;
   protected int hPref = 6;
   protected int hHighlight = 1;
   protected boolean bBarBorder = true;

   /**
    * Create a default bar -- range is min necessary to hold colored ranges
    */
   public ColoredRangeBar()
   {
      this(null, true);
   }

   /**
    * create a bar with the given range
    * 
    * @param _rBar range of the new bar
    */
   public ColoredRangeBar(Range _rBar)
   {
      this(_rBar, true);
   }

   /**
    * create a bar with the given range
    * 
    * @param _rBar range of the new bar
    * @param _bBarBorder draw border around each colored bar?
    */
   public ColoredRangeBar(Range _rBar, boolean _bBarBorder)
   {
      rBar = _rBar;
      bBarBorder = _bBarBorder;
      ranges = new ArrayList<Range>();
      highlights = new ArrayList<Range>();
      setBackground(Color.black);
      setOpaque(true);
   }

   public void setBarBorder(boolean b)
   {
      if (b != bBarBorder){
         bBarBorder = b;
         repaint();
      }
   }

   public void setHighlightLoc(Highlight hl)
   {
      if (hl != highlight){
         highlight = hl;
         if (!highlights.isEmpty()) repaint();
      }
   }
   
   public void setHighlightHeight(int h)
   {
      if (h != hHighlight){
         hHighlight = h;
         if (highlight!=Highlight.None && !highlights.isEmpty()) repaint();
      }
   }
   
   public void clearHighlight()
   {
      if (!highlights.isEmpty()){
         highlights.clear();
         if (highlight!=Highlight.None) repaint();
      }
   }
   
   public void addHighlight(Range r)
   {
      highlights.add(r);      
      if (highlight!=Highlight.None) repaint();
   }

   public void setHighlightColor(Color c)
   {
      if (c != cHighlight){
         cHighlight = c;
         if (highlight!=Highlight.None && !highlights.isEmpty()) repaint();
      }
   }
   
   public void setPreferredHeight(int h)
   {
      hPref = h;
      revalidate();
   }

   /**
    * remove all colored reanges
    */
   public void clear()
   {
      if (!ranges.isEmpty()){
         ranges.clear();
         repaint();
      }
   }

   /** convenience method for adding a range and performing all updates */
   public void add(Range r){
      add(r, true, true, true);
   }
   
   /**
    * add a new colored range
    * 
    * @param r the range to add
    */
   public void add(Range r, boolean bSort, boolean bUpdateToolTip, boolean bRepaint)
   {
      assert (r.payload instanceof Pair);

      ranges.add(r);
      if (bSort) Collections.sort(ranges);
      if (bUpdateToolTip) updateToolTip();
      if (bRepaint) repaint();
   }
   
   public void sortBars()
   {
      Collections.sort(ranges);
   }
   
   public void updateToolTip()
   {
      StringBuffer sb = new StringBuffer();
      for(Range range : ranges){
         sb.append('[');
         sb.append(((Pair)range.payload).first);
         sb.append("] ");
      }
      if (sb.length() < 200) setToolTipText(sb.toString()); // TODO smarter tool tip text
      else setToolTipText(sb.substring(0, 197)+"...");
   }

   public void paintComponent(Graphics2D g, int w, int h)
   {
      // if there's no data, just fill in the bg color
      if (ranges.isEmpty() && highlights.isEmpty()){
         g.setColor(getBackground());
         g.fillRect(0, 0, w, h);
         return;
      }

      // we have some data to render...
      Range rb = (rBar != null ? rBar : new Range(ranges.get(0).a, ranges.get(ranges.size() - 1).b));
      double scale = (double)w / rb.length();
      int hTop = 0, hBot = 0;
      if (highlight==Highlight.Both || highlight==Highlight.Top) hTop = hHighlight;
      if (highlight==Highlight.Both || highlight==Highlight.Bottom) hBot = hHighlight;
      int lastx = 0;
      for(Range r : ranges){
         int a = getPixelPos(r.a, rb.a, scale).a;
         int b = getPixelPos(r.b, rb.a, scale).b;

         if (a > lastx){
            g.setColor(getBackground());
            g.fillRect(lastx, 0, a - lastx, h);
         }

         Color color = (Color)((Pair)r.payload).second;
         g.setColor(color);
         g.fillRect(a, hTop, b - a, h-hTop-hBot);
         if (bBarBorder){
            g.setColor(color.darker());
            g.drawRect(a, hTop, b - a - 1, h - 1 - hTop - hBot);
         }
         lastx = b;
      }
      
      // fill in the last empty bit
      if (lastx < w){
         g.setColor(getBackground());
         g.fillRect(lastx, 0, w - lastx, h);
      }

      // clear the highlight region
      g.setColor(getBackground());
      if (hTop > 0) g.fillRect(0, 0, w, hTop);
      if (hBot > 0) g.fillRect(0, h-hBot, w, hBot);   
      
      // draw the highlights
      for(Range r : highlights){
         int a = getPixelPos(r.a, rb.a, scale).a;
         int b = getPixelPos(r.b, rb.a, scale).b;

         g.setColor(cHighlight);
         if (hTop > 0) g.fillRect(a, 0, b - a, hTop);         
         if (hBot > 0) g.fillRect(a, h-hBot, b - a, hBot);         
      }
   }

   protected static Range getPixelPos(int x, int xStart, double scale)
   {
      int a = (int)Math.round((x - xStart) * scale);
      int b = (int)Math.round((x + 1 - xStart) * scale) - 1;
      return new Range(a, b);
   }

   public Dimension getPreferredSize()
   {
      Range rb = (rBar != null ? rBar : new Range(ranges.get(0).a, ranges.get(ranges.size() - 1).b));
      Insets ins = getInsets();
      return new Dimension(rb.length(), hPref + ins.top + ins.bottom);
   }
}
