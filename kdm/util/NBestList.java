package kdm.util;

import java.util.*;

/**
 * container class that keeps the N "best" (i.e., largest) elements offered to it; elements are stored in
 * descending order
 */
public class NBestList<T>
{
   protected int nBest, nReal;
   protected Object[] list;
   protected Comparator comp;
   protected boolean bBigIsBetter = true;
   protected boolean bAvoidConflict = false;

   public NBestList(int nBest)
   {
      this(nBest, null);
   }

   public NBestList(int nBest, Comparator comp)
   {
      this.comp = comp;
      this.nBest = nBest;
      list = new Object[nBest];
      nReal = 0;
   }

   /** true => bigger is better, false => smaller is better */
   public void setBiggerIsBetter(boolean b)
   {
      bBigIsBetter = b;
   }
   
   /** true => ensure elements aren't in conclict with each other (defined by Conflicter interface) */
   public void setAvoidConflict(boolean b)
   {   
      if (bAvoidConflict == b) return;
      bAvoidConflict = b;
      if (bAvoidConflict){
         // ensure no conflict by removing worse element of any conflicting pair
         for(int i=0; i<nReal; i++){
            Conflicter x = (Conflicter)list[i];
            for(int j=i+1; j<nReal; j++){
               Conflicter y = (Conflicter)list[j];
               if (x.hasConflict(y)){
                  // remove element j
                  for(int k=j+1; k<nReal; k++)
                     list[k-1] = list[k];
                  j--;
                  nReal--;
               }
            }
         }
      }
   }
   
   /** @return current real size of the list */
   public int size()
   {
      return nReal;
   }

   /** @return max size of the list (N for a N-best list) */
   public int getN()
   {
      return nBest;
   }

   /** clear all elements, effectively reseting the data structure */
   public void clear()
   {
      nReal = 0;
   }

   /** @return largest element in N-best list */
   public T largest()
   {
      return (T)(bBigIsBetter ? list[0] : list[nReal-1]);
   }

   /** @return smallest element in list */
   public T smallest()
   {
      return (T)(bBigIsBetter ? list[nReal-1] : list[0]);      
   }
   
   /** @return best element */
   public T best()
   {
      return (T)list[0];
   }
   
   /** @return worst element */
   public T worst()
   {
      return (T)list[nReal-1];
   }

   public boolean add(T x)
   {
      // handle the empty list case
      if (nReal == 0){
         list[0] = x;
         nReal = 1;
         return true;
      }
      
      // make sure there aren't any conflicts (if requested)
      if (bAvoidConflict){
         for(int i=0; i<nReal; i++){
            if (((Conflicter)x).hasConflict(list[i])){
               int v = (comp != null) ? comp.compare(x, list[i]) : ((Comparable)x).compareTo(list[i]);
               if (v < 0){
                  // new item is better, so remove existing
                  for(int j=i+1; j<nReal; j++)
                     list[j-1] = list[j];
                  i--;
                  nReal--;
               }
               else{
                  // existing item is better, so we won't include new one
                  return false;
               }
            }
         }
      }

      // now handle the more general case
      int i;
      for(i = 0; i < nReal; i++){
         int v = (comp != null) ? comp.compare(x, list[i]) : ((Comparable)x).compareTo(list[i]);
         if ((bBigIsBetter && v > 0) || (!bBigIsBetter && v < 0)){
            for(int j = (int)Math.min(nReal, nBest - 1); j > i; j--)
               list[j] = list[j - 1];
            list[i] = x;
            if (nReal < nBest) nReal++;
            return true;
         }
      }

      // we can also add to the end of the real list if there's space
      if (nReal < nBest){
         list[i] = x;
         nReal++;
         return true;
      }

      return false;
   }

   public T get(int i)
   {
      return (T)list[i];
   }

}
