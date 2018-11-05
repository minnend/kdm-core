package kdm.models;

import kdm.data.*;
import kdm.util.*;

import java.util.*;

/**
 * A left-right HMM with multivariate, independent Gaussian observation pdfs.
 */
public class HmmLR extends AbstractHMMLR
{
   protected boolean bUpdateVar = true;
   protected FeatureVec minVar = null;

   public HmmLR(int nStates, int nDims)
   {
      this(nStates, 0, nDims);
   }

   public HmmLR(int nStates, int nSkip, int nDims)
   {
      super(nStates, nSkip, nDims);
      states = new ProbFVModel[nStates];
      for(int i = 0; i < nStates; i++)
         states[i] = new GaussianDiagonal(nDims);
   }

   /** set minimum variance for each dimension */
   public void setMinVar(FeatureVec minVar)
   {
      this.minVar = minVar;
      ensureMinVar();
   }

   /**
    * force all variances to be at least as large as the specified (via setMinVar) min; no update if minVar is
    * null
    */
   public void ensureMinVar()
   {
      if (minVar == null) return;
      int nStates = getNumStates();
      for(int i = 0; i < nStates; i++){
         GaussianDiagonal gd = (GaussianDiagonal)states[i];
         gd.setVar(gd.getVar()._max(minVar));
      }
   }
   
   /** check the transition matrix and remove any states that can't be reached */
   public boolean removeUnusedStates()
   {
      int nStates = getNumStates();
      boolean[] used = new boolean[nStates];
      used[0] = true;
      for(int i=0; i<nStates; i++){
         if (!used[i]) continue;
         for(int j=0; j<tran[i].length; j++)
            if (tran[i][j] > Library.LOG_ZERO) used[i+j] = true;
      }
      
      // how many states are left?
      int nStates2 = 0;
      for(int i=0; i<nStates; i++) if(used[i]) nStates2++;
      if (nStates2 == nStates) return false; // anything to remove?
      
      ProbFVModel[] states2 = new ProbFVModel[nStates2];
      alpha = null;
      beta = null;
      bmat = null;
      seqLastCalcB = null;
      double[] piStart2 = new double[nStates2];
      double[] piEnd2 = new double[nStates2];
      double[] piLeave2 = new double[nStates2];
      double[][] tran2 = new double[nStates2][];
      
      // copy over only the used states
      int i2 = 0;
      for(int i=0; i<nStates; i++){
         if (!used[i]) continue;
         
         piStart2[i2] = piStart[i];
         piEnd2[i2] = piEnd[i];
         piLeave2[i2] = piLeave[i];
         states2[i2] = states[i];
         
         int nAdj = 1;
         for(int j=1; j<tran[i].length; j++)
            if (used[i+j]) nAdj++;
         tran2[i2] = new double[nAdj];
         int j2 = 0;
         for(int j=0; j<tran[i].length; j++)
            if (used[i+j]) tran2[i2][j2++] = tran[i][j];
         i2++;
      }
      
      // update variables in this model
      states = states2;
      piStart = piStart2;
      piEnd = piEnd2;
      piLeave = piLeave2;
      tran = tran2;

      return true;
   }

   public void setVar(FeatureVec var)
   {
      int nStates = getNumStates();
      for(int i = 0; i < nStates; i++)
         ((GaussianDiagonal)states[i]).setVar(var);
   }

   public void mulVar(double x)
   {
      int nStates = getNumStates();
      for(int i = 0; i < nStates; i++)
         ((GaussianDiagonal)states[i]).mulVar(x);
   }

   public FeatureVec getMean(int i)
   {
      return ((GaussianDiagonal)states[i]).getMean();
   }

   public FeatureVec getVar(int i)
   {
      return ((GaussianDiagonal)states[i]).getVar();
   }

   public void setUpdateVar(boolean b)
   {
      bUpdateVar = b;
   }

   public boolean getUpdateVar()
   {
      return bUpdateVar;
   }
   
