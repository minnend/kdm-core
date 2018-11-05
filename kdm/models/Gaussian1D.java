package kdm.models;

import kdm.data.*;
import kdm.util.*;
import kdm.io.*;
import org.apache.commons.math.stat.*;
import eduni.distributions.*;
import java.util.*;
import java.io.*;

/**
 * 1D Gaussian model
 */
public class Gaussian1D extends ProbFVModel
{
   protected double mean;
   protected double var;
   protected double sdev;
   protected double probCoef, loglikCoef;
   protected double minVar = 1e-9;
   transient protected Normal normal = null;

   public final static double sqrt_2pi = Math.sqrt(2.0 * Math.PI);

   /**
    * Construct a standard normal Gaussian
    */
   public Gaussian1D()
   {
      this(0, 1);
   }

   /**
    * Construct a 1D Gaussian with the specified mean and variance
    */
   public Gaussian1D(double _mean, double _var)
   {
      super(1);
      mean = _mean;
      var = _var;
      update();
   }

   /**
    * Construct a 1D Gaussian with the specified mean and variance
    */
   public Gaussian1D(double _mean, double _var, double _minVar)
   {
      super(1);
      mean = _mean;
      var = _var;
      minVar = _minVar;
      update();
   }

   /**
    * Construct a 1D Gaussian from data stored in a double array
    */
   public Gaussian1D(double data[])
   {
      super(1);
      learn(data);
   }

   /**
    * Construct a 1D Gaussian from data stored in an ArrayList
    */
   public Gaussian1D(MyDoubleList adata)
   {
      super(1);
      learn(adata);
   }

   /**
    * Construct a 1D Gaussian from data stored in a double array where each data point has
    * an associated weight.
    */
   public Gaussian1D(double data[], double weight[])
   {
      super(1);
      learn(data, weight);
   }

   /**
    * Construct a 1D Gaussian from data stored in an ArrayList where each data point has
    * an associated weight.
    */
   public Gaussian1D(MyDoubleList adata, MyDoubleList aweight)
   {
      super(1);
      learn(adata, aweight);
   }

   public Gaussian1D(Gaussian1D gm)
   {
      super(1);
      copyFrom(gm);
   }

   @Override
   public ProbFVModel dup()
   {
      return new Gaussian1D(this);
   }
   
   @Override
   public void copyFrom(ProbFVModel model)
   {
      super.copyFrom(model);
      Gaussian1D gm = (Gaussian1D)model; 
      mean = gm.mean;
      var = gm.var;
      sdev = gm.sdev;
      probCoef = gm.probCoef;
      loglikCoef = gm.loglikCoef;
      minVar = gm.minVar;
   }

   public boolean learn(MyDoubleList adata)
   {
      return learn(adata.toArray());
   }

   public boolean learn(MyDoubleList adata, MyDoubleList aweight)
   {
      return learn(adata.toArray(), aweight.toArray());
   }

   public boolean learn(double data[])
   {
      assert data.length > 0 : "need data to learn Gaussian params";

      if (data.length == 1)
      {
         mean = data[0];
         var = 1.0;
      }
      else{
         mean = StatUtils.mean(data);
         var = StatUtils.variance(data);
      }
      update();
      return true;
   }

   /**
    * Estimate the parameters (mean and variance) from the given weighted data
    * 
    * @param data the data used for parameter estimation
    * @param weight weight of each data point [0..1]
    * @return true if successful
    */
   public boolean learn(double data[], double weight[])
   {
      return learn(data, weight, new SpanList(0, data.length - 1, true));
   }

