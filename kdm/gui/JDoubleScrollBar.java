package kdm.gui;

import java.awt.*;

import javax.swing.*;
import java.awt.event.*;
import java.util.*;
import kdm.util.*;

import static java.awt.event.AdjustmentEvent.*;

/**
 * Double scrollbar allows normal scrolling plus interactive adjustment of scrollbar extent. This allows
 * "zooming" by making the extent larger (zoom out) or smaller (zoom in).
 */
public class JDoubleScrollBar extends JMyComponent implements KeyListener, MouseMotionListener, Adjustable, FocusListener
{
   // TODO vertical scrollbar
   // TODO better keyboard focus visualization
   // TODO click on background to scroll
   // TODO auto-scroll if hold button down
   // TODO ensure knob is drawn properly even when small
   // TODO scroll wheel
   // TODO render better slider pointing up/down 
   // TODO min extent, max extent
   // TODO make it not ugly
   
   public static enum DragType {
      None, Main, Left, Right
   }

   public static final int HORIZONTAL = 0;
   public static final int VERTICAL = 1;

   public static final int KNOB_VALUE_CHANGED = ADJUSTMENT_LAST + 1;
   public static final int LEFT_KNOB = 14301;
   public static final int RIGHT_KNOB = 14302;

   public static final int BUTTONW = 16;
   public static final int KNOB_EDGEW = 12;
   public static final int PREF_HEIGHT = 16;

   public static final Color cEnabledBG = Library.makeGray(200);
   public static final Color cDisabledBG = Library.makeGray(220);
   public static final Color cEnabledFG = Library.makeGray(160);
   public static final Color cDisabledFG = Library.makeGray(200);

   protected int orientation, mousex;
   protected int vmin, vmax, extent, val;
   protected int incUnit, incBlock;
   protected JButton btSmaller, btLarger;
   protected Vector<AdjustmentListener> alist;
   protected DragType kDrag = DragType.None;
   protected boolean bSlider = false;

   public JDoubleScrollBar(int _vmin, int _vmax, int _val, int _extent, int _orientation)
   {
      vmin = _vmin;
      vmax = _vmax;
      val = _val;
      extent = _extent;
      assert (vmin <= vmax);
      assert (val >= vmin && val + extent <= vmax);

      orientation = _orientation;
      assert (orientation == HORIZONTAL || orientation == VERTICAL);

      alist = new Vector<AdjustmentListener>();
      incUnit = 1;
      incBlock = 10;

      setOpaque(true);
      setFocusable(true);
      addKeyListener(this);
      addMouseMotionListener(this);
      addFocusListener(this);
      mousex = -1;

      btSmaller = new JButton("<");
      //btSmaller.setAutoscrolls(true);
      btSmaller.setMargin(new Insets(0, 0, 0, 0));
      btSmaller.addMouseListener(this);
      btSmaller.addKeyListener(this);
      add(btSmaller);
      btLarger = new JButton(">");
      //btLarger.setAutoscrolls(true);
      btLarger.setMargin(new Insets(0, 0, 0, 0));
      btLarger.addMouseListener(this);
      btLarger.addKeyListener(this);
      add(btLarger);
   }

   public void setSlider(boolean bSlider)
   {
      if (this.bSlider == bSlider) return;
      this.bSlider = bSlider;
      btSmaller.setEnabled(!bSlider);
      btSmaller.setVisible(!bSlider);
      btLarger.setEnabled(!bSlider);
      btLarger.setVisible(!bSlider);
      setOpaque(!bSlider);
      repaint();
   }
   
   public void setEnabled(boolean b)
   {
      btSmaller.setEnabled(b && !bSlider);
      btLarger.setEnabled(b && !bSlider);
      super.setEnabled(b);
   }

   public Dimension getPreferredSize()
   {
      return new Dimension(Integer.MAX_VALUE, PREF_HEIGHT);
   }

   public void doLayout()
   {
      int w = getWidth();
      int h = getHeight();
      btSmaller.setBounds(0, 0, BUTTONW, h);
      btLarger.setBounds(w - BUTTONW, 0, BUTTONW, h);
   }
   
   protected int getPixelStartBG()
   {
      return (bSlider ? 0 : btSmaller.getWidth());
   }
   
   protected int getPixelEndBG()
   {
      return (bSlider ? getWidth() : btLarger.getX());
   }

