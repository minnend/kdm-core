package kdm.mlpr.dataTree;


/** code that can be applied to every node in a data tree */
public interface DataTreeApply
{
   public void apply(DataTreeNode node, Object param);
}
