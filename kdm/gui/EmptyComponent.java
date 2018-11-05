package kdm.gui;

import java.awt.*;
import javax.swing.*;

/**
 * An empty JComponent that is transparent and does not draw anything; it's useful in
 * certain layouts when you need to put a component in a spot, but you actually want it to
 * be blank.
 */
public class EmptyComponent extends JComponent
{

   public EmptyComponent()
   {
      setOpaque(false);
      setDoubleBuffered(false);
   }

   public void paintComponent(Graphics g)
   {}

}
