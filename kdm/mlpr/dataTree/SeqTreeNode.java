package kdm.mlpr.dataTree;

import java.util.*;
import kdm.data.*;
import kdm.metrics.*;
import kdm.util.*;

import static kdm.mlpr.dataTree.SeqTree.*;

/** node in a sequence tree */
public class SeqTreeNode extends DataTreeNode
{
   protected ArrayList<Sequence> data;
   protected SeqTreeNode kid1, kid2;

   public SeqTreeNode(ArrayList<Sequence> data)
   {
      this.data = data;
   }

   public ArrayList<Sequence> getData(){ return data; }
   
   public int getNumMembers()
   {
      return data.size();
   }

   public SeqTreeNode getLeftKid()
   {
      return kid1;
   }

   public SeqTreeNode getRightKid()
   {
      return kid2;
   }

   public Sequence getMember(int ix)
   {
      return data.get(ix);
   }

   /** @return number of nodes below this node */
   public int getNumKids()
   {
      int n = 0;
      if (kid1 != null) n += 1 + kid1.getNumKids();
      if (kid2 != null) n += 1 + kid2.getNumKids();
      return n;
   }

   public boolean isLeaf()
   {
      return (kid1 == null && kid2 == null);
   }

   public boolean isInternal()
   {
      return (kid1 != null || kid2 != null);
   }

   /** @return approximate width of this node = dist between two farthest points (est) */
   public double calcWidth(MetricSeq metseq)
   {
      int N = data.size();
      if (N<1) return 0;
      if (N<5){
         double wMax = 0;
         for(int i=0; i<N; i++)
            for(int j=i+1; j<N; j++){
               double w = metseq.dist(data.get(i), data.get(j));
               if (w > wMax) wMax = w;
            }
         return wMax;
      }
      
      // choose two distant points
      double wMax = 0;
      for(int i = 0; i < 3; i++){
         int ii = Library.random(N);
         int ij = findFarthest(ii, metseq);
         ii = findFarthest(ij, metseq);
         double w = metseq.dist(data.get(ii), data.get(ij));
         if (w > wMax) wMax = w;
      }
      return wMax;
   }

   /** @return sum of distances from the given sequence to all members of this node */
   public double getDistSum(Sequence seqBase, MetricSeq metseq)
   {
      int N = data.size();
      double v = 0;
      for(int i = 0; i < N; i++)
         v += metseq.dist(data.get(i), seqBase);
      return v;
   }

   /** @return index of most distant member from the bases */
   public int findFarthest(MyIntList bases, MetricSeq metseq)
   {
      int iFar = -1;
      double vFar = 0;
      int M = bases.size();
      int N = data.size();
      for(int i = 0; i < N; i++){
         if (bases.contains(i)) continue;
         double v = 0;
         for(int j = 0; j < M; j++)
            v += metseq.dist(data.get(i), data.get(bases.get(j)));
         if (v > vFar){
            vFar = v;
            iFar = i;
         }
      }
      return iFar;
   }

   /** @return index of nearest member from the bases */
   public int findClosest(MyIntList bases, MetricSeq metseq)
   {
      int iFar = -1;
      double vFar = Library.INF;
      int M = bases.size();
      int N = data.size();
      for(int i = 0; i < N; i++){
         if (bases.contains(i)) continue;
         double v = 0;
         for(int j = 0; j < M; j++)
            v += metseq.dist(data.get(i), data.get(bases.get(j)));
         if (v < vFar){
            vFar = v;
            iFar = i;
         }
      }
      return iFar;
   }

   /** @return index of most distant member from the base */
   public int findFarthest(int iBase, MetricSeq metseq)
   {
      int iFar = -1;
      double vFar = 0;
      int N = data.size();
      Sequence seqBase = data.get(iBase);
      for(int i = 0; i < N; i++){
         if (i == iBase) continue;
         double v = metseq.dist(data.get(i), seqBase);
         if (v > vFar){
            vFar = v;
            iFar = i;
         }
      }
      return iFar;
   }

   /** @return true if split successful, otherwise false */
   public boolean split(MetricSeq metseq)
   {
      int N = data.size();
      if (N < 2) return false;

      // choose two distant points
      int ii = Library.random(N);
      int ij = findFarthest(ii, metseq);
      ii = findFarthest(ij, metseq);

      // split points according to which attractor is closer
      ArrayList<Sequence> data1 = new ArrayList<Sequence>();
      ArrayList<Sequence> data2 = new ArrayList<Sequence>();
      Sequence seq1 = data.get(ii);
      Sequence seq2 = data.get(ij);
      for(int i = 0; i < N; i++){
         if (i == ii) data1.add(data.get(i));
         else if (i == ij) data2.add(data.get(i));
         else{
            Sequence seq = data.get(i);
            double v1 = metseq.dist(seq, seq1);
            double v2 = metseq.dist(seq, seq2);
                                    
            double ratio = Math.max(v1,v2)/Math.min(v1,v2);
            //System.err.printf("v1=%f  v2=%f  ratio=%f\n", v1, v2, ratio);
            if (ratio < 1.05){ // TODO !! debug
               data1.add(seq);
               data2.add(seq);
            }
            else{
               if (v1 < v2) data1.add(seq);
               else data2.add(seq);
            }
         }
      }

      // TODO algo that leads to more even splits?
      // TODO maybe sample a few points and learn distance weighting

      if (data1.size()+10 >= data.size() || data2.size()+10 >= data.size()) return false;
      
      System.err.printf("%d -> %d, %d\n", data.size(), data1.size(), data2.size());
      
      kid1 = new SeqTreeNode(data1);
      kid1.meta.put("attractor", seq1);
      kid2 = new SeqTreeNode(data2);
      kid2.meta.put("attractor", seq2);
      //data = null; // TODO don't need data in internal nodes
   
      return true;
   }

   @Override
   public void apply(DataTreeApply op, Object param)
   {
      op.apply(this, param);
      if (kid1 != null) kid1.apply(op, param);
      if (kid2 != null) kid2.apply(op, param);
   }
}
