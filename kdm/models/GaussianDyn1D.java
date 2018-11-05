package kdm.models;

import kdm.data.*;

/**
 * 1D gaussian model that can be updated online (i.e., parameters are estimated
 * incrementally)
 */
public class GaussianDyn1D extends Gaussian1D
{
   protected double sx = 0; // sum of x
   protected double sx2 = 0; // sum of x^2
   protected double n = 0; // "number" of data points; can be a real number due
   // to fractional membership (e.g., in an EM algorithm)
   protected boolean bUnbiased = true;

   public GaussianDyn1D()
   {
      reset();
   }

   public GaussianDyn1D(double _mean, double _var)
   {
      super(_mean, _var);
      sx2 = mean * mean;
      n = 1;
      bUnbiased = true;
   }

   /**
    * Construct a 1D Gaussian from data using an unbiased variance estimator
    */
   public GaussianDyn1D(double data[])
   {
      this(data, true);
   }

   public GaussianDyn1D(double data[], boolean _bUnbiased)
   {
      bUnbiased = _bUnbiased;
      reset();
      add(data);
   }

   public GaussianDyn1D(GaussianDyn1D gmd)
   {
      copyFrom(gmd);
   }

   public void copyFrom(GaussianDyn1D gmd)
   {
      super.copyFrom((Gaussian1D)gmd);
      sx = gmd.sx;
      sx2 = gmd.sx2;
      n = gmd.n;
      bUnbiased = gmd.bUnbiased;
   }

   public int getN()
   {
      return (int)n;
   }

   public void setUnbiased(boolean _bUnbiased)
   {
      bUnbiased = _bUnbiased;
      update();
   }

   public boolean getUnbiased()
   {
      return bUnbiased;
   }

   /**
    * Reset the model and the underlying dataset. This will leave the model in an
    * undefined state until new data is added.
    */
   public void reset()
   {
      sx = 0.0;
      sx2 = 0.0;
      n = 0;
      mean = 0.0;
      var = 1.0;
      sdev = 1.0;
   }

   /**
    * Add a new data point to the underlying dataset and update model parameters.
    */
   public void add(double x)
   {
      add(x, true);
   }

   /**
    * Add a new data point to the underlying dataset used to estimate this model. bUpdate
    * is true to update model parameters (may be faster to only update at end if many new
    * values are added).
    */
   public void add(double x, boolean bUpdate)
   {
      sx += x;
      sx2 += x * x;
      n++;
      if (bUpdate) update();
   }

   /**
    * Add several new data point to the udnerlying dataset and update model parameters.
    */
   public void add(double data[])
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
      if (n < 1) reset();
      else if (n == 1)
      {
         mean = sx;
         var = minVar;
      }
      else
      {
         mean = sx / n;
         if (bUnbiased) var = (sx2 - mean * sx) / (n - 1.0);
         else var = sx2 / n - mean * mean;
      }
      super.update();
   }
}
