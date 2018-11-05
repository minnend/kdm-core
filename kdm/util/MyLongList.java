package kdm.util;

import org.apache.commons.collections.primitives.*;

/**
 * Same as apache primitives ArrayLongList, but with fill and setSize functions.
 */
public class MyLongList extends ArrayLongList
{
   /** Create an empty int list */
   public MyLongList(){}
   
   /** Create an int list with the specified size */
   public MyLongList(int n){ super(); setSize(n); }
   
   public void fill(long x)
   {
      int n = size(); 
      for(int i=0; i<n; i++) set(i, x);
   }
   
   /** add the given value to the front of the list */
   public void push(long x)
   {
      add(0, x);
   }
   
   public void setSize(int n){ setSize(n, 0); }
   
   public void setSize(int n, int x)
   {
      if (n<1) clear();
      else{
         int m = size();
         while(n>m){ add(x); m++; }
         while(n < m) this.removeElementAt(--m);
         trimToSize();
      }
   }
}
