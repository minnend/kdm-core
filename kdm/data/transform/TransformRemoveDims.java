package kdm.data.transform;

import java.util.*;

import kdm.data.*;
import kdm.util.*;

/**
 * Remove given dimensions from data
 */
public class TransformRemoveDims extends DataTransform
{
   MyIntList skip = new MyIntList();

   public void dumpParams()
   {
      System.err.printf("%s) skip: ", getClass());
      for(int i=0; i<skip.size(); i++) System.err.printf("%d ", skip.get(i));
      System.err.println();
   }
   
   public Sequence transform(Sequence _data)
   {
      Sequence data = new Sequence("RemDims: " + _data.getName(), _data.getFreq(), _data.getStartMS());
      int iDate = 0;
      int nDims = _data.getNumDims();

      int ndSrc = _data.getNumDims();
      boolean keep[] = new boolean[ndSrc];
      Arrays.fill(keep, true);
      for(int i = 0; i < skip.size(); i++)
      {
         int x = skip.get(i);
         if (x < 0) x = ndSrc + x;
         if (x >= 0 && x < ndSrc) keep[x] = false;
      }
      int ndLeft = 0;
      for(int i = 0; i < keep.length; i++)
         ndLeft += (keep[i] ? 1 : 0);
      assert (ndLeft > 0) : String.format("dimensions left: %d", ndLeft);

      int T = _data.length();
      for(int t = 0; t < T; t++)
      {
         FeatureVec fv = new FeatureVec(ndLeft);
         FeatureVec fvSrc = _data.get(t);
         int j = 0;
         for(int i = 0; i < ndSrc; i++)
            if (keep[i]) fv.set(j++, fvSrc.get(i));
         data.add(fv);
      }
      data.copyMeta(_data);
      return data;
   }

   public boolean config(ConfigHelper chelp, String sKey, String sVal)
   {
      if (sKey.equals("skip"))
      {
         StringTokenizer st = new StringTokenizer(sVal, ", ");
         while(st.hasMoreTokens()){
            String token = st.nextToken();
            int ix = token.indexOf(':');
               
            if (ix < 0){
               int a = Integer.parseInt(token);
               skip.add(a);
            }
            else{
               // could have one or two colons
               int a, b, step=1;
               a = Integer.parseInt(token.substring(0, ix));
               token = token.substring(ix+1);
               ix = token.indexOf(':');               
               if (ix < 0) b = Integer.parseInt(token);
               else{
                  step = Integer.parseInt(token.substring(0, ix));
                  b = Integer.parseInt(token.substring(ix+1));
               }
               for(int i=a; i<=b; i+=step) skip.add(i);
            }
         }
         return true;
      }
      return false;
   }
}
