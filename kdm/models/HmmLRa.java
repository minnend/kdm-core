package kdm.models;

import java.util.*;
import kdm.data.*;
import kdm.util.*;

/**
 * A left-right HMM augmented with a single additional start state; this start state
 * always accounts for just the first frame of data. This is useful for efficient word
 * spotting.
 */
public class HmmLRa extends HmmLR
{
   /**
    * Construct a default HMM with the specified number of hidden states and with
    * observations of the given dimensionality. The observation model is a multivariate
    * Gaussian with independent dimensions (e.g., diagonal covariance matrix).
    */
   public HmmLRa(int nStates, int nDims)
   {
      this(nStates, nStates, nDims);
   }

   /**
    * Construct a default HMM with the specified number of hidden states and with
    * observations of the given dimensionality. The transition matrix is setup with the
    * minimum number of skip transitions so that the model can match a sequence with
    * nMinLen frames. Also, the first state is special in that it can only account for
    * one frame of data (e.g., self-transition is 0.0).  The observation model is a
    * multivariate Gaussian with independent dimensions (e.g., diagonal covariance
    * matrix).
    */
   public HmmLRa(int nStates, int nMinLen, int nDims)
   {
      super(nStates, nDims);
      
      assert (nMinLen >= nStates + 1 || nMinLen >= 3) : String.format(
            "impossible to match such a short sequence (#states=%d  min=%d)", nStates, nMinLen);
            
      // initialize the tran matrix to a valid default
      tran[0] = new double[] { Library.LOG_ZERO, Library.LOG_ONE };
      int nLeft = nMinLen - 2;
      int iSkip = 1;
      for(int i = 1; i < nStates-1; i++)
      {
         int skip = (int)Math.ceil((double)(nStates - i-1) / nLeft) - 1;
         if (i == iSkip && skip>0)
         {
            assert (i + skip < nStates) : String.format("i=%d  skip=%d  nStates=%d  total=%d", i, skip,
                  nStates, tran.length);
            tran[i] = new double[skip + 2];
            tran[i][0] = Math.log(0.8);
            tran[i][1] = Math.log(0.1);
            for(int j = 2; j < skip + 2; j++)
               tran[i][j] = Library.LOG_ZERO;
            tran[i][skip + 1] = Math.log(0.1);
            iSkip = i+skip+1;
            nLeft--;
            //System.err.printf("  next skip: %d\n", iSkip);
         }
         else tran[i] = new double[] { Math.log(0.9), Math.log(0.1) };
      }
      tran[nStates - 1] = new double[] { Library.LOG_ONE };
   }

}
