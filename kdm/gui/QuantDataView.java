package kdm.gui;

import java.awt.*;
import javax.swing.*;
import kdm.data.*;
import kdm.util.*;

/**
 * Graphical representation of a quantized data sequences. A quantized sequence has values
 * in [0..n). The representation is simply a horizontal bar with a different color for
 * each value in the series.
 */
public class QuantDataView extends JMyComponent
{
   public static final Color colorHighlight = new Color(1.0f, 0.8f, 0.4f);

   protected DiscreteSeq seq;
   protected Color[] colors;
   protected boolean bHighlight;
   protected SpanList highlight;
   protected int maxHeight = -1;
   protected int hgap = 0;

   /**
    * Create a quantized data view for the specified sequence using the given colors for
    * each value.
    * 
    * @param _seq sequence to represent
    * @param _colors colors to use for each value
    */
   public QuantDataView(DiscreteSeq _seq, Color[] _colors)
   {
      this(_seq, _colors, false, 0);
   }

   public QuantDataView(DiscreteSeq _seq, Color[] _colors, boolean _bHighlight, int _hgap)
   {
      seq = _seq;
      assert (seq.getNumDims() == 1) : String.format("Must have 1D data for quantized view (%s: %dD)", seq.getName(), seq.getNumDims());
      colors = _colors;
      bHighlight = _bHighlight;
      if (bHighlight) highlight = new SpanList(0, seq.length(), false);
      hgap = _hgap;
      setBackground(Color.black);
      setOpaque(true);
   }

   public void setHGap(int _hgap)
   {
      hgap = _hgap;
      repaint();
   }

   /**
    * Remove all highlights
    */
   public void clear()
   {
      if (!bHighlight) return;
      highlight.clear();
      repaint();
   }

   /**
    * highlight the specified range
    * 
    * @param a start index
    * @param b end index
    */
   public void highlight(int a, int b)
   {
      highlight.add(a, b);
      repaint();
   }

   /**
    * highlight the specified ranges
    * 
    * @param sl list containing ranges to highlight
    */
   public void highlight(SpanList sl)
   {
      highlight.add(sl);
      repaint();
   }

   /**
    * Highlight all occcurrences of the given sequence. Values less than zero are "don't
    * care"
    * 
    * @param sub sequence to match.
    */
   public void highlight(int[] sub)
   {
      highlight(sub, new SpanList(0, seq.length()-1, true));
   }
   
   /**
    * Highlight all occcurrences of the given sequence. Values less than zero are "don't
    * care"
    * 
    * @param sub sequence to match
    * @param spanAvail list specifying which locations are available for matching
    */
   public void highlight(int[] sub, SpanList spanAvail)
   {
      int iSkipTo = -1;
      int nw = sub.length;
      int[] data = seq.toArray();
      SpanList spanSub = new SpanList(spanAvail.getRangeMin(), spanAvail.getRangeMax(), false);
      spanAvail.itReset();
      while(spanAvail.itMore())
      {
         int i = spanAvail.itNext();
         if (!spanAvail.contains(new Range(i, i+nw-1))) continue;
         if (i < iSkipTo) continue;
         
         boolean bMatch = true;
         for(int j = 0; j < nw; j++)
         {
            if (sub[j] >= 0 && sub[j] != data[i + j])
            {
               bMatch = false;
               break;
            }
         }
         if (bMatch)
         {
            highlight.add(i, i + nw - 1);            
            spanSub.add(i, i+nw-1);
            iSkipTo = i+nw;
         }
      }
      spanAvail.sub(spanSub);
      repaint();
   }

   public Dimension getPreferredSize()
   {
      Insets ins = getInsets();
      int w = Math.max(seq.length(), 50) + hgap * (seq.length() - 1) + ins.left + ins.right;
      int h = Math.min(4, getMaximumSize().height) + (bHighlight ? 4 : 0) + ins.top + ins.bottom;
      return new Dimension(w, h);
   }

   public Dimension getMaximumSize()
   {      
      return new Dimension(Integer.MAX_VALUE, maxHeight < 1 ? Integer.MAX_VALUE : maxHeight);
   }

   public void setMaxHeight(int maxh)
   {
      // TODO: could be smarter: square pixels, max+square,
      // height-priority vs. width-priority, etc.
      maxHeight = maxh;
      revalidate();
   }

   public void paintComponent(Graphics2D g, int w, int h)
   {
      int border = (bHighlight ? Math.max(2, h / 10) : 0);
      int n = seq.length();
      int qh = h - 2 * border;

      if (hgap > 0)
      {
         g.setColor(getBackground());
         g.fillRect(0, 0, w, h);
      }

      // draw the (stretched) quantized data
      for(int i = 0; i < n; i++)
      {
         int a = (int)(w * i / n);
         int b = (int)(w * (i + 1) / n);
         if (i > 0) a += hgap / 2;
         if (i + 1 < n) b -= hgap - hgap / 2;
         int c = seq.geti(i);
         if (c<0 || c>=colors.length) g.setColor(Color.lightGray);
         else g.setColor(colors[c]);
         g.fillRect(a, border, b - a, qh);
      }

      if (bHighlight)
      {
         // draw black borders where highlight would go
         g.setColor(Color.black);
         g.fillRect(0, 0, w, border);
         g.fillRect(0, qh + border, w, border);

         // now draw the selections
         g.setColor(colorHighlight);
         for(int i = 0; i < highlight.getNumSpans(); i++)
         {
            Range range = highlight.getRange(i);
            int a = (int)(w * range.a / n);
            int b = (int)(w * (range.b+1) / n);
            g.fillRect(a, 0, b - a, border);
            g.fillRect(a, border + qh, b - a, border);
         }
      }
   }
}
