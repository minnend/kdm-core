package kdm.gui;

import java.awt.*;

/**
 * Just like a GridLayout, but the components might have slightly different sizes (+/-1
 * pixel) to ensure that they cover the container
 */
public class GridFillLayout extends GridLayout
{
   public GridFillLayout(int rows, int cols)
   {
      super(rows, cols);
   }

   public GridFillLayout(int rows, int cols, int hgap, int vgap)
   {
      super(rows, cols, hgap, vgap);
   }

   public void layoutContainer(Container parent)
   {
      super.layoutContainer(parent);

      int w = parent.getWidth();
      int h = parent.getHeight();
      int nc = getColumns();
      int nr = getRows();
      int hgap = getHgap();
      int vgap = getVgap();

      Insets ins = parent.getInsets();

      double ppc = (double)(w - (nc - 1) * hgap - ins.left - ins.right) / nc;
      double ppr = (double)(h - (nr - 1) * vgap - ins.top - ins.bottom) / nr;

      Component[] comps = parent.getComponents();
      int iComp = 0;
      Point tl = new Point(ins.left, ins.top);
      Point br = new Point(ins.left + (int)Math.round(ppc), ins.top + (int)Math.round(ppr));
      for(int ir = 0; ir < nr; ir++)
      {
         // handle this row
         int comph = br.y - tl.y;
         for(int ic = 0; ic < nc; ic++)
         {
            if (iComp >= comps.length) return;
            comps[iComp++].setBounds(tl.x, tl.y, br.x - tl.x, comph);
            tl.x = br.x + hgap;
            br.x = ins.left + (int)Math.round((ic + 2) * (ppc + hgap)-hgap);
         }

         // reset for the next row
         tl.x = ins.left;
         tl.y = br.y + vgap;
         br.x = ins.left + (int)Math.round(ppc);
         br.y = ins.top + (int)Math.round((ir + 2) * (ppr + vgap)-vgap);
      }
   }
}
