package kdm.util;

import java.util.*;

/**
 * A 2D list of objects of type T, implemented as an ArrayList of ArrayLists.
 */
public class AList2<T>
{
   protected ArrayList<ArrayList<T>> a;

   public AList2()
   {
      a = new ArrayList<ArrayList<T>>();
   }

   public void add(ArrayList<T> list)
   {
      a.add(list);
   }

   public void add(int i, T t)
   {
      int n = a.size();
      if (i >= n){ // create the row if it doesn't already exist
         int m = i-n;
         for(int j=0; j<=m; j++)
            a.add(new ArrayList<T>());
         assert(a.size() == i+1);
      }
      a.get(i).add(t);
   }

   public void add(T t)
   {
      a.get(a.size() - 1).add(t);
   }

   public ArrayList<T> get(int i)
   {
      return a.get(i);
   }

   public T get(int i, int j)
   {
      return a.get(i).get(j);
   }

   public int size()
   {
      return a.size();
   }

   public int size(int i)
   {
      if (i >= a.size()) return 0;
      return a.get(i).size();
   }

   public int totalSize()
   {
      int n = a.size(), z = 0;
      for(int i = 0; i < n; i++)
         z += a.get(i).size();
      return z;
   }
}
