package kdm.mlpr;

import java.util.ArrayList;
import java.util.Arrays;

import kdm.util.SpanList;
import kdm.util.TimerMS;

/**
 * Implements a generic agglomerative clusterer that operates over a (lower-triangular) distance matrix.
 */
public class AgglomCluster
{
   protected ArrayList<AgglomInfo> path;
   
   public ArrayList<AgglomInfo> getPath(){ return path; }
   public AgglomInfo getRoot(){ return path.get(path.size()-1); }      
   
   /**
    * Cluster the data using the given distance matrix
    * @param dmap distance matrix
    * @param dcomp method for computing a new distance after a merge
    */
   public void cluster(double[][] dmap, DoubleComp dcomp)
   {
      int N = dmap.length;
      path = new ArrayList<AgglomInfo>();
      int memCount[] = new int[N];
      Arrays.fill(memCount, 1);
      AgglomInfo aiList[] = new AgglomInfo[N];
      for(int i=0; i<N; i++) aiList[i] = new AgglomInfo(i, -1, dmap[i].length>i ? dmap[i][i] : 0, 1);
      SpanList spanc = new SpanList(0, N-1, true);
      
      // now merge the two most similar clusters until there's only one left
      while(spanc.size() > 1)
      {
         // find two closest clusters
         int iBest=-1, jBest=-1;
         spanc.itReset();
         while(spanc.itMore())
         {
            int i = spanc.itNext(); 
            for(int j=0; j<i; j++)
            {
               if (!spanc.contains(j)) continue; // TODO: iteratore over spanc twice?
               if (iBest<0 || dmap[i][j] < dmap[iBest][jBest])
               {
                  iBest = i;
                  jBest = j;
               }
            }
         }
         
         // merge them
         int ni = memCount[iBest];
         int nj = memCount[jBest];
         memCount[jBest] += memCount[iBest];
         memCount[iBest] = 0;
         AgglomInfo ainfo = new AgglomInfo(iBest, jBest, dmap[iBest][jBest], memCount[jBest], aiList[iBest], aiList[jBest]);
         aiList[jBest] = ainfo;
         path.add(ainfo);
         //System.err.printf("merge (%d): %s\n", spanc.size()-1, ainfo);
         spanc.sub(iBest); // we move iBest into jBest                 
         
         // recompute the disturbed areas
         for(int i=0; i<N; i++)
         {
            if (i == jBest || i==iBest) continue;
            double dii = (i<iBest ? dmap[iBest][i] : dmap[i][iBest]);
            double dij = (i<jBest ? dmap[jBest][i] : dmap[i][jBest]);            
            double d = dcomp.comp(dii, dij, ni, nj);
            if (i<jBest) dmap[jBest][i] = d;
            else dmap[i][jBest] = d;
         }            
      }
   }
}
