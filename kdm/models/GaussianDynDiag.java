package kdm.models;

import kdm.data.*;

/**
 * multidimensional gaussian in which dimensions are assumed to be independent (ie, diagonal covariance
 * matrix) and parameters can be updated online.
 */
public class GaussianDynDiag extends GaussianDiagonal
{
   protected GaussianDyn1D[] gd1;
   
   public GaussianDynDiag(int nDims)
   {
      super(nDims);
      gd1 = new GaussianDyn1D[nDims];
      for(int d=0; d<nDims; d++) gd1[d] = new GaussianDyn1D();
      reset();
   }
   
   public int getN()
   {
      return gd1[0].getN();
   }

   /**
    * Reset the model and the underlying dataset. This will leave the model in an
    * undefined state until new data is added.
    */
   public void reset()
   {
      for(int d=0; d<nDims; d++) gd1[d].reset();
   }

   /**
    * Add a new data point to the underlying dataset and update model parameters.
    */
   public void add(FeatureVec x)
   {
      for(int d=0; d<nDims; d++) gd1[d].add(x.get(d), true);
   }

   /**
    * Add a new data point to the underlying dataset used to estimate this model. bUpdate
    * is true to update model parameters (may be faster to only update at end if many new
    * values are added).
    */
   public void add(FeatureVec x, boolean bUpdate)
   {
      for(int d=0; d<nDims; d++) gd1[d].add(x.get(d), false);
      if (bUpdate) update();
   }

   /**
    * Add several new data point to the udnerlying dataset and update model parameters.
    */
   public void add(FeatureVec data[])
   {
      for(int i = 0; i < data.length; i++)
         add(data[i], false);
      update();
   }

   /**
    * Update the mode parameters based on the underlying dataset
    */
   public void update()
   {
      for(int d=0; d<nDims; d++){
         gd1[d].update();
         setMean(d, gd1[d].getMean());
         setVar(d, gd1[d].getVar());
      }
   }
}
