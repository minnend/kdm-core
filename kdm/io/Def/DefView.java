package kdm.io.Def;

import java.awt.Color;
import java.io.*;
import kdm.util.*;

public class DefView extends AbstractDef
{
   public int dim;
   public Color color;

   public DefView(File fBase)
   {
      super(fBase);
   }
   
   public DefView(File fBase, String sBlock)
   {
      this(fBase);
      if (!init(sBlock))
      {
         System.err.println("Failed to initialize view block");
         System.exit(1);
      }
   }

   public boolean init(String sKey, String sVal)
   {
      if (Library.stricmp(sKey, "dim")) dim = Integer.parseInt(sVal) - 1;
      else if (Library.stricmp(sKey, "color"))
      {
         int r = Integer.parseInt("" + sVal.charAt(0), 16) * 17;
         int g = Integer.parseInt("" + sVal.charAt(1), 16) * 17;
         int b = Integer.parseInt("" + sVal.charAt(2), 16) * 17;
         color = new Color(r, g, b);
      }
      else{
         System.err.printf("Error: unknown view parameters: %s\n", sKey);
         return false;
      }
      return true;
   }

   public String toString()
   {
      return "[View: " + dim + " " + color + "]";
   }
}
