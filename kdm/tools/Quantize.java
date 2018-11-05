package kdm.tools;

import kdm.data.*;
import kdm.data.transform.*;
import kdm.io.*;
import kdm.io.DataLoader.*;
import kdm.io.DataSaver.DSRaw;
import kdm.io.Def.DataDefLoader;
import kdm.mlpr.*;
import kdm.util.*;

import java.util.*;

import gnu.getopt.*;
import java.io.*;
import org.apache.commons.math.stat.*;

/**
 * Transforms an input data file into a discretized (quantized) output file
 */
public class Quantize
{
   public static enum Method {
      EM, SAX
   }

   protected static PrintWriter out = null;
   protected static boolean bVerbose = false;
   protected static boolean bWhiten = false;
   protected static double sdev[];
   protected static ArrayList<Sequence> tseries;
   protected static Sequence input;
   protected static DiagGaussKMeans clust;
   protected static DataLoader gloader = new DLRaw();
   protected static Method method;

   /**
    * Usage info for this tool
    */
   public static void usage()
   {
      System.err.println("USAGE: java kdm.tools.Quantize [options] <input files>");
      System.err.println();
      System.err.println(" Options:");
      System.err.println("  -v                     Output verbose information");
      System.err.println("  -method <k>            Quant method (EM (def) or SAX)");
      System.err.println("  -quant <N>             Number of discrete symbols (for EM)");
      System.err.println("  -wlen <N>              Window Length (for SAX)");
      System.err.println("  -paa <N>               Number of PAA segments (for SAX, def=wlen/3)");
      System.err.println("  -sax <N>               Nymber of SAX divisions (for SAX, def=3)");
      System.err.println("  -scan                  Output info over a range of quant values");
      System.err.println("  -white                 Whiten data before clustering");
      System.err.println("  -out <file>            Send output to this file");
      System.err.println("                          \"auto\" => foo123.ext -> foo123q.txt");
      System.err.println("  -centers-out <file>    Write clusters to this file");
      System.err.println("  -centers-in <file>     Reads clusters instead of computing from data");
      System.err.println("  -loader <load class>   Specify which class to use to load data (def = DLRaw)");
      System.err.println();
   }

   /**
    * Transform the given data by fitting a mixture of Gaussians via EM
    * 
    * @param K number of discrete symbols (ignored if sCentersIn is specified)
    * @param sCentersIn input file for cluster center data
    * @param sCentersOut output file for cluster center data
    * @return true if successful
    */
   public static boolean processEM(int K, String sCentersIn, String sCentersOut)
   {
      // load or compute the k-means
      if (sCentersIn != null){
         double x[][] = Library.read(sCentersIn, Library.MatrixOrder.RowMajor);
         if (x == null){
            System.err.println("Error: unable to open cluster input file\n " + sCentersIn);
            return false;
         }
         FeatureVec[] centers = new FeatureVec[x.length];
         for(int i = 0; i < x.length; i++)
            centers[i] = new FeatureVec(x[i]);
         clust.setCenters(centers);
         K = centers.length;
         if (bVerbose)
            System.err.printf("Loaded Cluster Centers\n %d x %d -> %d symbols\n", input.length(), input
                  .getNumDims(), K);
      }
      else{
         if (bVerbose)
            System.err.printf("Quantizing Data via MoG/EM:\n %d x %d -> %d symbols\n", input.length(), input
                  .getNumDims(), K);
         clust.cluster(K, input);
         if (bVerbose){
            int nc[] = clust.getMembershipCount(input);
            for(int i = 0; i < K; i++)
               System.err.printf("Cluster %d: %d members\n", i + 1, nc[i]);
         }
      }

      // write the cluster centers if requested
      if (sCentersOut != null){
         try{
            PrintWriter outc = new PrintWriter(new FileWriter(sCentersOut));
            FeatureVec centers[] = clust.getCenters();
            if (bWhiten) // we need to un-whiten the cluster centers
            {
               FeatureVec fv = new FeatureVec(sdev);
               for(int i = 0; i < centers.length; i++)
                  centers[i]._mul(fv);
            }
            for(int i = 0; i < centers.length; i++){
               for(int j = 0; j < centers[i].getNumDims(); j++)
                  outc.printf("%f ", centers[i].get(j));
               outc.println();
            }
            outc.close();
         } catch (IOException ioe){
            System.err.println("Error: unable to open cluster output file for writing\n " + sCentersOut);
            return false;
         }
      }

      // write the transformed sequence
      if (out != null){
         // transform the data sequence
         TransformCluster quant = new TransformCluster(clust);
         Sequence data = quant.transform(input);
         if (bVerbose) System.err.printf("Average error: %.6f\n", clust.getAvgError(input));

         for(int i = 0; i < data.length(); i++)
            out.printf("%d\n", (int)data.get(i, 0));
      }

      return true;
   }

