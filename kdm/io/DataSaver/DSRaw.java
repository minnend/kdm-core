package kdm.io.DataSaver;

import kdm.data.*;
import kdm.util.*;
import java.io.*;

/**
 * Data saver the outputs data using the raw format (one sample per line, no meta-info).
 */
public class DSRaw extends DataSaver
{
   protected String sFormat = "%.6f ";
   
   public void setFormat(String sFormat)
   {
      char c = sFormat.charAt(sFormat.length()-1);
      if (c!=' ' && c!='\t') sFormat += " ";
      this.sFormat = sFormat;
   }
   
   @Override
   public boolean save(Sequence seq, String path)
   {
      try
      {
         PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(path)));

         int T = seq.length();
         for(int t = 0; t < T; t++)
         {
            FeatureVec fv = seq.get(t);
            int D = fv.getNumDims();
            for(int d = 0; d < D; d++) out.printf(sFormat, fv.get(d));
            out.println();
         }
         out.close();
      } catch (IOException e)
      {
         return false;
      }
      return true;
   }

}
