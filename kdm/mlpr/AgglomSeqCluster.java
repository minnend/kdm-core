package kdm.mlpr;

import kdm.data.*;
import kdm.util.*;
import java.util.*;

/**
 * Clusters a set of sequences using agglomerative clustering
 */
public class AgglomSeqCluster extends AgglomCluster
{
   protected SeqDist seqdist;   

   public AgglomSeqCluster(SeqDist _seqdist)
   {
      seqdist = _seqdist;
   }
   
   public void cluster(ArrayList<Sequence> data, DoubleComp dcomp)
   {
      int N = data.size();
      
      // build an initial array of distances      
      System.err.print("Building initial distance table... ");
      TimerMS timer = new TimerMS();
      double[][] dmap = new double[N][];
      for(int i=0; i<N; i++)
      {
         dmap[i] = new double[i];
         for(int j=0; j<i; j++) dmap[i][j] = seqdist.dist(data.get(i), data.get(j));
      }      
      System.err.printf("done. (%dms)\n", timer.time());
      cluster(dmap, dcomp);
   }
     
}
