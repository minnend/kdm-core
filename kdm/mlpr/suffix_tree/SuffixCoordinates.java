package kdm.mlpr.suffix_tree;

/**
 * Holds the coordinates of this leaf; can have more than one set of coordinates, since this suffix can be
 * suffix to more than one string.
 */
public final class SuffixCoordinates
{
   /**
    * the position of the suffix associated with this leaf node; Note that the position is calculated
    * relatively to the first char of the first string added to the tree
    */
   private final int position;

   /**
    * the next coordinate object
    */
   private SuffixCoordinates nextCoordinates;

   /**
    * @stereotype constructor
    */
   SuffixCoordinates(int position, SuffixCoordinates nextCoordinates)
   {
      this.position = position;
      this.nextCoordinates = nextCoordinates;
   }

   /**
    * returns the position of the suffix
    */
   public final int getPosition()
   {
      return position;
   }

   /**
    * returns the next coordinate object
    */
   public final SuffixCoordinates getNext()
   {
      return nextCoordinates;
   }

   public String toString()
   {

      if (nextCoordinates == null) return Integer.toString(position);
      else return position + ", " + nextCoordinates.toString();
   }

   /** @return number of occurences (ie, size of list) */
   public int getNum()
   {
      int n = 1;
      SuffixCoordinates sc = getNext();
      while(sc != null)
      {
         n++;
         sc = sc.getNext();
      }
      return n;
   }
}
