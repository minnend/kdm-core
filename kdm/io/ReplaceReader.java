package kdm.io;

import java.io.*;
import java.util.*;

/**
 * This reader replaces text of the form $(<key>) with prespecified values.
 * 
 * example: text = "my path = $(base)/foo.html map contains: "base" -> "/web/test" output =
 * "my path = /web/test/foo.html"
 */
public class ReplaceReader extends Reader implements MyReader
{
   protected boolean bBuf = true;
   protected Reader reader = null;
   protected Set<Map.Entry<String, String>> reps;

   /**
    * Reads text via the specified reader and transforms characters of the form "$(<key>)"
    * into the appropriate value as specified by the string map. Note that the keys in the
    * map hold only the <key> and not the "$" or parens.
    */
   public ReplaceReader(Reader _reader, AbstractMap<String, String> map)
   {
      if (_reader == null) reader = null;
      else
      {
         if (_reader instanceof MyReader)
         {
            bBuf = false;
            reader = _reader;
         }
         else if (_reader instanceof BufferedReader) reader = _reader;
         else reader = new BufferedReader(_reader);
      }
      reps = map.entrySet();
   }

   public String readFile() throws IOException
   {
      StringBuffer sb = new StringBuffer();
      String line;
      while((line = readLine()) != null)
      {
         sb.append(line);
         sb.append('\n');
      }
      return sb.toString();
   }

   public String readLine() throws IOException
   {
      while(true)
      {
         String line = null;
         if (bBuf) line = ((BufferedReader)reader).readLine();
         else line = ((MyReader)reader).readLine();
         if (line == null) return null;
         line = replace(line);
         if (line.length() > 0) return line;
      }
   }

   protected String replace(String line)
   {
      StringBuffer sb = new StringBuffer(line);
      boolean bFound = true;
      while(bFound)
      {
         bFound = false;
         Iterator<Map.Entry<String, String>> it = reps.iterator();
         while(it.hasNext())
         {
            Map.Entry<String, String> entry = it.next();
            String sSearch = "$(" + entry.getKey() + ")";
            int n = sSearch.length();
            int m = entry.getValue().length();
            int j, i = 0;
            while((j = sb.indexOf(sSearch, i)) >= 0)
            {
               sb.replace(j, j + n, entry.getValue());
               i = j + m;
            }
         }
      }
      return sb.toString();
   }

   public int read(char[] cbuf, int off, int len)
   {
      assert false : "not yet implemented";
      return 0;
   }

   public void close() throws IOException
   {
      reader.close();
   }

   public static String replace(String s, AbstractMap<String, String> map)
   {
      ReplaceReader reader = new ReplaceReader(null, map);
      return reader.replace(s);
   }
}
