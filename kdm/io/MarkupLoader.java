package kdm.io;

import kdm.data.*;
import kdm.util.*;
import java.io.*;
import java.util.*;

public abstract class MarkupLoader implements Configurable
{
   /** does this markup loader support multiple sequences per file (def = no)? */
   protected boolean bMultiple = false;
   protected ArrayList<MarkupSet> markupSets;   

   public MarkupLoader(boolean bMultiple)
   {
      this.bMultiple = bMultiple;
   }

   /**
    * @return first markup set in the given file. Call getMarkupSets() if this loader supports multiple sets
    *         per file.
    */
   public abstract MarkupSet load(String path);

   public boolean config(ConfigHelper chelp, String sKey, String sVal)
   {
      return false;
   }

   public boolean config(File fBase, String s)
   {
      // grrr: no multiple inheritance in java
      ConfigHelper chelp = new ConfigHelper(fBase);
      return chelp.config(s, this);
   }

   public ArrayList<MarkupSet> getMarkupSets()
   {
      return markupSets;
   }

   public boolean isMultipleRead()
   {
      return bMultiple;
   }
}
