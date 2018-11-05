package kdm.mlpr.suffix_tree;

/** stores the number of occurrences in the original data of the corresponding node */
public class CountInfo extends Info
{
   protected int count;
   
   public CountInfo(int n)
   {
      count = n;
   }
   
   public int getCount(){ return count; }
   
   /** add count info to each node */
   public static void annotate(SuffixTree st)
   {
      annotate(st, st.getRoot());
   }
   
   protected static int annotate(SuffixTree st, NodeInterface node)
   {
      if (node == null) return 0;
      
      if (node instanceof InternalNode)
      {
         InternalNode inode = (InternalNode)node;
         NodeInterface kid = inode.getFirstChild();
         int n = 0;
         while(kid != null)
         {
            n += annotate(st, kid);
            kid = kid.getRightSibling();
         }
         inode.addInfo(new CountInfo(n));
         return n;
      }
      else{
         LeafNode lnode = (LeafNode)node;
         CountInfo ci = new CountInfo(lnode.getCoordinates().getNum());
         lnode.addInfo(ci);
         return ci.getCount();
      }
   }
}
