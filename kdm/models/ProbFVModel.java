package kdm.models;

import kdm.data.*;
import kdm.io.*;
import kdm.util.*;

import java.io.*;

/**
 * abstract base class for probabilistic models (distributions) over feature vectors
 */
public abstract class ProbFVModel
{
   /** true => models should check for NaN parameters */
   public static boolean bCheckNaN = true;
   
   public static enum Report {
      prob, loglik, exp, error
   }

   protected int nDims;
   protected Report report = Report.loglik;

   public ProbFVModel(int nDims)
   {
      this.nDims = nDims;
   }

   public final int getNumDims()
   {
      return nDims;
   }

   /**
    * @return likelihood of the data point given the model, using the current report format
    */
   public abstract double eval(FeatureVec x);

   /** @return likelihood of the data set given the model, using the current report format */
   public double eval(Sequence data)
   {
      if (report == Report.loglik)
      {         
         double loglik = Library.LOG_ONE;
         int N = data.length();
         for(int i = 0; i < N; i++)
            loglik += eval(data.get(i));
         return loglik;
      }
      else if (report == Report.prob)
      {
         double prob = 1.0;
         int N = data.length();
         for(int i = 0; i < N; i++)
            prob *= eval(data.get(i));
         return prob;
      }
      return Double.NaN;
   }

   /**
    * Sets the given feature vector to a random sample from this distribution.
    */
   public abstract void sample(FeatureVec fv);

   public Report getReport()
   {
      return report;
   }

   public void setReport(Report rep)
   {
      report = rep;
   }

   /** learn the parameters of this model from the given data set */
   public abstract boolean learn(Sequence seq);

   /** learn the parameters of this model from the given weighted data set */
   public abstract boolean learn(Sequence seq, double[] weights);

   /** @return generic (unitialized) model; allows instance to be used as a factory */
   public abstract ProbFVModel construct(int nDims);

   /**
    * @return sample taken from this distribution.
    */
   public FeatureVec sample()
   {
      FeatureVec fv = new FeatureVec(nDims);
      sample(fv);
      return fv;
   }

   /** @return copy of this distribution */
   public abstract ProbFVModel dup();

   /**
    * copy the parameters of the given model into this model; subclass should always override and add to this
    * function if they define additional variables
    */
   public void copyFrom(ProbFVModel model)
   {      
      nDims = model.nDims;
      report = model.report;
   }
}
