package kdm.mlpr.classifier1D;

import java.util.*;

import kdm.mlpr.ensemble.*;
import kdm.models.*;
import kdm.models.misc.*;
import kdm.util.*;
import kdm.data.*;
import java.util.regex.*;

import static kdm.mlpr.classifier1D.DecisionStumpDatum.*;

/**
 * Represents an actual decision stump (value + sign)
 */
public class DecisionStump extends WeakClassifier
{
   protected static final Pattern prex = Pattern.compile("\\[(.+)\\s+([-+])\\s+(\\d+)\\s+(.+)\\s*\\|.*DecisionStump\\]");
   public static enum LearnMethod { Search, Gauss }
   
   protected double xbound;
   protected int sign, iFeat;

   public DecisionStump()
   {
      xbound = 0.0;
      sign = 1;
      iFeat = -1;
   }

   public DecisionStump(double _x, int _sign, int _iFeat)
   {
      xbound = _x;
      sign = _sign;
      assert (sign == 1 || sign == -1);
      iFeat = _iFeat;
   }

   public DecisionStump(DecisionStump ds)
   {
      copyFrom(ds);
   }
   
   @Override
   public WeakClassifier dup()
   {
      return new DecisionStump(this);      
   }

   public void copyFrom(DecisionStump ds)
   {
      super.copyFrom(ds);
      xbound = ds.xbound;
      sign = ds.sign;
      iFeat = ds.iFeat;
   }

   public final double getDecisionBoundary()
   {
      return xbound;
   }

   public final int getSign()
   {
      return sign;
   }

   public final int getFeature()
   {
      return iFeat;
   }

   @Override
   public double learn(int iClass, Sequence[] examples, double[] weights, double priorPos, Object meta)
   {
      LearnMethod lm = (LearnMethod)meta;
      int nFeats = examples[0].getNumDims();

      DecisionStump dsBest = new DecisionStump();
      DecisionStumpDatum[] data = null;
      double errBest = Library.INF;
      
      for(int iFeat = 0; iFeat < nFeats; iFeat++){
         data = createDataSet(iFeat, iClass, examples, weights, data);
         if (lm == LearnMethod.Search) learn(data);
         else learnGauss(priorPos, data);

         // calc error
         double err = 0;
         for(int i = 0; i < data.length; i++)
            if (!isCorrect(data[i])) err += data[i].weight;

         // see if this is a new best
         if (err < errBest){
            dsBest.sign = sign;
            dsBest.xbound = xbound;
            dsBest.iFeat = iFeat;
            errBest = err;
         }
      }

      // copy data back to this object
      copyFrom(dsBest);

      return errBest;
   }
   
   /**
    * Calculate decision boundary for 1D, 2-class Gaussians
    * 
    * @param p1 prior on class 1
    * @param u1 mean of class 1
    * @param s1 standard deviation of class 1
    * @param u2 mean of class 2
    * @param s2 standard deviation of class 2
    * @return optimal decision boundary for 2-class 1D gaussians, NaN if no boundary
    */
   public static double calcGaussDBound(double p1, double u1, double s1, double u2, double s2)
   {
      double[] x = Gauss2.calcGaussDBound(p1, u1, s1, u2, s2);
      if (x == null) return Double.NaN;
      if (x.length==1) return x[0];
      double m = (u1 + u2) / 2.0;
      if (Math.abs(x[0] - m) < Math.abs(x[1] - m)) return x[0];
      else return x[1];
   }

   /** learn decision surface by modeling each class with a gaussian */
   public double learnGauss(double priorPos, DecisionStumpDatum[] data)
   {
      // calculate the decision boundary
      int N = data.length;
      double[] x = new double[N];
      double[] w = new double[N];
      SpanList spanPos = new SpanList(0, N - 1, false);
      SpanList spanNeg = new SpanList(0, N - 1, false);

      for(int i = 0; i < N; i++){
         x[i] = data[i].x;
         w[i] = data[i].weight;
         if (data[i].wclass == DecisionStumpDatum.POS) spanPos.add(i);
         else spanNeg.add(i);
      }
      
      Gaussian1D gm = new Gaussian1D();
      gm.learn(x, w, spanPos);
      double u1 = gm.getMean();
      double s1 = gm.getSDev();

      gm.learn(x, w, spanNeg);
      double u2 = gm.getMean();
      double s2 = gm.getSDev();

      xbound = calcGaussDBound(priorPos, u1, s1, u2, s2);

      // if no bound, go with prior
      if (Double.isNaN(xbound)){
         if (priorPos >= 0.5){
            xbound = Library.NEGINF;
            sign = 1;
         }
         else{
            xbound = Library.INF;
            sign = -1;
         }
      }

      // otherwise sign
      if (u1 >= u2) sign = 1;
      else sign = -1;

      // calc error rate
      int nErr = 0;
      for(int i = 0; i < N; i++)
         if (classify(data[i].x) != data[i].wclass) nErr++;

      return (double)nErr / N;
   }