   /**
    * initialize this LR HMM using overlapping segments
    */
   public boolean init_segk_overlap(ArrayList<? extends Sequence> vtrain, double percentOverlap)
   {
      assert (vtrain.get(0).getNumDims() == nDims);

      int nStates = getNumStates();
      int nSeqs = vtrain.size();
      if (nStates % 2 == 0){
         assert false : String.format(
               "Error: can't use init_segk_overlap with even number of states (%d)\n", nStates);
         return false;
      }

      // divide each training sequence into the appropriate blocks and calc stats
      GaussianDyn1D[][] gmd = new GaussianDyn1D[nStates][nDims];
      for(int i = 0; i < nStates; i++)
         for(int j = 0; j < nDims; j++)
            gmd[i][j] = new GaussianDyn1D();

      double denom = nStates - (nStates - 1) * percentOverlap;
      for(int iSeq = 0; iSeq < nSeqs; iSeq++){
         Sequence seq = vtrain.get(iSeq);
         int T = seq.length();
         double fpd = (double)T / denom;
         double foverlap = fpd * percentOverlap;
         for(int iState = 0; iState < nStates; iState++){
            int a = (int)Math.round(iState * (fpd - foverlap));
            int b = (int)Math.round(iState * (fpd - foverlap) + fpd);
            // int a = (int)Math.floor(iState * (fpd - foverlap));
            // int b = (int)Math.ceil(iState * (fpd - foverlap) + fpd);
            b = Math.max(b, a + 1);
            if (b>=T){
               a = T-1;
               b = T;
            }

            //int a2 = (int)Math.floor(iState * (fpd - foverlap));
            //int b2 = (int)Math.ceil(iState * (fpd - foverlap) + fpd);
            //b2 = Math.max(b2, a2+1);
            //System.err.printf("T=%d  iStates=%d/%d  [%d,%d)  [%d, %d)\n", T, iState, nStates, a,b,a2,b2); 
            
            for(int j = 0; j < nDims; j++)
               for(int k = a; k < b; k++)
                  gmd[iState][j].add(seq.get(k, j), false);
         }
      }

      // update obs pdf
      for(int i = 0; i < nStates; i++)
         for(int j = 0; j < nDims; j++){
            gmd[i][j].update();
            ((GaussianDiagonal)states[i]).setMean(j, gmd[i][j].getMean());
         }
      if (bUpdateVar && nSeqs > 1){
         for(int i = 0; i < nStates; i++)
            for(int d = 0; d < nDims; d++)
               ((GaussianDiagonal)states[i]).setVar(d, gmd[i][d].getVar());
         ensureMinVar();
      }

      return true;
   }

   @Override
   public void init_segk(ArrayList<? extends Sequence> vtrain)
   {
      assert (vtrain.get(0).getNumDims() == nDims);
      int nStates = getNumStates();

      // divide each training sequence into nStates blocks and calc stats
      GaussianDyn1D[][] gmd = new GaussianDyn1D[nStates][nDims];
      int nSeqs = vtrain.size();
      for(int i = 0; i < nStates; i++)
         for(int j = 0; j < nDims; j++)
            gmd[i][j] = new GaussianDyn1D();
      for(int iSeries = 0; iSeries < nSeqs; iSeries++){
         Sequence seq = vtrain.get(iSeries);
         int T = seq.length();
         for(int i = 0; i < nStates; i++){
            int a = i * T / nStates;
            int b = (i + 1) * T / nStates;
            b = Math.max(b, a + 1);
            for(int j = 0; j < nDims; j++)
               for(int k = a; k < b; k++)
                  gmd[i][j].add(seq.get(k, j), false);
         }
      }

      // update obs pdf
      for(int i = 0; i < nStates; i++)
         for(int j = 0; j < nDims; j++){
            gmd[i][j].update();
            ((GaussianDiagonal)states[i]).setMean(j, gmd[i][j].getMean());
         }
      if (bUpdateVar && nSeqs > 1){
         for(int i = 0; i < nStates; i++)
            for(int j = 0; j < nDims; j++)
               ((GaussianDiagonal)states[i]).setVar(j, gmd[i][j].getVar());
         ensureMinVar();
      }
   }

