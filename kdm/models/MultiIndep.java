package kdm.models;

import kdm.data.*;
import java.util.*;
import kdm.util.*;

/** multivariate model with arbitrary, independent model per dimension */
public class MultiIndep extends ProbFVModel
{
   ProbFVModel[] models;

   public MultiIndep(int nDims)
   {
      super(nDims);
      models = new ProbFVModel[nDims];
   }
   
   public ProbFVModel getModel(int d)
   {
      return models[d];
   }
   
   public void setModel(int d, ProbFVModel model)
   {
      models[d] = model;
      models[d].setReport(report);
   }
   
   public void setModels(ProbFVModel[] models)
   {
      assert(nDims == models.length);
      for(int i=0; i<nDims; i++)
         this.models[i] = models[i];
   }
   
   @Override
   public ProbFVModel construct(int nDims)
   {
      // TODO Auto-generated method stub
      return null;
   }
   
   @Override
   public void setReport(Report report)
   {
      super.setReport(report);
      for(int i=0; i<nDims; i++)
         models[i].setReport(report);
   }

   @Override
   public ProbFVModel dup()
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public double eval(FeatureVec x)
   {
      if (report == Report.loglik){
         double loglik = Library.LOG_ONE;
         for(int d=0; d<nDims; d++)
            loglik += models[d].eval(new FeatureVec(1, x.get(d)));
         return loglik;
      }
      else if (report == Report.prob){
         double prob = 1.0;
         for(int d=0; d<nDims; d++)
            prob *= models[d].eval(new FeatureVec(1, x.get(d)));
         return prob;
      }
      else{
         assert false;
         return Double.NaN;
      }
   }

   @Override
   public boolean learn(Sequence seq)
   {
      // TODO Auto-generated method stub
      assert false;
      return false;
   }

   @Override
   public boolean learn(Sequence seq, double[] weights)
   {
      // TODO Auto-generated method stub
      assert false;
      return false;
   }

   @Override
   public void sample(FeatureVec fv)
   {
      // TODO Auto-generated method stub
      assert false;
   }

}
