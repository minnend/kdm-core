package kdm.mlpr.suffix_tree;

import kdm.util.*;

/**
 * Interface to access the node information. Implemented by InternalNode and LeafNode.
 */
public abstract class NodeInterface
{
   /**
    * May contain additional information about this node (such as lca info).
    */
   protected Info info;
   
   /**
    * The starting index of the branch that leads to this node. Note that the index is calculated relatively
    * to the first char of the first string inserted in the tree.
    */
   protected int leftIndex;
   
   /** The length of the branch leading to this node. */
   protected int length;
   
   /** Link to the right sibling of this node (if there is one). */
   protected NodeInterface rightSibling;
   
   
   
   public final NodeInterface getRightSibling()
   {
      return rightSibling;
   }

   protected final void setRightSibling(NodeInterface rightSibling)
   {
      this.rightSibling = rightSibling;
   }
   
   protected final void setLength(int length)
   {
      this.length = length;
   }
   
   public final int getLength()
   {
      return this.length;
   }

   /**
    * @return the length on the branch leading to this node.
    */
   public final int getLeftIndex()
   {
      return this.leftIndex;
   }

   /**
    * set the left index of this node
    */
   public final void setLeftIndex(int leftIndex)
   {
      this.leftIndex = leftIndex;
   }

   /**
    * @return the first Info object that is associated with the node. One example could be the LcaInfo.
    */
   public final Info getInfo()
   {
      return this.info;
   }
   
   /**
    * @return the first Info object that is associated with the node and has the given class.
    */
   public final Info getInfo(Class cls)
   {
      Info info = this.info;
      while(info != null)
      {
         if (info.getClass().equals(cls)) return info;
         info = info.next;
      }
      return null;
   }

   /**
    * add an info object to this node
    */
   public final void addInfo(Info info)
   {
      if (this.info != null)
      {
         info.setNextInfo(this.info);
      }
      this.info = info;
   }
   
   /**
    * Set the info object in this node to the given one, ignoring any existing info objects in the current list
    */
   public final void setInfo(Info info)
   {
      this.info = info;
   }

   /**
    * Collect all position coordinates
    * 
    * @param coors all coordinates are added to this list
    */
   public abstract MyIntList getCoors(MyIntList coors);
}
