package kdm.gui;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import kdm.mlpr.AgglomInfo;

public class Dendrogram extends JMyComponent implements MouseMotionListener, MouseListener
{
   public static final int vmargin = 4;
   public static final int hmargin = 2;
   public static final int radius = 6;
   public static final int mouseSelectRadius = 100;
   
   protected AgglomInfo root, aiMouse;
   protected Point mousePos = null;
   protected Point renderMouse = null;
   
   public Dendrogram(AgglomInfo _root)
   {
      root = _root;
      setBackground(Color.white);
      addMouseMotionListener(this);
      addMouseListener(this);
   }
   
   public AgglomInfo getMouseAI(){ return aiMouse; }
   
   public void paintComponent(Graphics2D g, int w, int h)
   {
      g.setColor(getBackground());
      g.fillRect(0,0,w,h);
            
      double ppd = (double)(h - 2*vmargin) / root.getDist(); // pixels per unit distance
      if (ppd <= 0) return;
      
      Rectangle r = new Rectangle(hmargin, vmargin, w-2*hmargin, h-2*vmargin);
      renderMouse = null;
      renderDendrogram(g, root, r, ppd);
      if (renderMouse != null)
      {
         g.setColor(Color.yellow);         
         g.fillOval(renderMouse.x-radius, renderMouse.y-radius, radius*2, radius*2);
      }
   }
   
   protected double calcMaxDist(AgglomInfo node)
   {
      if (node == null) return 0;      
      double a = calcMaxDist(node.getChild(1));
      double b = calcMaxDist(node.getChild(2));
      return node.getDist() + Math.max(a,b);
   }
   
   protected void renderDendrogram(Graphics2D g, AgglomInfo ai, Rectangle r, double ppd)
   {
      if (ai == null) return;
      if (r.width < 1  || r.height < 1) return;
      
      int nLeft = (ai.getChild(1)==null ? 1 : ai.getChild(1).getCount());
      int nRight = (ai.getChild(2)==null ? 1 : ai.getChild(2).getCount());
      
      int wLeft = r.width * nLeft / ai.getCount();
      int wRight = r.width - wLeft;
      
      //g.setColor(Color.lightGray);
      //g.drawRect(r.x, r.y, r.width, r.height);
      
      int dLeft = (ai.getChild(1)!=null ? (int)Math.round(ppd*(ai.getDist()-ai.getChild(1).getDist())) : 0);
      int dRight = (ai.getChild(2)!=null ? (int)Math.round(ppd*(ai.getDist()-ai.getChild(2).getDist())) : 0);
      
      Rectangle rLeft = new Rectangle(r.x, r.y+dLeft, wLeft-1, r.height-dLeft);
      Rectangle rRight = new Rectangle(r.x+wLeft+1, r.y+dRight, wRight-1, r.height-dRight);
      
      Point p1 = new Point(rLeft.x+(rLeft.width+1)/2, rLeft.y);
      Point p2 = new Point(rRight.x+(rRight.width+1)/2, rRight.y);
      
      g.setColor(Color.black);      
      if (ai.hasKids())
      {
         // TODO: should render bottom-up, so we can center vert lines over cluster
         g.drawLine(p1.x, r.y, p2.x, r.y);
         if (ai.hasGrandKids(1))
            g.drawLine(p1.x, r.y, p1.x, p1.y);
         else
            g.drawLine(p1.x, r.y, p1.x, r.y+r.height);
         
         if (ai.hasGrandKids(2))
            g.drawLine(p2.x, r.y, p2.x, p2.y);
         else
            g.drawLine(p2.x, r.y, p2.x, r.y+r.height);
      
         if (mousePos != null)
         {
            Point p = new Point(r.x+(r.width+1)/2, r.y);
            double d = p.distance(mousePos); 
            if (d < mouseSelectRadius && (renderMouse==null || d<renderMouse.distance(mousePos)))
            {
               renderMouse = p;
               aiMouse = ai;
            }
         }
      
         renderDendrogram(g, ai.getChild(1), rLeft, ppd);
         renderDendrogram(g, ai.getChild(2), rRight, ppd);         
      }
   }

   public void mouseDragged(MouseEvent e)
   {
   }

   public void mouseMoved(MouseEvent e)
   {
      mousePos = e.getPoint();
      repaint();
   }

   public void mouseClicked(MouseEvent e)
   {
      // TODO Auto-generated method stub
      
   }

   public void mousePressed(MouseEvent e)
   {
      if (aiMouse != null) System.err.println(aiMouse);
      
   }

   public void mouseReleased(MouseEvent e)
   {
      // TODO Auto-generated method stub
      
   }

   public void mouseEntered(MouseEvent e)
   {
      // TODO Auto-generated method stub
      
   }

   public void mouseExited(MouseEvent e)
   {
      mousePos = null;
      repaint();      
   }

}
