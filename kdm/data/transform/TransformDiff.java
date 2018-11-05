package kdm.data.transform;

import kdm.data.*;
import java.util.*;

/** Calculate the first discrete derivative ([1..end] - [0..end-1]) */
public class TransformDiff extends DataTransform
{
   public void dumpParams()
   {
      System.err.printf("%s: no params\n", getClass());
   }

   public Sequence transform(Sequence _data)
   {
      Sequence data = new Sequence("Diff: " + _data.getName(), _data.getFreq(), _data.getStartMS());
      int iDate = 0;
      int nDims = _data.getNumDims();
      for(int i = 1; i < _data.length(); i++)
      {
         FeatureVec fv = _data.get(i).sub(_data.get(i - 1));
         data.add(fv);
      }
      return data;
   }
}
