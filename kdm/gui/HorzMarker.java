package kdm.gui;

import java.awt.*;

/** Represents a horizontal marker on a graph */
public class HorzMarker
{
   public double value;
   public Color color;
   public BasicStroke stroke;
   
   public HorzMarker(double value, Color color, float thick)
   {
      this.value = value;
      this.color = color;
      stroke = new BasicStroke(thick);
   }
}