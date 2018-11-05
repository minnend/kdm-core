package kdm.mlpr.suffix_tree;

public class TreePrinter
{

   private SuffixTree tree;

   public TreePrinter(SuffixTree tree)
   {
      this.tree = tree;
   }

   private void prettyPrint(NodeInterface node, String depth)
   {

      String nodeString = null;

      if (this.tree.getRoot() == node) nodeString = "root";
      else{
         String label = this.tree.getSubstring(node.getLeftIndex(), node.getLength());
         nodeString = "label=" + label;
      }

      Info info = node.getInfo();
      if (info != null) nodeString = nodeString + " " + info;

      if (node instanceof InternalNode){

         System.out.println(depth + "<node " + nodeString + ">");
         NodeInterface child = (NodeInterface) ((InternalNode) node).getFirstChild();
         prettyPrint(child, depth + "  ");
         System.out.println(depth + "</node>");

      }
      else{
         String coord = ((LeafNode) node).getCoordinates().toString();
         System.out.println(depth + "<leaf " + nodeString + " pos=" + coord + ">");
      }

      NodeInterface right = (NodeInterface) node.getRightSibling();
      if (right != null){
         prettyPrint(right, depth);
      }
   }

   public void prettyPrint()
   {
      prettyPrint((NodeInterface) tree.getRoot(), "");
   }
}
