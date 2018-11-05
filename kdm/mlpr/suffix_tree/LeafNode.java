package kdm.mlpr.suffix_tree;

import kdm.util.*;

public final class LeafNode extends NodeInterface
{
   /**
    * the coordinates of one of the suffixes ending at this leaf node
    */
   private SuffixCoordinates coordinates;

   /**
    * @stereotype constructor
    */
   LeafNode(int leftIndex, int length, NodeInterface rightSibling)
   {
      this.leftIndex = leftIndex;
      this.length = length;
      this.rightSibling = rightSibling;
      this.coordinates = null;
      this.info = null;
   }

   /**
    * @stereotype constructor
    */
   LeafNode()
   {
      this.leftIndex = 0;
      this.length = 0;
      this.rightSibling = null;
      this.coordinates = null;
      this.info = null;
   }

   /** @return first coordinate object. */
   public final SuffixCoordinates getCoordinates()
   {
      return coordinates;
   }
   
   /**
    * prepends the current coordinates (an object that contains the starting index of the current added
    * suffix) to the list of coordinates - that in case two strings have the same suffix
    */
   final void addCoordinates(int position)
   {
      SuffixCoordinates newCoord;
      if (this.coordinates != null)
         newCoord = new SuffixCoordinates(position, this.coordinates);
      else newCoord = new SuffixCoordinates(position, null);
      this.coordinates = newCoord;
   }

   /** collect all of the coordinates of this node into the given list */
   public MyIntList getCoors(MyIntList coors)
   {
      if (coors == null) coors = new MyIntList();
      SuffixCoordinates sc = getCoordinates();
      while(sc != null)
      {
         coors.add(sc.getPosition());
         sc = sc.getNext();
      }
      return coors;
   }

}
