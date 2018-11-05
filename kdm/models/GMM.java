package kdm.models;

import kdm.data.*;
import kdm.models.ProbFVModel.Report;
import kdm.util.*;

/** multivariate Gaussian mixture model */
public class GMM extends ProbFVModel
{
   protected Multinomial weights;
   protected ProbFVModel[] models;

   public GMM(int nDims, int nComps)
   {
      this(nDims, nComps, null);
   }

   public GMM(int nDims, int nComps, ProbFVModel factory)
   {
      super(nDims);
      weights = new Multinomial(nComps);
      models = new ProbFVModel[nComps];
      if (factory != null){
         for(int i = 0; i < nComps; i++)
            models[i] = factory.construct(nDims);
      }
   }

   public GMM(GMM gmm)
   {
      super(gmm.getNumDims());
      copyFrom(gmm);
   }

   @Override
   public ProbFVModel dup()
   {
      return new GMM(this);
   }

   @Override
   public void copyFrom(ProbFVModel model)
   {
      super.copyFrom(model);
      GMM gmm = (GMM)model;
      weights = new Multinomial(gmm.getWeights());
      int nComps = gmm.getNumMix();
      models = new ProbFVModel[nComps];
      for(int i = 0; i < nComps; i++)
         models[i] = gmm.getComp(i).dup();
   }

   public void setComp(int i, double w, ProbFVModel model)
   {
      weights.set(i, w);
      models[i] = model;
   }

   /** @return probability of i'th component */
   public double getWeight(int i)
   {
      return weights.get(i);
   }

   /** set the probability of the i'th component */
   public void setWeight(int i, double prob)
   {
      weights.set(i, prob);
   }

   public Multinomial getWeights()
   {
      return weights;
   }

   public ProbFVModel getComp(int i)
   {
      return models[i];
   }

   @Override
   public double eval(FeatureVec x)
   {
      assert (weights.getReport() == Report.loglik);
      assert (models[0].getReport() == Report.loglik);

      double loglik = weights.eval(0) + models[0].eval(x);
      for(int i = 1; i < models.length; i++)
         loglik = Library.logadd(loglik, weights.eval(i) + models[i].eval(x));
      if (report == Report.loglik) return loglik;
      else if (report == Report.prob) return Math.exp(loglik);
      else return Double.NaN;
   }

   @Override
   public void sample(FeatureVec fv)
   {
      int iComp = weights.samplei();
      models[iComp].sample(fv);
   }

   /** @return number of components in this mixture model */
   public int getNumMix()
   {
      return models.length;
   }

   /** @return new mixture model without the given component */
   public GMM removeComponent(int iRemove)
   {
      int nComps = models.length;
      GMM gmm = new GMM(nDims, nComps - 1);

      double wsum = 1.0 - weights.get(iRemove);
      int iNew = 0;
      for(int iOld = 0; iOld < nComps; iOld++){
         if (iOld == iRemove) continue;
         gmm.setComp(iNew++, weights.get(iOld) / wsum, models[iOld]);
      }
      return gmm;
   }

   public void dump()
   {
      System.err.printf("Gaussian Mixture Model: %d comps, %dD\n", models.length, nDims);
      for(int i = 0; i < models.length; i++){
         if (models[i] instanceof GaussianSpherical){
            GaussianSpherical g = (GaussianSpherical)models[i];
            System.err.printf(" Component %d: w=%.3f  mean=%s  var=%.4f\n", i + 1, weights.get(i), g
                  .getMean(), g.getVar());
         }
         else{
            GaussianDiagonal g = (GaussianDiagonal)models[i];
            System.err.printf(" Component %d: w=%.3f  mean=%s  var=%s\n", i + 1, weights.get(i),
                  g.getMean(), g.getVar());
         }
      }
   }

   @Override
   public ProbFVModel construct(int nDims)
   {
      assert false : "can't construct a generic GMM";
      return null;
   }

   @Override
   public void setReport(Report rep)
   {
      super.setReport(rep);
      weights.setReport(rep);
      for(int i = 0; i < models.length; i++)
         models[i].setReport(rep);
   }

   @Override
   public boolean learn(Sequence data)
   {
      int N = data.length();
      int nComps = getNumMix();
      double[][] pij = new double[nComps][N];

      double loglik = eval(data);

      // iterate for EM, max number of iterations unless loglik stabilizes
      for(int iter = 0; iter < 200; iter++){
         // E-step
         setReport(Report.prob);
         for(int i = 0; i < N; i++){
            FeatureVec x = data.get(i);
            double wsum = 0.0;
            for(int j = 0; j < nComps; j++){
               pij[j][i] = models[j].eval(x);
               wsum += pij[j][i];
            }
            if (wsum > 0){
               for(int j = 0; j < nComps; j++)
                  pij[j][i] /= wsum;
            }
         }

         // M-step
         for(int j = 0; j < nComps; j++){
            weights.set(j, Library.sum(pij[j]));
            models[j].learn(data, pij[j]);
         }
         weights.normalize();

         // stopping criteria
         setReport(Report.loglik);
         double loglik2 = eval(data);
         // System.err.printf("ll=%f ll2=%f ratio=%f\n", loglik, loglik2, loglik/loglik2);
         if (loglik / loglik2 - 1 < 1e-5) break;
         loglik = loglik2;
      }

      return true;
   }

   @Override
   public boolean learn(Sequence seq, double[] weights)
   {
      assert false : "not yet implemented";
      return false;
   }
}
