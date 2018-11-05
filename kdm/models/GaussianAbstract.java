package kdm.models;

import kdm.data.*;

public abstract class GaussianAbstract extends ProbFVModel
{  
   public GaussianAbstract(int nDims)
   {
      super(nDims);
   }

   /** Change the mean of this gaussian */
   public abstract void setMean(FeatureVec x);
   
   /** @return mean of this gaussian */
   public abstract FeatureVec getMean();
   
   /** @return diagonal elements of the covariance matrix */
   public abstract FeatureVec getCovDiag();
}
