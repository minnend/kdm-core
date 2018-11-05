package kdm.mlpr;

import kdm.data.*;
import kdm.util.*;
import java.util.*;
import java.io.*;

/**
 * Cluster data using the fuzzy k-means algorithm (weighting via 1/dist^2 - see DHS)
 */
public class FuzzyKMeans extends AbstractDistKMeans
{
   public FuzzyKMeans()
   {
      this(DEF_MAX_ITERS, DEF_EPSILON);
   }

   public FuzzyKMeans(int _nMaxIters, double _epsilon)
   {
      super(_nMaxIters, _epsilon);
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
      int k = pw[0].length;

      for(int i = 0; i < n; i++)
      {
         FeatureVec fv = data.get(i);
         double wsum = 0;
         for(int j = 0; j < k; j++)
         {
            pw[i][j] = fv.dist(centers[j]);
            if (Math.abs(pw[i][j]) < Library.EPS)
            {
               System.err.println("center = data point");
               Arrays.fill(pw[i], 0);
               pw[i][j] = 1.0;
               wsum = 1.0;
               break;
            }
            else
            {
               pw[i][j] = 1.0 / pw[i][j];
               wsum += pw[i][j];
            }
         }
         for(int j = 0; j < k; j++)
            pw[i][j] /= wsum; // normalize
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
      int n = pw.length;
      int k = getK();

      // clear cluster centers and calc normalization constants (DHS p529 eq 32)
      for(int i = 0; i < k; i++)
      {
         double wsum = 0;
         centers[i].fill(0);
         for(int j = 0; j < n; j++)
         {
            double wb = pw[j][i] * pw[j][i];
            centers[i]._add(data.get(j).mul(wb));
            wsum += wb;
         }
         centers[i]._div(wsum); // normalize
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

      FuzzyKMeans kmeans = new FuzzyKMeans(500, FuzzyKMeans.DEF_EPSILON);
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

      System.err.printf("figure;hold on;plot(x(:,1),x(:,2),'b.');y=load('track.txt');"
            + "plot(y(:,1),y(:,2),'g');plot(y(:,3),y(:,4),'g');plot(y(:,5),y(:,6),'g');"
            + "plot([%.4f %.4f %.4f],[%.4f %.4f %.4f],'r.');\n", centers[0]
            .get(0), centers[1].get(0), centers[2].get(0), centers[0].get(1), centers[1].get(1),
            centers[2].get(1));
   }

}