   /**
    * Transform the given data using the hybrid SAX method of Tanaka et al.
    * 
    * @param wlen length of window to use
    * @param nPaa number of PAA segments
    * @param nSax number of SAX symbols
    * @return true if successful
    */
   public static boolean processSAX(int wlen, int nPaa, int nSax)
   {
      TimerMS timer = new TimerMS();
      if (bVerbose)
         System.err.printf("Quantizing Data via SAX:\n %d x %d, %d seqs, wlen=%d, nPaa=%d, nSax=%d\n", input
               .length(), input.getNumDims(), tseries.size(), wlen, nPaa, nSax);

      int nSeqs = tseries.size();
      int nDims = tseries.get(0).getNumDims();

      // make sure we have 1D data
      ArrayList<Sequence> data1;
      if (nDims == 1) data1 = tseries; // no need for PCA
      else{
         // need to convert from multidim to 1D via PCA
         System.err.printf("Using PCA to convert from %dD to 1D... ", nDims);
         timer.reset();

         // concatenate all data
         Sequence seqAll = new Sequence();
         for(Sequence seq : tseries)
            seqAll.append(seq, false, false);
         TransformPCA pca = new TransformPCA(1);
         Sequence seqPCA = pca.transform(seqAll);
         data1 = new ArrayList<Sequence>(nSeqs);
         int ix = 0;
         for(Sequence inseq : tseries){
            int len = inseq.length();
            Sequence seq1 = seqPCA.subseq(ix, ix + len, inseq.getParentIndex());
            data1.add(seq1);
            ix += len;
         }
         System.err.printf("done (%dms).\n", timer.time());
      }

      // determine the SAX string for each position
      timer.reset();
      int iBS = 0;
      HashMap<String, Integer> hash = new HashMap<String, Integer>();
      String[][] saxData = new String[nSeqs][];
      TransformSAX sax = new TransformSAX(nPaa, nSax, 0, wlen);
      for(int iSeq = 0; iSeq < nSeqs; iSeq++){
         Sequence seq = data1.get(iSeq);
         int len = seq.length();
         int nwin = Library.getNumSlidingWindowSites(len, wlen, 1);
         saxData[iSeq] = new String[nwin];

         for(int t = 0; t < nwin; t++){
            sax.setStart(t);
            DiscreteSeq dseq = (DiscreteSeq)sax.transform(seq);
            StringBuffer sb = new StringBuffer();
            for(int i = 0; i < nPaa; i++)
               sb.append((char)(dseq.geti(i) + 'a'));
            String s = sb.toString();
            saxData[iSeq][t] = s;
            if (!hash.containsKey(s)) hash.put(s, iBS++);
         }
      }

      // now we can use the hash table to assign a unique symbol to each string
      int[][] bsData = new int[nSeqs][];
      for(int iSeq = 0; iSeq < nSeqs; iSeq++){
         int len = saxData[iSeq].length;
         bsData[iSeq] = new int[len];
         for(int i = 0; i < len; i++)
            bsData[iSeq][i] = hash.get(saxData[iSeq][i]);
      }
      int nSymbols = hash.size();
      System.err.printf("Found %d unique strings (wlen=%d,#paa=%d,#sax=%d,%dms)\n", nSymbols, wlen, nPaa,
            nSax, timer.time());

      // output results
      for(int i = 0; i < bsData.length; i++){
         Sequence seqOrig = tseries.get(i);
         String sOut = convFileAuto(seqOrig.getOrigFile());
         try{
            PrintWriter out = new PrintWriter(new FileWriter(sOut));
            for(int t = 0; t < bsData[i].length; t++)
               out.println(bsData[i][t]);
            out.close();
         } catch (Exception e){
            System.err.printf("Warning: failed to save quantized data file:\n %s\n", sOut);
         }
      }

      return true;
   }

