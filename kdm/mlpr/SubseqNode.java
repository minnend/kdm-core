package kdm.mlpr;

import java.util.*;

public class SubseqNode
{
   public static final int STATE_VISIBLE = 0;
   public static final int STATE_HIDDEN = 1;

   protected SubseqNode[] kids;
   protected SubseqNode parent;
   protected int symbol;

   /** number of sequences given current settings */
   protected int nSeqs;

   /** number of sequences assuming all children visible */
   protected int nRealSeqs;

   protected int state;

   public SubseqNode(int nq)
   {
      this(null, -1, nq);
   }

   public SubseqNode(SubseqNode _parent, int _symbol, int nq)
   {
      symbol = _symbol;
      kids = new SubseqNode[nq];
      parent = _parent;
      nSeqs = 0;
      nRealSeqs = 0;
      state = STATE_VISIBLE;
   }

   public int getSymbol()
   {
      return symbol;
   }

   public SubseqNode get(int i)
   {
      return kids[i];
   }

   public boolean isRoot()
   {
      return (parent == null);
   }

   public boolean isVisible()
   {
      return (state == STATE_VISIBLE);
   }

   public boolean isLeaf()
   {
      for(int i = 0; i < kids.length; i++)
         if (kids[i] != null) return false;
      return true;
   }

   public SubseqNode getParent()
   {
      return parent;
   }

   public void add(int[] sub)
   {
      add(0, sub);
   }

   protected void add(int ix, int[] sub)
   {
      int n = sub.length;
      nSeqs = ++nRealSeqs;
      if (ix < n)
      {
         if (kids[sub[ix]] == null) kids[sub[ix]] = new SubseqNode(this, sub[ix], kids.length);
         kids[sub[ix]].add(ix + 1, sub);
      }

   }

   public int getNumSeqs()
   {
      return nSeqs;
   }

   /**
    * @return height of this node -- i.e., number of children beneath it
    */
   public int getHeight()
   {
      int n = 0;
      for(int i = 0; i < kids.length; i++)
      {
         if (kids[i] == null) continue;
         int x = kids[i].getHeight() + 1;
         if (x > n) n = x;
      }
      return n;
   }

   /**
    * @return depth of this node -- i.e., number of parents above it
    */
   public int getDepth()
   {
      int n = 0;
      for(SubseqNode node = this; !node.isRoot(); node = node.getParent()) n++;
      return n;
   }

   public int getNumKids()
   {
      return kids.length;
   }

   public SubseqNode getKid(int i)
   {
      return kids[i];
   }

   public void dump(String sTab)
   {
      System.err.printf("%s(%c %d)\n", sTab, symbol >= 0 ? 'a' + symbol : '.', nRealSeqs);
      for(int i = 0; i < kids.length; i++)
         if (kids[i] != null) kids[i].dump(sTab + "  ");
   }

   public void setVisible(boolean b)
   {
      if (b) state = STATE_VISIBLE;
      else state = STATE_HIDDEN;
   }

   public int update(int iMinTrans, int iMinCount)
   {
      return update(0, iMinTrans, iMinCount);
   }

   protected int update(int iTransSoFar, int iMinTrans, int iMinCount)
   {
      if (isLeaf())
      {
         if (!isRoot() 
               && ((nRealSeqs < iMinCount) 
               || (iTransSoFar + (symbol != parent.getSymbol() ? 1 : 0) < iMinTrans)))                     
         {
            state = STATE_HIDDEN;
            nSeqs = 0;
         }
         else
         {
            state = STATE_VISIBLE;
            nSeqs = nRealSeqs;
         }
      }
      else
      { // internal node
         nSeqs = 0;
         int iTran = (!isRoot() && !parent.isRoot() && symbol!=parent.getSymbol() ? 1 : 0);
         for(int i = 0; i < kids.length; i++)
            if (kids[i] != null)
               nSeqs += kids[i].update(iTransSoFar+iTran, iMinTrans, iMinCount);
      }
      return nSeqs;
   }
   
   /*
     if (isLeaf())
      {
         if (!isRoot()
               && ((!bShowSingle && nRealSeqs == 1) || (!bShowConstant && bIsConstant && symbol == parent
                     .getSymbol())))
         {
            state = STATE_HIDDEN;
            nSeqs = 0;
         }
         else
         {
            state = STATE_VISIBLE;
            nSeqs = nRealSeqs;
         }
      }
      else
      { // internal node
         nSeqs = 0;
         boolean bStillConstant = (bIsConstant && (isRoot() || parent.isRoot() || symbol == parent
               .getSymbol()));
         for(int i = 0; i < kids.length; i++)
            if (kids[i] != null)
               nSeqs += kids[i].update(bShowConstant, bShowSingle, bStillConstant);
      }
      return nSeqs;
    */

   /**
    * Recompute nSeqs for all children node based on current values
    * 
    * @return nSeqs for this node
    */
   public int updateCount()
   {
      if (isLeaf()) return nSeqs;
      nSeqs = 0;
      for(int i = 0; i < kids.length; i++)
         if (kids[i] != null) nSeqs += kids[i].updateCount();
      return nSeqs;
   }

   public int[] getSeq()
   {
      int d = getDepth();
      int h = getHeight();
      int n = d+h;
      int[] s = new int[n];
      Arrays.fill(s, -1);
      for(SubseqNode node = this; !node.isRoot(); node = node.getParent())
         s[--d] = node.getSymbol();
      return s;
   }
}
