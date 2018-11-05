package kdm.mlpr.suffix_tree;

/**
 * An example of the kind of information that can be attached to the nodes of the suffix tree: here, the sum
 * of the length of all the labels on the path from the root to the current node.
 * 
 * @author Marcel Turcotte
 */

public class Annotation extends Info
{

   private int pathLength;

   /**
    * Stores pathLen in this Annotation object.
    * 
    * @param pathLen pathLen to be stored in this Annotation.
    */
   Annotation(int pathLength)
   {
      this.pathLength = pathLength;
   }

   /**
    * Returns the pathLen.
    * 
    * @return the pathLen.
    */
   public int getPathLength()
   {
      return pathLength;
   }

   /**
    * A class method to decorate a tree with pathLen information at each node.
    * 
    * @param tree suffix tree to be decorated.
    */
   public static void addPathLength(SuffixTree tree)
   {
      InternalNode root = tree.getRoot();
      if (root != null) addPathLength(0, root.getFirstChild());
   }

   private static void addPathLength(int prefix, NodeInterface node)
   {

      if (node == null) return;

      int pathLength = prefix + node.getLength();

      node.addInfo(new Annotation(pathLength));

      if (node instanceof InternalNode)
         addPathLength(pathLength, ((InternalNode)node).getFirstChild());

      addPathLength(prefix, node.getRightSibling());

   }

   /**
    * Returns a String representation of this and the following elements of information.
    * 
    * @return a String representation of this and the following elements of information.
    */
   public String toString()
   {
      String out = "pathLength = " + pathLength;

      if (next != null) out = out + next.toString();

      return out;
   }

}
