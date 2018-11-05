package kdm.mlpr.ensemble;

import java.util.*;
import kdm.gui.*;
import kdm.data.Sequence;
import kdm.mlpr.classifier1D.*;
import kdm.models.misc.*;
import kdm.util.*;
import kdm.data.*;
import kdm.io.DataLoader.*;

import static kdm.mlpr.classifier1D.DecisionStump.*;
import static kdm.mlpr.classifier1D.DecisionStumpDatum.*;

/**
 * general adaboost frameowrk
 */
public class Adaboost
{
   protected String name;
   protected ArrayList<WeakClassifier> weak;
   protected double priorPos = 0.5;
   protected int nBoostIters = 10;
   protected int nMaxWeak = -1;
   protected ArrayList<Pair<WeakClassifier, Object>> wcs;

   protected BDSViewer view;

   public Adaboost(String name)
   {
      this(name, null);
   }

   public Adaboost(String name, ArrayList<Pair<WeakClassifier, Object>> wcs)
   {
      this.name = name;
      this.wcs = wcs;
      weak = new ArrayList<WeakClassifier>();
   }

   /** build a list of weak classifiers with meta data objects */
   public static ArrayList<Pair<WeakClassifier, Object>> buildWCList(Object... list)
   {
      assert (list.length % 2 == 0) : "list must contain wc/meta pairs";
      ArrayList<Pair<WeakClassifier, Object>> ret = new ArrayList<Pair<WeakClassifier, Object>>();
      int i = 0;
      while(i < list.length){
         WeakClassifier wc = (WeakClassifier)list[i++];
         Object meta = list[i++];
         ret.add(new Pair(wc, meta));
      }
      return ret;
   }

   public void setPriorPos(double prior)
   {
      assert (prior > 0 && prior < 1) : prior;
      priorPos = prior;
   }

   /** set the number of rounds of boosting used for learning */
   public void setNumRounds(int n)
   {
      nBoostIters = n;
   }

   /** set the max number of weak classifiers used during classification */
   public void setMaxWeak(int n)
   {
      nMaxWeak = n;
   }

   public String getName()
   {
      return name;
   }

   public ArrayList<WeakClassifier> getWeak()
   {
      return weak;
   }

   public int getNumWeak()
   {
      return weak.size();
   }

   public void addWeak(WeakClassifier wc)
   {
      weak.add(wc);
   }
   
   public boolean updateDist(double[] weights, int iClass, WeakClassifier wc, Sequence[] examples,
         double beta)
   {
      assert (beta > 0 && beta < 1) : beta;
      int iw = 0;
      for(int i = 0; i < examples.length; i++)
         for(int j = 0; j < examples[i].length(); j++){
            int lab = wc.classify(examples[i].get(j));
            if ((i == iClass && lab == POS) || (i != iClass && lab == NEG)) weights[iw] *= beta;
            iw++;
         }
      return Library.normalize(weights);
   }

   /** learn weak classifiers and weights for this ensemble */
   public boolean learn(int iClass, Sequence[] examples)
   {
      int nClasses = examples.length;
      int nFeats = examples[0].getNumDims();

      // how many examples, pos/neg
      int nPos = examples[iClass].length();
      int nNeg = 0;
      for(int i = 0; i < nClasses; i++)
         if (i != iClass) nNeg += examples[i].length();
      int nTotal = nPos + nNeg;

      // calc initial weights
      double[] weights = new double[nTotal];
      double wPos = priorPos;
      double wNeg = 1.0 - priorPos;
      if (nPos == 0) wNeg = 1.0;
      if (nNeg == 0) priorPos = 1.0;
      Arrays.fill(weights, wNeg / nNeg);

      int iData = 0;
      for(int i = 0; i < iClass; i++)
         iData += examples[i].length();
      double wPosPer = wPos / nPos;
      for(int j = 0; j < examples[iClass].length(); j++)
         weights[iData + j] = wPosPer;
      assert (Library.isNormalized(weights));

      for(int iter = 0; iter < nBoostIters; iter++){
         assert (Library.isNormalized(weights));

         // find the best weak classifier
         int iBest = -1;
         double err = Library.INF;
         for(int i = 0; i < wcs.size(); i++){
            WeakClassifier wc = wcs.get(i).first;
            Object meta = wcs.get(i).second;
            double errI = wc.learn(iClass, examples, weights, priorPos, meta);
            if (errI < err){
               err = errI;
               iBest = i;
            }
         }

         WeakClassifier wc = wcs.get(iBest).first;
         if (err > 0.5){
            System.err.printf("\nWarning: error rate worse than chance (err=%.4f (%.4f) iter=%d)\n", err,
                  wPos, iter);
            break;
         }

         // boost it
         double alpha, beta;
         if (err == 0){
            beta = 0;
            alpha = 10.0;
         }
         else{
            beta = err / (1 - err);
            alpha = Math.log(1.0 / beta);
            updateDist(weights, iClass, wc, examples, beta);
         }

         // save this weak classifier
         wc.setAlpha(alpha);
         weak.add(wc.dup());
         if (wc instanceof DecisionStump) System.err.printf(" %d", ((DecisionStump)wc).getFeature());
         if (wc instanceof Gauss2) System.err.printf(" %d", ((Gauss2)wc).getFeature());

         if (view != null){
            if (wc instanceof DecisionStump) view.add((DecisionStump)wc);
            view.setWeights(weights);
            int[] a = classify(0, examples);
            System.err.printf("ensemble: %s  (%.2f%%)    %s\n", Eval4.str(a), Eval4.accuracy(a) * 100, wc);
            view.waitForClick();
         }

         // nothing left to learn
         // -- but how does "test error continue to decrease after training error goes to zero?"
         // -- could force boost to find other useful weak classifiers...
         if (err == 0) break;
      }
      System.err.println();

      return true;
   }

