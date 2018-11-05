package kdm.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import javax.swing.*;

/**
 * A JComponent that handles borders automatically -- subclasses should do all rendering in the new
 * paintComponent(Graphics2D, int, int) method. Note that this component includes a mouse listener (for
 * popups), so you don't need to add one in subclases.
 */
public abstract class JMyComponent extends JComponent implements MouseListener
{
   public JMyComponent()
   {
      setOpaque(true);
      addMouseListener(this);
   }

   public void paintComponent(Graphics g)
   {
      Insets ins = getInsets();
      int w = getWidth();
      int h = getHeight();

      int cw = w - (ins.left + ins.right);
      int ch = h - (ins.top + ins.bottom);

      if (cw > 0 && ch > 0){
         Graphics2D g2 = (Graphics2D)g;
         AffineTransform at = g2.getTransform();
         if (ins.left != 0 || ins.top != 0) g2.translate(ins.left, ins.top);
         paintComponent((Graphics2D)g, cw, ch);
         if (ins.left != 0 || ins.top != 0) g2.setTransform(at);
      }
   }

   public abstract void paintComponent(Graphics2D g, int cw, int ch);

   /** build a new popup menu or add items to your parent's popup */
   public JPopupMenu buildPopup(boolean bAppend)
   {
      return null;
   }

   /** append items to the end of the popup menu */
   public JPopupMenu appendPopup(JPopupMenu menu)
   {
      return menu;
   }

   public void showPopup(JPopupMenu menu, MouseEvent e)
   {
      if (menu == null || e == null) return;
      menu.show(e.getComponent(), e.getX(), e.getY());
   }

   public void mouseClicked(MouseEvent e)
   {}

   public void mouseEntered(MouseEvent e)
   {}

   public void mouseExited(MouseEvent e)
   {}

   public void mousePressed(MouseEvent e)
   {
      if (e.isPopupTrigger()) showPopup(buildPopup(true), e);
   }

   public void mouseReleased(MouseEvent e)
   {
      if (e.isPopupTrigger()) showPopup(buildPopup(true), e);
   }
}
