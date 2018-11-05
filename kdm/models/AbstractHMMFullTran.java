package kdm.models;

import java.util.*;

import kdm.data.*;
import kdm.util.Library;

/** abstract base class for unconstrained (full transition matrix) HMMs */
public abstract class AbstractHMMFullTran extends AbstractHMM
{
   /** tran[i][j] = loglik of tran from state i to state j */
   protected double[][] tran;
      
   public AbstractHMMFullTran(int nStates, int nDims)
   {
      super(nDims);
      tran = new double[nStates][nStates];

      // build default transition matrix and obs dist
      piStart = new double[nStates];
      piEnd = new double[nStates];
      piLeave = new double[nStates];
      Arrays.fill(piStart, Math.log(1.0 / nStates)); // start in any state
      Arrays.fill(piEnd, Math.log(1.0 / nStates));  // end in any state
      Arrays.fill(piLeave, Math.log(0.1)); 
      double pself = 0.9;
      double llself = Math.log(pself);
      double llother = Math.log((1.0-pself) / (nStates-1));
      for(int i = 0; i < nStates; i++){
         for(int j = 0; j < nStates; j++){
            if (i == j) tran[i][j] = llself;
            else tran[i][j] = llother;
         }
      }
   }

   @Override
   public double[][] getFullTransMatrix()
   {
      return tran;
   }
   
   @Override
   public double eval(Sequence seq)
   {
      calcAlpha(seq);      
      int iLast = seq.length()-1;
      int nStates = getNumStates();
      double loglik = Library.LOG_ZERO;
      for(int i=0; i<nStates; i++)
         loglik = Library.logadd(loglik, alpha[iLast][i] + piEnd[i]);
      return loglik;
   }

   /**
    * Compute the forward (alpha) matrix for the given sequence.
    */
   protected void calcAlpha(Sequence seq)
   {
      int T = seq.length();
      int nStates = getNumStates();
      alpha = Library.allocMatrixDouble(T, nStates, Library.LOG_ZERO);
      calcB(seq);

      // we can start anywhere, according to the prior probs
      for(int i = 0; i < nStates; i++)
         alpha[0][i] = piStart[i] + bmat[i][0];

      // fill in the alpha matrix
      for(int t = 1; t < T; t++){
         // calc j -> i
         for(int i = 0; i < nStates; i++){
            for(int j = 0; j < nStates; j++)
               alpha[t][i] = Library.logadd(alpha[t][i], alpha[t - 1][j] + tran[j][i]);
            alpha[t][i] += bmat[i][t];
         }
      }
   }

   /**
    * Compute the backward (beta) matrix for the given sequence.
    */
   protected void calcBeta(Sequence seq)
   {
      int T = seq.length();
      int nStates = getNumStates();
      int iLast = nStates - 1;
      beta = Library.allocMatrixDouble(T, nStates, Library.LOG_ZERO);
      calcB(seq);

      // end probability according to prior
      for(int i = 0; i < nStates; i++)
         beta[T-1][i] = piEnd[i] + bmat[i][T - 1];

      for(int t = T - 2; t >= 0; t--){
         // calc i -> j
         for(int i = 0; i < nStates; i++){
            assert (beta[t][i] == Library.LOG_ZERO);
            for(int j = 0; j < nStates; j++){
               double a = tran[i][j] + bmat[j][t + 1] + beta[t + 1][j];
               if (a == Library.LOG_ZERO) continue;
               beta[t][i] = Library.logadd(beta[t][i], a);
            }
         }
      }
   }

   @Override
   public double viterbi(Sequence seq, double m[][], int par[][])
   {
      int T = seq.length();
      int nStates = getNumStates();
      calcB(seq);      

      // start in any state according to prior
      for(int i = 0; i < nStates; i++)
      {
         m[0][i] = piStart[i] + bmat[i][0];
         par[0][i] = -1;
      }

      // fill in the trellis
      for(int t = 1; t < T; t++){
         // best to come to i from which j?
         for(int i = 0; i < nStates; i++){
            int iBest = 0;
            double vBest = m[t - 1][0] + tran[0][i];
            for(int j=1; j<nStates; j++){
               double v = m[t - 1][j] + tran[j][i];
               if (v > vBest){
                  vBest = v;
                  iBest = j;
               }
            }
            m[t][i] = vBest + bmat[i][t];
            par[t][i] = iBest;
         }
      }

      // extract path
      path = new int[T];
      
      // find best last state
      int iLast = 0;
      for(int i=1; i<nStates; i++)
         if (m[T-1][i] > m[T-1][iLast]) iLast = i;
      path[T - 1] = iLast;
      for(int t = T - 2; t >= 0; t--)
         path[t] = par[t + 1][path[t + 1]];

      // return viterbi prob of best final state
      return m[T - 1][iLast];
   }

   @Override
   public Sequence sample()
   {
      assert false : "nyi";
      return null;
   }
}