   /**
    * Estimate the parameters (mean and variance) from the given weighted data
    * 
    * @param data the data used for parameter estimation
    * @param weight weight of each data point [0..1]
    * @param span which data points to include (implicitly, all others have weight of
    *           zero)
    * @return true if successful
    */
   public boolean learn(double data[], double weight[], SpanList span)
   {
      assert data.length > 1;
      assert data.length == weight.length;

      double a, b, x, y, wsum;
      double n = span.size();

      // first calc the mean
      wsum = 0.0;
      mean = 0.0;
      span.itReset();
      while(span.itMore())
      {
         int i = span.itNext();
         assert (weight[i] >= 0.0) : "i=" + i + "  weight=" + weight[i];
         mean += weight[i] * data[i];
         wsum += weight[i];
      }
      if (wsum == 0.0)
      {
         System.err.println("wsum: " + wsum + "  ----------------------------------------");
         return false; // TODO
      }
      assert (wsum > 0.0) : "wsum = " + wsum;
      mean /= wsum;

      // now calc the variance using the "corrected two-pass
      // algorithm" adapted from nr (eq 14.1.8)
      if (n == 1) var = minVar;
      else
      {
         a = 0.0;
         b = 0.0;
         span.itReset();
         while(span.itMore())
         {
            int i = span.itNext();
            x = data[i] - mean;
            y = weight[i] * x;
            a += y * x;
            b += y;
         }
         var = (a - b * b / wsum) / (wsum - wsum / n);
         var = Math.max(var, minVar);
      }
      update();
      return true;
   }

   public double getMean()
   {
      return mean;
   }

   public double getVar()
   {
      return var;
   }

   public double getMinVar()
   {
      return minVar;
   }

   public double getSDev()
   {
      return sdev;
   }

   public String toString()
   {
      return String.format("[Gaussian: %.3f, %.3f]", mean, var);
   }

   public void set(double _mean, double _var)
   {
      mean = _mean;
      var = _var;
      update();
   }

   public void setMean(double _mean)
   {
      mean = _mean;
   }

   public void setVar(double _var)
   {
      var = _var;
      update();
   }

   public void setVar(double _var, boolean bUpdate)
   {
      var = _var;
      if (bUpdate) update();
   }

   public void setMinVar(double _minVar)
   {
      minVar = _minVar;
   }

   public void add(Gaussian1D g)
   {
      mean += g.getMean();
      var += g.getVar();
      update();
   }

   /**
    * Computes the standard deviation from the (previously set) variance and also stores
    * the Gaussian coefficient (which doesn't depend on x) in the log domain. This must be
    * called after the variance changes and before any calls to eval().
    */
   protected void update()
   {
      if (var <= minVar) var = minVar;
      sdev = Math.sqrt(var);
      probCoef = 1.0 / (sdev * sqrt_2pi);
      loglikCoef = Math.log(probCoef);
      normal = null;
   }

   /**
    * Compute the "Gaussian error" for this Gaussian distribution (equivalent to the
    * exponent in the standard Gaussian equation). Thus the error function has a maximum
    * (equal to zero) when x=mean, and falls off from there.
    */
   public double evalError(double x)
   {
      double dx = x - mean;
      return (dx * dx) / (-2.0 * var);
   }

   /**
    * Evaluate the Gaussian at the point x; return the log-probability
    */
   public double eval(double x)
   {
      double b = evalError(x);
      switch(report){
      case prob:
         return probCoef * Math.exp(b);
      case loglik:
         return loglikCoef + b;
      case exp:
         return Math.exp(b);
      case error:
         return b;
      }
      assert false : "Unknown report value";
      return Double.NEGATIVE_INFINITY;
   }

   public double eval(FeatureVec x)
   {
      assert x.getNumDims() == 1;
      return eval(x.get(0));
   }

   public double sample1()
   {
      if (normal == null) normal = new Normal(mean, var);
      return normal.sample();
   }

   public void sample(FeatureVec fv)
   {
      fv.set(0, sample1());
   }

   @Override
   public ProbFVModel construct(int nDims)
   {
      if (nDims!=1) return null;
      return new Gaussian1D();
   }

   @Override
   public boolean learn(Sequence seq)
   {
      if (seq.getNumDims() != 1) return false;
      double[][] d = seq.toSeqArray();
      return learn(d[0]);
   }

   @Override
   public boolean learn(Sequence seq, double[] weights)
   {
      if (seq.getNumDims() != 1) return false;
      double[][] d = seq.toSeqArray();
      return learn(d[0], weights);
   }
}
