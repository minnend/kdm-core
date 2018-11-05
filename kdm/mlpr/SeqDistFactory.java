package kdm.mlpr;

import java.util.ArrayList;

import kdm.data.*;
import kdm.models.*;
import kdm.util.*;

/**
 * Factory class to create different kinds of sequence distance calculators
 */
public abstract class SeqDistFactory
{
   /**
    * @return distance after alignment via oates model, distance equals sum of squared
    *         differences
    */
   public static SeqDist createSSD()
   {
      return new SeqDist() {
         public double dist(Sequence a, Sequence b)
         {
            ArrayList<Sequence> train = new ArrayList<Sequence>();
            train.add(a);
            train.add(b);
            OatesModelUSamp oates = new OatesModelUSamp(train, Library.min(a.length(), b.length()));
            OatesMapping amap = oates.align(a);
            assert (amap != null) : String.format("a=%s (%d)  oates=%d\n", a, a.length(), oates.length());
            OatesMapping bmap = oates.align(b);
            assert (bmap != null) : String.format("b=%s (%d)  oates=%d\n", b, b.length(), oates.length());
            FeatureVec fvd = FeatureVec.zeros(a.getNumDims());
            for(int i = 0; i < oates.length(); i++)
            {
               FeatureVec fva = a.get(amap.imap[i]);
               FeatureVec fvb = b.get(bmap.imap[i]);
               FeatureVec fv = fva.sub(fvb);
               fvd._add(fv._sqr());
            }
            return fvd.sum();
         }
      };
   }

   /**
    * @return distance after alignment via oates model, distance equals sum of squared
    *         differences normalized for model length
    */
   public static SeqDist createNormSSD()
   {
      return new SeqDist() {
         public double dist(Sequence a, Sequence b)
         {
            ArrayList<Sequence> train = new ArrayList<Sequence>();
            train.add(a);
            train.add(b);
            OatesModelUSamp oates = new OatesModelUSamp(train, Library.min(a.length(), b.length()));
            OatesMapping amap = oates.align(a);
            assert (amap != null) : String.format("a=%s (%d)  oates=%d\n", a, a.length(), oates.length());
            OatesMapping bmap = oates.align(b);
            assert (bmap != null) : String.format("b=%s (%d)  oates=%d\n", b, b.length(), oates.length());
            FeatureVec fvd = FeatureVec.zeros(a.getNumDims());
            for(int i = 0; i < oates.length(); i++)
            {
               FeatureVec fva = a.get(amap.imap[i]);
               FeatureVec fvb = b.get(bmap.imap[i]);
               FeatureVec fv = fva.sub(fvb);
               fvd._add(fv._sqr());
            }
            return fvd.sum() / oates.length();
         }
      };
   }
}
