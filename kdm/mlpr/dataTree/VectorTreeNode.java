package kdm.mlpr.dataTree;

import java.util.*;
import kdm.data.*;

/** node in a vector tree */
public class VectorTreeNode extends DataTreeNode
{
   protected VectorTreeNode[] kids;
   protected ArrayList<FeatureVec> data;
   
   public VectorTreeNode(ArrayList<FeatureVec> data)
   {
      this.data = data;     
   }
   
   public ArrayList<FeatureVec> getData(){ return data; }
   public int getNumData(){ return (data==null ? 0 : data.size()); }
   public int getNumKids(){ return (kids==null ? 0 : kids.length); }
   public boolean hasKids(){ return (kids!=null && kids.length>0); }
   public boolean isLeaf(){ return (kids==null || kids.length==0); }
   public VectorTreeNode getKid(int i){ return kids[i]; }
   
   public VectorTreeNode getOtherKid(VectorTreeNode v){
      if (v==kids[0]) return kids[1];
      else return kids[0];
   }
   
   @Override
   public void apply(DataTreeApply op, Object param)
   {
      op.apply(this, param);
      if (kids == null) return;
      for(int i=0; i<kids.length; i++)
         if (kids[i]!=null) kids[i].apply(op, param);
   }

}
