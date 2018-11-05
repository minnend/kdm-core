package kdm.metrics;

import kdm.data.*;

/**
 * Computes the sum (or average) of the distances at each time step. The method used for
 * computing the distance is controlled by the feature vector metric passed to the
 * constructor.
 */
public class SumSeqDist extends MetricSeq
{
   protected MetricFV fvm;
   protected boolean bAvg;

   public SumSeqDist(MetricFV _fvm, boolean _bAvg)
   {
      this(_fvm, _bAvg, LengthPrep.extend);
   }
   
   public SumSeqDist(MetricFV _fvm, boolean _bAvg, LengthPrep _prep)
   {
      super(_prep);
      fvm = _fvm;
      bAvg = _bAvg;
      if (prep == LengthPrep.none)
      {
         System.err.printf("Warning: attempted to set SumSeqDist length prep to \"none\" -- changing to \"extend.\"");
         prep = LengthPrep.extend;
      }
   }

   public double calcDist(Sequence a, WindowLocation winA, Sequence b, WindowLocation winB)
   {
      int lenA = winA.length();
      int lenB = winB.length();
      assert (lenA == lenB) : String.format("SumSeqDist requires equal length windows (%d vs. %d)\n", lenA,
            lenB);

      int i = winA.start();
      int j = winB.start();
      double d = 0.0;
      for(; i < winA.end(); i++, j++)
         d += fvm.dist(a.get(i), b.get(j));
      if (bAvg) d /= (double)winA.length();
      return d;
   }
}