   /** @return margin for this ensemble (m > 1/2 => POS; 0<=m<=1) */
   public double getMargin(FeatureVec fv)
   {
      double h = 0;
      double asum = 0;
      int nWeak = weak.size();
      if (nMaxWeak > 0) nWeak = Math.min(nMaxWeak, weak.size());
      for(int i = 0; i < nWeak; i++){
         WeakClassifier wc = weak.get(i);
         if (wc.classify(fv) == POS) h += wc.alpha;
         else h -= wc.alpha;
         asum += wc.alpha;
      }
      return h / asum;
   }

   /** @return POS or NEG */
   public int classify(FeatureVec fv)
   {
      return (getMargin(fv) >= 0) ? POS : NEG;
   }

   public int[] classify(int iClass, Sequence[] examples)
   {
      int TP = 0, TN = 0, FP = 0, FN = 0;
      int nc = examples.length;
      for(int i = 0; i < nc; i++){
         int wtrue = (iClass == i ? POS : NEG);
         int nex = examples[i].length();
         for(int j = 0; j < nex; j++){
            FeatureVec fv = examples[i].get(j);
            if (isCorrect(fv, wtrue)){
               if (wtrue == POS) TP++;
               else TN++;
            }
            else{
               if (wtrue == POS) FN++;
               else FP++;
            }
         }
      }
      return Eval4.arr(TP, TN, FP, FN);
   }

   public boolean isCorrect(FeatureVec fv, int wclass)
   {
      assert (wclass == POS || wclass == NEG);
      return (classify(fv) == wclass);
   }

   public static void main(String args[])
   {
      if (args.length != 2){
         System.err.printf("USAGE: java ~.BoostedDecStump <class1 file> <class2 file>\n");
         return;
      }

      DLRaw loader = new DLRaw();
      Sequence[] examples = new Sequence[2];
      examples[0] = loader.load(args[0]);
      examples[1] = loader.load(args[1]);
      System.err.printf("Class 0: %d points     Class 1: %d points\n", examples[0].length(), examples[1]
            .length());

      BDSViewer view = new BDSViewer(examples);

      ArrayList<Pair<WeakClassifier, Object>> wcs = new ArrayList<Pair<WeakClassifier, Object>>();
      wcs.add(new Pair(new DecisionStump(), LearnMethod.Search));
      // wcs.add(new Pair(new DecisionStump(),LearnMethod.Gauss));
      wcs.add(new Pair(new Gauss2(), null));
      Adaboost ada = new Adaboost("test", wcs);
      ada.view = view;
      ada.setNumRounds(500);

      if (view != null) view.waitForClick();
      ada.learn(0, examples);

      for(int i = 0; i < ada.getWeak().size(); i++)
         System.err.printf("%d) %s\n", i + 1, ada.getWeak().get(i));

      int[] a = ada.classify(0, examples);
      System.err.printf("Result: %s (%.2f%%)\n", Eval4.str(a), 100.0 * Eval4.accuracy(a));
   }
}
