package kdm.mlpr;

import java.util.*;
import kdm.data.*;
import kdm.util.*;

public class SubseqTree
{
   protected SubseqNode root;

   /**
    * Build a subsequence tree from the given data and window length
    * @param dseq data from which to build tree
    * @param wlen window length
    */
   public SubseqTree(DiscreteSeq dseq, int wlen)
   {
      int nq = dseq.getNumSymbols();
      root = new SubseqNode(nq);
      
      for(int i=0; i+wlen<=dseq.length(); i++)
         root.add(dseq.extract(i, wlen));
   }

   /**
    * Build a subsequence tree from the given data and window length
    * @param dseqs data from which to build tree
    * @param wlen window length
    */
   public SubseqTree(ArrayList<DiscreteSeq> dseqs, int wlen)
   {
      int nq = dseqs.get(0).getNumSymbols();
      root = new SubseqNode(nq);
      
      for(DiscreteSeq dseq : dseqs)
         for(int i=0; i+wlen<=dseq.length(); i++)
            root.add(dseq.extract(i, wlen));
   }
   
   public int getHeight()
   {
      return root.getHeight();
   }
   
   public SubseqNode getRoot(){ return root; }
   
   public void dump()
   {
      root.dump("");
   }
   
   public void update(int iMinTrans, int iMinCount)   
   {
      root.update(iMinTrans, iMinCount);
   }
   
   public static String seq2str(int[] seq)
   {
      StringBuffer sb = new StringBuffer();
      for(int i=0; i<seq.length; i++)
         sb.append(seq[i]>=0 ? (char)(seq[i]+'a') : '-');
      return sb.toString();
   }
}
