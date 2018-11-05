package kdm.models;

import java.util.*;

import kdm.data.*;
import kdm.util.*;

public class HMM extends AbstractHMMFullTran
{
   protected boolean bUpdateVar = true;

   public HMM(int nStates, int nDims)
   {
      super(nStates, nDims);

      states = new ProbFVModel[nStates];
      for(int i = 0; i < nStates; i++)
         states[i] = new GaussianDiagonal(nDims);
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

   @Override
   public ProbSeqModel build(ArrayList<Sequence> examples, String sConfig)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public ProbSeqModel build(Sequence seq, WindowLocation win, String sConfig)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void init_segk(ArrayList<? extends Sequence> vtrain)
   {
      // TODO Auto-generated method stub
      assert false : "nyi";
   }

   @Override
   public boolean init_segk_overlap(ArrayList<? extends Sequence> vtrain, double percentOverlap)
   {
      // TODO Auto-generated method stub
      assert false : "nyi";
      return false;
   }

   
   @Override
   public int train_bw(ArrayList<? extends Sequence> vtrain)
   {
      // TODO update p(start) and p(end)
      
      assert (vtrain.get(0).getNumDims() == nDims);
      int nStates = getNumStates();
      int nSeq = vtrain.size();
      boolean bConverge = false;
      double prevLogProb = Library.NEGINF;
      int iter = 0;

      for(; iter < NMAX_ITER && !bConverge; iter++){
         bConverge = true;

         double logprob = 0.0;
         double[][] tran_acc = new double[nStates][nStates];
         double[][][] gamma = new double[nSeq][][];
         double[] wsum = new double[nStates];

         for(int iSeq = 0; iSeq < nSeq; iSeq++){
            Sequence seq = vtrain.get(iSeq);
            int T = seq.length();
            int iLast = T - 1;

            calcB(seq);

            // create gamma and xi storage
            gamma[iSeq] = new double[T][nStates];
            double[][][] xi = new double[iLast][nStates][nStates];

            // compute alpha and beta matrices
            calcAlpha(seq);
            calcBeta(seq);

            // compute the loglik of this sequence
            double logprob_i = Library.LOG_ZERO;
            for(int i = 0; i < nStates; i++)
               logprob_i = Library.logadd(logprob_i, alpha[iLast][i] + piEnd[i]);
            logprob += logprob_i;

            // compute gamma and xi from alpha and beta
            for(int t = 0; t < T; t++)
               for(int i = 0; i < nStates; i++){
                  gamma[iSeq][t][i] = alpha[t][i] + beta[t][i] - logprob_i;
                  if (t < iLast){
                     for(int j = 0; j < nStates; j++)
                        xi[t][i][j] = alpha[t][i] + beta[t + 1][j] + tran[i][j] + bmat[j][t + 1] - logprob_i;
                  }
               }

            for(int i = 0; i < nStates; i++){
               for(int t = 0; t < T; t++){
                  gamma[iSeq][t][i] = Math.exp(gamma[iSeq][t][i]);
                  wsum[i] += gamma[iSeq][t][i]; // don't count gamma[iSeq][iLast][i] for p(exit state i)
                  if (t < iLast){
                     for(int j = 0; j < nStates; j++)
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
               for(int j = 0; j < nStates; j++)
                  gamma[i][t][j] /= wsum[j];
         }

         // update the tran matrix and obs pdf
         for(int i = 0; i < nStates; i++){
            // update the transition matrix
            double sum = 0.0;
            for(int j = 0; j < nStates; j++)
               sum += tran_acc[i][j];
            sum = Math.log(sum);
            for(int j = 0; j < nStates; j++)
               tran[i][j] = Math.log(tran_acc[i][j]) - sum;

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
      }
      return iter;
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
      int nTran[][] = new int[nStates][nStates];
      boolean bConverge = false;
      int iter = 0;

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
               if (i < T - 1) nTran[iState][path[i + 1]]++;
               for(int j = 0; j < nDims; j++)
                  gmd[iState][j].add(seq.get(i, j), false);
            }
         }

         for(int i = 0; i < nStates; i++){
            // reest tran matrix
            for(int j = 0; j < nStates; j++){
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
      }
      return iter;
   }

   @Override
   public String toString()
   {
      StringBuffer sb = new StringBuffer();

      int nStates = getNumStates();
      sb.append(String.format("|HMM %d states, %d dims\n", nStates, nDims));
      sb.append("| p(start): ");
      for(int i = 0; i < nStates; i++)
         sb.append(String.format(" %.2f", Math.exp(getPiStart(i))));
      sb.append("\n");
      for(int i = 0; i < nStates; i++){
         FeatureVec fvm = ((GaussianDiagonal)states[i]).getMean();
         FeatureVec fvv = ((GaussianDiagonal)states[i]).getVar();
         sb.append(String.format("|  State %d:", i + 1));
         for(int j = 0; j < nDims; j++)
            sb.append(String.format(" N(%.2f,%.2f)", fvm.get(j), fvv.get(j)));
         sb.append(String.format(" [%.2f", Math.exp(tran[i][0])));
         for(int j = 1; j < nStates; j++)
            sb.append(String.format(" %.2f", Math.exp(tran[i][j])));
         sb.append("]");
         sb.append("\n");
      }
      sb.append("| p(end): ");
      for(int i = 0; i < nStates; i++)
         sb.append(String.format(" %.2f", Math.exp(getPiEnd(i))));
      sb.append("\n");
      return sb.toString();
   }

   public static void main(String[] args)
   {
      // test code that compares HMM with HmmLR
      Sequence seq = new Sequence();
      seq.add(new FeatureVec(1, 0.1));
      seq.add(new FeatureVec(1, 0.0));
      seq.add(new FeatureVec(1, -0.1));
      seq.add(new FeatureVec(1, 3.5));
      seq.add(new FeatureVec(1, 3.4));
      seq.add(new FeatureVec(1, 3.6));

      HmmLR hlr = new HmmLR(2, 1);
      GaussianDiagonal dg = (GaussianDiagonal)hlr.getState(0);
      dg.setMean(new FeatureVec(1, 0));
      dg = (GaussianDiagonal)hlr.getState(1);
      dg.setMean(new FeatureVec(1, 3.5));

      HMM hmm = new HMM(2, 1);
      dg = (GaussianDiagonal)hmm.getState(0);
      dg.setMean(new FeatureVec(1, 0));
      dg = (GaussianDiagonal)hmm.getState(1);
      dg.setMean(new FeatureVec(1, 3.5));
      double[] pi = hmm.getPiStart();
      Arrays.fill(pi, Library.LOG_ZERO);
      pi[0] = Library.LOG_ONE;
      pi = hmm.getPiEnd();
      Arrays.fill(pi, Library.LOG_ZERO);
      pi[1] = Library.LOG_ONE;
      double[][] tran = hmm.getFullTransMatrix();
      tran[0][0] = Math.log(0.9);
      tran[0][1] = Math.log(0.1);
      tran[1][0] = Library.LOG_ZERO;
      tran[1][1] = Library.LOG_ONE;

      hlr.train_viterbi(seq);
      hmm.train_viterbi(seq);

      double lvLR = hlr.viterbi(seq);
      double lvFull = hmm.viterbi(seq);

      System.err.printf("left-right: %s\n", hlr);
      System.err.printf("full: %s\n", hmm);

      int[] path = hlr.getPath();
      System.err.printf("  LR: ");
      for(int i = 0; i < path.length; i++)
         System.err.printf(" %d", path[i]);
      System.err.println();
      path = hmm.getPath();
      System.err.printf("Full: ");
      for(int i = 0; i < path.length; i++)
         System.err.printf(" %d", path[i]);
      System.err.println();

      System.err.printf("lr.loglik=%f (%f)   hmm.loglik=%f (%f)\n", hlr.eval(seq), lvLR, hmm.eval(seq),
            lvFull);
   }
}
