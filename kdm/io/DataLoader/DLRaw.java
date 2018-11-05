package kdm.io.DataLoader;

import kdm.data.*;
import kdm.util.*;
import java.util.*;
import java.io.*;

/**
 * Loads data from a raw file with one time step per line. The data is assumed to be
 * sampled at 1Hz.
 */
public class DLRaw extends DataLoader
{
   protected String delims;
   protected double freq = 1.0;

   public DLRaw()
   {
      delims = " \t\r\n,;:";
   }

   public DLRaw(String _delims)
   {
      delims = _delims;
   }

   public boolean config(ConfigHelper chelp, String key, String val)
   {
      if (Library.stricmp(key, "delims"))
      {
         delims = val;
      }
      else if (Library.stricmp(key, "freq"))
      {
         freq = Double.parseDouble(val);
      }
      else super.config(chelp, key, val);
      return true;
   }

   public Sequence load(String path)
   {
      LineNumberReader in = null;
      try
      {
         File file = new File(path);
         in = new LineNumberReader(new FileReader(file));
         String line;
         Sequence data = null;
         double v[] = null;
         StringTokenizer st;
         int nBadLines = 0;

         // System.err.println("Loading data file (DLRaw): "+path);

         while((line = in.readLine()) != null)
         {
            // skip empty lines
            if (line.length() == 0) continue;

            // setup string tokenizer
            st = new StringTokenizer(line, delims);

            // figure out the dimensionality of the data if this is the first line
            int nTokens = st.countTokens();
            if (v == null) v = new double[nTokens];
            else if (nTokens < v.length)
            {
               nBadLines++;
               if (nBadLines > 1)
               {
                  System.err.printf("Error: too many incomplete lines (%d)\n (%s)\n", nBadLines, path);
                  return null;
               }
               else continue;
            }

            // parse this line's data
            for(int i = 0; i < v.length; i++)
               v[i] = Double.parseDouble(st.nextToken());
            st = null;

            // add to the dataset (and create it if necessary)
            if (data == null)
            {
               if (calStart != null) data = new Sequence(path+" (Raw)", freq, calStart.getTimeInMillis());
               else data = new Sequence(path+" (Raw)", freq);
            }
            data.add(new FeatureVec(v));
         }
         data.setOrigFile(path);
         in.close();
         return data;
      } catch (Exception e)
      {
         System.err.println("Error: unable to open data file\n (" + path + ")");
         return null;
      }
   }
}
