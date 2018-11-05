package kdm.io.DataSaver;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import kdm.data.FeatureVec;
import kdm.data.Sequence;
import kdm.util.PrettyPrint;

/**
 * Saves data using a raw format with the timestamp (ms since the epoch) in the first column
 */
public class DSRawTimestamp extends DataSaver
{

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
            out.printf("%d ", seq.getTimeMS(t));
            int D = fv.getNumDims();            
            for(int d = 0; d < D; d++) out.printf("%.6f ", fv.get(d));
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
