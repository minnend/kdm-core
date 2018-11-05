package kdm.gui;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import javax.swing.*;
import kdm.util.*;

/** draws an image while maintaining the original aspect ratio */
public class ImageComp extends JMyComponent
{
   Image img;
   
   public ImageComp(Image img)
   {      
      setBackground(Color.black);
      this.img = img;
   }
   
   public ImageComp(File file)
   {
      setBackground(Color.black);
      if (file != null) img = Library.readImage(file.getAbsolutePath());
   }
   
   public ImageComp(String sFile)
   {
      this(new File(sFile));
   }
   
   public Dimension getPreferredSize()
   {
      int imgw = img.getWidth(this);
      int imgh = img.getHeight(this);
      return new Dimension(imgw, imgh);
   }

   @Override
   public void paintComponent(Graphics2D g, int w, int h)
   {
      g.setColor(getBackground());
      g.fillRect(0,0,w,h);
      
      if (img == null) return;
      
      int imgw = img.getWidth(this);
      int imgh = img.getHeight(this);
      
      int arw = w;
      int arh = (int)Math.round((double)arw*imgh/imgw);
      if (arh > h)
      {
         arh = h;
         arw = (int)Math.round((double)arh*imgw/imgh);           
      }
      
      int x = (w-arw)/2;
      int y = (h-arh)/2;
      
      g.drawImage(img, x, y, arw, arh, null);
   }

}
