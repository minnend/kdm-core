package kdm.data.transform;

import kdm.data.*;
import jnt.FFT.*;
import java.util.*;

/** Compute the FFT of a sequence */
public class TransformFFT extends DataTransform
{
   int nw; // window size -- must be a power of 2
   int nSkip; // window increment -- how many time steps to skip between successive
               // windows
   int iSpecStart; // first spectrum component to include
   int iSpecStop; // last spectrum component to include (ie. endpoint is inclusive)
   boolean bReal; // include real component of DFT?
   boolean bImag; // include imaginary component of DFT?
   RealDoubleFFT_Radix2 fft;

   public TransformFFT(int _nw, int _nSkip, int _iSpecStart, int _iSpecStop)
   {
      this(_nw, _nSkip, _iSpecStart, _iSpecStop, true, true);
   }

   public TransformFFT(int _nw, int _nSkip, int _iSpecStart, int _iSpecStop, boolean _bReal, boolean _bImag)
   {
      nw = _nw;
      nSkip = _nSkip;
      iSpecStart = _iSpecStart;
      iSpecStop = _iSpecStop;
      bReal = _bReal;
      bImag = _bImag;
      fft = new RealDoubleFFT_Radix2(nw);

      assert bReal || bImag;
   }
   
   public void dumpParams()
   {
      System.err.printf("%s:\n", getClass());
      System.err.printf(" Window length: %d\n", nw);
      System.err.printf(" Window increment: %d\n", nSkip);
      System.err.printf(" Spectrum Start: %d\n", iSpecStart);
      System.err.printf(" Spectrum Stop: %d\n", iSpecStop);
   }

   public Sequence transform(Sequence _data)
   {
      double period = nSkip * _data.getPeriod();
      Sequence data = new Sequence("FFT: " + _data.getName(), 1.0 / period, _data.getStartMS());
      int nDims = _data.getNumDims(); // num dims of original data
      int D = (iSpecStop - iSpecStart + 1) * (bReal && bImag ? 2 : 1); // # DFT coefs
      int D2 = nDims * D; // num dimensions for all DFT coefs
      double v[] = new double[nw];
      for(int iw = 0; iw + nw <= _data.length(); iw += nSkip)
      {
         // offset in feature vectors is initially zero
         int ifv = 0;
         FeatureVec fv = new FeatureVec(D2);

         // process each dimension separately
         for(int d = 0; d < nDims; d++)
         {
            // copy this dimension into the v array and compute the DFT
            for(int i = 0; i < nw; i++)
               v[i] = _data.get(iw + i, d);
            fft.transform(v, 0, 1);

            // now get the coefficients back out of the v array
            for(int i = iSpecStart; i <= iSpecStop; i++)
            {
               if (bReal) fv.set(ifv++, v[i]);
               if (bImag) fv.set(ifv++, i == 0 ? 0.0 : v[nw - i]);
            }
         }

         // finally, add the DFT coefs FV to the new dataset
         data.add(fv);
      }
      return data;
   }

}
