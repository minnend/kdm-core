package kdm.util;

import org.apache.commons.collections.primitives.*;

/**
 * Same as apache primitives ArrayDoubleList, but with fill and setSize functions.
 */
public class MyDoubleList extends ArrayDoubleList
{
   /** Create an empty float list */
   public MyDoubleList(){}
   
   /** Create a float list with the specified size */
   public MyDoubleList(int n){ super(); setSize(n); }
   
   /** add the given value to the front of the list */
   public void push(double x)
   {
      add(0, x);
   }
   
   public double last(){ return get(size()-1); }
   
   public void fill(double x)
   {
      int n = size(); 
      for(int i=0; i<n; i++) set(i, x);
   }
   
   public void removeLast()
   {
      int T = size();
      if (T > 0) this.removeElementAt(T-1);
   }
   
   public void setSize(int n){ setSize(n, 0.0f); }
   
   public void setSize(int n, double x)
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
