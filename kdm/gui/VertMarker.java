package kdm.gui;

import java.awt.*;

/** Represents a vertical marker on a graph */
public class VertMarker
{
   public long ms;
   public Color color;
   public BasicStroke stroke;
   
   public VertMarker(long ms, Color color, float thick)
   {
      this.ms = ms;
      this.color = color;
      stroke = new BasicStroke(thick);
   }
}