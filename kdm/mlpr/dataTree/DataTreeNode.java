package kdm.mlpr.dataTree;

import java.util.*;

/** Abstract base class for nodes in a data tree */
public abstract class DataTreeNode
{
   public HashMap<String, Object> meta;

   public abstract void apply(DataTreeApply op, Object param);

   public DataTreeNode()
   {
      meta = new HashMap<String, Object>();
   }
}
