package kdm.util;

import java.util.*;
import java.io.*;

/**
 * Helper class for configurable objects
 */
public class ConfigHelper
{
   protected File fBase;

   public ConfigHelper()
   {
      this(null);
   }

   public ConfigHelper(File _fBase)
   {
      fBase = _fBase;
   }

   public File getBasePath()
   {
      return fBase;
   }

   /** @return true if the string represents some form of "true" */
   public final static boolean isTrueString(String val)
   {
      return (Library.stricmp(val, "true") || Library.stricmp(val, "t") || Library.stricmp(val, "yes")
            || Library.stricmp(val, "y") || Library.stricmp(val, "1"));
   }
   
   public boolean config(String s, Configurable obj)
   {
      HashMap<String, String> map = parseConfig(s);
      if (map == null) return true;
      for(Map.Entry<String, String> x : map.entrySet())
      {
         String key = x.getKey();
         String val = x.getValue();
         if (!obj.config(this, key, val)) return false;
      }
      return true;
   }

   /**
    * Parses a config string into a map of key/value pairs. The format for a config string includes pairs of
    * keys and values where each value is quoted, and equals sign separates they key and its value, and white
    * space is allowed anywhere. Both keys and values will be trimmed of leading and trailing white space. For
    * example: [name="foo \"bar\"" size = "8" max length =" 9 "] would create a map with these entries: [name] ->
    * [foo "bar"] [size] -> [8] [max length] -> [9]
    */
   public HashMap<String, String> parseConfig(String _s)
   {
      if (_s == null) return null;
      final int KEY = 2;
      final int EQ = 3;
      final int VAL = 4;

      char[] s = _s.toCharArray();
      int i = 0;
      int n = s.length;
      int state = KEY;
      String sKey = null, sVal = null;
      HashMap<String, String> map = new HashMap<String, String>();

      while(i < n)
      {
         switch(state){
         case KEY: {
            int j = i;
            while(i < n && s[i] != '=')
               i++;
            sKey = new String(s, j, i - j).trim();
            // System.err.println("key: "+sKey);
            state = EQ;
            break;
         }
         case EQ: {
            assert s[i] == '=';
            i++;
            while(i < n && Character.isWhitespace(s[i])) i++;
            state = VAL;
            break;
         }
         case VAL: {
            boolean bQuoted = (s[i] == '"');
            if (bQuoted) i++;
            StringBuffer sb = new StringBuffer();
            while(i < n)
            {
               if ((bQuoted && s[i] == '"') || (!bQuoted && Character.isWhitespace(s[i]))) break;               
               if (i + 1 < n && s[i] == '\\' && s[i + 1] == '"') sb.append(s[++i]);
               else sb.append(s[i++]);
            }
            sVal = sb.toString().trim();
            // System.err.println("val: "+sVal);
            String sPrev = map.get(sKey);
            if (sPrev != null) map.put(sKey, sPrev + "," + sVal);
            else map.put(sKey, sVal);
            i++;
            state = KEY;
            break;
         }
         }
      }
      
      if (map.isEmpty()) System.err.printf("Warning: no parameters found (%s)\n", _s);
      return map;
   }
}
