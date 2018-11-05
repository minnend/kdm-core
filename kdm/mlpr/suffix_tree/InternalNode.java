package kdm.mlpr.suffix_tree;

import kdm.util.*;

public final class InternalNode extends NodeInterface
{
   /**
    * Link to the first child of this node. There are always at least two children to this node. The access to
    * the rest of the children is done following the right sibling(s) of the first child.
    */
   private NodeInterface firstChild;

   /**
    * An internal node always has a sufix link.
    */
   private NodeInterface suffixLink;

   /**
    * @stereotype constructor
    */
   InternalNode()
   {
      this.leftIndex = 0;
      this.length = 0;
      this.rightSibling = null;
      this.firstChild = null;
      this.suffixLink = null;
      this.info = null;
   }

   /**
    * @stereotype constructor
    */
   InternalNode(int leftIndex, int length, NodeInterface rightSibling, NodeInterface firstChild)
   {
      this.leftIndex = leftIndex;
      this.length = length;
      this.rightSibling = rightSibling;
      this.firstChild = firstChild;
      this.suffixLink = null;
      this.info = null;
   }

   public final NodeInterface getFirstChild()
   {
      return this.firstChild;
   }

   final void setFirstChild(NodeInterface firstChild)
   {
      this.firstChild = firstChild;
   }

   public final NodeInterface getSuffixLink()
   {
      return this.suffixLink;
   }

   final void setSuffixLink(NodeInterface suffixLink)
   {
      this.suffixLink = suffixLink;
   }

   public MyIntList getCoors(MyIntList coors)
   {
      if (coors == null) coors = new MyIntList();
      NodeInterface kid = getFirstChild();
      while(kid != null)
      {
         kid.getCoors(coors);
         kid = kid.getRightSibling();
      }
      return coors;
   }
}
