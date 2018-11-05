package kdm.models.misc;

/**
 * Computes various metrics based on true/false, pos/neg counts. Arrays are always
 * TP,TN,FP,FN
 */
public class Eval4
{
   public static int tp(int[] res){ return res[0]; }
   public static int tn(int[] res){ return res[1]; }
   public static int fp(int[] res){ return res[2]; }
   public static int fn(int[] res){ return res[3]; }
   
   public static int[] arr(int TP, int TN, int FP, int FN)
   {
      return new int[]{ TP,TN,FP,FN };
   }
   
   public static int ncorr(int[] res)
   {
      int TP = tp(res), TN = tn(res), FP = fp(res), FN = fn(res);
      return (TP+TN);
   }
   
   public static double accuracy(int[] res)
   {
      int TP = tp(res), TN = tn(res), FP = fp(res), FN = fn(res);
      return (double)(TP + TN) / (TP + TN + FP + FN);
   }
   
   public static double recall(int[] res)
   {
      int TP = tp(res), TN = tn(res), FP = fp(res), FN = fn(res);
      return (double)TP / (TP + FP);
   }
   
   public static double precision(int[] res)
   {
      int TP = tp(res), TN = tn(res), FP = fp(res), FN = fn(res);
      return (double)TP / (TP + FN);
   }
   
   public static String str(int[] res)
   {      
      return String.format("[%d,%d,%d,%d]", res[0],res[1],res[2],res[3]);
   }
}
