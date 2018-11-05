package kdm.models;

import java.util.*;

import kdm.data.*;
import kdm.util.Library;

/** unconstrained discrete hidden markov model */
public class HMMD extends AbstractHMMFullTran
{
   protected int nSymbols = 0;

   public HMMD(int nStates, int nSymbols)
   {
      super(nStates, nSymbols);
      this.nSymbols = nSymbols;
      states = new Multinomial[nStates];

      for(int i = 0; i < nStates; i++){
         states[i] = new Multinomial(nSymbols);
         states[i].setReport(Multinomial.Report.loglik);
      }
   }

   public static void main(String[] args)
   {
      HMMD hmm = new HMMD(2, 2);
      double[][] tran = hmm.getFullTransMatrix();
      tran[0][0] = tran[1][1] = Math.log(0.8);
      tran[0][1] = tran[1][0] = Math.log(0.2);
      
      Multinomial m = new Multinomial(2);
      m.set(0, 0.7); 
      m.set(1, 0.3); 
      hmm.setState(0, m);
      
      m = new Multinomial(2);      
      m.set(0, 0.3); 
      m.set(1, 0.7); 
      hmm.setState(1, m);
      
      DiscreteSeq dseq = new DiscreteSeq("test", 2);
      dseq.add(new FeatureVec(1, 0));
      dseq.add(new FeatureVec(1, 0));
      dseq.add(new FeatureVec(1, 1));      
      dseq.add(new FeatureVec(1, 0));
      dseq.add(new FeatureVec(1, 0));
      dseq.add(new FeatureVec(1, 0));
      dseq.add(new FeatureVec(1, 0));
      dseq.add(new FeatureVec(1, 1));
      dseq.add(new FeatureVec(1, 1));
            
      for(int i=0; i<dseq.length(); i++)
         System.err.printf("%d ", dseq.geti(i));
      System.err.println();
      
      hmm.viterbi(dseq);
      int[] path = hmm.getPath();
      for(int i=0; i<path.length; i++)
         System.err.printf("%d ", path[i]);
      System.err.println();
   }

   @Override
   public ProbSeqModel build(ArrayList<Sequence> examples, String sConfig)
   {
      // TODO Auto-generated method stub
      assert false : "nyi";
      return null;
   }

   @Override
   public ProbSeqModel build(Sequence seq, WindowLocation win, String sConfig)
   {
      // TODO Auto-generated method stub
      assert false : "nyi";
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
      // TODO Auto-generated method stub
      assert false : "nyi";
      return 0;
   }

   @Override
   public int train_viterbi(ArrayList<? extends Sequence> vtrain)
   {
      // TODO Auto-generated method stub
      assert false : "nyi";
      return 0;
   }
}
