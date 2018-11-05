package kdm.data.transform;

import kdm.data.*;
import kdm.mlpr.*;

/** transform the data by clustering; any ClusteringAlgo can be used */
public class TransformCluster extends DataTransform
{
   protected ClusteringAlgo clust;

   public TransformCluster(ClusteringAlgo _clust)
   {
      clust = _clust;
   }

   public ClusteringAlgo getClusteringAlgo()
   {
      return clust;
   }

   public void dumpParams()
   {
      System.err.printf("%s) Clustering Algorithm: %s\n", getClass(), clust.getClass());
   }
   
   public Sequence transform(Sequence _data)
   {
      int[] ii = clust.getMembership(_data);

      // create the quantized Sequence
      Sequence data = new Sequence("Clust: " + _data.getName(), _data.getFreq(), _data.getStartMS());
      for(int i = 0; i < ii.length; i++)
         data.add(new FeatureVec(1, (double)ii[i]));      
      data.copyMeta(_data);
      return data;
   }
}
