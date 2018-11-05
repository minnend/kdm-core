package kdm.models;

import kdm.data.*;
import kdm.io.*;
import kdm.util.*;
import java.util.*;
import java.io.*;

/**
 * This class represents a multinomial distribution. This is basically a discrete
 * histogram where each bin has an associated probability [0..1] and the sum of all bins
 * must be 1.0. The class also supports a prior on the bins, which must be specified in
 * the probability domain. By default, however, the class returns evaluations as
 * log-likelihoods, but this can be changed via the useLogLik() function.
 */
public class Multinomial extends ProbFVModel
{
   protected int nBins;
   public double bins[];
   public double llbins[];
   private double prior[];

   /**
    * Create a multinomial distribution with a uniform prior
    * 
    * @param nBins number of bins in the distribution
    */
   public Multinomial(int nBins)
   {
      super(nBins);
      this.nBins = nBins;
      bins = new double[nBins];
      llbins = new double[nBins];
      Arrays.fill(bins, 1.0 / (double)nBins);
      for(int i = 0; i < nBins; i++)
         llbins[i] = Math.log(bins[i]);
   }

   /**
    * Create a multinomial distribution with the given prior
    * 
    * @param prior prior probability for each bin
    */
   public Multinomial(double[] prior)
   {
      super(prior.length);
      this.prior = prior;
      nBins = prior.length;
      bins = (double[])prior.clone();
      llbins = new double[nBins];
      normalize();
   }

   public Multinomial(Multinomial m)
   {
      super(m.getSize());
      copyFrom(m);
   }
   
   @Override
   public ProbFVModel dup()
   {
      return new Multinomial(this);
   }

   @Override
   public void copyFrom(ProbFVModel model)
   {
      super.copyFrom(model);
      Multinomial m = (Multinomial)model;
      nBins = m.nBins;
      if (bins == null || bins.length != nBins)
      {
         bins = new double[nBins];
         llbins = new double[nBins];
      }
      Library.copy(m.bins, bins);
      Library.copy(m.llbins, llbins);
      prior = m.prior;
   }
   
   /**
    * Reset the distribution by setting the prob of all symbols to zero.
    */
   public void reset(){ reset(0.0); }

   /**
    * Reset the distribution by setting the prob of all symbols to p.
    * @param p starting value in each bin
    */
   public void reset(double p){ Arrays.fill(bins, p); }

   
   /**
    * Add the probability to the given been.  Don't automatically normalize.
    * @param i index of bin
    * @param p "probability" to add
    */
   public void addProb(int i, double p){ bins[i] += p; }
   
   /**
    * Evaluate the probability of the given value.
    */
   public double eval(int v)
   {            
      if (report == Report.loglik) return llbins[v];
      else if (report == Report.prob) return bins[v];
      else{
         assert false;
         return Double.NaN;
      }
   }

   public double eval(FeatureVec x)
   {
      assert (x.getNumDims() == 1);
      return eval((int)x.get(0));
   }
   
   public boolean learn(MyIntList adata)
   {
      return learn(adata.toArray());
   }

   public boolean learn(MyIntList adata, MyDoubleList aweight)
   {
      return learn(adata.toArray(), aweight.toArray());
   }

   public boolean learn(int data[])
   {
      reset();
      for(int i = 0; i < data.length; i++)
         bins[data[i]] += 1.0;
      normalize();
      addPrior(prior);
      return true;
   }

   public boolean learn(int data[], double w[])
   {
      return learn(data, w, new SpanList(0, data.length - 1, true));
   }

   public boolean learn(int data[], double w[], SpanList span)
   {
      Arrays.fill(bins, 0.0);
      span.itReset();
      while(span.itMore())
      {
         int i = span.itNext();
         bins[data[i]] += w[i];
      }
      normalize();
      addPrior(prior);
      return true;
   }

   public void addPrior(double _prior[])
   {
      assert (_prior.length == nBins);
      prior = _prior;
      for(int i = 0; i < nBins; i++)
         bins[i] += prior[i];
      normalize();
   }

   public int getSize()
   {
      return nBins;
   }

   /**
    * @param i index of bin
    * @return probability of i'th bin
    */
   public double get(int i)
   {
      return bins[i];
   }

   /**
    * @param i  index of bin
    * @return log-likelihood of the i'th bin
    */
   public double getll(int i)
   {
      return llbins[i];
   }

   /** set the probability of the i'th bin (log-bin auto-updated) */
   public void set(int i, double v)
   {
      bins[i] = v;
      llbins[i] = Math.log(bins[i]);
   }

   public int samplei()
   {
      return Library.sample(bins);
   }
   
   public void sample(FeatureVec fv)
   {
      assert (fv.getNumDims() == 1);
      fv.set(0, samplei());
   }

   /**
    * Ensure that the bins sum to one and recompute the loglik bins from the prob-domain
    * bins.
    */
   public void normalize()
   {
      if (!Library.normalize(bins)) Arrays.fill(llbins, Library.LOG_ZERO);
      else{
         for(int i = 0; i < nBins; i++)
            llbins[i] = Math.log(bins[i]);
      }
   }

   @Override
   public ProbFVModel construct(int nDims)
   {
      return new Multinomial(nDims);
   }

   @Override
   public boolean learn(Sequence seq)
   {
      return false;
   }

   @Override
   public boolean learn(Sequence seq, double[] weights)
   {
      return false;
   }
}
