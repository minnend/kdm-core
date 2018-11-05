package kdm.metrics;

import kdm.data.FeatureVec;

/** Euclidean distance metric (L_2 norm) */
public class EuclideanFV extends MetricFV
{
   protected boolean bRoot;

   /** Calculate the true L_2 norm */
   public EuclideanFV()
   {
      this(true);
   }

   /** Calculate the full (bRoot=true) or squared (bRoot=false) distance */
   public EuclideanFV(boolean bRoot)
   {
      this.bRoot = bRoot;
   }

   @Override
   public double dist(FeatureVec a, FeatureVec b)
   {
      double d = a.dist2(b);
      if (bRoot) return Math.sqrt(d);
      else return d;
   }
}
