package kdm.gui;

import kdm.data.*;
import kdm.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import java.text.*;
import java.io.*;

/** vertical legend showing different values at different heights */
public class ValueLegend extends JComponent implements GraphListener
{
   protected Graph graph;
   protected transient ValueY mouseValue = null;
   protected int nSigDigs = 5;

   // TODO specify sigdigs and adjust width

   public ValueLegend(Graph graph)
   {
      this.graph = graph;
      graph.addGraphListener(this);
      setBackground(Color.lightGray);
      setForeground(Color.black);
      setOpaque(true);
      setDoubleBuffered(false);
   }

   public void setSigDigs(int sd)
   {
      if (sd != nSigDigs)
      {
         nSigDigs = sd;
         repaint();
      }
   }

   public void paintComponent(Graphics g)
   {      
      Dimension dims = getSize();
      g.setFont(Library.smallSansFont);
      int h = g.getFontMetrics().getHeight();

      // clear the background
      g.setColor(getBackground());
      g.fillRect(0, 0, dims.width, dims.height);

      // draw the values
      ArrayList<ValueY> gridy = graph.getGridValues();
      if (gridy != null)
      {
         g.setColor(Color.black);
         for(ValueY vy : gridy)
         {
            int y = dims.height - vy.y;
            String s = PrettyPrint.printSigDig(vy.v, nSigDigs, false);
            g.drawString(s, 1, y + h / 2 - 3);
         }
      }

      // render mouse info
      if (mouseValue != null && mouseValue.y >= 0)
      {
         int wh = h + 2;
         int y = mouseValue.y - wh / 2;
         y = (int)Math.min(Math.max(y, 0), dims.height - wh);

         g.setColor(Color.gray);
         g.fillRect(0, y, dims.width, wh);
         g.setColor(Color.black);
         g.drawLine(0, y, dims.width, y);
         g.drawLine(0, y + wh - 1, dims.width, y + wh - 1);

         String s = PrettyPrint.printSigDig(mouseValue.v, nSigDigs, false);
         g.drawString(s, 2, y + wh - 4);
      }
   }

   public Dimension getPreferredSize()
   {
      return new Dimension(40, 200);
   }

   public void graphMouseMoved(Graph graph, TimeX timex, ValueY valuey)
   {
      mouseValue = valuey;
      repaint();
   }

   public void graphDataChanged(Graph graph)
   {
      repaint();
   }

   public void graphVizChanged(Graph graph)
   {
      repaint();
   }
}
