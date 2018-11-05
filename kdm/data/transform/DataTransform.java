package kdm.data.transform;

import kdm.data.*;
import kdm.util.*;
import java.io.*;

/**
 * Abstract base class that allows transforming one sequence into a new sequence
 */
public abstract class DataTransform implements Configurable
{
   public abstract Sequence transform(Sequence data);

   public boolean config(ConfigHelper chelp, String sKey, String sVal)
   {
      return false;
   }
   
   /** output information about parameter settings */
   public abstract void dumpParams();

   public boolean config(File fBase, String s)
   {
      // grrr: no multiple inheritance in java
      ConfigHelper chelp = new ConfigHelper(fBase);
      return chelp.config(s, this);
   }
}
