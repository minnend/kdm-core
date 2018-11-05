package kdm.io.Def;

import java.util.*;
import java.io.*;

/**
 * Abstract base class of all definitio blocks
 */
public abstract class AbstractDef
{
   protected File fBase;
   
   public AbstractDef(File _fBase)
   {
      fBase= _fBase;
   }
   
   protected HashMap<String, String> parseBlock(String sBlock)
   {
      HashMap<String, String> map = new HashMap<String, String>();

      StringTokenizer st = new StringTokenizer(sBlock, "\r\n");
      while(st.hasMoreTokens())
      {
         String line = st.nextToken();
         int i = line.indexOf('=');
         if (i < 0)
         {
            System.err.println("Error: no = sign found!");
            System.err.println(" line: " + line);
            return null;
         }
         String sKey = line.substring(0, i).trim();
         String sVal = line.substring(i + 1).trim();
         String sPrev = map.get(sKey);
         if (sPrev != null) map.put(sKey, sPrev+","+sVal);
         else map.put(sKey, sVal);
      }
      return map;
   }

   public boolean init(String sBlock)
   {
      HashMap<String, String> map = parseBlock(sBlock);
      if (map == null) return false;
      for(Map.Entry<String, String> x : map.entrySet())
      {
         String key = x.getKey();
         String val = x.getValue();         
         if (!init(key, val)) return false;
      }
      return true;
   }

   public boolean init(String sKey, String sVal)
   {
      // no base parameters
      System.err.printf("Error (%s): unrecognized config key (%s)\n", getClass().getName(), sKey);
      return false;
   }
   
   public File getBasePath(){ return fBase; }
}