   @Override
   public int train_viterbi(ArrayList<? extends Sequence> vtrain)
   {
      assert (vtrain.get(0).getNumDims() == nDims);
      int nStates = getNumStates();
      int nSeq = vtrain.size();
      int oldpath[][] = new int[nSeq][];
      GaussianDyn1D[][] gmd = new GaussianDyn1D[nStates][nDims];
      int nObsPerState[] = new int[nStates];
      int nTran[][] = new int[nStates][];
      boolean bConverge = false;
      int iter = 0;

      for(int i = 0; i < nStates; i++)
         nTran[i] = new int[tran[i].length];

      for(; iter < NMAX_ITER && !bConverge; iter++){
         bConverge = true; // hope for the best

         // create / reset stat accumulators
         Arrays.fill(nObsPerState, 0);
         for(int i = 0; i < nStates; i++){
            Arrays.fill(nTran[i], 0);
            for(int j = 0; j < nDims; j++)
               if (gmd[i][j] == null) gmd[i][j] = new GaussianDyn1D();
               else gmd[i][j].reset();
         }

         // calc viterbi paths, accum stats
         for(int iSeries = 0; iSeries < nSeq; iSeries++){
            Sequence seq = vtrain.get(iSeries);
            int T = seq.length();
            calcB(seq);
            viterbi(seq);

            if (oldpath[iSeries] == null){
               oldpath[iSeries] = path.clone();
               bConverge = false;
            }
            else{
               // check for (lack of) convergence
               if (bConverge){
                  for(int i = 0; i < T; i++){
                     if (path[i] != oldpath[iSeries][i]){
                        bConverge = false;
                        break;
                     }
                  }
               }
               oldpath[iSeries] = path.clone();
            }

            // accum stats based on viterbi path
            for(int i = 0; i < T; i++){
               int iState = path[i];
               nObsPerState[iState]++;
               if (i < T - 1) nTran[iState][path[i + 1] - iState]++;
               for(int j = 0; j < nDims; j++)
                  gmd[iState][j].add(seq.get(i, j), false);
            }
         }

         for(int i = 0; i < nStates; i++){
            // reest tran matrix
            for(int j = 0; j < tran[i].length; j++){
               int nTrans = Library.sum(nTran[i]);
               tran[i][j] = Math.log((double)nTran[i][j] / nTrans);
            }

            // reest obs pdf
            for(int j = 0; j < nDims; j++){
               gmd[i][j].update();
               ((GaussianDiagonal)states[i]).setMean(j, gmd[i][j].getMean());
               if (bUpdateVar) ((GaussianDiagonal)states[i]).setVar(j, gmd[i][j].getVar());
            }
         }
         
         if (bUpdateVar) ensureMinVar();
      }      
      return iter;
   }

   @Override
   public int train_bw(ArrayList<? extends Sequence> vtrain)
   {
      assert (vtrain.get(0).getNumDims() == nDims);
      int nStates = getNumStates();
      int nSeq = vtrain.size();
      boolean bConverge = false;
      double prevLogProb = Library.NEGINF;
      int iter = 0;

      for(; iter < NMAX_ITER && !bConverge; iter++){
         bConverge = true;
         
         double logprob = Library.LOG_ONE;
         double[][] tran_acc = new double[nStates][];
         double[][][] gamma = new double[nSeq][][];
         double[] wsum = new double[nStates];
         
         for(int i = 0; i < nStates; i++)
            tran_acc[i] = new double[tran[i].length];

         for(int iSeq = 0; iSeq < nSeq; iSeq++){
            Sequence seq = vtrain.get(iSeq);
            int T = seq.length();
            int iLast = T - 1;
            int iLastState = nStates - 1;

            calcB(seq);

            // create gamma and xi storage
            gamma[iSeq] = new double[T][nStates];
            double[][][] xi = new double[iLast][nStates][];
            for(int t = 0; t < iLast; t++)
               for(int j = 0; j < nStates; j++)
                  xi[t][j] = new double[tran[j].length];

            // compute alpha and beta matrices
            calcAlpha(seq);
            calcBeta(seq);

            // logprob_i is the log-likelihood of the i^th training sequence
            double logprob_i = alpha[iLast][iLastState];
            assert(!Double.isInfinite(logprob_i) && !Double.isNaN(logprob_i)) : logprob_i;
            logprob += logprob_i;

            // compute gamma and xi from alpha and beta
            for(int t = 0; t < T; t++)
               for(int i = 0; i < nStates; i++){
                  gamma[iSeq][t][i] = alpha[t][i] + beta[t][i] - logprob_i;
                  if (t < iLast){
                     for(int j = 0; j < tran[i].length; j++)
                        xi[t][i][j] = alpha[t][i] + beta[t + 1][i + j] + tran[i][j] + bmat[i + j][t + 1] - logprob_i;
                  }
               }

            for(int i = 0; i < nStates; i++){
               for(int t = 0; t < T; t++){
                  gamma[iSeq][t][i] = Math.exp(gamma[iSeq][t][i]);
                  wsum[i] += gamma[iSeq][t][i]; // don't count gamma[iSeq][iLast][i] for p(exit state i)
                  if (t < iLast){
                     for(int j = 0; j < tran[i].length; j++)
                        tran_acc[i][j] += Math.exp(xi[t][i][j]);
                  }
               }
            }
         }

         // check for convergence / error
         if (logprob < prevLogProb){
            // System.err.printf("\n*** Warning:\niter=%d) ll=%.8f prev=%.8f (%.8f)\n\n",
            // iter, logprob, prevLogProb, logprob-prevLogProb);
            break; // don't update params with worse estimates
         }
         if (logprob - prevLogProb > CONVERGE_THRESH) bConverge = false;
         prevLogProb = logprob;

         // normalize the weights
         for(int i = 0; i < nSeq; i++){            
            int T = vtrain.get(i).length();
            for(int t = 0; t < T; t++)
               for(int j = 0; j < nStates; j++){
                  if (Double.isNaN(wsum[j])) System.err.printf("wsum[%d] is NaN!\n", j);
                  gamma[i][t][j] /= wsum[j];
               }
         }

         // update the tran matrix and obs pdf
         for(int i = 0; i < nStates; i++){
            // update the transition matrix
            double sum = 0.0;
            for(int j = 0; j < tran[i].length; j++)
               sum += tran_acc[i][j];
            assert(!Double.isNaN(sum));
            if (sum<1e-14){
               // we never saw this state!  so just make up transition values and ignore obs dist
               double p = Math.log(1.0 / tran[i].length);
               for(int j=0; j<tran[i].length; j++) tran[i][j] = p;
               continue;
            }
            else{
               sum = Math.log(sum);
               for(int j = 0; j < tran[i].length; j++)
                  tran[i][j] = Math.log(tran_acc[i][j]) - sum;
            }

            // compute the new mean for this state
            double[] u = new double[nDims];
            for(int j = 0; j < nSeq; j++){
               Sequence seq = vtrain.get(j);
               int T = seq.length();
               for(int t = 0; t < T; t++)
                  for(int d = 0; d < nDims; d++)
                     u[d] += gamma[j][t][i] * seq.get(t, d);
            }
            ((GaussianDiagonal)states[i]).setMean(new FeatureVec(u));

            // compute the new variance for this state
            if (bUpdateVar){
               double[] v = new double[nDims];
               double sw2 = 0.0;
               int n = 0;
               for(int j = 0; j < nSeq; j++){
                  Sequence seq = vtrain.get(j);
                  int T = seq.length();
                  for(int t = 0; t < T; t++){
                     double w = gamma[j][t][i];
                     assert (w >= 0 || Double.isNaN(w)) : w;
                     if (w > 0 && !Double.isNaN(w)){
                        for(int d = 0; d < nDims; d++){
                           double x = seq.get(t, d) - u[d];
                           v[d] += w * x * x;
                        }
                        sw2 += w * w;
                        n++;
                     }
                  }
               }
               for(int d = 0; d < nDims; d++)
                  v[d] /= (1.0 - sw2);
               ((GaussianDiagonal)states[i]).setVar(new FeatureVec(v));
            }
         }
         if (bUpdateVar) ensureMinVar();
      }
      return iter;
   }

