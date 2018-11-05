package kdm.data.transform;

import java.util.*;

import kdm.data.*;
import kdm.util.*;

public class TransformThreshold extends DataTransform
{
   protected FeatureVec fvMin, fvMax;

   public void dumpParams()
   {
      System.err.printf("%s:\n", getClass());
      System.err.printf(" Min: %s\n", fvMin);
      System.err.printf(" Max: %s\n", fvMax);
   }
   
   public Sequence transform(Sequence _data)
   {
       Sequence data = new Sequence("Thresh: "+_data.getName(),
                                    _data.getFreq(), _data.getStartMS());
       int iDate = 0;
       int nDims = _data.getNumDims();
       for(int i=0; i<_data.length(); i++)
       {
           FeatureVec fv = _data.get(i).min(fvMax).max(fvMin);
           data.add(fv);
       }
       data.copyMeta(_data);
       return data;
   }
   
   public boolean config(ConfigHelper chelp, String sKey, String sVal)
   {
      if (sKey.equals("min"))
      {
         StringTokenizer st = new StringTokenizer(sVal, ", ");
         int nd = st.countTokens();
         fvMin = new FeatureVec(nd);
         for(int i=0; i<nd; i++) fvMin.set(i, Double.parseDouble(st.nextToken()));
         return true;
      }
      else if (sKey.equals("max"))
      {
         StringTokenizer st = new StringTokenizer(sVal, ", ");
         int nd = st.countTokens();
         fvMax = new FeatureVec(nd);
         for(int i=0; i<nd; i++) fvMax.set(i, Double.parseDouble(st.nextToken()));
         return true;
      }
      return false;
   }
}
