package kdm.gui;

import kdm.data.*;
import kdm.util.*;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import java.util.*;
import java.text.*;
import java.io.*;

/**
 * Legend that display the time over uniform intervals and can display the "current" time at a given location
 */
public class TimeLegend extends JMyComponent implements GraphListener
{
   protected Graph graph;
   protected transient TimeX mouseTime = null;   
   protected boolean bAntiAlias = false;
   protected boolean bDrawIndex = true;

   /**
    * Create a TimeLegend that listens to the given graph for update events
    */
   public TimeLegend(Graph _graph)
   {
      graph = _graph;
      graph.addGraphListener(this);
      setBackground(Color.lightGray);
      setForeground(Color.black);
      setOpaque(true);
      setDoubleBuffered(true);
   }
   
   public void setDrawIndex(boolean b)
   {
      if (b != bDrawIndex)
      {
         bDrawIndex = b;
         repaint();
      }
   }

   /** make this TimeLegend alist listen for updates from the given graph */
   public void addGraph(Graph graph)
   {
      graph.addGraphListener(this);
   }

   @Override
   public void paintComponent(Graphics2D g, int w, int h)
   {
      Dimension dims = getSize();
      g.setFont(Library.smallSansFont);

      // clear the background
      g.setColor(getBackground());
      g.fillRect(0, 0, dims.width, dims.height);

      FontMetrics fm = g.getFontMetrics();
      if (bAntiAlias) Library.setAntiAlias(g, true);

      long mspx = graph.getNumVisibleMS() / w;
      long minute = 60L*1000;
      String sFormatBot = "HH:mm:ss.SSS";
      if (mspx > minute) sFormatBot = "HH:mm";
      
      // draw the times      
      int x = 0;
      int dx = 0;
      int sw = 90; // zot: should calculate this
      while(true)
      {
         TimeX timex = graph.getTimeX(x);
         renderTime(timex, g, sFormatBot, fm, dims, null, x!=0);
         
         if (x==0) dx = (int)Math.round(sw*1.8);
         x += dx;
         if (x > dims.width) break; 
      }
            
      // render mouse info
      if (mouseTime != null) renderTime(mouseTime, g, sFormatBot, fm, dims, Color.gray, true);
      if (bAntiAlias) Library.setAntiAlias(g, false);
   }
   
   /** Render time information and background triangle */
   protected int renderTime(TimeX timex, Graphics2D g, String sFormatBot, FontMetrics fm, Dimension dims, Color bgColor, boolean bCenter)
   {
      if (timex==null) return 10;
      final int hpad = 2;
      SimpleDateFormat sdf = Library.getSDF("d MMM yyyy");
      String sTop = sdf.format(timex.getDate());
      
      sdf = Library.getSDF(sFormatBot);
      String sBot = sdf.format(timex.getDate());
      if (bDrawIndex) sBot += String.format(" (%d)", timex.index);

      int wTop = fm.stringWidth(sTop);
      int wBot = fm.stringWidth(sBot);
      int w = Math.max(wTop, wBot) + hpad*2;
      int x = Math.min(Math.max(0, timex.x - w / 2), dims.width - w - 1);
      
      if (bgColor != null)
      {
         g.setColor(bgColor);
         g.fillRect(x, 0, w, dims.height);      
         g.setColor(getForeground());
         g.drawLine(x, 0, x, dims.height);
         g.drawLine(x+w, 0, x+w, dims.height);
      }
      else{
         int tx = timex.x;
         int v = getBackground().getRed();    
         int vv = Math.max(v - 12, 0);
         int[] xx = new int[]{tx-w/2+8, tx+w/2-8, tx};
         int[] yy = new int[]{0, 0, dims.height};
         g.setColor(Library.makeGray(vv));
         g.fillPolygon(xx, yy, 3);
         vv = Math.max(v - 24, 0);
         g.setColor(new Color(vv, vv, vv));
         g.drawPolygon(xx, yy, 3);
         
      }
      g.setColor(getForeground());
      int xx = (bCenter ? x + (w-wTop)/2 : x+hpad);
      g.drawString(sTop, xx, dims.height - fm.getHeight() - 3);
      xx = (bCenter ? x + (w-wBot)/2 : x+hpad);
      g.drawString(sBot, xx, dims.height - 3);      
      return w;
   }

   public Dimension getPreferredSize()
   {
      // 32 is a good height for two lines
      return new Dimension(200, 28);
   }

   public void graphMouseMoved(Graph graph, TimeX timex, ValueY valuey)
   {
      mouseTime = timex;
      repaint();
   }
   
   public void graphDataChanged(Graph graph)
   {
      // nothing to do
   }

   public void graphVizChanged(Graph graph)
   {
      repaint();
   }   
}
