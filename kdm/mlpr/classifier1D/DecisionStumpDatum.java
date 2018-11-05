package kdm.mlpr.classifier1D;

import static kdm.mlpr.classifier1D.DecisionStumpDatum.NEG;
import static kdm.mlpr.classifier1D.DecisionStumpDatum.POS;
import kdm.data.Sequence;

/**
 * Represents one data point for learning a decision stump
 */
public class DecisionStumpDatum implements Comparable
{
   public static final int POS = 1;
   public static final int NEG = 0;
   
   public double weight;
   public int wclass;
   public double x;
   
   public DecisionStumpDatum()
   {
      set(0.0,POS,1.0);
   }
   
   public DecisionStumpDatum(double _x, int _wclass, double _weight)
   {
      set(_x, _wclass, _weight);
   }
   
   public DecisionStumpDatum(DecisionStumpDatum d)
   {
      set(d.x, d.wclass, d.weight);
   }

   public void set(double _x, int _wclass, double _weight)
   {
      x = _x;
      wclass = _wclass;
      assert(wclass==POS || wclass==NEG);
      weight = _weight;      
   }
   
   public void set(double _x, int _wclass)
   {
      x = _x;
      wclass = _wclass;
      assert(wclass==POS || wclass==NEG);
   }
   
   public static boolean isNormalized(DecisionStumpDatum[] dsd)
   {
      double sum = 0.0;
      for(int i=0; i<dsd.length; i++) sum += dsd[i].weight;
      return Math.abs(1.0 - sum)<0.0000001;
   }
   
   public static void normalize(DecisionStumpDatum[] dsd)
   {
      double sum = 0.0;
      for(int i=0; i<dsd.length; i++) sum += dsd[i].weight;
      for(int i=0; i<dsd.length; i++) dsd[i].weight /= sum;
   }
   
   public boolean equals(Object o)
   {
      DecisionStumpDatum dsd = (DecisionStumpDatum)o;
      return (x==dsd.x);
   }

   public int compareTo(Object o)
   {
      DecisionStumpDatum dsd = (DecisionStumpDatum)o;
      if (x < dsd.x) return -1;
      if (x > dsd.x) return 1;
      return 0;
   }
   
   public String toString()
   {
      return String.format("[%.4f  %d |%.4f]", x, wclass, weight);
   }
   
   /** Create a data set for a given feature index */
   public static DecisionStumpDatum[] createDataSet(int iFeat, int iClass, Sequence[] c, double[] weights,
         DecisionStumpDatum[] data)
   {
      // create the data structure if necessary
      if (data == null){
         int nTotal = 0;
         for(int i = 0; i < c.length; i++)
            nTotal += c[i].length();
         data = new DecisionStumpDatum[nTotal];
         for(int i = 0; i < nTotal; i++)
            data[i] = new DecisionStumpDatum();
      }

      // fill in data and class info
      int iData = 0;
      for(int ic = 0; ic < c.length; ic++){
         int nex = c[ic].length();
         for(int iex = 0; iex < nex; iex++){
            data[iData].set(c[ic].get(iex, iFeat), iClass == ic ? POS : NEG, weights[iData]);
            iData++;
         }
      }

      return data;
   }
}
