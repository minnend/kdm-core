package kdm.metrics;

import kdm.data.FeatureVec;

/** abstract class representing a metric between (feature) vectors */
public abstract class MetricFV
{
   private FeatureVec fva, fvb;

   /** @return distance between the two given vectors */
   public abstract double dist(FeatureVec a, FeatureVec b);

   /** @return distance between x and y (1D vectors) */ 
   public double dist(double x, double y)
   {
      if (fva == null) // lazy construction
      {         
         fva = new FeatureVec(1);
         fvb = new FeatureVec(1);
      }
      
      fva.set(0, x);
      fvb.set(0, y);
      return dist(fva, fvb);
   }
}
