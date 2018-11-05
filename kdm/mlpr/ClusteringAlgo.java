package kdm.mlpr;

import kdm.data.*;

public interface ClusteringAlgo
{
   public abstract int getK();
   public abstract FeatureVec[] getCenters();   
   public abstract int[] cluster(int k, Sequence data);
   public abstract int[] getMembership(Sequence data);
}
