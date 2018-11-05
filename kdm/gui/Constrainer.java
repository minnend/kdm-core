package kdm.gui;

import java.awt.*;
import javax.swing.*;

/**
 * A container that only holds one child and constrains its size (min/max/fixed). 
 */
public class Constrainer extends JComponent
{
   protected int minw, maxw, minh, maxh;

   /**
    * Construct a constrainer with fixed dimensions (-1 for no constraint)
    * @param comp component to constrain
    * @param fixedw width of constrainer
    * @param fixedh height of constrainer
    */
   public Constrainer(JComponent comp, int fixedw, int fixedh)
   {
      this(comp, fixedw, fixedw, fixedh, fixedh);
   }

   /**
    * Construct a constrainer with min/max dimensions (-1 for no constraint)
    * @param comp component to constrain
    * @param minw minimum width of constrainer
    * @param maxw maximum width of constrainer
    * @param minh minimum height of constrainer
    * @param maxh maximum height of constrainer
    */
   public Constrainer(JComponent comp, int minw, int maxw, int minh, int maxh)
   {
      this.minw = minw;
      this.maxw = maxw;
      this.minh = minh;
      this.maxh = maxh;
      setLayout(null);
      if (comp!=null) add(comp);
   }

   public void doLayout()
   {
      Component[] comps = getComponents();
      if (comps==null || comps.length == 0) return;
      Dimension dim = getSize();
      comps[0].setBounds(0, 0, dim.width, dim.height);
   }

   public Dimension getMinimumSize()
   {
      return new Dimension(minw, minh);
   }

   public Dimension getPreferredSize()
   {
      Dimension prefd = new Dimension(100,100); // arbitrary default     
      Component[] comps = getComponents();
      if (comps!=null && comps.length > 0)
      {
         Component comp = comps[0];
         prefd = comp.getPreferredSize();         
      }
      if (minw>=0) prefd.width = Math.max(prefd.width, minw);
      if (maxw>=0) prefd.width = Math.min(prefd.width, maxw);
      if (minh>=0) prefd.height = Math.max(prefd.height, minh);
      if (maxh>=0) prefd.height = Math.min(prefd.height, maxh);
      return prefd;
   }

   public Dimension getMaximumSize()
   {
      return new Dimension(maxw > 0 ? maxw : Integer.MAX_VALUE, maxh > 0 ? maxh : Integer.MAX_VALUE);
   }
}
