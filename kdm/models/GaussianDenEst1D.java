package kdm.models;

import kdm.data.*;
import kdm.util.*;

import java.util.*;

/**
 * Allows kernel density estimation for 1D data using a Gaussian kernel
 */
public class GaussianDenEst1D extends ProbFVModel
{
   // TODO should rework to be a subclass of kde with kernel as a param
   protected double data[];
   protected double width = 1.0;

   public GaussianDenEst1D(double data[], double width)
   {
      super(1);
      this.data = data;
      this.width = width;
   }
   
   public GaussianDenEst1D(GaussianDenEst1D g)
   {
      super(1);
      copyFrom(g);
   }
   
   @Override
   public ProbFVModel dup()
   {
      return new GaussianDenEst1D(this);
   }

   @Override
   public void copyFrom(ProbFVModel model)
   {
      super.copyFrom(model);
      GaussianDenEst1D g = (GaussianDenEst1D)model;
      data = g.data.clone();
      width = g.width;
   }

   public boolean init(MyDoubleList adata)
   {
      data = adata.toArray();
      return true;
   }

   public boolean init(double _data[])
   {
      data = _data;
      return true;
   }

   public double eval(double vals[])
   {
      if (report == Report.loglik)
      {
         double x = 0.0;
         for(int i = 0; i < vals.length; i++)
            x += eval(vals[i]);
         return x;
      }
      else
      {
         double x = 1.0;
         for(int i = 0; i < vals.length; i++)
            x *= eval(vals[i]);
         return x;
      }

   }

   public double eval(double x)
   {
      double y = 0.0;
      double a = 1.0 / Math.sqrt(2.0 * Math.PI * width);
      double c = 1.0 / (-0.5 * width);
      for(int i = 0; i < data.length; i++)
      {
         double dx = x - data[i];
         double b = (dx * dx) * c;
         y += a * Math.exp(b);
      }
      y /= (double)data.length;
      return (report == Report.loglik ? Math.log(y) : y);
   }

   public double eval(FeatureVec x)
   {
      assert (x.getNumDims() == 1);
      return eval(x.get(0));
   }

   public FeatureVec sample()
   {
      FeatureVec fv = new FeatureVec(1);
      sample(fv);
      return fv;
   }

   public void sample(FeatureVec fv)
   {
      // TODO
      fv.set(0, Double.NEGATIVE_INFINITY);
      assert false : "not yet implemented";
   }

   public String toString()
   {
      return String.format("[Gaussian Density Estimator: width=%.2f  nData=%d]", width, data.length);
   }

   @Override
   public ProbFVModel construct(int nDims)
   {
      assert false : "can't construct a KDE without data";
      return null;
   }

   @Override
   public boolean learn(Sequence seq)
   {
      if (seq.getNumDims()!=1) return false;
      data = seq.toSeqArray()[0];
      return true;
   }

   @Override
   public boolean learn(Sequence seq, double[] weights)
   {      
      return false;
   }
}
