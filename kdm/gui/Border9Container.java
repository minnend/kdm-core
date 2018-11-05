package kdm.gui;

import kdm.data.*;
import java.awt.*;
import javax.swing.*;

/**
 * A container that positions its children on a 3x3 grid where the center area is flexible
 */
public class Border9Container extends JComponent
{
   public static final int TopLeft = 0;
   public static final int Top = 1;
   public static final int TopRight = 2;
   public static final int Left = 3;
   public static final int Center = 4;
   public static final int Right = 5;
   public static final int BottomLeft = 6;
   public static final int Bottom = 7;
   public static final int BottomRight = 8;

   protected Component kids[];
   protected Point ptl, pbr; // top-left and bottom-right of center area

   public Border9Container()
   {
      setLayout(null);
      kids = new Component[9];
      for(int i = 0; i < 9; i++)
         kids[i] = null;
   }

   public Component add(Component comp, int spot)
   {
      Component old = kids[spot];
      if (old != null) remove(old);
      if (comp != null) add(comp);
      kids[spot] = comp;
      revalidate();
      return old;
   }
   
   public void remove(int spot)
   {
      if (kids[spot] == null) return;
      remove(kids[spot]);
      kids[spot] = null;
      revalidate();
   }
   
   public Component getKid(int spot)
   {
      return kids[spot];
   }

   protected void calcBounds(Dimension dims)
   {
      Insets ins = getInsets();
      ptl = new Point(0, 0);
      pbr = new Point(dims.width, dims.height);

      // adjust ptl/pbr based on top row
      if (kids[TopLeft] != null)
      {
         Dimension d = kids[TopLeft].getPreferredSize();
         ptl.x = (int)Math.max(ptl.x, d.width);
         ptl.y = (int)Math.max(ptl.y, d.height);
      }
      if (kids[Top] != null)
      {
         Dimension d = kids[Top].getPreferredSize();
         ptl.y = (int)Math.max(ptl.y, d.height);
      }
      if (kids[TopRight] != null)
      {
         Dimension d = kids[TopRight].getPreferredSize();
         pbr.x = (int)Math.min(pbr.x, dims.width - d.width);
         ptl.y = (int)Math.max(ptl.y, d.height);
      }

      // adjust ptl/pbr based on middle row
      if (kids[Left] != null)
      {
         Dimension d = kids[Left].getPreferredSize();
         ptl.x = (int)Math.max(ptl.x, d.width);
      }
      if (kids[Right] != null)
      {
         Dimension d = kids[Right].getPreferredSize();
         pbr.x = (int)Math.min(pbr.x, dims.width - d.width);
      }

      // adjust ptl/pbr based on bottom row
      if (kids[BottomLeft] != null)
      {
         Dimension d = kids[BottomLeft].getPreferredSize();
         ptl.x = (int)Math.max(ptl.x, d.width);
         pbr.y = (int)Math.min(pbr.y, dims.height - d.height);
      }
      if (kids[Bottom] != null)
      {
         Dimension d = kids[Bottom].getPreferredSize();
         pbr.y = (int)Math.min(pbr.y, dims.height - d.height);
      }
      if (kids[BottomRight] != null)
      {
         Dimension d = kids[BottomRight].getPreferredSize();
         pbr.x = (int)Math.min(pbr.x, dims.width - d.width);
         pbr.y = (int)Math.min(pbr.y, dims.height - d.height);
      }
   }

   public void doLayout()
   {
      // TODO: doesn't handle insets properly (i.e., at all!)
      super.doLayout();
      Dimension dims = getSize();
      calcBounds(dims);

      // now we can do the layout
      int w[] = { ptl.x, pbr.x - ptl.x, dims.width - pbr.x };
      int h[] = { ptl.y, pbr.y - ptl.y, dims.height - pbr.y };

      // layout center first because other components likely depend on it
      if (kids[Center] != null) kids[Center].setBounds(ptl.x, ptl.y, w[1], h[1]);
      if (kids[TopLeft] != null) kids[TopLeft].setBounds(0, 0, w[0], h[0]);
      if (kids[Top] != null) kids[Top].setBounds(ptl.x, 0, w[1], h[0]);
      if (kids[TopRight] != null) kids[TopRight].setBounds(pbr.x, 0, w[2], h[0]);
      if (kids[Left] != null) kids[Left].setBounds(0, ptl.y, w[0], h[1]);
      if (kids[Right] != null) kids[Right].setBounds(pbr.x, ptl.y, w[2], h[1]);
      if (kids[BottomLeft] != null) kids[BottomLeft].setBounds(0, pbr.y, w[0], h[2]);
      if (kids[Bottom] != null) kids[Bottom].setBounds(ptl.x, pbr.y, w[1], h[2]);
      if (kids[BottomRight] != null) kids[BottomRight].setBounds(pbr.x, pbr.y, w[2], h[2]);
   }

   public Dimension getMinimumSize()
   {
      return new Dimension(0, 0);
   }

   public Dimension getPreferredSize()
   {
      calcBounds(getSize());
      return new Dimension(2 * (ptl.x + pbr.x), 2 * (ptl.y + pbr.y));
   }

   public Dimension getMaximumSize()
   {
      return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
   }
}
