package kdm.mlpr.dataTree;

import java.util.*;
import kdm.data.*;
import kdm.metrics.*;
import kdm.util.Library;

/** tree of sequences where each child node contains a subset of (similar) sequences */
public class SeqTree extends DataTree
{
   public static final String KEY_WIDTH = "SeqTree.Width";

   public static enum SplitOrder {
      MaxMembers, Widest
   }

   public static enum Stop {
      NLeaves, MaxMembers
   }

   protected SeqTreeNode root;

   public SeqTreeNode getRoot()
   {
      return root;
   }

   public boolean build(ArrayList<Sequence> data, SplitOrder split, final MetricSeq metseq, Stop stop,
         int stopParam)
   {
      Comparator comp = null;

      if (split == SplitOrder.MaxMembers) comp = new Comparator<SeqTreeNode>() {
         public int compare(SeqTreeNode stn1, SeqTreeNode stn2)
         {
            // sort so that largest number of members comes first (is smallest element)
            int n1 = stn1.getNumMembers();
            int n2 = stn2.getNumMembers();
            if (n1 > n2) return -1;
            if (n1 < n2) return 1;
            return 0;
         }
      };
      else if (split == SplitOrder.Widest){
         comp = new Comparator<SeqTreeNode>() {
            public int compare(SeqTreeNode stn1, SeqTreeNode stn2)
            {
               // sort so that max dist between members comes first (is smallest element)
               double width1, width2;

               if (stn1.meta.containsKey(KEY_WIDTH)) width1 = (Double)stn1.meta.get(KEY_WIDTH);
               else{
                  width1 = stn1.calcWidth(metseq);
                  stn1.meta.put(KEY_WIDTH, width1);
               }

               if (stn2.meta.containsKey(KEY_WIDTH)) width2 = (Double)stn2.meta.get(KEY_WIDTH);
               else{
                  width2 = stn2.calcWidth(metseq);
                  stn2.meta.put(KEY_WIDTH, width2);
               }

               if (width1 > width2) return -1;
               if (width1 < width2) return 1;
               return 0;
            }
         };
      }

      PriorityQueue<SeqTreeNode> que = new PriorityQueue<SeqTreeNode>(8, comp);
      root = new SeqTreeNode(data);
      que.add(root);

      int nNodes = 1;
      while(!que.isEmpty()){
         SeqTreeNode node = que.poll();
         int nMembers = node.getNumMembers();
         if (!node.split(metseq)) continue;
         /*
          * System.err.printf("Splitting Node: %d members, width=%.1f (%d,%.1f | %d,%.1f)\n",
          * node.getNumMembers(), node.calcWidth(metseq), node.getLeftKid().getNumMembers(),
          * node.getLeftKid().calcWidth(metseq), node.getRightKid().getNumMembers(),
          * node.getRightKid().calcWidth(metseq));
          */
         nNodes += 2;
         if (stop == Stop.NLeaves){
            if (nNodes >= stopParam) break;
            que.add(node.getLeftKid());
            que.add(node.getRightKid());
         }
         else if (stop == Stop.MaxMembers){
            SeqTreeNode kid = node.getLeftKid();
            if (kid.getNumMembers() > stopParam) que.add(kid);
            kid = node.getRightKid();
            if (kid.getNumMembers() > stopParam) que.add(kid);
         }
      }
      return true;
   }

   public int getNumNodes()
   {
      return (root == null ? 0 : 1 + root.getNumKids());
   }

   @Override
   public void apply(DataTreeApply op, Object param)
   {
      if (root != null) root.apply(op, param);
   }
}
