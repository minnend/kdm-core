package kdm.mlpr;

import kdm.data.*;
import kdm.util.*;

/**
 * Cluster data using the (hard) k-means algorithm
 */
public class KMeans implements ClusteringAlgo
{
   public static final int DEF_MAX_ITERS = 200;
   public static final double DEF_EPSILON = 1e-6;

   protected FeatureVec[] centers;
   protected int nMaxIters;
   protected double epsilon;

   public KMeans()
   {
      this(DEF_MAX_ITERS, DEF_EPSILON);
   }

   public KMeans(int _nMaxIters, double _epsilon)
   {
      nMaxIters = _nMaxIters;
      epsilon = _epsilon;
   }

   /**
    * @return number of clusters or -1 if no clustering has been requested yet
    */
   public int getK()
   {
      if (centers == null) return -1;
      return centers.length;
   }

   /**
    * @return dimensionality of data or -1 if no clustering has been requested
    *         yet
    */
   public int getNDims()
   {
      if (centers == null) return -1;
      return centers[0].getNumDims();
   }

   /**
    * @return center point of each cluster or null if no clustering has been
    *         requested
    */
   public FeatureVec[] getCenters()
   {
      return centers;
   }

   /**
    * Cluster the given data
    * 
    * @param k number of clusters
    * @param data points to cluster
    * @return cluster membership (use getCenters() to find cluster center
    *         locations)
    */
   public int[] cluster(int k, Sequence data)
   {
      int n = data.length();
      centers = new FeatureVec[k];
      FeatureVec[] oldCenters = new FeatureVec[k];
      int w[] = new int[n];

      if (k > n){
         System.err
               .printf(
                     "Warning: invalid k-means invocation; too many clusters (k=%d with %d points",
                     k, n);
         return null;
      }

      // randomly initialize cluster centers
      // TODO: better to init cluster membership and compute cluster center?
      int ic[] = Library.selectRandomIndices(k, n);
      for(int i = 0; i < k; i++){
         centers[i] = data.get(ic[i]);
         oldCenters[i] = new FeatureVec(centers[i]);
         System.err.printf("init center %d = %d\n", i + 1, ic[i] + 1);
      }

      // iterate and update centers
      boolean bEps = false;
      for(int iter = 0; (iter < nMaxIters || nMaxIters<0) && !bEps; iter++){
         getMembership(data, w);
         updateCenters(data, w);

         // see if the change is small enough to stop
         bEps = true;
         for(int i = 0; i < k; i++){
            double d = centers[i].dist(oldCenters[i]);
            if (d >= epsilon){
               bEps = false;
               break;
            }
         }
         if (!bEps) for(int i = 0; i < k; i++)
            oldCenters[i].copyFrom(centers[i]);
      }

      return w;
   }

   /**
    * @return index of the center that is closest to each data point
    */
   public int[] getMembership(Sequence data)
   {
      return getMembership(data, new int[data.length()]);
   }

   /**
    * @param data points to cluster
    * @param w storage for cluster membership info
    * @return index of the center that is closest to each data point (same as w)
    */
   public int[] getMembership(Sequence data, int[] w)
   {
      int n = data.length();
      for(int i = 0; i < n; i++){
         FeatureVec fv = data.get(i);
         double dmin = fv.dist2(centers[0]);
         int di = 0;
         for(int j = 1; j < centers.length; j++){
            double d = fv.dist2(centers[j]);
            if (d < dmin){
               dmin = d;
               di = j;
            }
         }
         w[i] = di;
      }
      return w;
   }

   /**
    * @return number of points in each cluster
    */
   public int[] getMembershipSize(Sequence data)
   {
      int nw[] = new int[getK()];
      int w[] = getMembership(data);
      for(int i = 0; i < w.length; i++)
         nw[w[i]]++;
      return nw;
   }

   /**
    * Update the center estimates from the membership data
    * 
    * @param data points to cluster
    * @param w membership info
    */
   protected void updateCenters(Sequence data, int w[])
   {
      int k = getK();
      int n = w.length;
      int nw[] = new int[k];

      for(int i = 0; i < k; i++)
         centers[i].fill(0);
      for(int i = 0; i < n; i++){
         centers[w[i]]._add(data.get(i));
         nw[w[i]]++;
      }
      for(int i = 0; i < k; i++)
         centers[i]._div(nw[i]);
   }

   /**
    * Test procedure
    * 
    * @param args args[0] = path to text file containing data (one vec per line)
    */
   public static void main(String[] args)
   {
      double x[][] = Library.read(args[0], Library.MatrixOrder.RowMajor);
      System.err.printf("x = %d x %d\n", x.length, x[0].length);
      Sequence date = new Sequence("data", x);      

      KMeans kmeans = new KMeans();
      int mem[] = kmeans.cluster(3, date);
      FeatureVec centers[] = kmeans.getCenters();
      int nw[] = kmeans.getMembershipSize(date);
      for(int i = 0; i < centers.length; i++)      
         System.err.printf("Center %d: %s  (%d)\n", i + 1, centers[i], nw[i]);

      System.err.printf("figure;hold on;plot(x(:,1),x(:,2),'.');"
            + "plot([%.4f %.4f %.4f],[%.4f %.4f %.4f],'r.');\n", centers[0]
            .get(0), centers[1].get(0), centers[2].get(0), centers[0].get(1),
            centers[1].get(1), centers[2].get(1));
   }

}
