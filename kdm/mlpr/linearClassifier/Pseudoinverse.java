package kdm.mlpr.linearClassifier;

import kdm.data.*;
import kdm.util.*;
import java.util.Arrays;
import no.uib.cipr.matrix.*;

/** psuedoinverse method for 2-class linear classification (see DHS 5.8.1) */
public class Pseudoinverse
{
   /** @return MSE solution for linear classification: ret*[1 x1 x2 ... xd)=0 */
   public FeatureVec solve(Sequence data1, Sequence data2)
   {
      int nData1 = data1.length();
      int nData2 = data2.length();
      int N = nData1 + nData2;
      int D = data1.getNumDims();
      assert(D == data2.getNumDims());
      
      // build the Y matrix
      DenseMatrix Y = new DenseMatrix(N, D+1);
      for(int i=0; i<nData1; i++)
      {
         Y.set(i, 0, 1.0);
         FeatureVec x = data1.get(i);
         for(int d=1; d<=D; d++)
            Y.set(i, d, x.get(d-1));
      }
      for(int i=0; i<nData2; i++)
      {
         Y.set(i+nData1, 0, -1.0);
         FeatureVec x = data2.get(i);
         for(int d=1; d<=D; d++)
            Y.set(i+nData1, d, -x.get(d-1));
      }
      
      // create b matrix
      double[] ones = new double[N];
      Arrays.fill(ones, 1.0);
      DenseVector b = new DenseVector(ones);      
      
      // create Y^t*b
      Vector Ytb = new DenseVector(D+1); 
      Y.transMult(b, Ytb);
      
      // create Y^t*Y
      DenseMatrix YtY = new DenseMatrix(D+1, D+1); 
      Y.transAmult(Y, YtY);
      
      // solve for a
      Vector a = new DenseVector(D+1);
      YtY.solve(Ytb, a);      
      
      FeatureVec fvSolution = new FeatureVec(D+1);
      for(int i=0; i<=D; i++) fvSolution.set(i, a.get(i));
      return fvSolution;
   }
   
   /** @return MSE solution for weighted linear classification: ret*[1 x1 x2 ... xd)=0 (doesn't work!) */
   public FeatureVec solve(Sequence data1, Sequence data2, double[] w)
   {
      int nData1 = data1.length();
      int nData2 = data2.length();
      int N = nData1 + nData2;
      int D = data1.getNumDims();
      assert(D == data2.getNumDims());
      
      // build the Y matrix
      DenseMatrix Y = new DenseMatrix(N, D+1);
      for(int i=0; i<nData1; i++)
      {
         Y.set(i, 0, 1);
         FeatureVec x = data1.get(i);
         for(int d=1; d<=D; d++)
            Y.set(i, d, x.get(d-1));
      }
      for(int i=0; i<nData2; i++)
      {
         Y.set(i+nData1, 0, -1);
         FeatureVec x = data2.get(i);
         for(int d=1; d<=D; d++)
            Y.set(i+nData1, d, -x.get(d-1));
      }
      
      // compute weighted version of Y
      DenseMatrix Yw = Y.copy();
      for(int i=0; i<N; i++)
         for(int j=1; j<D; j++)
            Yw.set(i,j, Yw.get(i,j)*w[i]);
      
      // create b matrix
      double[] ones = new double[N];
      Arrays.fill(ones, 1.0);
      DenseVector b = new DenseVector(ones);      
      
      // create Y^t*b
      Vector Ytb = new DenseVector(D+1); 
      Yw.transMult(b, Ytb);
      
      // create Y^t*Y
      DenseMatrix YtY = new DenseMatrix(D+1, D+1); 
      Yw.transAmult(Yw, YtY);
      
      // solve for a
      Vector a = new DenseVector(D+1);
      YtY.solve(Ytb, a);      
      
      FeatureVec fvSolution = new FeatureVec(D+1);
      for(int i=0; i<=D; i++) fvSolution.set(i, a.get(i));
      return fvSolution;
   }
   
   public static void main(String[] args)
   {
      Pseudoinverse pi = new Pseudoinverse();
      Sequence data1 = new Sequence();
      data1.add(new FeatureVec(2, 1, 2));
      data1.add(new FeatureVec(2, 2, 0));
      Sequence data2 = new Sequence();
      data2.add(new FeatureVec(2, 3, 1));
      data2.add(new FeatureVec(2, 2, 3));
      FeatureVec a = pi.solve(data1, data2);
      System.err.printf("a: %s\n", a);
      double[] w = new double[]{50, 100, 50, 100};
      Library.normalize(w);
      a = pi.solve(data1, data2, w);      
      System.err.printf("a(w): %s\n", a);
      
   }
   
}
