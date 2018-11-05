package kdm.data.transform;

import java.util.*;
import kdm.data.*;
import org.apache.commons.math.stat.*;

/**
 * Normalize each dimension of the sequence to have zero mean and unit variance
 */
public class TransformZNorm extends DataTransform
{
   public void dumpParams()
   {
      System.err.printf("%s: no parameters\n", getClass());
   }
   
   public Sequence transform(Sequence _data)
   {
      double x[][] = _data.toSeqArray();
      FeatureVec fvMean = new FeatureVec(x.length);
      FeatureVec fvSDev = new FeatureVec(x.length);
      for(int i=0; i<x.length; i++)
      {
         fvMean.set(i, StatUtils.mean(x[i]));
         fvSDev.set(i, Math.sqrt(StatUtils.variance(x[i])));
      }      
      
      Sequence data = new Sequence("ZNorm: " + _data.getName(), _data.getFreq(), _data.getStartMS());
      for(int i = 1; i < _data.length(); i++)
      {
         FeatureVec fv = _data.get(i).sub(fvMean)._div(fvSDev);
         data.add(fv);
      }
      data.copyMeta(_data);
      return data;
   }
}
