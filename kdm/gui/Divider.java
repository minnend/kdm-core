package kdm.gui;

import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;

/**
 * A divider draws a horizontal line and can be titled
 */
public class Divider extends JMyComponent
{
   protected String title;
   protected int leftRule = 12; // TODO: should be able to center title as well
   protected int leftGap = 3;
   protected int rightGap = 2;
   protected JLabel lbTitle;

   public Divider()
   {
      this(null);
   }

   public Divider(String _title)
   {
      title = _title;
      lbTitle = new JLabel(_title);
      setLayout(null);
      add(lbTitle);
   }
   
   public JLabel getLabel(){ return lbTitle; }

   public void paintComponent(Graphics2D g, int w, int h)
   {
      int h2 = h / 2;

      g.setColor(getBackground());
      g.fillRect(0, 0, w, h);

      if (title != null)
      {
         Dimension dim = lbTitle.getPreferredSize();
         lbTitle.setBounds(leftRule+leftGap, (h - dim.height) / 2, dim.width, dim.height);
         if (leftRule > 0) drawLine(g, 0, h2, leftRule);
         drawLine(g, leftRule + leftGap + dim.width + rightGap, h2, w);
      }
      else drawLine(g, 0, h2, w);
   }

   protected void drawLine(Graphics2D g, int x1, int y, int x2)
   {
      g.setColor(Color.gray);
      g.drawLine(x1, y, x2, y);
      g.setColor(Color.white);
      g.drawLine(x1, y+1, x2, y+1);
   }

   public Dimension getPreferredSize()
   {
      if (lbTitle != null)
      {
         Dimension dim = lbTitle.getPreferredSize();
         dim.height += 2;
         return dim;
      }
      return new Dimension(1, 4);
   }
}