   @Override
   public void paintComponent(Graphics2D g, int cw, int ch)
   {
      int v0 = getPixelStartBG();
      int vw = getPixelEndBG() - v0;

      renderBackground(g, v0, vw, ch);

      int bx = getXFromVal(val);
      int bw = getXFromVal(val + extent) - bx;

      renderKnob(g, bx, bw, ch);
   }

   /**
    * Render the background of the scrollbar
    * 
    * @param g graphics context for rendering
    * @param x x-coord where background starts (to right of left button)
    * @param w width of background
    * @param h height of scrollbar
    */
   protected void renderBackground(Graphics2D g, int x, int w, int h)
   {
      if (bSlider){
         // clear the background
         if (isOpaque()){
            g.setColor(getBackground());
            g.fillRect(x, 0, w, h);
         }
         
         // render border
         int ym = h/2;
         g.setColor(Color.lightGray);
         g.fillRect(x,ym-1,w-1,3);
         g.setColor(Color.gray);
         g.drawRect(x,ym-2,w-1,4);
      }
      else{
         // clear the background
         if (isEnabled()) g.setColor(cEnabledBG);
         else g.setColor(cDisabledBG);
         g.fillRect(x, 0, w, h);

         // draw the top/bottom guide rail
         int x2 = x + w;
         g.setColor(Color.gray);
         g.drawLine(x, 0, x2, 0);
         g.drawLine(x, h - 1, x2, h - 1);
      }
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
      Color cDarker = cBase.darker().darker();
      g.setColor(cBase);
      g.fillRect(kx, 0, kw, h);

      // draw border
      g.setColor(Color.darkGray);
      g.drawRect(kx, 0, kw - 1, h - 1);
      
      if (isFocusOwner() || btSmaller.isFocusOwner() || btLarger.isFocusOwner()){
         g.setColor(Color.lightGray);
         g.drawRect(kx+1,1,kw-3,h-3);
      }

      // draw the edge seps
      g.setColor(cDarker); // TODO: max size so no cross / draw outside knob
      g.drawLine(kx + KNOB_EDGEW, 1, kx + KNOB_EDGEW, h - 2);
      g.setColor(Color.lightGray);
      g.drawLine(kx + KNOB_EDGEW + 1, 1, kx + KNOB_EDGEW + 1, h - 2);

      g.setColor(Color.lightGray);
      g.drawLine(kx + kw - KNOB_EDGEW, 1, kx + kw - KNOB_EDGEW, h - 2);
      g.setColor(cDarker);
      g.drawLine(kx + kw - KNOB_EDGEW - 1, 1, kx + kw - KNOB_EDGEW - 1, h - 2);
   }

   /** @return x-coor of the (start of) the given value */
   protected int getXFromVal(int v)
   {
      int v0 = getPixelStartBG();
      int vw = getPixelEndBG() - v0;
      return (int)(v0 + (v - vmin) * vw / (vmax - vmin + 1));
   }

   /** @return value of scrollbar at the given x-coor */
   protected int getValFromX(int x)
   {
      int v0 = getPixelStartBG();
      int vw = getPixelEndBG() - v0;
      return vmin + (x - v0) * (vmax - vmin + 1) / vw;
   }

   public boolean isLeftKnob(int x)
   {
      int bx = getXFromVal(val);
      return (x >= bx && x < bx + KNOB_EDGEW);
   }

   public boolean isMainKnob(int x)
   {
      int bx = getXFromVal(val) + KNOB_EDGEW;
      int bx2 = getXFromVal(val + extent) - KNOB_EDGEW;
      return (x >= bx && x < bx2);
   }

   public boolean isRightKnob(int x)
   {
      int bx = getXFromVal(val + extent) - KNOB_EDGEW;
      return (x >= bx && x < bx + KNOB_EDGEW);
   }

   public void mouseClicked(MouseEvent e)
   {}

   public void mousePressed(MouseEvent e)
   {
      if (!isEnabled()) return;

      Object src = e.getSource();
      int x = e.getX();

      if (src == btSmaller) doDecUnit();
      if (src == btLarger) doIncUnit();

      if (src == this){
         if (isMainKnob(x)){
            //System.err.printf("Main Knob!\n");
            kDrag = DragType.Main;
            mousex = x;
         }
         else if (isLeftKnob(x)){
            //System.err.printf("Left Knob!\n");
            kDrag = DragType.Left;
            mousex = x;
         }
         else if (isRightKnob(x)){
            //System.err.printf("Right Knob!\n");
            kDrag = DragType.Right;
            mousex = x;
         }
      }

      repaint();
   }

