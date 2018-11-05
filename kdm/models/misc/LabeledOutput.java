package kdm.models.misc;

/** Stores output from a classifier along with the correct index */
public class LabeledOutput
{
   public double[] output;
   public int iCorrect;
   public double weight;

   public LabeledOutput(double[] _output, int _iCorrect)
   {
      this(_output, _iCorrect, 1);
   }
   
   public LabeledOutput(double[] _output, int _iCorrect, double _weight)
   {
      output = _output;
      iCorrect = _iCorrect;
      weight = _weight;
   }
   
   public void setWeight(double w){ weight = w; }
   
   /** @return true if the weights sum to one */
   public static boolean isNormalized(LabeledOutput[] examples)
   {
      double wsum = totalWeight(examples);
      return Math.abs(wsum-1)<0.000001;
   }
   
   /** normalize the weights of examples */
   public static void normalize(LabeledOutput[] examples)
   {
      double wsum = totalWeight(examples);
      int nex = examples.length;
      for(int i = 0; i < nex; i++) examples[i].weight /= wsum;
   }
   
   /** @return compute the total weight of the examples */
   public static double totalWeight(LabeledOutput[] examples)
   {
      int nex = examples.length;
      double w = 0;
      for(int i = 0; i < nex; i++) w += examples[i].weight;
      return w;      
   }
   
   /** @return weight associated with the correctly labeled examples */ 
   public static double score(LabeledOutput[] examples, int[] labels)
   {
      assert (examples.length == labels.length);

      int nex = labels.length;
      double score = 0;
      for(int i = 0; i < nex; i++)
         if (labels[i] == examples[i].iCorrect) score += examples[i].weight;
      return score;
   }
}
