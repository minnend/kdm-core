package kdm.models;

import kdm.data.*;
import kdm.models.ProbFVModel.Report;
import kdm.util.*;
import eduni.distributions.*;
import org.apache.commons.math.stat.*;

/**
 * N-dimensional Guassian distributation assuming a fixed variance across all dimensions (i.e., spherical
 * distribution)
 */
public class GaussianSpherical extends GaussianAbstract
{
   protected FeatureVec mean;
   protected double var, sdev;

   public GaussianSpherical(int nDims)
   {
      super(nDims);
      mean = new FeatureVec(nDims);
      var = sdev = 1.0;
   }

   public GaussianSpherical(GaussianSpherical g)
   {
      super(g.getNumDims());
      copyFrom(g);
   }
   
   /** @return spherical gaussian with zero mean and unit variance */ 
   public static final GaussianSpherical eye(int nDims)
   {
      return new GaussianSpherical(nDims);
   }

   @Override
   public ProbFVModel dup()
   {
      return new GaussianSpherical(this);
   }
   
   @Override
   public void copyFrom(ProbFVModel model)
   {
      super.copyFrom(model);
      GaussianSpherical g = (GaussianSpherical)model;
      mean = new FeatureVec(g.getMean());
      setVar(g.getVar());
   }
   
   @Override
   public void setMean(FeatureVec x)
   {
      assert(mean.getNumDims() == x.getNumDims());
      mean = new FeatureVec(x);
   }

   public double getVar(){ return var; }
   
   public void setVar(double var)
   {
      this.var = var; 
      sdev = Math.sqrt(var);
   }

   public void setMean(int i, double x)
   {
      mean.set(i, x);
   }

   public FeatureVec getMean()
   {
      return mean;
   }

   /**
    * @return log-likelihood of the data point given the model
    */
   public double eval(FeatureVec x)
   {
      assert (x.getNumDims() == nDims);

      double alpha = -Math.log(Library.SQRT_2PI * sdev);
      double ret = nDims * alpha;      
      for(int i = 0; i < nDims; i++)
      {
         double dx = x.get(i) - mean.get(i);
         ret += -0.5*dx*dx/var;
      }
      
      if (report==Report.loglik) return ret;
      else if (report == Report.prob) return Math.exp(ret);
      else{
         assert false;
         return Double.NaN;
      }
   }

   /**
    * Sets the given feature vector to a random sample from this distribution.
    */
   public void sample(FeatureVec fv)
   {
      assert (fv.getNumDims() == nDims);
      Normal normal = new Normal(0, var);
      for(int i = 0; i < nDims; i++)
         fv.set(i, normal.sample() + mean.get(i));
   }
   
   @Override
   public ProbFVModel construct(int nDims)
   {
      return new GaussianSpherical(nDims);
   }
   
   @Override
   public boolean learn(Sequence seq)
   {
      assert (seq.getNumDims() == nDims);
      mean = seq.getMean();      
      int T = seq.length();
      double x[] = new double[T*nDims];
      int i=0;
      for(int t=0; t<T; t++)
      {
         FeatureVec fv = seq.get(t);
         for(int d=0; d<nDims; d++)
            x[i++] = fv.get(d)-mean.get(d);
      }
      setVar(StatUtils.variance(x, 0));
      return true;
   }

   @Override
   public boolean learn(Sequence seq, double[] weights)
   {
      assert (seq.getNumDims() == nDims);
      int T = seq.length();
      assert (T == weights.length);

      double[][] data = seq.toSeqArray();
      
      // calc weighted mean for each dimension
      mean = new FeatureVec(nDims);
      for(int d=0; d<nDims; d++)
         mean.set(d, Library.mean(weights, data[d]));
               
      // calc weighted variance
      double[] dx = new double[T*nDims];
      double[] w = new double[T*nDims];
      int i=0;
      for(int t=0; t<T; t++)
      {
         FeatureVec fv = seq.get(t);
         for(int d=0; d<nDims; d++)
         {
            w[i] = weights[t];
            dx[i++] = fv.get(d) - mean.get(d);
         }
      }
      setVar(Library.var(w, dx, 0));
      
      return true;
   }

   @Override
   public FeatureVec getCovDiag()
   {
      FeatureVec v = FeatureVec.ones(nDims);
      return v._mul(var);
   }
}