   /**
    * Output info for this data over a range of quantization parameters
    */
   public static boolean scan()
   {
      clust.setVerbose(true);
      System.err.println("Scanning for K=2:20...");
      for(int nQuant = 2; nQuant <= 20; nQuant++){
         clust.cluster(nQuant, input);
         double err = clust.getAvgError(input);
         if (out != null){
            out.printf("%02d  %.6f\n", nQuant, err);
            out.flush();
         }
      }

      return true;
   }

   protected static String convFileAuto(String s)
   {
      String sPath = Library.getPath(s);
      String sTitle = Library.getTitle(s);
      String sSuffix = Library.getSuffix(s);
      return sPath + sTitle + "q." + sSuffix;
   }

   /**
    * Main entry point of program
    */
   public static void main(String args[])
   {
      clust = new DiagGaussKMeans();
      clust.setVerbose(true);

      int nQuant = -1;
      boolean bScan = false;
      String sCentersIn = null;
      String sCentersOut = null;
      String sFileOut = null;
      String sLoader = null;
      int wlen = 0, nSax = 3, nPaa = 0;
      String sMethod = null;

      int c, iw;
      LongOpt[] longopts = new LongOpt[] { new LongOpt("help", LongOpt.NO_ARGUMENT, null, 1001),
            new LongOpt("v", LongOpt.NO_ARGUMENT, null, 1002),
            new LongOpt("quant", LongOpt.REQUIRED_ARGUMENT, null, 1003),
            new LongOpt("scan", LongOpt.NO_ARGUMENT, null, 1004),
            new LongOpt("centers-out", LongOpt.REQUIRED_ARGUMENT, null, 1005),
            new LongOpt("centers-in", LongOpt.REQUIRED_ARGUMENT, null, 1006),
            new LongOpt("out", LongOpt.REQUIRED_ARGUMENT, null, 1007),
            new LongOpt("white", LongOpt.NO_ARGUMENT, null, 1008),
            new LongOpt("loader", LongOpt.REQUIRED_ARGUMENT, null, 1009),
            new LongOpt("method", LongOpt.REQUIRED_ARGUMENT, null, 1010),
            new LongOpt("wlen", LongOpt.REQUIRED_ARGUMENT, null, 1011),
            new LongOpt("paa", LongOpt.REQUIRED_ARGUMENT, null, 1012),
            new LongOpt("sax", LongOpt.REQUIRED_ARGUMENT, null, 1013) };

      Getopt g = new Getopt("Quantize", args, "?", longopts, true);
      while((c = g.getopt()) != -1){
         switch(c){
         case '?':
         case 1001: // help
            usage();
            System.exit(0);
            break;
         case 1002: // verbose
            bVerbose = true;
            break;
         case 1003: // quant
            nQuant = Integer.parseInt(g.getOptarg());
            break;
         case 1004: // scan
            bScan = true;
            break;
         case 1005: // centers-out
            sCentersOut = g.getOptarg();
            break;
         case 1006: // centers-in
            sCentersIn = g.getOptarg();
            break;
         case 1007: // out
            sFileOut = g.getOptarg();
            break;
         case 1008: // white
            bWhiten = true;
            break;
         case 1009: // loader
            sLoader = g.getOptarg();
            break;
         case 1010: // method
            sMethod = g.getOptarg();
            break;
         case 1011: // wlen
            wlen = Integer.parseInt(g.getOptarg());
            break;
         case 1012: // paa
            nPaa = Integer.parseInt(g.getOptarg());
            break;
         case 1013: // sax
            nSax = Integer.parseInt(g.getOptarg());
            break;
         }
      }

      // which method are we using?
      if (sMethod == null){
         System.err.printf("Error: no method specified (use -method)\n");
         System.exit(1);
      }

      if (Library.stricmp(sMethod, "em")) method = Method.EM;
      else if (Library.stricmp(sMethod, "sax")) method = Method.SAX;
      else{
         System.err.printf("Error: unknown method (%s)\n", sMethod);
         System.exit(1);
      }

      if (method == Method.EM){
         if (nQuant < 2 && sCentersIn == null){
            System.err.printf("Error: invalid number of symbols (use -quant option): %d\n", nQuant);
            System.exit(1);
         }
      }
      else{
         if (nSax < 2 || nSax > 9){
            System.err.printf("Error: invalid number of SAX symbols (%d)\n", nSax);
            System.exit(1);
         }
         if (wlen < 1){
            System.err.printf("Error: invalid window length (use -wlen): %d\n", wlen);
            System.exit(1);
         }
         if (nPaa == 0) nPaa = Math.max(wlen / 3, 2);
         if (nPaa < 1 || nPaa > wlen){
            System.err.printf("Error: invalid number of PAA segmetns (wlen=%d, #paa=%d)\n", wlen, nPaa);
            System.exit(1);
         }
      }

      // check for the input file
      int nArgs = args.length - g.getOptind();
      if (nArgs < 1){
         System.err.println("\nError: expecting input file(s), found " + nArgs + " arguments\n");
         System.exit(1);
      }

      // instantiate a new loader if requested
      if (sLoader != null){
         try{
            Class cls = Library.getClass(sLoader, "kdm.io.DataLoader");
            gloader = (DataLoader)cls.newInstance();
         } catch (Exception e){
            System.err.printf("Error: failed to instantiate class (%s)\n", sLoader);
         }
      }

      // load the data
      for(int i = g.getOptind(); i < args.length; i++){
         String sFileIn = args[i];
         String sExt = Library.getExt(sFileIn);
         if (sExt.equals("def")){
            tseries = DataDefLoader.loadSeqs(new File(sFileIn), null);
            for(Sequence seq : tseries){
               if (input == null) input = new Sequence(seq);
               else input.append(seq, true, false);
            }
         }
         else{
            tseries = new ArrayList<Sequence>();
            DataLoader loader = null;
            if (sExt.equals("ext")) loader = new DLHtkExt();
            else loader = gloader;
            Sequence seq = loader.load(sFileIn);
            if (seq == null){
               System.err.println("Failed to load data file:\n " + sFileIn);
               System.exit(1);
            }
            seq.setName(sFileIn);
            tseries.add(seq);
            if (input == null) input = new Sequence(seq);
            else input.append(seq, true, false);
         }
      }

      // setup output to a file if requested
      if (sFileOut != null){
         try{
            if (Library.stricmp(sFileOut, "auto")){
               String sIn = args[g.getOptind()];
               sFileOut = convFileAuto(sIn);
               if (bVerbose) System.err.printf("Auto-generated output file:\n %s\n", sFileOut);
            }
            out = new PrintWriter(new FileWriter(sFileOut));
         } catch (IOException e){
            System.err.println("Error: unable to open output file for writing\n " + sFileOut);
            System.exit(1);
         }
      }
      else if (sCentersOut == null) out = new PrintWriter(System.out);

      // whiten data if requested
      if (bWhiten){
         if (bVerbose) System.err.println("Whitening data...");
         double[][] data = input.toSeqArray();
         sdev = new double[input.getNumDims()];
         for(int i = 0; i < sdev.length; i++){
            sdev[i] = Math.sqrt(StatUtils.variance(data[i]));
            if (bVerbose) System.err.printf(" Standard Deviation %d: %.4f\n", i + 1, sdev[i]);
         }

         // do the actual whitening
         for(int i = 0; i < input.length(); i++)
            for(int d = 0; d < sdev.length; d++)
               input.set(i, d, input.get(i, d) / sdev[d]);
      }

      // execute the requested functionality
      if (bScan) scan();
      else if (method == Method.EM || sCentersIn != null) processEM(nQuant, sCentersIn, sCentersOut);
      else if (method == Method.SAX) processSAX(wlen, nPaa, nSax);

      // clean up
      if (out != null) out.close();
   }

}
