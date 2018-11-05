package kdm.io;

import java.io.*;

import kdm.models.*;
import kdm.util.*;

/**
 * Provides functions to read and write HMMs using the HTK format
 */
public class HtkHmm
{
   /**
    * Read an HMM from the given file
    * 
    * @param file file to read from
    * @return the HMM or null on error
    */
   public static AbstractHMM load(File file)
   {
      System.err.printf("Error: loading HMMs from HTK text files is not yet implemented");
      assert false : "not yet implemented";
      return null;
   }

   public static boolean save(AbstractHMM hmm, File file)
   {
      return save(hmm, file, false);
   }

   /**
    * write the given HMM to the given file
    * 
    * @param hmm HMM to write
    * @param file file to write to
    * @param bPretty PrettyPrint values for transition matrix (helpful for manual inspection)
    * @return true if successful
    */
   public static boolean save(AbstractHMM hmm, File file, boolean bPretty)
   {
      try{
         int nDims = hmm.getNumDims();
         int nStates = hmm.getNumStates();
         double[][] tran = hmm.getFullTransMatrix();

         // make sure we have obs dists that we can handle
         GaussianDiagonal dg[] = new GaussianDiagonal[nStates];
         GMM gmm[] = new GMM[nStates];
         for(int i = 0; i < nStates; i++){
            ProbFVModel pm = hmm.getState(i);
            if (pm instanceof GaussianDiagonal) dg[i] = (GaussianDiagonal)pm;
            else if (pm instanceof GMM) gmm[i] = (GMM)pm;
            else{
               System.err.printf("Error (state %d): can't write HMMs unless obs dist is a diagonal "
                     + "Gaussian or MultiIndep model.\n", i);
               return false;
            }
         }

         PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(file)));
         out.println("~o");
         out.printf("<STREAMINFO> 1 %d\n", nDims);
         out.printf("<VecSize> %d <DIAGC><nullD><user>\n", nDims);
         out.printf("~h \"%s\"\n", hmm.getName());
         out.println("<BeginHMM>");
         out.printf(" <NumStates> %d\n", nStates + 2);

         // write real states
         for(int iState = 0; iState < nStates; iState++){
            if (dg[iState] != null){
               out.printf(" <State> %d\n", iState + 2);
               out.printf("  <Mean> %d\n  ", nDims);
               for(int i = 0; i < nDims; i++)
                  out.printf(" %f", dg[iState].getMean().get(i));
               out.println();
               out.printf("  <Variance> %d\n  ", nDims);
               for(int i = 0; i < nDims; i++)
                  out.printf(" %f", dg[iState].getVar().get(i));
               out.println();
            }
            else if (gmm[iState] != null){
               int nMix = gmm[iState].getNumMix();
               out.printf(" <State> %d <NumMixes> %d\n", iState + 2, nMix);
               for(int iMix = 0; iMix < nMix; iMix++){
                  out.printf("  <Mixture> %d %.4f\n", iMix + 1, gmm[iState].getWeight(iMix));
                  out.printf("  <Mean> %d\n  ", nDims);
                  GaussianDiagonal g = (GaussianDiagonal)gmm[iState].getComp(iMix);
                  for(int i = 0; i < nDims; i++)
                     out.printf(" %f", g.getMean().get(i));
                  out.println();
                  out.printf("  <Variance> %d\n  ", nDims);
                  for(int i = 0; i < nDims; i++)
                     out.printf(" %f", g.getVar().get(i));
                  out.println();
               }
            }
            else{
               System.err.printf("Error: no observation distribution for state %d\n", iState + 1);
               return false;
            }
         }

         // write transitions matrix
         out.printf(" <TransP> %d\n", nStates + 2);

         if (bPretty){
            // dummy start state transitions according to pi (start probability)
            out.print("   0");
            for(int i = 0; i < nStates; i++)
               out.printf(" " + PrettyPrint.printDouble(Math.exp(hmm.getPiStart(i)), 6));
            
            out.println(" 0");
            
            // output the real state transitions
            for(int i = 0; i < nStates; i++){
               out.print("   0");
               double pleave = Math.exp(hmm.getPiLeave(i));
               for(int j = 0; j < nStates; j++)
                  if (tran[i][j] == Library.LOG_ZERO) out.print(" 0");
                  else out.print(" " + PrettyPrint.printDouble(Math.exp(tran[i][j]) * (1.0 - pleave), 6));
               out.println(" " + PrettyPrint.printDouble(pleave, 6));
            }
         }
         else{
            // dummy start state transitions according to pi (start probability)
            out.print("   0");
            for(int i = 0; i < nStates; i++){
               double v = hmm.getPiStart(i);
               if (Double.isInfinite(v)) out.print(" 0");
               else out.printf(" %f", Math.exp(v));
            }
            out.println(" 0");
            
            // output the real state transitions
            for(int i = 0; i < nStates; i++){
               out.print("   0");
               double pleave = Math.exp(hmm.getPiLeave(i));
               for(int j = 0; j < nStates; j++)
                  if (tran[i][j] == Library.LOG_ZERO) out.print(" 0");
                  else out.printf(" %f", Math.exp(tran[i][j]) * (1.0 - pleave));
               out.printf(" %f\n", pleave);
            }
         }
         
         // last row is all zeros
         out.print("  ");
         for(int i = 0; i < nStates + 2; i++)
            out.print(" 0");
         out.println();
         
         out.println("<EndHMM>");
         out.close();
         return true;
      } catch (IOException e){
         e.printStackTrace();
         return false;
      }
   }
}
