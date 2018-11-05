package kdm.data.transform;

import kdm.data.*;
import kdm.models.*;
import kdm.util.*;

import java.util.*;

/** transform the data by applying a kernel (default: 5-tap gaussian) */
public class TransformSmooth extends DataTransform
{
   protected double k[];
   int step;

   public TransformSmooth()
   {      
      this(5);
   }
   
   public TransformSmooth(int kernelWidth)
   {
      this(Library.buildSmoothKernel(kernelWidth), 1);  
   }
   
   public TransformSmooth(double _kernel[])
   {
      this(_kernel, 1);
   }

   public TransformSmooth(double _kernel[], int _step)
   {
      assert (_kernel.length % 2) == 1;
      k = _kernel;
      step = _step;
      Library.normalize(k);
   }
   
   public double[] getKernel(){ return k; }
   
   public void dumpParams()
   {
      System.err.printf("%s:\n", getClass());
      System.err.printf(" Kernel size: %d\n", k.length);
      System.err.printf(" Step size: %d\n", step);      
   }
   
   public boolean config(ConfigHelper chelp, String sKey, String sVal)
   {
      if (Library.stricmp(sKey, "kernel"))
      {
         StringTokenizer st = new StringTokenizer(sVal, " \t");
         k = new double[st.countTokens()];
         for(int i = 0; i < k.length; i++)
         {
            k[i] = Double.parseDouble(st.nextToken());
         }
         Library.normalize(k);
      }
      else if (Library.stricmp(sKey, "step"))
      {
         step = Integer.parseInt(sVal);
      }
      else if (Library.stricmp(sKey, "ksize"))
      {
         int x = Integer.parseInt(sVal);
         k = Library.buildSmoothKernel(x);
      }
      else
      {
         System.err.printf("Error: unknown configuration key (%s)\n", sKey);
         return false;
      }
      return true;
   }
   
   public Sequence transform(Sequence raw)
   {      
      Sequence seq = new Sequence("Smooth: "+raw.getName(), raw.getFreq(), raw.getStartMS());
      int len = raw.length();
      for(int i=0; i<len; i+=step)
         seq.add(raw.applyKernel(i, k));
      seq.copyMeta(raw);
      return seq;
   }
}
