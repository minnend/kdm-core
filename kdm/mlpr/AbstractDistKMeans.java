package kdm.mlpr;

import kdm.data.*;
import kdm.util.*;
import java.util.*;
import java.io.*;

/**
 * Abstract base class for clustering algorithms based on known models for the clusters
 */
public abstract class AbstractDistKMeans implements ClusteringAlgo
{
   public static final int DEF_MAX_ITERS = 200;
   public static final double DEF_EPSILON = 1e-6;

   protected FeatureVec[] centers;
   protected ArrayList<FeatureVec[]> track;
   protected int nMaxIters;
   protected double epsilon;
   protected boolean bVerbose = false;

   public AbstractDistKMeans()
   {
      this(DEF_MAX_ITERS, DEF_EPSILON);
   }

   public AbstractDistKMeans(int _nMaxIters, double _epsilon)
   {
      nMaxIters = _nMaxIters;
      epsilon = _epsilon;
   }

   public void setVerbose(boolean _bVerbose)
   {
      bVerbose = _bVerbose;
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
    * @return dimensionality of data or -1 if no clustering has been requested yet
    */
   public int getNDims()
   {
      if (centers == null) return -1;
      return centers[0].getNumDims();
   }

   /**
    * @return center point of each cluster or null if no clustering has been requested
    */
   public FeatureVec[] getCenters()
   {
      return centers;
   }

   /**
    * Set the centers of the clusters externally
    * 
    * @param _centers new cluster centers
    */
   public void setCenters(FeatureVec[] _centers)
   {
      centers = _centers;
   }

   public ArrayList<FeatureVec[]> getTracks()
   {
      return track;
   }

   /**
    * Duplicate the array of feature vectors
    * 
    * @param a the array to dup
    * @return a deep copy of 'a'
    */
   public static FeatureVec[] dup(FeatureVec[] a)
   {
      if (a == null) return null;
      FeatureVec[] b = new FeatureVec[a.length];
      for(int i = 0; i < a.length; i++)
         b[i] = new FeatureVec(a[i]);
      return b;
   }

   /**
    * Cluster the given data
    * 
    * @param k number of clusters
    * @param data points to cluster
    * @return membership (use getCenters() to find cluster center locations)
    */
   public int[] cluster(int k, Sequence data)
   {
      return cluster(k, data, false);
   }

   /**
    * Cluster the given data
    * 
    * @param k number of clusters
    * @param data points to cluster
    * @param bTrack true to store cluster centers during iteration (use getTracks());
    * @return cluster membership (use getCenters() to find cluster center locations)
    */
   public int[] cluster(int k, Sequence data, boolean bTrack)
   {
      int n = data.length();
      int nd = data.get(0).getNumDims();
      centers = new FeatureVec[k];
      FeatureVec[] oldCenters = new FeatureVec[k];
      double pw[][] = new double[n][k];

      if (bTrack) track = new ArrayList<FeatureVec[]>();

      if (k > n)
      {
         System.err.printf("Warning: invalid k-means invocation; "
               + "too many clusters (k=%d with %d points", k, n);
         return null;
      }

      // randomly initialize cluster centers around global mean
      FeatureVec gmean = data.getMean();      
      System.err.printf("mean(%d): %s\n", n, gmean);
      
      for(int i = 0; i < k; i++)
      {
         centers[i] = new FeatureVec(gmean);
         // TODO scale by sample var?
         centers[i]._add(FeatureVec.rand(nd)._sub(-0.5)._mul(0.1)); 
         oldCenters[i] = new FeatureVec(centers[i]);
      }
      if (bTrack) track.add(dup(centers));
      //System.err.printf("Initial error: %.6f\n", getAvgError(data));

      // iterate and update centers
      boolean bEps = false;
      TimerMS timer = new TimerMS();
      int iter;
      double last_eps = 0;
      for(iter = 0; (iter < nMaxIters || nMaxIters < 0) && !bEps; iter++)
      {
         getMembershipProbs(data, pw);
         updateCenters(data, pw);
         if (bTrack) track.add(dup(centers));

         /*
          * int nm[] = getMembershipCount(data); System.err.printf("Iter %d: %.6f -- ",
          * iter + 1, getError(data)); for(int i = 0; i < nm.length; i++)
          * System.err.printf("%d ", nm[i]); System.err.println();
          */

         // TODO stopping criteria could depend on subclass (eg, change in loglik)
         
         // see if the change is small enough to stop
         bEps = true;
         last_eps = 0;
         for(int i = 0; i < k; i++)
         {
            double d = centers[i].dist(oldCenters[i]);
            if (d > last_eps) last_eps = d;
            if (d >= epsilon)
            {
               bEps = false;
               break;
            }
         }
         if (!bEps) for(int i = 0; i < k; i++)
            oldCenters[i].copyFrom(centers[i]);
      }
      if (bVerbose)
      {
         System.err.printf("Clustering converged after %d iterations (%dms).\n", iter, timer
               .time());
         System.err.printf("(%s) Final error (k=%d): %.6f\n", getClass().getName(), k,
               getAvgError(data));
      }

      return getMembership(data);
   }

   /**
    * @return index of the most likely cluster
    */
   public int[] getMembership(Sequence data)
   {
      int n = data.length();
      int k = getK();
      int w[] = new int[n];
      double[][] pw = getMembershipProbs(data);

      for(int i = 0; i < n; i++)
      {
         int iBest = 0;
         double xBest = pw[i][0];
         for(int j = 1; j < k; j++)
         {
            if (pw[i][j] > xBest)
            {
               xBest = pw[i][j];
               iBest = j;
            }
         }
         w[i] = iBest;
      }
      return w;
   }

   /**
    * Determine how many data points are accounted for by each cluster
    * 
    * @param data data to test
    * @return number of data points "belonging" to each cluster center
    */
   public int[] getMembershipCount(Sequence data)
   {
      int[] m = getMembership(data);
      return getMembershipCount(data, m);
   }

   /**
    * Determine how many data points are accounted for by each cluster
    * 
    * @param data data to test
    * @param membership membership info (returned by getMembership method)
    * @return number of data points "belonging" to each cluster center
    */
   public int[] getMembershipCount(Sequence data, int[] membership)
   {
      int n = data.length();
      int k = getK();
      int nm[] = new int[k];
      for(int i = 0; i < n; i++)
         nm[membership[i]]++;
      return nm;
   }

   /**
    * @return cluster membership probabilities -- return[i][j] = p(w_j|x_i)
    */
   public double[][] getMembershipProbs(Sequence data)
   {
      return getMembershipProbs(data, new double[data.length()][getK()]);
   }

   /**
    * Compute cluster membership probabilities
    * 
    * @param data points to cluster
    * @param pw storage for cluster membership info
    * @return cluster membership probabilities -- return[i][j] = p(w_j|x_i)
    */
   public abstract double[][] getMembershipProbs(Sequence data, double[][] pw);

   /**
    * Update the center estimates from the membership data
    * 
    * @param data points to cluster
    * @param pw membership info
    */
   protected abstract void updateCenters(Sequence data, double pw[][]);

   /**
    * Compute the average error (dist from data point to cluster center)
    * 
    * @param data data to cluster
    * @return average error (average distance to cluster center)
    */
   public double getAvgError(Sequence data)
   {
      int ii[] = getMembership(data);
      double err = 0;
      for(int i = 0; i < ii.length; i++)
         err += centers[ii[i]].dist(data.get(i));
      return err / ii.length;
   }

}
