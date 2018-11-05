package kdm.mlpr.htk;

import java.io.*;
import java.util.*;

import kdm.data.*;
import kdm.io.*;
import kdm.io.DataSaver.*;
import kdm.models.*;
import kdm.util.*;

/** Interface (via command line) to HTK */
public class HTK
{
   public static final boolean bDeleteFiles = true;
   public static final String subdir;
   public static File fTmpDir;
   
   static{
      StringBuffer sb = new StringBuffer("htk_");
      for(int i=0; i<10; i++){
         if (Library.random(2)==0) sb.append((char)('a'+Library.random(26)));
         else sb.append((char)('A'+Library.random(26)));
      }
      subdir = sb.toString();
   }
   
   public static HtkSetupInfo setupContRec(HtkSetupInfo si, ArrayList<AbstractHMM> hmms, ArrayList<Sequence> seqs)
   {
      if (si==null) si = new HtkSetupInfo(hmms, seqs);
      else si.hmms = hmms;
      assert(si.seqs!=null);

      // TODO cleanup func to delete (reuse?) files while app is still running
      
      final CommandRunner cr = new CommandRunner();
      int nDims = si.seqs.get(0).getNumDims();
      TreeSet<String> words = new TreeSet<String>(); // TODO hash set instead? (nice to have words sorted)
      for(AbstractHMM hmm : hmms){
         String s = hmm.getName();
         if (words.contains(s)){
            System.err.printf("Error: duplicate HMM name (%s)\n", s);
            return null;
         }
         words.add(s);
      }

      // create temp dir if necessary
      if (fTmpDir == null){
         String sTmpDir = Library.ensurePathSep(System.getProperty("java.io.tmpdir"))+subdir;      
         fTmpDir = new File(sTmpDir);         
         fTmpDir.mkdirs();
         if (!fTmpDir.exists() || !fTmpDir.isDirectory()){
            System.err.printf("Error: failed to create HTK tmp directory (%s)\n", sTmpDir);
            assert false;
            return null;
         }
         if (bDeleteFiles) fTmpDir.deleteOnExit();
      }
      
      // create hmm for each word
      for(AbstractHMM hmm : hmms){
         File fHmm = new File(fTmpDir, hmm.getName());
         if (bDeleteFiles) fHmm.deleteOnExit();
         if (!HtkHmm.save(hmm, fHmm)){
            System.err.printf("Error: failed to save HMM (%s)\n", fHmm.getAbsolutePath());
            return null;
         }
            
      }

      // create dictionary/vocab file
      try{
         si.fDict = File.createTempFile("dict_", ".txt");
         if (bDeleteFiles) si.fDict.deleteOnExit();
         PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(si.fDict)));
         for(String s : words)
            out.printf("%s %s\n", s, s);
         out.close();
      } catch (IOException e1){
         System.err.println("Error: unable to create dictionary file");
         return null;
      }

      // create command file
      try{
         si.fCommands = File.createTempFile("commands_", ".txt");
         if (bDeleteFiles) si.fCommands.deleteOnExit();
         PrintWriter out = new PrintWriter(new FileWriter(si.fCommands));
         for(String s : words)
            out.println(s);
         out.close();
      } catch (IOException e1){
         System.err.println("Error: unable to create command file");
         return null;
      }

      // create grammar file
      try{
         si.fGrammar = File.createTempFile("grammar_", ".txt");
         if (bDeleteFiles) si.fGrammar.deleteOnExit();
         PrintWriter out = new PrintWriter(new FileWriter(si.fGrammar));
         Iterator<String> it = words.iterator();
         out.printf("$W = %s", it.next());
         while(it.hasNext())
            out.printf(" | %s", it.next());
         out.println(";");
         out.println("( < $W > )");
         out.close();
      } catch (IOException e1){
         System.err.println("Error: unable to create grammar file");
         return null;
      }

      // create lattice file
      try{
         si.fLattice = File.createTempFile("lattice_", ".txt");
         if (bDeleteFiles) si.fLattice.deleteOnExit();
         String cmd = String.format("HParse %s %s", si.fGrammar.getAbsolutePath(), si.fLattice
               .getAbsolutePath());
         if (!cr.run(cmd, fTmpDir)){
            System.err.printf("Failed to generate word lattice\n (%s)\n", cmd);
            System.exit(1);
         }
      } catch (IOException e1){
         System.err.println("Error: unable to create word lattice file");
         return null;
      }
      
      // save sequences as .ext files
      if (si.fExt == null){ // maybe they're already saved
         si.fExt = new ArrayList<File>();
         int nSeqs = seqs.size();
         DSHtkExt saveExt = new DSHtkExt();
         for(int i = 0; i < nSeqs; i++){
            Sequence seq = seqs.get(i);
            // convert data to htk ext format
            try{
               File f = File.createTempFile(String.format("data%03d_", i + 1), ".ext");
               if (bDeleteFiles) f.deleteOnExit();
               saveExt.save(seq, f.getAbsolutePath());
               si.fExt.add(f);
            } catch (IOException e1){
               System.err.println("Error: unable to create EXT file");
               return null;
            }
         }
         si.sampPer = saveExt.getSampPer();
      }
      
      return si;
   }

   public static ContRecRet contRecAfterSetup(HtkSetupInfo si)
   {
      File fMlf;

      // get name of mlf result file
      try{
         fMlf = File.createTempFile("discover_", ".mlf");
         if (bDeleteFiles) fMlf.deleteOnExit();
      } catch (IOException e1){
         System.err.println("Error: unable to create MLF file");
         return null;
      }

      // call HVite to generate the viterbi parse
      String sTmpDir = fTmpDir.getAbsolutePath();
      String cmd = String.format("HVite -d %s -L %s -i %s -w %s %s %s", sTmpDir, sTmpDir, fMlf
            .getAbsolutePath(), si.fLattice.getAbsolutePath(), si.fDict.getAbsolutePath(), si.fCommands
            .getAbsolutePath());
      for(File f : si.fExt)
         cmd += " " + f.getAbsolutePath();
      //System.err.printf("Running HVite: \"%s\"\n", cmd);

      CommandRunner cr = new CommandRunner();
      File fTmpDir = new File(sTmpDir);
      if (!cr.run(cmd, fTmpDir)){
         System.err.printf("Error: failed to generate viterbi parse\n (%s)\n", cmd);
         return null;
      }

      // parse the MLF file
      MLHtkMlf mlf = new MLHtkMlf(si.sampPer);
      if (mlf.load(fMlf.getAbsolutePath()) == null){
         System.err.printf("Error: failed to load MLF file (%s)\n", fMlf.getAbsolutePath());
         return null;
      }
      ArrayList<MarkupSet> marks = mlf.getMarkupSets();

      return new ContRecRet(marks, mlf.getSeqLogLiks(), mlf.getTotalLogLik(), cr.getOutStream(), cr
            .getErrStream());
   }
   
   /**
    * perform continuous recognition (via HVite) using the given HMMs and data sequences; note that an
    * uninformative grammar will be used (any word can follow any word). Also, all intermediate files will are
    * set to "delete on exit", including the MLF result file.
    * 
    * @param si setup information which precludes redundant work
    * @param hmms "word" models used for recognition
    * @param seqs sequences to recognize
    * @return resulting MLF file or null on error
    */
   public static ContRecRet contRec(HtkSetupInfo si, ArrayList<AbstractHMM> hmms, ArrayList<Sequence> seqs)
   {
      si = setupContRec(si, hmms, seqs);
      if (si == null) return null;
      return contRecAfterSetup(si);
   }
   
   /**
    * perform continuous recognition (via HVite) using the given HMMs and data sequences; note that an
    * uninformative grammar will be used (any word can follow any word). Also, all intermediate files will are
    * set to "delete on exit", including the MLF result file.
    * 
    * @param hmms "word" models used for recognition
    * @param seqs sequences to recognize
    * @return resulting MLF file or null on error
    */
   public static ContRecRet contRec(ArrayList<AbstractHMM> hmms, ArrayList<Sequence> seqs)
   {
      HtkSetupInfo si = setupContRec(null, hmms, seqs);
      if (si == null) return null;
      return contRecAfterSetup(si);
   }

}
