package kdm.mlpr.classifier1D;

import static kdm.mlpr.classifier1D.DecisionStumpDatum.*;

import kdm.data.*;
import kdm.models.*;
import kdm.mlpr.classifier1D.*;
import kdm.mlpr.ensemble.*;
import kdm.models.*;
import kdm.util.*;
import java.util.regex.*;

/** 2-class classifier via LRT using a Gaussian for each class */
public class Gauss2 extends WeakClassifier
{
   protected static final Pattern prex = Pattern.compile("\\[(.+)\\,(.+)\\s+(\\d)\\(([+-]+)\\)\\s+(\\d+)\\s+(.+)\\s*\\|.*Gauss2\\]");
   protected double x1, x2;
   protected int[] wx;
   protected int iFeat;   
   
   public Gauss2()
   {
      x1 = x2 = Double.NaN;
      iFeat = -1;
   }
   
   public Gauss2(Gauss2 g)
   {
      copyFrom(g);
   }
   
   public void copyFrom(Gauss2 g)
   {
      super.copyFrom(g);
      x1 = g.x1;
      x2 = g.x2;
      wx = g.wx.clone();
      iFeat = g.iFeat;
   }
   
   @Override
   public WeakClassifier dup()
   {
      return new Gauss2(this);      
   }

   public int getFeature(){ return iFeat; }
   
   @Override
   public double learn(int iClass, Sequence[] examples, double[] weights, double priorPos, Object meta)
   {
      int nFeats = examples[0].getNumDims();

      Gauss2 gBest = new Gauss2();
      DecisionStumpDatum[] data = null;
      double errBest = Library.INF;
      
      for(int iFeat = 0; iFeat < nFeats; iFeat++){
         data = DecisionStumpDatum.createDataSet(iFeat, iClass, examples, weights, data);
         learn(priorPos, data);

         // calc error
         double err = 0;
         for(int i = 0; i < data.length; i++)
            if (!isCorrect(data[i])) err += data[i].weight;

         // see if this is a new best
         if (err < errBest){
            gBest.copyFrom(this);
            gBest.iFeat = iFeat;
            errBest = err;
         }
      }

      // copy data back to this object
      copyFrom(gBest);

      return errBest;
   }
   
   /** learn decision surface by modeling each class with a gaussian */
   protected double learn(double priorPos, DecisionStumpDatum[] data)
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
      double v1 = gm.getVar();

      gm.learn(x, w, spanNeg);
      double u2 = gm.getMean();
      double s2 = gm.getSDev();
      double v2 = gm.getVar();

      double[] dbx = calcGaussDBound(priorPos, u1, s1, u2, s2);

      if (dbx == null) wx = new int[]{ (priorPos>=0.5 ? POS : NEG) };
      else{
         double logPriorPos = Math.log(priorPos/(1-priorPos));      
         if (dbx.length == 1)
         {
            x1 = dbx[0];
            wx = new int[2];
            double lPos = Library.logGaussV(x1-1, u1, v1)+logPriorPos;
            double lNeg = Library.logGaussV(x1-1, u2, v2);
            if (lPos >= lNeg){
               wx[0] = POS;
               wx[1] = NEG;
            }
            else{
               wx[0] = NEG;
               wx[1] = POS;            
            }
         }
         else{
            // dbx.length == 2
            x1 = dbx[0];
            x2 = dbx[1];
            wx = new int[3];
            double lPos, lNeg;
            lPos = Library.logGaussV(x1-1, u1, v1)+logPriorPos;
            lNeg = Library.logGaussV(x1-1, u2, v2);
            if (lPos >= lNeg) wx[0] = POS;
            lPos = Library.logGaussV((x1+x2)/2, u1, v1)+logPriorPos;
            lNeg = Library.logGaussV((x1+x2)/2, u2, v2);
            if (lPos >= lNeg) wx[1] = POS;
            lPos = Library.logGaussV(x2+1, u1, v1)+logPriorPos;
            lNeg = Library.logGaussV(x2+1, u2, v2);
            if (lPos >= lNeg) wx[2] = POS;
         }
      }

      // calc error rate
      int nErr = 0;
      for(int i = 0; i < N; i++)
         if (classify(data[i].x) != data[i].wclass) nErr++;

      return (double)nErr / N;
   }

   /**
    * Calculate decision boundary for 1D, 2-class Gaussians
    * 
    * @param p1 prior on class 1
    * @param u1 mean of class 1
    * @param s1 standard deviation of class 1
    * @param u2 mean of class 2
    * @param s2 standard deviation of class 2
    * @return decision boundaries (can be zero (null), one or two)
    */
   public static double[] calcGaussDBound(double p1, double u1, double s1, double u2, double s2)
   {
      double p2 = 1.0 - p1;
      double v1 = s1 * s1;
      double v2 = s2 * s2;
      double a = v1 - v2;
      double b = 2 * (u1 * v2 - u2 * v1);
      double c = (v1 * u2 * u2 - v2 * u1 * u1) - 2 * v1 * v2 * Math.log((p2 * s1) / (p1 * s2));

      // don't divide by zero
      if (Math.abs(a) < 1e-9) return new double[]{ c / b };

      return Library.quadratic(a, b, c);
   }
   
   /** @return POS or NEG */
   public final int classify(FeatureVec fv)
   {
      return classify(fv.get(iFeat));
   }
      
   /** @return POS or NEG */
   public final int classify(double x)
   {
      if (wx.length == 1) return wx[0]; // always vote the same
      if (wx.length == 2){
         if (x < x1) return wx[0];
         return wx[1];
      }
      else{
         // three ranges
         if (x < x1) return wx[0];
         if (x <= x2) return wx[1];         
         return wx[2];
      }
   }

   /** @return true if the data point is clasified correctly */
   public final boolean isCorrect(DecisionStumpDatum d)
   {
      return (classify(d.x) == d.wclass);
   }

   public String toString()
   {
      StringBuffer sb = new StringBuffer();
      int nw = wx==null?0:wx.length;
      sb.append(String.format("[G2: x1=%.2f  x2=%.2f  wx(%d) ", x1, x2, nw));
      for(int i=0; i<nw; i++) sb.append(String.format("%d ", wx[i]));
      sb.append(String.format("iFeat=%d]", iFeat));
      return sb.toString();
   }

   @Override
   public boolean fromText(String s)
   {
      Matcher m = prex.matcher(s);
      if (!m.find()){
         System.err.printf("Error: regex doesn't match Gauss2 text\n");
         return false;
      }
      if (m.groupCount() != 6){
         System.err.printf("Error: expecting 6 groups in Gauss2 regex, found %d\n", m.groupCount());
         return false;
      }
      try{
         x1 = Double.parseDouble(m.group(1));
         x2 = Double.parseDouble(m.group(2));
         int nwx = Integer.parseInt(m.group(3));
         wx = new int[nwx];
         String w = m.group(4);
         for(int i=0; i<nwx; i++){
            if (w.charAt(i)=='+') wx[i] = POS;
            else wx[i] = NEG;
         }
         iFeat = Integer.parseInt(m.group(5));
         alpha = Double.parseDouble(m.group(6));
         return true;
      }
      catch(Exception e){ e.printStackTrace(); return false; }      
   }

   @Override
   public String toText()
   {
      StringBuffer sb = new StringBuffer();
      for(int i=0; i<wx.length; i++)
         sb.append(wx[i]==POS?'+':'-');
      return String.format("[%f,%f  %d(%s)  %d  %f |%s]", x1, x2, wx.length, sb.toString(), iFeat, alpha, getClass().getName());      
   }
}