   public void mouseReleased(MouseEvent e)
   {
      mousex = -1;
      kDrag = DragType.None;
   }

   public void mouseEntered(MouseEvent e)
   {}

   public void mouseExited(MouseEvent e)
   {}

   public void mouseDragged(MouseEvent e)
   {
      if (!isEnabled()) return;
      int x = e.getX();
      
      if (kDrag == DragType.Main){         
         int dv = getValFromX(x) - getValFromX(mousex);
         int val2 = Math.max(vmin, Math.min(vmax - extent + 1, val + dv));
         if (val != val2){
            mousex = x;
            val = val2;
            fireAdjustmentEvent(ADJUSTMENT_VALUE_CHANGED, TRACK);
            repaint();
         }
      }
      else if (kDrag == DragType.Left){
         int dv = getValFromX(x) - getValFromX(mousex);
         dv = Math.max(vmin-val, dv);
         dv = Math.min(extent-1, dv);
         if (dv!=0){
            mousex = Math.max(x, getPixelStartBG());
            val += dv;
            extent -= dv;            
            fireAdjustmentEvent(KNOB_VALUE_CHANGED, LEFT_KNOB);
            repaint();
         }         
      }
      else if (kDrag == DragType.Right){
         int dv = getValFromX(x) - getValFromX(mousex);
         dv = Math.max(-extent+1, dv);
         dv = Math.min(vmax+1-(val+extent), dv);
         if (dv!=0){
            mousex = Math.min(x, getPixelEndBG()-1);
            extent += dv;
            fireAdjustmentEvent(KNOB_VALUE_CHANGED, RIGHT_KNOB);
            repaint();
         }         

      }
      assert(val >= vmin);
      assert(val <= vmax);
      assert(extent >= 1);
      assert(extent <= vmax-vmin+1);
   }

   public void mouseMoved(MouseEvent e)
   {}

   public void keyTyped(KeyEvent e)
   {}

   public void doIncUnit()
   {
      if (val == vmax - extent + 1) return;
      val = Math.min(vmax - extent + 1, val + incUnit);
      fireAdjustmentEvent(ADJUSTMENT_VALUE_CHANGED, UNIT_INCREMENT);
   }

   public void doDecUnit()
   {
      if (val == vmin) return;
      val = Math.max(vmin, val - incUnit);
      fireAdjustmentEvent(ADJUSTMENT_VALUE_CHANGED, UNIT_DECREMENT);
   }

   public void doIncBlock()
   {
      if (val == vmax - extent + 1) return;
      val = Math.min(vmax - extent + 1, val + incBlock);
      fireAdjustmentEvent(ADJUSTMENT_VALUE_CHANGED, BLOCK_INCREMENT);
   }

   public void doDecBlock()
   {
      if (val == vmin) return;
      val = Math.max(vmin, val - incBlock);
      fireAdjustmentEvent(ADJUSTMENT_VALUE_CHANGED, BLOCK_DECREMENT);
   }

   public void doHome()
   {
      val = vmin;
      fireAdjustmentEvent(ADJUSTMENT_VALUE_CHANGED, TRACK);
   }

   public void doEnd()
   {
      val = vmax - extent + 1;
      fireAdjustmentEvent(ADJUSTMENT_VALUE_CHANGED, TRACK);
   }

   public void doIncUnitLeftKnob()
   {
      if (extent == 1) return;
      val = Math.min(val + incUnit, val + extent - 1);
      extent = Math.max(1, extent - incUnit);
      fireAdjustmentEvent(KNOB_VALUE_CHANGED, LEFT_KNOB);
   }

   public void doDecUnitLeftKnob()
   {
      if (val == vmin) return;
      int val2 = Math.max(vmin, val - incUnit);
      extent += val - val2;
      val = val2;
      fireAdjustmentEvent(KNOB_VALUE_CHANGED, LEFT_KNOB);
   }

