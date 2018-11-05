package kdm.mlpr.suffix_tree;

/** Stores information necessary for computing all maximal repeats; see Gusfield97 for more info */
public class MaxRepeatInfo extends Info
{
   protected boolean bLeftDiverse;
   protected char leftChar;

   /** contruct info for a max repeat (left diverse) node */
   public MaxRepeatInfo()
   {
      this(true, (char)0);
   }

   /**
    * construct info for a non-max repeat (not left diverse) node
    * 
    * @param c common character to all child leaf nodes
    */
   public MaxRepeatInfo(char c)
   {
      this(false, c);
   }

   /**
    * Construct a MaxRepeatInfo object representing the given parameters; either the object is left diverse,
    * or leftChar holds the common character
    */
   public MaxRepeatInfo(boolean _bLeftDiverse, char _leftChar)
   {
      bLeftDiverse = _bLeftDiverse;      
      leftChar = _leftChar;
   }
   
   public final char getLeftChar(){ return leftChar; }

   public final boolean isMaxRepeat()
   {
      return bLeftDiverse;
   }
   
   public final void set(boolean _bLeftDiverse, char _leftChar)
   {
      bLeftDiverse = _bLeftDiverse;
      leftChar = _leftChar;
   }
   
   /**
    * Find the (first) MaxRepeatInfo object in the info list
    * @param info start of list
    * @return MaxRepeatInfo object or null if none exists
    */
   public static MaxRepeatInfo find(Info info)
   {
      while(info!=null)
      {
         if (info instanceof MaxRepeatInfo) return (MaxRepeatInfo)info;
         info = info.getNextInfo();
      }
      return null;
   }   
   
   /**
    * Remove any MaxRepeatInfo objects in the given node's info list
    * @param node node from which to remove the object
    */
   public static void remove(NodeInterface node)
   {
      if (node == null) return;
      Info root = node.getInfo();
      Info info = root;
      Info prev = null;
      while(info != null)
      {
         if (info instanceof MaxRepeatInfo)
         {
            if (info == root)
            {
               assert(prev == null);
               root = root.getNextInfo();
               info = root;
               continue;
            }
            else prev.setNextInfo(info.getNextInfo());
         }
         prev = info;
         info = info.getNextInfo();
      }
      node.setInfo(root);
   }
}