   @Override
   public String toString()
   {
      StringBuffer sb = new StringBuffer();

      int nStates = getNumStates();
      sb.append(String.format("|HMM %d states, %d dims\n", nStates, nDims));
      for(int i = 0; i < nStates; i++){
         FeatureVec fvm = ((GaussianDiagonal)states[i]).getMean();
         FeatureVec fvv = ((GaussianDiagonal)states[i]).getVar();
         sb.append(String.format(" | State %d:", i + 1));
         for(int j = 0; j < nDims; j++)
            sb.append(String.format(" N(%.2f,%.2f)", fvm.get(j), fvv.get(j)));
         sb.append(String.format(" [%.2f", Math.exp(tran[i][0])));
         for(int j = 1; j < tran[i].length; j++)
            sb.append(String.format(" %.2f", Math.exp(tran[i][j])));
         sb.append("]");
         sb.append("\n");
      }
      /*
       * for(int i=0; i<nStates; i++) { sb.append("| "); for(int j=0; j<tran[i].length; j++)
       * sb.append(String.format("%4.2f ", Math.exp(tran[i][j]))); sb.append("\n"); }
       */
      return sb.toString();
   }

   /**
    * Builds (trains) a model from the given examples.
    */
   public ProbSeqModel build(ArrayList<Sequence> examples, String sConfig)
   {
      assert false : "not yet implemented"; // TODO
      return null;
   }

   /**
    * Builds (initializes) a model from the given sequence and time range.
    */
   public ProbSeqModel build(Sequence seq, WindowLocation win, String sConfig)
   {
      assert false : "not yet implemented"; // TODO
      return null;
   }

   public boolean config(ConfigHelper chelp, String sKey, String sVal)
   {
      // TODO: currently, there aren't any configurable parameters
      System.err.printf("Error: unrecognized %s parameters: %s\n", getClass(), sKey);
      assert false;
      return false;
   }

   public boolean config(String s)
   {
      // grrr: no multiple inheritance in java
      ConfigHelper chelp = new ConfigHelper();
      return chelp.config(s, this);
   }

   @Override
   public Sequence sample()
   {
      assert false : "nyi";
      return null;
   }
}
