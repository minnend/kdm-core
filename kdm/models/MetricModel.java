package kdm.models;

import java.util.*;
import kdm.data.*;
import kdm.metrics.*;
import kdm.util.*;

/** A generic model that takes a sequence metric and learns a model */
public class MetricModel extends ProbSeqModel
{
   public static enum Method {
      Centroid, NN
   }

   protected MetricSeq metseq;
   protected Sequence[] neighbors;
   protected int iLastNeighbor;
   protected int iCentroid;

   public MetricModel(MetricSeq metseq, ArrayList<Sequence> data, Method method)
   {
      this.metseq = metseq;
      iLastNeighbor = -1;
      iCentroid = -1;

      if (method == Method.NN){
         // no real training for NN, just copy the data
         neighbors = data.toArray(new Sequence[0]);
      }
      else if (method == Method.Centroid){
         // the "centroid" is defined as the example with smallest distance to all other examples
         int n = data.size();
         double[][] dm = Library.allocMatrixDouble(n, n, 0.0);

         // compute dist matrix
         for(int i = 0; i < n; i++)
            for(int j = i + 1; j < n; j++){
               double d = Library.INF;
               d = metseq.dist(data.get(i), data.get(j));
               dm[i][j] = dm[j][i] = d;
            }

         // find min dist element
         int iBest = -1;
         double vBest = Library.INF;
         for(int i = 0; i < n; i++){
            double sum = 0.0;
            for(int j = 0; j < n; j++)
               sum += dm[i][j];
            if (sum < vBest){
               vBest = sum;
               iBest = i;
            }
         }

         // TODO could align sequence and find true mean/prototype
         
         assert (iBest >= 0);
         iCentroid = iBest;
         neighbors = new Sequence[] { data.get(iBest) };
      }
   }
   
   public MetricSeq getMetSeq(){ return metseq; }

   public int getCentroidIndex()
   {
      return iCentroid;
   }
   
   public Sequence getCentroid()
   {      
      assert neighbors.length == 1;
      return neighbors[0];
   }

   public int length()
   {
      if (neighbors == null || neighbors.length == 0 || (neighbors.length > 1 && iLastNeighbor < 0)){
         assert false : "ambiguous model length at this point (call eval() first)";
         return 0;
      }
      if (neighbors.length == 1) return neighbors[0].length();
      else return neighbors[iLastNeighbor].length();
   }

   @Override
   public double eval(Sequence seq)
   {
      if (neighbors == null || neighbors.length == 0) return Double.NaN;
      double mindist = Library.INF;
      iLastNeighbor = -1;
      for(int i = 0; i < neighbors.length; i++){
         double dist = metseq.dist(neighbors[i], seq);
         if (dist < mindist){
            mindist = dist;
            iLastNeighbor = i;
         }
      }
      return mindist;
   }

   @Override
   public Sequence sample()
   {
      assert false : "can't sample from a metric model";
      return null;
   }

   @Override
   public ProbSeqModel build(ArrayList<Sequence> examples, String sConfig)
   {
      // TODO Auto-generated method stub
      assert false : "not yet implemented";
      return null;
   }

   @Override
   public ProbSeqModel build(Sequence seq, WindowLocation win, String sConfig)
   {
      // TODO Auto-generated method stub
      assert false : "not yet implemented";
      return null;
   }

}
