package kdm.gui;

import java.awt.*;

/**
 * A layout manager that positions components vertically and forces each component to have
 * the maximum width.
 */
public class VerticalLayout implements LayoutManager
{
   protected int maxw = -1;
   protected int minw = -1;
   protected int vgap = 0;
   
   // TODO: should have a min=pref, but no max mode

   /**
    * Constructe a vertical layout with no constraints
    */
   public VerticalLayout()
   {}
   
   /**
    * Constructe a vertical layout with no constraints
    */
   public VerticalLayout(int fixedw)
   {
      this(fixedw, 0);
   }

   /**
    * Constructe a vertical layout with the given width
    */
   public VerticalLayout(int fixedw, int _vgap)
   {
      this(fixedw, fixedw, _vgap);
   }

   /**
    * Constructe a vertical layout with the given minimum and maximum width
    */
   public VerticalLayout(int _minw, int _maxw, int _vgap)
   {
      minw = _minw;
      maxw = _maxw;
      vgap = _vgap;
   }

   public void setMinWidth(int _minw)
   {
      minw = _minw;
   }

   public void setMaxWidth(int _maxw)
   {
      maxw = _maxw;
   }
   
   public void setVerticalGap(int _vgap)
   {
      vgap = _vgap;
   }

   public void addLayoutComponent(String name, Component comp)
   {}

   public void removeLayoutComponent(Component comp)
   {}

   public Dimension preferredLayoutSize(Container parent)
   {
      Insets ins = parent.getInsets();
      Dimension dim = new Dimension(0, 0);
      Component[] comps = parent.getComponents();
      for(int i = 0; i < comps.length; i++)
      {
         Dimension d = comps[i].getPreferredSize();
         if (d != null)
         {
            dim.width = Math.max(dim.width, d.width);
            dim.height += d.height;
         }
      }

      dim.width += ins.left + ins.right;
      dim.height += ins.top + ins.bottom;

      dim.height += vgap * (comps.length-1);
      
      if (minw >= 0) dim.width = Math.max(dim.width, minw);
      if (maxw >= 0) dim.width = Math.min(dim.width, maxw);
      
      return dim;
   }

   public Dimension minimumLayoutSize(Container parent)
   {
      Insets ins = parent.getInsets();
      Dimension dim = new Dimension(Integer.MAX_VALUE, 0);
      Component[] comps = parent.getComponents();
      for(int i = 0; i < comps.length; i++)
      {
         Dimension d = comps[i].getMinimumSize();
         dim.width = Math.max(dim.width, d.width);
         dim.height += d.height;
      }

      if (dim.width >= 0) dim.width += ins.left + ins.right;
      if (dim.height >= 0) dim.height += ins.top + ins.bottom;

      dim.height += vgap * (comps.length-1);
      
      if (maxw >= 0) dim.width = Math.min(dim.width, maxw);
      if (minw >= 0) dim.width = minw; 
      
      return dim;
   }

   public void layoutContainer(Container parent)
   {
      Insets ins = parent.getInsets();
      int insWidth = ins.left + ins.right;
      Component[] comps = parent.getComponents();
      int y = ins.top;
      Dimension dim = parent.getSize();

      for(int i = 0; i < comps.length; i++)
      {
         Dimension prefd = comps[i].getPreferredSize();
         if (prefd == null) prefd = new Dimension(100,24);
         comps[i].setBounds(ins.left, y, dim.width - insWidth, prefd.height);
         y += prefd.height + vgap;
      }
   }

}
