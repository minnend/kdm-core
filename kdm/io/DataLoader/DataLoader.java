package kdm.io.DataLoader;

import java.util.*;
import java.io.*;
import kdm.data.*;
import kdm.util.*;

/**
 * Abstract base class for other classes that load data from different file formats
 */
public abstract class DataLoader implements Configurable
{
   protected Calendar calStart = Library.date2cal(new Date(0));
   protected int version = 1;

   public abstract Sequence load(String path);

   public void setStarTime(Calendar cal){ calStart = cal; }
   
   public boolean config(ConfigHelper chelp, String sKey, String sVal)
   {
      if (Library.stricmp(sKey, "ver")) version = Integer.parseInt(sVal); 
      else{
          System.err.printf("%s) Error: unknown setup key (%s)\n", getClass().getName(), sKey);
          if (sKey.startsWith("\"")) System.err.println("  fyi: parameters are specified as '<key>=\"<value>\"' not '\"<key>=<value>\"'");
          return false;
      }
      return true;
   }

   public boolean config(File fBase, String s)
   {
      // grrr: no multiple inheritance in java
      ConfigHelper chelp = new ConfigHelper(fBase);
      return chelp.config(s, this);
   }
}
