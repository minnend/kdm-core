package kdm.mlpr.dataTree;

import kdm.util.MutableInteger;

/** tree structure that holds feature vectors */
public class VectorTree extends DataTree
{
   protected VectorTreeNode root;

   public VectorTreeNode getRoot(){ return root; }
   
   @Override
   public void apply(DataTreeApply op, Object param)
   {
      // TODO Auto-generated method stub
      if (root != null) root.apply(op, param);
   }

   public int getNumNodes()
   {
      if (root == null) return 0;
      MutableInteger mi = new MutableInteger(0);
      root.apply(new DataTreeApply(){
         public void apply(DataTreeNode node, Object param)
         {
            ((MutableInteger)param).inc();            
         }
      }, mi);
      return mi.getValue();
   }
}
