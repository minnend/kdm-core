package kdm.metrics;

/** tag a class as storing lower-bound information */
public abstract class LBInfo
{
   /**
    * Determine length (if any) that is required for query sequences
    * 
    * @return required length or -1 if no requirements
    */
   public int getReqLength()
   {
      return -1;
   }
}
