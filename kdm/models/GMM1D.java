package kdm.models;

import kdm.data.*;
import kdm.models.ProbFVModel.Report;
import kdm.util.*;
import java.util.*;
import org.apache.commons.math.stat.*;

/** Gaussian mixture model in 1D */
public class GMM1D extends ProbFVModel
{
   protected int nMix;
   protected Multinomial w;
   protected Gaussian1D models[];

   public GMM1D(int nMix)
   {
      super(1);
      this.nMix = nMix;
      w = new Multinomial(nMix);
   }

   public GMM1D(GMM1D gmm)
   {
      super(1);
      copyFrom(gmm);
   }

   @Override
   public ProbFVModel dup()
   {
      return new GMM1D(this);
   }

   @Override
   public void copyFrom(ProbFVModel model)
   {
      super.copyFrom(model);
      GMM1D g = (GMM1D)model;
      w = (Multinomial)g.getWeights().dup();
      models = new Gaussian1D[nMix];
      for(int i = 0; i < nMix; i++)
         models[i] = (Gaussian1D)g.getModel(i).dup();
   }

   public int getNumComps()
   {
      return nMix;
   }

   public Multinomial getWeights()
   {
      return w;
   }

   public Gaussian1D getModel(int i)
   {
      return models[i];
   }

   /** @return likelihood of data given learned model */
   public double learn(MyDoubleList adata)
   {
      return learn(adata.toArray());
   }   

   /** @return likelihood of data given learned model */
   public double learn(double data[])
   {
      double xMin = StatUtils.min(data);
      double xMax = StatUtils.max(data);
      double range = xMax - xMin;
      double initWidth = range / (2.0 * nMix); // TODO: proper params?
      double minVar = range / (10.0 * nMix);
      int n = data.length;

      // initialize the model
      models = new Gaussian1D[nMix];
      for(int i = 0; i < nMix; i++){
         int j = (int)(Library.random() * n);
         // add noise to variance to ensure each component has different starting params
         // TODO is this enough? (+/- 1/2%)
         double varnoise = initWidth / 100.0;
         double var = initWidth + Library.random() * varnoise/2 - varnoise;
         models[i] = new Gaussian1D(data[j], var, minVar);
         models[i].setReport(Report.prob);
      }

      // train the model on the given data
      double p[][] = new double[nMix][n];
      // TODO how many iterations? (should check loglik diff)
      for(int iter = 0; iter < 30; iter++){
         // compute prob that point belongs in each class
         for(int i = 0; i < n; i++){
            double sum = 0.0;
            for(int j = 0; j < nMix; j++){               
               double v = models[j].eval(data[i]);
               p[j][i] = v;
               sum += v;
            }
            for(int j = 0; j < nMix; j++)
               p[j][i] /= sum;
         }

         // re-estimate the model parameters
         for(int i = 0; i < nMix; i++){
            models[i].learn(data, p[i]);
            w.set(i, Library.sum(p[i]) / (double)n);
         }
      }
      
      return eval(data);
   }

   public double eval(double data[])
   {
      if (report == Report.loglik){
         double x = 0.0;
         for(int i = 0; i < data.length; i++)
            x += eval(data[i]);
         return x;
      }
      else{
         double x = 1.0;
         for(int i = 0; i < data.length; i++)
            x *= eval(data[i]);
         return x;
      }
   }

   public double eval(double x)
   {
      double y = 0.0;
      for(int i = 0; i < nMix; i++){
         y += w.get(i) * models[i].eval(x);
      }
      return (report == Report.loglik ? Math.log(y) : y);
   }

   public double eval(FeatureVec x)
   {
      assert (x.getNumDims() == 1);
      return eval(x.get(0));
   }

   public void sample(FeatureVec fv)
   {
      int iMix = w.samplei();
      models[iMix].sample(fv);
   }

   public String toString()
   {
      StringBuffer sb = new StringBuffer();
      sb.append("Gaussian Mixture Model 1D: " + nMix + " components\n");
      for(int i = 0; i < nMix; i++){
         sb.append(String.format("%.3f:  %.4f, %.4f\n", w.get(i), models[i].getMean(), models[i].getVar()));
      }
      return sb.toString();
   }

   @Override
   public ProbFVModel construct(int nDims)
   {
      assert false : "can't construct GMM directly";
      return null;
   }

   @Override
   public boolean learn(Sequence seq)
   {
      if (seq.getNumDims() != 1) return false;
      learn(seq.toSeqArray()[0]);
      return true;
   }

   @Override
   public boolean learn(Sequence seq, double[] weights)
   {
      // TODO Auto-generated method stub
      return false;
   }
}
