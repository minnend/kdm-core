package kdm.data.transform;

import kdm.data.*;
import java.util.*;

/** transform the data according to f(x) = sign(x)*log(|x|+1) */
public class TransformLog extends DataTransform
{
   public void dumpParams()
   {
      System.err.printf("%s:\n", getClass());
   }

   public Sequence transform(Sequence _data)
   {
      Sequence data = new Sequence("Log: " + _data.getName(), _data.getFreq(), _data.getStartMS());
      int iDate = 0;
      int nDims = _data.getNumDims();
      for(int i = 0; i < _data.length(); i++)
      {
         FeatureVec _fv = _data.get(i);
         FeatureVec fv = new FeatureVec(nDims);
         for(int j = 0; j < nDims; j++)
         {
            double x = _fv.get(j);
            fv.set(j, Math.signum(x) * Math.log(Math.abs(x) + 1));
         }
         data.add(fv);
      }
      data.copyMeta(_data);
      return data;
   }
}
