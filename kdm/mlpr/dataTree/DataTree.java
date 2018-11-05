package kdm.mlpr.dataTree;


/** abstract base class for all data storage trees (SeqTree, VectorTree, etc.) */
public abstract class DataTree
{
   /** execute the given code for each leaf in the tree */
   public abstract void apply(DataTreeApply op, Object param);
}
