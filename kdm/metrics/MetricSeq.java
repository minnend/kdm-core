package kdm.metrics;

import kdm.data.*;

/**
 * A metric that operates over sequences. The class supports optional automatic shrinking
 * or extending of (sub)sequences to ensure that distances are calculated across equal
 * length sequences.
 */
public abstract class MetricSeq
{
   public enum LengthPrep {
      none, extend, shrink
   }

   protected LengthPrep prep = LengthPrep.none;

   public MetricSeq()
   {
      this(LengthPrep.none);
   }

   public MetricSeq(LengthPrep prep)
   {
      this.prep = prep;
   }

   public void setLengthPrep(LengthPrep prep)
   {
      this.prep = prep;
   }

   public LengthPrep getLengthPrep()
   {
      return prep;
   }

   /** @return distance between (sub)sequences */
   protected abstract double calcDist(Sequence a, WindowLocation winA, Sequence b, WindowLocation winB);

   /**
    * @return lower bound on distance between (sub)sequences. The default implementation
    *         just returns 0, so subclasses should supply their own non-trivial
    *         lower-bound.
    */
   protected double calcLowerBound(Sequence a, WindowLocation winA, Sequence b, WindowLocation winB)
   {
      return 0.0;
   }

   /**
    * @return lower bound information (whatever can be pre-computed) for the given (sub)sequence
    */
   public LBInfo calcLBInfo(Sequence seq, WindowLocation wloc)
   {
      return null;
   }
   
   /**
    * @return lower bound information (whatever can be pre-computed) for the given sequence
    */
   public LBInfo calcLBInfo(Sequence seq)
   {
      return calcLBInfo(seq, new WindowLocation(0, seq.length()));
   }
   
   /**
    * @return actual lower bound calculated from precomputed info and the given (sub)sequence 
    */
   public double lowerBound(LBInfo lbi, Sequence seq, WindowLocation wloc)
   {
      return 0;
   }
   
   /**
    * @return actual lower bound calculated from precomputed info and the given sequence 
    */
   public double lowerBound(LBInfo lbi, Sequence seq)
   {      
      // resample input if necessary
      int nReq = lbi.getReqLength();
      int nAct = seq.length();
      if (nReq > 0 && nAct!=nReq) seq = seq.resample(nReq);         
      WindowLocation wloc = new WindowLocation(0, seq.length());
      
      return lowerBound(lbi, seq, wloc);
   }
   
   /** @return lower bound on the distance, after performing length prep */
   public double lowerBound(Sequence a, WindowLocation winA, Sequence b, WindowLocation winB)
   {
      if ((prep == LengthPrep.extend && winA.length() < winB.length())
            || (prep == LengthPrep.shrink && winA.length() > winB.length()))
      {
         a = a.subseq(winA).resample(winB.length());
         winA = new WindowLocation(winA.iSeries, 0, a.length());
      }
      else if ((prep == LengthPrep.extend && winB.length() < winA.length())
            || (prep == LengthPrep.shrink && winB.length() > winA.length()))
      {
         b = b.subseq(winB).resample(winA.length());
         winB = new WindowLocation(winB.iSeries, 0, b.length());
      }

      return calcLowerBound(a, winA, b, winB);
   }

   /** @return lower-bound on distance between the two sequence (full length) */
   public double lowerBound(Sequence a, Sequence b)
   {
      WindowLocation winA = new WindowLocation(a.getParentIndex(), 0, a.length());
      WindowLocation winB = new WindowLocation(b.getParentIndex(), 0, b.length());
      return lowerBound(a, winA, b, winB);
   }

   /**
    * Calculate distance between two subsequences, performing extending/shrinking as
    * appropriate
    * 
    * @return distance between two subsequences
    */
   public double dist(Sequence a, WindowLocation winA, Sequence b, WindowLocation winB)
   {
      if ((prep == LengthPrep.extend && winA.length() < winB.length())
            || (prep == LengthPrep.shrink && winA.length() > winB.length()))
      {
         a = a.subseq(winA).resample(winB.length());
         winA = new WindowLocation(winA.iSeries, 0, a.length());
      }
      else if ((prep == LengthPrep.extend && winB.length() < winA.length())
            || (prep == LengthPrep.shrink && winB.length() > winA.length()))
      {
         b = b.subseq(winB).resample(winA.length());
         winB = new WindowLocation(winB.iSeries, 0, b.length());
      }
      return calcDist(a, winA, b, winB);
   }

   /** @return distance between the two sequence (full length) */
   public double dist(Sequence a, Sequence b)
   {
      WindowLocation winA = new WindowLocation(a.getParentIndex(), 0, a.length());
      WindowLocation winB = new WindowLocation(b.getParentIndex(), 0, b.length());
      return dist(a, winA, b, winB);
   }
}
