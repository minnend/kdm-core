package kdm.mlpr;

import org.apache.commons.collections.primitives.*;

/**
 * Stores information about a single merge during agglomerative clustering
 */
public class AgglomInfo
{
   protected int iMerge, jMerge, count;
   protected double dist;
   protected AgglomInfo child1, child2;
   
   public AgglomInfo(int _i, int _j, double _dist, int _count)
   {
      this(_i, _j, _dist, _count, null, null);
   }
      
   /**
    * Create a new agglom info object
    * @param _iMerge one of the merged cluster, or the index of the data point if _jMerge<0
    * @param _jMerge the other merged cluster, or -1 if this is a singleton
    * @param _dist distance between i & j
    * @param _count number of members of this cluster
    * @param _child1 reference to iMerge cluster (or null if singleton)
    * @param _child2 reference to jMerge cluster (or null if singleton)
    */
   public AgglomInfo(int _iMerge, int _jMerge, double _dist, int _count, AgglomInfo _child1, AgglomInfo _child2)
   {
      iMerge = _iMerge;
      jMerge = _jMerge;
      dist = _dist;
      count = _count;
      child1 = _child1;
      child2 = _child2;
   }
   
   public int geti(){ return iMerge; }
   public int getj(){ return jMerge; }
   public int getCount(){ return count; }
   public double getDist(){ return dist; }
   public AgglomInfo getChild(int i){ return (i==1 ? child1 : child2); }
   public boolean hasKids(){ return (child1 != null); }
   public boolean hasGrandKids(int i)
   {
      AgglomInfo kid = getChild(i);
      return (kid!=null && kid.hasKids());
   }
   
   public ArrayIntList collectMembers()
   {
      ArrayIntList list = new ArrayIntList();
      collectMembers(list);
      return list;
   }
   
   protected void collectMembers(ArrayIntList list)
   {
      if (!hasKids())
      {
         assert(iMerge>=0 && jMerge<0) : String.format("iMerge=%d  jMerge=%d", iMerge, jMerge);
         list.add(iMerge);         
      }
      else{
         child1.collectMembers(list);
         child2.collectMembers(list);
      }
   }
   
   public String toString()
   {
      return String.format("[AgglomInfo: i=%d  j=%d  dist=%.4f  #%d]", iMerge,jMerge,dist,count);
   }
}
