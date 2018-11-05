package kdm.mlpr;

import java.util.*;
import kdm.util.*;
import kdm.data.*;

/**
 * Factory class to create different kinds of sequence comparators
 */
public abstract class SeqCompFactory
{   
   /**
    * @return minimum distance between any pair of sequences
    */
   public static SeqComparator createMinComp(final SeqDist seqd)
   {
      return new SeqComparator()
      {
         public double dist(ArrayList<Sequence> a, ArrayList<Sequence> b)
         {            
            double vBest = Library.INF; 
            int na = a.size();
            int nb = b.size();
            for(int i=0; i<na; i++)
               for(int j=0; j<nb; j++)
               {
                  double d = seqd.dist(a.get(i), b.get(j));
                  if (d < vBest) vBest = d;
               }
            return vBest;
         }         
      };
   }
   
   /**
    * @return maximum distance between any pair of sequences
    */
   public static SeqComparator createMaxComp(final SeqDist seqd)
   {
      return new SeqComparator()
      {
         public double dist(ArrayList<Sequence> a, ArrayList<Sequence> b)
         {            
            double vBest = Library.NEGINF; 
            int na = a.size();
            int nb = b.size();
            for(int i=0; i<na; i++)
               for(int j=0; j<nb; j++)
               {
                  double d = seqd.dist(a.get(i), b.get(j));
                  if (d > vBest) vBest = d;
               }
            return vBest;
         }         
      };
   }
   
   /**
    * @return average distance between all pairs of sequences
    */
   public static SeqComparator createAvgComp(final SeqDist seqd)
   {
      return new SeqComparator()
      {
         public double dist(ArrayList<Sequence> a, ArrayList<Sequence> b)
         {            
            double d = 0; 
            int na = a.size();
            int nb = b.size();
            for(int i=0; i<na; i++)
               for(int j=0; j<nb; j++)
                  d += seqd.dist(a.get(i), b.get(j));
            return d / (na*nb);
         }         
      };
   }
}
