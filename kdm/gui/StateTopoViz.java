package kdm.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;

import kdm.util.*;

/**
 * Visualizes a state transition matrix topology by drawing nodes and arcs
 */
public class StateTopoViz extends JMyComponent
{
   protected double[][] m;

   protected transient int[] xState;
   protected transient int baseline, diam, rad;
   protected transient int iHighlight;

   public StateTopoViz(double _m[][])
   {      
      setBackground(Color.white);
      setTransMatrix(_m);
      iHighlight = -1;
   }

   public void setTransMatrix(double[][] _m)
   {
      m = _m;
      repaint();
   }
   
   public void setHighlight(int i)
   {
      if (i != iHighlight)
      {
         iHighlight = i;
         repaint();
      }
   }
   
   @Override
   public void paintComponent(Graphics2D g, int w, int h)
   {
      g.setColor(getBackground());
      g.fillRect(0,0,w,h);
      
      if (m == null) return;
      
      // make sure we can fit
      int nStates = m.length;
      int minw = 4*nStates-1;
      if (w < minw) return;
      
      // figure out the locations of the states      
      xState = new int[nStates];
      if (nStates == 1){
         xState[0] = w/2;
         diam = w;
         rad = diam/2;
      }
      else{
         double k = 0.4;
         double gap = (w) / (nStates-1+k);
         diam = (int)Math.round(k*gap);
         rad = diam/2;
         double x = rad;
         for(int i=0; i<nStates; i++)
         {
            xState[i] = (int)Math.round(x);
            x += gap;
         }         
      }      
      
      Library.setAntiAlias(g, true);
      
      // baseline of nodes
      baseline = h-rad-1;
      
      // draw the arcs
      g.setColor(Color.black);
      for(int i=0; i<nStates; i++)
         for(int j=0; j<nStates; j++)
         {
            if (i==j) continue;
            if (m[i][j] == Library.LOG_ZERO) continue;
            if (i+1==j)
            {
               g.drawLine(xState[i]+rad, baseline, xState[j]-rad, baseline);               
            }
            else{
               int d = Math.abs(j-i);
               int b = baseline-rad-8;
               float dy = 30f;
               float y2 = 1.5f*(b+dy) / d - dy;
               QuadCurve2D qc = new QuadCurve2D.Float(xState[i], baseline, (xState[i]+xState[j])/2, y2, xState[j], baseline);
               g.draw(qc);
            }
         }
      
      // draw the nodes           
      for(int i=0; i<nStates; i++)
      {
         g.setColor(Color.black);
         g.fillOval(xState[i]-rad, baseline-rad, diam, diam);
         g.setColor(i==iHighlight ? Color.yellow : Color.white);
         g.fillOval(xState[i]-rad+1, baseline-rad+1, diam-2, diam-2);
      }
      
      Library.setAntiAlias(g, false);
   }

   /** @return index of state at the given point or -1 if none */
   public int getState(Point p)
   {
      if (xState==null) return -1;
      for(int i=0; i<xState.length; i++)
         if (p.distance(xState[i], baseline) <= rad) return i;
      return -1;
   }
}
