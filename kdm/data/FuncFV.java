package kdm.data;

/** Represents a function of a feature vector: y=f(x), where x and y are vectors */
public abstract class FuncFV
{
   public abstract FeatureVec compute(FeatureVec fv);

   /** X -> mean(x) */
   public final static FuncFV mean = new FuncFV() {
      public FeatureVec compute(FeatureVec fv)
      {
         return new FeatureVec(1, fv.mean());
      }
   };

   /** function that makes no change (X -> X) */
   public final static FuncFV passThru = new FuncFV() {
      public FeatureVec compute(FeatureVec fv)
      {
         return fv;
      }
   };

   /** generate a function that returns the value of the x'th dimension */
   public final static FuncFV retDimX(final int x)
   {
      return new FuncFV() {
         public FeatureVec compute(FeatureVec fv)
         {
            return new FeatureVec(1, fv.get(x));
         }
      };
   }
}
