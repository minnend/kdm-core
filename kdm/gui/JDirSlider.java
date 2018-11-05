package kdm.gui;

import static java.awt.event.AdjustmentEvent.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

import kdm.util.Library;

/**
 * A kind of slider similar to a joystick in which moving the knob right or left slides the value up or down,
 * respectively. Moving the knob farther from center accelerates the slide.
 */
public class JDirSlider extends JComponent implements KeyListener, MouseListener, MouseMotionListener,
      Adjustable, Runnable
{
   // TODO: center point should be center of current page

   public static final int HORIZONTAL = 0;
   public static final int VERTICAL = 1;

   public static final int PREF_HEIGHT = 16;

   public static final Color cEnabledBG = Library.makeGray(200);
   public static final Color cDisabledBG = Library.makeGray(220);
   public static final Color cEnabledFG = Library.makeGray(160);
   public static final Color cDisabledFG = Library.makeGray(200);
   public static final Color cHighlight = Library.makeGray(200);
   public static final Color cShadow = Library.makeGray(140);
   public static final Color cExtent = Library.makeGray(190);
   public static final Color cExtentBorder = Library.makeGray(170);

   protected final Object lock = new Object();
   protected boolean bThread = true;

   protected int sbExtent = 10;
   protected int sbMax = 99 + sbExtent;
   protected final int sbMin = -100;
   protected int sbVal = 0;

   protected int orientation, mousex;
   protected int vmin, vmax, val, extent;
   protected Vector<AdjustmentListener> alist;

   public JDirSlider(int min, int max, int val, int extent, int orientation)
   {
      vmin = min;
      vmax = max;
      this.val = val;
      this.extent = extent;
      assert (vmin <= vmax);
      assert (val >= vmin && val <= vmax);

      this.orientation = orientation;
      assert (orientation == HORIZONTAL || orientation == VERTICAL);

      alist = new Vector<AdjustmentListener>();

      setFocusable(true);
      addKeyListener(this);
      addMouseMotionListener(this);
      addMouseListener(this);
      mousex = -1;

      Thread thread = new Thread(this);
      thread.start();
   }

   public void finalize()
   {
      System.err.printf("JDirSlider finalize!\n");
      bThread = false;
   }

   public Dimension getPreferredSize()
   {
      if (orientation == HORIZONTAL) return new Dimension(999999, PREF_HEIGHT);
      else return new Dimension(PREF_HEIGHT, 999999);
   }

   public void paintComponent(Graphics g)
   {
      Graphics2D g2 = (Graphics2D)g;
      int w = getWidth();
      int h = getHeight();

      // TODO visualize keyboard focus

      sbExtent = Math.max(w / 16, 1);
      sbMax = 99 + sbExtent;

      // clear the background
      if (isEnabled()) g.setColor(cEnabledBG);
      else g.setColor(cDisabledBG);
      g.fillRect(0, 0, w, h);

      // draw the top/bottom guide rail
      g.setColor(Color.gray);
      g.drawRect(0, 0, w - 1, h - 1);

      int bx = (val - vmin) * w / (vmax - vmin);
      int bw = Math.max(extent * w / (vmax - vmin), 4);
      renderExtent(g2, bx, bw, h);

      bx = getXFromVal(sbVal);
      bw = Math.max(getXFromVal(sbVal + sbExtent) - bx, 2);
      renderKnob(g2, bx, bw, h);
   }

   /**
    * Render the extent indicator
    * 
    * @param g graphcis context for rendering
    * @param x x-coordinate of start of viz
    * @param w width of viz
    * @param h height of slider
    */
   protected void renderExtent(Graphics2D g, int x, int w, int h)
   {
      g.setColor(cExtent);
      g.fillRoundRect(x, 4, w, h - 8, 2, 2);
      g.setColor(cExtentBorder);
      g.drawRoundRect(x, 4, w, h - 8, 2, 2);
   }

   /**
    * Render the scrollbar knob
    * 
    * @param g graphics context for rendering
    * @param kx x-coord of knob
    * @param kw width of knob
    * @param h height of scrollbar
    */
   protected void renderKnob(Graphics2D g, int kx, int kw, int h)
   {
      // draw the knob
      Color cBase = (isEnabled() ? cEnabledFG : cDisabledFG);
      g.setColor(cBase);
      g.fillRect(kx, 0, kw, h);

      // draw border
      g.setColor(Color.darkGray);
      g.drawRect(kx, 0, kw - 1, h - 1);

      if (isEnabled())
      {
         // draw the highlight
         g.setColor(cHighlight);
         g.drawLine(kx + 1, 1, kx + kw - 2, 1);
         g.drawLine(kx + 1, 1, kx + 1, h - 2);

         // draw the shadow
         g.setColor(cShadow);
         g.drawLine(kx + 2, h - 2, kx + kw - 2, h - 2);
         g.drawLine(kx + kw - 2, 2, kx + kw - 2, h - 3);
      }
   }

   /** @return x-coor of the (start of) the given scrollbar value */
   protected int getXFromVal(int v)
   {
      int vw = getWidth();
      int x = (int)((v - sbMin) * vw / (sbMax - sbMin + 1));
      return x;
   }

   /** @return value of scrollbar at the given x-coor */
   protected int getValFromX(int x)
   {
      int vw = getWidth();
      return x * (sbMax - sbMin + 1) / vw + sbMin;
   }

   public boolean isMainKnob(int x)
   {
      int bx = getXFromVal(sbVal);
      int bx2 = getXFromVal(sbVal + sbExtent);
      return (x >= bx && x < bx2);
   }

   public void mouseClicked(MouseEvent e)
   {}

   public void mousePressed(MouseEvent e)
   {
      if (!isEnabled()) return;
      requestFocusInWindow();

      Object src = e.getSource();
      int x = e.getX();

      if (src == this && isMainKnob(x)) mousex = x;

      repaint();
   }

   public void mouseReleased(MouseEvent e)
   {
      mousex = -1;
      sbVal = 0; // TODO: animate way back to zero
      repaint();
   }

   public void mouseEntered(MouseEvent e)
   {}

   public void mouseExited(MouseEvent e)
   {}

   public void mouseDragged(MouseEvent e)
   {
      if (!isEnabled()) return;

      if (mousex >= 0)
      {
         int x = e.getX();
         int dv = getValFromX(x) - getValFromX(mousex);
         int val2 = Math.max(sbMin, Math.min(sbMax - sbExtent + 1, sbVal + dv));
         if (sbVal != val2)
         {
            mousex = x;
            sbVal = val2;
            synchronized (lock)
            {
               lock.notify();
            }
         }
      }
   }

   public void mouseMoved(MouseEvent e)
   {}

   public void keyTyped(KeyEvent e)
   {}

   public void keyPressed(KeyEvent e)
   {
      if (!isEnabled()) return;

      Object src = e.getSource();
      int key = e.getKeyCode();

      // TODO: left/right to control scroll
      System.err.printf("dir slider key: %d\n", key);

      repaint();
   }

   public int getValue()
   {
      return val;
   }

   public int getMaximum()
   {
      return vmax;
   }

   public int getMinimum()
   {
      return vmin;
   }

   public void fireAdjustmentEvent(int id, int type)
   {
      AdjustmentEvent e = new AdjustmentEvent(this, id, type, val);
      for(AdjustmentListener al : alist)
         al.adjustmentValueChanged(e);
   }

   public void addAdjustmentListener(AdjustmentListener al)
   {
      if (!alist.contains(al)) alist.add(al);
   }

   public void keyReleased(KeyEvent e)
   {}

   public int getOrientation()
   {
      return orientation;
   }

   public void setMinMax(int min, int max, int extent)
   {
      assert (min + extent <= max);
      vmin = min;
      vmax = max;
      this.extent = extent;
      int prevVal = val;
      if (val < vmin) val = vmin;
      else if (val > vmax - extent) val = vmax - extent;
      if (val != prevVal) fireAdjustmentEvent(ADJUSTMENT_VALUE_CHANGED, TRACK);
      repaint();
   }

   public void setMinimum(int min)
   {
      assert (vmin <= vmax);
      vmin = min;
      if (val < vmin)
      {
         val = vmin;
         fireAdjustmentEvent(ADJUSTMENT_VALUE_CHANGED, TRACK);
      }
      repaint();
   }

   public void setMaximum(int max)
   {
      assert (vmax - extent >= vmin);
      vmax = max;
      if (val > vmax - extent)
      {
         val = vmax - extent;
         fireAdjustmentEvent(ADJUSTMENT_VALUE_CHANGED, TRACK);
      }
      repaint();
   }

   public void setValue(int v)
   {
      assert (val >= vmin && val <= vmax - extent);
      if (v == val) return;
      val = v;
      fireAdjustmentEvent(ADJUSTMENT_VALUE_CHANGED, TRACK);
      repaint();
   }

   public void removeAdjustmentListener(AdjustmentListener al)
   {
      alist.remove(al);
   }

   public static void main(String args[])
   {
      JFrame f = new JFrame("Test Directional Slider");
      f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      f.setSize(800, 600);
      JPanel p = new JPanel(new BorderLayout());
      f.setContentPane(p);
      final JDirSlider ds = new JDirSlider(1, 100, 50, 10, JDoubleScrollBar.HORIZONTAL);
      ds.addAdjustmentListener(new AdjustmentListener() {
         public void adjustmentValueChanged(AdjustmentEvent e)
         {
            System.err.printf("adjust-horz: sbVal=%d  val=%d\n", ds.sbVal, ds.getValue());
         }
      });

      p.add(ds, BorderLayout.SOUTH);
      f.setVisible(true);
   }

   public int getBlockIncrement()
   {
      assert false;
      return 0;
   }

   public int getUnitIncrement()
   {
      assert false;
      return 0;
   }

   public int getVisibleAmount()
   {
      assert false;
      return 0;
   }

   public void setBlockIncrement(int b)
   {
      assert false;
   }

   public void setUnitIncrement(int u)
   {
      assert false;
   }

   public void setVisibleAmount(int v)
   {
      assert false;
   }

   protected int mapval(int v)
   {
      int vw = vmax - vmin + 1;
      double k = 5; // b = ka in quadratic: ax^2+bx
      double x = 50; // vw/vmax = max jump
      double a = vw / (x * 100 * (100 + k));
      double sgn = Math.signum(v);
      v = Math.abs(v);
      return (int)Math.round(sgn * Math.max((a * v) * (v + k), 1));
   }

   public void run()
   {
      while(bThread)
      {
         synchronized (lock)
         {
            try
            {
               lock.wait();
            } catch (InterruptedException e)
            {
               continue;
            }
         }

         while(sbVal != 0)
         {
            int dv = mapval(sbVal);
            val = val + dv;
            if (val > (vmax - extent)) val = vmax - extent;
            else if (val < vmin) val = vmin;
            fireAdjustmentEvent(ADJUSTMENT_VALUE_CHANGED, TRACK);
            repaint();
            Library.sleep(50);
         }
      }

   }
}
