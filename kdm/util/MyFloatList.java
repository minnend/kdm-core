package kdm.util;

import org.apache.commons.collections.primitives.*;

/**
 * Same as apache primitives ArrayFloatList, but with fill and setSize functions.
 */
public class MyFloatList extends ArrayFloatList
{
   /** Create an empty float list */
   public MyFloatList(){}
   
   /** Create a float list with the specified size */
   public MyFloatList(int n){ super(); setSize(n); }
   
   /** add the given value to the front of the list */
   public void push(float x)
   {
      add(0, x);
   }
   
   public void fill(float x)
   {
      int n = size(); 
      for(int i=0; i<n; i++) set(i, x);
   }
   
   public void setSize(int n){ setSize(n, 0.0f); }
   
   public void setSize(int n, float x)
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
