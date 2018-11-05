package kdm.util;

import java.util.*;

public class SparseMatrix<T>
{
   HashMap<Integer, HashMap<Integer, T>> mat = new HashMap<Integer, HashMap<Integer, T>>();

   public final T get(int i, int j)
   {
      HashMap<Integer, T> x = mat.get(i);
      if (x == null) return null;
      return x.get(j);
   }
   
   public final HashMap<Integer,T> getRow(int i){ return mat.get(i); }
   
   public final MatrixEntry getEntry(int i, int j)
   {
      if (get(i,j)==null) return null;
      return new MatrixEntry(this, i, j);
   }

   public final void set(int i, int j, T obj)
   {
      HashMap<Integer, T> x = mat.get(i);
      if (x == null)
      {
         x = new HashMap<Integer, T>();
         mat.put(i, x);
      }
      x.put(j, obj);
   }
   
   public final void remove(int i, int j)
   {
      HashMap<Integer, T> x = mat.get(i);
      if (x == null) return;
      x.remove(j);
      if (x.isEmpty()) mat.remove(i);
   }

   public final void clear()
   {
      mat.clear();
   }
   
   public final int size()
   {
      int n = 0;
      Iterator<Integer> it = mat.keySet().iterator();
      while(it.hasNext())
      {
         HashMap<Integer,T> x = mat.get(it.next()); 
         n += x.size();
      }
      return n;
   }

   public SMIterator iterator()
   {
      return new SMIterator(this);
   }

   /** iterator over the matrix entries in a SparseMatrix */
   public class SMIterator implements Iterator<MatrixEntry>
   {
      protected int i;
      protected Iterator<Integer> iti, itj;
      protected SparseMatrix sm;

      protected SMIterator(SparseMatrix _sm)
      {
         sm = _sm;
         iti = sm.mat.keySet().iterator();
      }

      public boolean hasNext()
      {
         if (itj != null && itj.hasNext()) return true;
         if (iti.hasNext()) return true;
         return false;
      }

      public MatrixEntry next()
      {
         if (itj != null && itj.hasNext()) return new MatrixEntry(sm, i, itj.next());
         if (iti.hasNext())
         {
            i = iti.next();
            HashMap<Integer, T> x = (HashMap<Integer, T>)sm.mat.get(i);
            itj = x.keySet().iterator();
            return new MatrixEntry(sm, i, itj.next());
         }
         return null;
      }

      public void remove()
      {
         assert false;
      }
   }

   /** entry in the sparse matrix (row, column) */
   public class MatrixEntry
   {
      protected int i, j;
      protected SparseMatrix sm;

      public MatrixEntry(SparseMatrix sm, int i, int j)
      {
         this.sm = sm;
         this.i = i;
         this.j = j;
      }

      public final T get()
      {
         HashMap<Integer, T> x = (HashMap<Integer, T>)sm.mat.get(i);
         if (x == null) return null;
         return x.get(j);
      }

      public final int row()
      {
         return i;
      }

      public final int column()
      {
         return j;
      }
   }
}
