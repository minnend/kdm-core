package kdm.io.Def;

import java.util.*;
import java.io.*;

import kdm.util.*;

public class DefComp extends AbstractDef
{
   public static enum Location {
      Graph, Header, Window
   }

   public String name, gui, sData, params;
   public int width = 320;
   public int height = 100;
   public boolean bAbsHeight = false;
   public boolean bIgnore = false;
   public Location location = Location.Graph;
   public DefData data;
   public ArrayList<DefView> views;

   public DefComp(File fBase)
   {
      super(fBase);
      views = new ArrayList<DefView>();
   }

   public boolean init(String sKey, String sVal)
   {
      if (Library.stricmp(sKey, "name")) name = sVal;
      else if (Library.stricmp(sKey, "class")) gui = sVal;
      else if (Library.stricmp(sKey, "data")) sData = sVal;
      else if (Library.stricmp(sKey, "params")) params = sVal;
      else if (Library.stricmp(sKey, "ignore")) bIgnore = ConfigHelper.isTrueString(sVal);
      else if (Library.stricmp(sKey, "location"))
      {
         if (Library.stricmp(sVal, "graph") || Library.stricmp(sVal, "inline")) location = Location.Graph;
         else if (Library.stricmp(sVal, "header") || Library.stricmp(sVal, "top")) location = Location.Header;
         else if (Library.stricmp(sVal, "window"))
         {
            location = Location.Window;
            if (!bAbsHeight) height = (int)Math.round(height * Library.getScreenSize().getHeight() / 100.0);
         }
         else
         {
            System.err.printf("Error: invalid location (%s)\n", sVal);
            return false;
         }
      }
      else if (Library.stricmp(sKey, "height"))
      {
         if (sVal.endsWith("%"))
         {
            sVal = sVal.substring(0, sVal.length() - 1);
            bAbsHeight = false;
         }
         else bAbsHeight = true;
         try
         {
            height = Integer.parseInt(sVal);
            if (height < 1)
            {
               System.err.printf("Deprecated use of \"height = -1\" -- use \"location = window\"\n");
               System.err.printf(" and use \"width\" and \"height\" to specify the size of the window.\n");
               return false;
            }
            if (!bAbsHeight && location == Location.Window)
               height = (int)Math.round(height * Library.getScreenSize().getHeight() / 100.0);
         } catch (NumberFormatException nfe)
         {
            System.err.printf("Error: invalid height value: \"%s\"\n", sVal);
            return false;
         }
      }
      else if (Library.stricmp(sKey, "width"))
      {
         boolean bAbs = true;
         if (sVal.endsWith("%"))
         {
            sVal = sVal.substring(0, sVal.length() - 1);
            bAbs = false;
         }

         try
         {
            width = Integer.parseInt(sVal);
            if (width < 1) throw new NumberFormatException();
            if (!bAbs) width = (int)Math.round(width * Library.getScreenSize().getWidth() / 100.0);
         } catch (NumberFormatException nfe)
         {
            System.err.printf("Error: invalid width value: \"%s\"\n", sVal);
            return false;
         }
      }
      else
      {
         System.err.printf("Error%s: unknown component parameter: %s\n", name != null ? String.format(
               " (%s)", name) : "", sKey);
         return false;
      }
      return true;
   }

   public String toString()
   {
      return String.format("[Comp: %s, %s, %s |%d]", name, gui, sData, views.size());
   }
}
