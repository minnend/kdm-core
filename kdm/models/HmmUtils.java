package kdm.models;

import java.util.*;
import kdm.util.*;

/**
 * Library class with utility functions for HMMs
 */
public class HmmUtils
{
   /**
    * @return length of the shortest path through the given HMM
    */
   public static int getMinPathLength(AbstractHMM hmm)
   {
      int nMin = Integer.MAX_VALUE;
      int nStates = hmm.getNumStates();
      int[] dist = new int[nStates];
      Arrays.fill(dist, Integer.MAX_VALUE);
      for(int i=0; i<nStates; i++) if (hmm.isStartState(i)) dist[i] = 1;
         
      for(int i=0; i<nStates; i++)
      {
         if (!hmm.isStartState(i)) continue;
         int n = getMinPathLength(hmm, i, dist);
         if (n<nMin) nMin = n;
      }
      return nMin;
   }
   
   protected static int getMinPathLength(AbstractHMM hmm, int iState, int[] dist)
   {
      if (hmm.isEndState(iState)) return dist[iState];
      
      int nStates = hmm.getNumStates();
      double[][] tran = hmm.getFullTransMatrix();
      int nMin = Integer.MAX_VALUE;
      int newDist = dist[iState] + 1;
      for(int i=0; i<nStates; i++)
      {
         if (i==iState) continue;
         if (tran[iState][i] == Library.LOG_ZERO) continue;
         if (dist[i]<=newDist) continue;            
         dist[i] = newDist;
         int n = getMinPathLength(hmm, i, dist);
         if (n<nMin) nMin = n;
      }
      return nMin;
   }
   
}