   public void doIncUnitRightKnob()
   {
      if (val + extent == vmax + 1) return;
      extent = Math.min(extent + incUnit, vmax - val + 1);
      fireAdjustmentEvent(KNOB_VALUE_CHANGED, RIGHT_KNOB);
   }

   public void doDecUnitRightKnob()
   {
      if (extent == 1) return;
      extent = Math.max(1, extent - incUnit);
      fireAdjustmentEvent(KNOB_VALUE_CHANGED, RIGHT_KNOB);
   }

   public void keyPressed(KeyEvent e)
   {
      if (!isEnabled()) return;

      Object src = e.getSource();
      int key = e.getKeyCode();

      if (key == KeyEvent.VK_SPACE){
         if (src == btSmaller) doDecUnit();
         else if (src == btLarger) doIncUnit();
      }

      if ((e.getModifiersEx() & (KeyEvent.SHIFT_DOWN_MASK | KeyEvent.CTRL_DOWN_MASK)) == 0){
         if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_UP) doDecUnit();
         else if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_DOWN) doIncUnit();
         else if (key == KeyEvent.VK_PAGE_UP) doDecBlock();
         else if (key == KeyEvent.VK_PAGE_DOWN) doIncBlock();
         else if (key == KeyEvent.VK_HOME) doHome();
         else if (key == KeyEvent.VK_END) doEnd();
      }
      else if ((e.getModifiersEx() & KeyEvent.SHIFT_DOWN_MASK) != 0){
         // SHIFT is down
         if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_UP) doDecUnitLeftKnob();
         else if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_DOWN) doIncUnitLeftKnob();
      }
      else{
         // CTRL is down
         if (key == KeyEvent.VK_LEFT || key == KeyEvent.VK_UP) doDecUnitRightKnob();
         else if (key == KeyEvent.VK_RIGHT || key == KeyEvent.VK_DOWN) doIncUnitRightKnob();
      }

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

   public void setMinimum(int min)
   {
      vmin = min;
      repaint();
   }

   public void setMaximum(int max)
   {
      vmax = max;
      repaint();
   }

   public void setUnitIncrement(int u)
   {
      incUnit = u;
   }

   public int getUnitIncrement()
   {
      return incUnit;
   }

   public void setBlockIncrement(int b)
   {
      incBlock = b;
   }

   public int getBlockIncrement()
   {
      return incBlock;
   }

   public void setVisibleAmount(int v)
   {
      extent = v;
      repaint();
   }

   public int getVisibleAmount()
   {
      return extent;
   }

   public int getExtent()
   {
      return extent;
   }

   public void setValue(int v)
   {
      val = v;
      repaint();
   }

   public void removeAdjustmentListener(AdjustmentListener al)
   {
      alist.remove(al);
   }

   public void focusGained(FocusEvent e)
   {
      repaint();
   }

   public void focusLost(FocusEvent e)
   {
      repaint();
   }
   
   public static void main(String args[])
   {
      JFrame f = new JFrame("Test DoubleScrollBar");
      f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      f.setSize(800, 600);
      JPanel p = new JPanel(new BorderLayout());
      f.setContentPane(p);
      JDoubleScrollBar dsbh = new JDoubleScrollBar(1, 100, 50, 10, JDoubleScrollBar.HORIZONTAL);
      dsbh.addAdjustmentListener(new AdjustmentListener() {
         public void adjustmentValueChanged(AdjustmentEvent e)
         {
            JDoubleScrollBar sb = (JDoubleScrollBar)e.getSource();
            System.err.printf("adjust-horz: %d  %d -> %d  (%d)\n", sb.getValue(), sb.getMinimum(), sb
                  .getMaximum(), sb.getExtent());
         }
      });
      /*
       * final JDoubleScrollBar dsbv = new JDoubleScrollBar(1, 100, 50, 10, JDoubleScrollBar.VERTICAL);
       * dsbv.addAdjustmentListener(new AdjustmentListener(){ public void
       * adjustmentValueChanged(AdjustmentEvent e) { System.err.printf("adjust-vert: %d %d -> %d (%d)\n",
       * dsbv.getValue(), dsbv.getMinimum(), dsbv.getMaximum(), dsbv.getExtent()); } });
       */
      //dsbh.setSlider(true);
      p.add(dsbh, BorderLayout.SOUTH);
      // p.add(dsbv, BorderLayout.EAST);
      f.setVisible(true);
   }
}
