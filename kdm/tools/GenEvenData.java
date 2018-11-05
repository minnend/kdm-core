package kdm.tools;

import kdm.util.*;

/** Geneated data for the "even problem" as described in Shalizi and Shalizi, 2004. */
public class GenEvenData
{

   public static void main(String args[])
   {
      if (args.length > 0 && args[0].equals("-?"))
      {
         System.err.printf("\nUSAGE: java ~.GenEvenData [# samples] [# sequences]\n\n");
         System.exit(1);
      }
      
      int nSamples = 100;
      if (args.length>0) nSamples = Integer.parseInt(args[0]);
      
      int nSeqs = 1;
      if (args.length>1) nSeqs = Integer.parseInt(args[1]);
      
      for(int i=0; i<nSeqs; i++)
      {
         int state = Library.random(2)+1; 
         for(int j=0; j<nSamples; j++)
         {            
            if (state==2)
            {
               System.out.print('b');
               state = 1;
            }
            else{
               if (Library.random()<0.5)
               {
                  System.out.print('a');
               }
               else{
                  System.out.print('b');
                  state = 2;
               }
            }
         }
         System.out.println();
      }
   }
   
}
