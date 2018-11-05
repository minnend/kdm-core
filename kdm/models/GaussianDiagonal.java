package kdm.models;

import kdm.data.*;

import static kdm.models.Gaussian1D.*;

/**
 * N-dimensional Guassian distributation assuming independent dimensions (i.e., diagonal covariance matrix)
 */
public class GaussianDiagonal extends GaussianAbstract
{
   protected Gaussian1D g[];

   public GaussianDiagonal(int nDims)
   {
      super(nDims);
      g = new Gaussian1D[nDims];
      for(int i = 0; i < nDims; i++){
         g[i] = new Gaussian1D();
         g[i].setReport(Report.loglik);
      }
   }

   public GaussianDiagonal(GaussianDiagonal g)
   {
      super(g.getNumDims());
      copyFrom(g);
   }

   public GaussianDiagonal(GaussianSpherical sg)
   {
      super(sg.getNumDims());
      g = new Gaussian1D[nDims];
      FeatureVec fvMean = sg.getMean();
      double var = sg.getVar();
      for(int i = 0; i < nDims; i++)
         g[i] = new Gaussian1D(fvMean.get(i), var);
   }

   public static final GaussianDiagonal eye(int _nDims)
   {
      return new GaussianDiagonal(_nDims);
   }

   @Override
   public ProbFVModel dup()
   {
      return new GaussianDiagonal(this);
   }

   @Override
   public void copyFrom(ProbFVModel model)
   {
      super.copyFrom(model);
      GaussianDiagonal dg = (GaussianDiagonal)model;
      g = new Gaussian1D[nDims];
      setMean(dg.getMean());
      setVar(dg.getVar());
   }

   @Override
   public void setMean(FeatureVec x)
   {
      assert (x.getNumDims() == nDims) : "dimensions don't match";
      for(int d = 0; d < nDims; d++){
         double mean = x.get(d);
         assert(!bCheckNaN || !Double.isNaN(mean));
         if (g[d] == null) g[d] = new Gaussian1D(mean, 1.0);
         else g[d].setMean(mean);
      }
   }

   public void setVar(FeatureVec x)
   {
      assert (x.getNumDims() == nDims) : "dimensions don't match";
      for(int i = 0; i < nDims; i++){
         if (g[i] == null) g[i] = new Gaussian1D(0.0, x.get(i));
         else g[i].setVar(x.get(i));
      }
   }

   public void mulVar(double x)
   {
      for(int i = 0; i < nDims; i++)
         g[i].setVar(g[i].getVar() * x);
   }

   public void setMean(int i, double x)
   {
      g[i].setMean(x);
   }

   public FeatureVec getMean()
   {
      FeatureVec fv = new FeatureVec(nDims);
      for(int i = 0; i < nDims; i++)
         fv.set(i, g[i].getMean());
      return fv;
   }

   public void setVar(int i, double x)
   {
      g[i].setVar(x);
   }

   public FeatureVec getVar()
   {
      FeatureVec fv = new FeatureVec(nDims);
      for(int i = 0; i < nDims; i++)
         fv.set(i, g[i].getVar());
      return fv;
   }

   @Override
   public FeatureVec getCovDiag()
   {
      return getVar();
   }

   /**
    * Returns the log-likelihood of the data point given the model
    */
   public double eval(FeatureVec x)
   {
      assert (x.getNumDims() == nDims);

      double ret = g[0].eval(x.get(0));
      for(int i = 1; i < nDims; i++)
         ret += g[i].eval(x.get(i));

      if (report == Report.loglik) return ret;
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
      for(int i = 0; i < nDims; i++)
         fv.set(i, g[i].sample1());
   }

   public boolean learn(Sequence seq)
   {
      assert (seq.getNumDims() == nDims) : String.format("%d != %d", seq.getNumDims(), nDims);
      double[][] data = seq.toSeqArray();
      for(int i = 0; i < data.length; i++)
         if (!g[i].learn(data[i])) return false;
      return true;
   }

   public boolean learn(Sequence seq, double[] weights)
   {
      assert (seq.getNumDims() == nDims);
      assert (seq.length() == weights.length);

      double[][] data = seq.toSeqArray();
      for(int i = 0; i < data.length; i++)
         if (!g[i].learn(data[i], weights)) return false;
      return true;
   }

   public String toString()
   {
      StringBuffer sb = new StringBuffer();
      sb.append("Diagonal Gaussian: #dims=" + nDims);
      for(int i = 0; i < nDims; i++)
         sb.append(String.format(" %d) %s\n", i + 1, g[i]));
      return sb.toString();
   }

   @Override
   public ProbFVModel construct(int nDims)
   {
      return new GaussianDiagonal(nDims);
   }
}
