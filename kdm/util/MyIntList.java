package kdm.util;

import org.apache.commons.collections.primitives.*;

/**
 * Same as apache primitives ArrayIntList, but with fill and setSize functions.
 */
public class MyIntList extends ArrayIntList
{
   /** Create an empty int list */
   public MyIntList()
   {}

   /** Create an int list with the specified size */
   public MyIntList(int n)
   {
      super();
      setSize(n);
   }

   public void fill(int x)
   {
      int n = size();
      for(int i = 0; i < n; i++)
         set(i, x);
   }

   /** add the given value to the front of the list */
   public void addFront(int x)
   {
      add(0, x);
   }

   public void push(int x)
   {
      add(x);
   }

   public int pop()
   {
      return removeElementAt(size() - 1);
   }

   public void setSize(int n)
   {
      setSize(n, 0);
   }

   public void setSize(int n, int x)
   {
      if (n < 1) clear();
      else{
         int m = size();
         while(n > m){
            add(x);
            m++;
         }
         while(n < m)
            this.removeElementAt(--m);
         trimToSize();
      }
   }
}
