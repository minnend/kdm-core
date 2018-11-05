package kdm.data.transform;

import java.util.*;
import kdm.data.*;

/**
 * Create a 1D sequence representing the RMS of the given sequences. The RMS is the square
 * root of the mean of the sum of the squares of the values of the sequence. So RMS =
 * (sum(x^2)/D)^(1/2), where D = # dimensions.
 */
public class TransformRMS extends DataTransform
{
   public void dumpParams()
   {
      System.err.printf("%s: no parameters\n", getClass());
   }
   public Sequence transform(Sequence _data)
   {
      Sequence data = new Sequence("RMS: " + _data.getName(), _data.getFreq(), _data.getStartMS());
      int iDate = 0;
      int nDims = _data.getNumDims();
      for(int i = 1; i < _data.length(); i++)
      {
         FeatureVec fv = new FeatureVec(1, Math.sqrt(_data.get(i).sqr().sum() / nDims));
         data.add(fv);
      }
      data.copyMeta(_data);
      return data;
   }
}
