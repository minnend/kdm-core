package kdm.mlpr;

import kdm.data.*;
import kdm.models.*;
import kdm.util.*;
import java.util.*;
import java.io.*;

/**
 * Cluster data assuming the clusters are spherical gaussian distributions and all have
 * equal priors
 */
public class SphericalGaussKMeans extends AbstractDistKMeans
{
   // TODO ! merge with DiagGaussKMeans
   // TODO !! need to init variance as well!
   protected GaussianSpherical g[];
   protected boolean bEqualVar;

   public SphericalGaussKMeans()
   {
      this(DEF_MAX_ITERS, DEF_EPSILON, false);
   }

   public SphericalGaussKMeans(int _nMaxIters, double _epsilon, boolean _bEqualVar)
   {
      super(_nMaxIters, _epsilon);
      bEqualVar = _bEqualVar;
   }

   public GaussianSpherical getModel(int i)
   {
      return g[i];
   }

   /**
    * Compute cluster membership probabilities
    * 
    * @param data points to cluster
    * @param pw storage for cluster membership info
    * @return cluster membership probabilities -- return[i][j] = p(w_j|x_i)
    */
   public double[][] getMembershipProbs(Sequence data, double[][] pw)
   {
      int n = data.length();
      int K = centers.length;

      // create gaussian model if necessary
      if (g == null || g.length != getK())
      {
         g = new GaussianSpherical[K];
         for(int i = 0; i < K; i++)
         {
            g[i] = new GaussianSpherical(data.getNumDims());
            g[i].setReport(GaussianSpherical.Report.loglik);
         }
      }
      
      // copy centers into gaussian model
      for(int i = 0; i < K; i++)
         g[i].setMean(centers[i]);
      
      // average variances if necessary
      if (bEqualVar)
      {
         double varsum = 0;
         for(int i=0; i<K; i++) varsum += g[i].getVar();
         varsum /= K;
         for(int i=0; i<K; i++) g[i].setVar(varsum);
      }

      // now calc membership probs
      for(int i = 0; i < n; i++)
      {
         FeatureVec fv = data.get(i);
         double wsum = Library.LOG_ZERO;
         for(int j = 0; j < K; j++)
         {
            pw[i][j] = g[j].eval(fv);
            wsum = Library.logadd(wsum, pw[i][j]);
         }
         
         // normalize and convert to a real probability (not loglik)
         for(int j = 0; j < K; j++)
            pw[i][j] = Math.exp(pw[i][j] - wsum);         
      }

      return pw;
   }

   /**
    * Update the center estimates from the membership data
    * 
    * @param data points to cluster
    * @param pw membership info
    */
   protected void updateCenters(Sequence data, double pw[][])
   {
      int K = getK();
      assert (g!=null && g.length==K);
      double[][] pwi = Library.transpose(pw);
      for(int i = 0; i < K; i++)
      {
         g[i].learn(data, pwi[i]);
         centers[i].copyFrom(g[i].getMean());
      }
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
      Sequence data = new Sequence("data", x);

      SphericalGaussKMeans kmeans = new SphericalGaussKMeans(500, FuzzyKMeans.DEF_EPSILON, true);
      int mem[] = kmeans.cluster(3, data, true);
      FeatureVec centers[] = kmeans.getCenters();
      for(int i = 0; i < centers.length; i++)
         System.err.printf("Center %d: %s\n", i + 1, centers[i]);

      ArrayList<FeatureVec[]> track = kmeans.getTracks();
      try
      {
         // TODO: change path to appropriate value
         PrintWriter out = new PrintWriter(new FileWriter("/home/dminn/research/matlab/track.txt"));
         for(int i = 0; i < track.size(); i++)
         {
            FeatureVec[] a = track.get(i);
            for(int j = 0; j < a.length; j++)
            {
               int D = a[j].getNumDims();
               for(int k = 0; k < D; k++)
                  out.printf("%f ", a[j].get(k));
            }
            out.println();
         }
         out.close();
      } catch (IOException e)
      {
         System.err.println(e);
      }

      for(int i = 0; i < kmeans.getK(); i++)
      {
         GaussianSpherical g = kmeans.getModel(i);
         System.err.printf("cluster %d variance: %.6f\n", i + 1, g.getVar());
      }

      System.err.printf("figure;hold on;plot(x(:,1),x(:,2),'b.');y=load('track.txt');"
            + "plot(y(:,1),y(:,2),'g');plot(y(:,3),y(:,4),'g');plot(y(:,5),y(:,6),'g');"
            + "plot([%.4f %.4f %.4f],[%.4f %.4f %.4f],'r.');\n", centers[0].get(0), centers[1]
            .get(0), centers[2].get(0), centers[0].get(1), centers[1].get(1), centers[2].get(1));
   }

}
