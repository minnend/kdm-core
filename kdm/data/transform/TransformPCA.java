package kdm.data.transform;

import kdm.data.*;
import kdm.io.DataLoader.*;
import kdm.io.DataSaver.*;
import kdm.io.Def.DataDefLoader;
import no.uib.cipr.matrix.*;
import java.util.*;
import java.io.*;
import kdm.util.*;

/** Calculate the first k principal components */
public class TransformPCA extends DataTransform
{
   protected int k, actualk;
   protected double[] eigval;
   protected FeatureVec[] eigvec;

   public TransformPCA()
   {
      k = -1; // return all of them
      actualk = -1;
   }

   public TransformPCA(int _k)
   {
      setK(_k);
   }
   
   public void dumpParams()
   {
      System.err.printf("%s: k=%d\n", getClass(), k);
   }

   /** number of principal components desired (-1 for all that data supports) */
   public void setK(int _k)
   {
      k = _k;
      if (k > 0) actualk = k;
      eigval = null;
      eigvec = null;
   }

   /** @return number of principal components actually computed */
   public int getK()
   {
      return actualk;
   }

   /** @return number of principal components requested (-1 for all that data supports) */
   public int getRequestedK()
   {
      return k;
   }

   /** @return variance of the princiapl axes in decreasing order */
   public double[] getVariance()
   {
      if (eigval == null)
      {
         System.err.printf("Warning: requested variance without calculating evd\n");
         return null;
      }
      return eigval;
   }

   /** @return principal axes in order of descending variance */
   public FeatureVec[] getAxes()
   {
      if (eigvec == null)
      {
         System.err.printf("Warning: requested principal components without calculating evd\n");
         return null;
      }
      return eigvec;
   }

   /**
    * Perform the PCA calculation
    * 
    * @param data data for which to calc PCA
    * @return true if successful
    */
   public boolean pca(Sequence data) throws NotConvergedException
   {
      return pca(data, data.getMean());
   }

   /**
    * Perform the PCA calculation. This method uses an unbiased estimate for the
    * covariance.
    * 
    * @param data data for which to calc PCA
    * @param mean mean of data (null to avoid mean subtraction)
    * @return true if successful
    */
   public boolean pca(Sequence data, FeatureVec mean) throws NotConvergedException
   {
      int nd = data.getNumDims();
      int n = data.length();
      assert (mean == null || mean.getNumDims() == nd);
      if (n < nd) return false; // not enough examples
      if (k < 1) actualk = Math.min(nd, n);
      EVD evd = new EVD(nd, false, true);

      // create the MTJ matrix
      DenseVector[] v = new DenseVector[n];
      if (mean != null)
      {
         for(int i = 0; i < n; i++)
            v[i] = data.get(i).sub(mean).getMTJVec(true);
      }
      else
      {
         for(int i = 0; i < n; i++)
            v[i] = data.get(i).getMTJVec(true);
      }
      DenseMatrix m = new DenseMatrix(v);

      // calc covariance matrix
      DenseMatrix cov = new DenseMatrix(nd, nd);

      // perform the eigenvalue decomposition
      evd.factor(cov);

      // sort the eigX by decreasing eigenvalue
      eigval = evd.getRealEigenvalues();
      int[] ii = Library.sort(eigval);

      // create eigenvector info
      m = evd.getRightEigenvectors();
      double[] evdata = m.getData();
      eigvec = new FeatureVec[actualk];
      for(int i = 0; i < actualk; i++)
      {
         eigvec[i] = new FeatureVec(nd);
         int base = nd * ii[nd-i-1];
         for(int j = 0; j < nd; j++)
            eigvec[i].set(j, evdata[base + j]);
      }

      // only keep the requested eigenvalues
      if (actualk < eigval.length)
      {
         double[] x = eigval;
         eigval = new double[actualk];
         for(int i=0; i<actualk; i++)
            eigval[i] = x[nd-i-1];
      }

      return true;
   }
   
   /**
    * Project the given data using the already-computed (via pca()) eigenvectors
    * @param data data to project
    * @return new sequence containing projected data
    */
   public Sequence project(Sequence data)
   {
      return project(data, data.getMean());
   }

   /**
    * Project the given data using the already-computed (via pca()) eigenvectors
    * @param data data to project
    * @param fvMean mean of the data
    * @return new sequence containing projected data
    */
   public Sequence project(Sequence data, FeatureVec fvMean)
   {
      Sequence seq = new Sequence("PCA: " + data, data.getFreq(), data.getStartMS());
      int T = data.length();
      for(int t = 0; t < T; t++)
      {
         FeatureVec fv = new FeatureVec(actualk);
         for(int i = 0; i < actualk; i++)
            fv.set(i, data.get(t).sub(fvMean).dot(eigvec[i]));
         seq.add(fv);
      }
      seq.copyMeta(data);
      return seq;
   }
   
   /**
    * @return original data projected onto the k principal components
    */
   @Override
   public Sequence transform(Sequence data)
   {
      try
      {                 
         pca(data);
         return project(data);         
      } catch (NotConvergedException e)
      {
         e.printStackTrace();
         return null;
      }
   }

   /**
    * Sample application for simple PCA tranformations
    */
   public static void main(String args[]) throws Exception
   {
      int k = 1;
      String sOut = "data%02d.txt";
      
      if (args.length==0)
      {
         System.err.println("USAGE: java ~.TransformPCA <load file> [<K>] [<save name>]");         
         System.err.println("  K = # comps to keep (def: 1)");
         System.err.printf("  save name should have a %d format to number the output files (def: %s)", sOut);
         System.exit(1);
      }
      
      String sFile = args[0];
      if (args.length > 1) k = Integer.parseInt(args[1]);
      if (args.length > 2) sOut = args[2];
            
      if (sFile.endsWith(".def"))
      {
         ArrayList<Sequence> seqs = DataDefLoader.loadSeqs(new File(sFile), null);
         Sequence all = new Sequence(seqs.get(0));
         for(int i=1; i<seqs.size(); i++)
            all.append(seqs.get(i), true, false);
         System.err.printf("Loaded %d seqs, %d total frames.\n", seqs.size(), all.length());
         
         TransformPCA pca = new TransformPCA(1);
         pca.pca(all);
         
         DSRaw saver = new DSRaw();
         for(int iSeq=0; iSeq<seqs.size(); iSeq++)
         {
            Sequence seq = seqs.get(iSeq);
            Sequence seqpca = pca.project(seq);
            String sFileOut = String.format(sOut, iSeq+1);
            System.err.printf("Saving file: %s\n", sFileOut);
            saver.save(seqpca, sFileOut);
         }
      }
      else{
         assert false : "support for direct loading not implemented yet";
      
         // TODO: same functionality as loading a .def
         DLRaw loader = new DLRaw();
         Sequence seq = loader.load(sFile);
         TransformPCA pca = new TransformPCA(1);
         Sequence seq2 = pca.transform(seq);
         double[] v = pca.getVariance();
         System.err.printf("\nk=%d  v=[", pca.getK());
         for(double x : v)
            System.err.printf("%.4f  ", x);
         System.err.printf("]\n\n");
         FeatureVec[] evec = pca.getAxes();
         for(int i = 0; i < evec.length; i++)
            System.err.printf("Evec %d: %s\n", i + 1, evec[i]);
         System.err.println();
         for(int i = 0; i < seq2.length() && i < 5; i++)
            System.err.printf("FV %d: %s\n", i + 1, seq2.get(i));
      }
   }
}
