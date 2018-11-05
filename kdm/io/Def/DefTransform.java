package kdm.io.Def;

import kdm.data.transform.*;
import kdm.util.*;

import java.io.*;

public class DefTransform extends AbstractDef
{
   protected String name, sClass, params;
   public DataTransform trans;
   
   public DefTransform(File fBase)
   {
      super(fBase);
   }
   
   public DefTransform(File fBase, String sBlock)
   {
      this(fBase);
      if (!init(sBlock))
      {
         System.err.println("Failed to initialize transform block");
         System.exit(1);
      }
   }
         
   public boolean init(String sBlock)
   {
      if (super.init(sBlock))
      {
         try
         {
            Class cls = Library.getClass(sClass, "kdm.data.transform");
            trans = (DataTransform)cls.newInstance();
            if (!trans.config(fBase, params))
            {
               System.err.println("Error: failed to configure transformation!");
               System.err.println(" class = " + sClass);
               System.err.println(" params = \"" + params + "\"");
               System.exit(1);
            }
         } catch (Exception e)
         {
            e.printStackTrace();
            return false;
         }
      }
      else{
         System.err.println("Failed to initialize transform");
         return false;
      }
      return true;
   }

   public boolean init(String sKey, String sVal)
   {
      if (Library.stricmp(sKey, "name")) name = sVal;
      else if (Library.stricmp(sKey, "class")) sClass = sVal;
      else if (Library.stricmp(sKey, "params")) params = sVal;
      else super.init(sKey, sVal);
      return true;
   }
}
