package kdm.gui;

import java.awt.*;
import javax.swing.*;

/** panel useful in JScrollPane that should only scroll vertically */
public class VerticalScrollPanel extends JPanel implements Scrollable
{
   protected int nInc, nBlock;

   public VerticalScrollPanel()
   {
      this(1, 10);
   }

   public VerticalScrollPanel(LayoutManager lm)
   {
      this(1, 10, lm);
   }

   public VerticalScrollPanel(int _nInc, int _nBlock)
   {
      nInc = _nInc;
      nBlock = _nBlock;
   }

   public VerticalScrollPanel(int _nInc, int _nBlock, LayoutManager lm)
   {
      super(lm);
      nInc = _nInc;
      nBlock = _nBlock;
   }

   public void setInc(int x){ nInc = x; }
   public void setBlock(int x){ nBlock = x; }
   
   public Dimension getPreferredScrollableViewportSize()
   {
      return getPreferredSize();
   }

   public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
   {
      return nBlock;
   }

   public boolean getScrollableTracksViewportHeight()
   {
      return false;
   }

   public boolean getScrollableTracksViewportWidth()
   {
      return true;
   }

   public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
   {
      return nInc;
   }
}