   /** learn optimal parameters for this dstump and return error rate */
   public double learn(DecisionStumpDatum[] data)
   {
      if (data == null || data.length < 2) return Double.NaN;

      Arrays.sort(data);
      assert (isNormalized(data));
      int nData = data.length;

      int iBest;
      double wBestErr, werr, weight;

      // setup, put divider at neg inf
      sign = 1;
      werr = 0;
      for(int i = 0; i < nData; i++)
         if (data[i].wclass == NEG) werr += data[i].weight;
      iBest = 0;
      wBestErr = werr;

      // scan decision boundary across data points, boundary is just smaller than data
      int iFirstSwitch = 0;
      for(int iData = 1; iData < nData; iData++){
         // no need to test until class change (this gives small speed-up)
         if (data[iData].wclass == data[iData-1].wclass) continue;
         
         // calc sum of weights for skipped (now swapped) points
         double wsum = 0;
         for(int i=iFirstSwitch; i<iData; i++)
            wsum += data[i].weight;
         iFirstSwitch = iData;
         
         // data[iData-1] switch sides (pos to neg side): if it's POS, then we had it right, now wrong
         // if it's NEG, then we had it wrong, now right
         if (data[iData - 1].wclass == POS) werr += wsum;
         else werr -= wsum;

         double a = Math.min(werr, 1.0 - werr);
         double b = Math.min(wBestErr, 1.0 - wBestErr);
         if (a < b){
            wBestErr = werr;
            iBest = iData;
         }         
      }

      // no need to try after the last point because it's the same as before first point

      if (iBest == 0) xbound = Library.NEGINF;
      else xbound = (data[iBest - 1].x + data[iBest].x) / 2.0;
      sign = (wBestErr >= .5 ? -1 : 1);

      return Math.min(wBestErr, 1.0 - wBestErr);
   }

   /**
    * Classify the data points
    * 
    * @param data data points to classify
    * @return Eval4 array
    */
   public final int[] classify(DecisionStumpDatum[] data)
   {
      int TP = 0, FP = 0, TN = 0, FN = 0;
      for(DecisionStumpDatum d : data){
         if (isCorrect(d)){
            if (d.wclass == POS) TP++;
            else TN++;
         }
         else{
            if (d.wclass == POS) FN++;
            else FP++;
         }
      }

      return Eval4.arr(TP, TN, FP, FN);
   }

   /** @return array of booleans, true means corresponding data point was correctly classified */
   public final boolean[] getRightWrong(DecisionStumpDatum[] d)
   {
      boolean[] wres = new boolean[d.length];
      for(int i = 0; i < d.length; i++)
         wres[i] = isCorrect(d[i]);
      return wres;
   }

   /**
    * score a point, >0 => on the positive side of the decision boundary
    * 
    * @param x data point to classify
    * @return score = distance from oriented decision boundary
    */
   public final double getScore(double x)
   {
      return (x - xbound) * sign;
   }

   /** @return POS or NEG */
   public final int classify(FeatureVec fv)
   {
      return classify(fv.get(iFeat));
   }

   /** @return POS or NEG */
   public final int classify(double x)
   {
      return (getScore(x) > 0 ? POS : NEG);
   }

   /** @return true if the data point is clasified correctly */
   public final boolean isCorrect(DecisionStumpDatum d)
   {
      return (classify(d.x) == d.wclass);
   }

   public String toString()
   {
      return String.format("[DS: x=%.4f  sign: %d  feat: %d]", xbound, sign, iFeat);
   }

   public static void main(String args[]) // TODO: for debug
   {
      DecisionStump ds = new DecisionStump();
      DecisionStumpDatum[] data = new DecisionStumpDatum[6];
      data[0] = new DecisionStumpDatum(0, POS, 100);
      data[1] = new DecisionStumpDatum(1, NEG, 1);
      data[2] = new DecisionStumpDatum(2, NEG, 1);
      data[3] = new DecisionStumpDatum(3, NEG, 1);
      data[4] = new DecisionStumpDatum(4, POS, 1);
      data[5] = new DecisionStumpDatum(5, POS, 1);
      normalize(data);
      if (Double.isNaN(ds.learn(data))){
         System.err.printf("Error: failed to learn data!\n");
         System.exit(0);
      }

      int[] res = ds.classify(data);
      System.err.printf("Learned: %.4f, %d,  #corr=%d  acc=%.2f%%\n", ds.xbound, ds.sign, Eval4.ncorr(res),
            100.0 * Eval4.accuracy(res));
   }

   @Override
   public boolean fromText(String s)
   {
      Matcher m = prex.matcher(s);
      if (!m.find()){
         System.err.printf("Error: regex doesn't match Gauss2 text\n");
         return false;
      }
      if (m.groupCount() != 4){
         System.err.printf("Error: expecting 4 groups in DStump regex, found %d\n", m.groupCount());
         return false;
      }
      try{
         xbound = Double.parseDouble(m.group(1));
         char c = m.group(2).charAt(0);
         if (c=='+') sign = 1;         
         else sign = -1;
         iFeat = Integer.parseInt(m.group(3));
         alpha = Double.parseDouble(m.group(4));
         return true;
      }
      catch(Exception e){ e.printStackTrace(); return false; }      
   }

   @Override
   public String toText()
   {
      return String.format("[%f  %c  %d  %f |%s]", xbound, sign>0?'+':'-', iFeat, alpha, getClass().getName());
   }
}
