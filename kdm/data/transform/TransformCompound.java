package kdm.data.transform;

import kdm.data.*;

import java.util.*;

/**
 * A transform that is composed of several transforms performed in sequence.
 */
public class TransformCompound extends DataTransform
{
   protected ArrayList<DataTransform> trans;
   
   /**
    * Create a compound transform that performs transform 'a' and then 'b'
    * @param a first transform
    * @param b second transform
    */
   public TransformCompound(DataTransform a, DataTransform b)
   {
      trans = new ArrayList<DataTransform>();
      trans.add(a);
      trans.add(b);
   }
   
   public void dumpParams()
   {
      System.err.printf("%s: %d transformations\n", getClass(), trans.size());
   }
   
   /**
    * Add a new transform to the end of this compound transform
    * @param tran the new tranform to perform
    */
   public void add(DataTransform tran)
   {
      trans.add(tran);
   }
   
   public Sequence transform(Sequence data)
   {      
      for (DataTransform tran : trans) data = tran.transform(data);
      return data;
   }
}
