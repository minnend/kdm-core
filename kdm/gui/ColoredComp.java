package kdm.gui;

import java.awt.*;

public class ColoredComp extends JMyComponent
{
   public ColoredComp(Color color)
   {
      setBackground(color);
      setOpaque(color.getAlpha() == 255);
   }

   @Override
   public void paintComponent(Graphics2D g, int w, int h)
   {
      g.setColor(getBackground());
      g.fillRect(0,0,w,h);
   }

}
