package kdm.data.transform;

import gnu.getopt.Getopt;
import gnu.getopt.LongOpt;

import java.io.*;
import java.util.*;
import kdm.data.*;
import kdm.io.*;
import kdm.io.DataLoader.DLRaw;
import kdm.util.*;

/**
 * Discretize a 1D sequence by finding n bins with equal probability. This can be seen as
 * a generalization of SAX where we don't assume a Guassian distribution. Instead, we
 * inspect the data directly to find equal-probability boundaries.
 */
public class TransformSAXGeneral extends DataTransform
{
   protected double bounds[];

   /**
    * Create a generalized SAX transform
    * 
    * @param nSymbols number of symbols in the discrete representation
    * @param dataAll all of the data, used here to estimate the boundary locations
    */
   public TransformSAXGeneral(int nSymbols, Sequence dataAll)
   {
      assert (dataAll.getNumDims() == 1) : "Error: Generalized SAX only works on 1D data";

      // find boundaries by sorting and then looking up equal-sized chunks
      double[] x = dataAll.toSeqArray()[0];
      int n = x.length;
      Arrays.sort(x);
      bounds = new double[nSymbols - 1];
      for(int i = 1; i < nSymbols; i++)
      {
         int j = i * n / nSymbols;
         bounds[i - 1] = x[j];         
      }
   }
   
   public void dumpParams()
   {
      System.err.printf("%s: no parameters\n", getClass());
   }

   public Sequence transform(Sequence _data)
   {
      if (_data.getNumDims() != 1)
      {
         System.err.println("Warning: tried to apply SAX to a multidimensonal sequence.");
         return null;
      }

      DiscreteSeq data = new DiscreteSeq("SAX: " + _data.getName(), _data.getFreq(),
            bounds.length + 1, _data.getStartMS());

      for(int i = 0; i < _data.length(); i++)
      {
         double x = _data.get(i, 0);
         int ix = bounds.length;
         for(int j = 0; j < bounds.length; j++)
            if (x <= bounds[j])
            {
               ix = j;
               break;
            }
         FeatureVec fv = new FeatureVec(1, ix);
         data.add(fv);
      }
      data.copyMeta(_data);
      return data;
   }

   public double[] getBounds()
   {
      return bounds;
   }

   public static void usage()
   {
      System.err
            .println("USAGE: java kdm.data.transform.TransformSAXGeneral [Options] <data files>");
      System.err.println(" Options:");
      System.err.println("  -save-bounds <file>      Save the computed boundaries");
      System.err
            .println("  -tran-ext <ext>         Transform each file <foo> to a new file <foo>.<ext>");
      System.err.println();
   }

   public static void main(String[] args)
   {
      int c;
      LongOpt[] longopts = new LongOpt[] { new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
            new LongOpt("save-bounds", LongOpt.REQUIRED_ARGUMENT, null, 1001),
            new LongOpt("tran-ext", LongOpt.REQUIRED_ARGUMENT, null, 1002),
            new LongOpt("nsymbols", LongOpt.REQUIRED_ARGUMENT, null, 1003) };

      String sTranExt = null;
      String sBounds = null;
      int nSymbols = -1;

      Getopt g = new Getopt("SupTest", args, "?", longopts, true);
      while((c = g.getopt()) != -1)
      {
         switch(c){
         case '?':
         case 'h': // help
            usage();
            System.exit(0);
            break;
         case 1001: // save-bounds
            sBounds = g.getOptarg();
            break;
         case 1002: // transform
            sTranExt = g.getOptarg();
            break;
         case 1003: // nsymbols
            nSymbols = Integer.parseInt(g.getOptarg());
            break;
         }
      }

      if (sTranExt == null && sBounds == null)
      {
         System.err
               .println("Error: Nothing to do!  Specify a boundary save file (-save-bounds) or transform extention (-tranext)");
         System.exit(1);
      }

      if (nSymbols < 1)
      {
         System.err
               .println("Error: please specify the number of symbols for quantization (-nsymbols)");
         System.exit(1);
      }

      if (g.getOptind() == args.length)
      {
         System.err.println("Error: no data files specified.");
         System.exit(1);
      }

      Sequence allData = null;
      ArrayList<Sequence> data = new ArrayList<Sequence>();
      for(int i = g.getOptind(); i < args.length; i++)
      {
         Sequence seq = new DLRaw().load(args[i]);
         if (seq == null) System.exit(1);
         if (seq.getNumDims() != 1)
         {
            System.err.println("Error: all data files must be 1D\n (" + args[i] + ")");
            System.exit(1);
         }
         data.add(seq);
         if (allData == null) allData = new Sequence(seq);
         else allData.append(seq, true, false);
      }

      System.err.printf("Sequences: %d  Total Frames: %d\n", data.size(), allData.length());

      TransformSAXGeneral gsax = new TransformSAXGeneral(nSymbols, allData);

      // save the boundary locations
      if (sBounds != null)
      {         
         try
         {
            PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(sBounds)));
            double[] b = gsax.getBounds();
            for(int i = 0; i < b.length; i++)
               out.println(b[i]);
            out.close();
         } catch (IOException e)
         {
            System.err.printf("Error: failed to open boundary file for writing\n (%s)\n", sBounds);
         }
      }

      // transform the sequences
      if (sTranExt != null)
      {
         for(int iSeq = 0; iSeq < data.size(); iSeq++)
         {
            int[] x = ((DiscreteSeq)gsax.transform(data.get(iSeq))).toArray();
            String sFile = args[g.getOptind() + iSeq] + "." + sTranExt;
            try
            {
               PrintWriter out = new PrintWriter(new FileWriter(sFile));
               for(int i = 0; i < x.length; i++)
                  out.println(x[i]);
               out.close();
            } catch (IOException ioe)
            {
               System.err.println("Error: unable to open output file for writing\n " + sFile);
            }
         }
      }
   }
}
