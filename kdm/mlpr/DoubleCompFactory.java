package kdm.mlpr;

/**
 * Factory class that creates double comparators.
 */
public abstract class DoubleCompFactory
{
   public static DoubleComp createMin()
   {
      return new DoubleComp()
      {
         public double comp(double a, double b, int na, int nb)
         {
            return Math.min(a,b);
         }
         
      };
   }
   
   public static DoubleComp createMax()
   {
      return new DoubleComp()
      {
         public double comp(double a, double b, int na, int nb)
         {
            return Math.max(a,b);
         }
         
      };
   }
   
   public static DoubleComp createAvg()
   {
      return new DoubleComp()
      {
         public double comp(double a, double b, int na, int nb)
         {
            return (na*a+nb*b) / (na+nb);
         }
         
      };
   }
}
