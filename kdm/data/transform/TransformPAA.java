package kdm.data.transform;

import java.util.ArrayList;

import kdm.data.*;

/** compute the piecewise aggregate approximation (PAA) of the data */
public class TransformPAA extends DataTransform
{
   int nSegs;

   public void dumpParams()
   {
      System.err.printf("%s) # segments: %d\n", getClass(), nSegs);
   }
   
   /**
    * Create a PAA transform that will generate sequences with the given number of
    * segments
    * 
    * @param _nSegs desired number of segments in output sequence
    */
   public TransformPAA(int _nSegs)
   {
      nSegs = _nSegs;
   }

   public Sequence transform(Sequence _data)
   {      
      Sequence data = new Sequence("PAA: " + _data.getName(), _data.getFreq(), _data.getStartMS());
      int a = 0;
      int n = _data.length();
      int nd = _data.getNumDims();
      double v[][] = _data.toSeqArray();
      for(int i = 1; i <= nSegs; i++)
      {
         int b = (int)Math.round((double)n * i / nSegs);
         FeatureVec fv = new FeatureVec(nd);
         for(int d = 0; d < nd; d++)
         {
            double x = 0;
            for(int j = a; j < b; j++)
               x += v[d][j];
            x /= (double)(b - a);
            fv.set(d, x);
         }
         data.add(fv);
         a = b;
      }      
      return data;      
   }
}
