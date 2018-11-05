package kdm.mlpr.suffix_tree;

/**
 * This abstract class is to be extended by new classes that need to add their own information to the tree
 * nodes.
 * 
 * Allows implementation of algorithms that use different information associated to each node.
 */
public abstract class Info
{
   protected Info next;
   
   /** @return next object in the list of object that implement Info interface. */
   public final Info getNextInfo()
   {
      return next;
   }
   
   /** @param next object that implements Info interface. */
   public final void setNextInfo(Info next)
   {
      this.next = next;
   }
}
