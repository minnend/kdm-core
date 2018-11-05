package kdm.tools;

import java.util.*;
import java.io.*;

import kdm.util.*;
import gnu.getopt.*;

/**
 * Generate synthetic word data for discovery testing
 */
public class GenWordData
{
   // words selected from 100 common SAT words
   // http://www.quia.com/servlets/quia.activities.common.ActivityPlayer?AP_rand=1671146769&AP_activityType=1&AP_urlId=1527&gameType=list
   public final static String[] vocab;
   public final static String allWords = "abbreviate, abstinence, adulation, adversity, aesthetic, amicable, anachronistic, "
         + "anecdote, anonymous, antagonist, arid, assiduous, asylum, benevolent, camaraderie, "
         + "censure, circuitous, clairvoyant, collaborate, compassion, compromise, condescending, "
         + "conditional, conformist, congregation, convergence, deleterious, demagogue, digression, "
         + "diligent, discredit, disdain, divergent, empathy, emulate, enervating, enhance, ephemeral, "
         + "evanescent, exasperation, exemplary, extenuating, florid, fortuitous, frugal, hackneyed, "
         + "haughty, hedonist, hypothesis, impetuous, impute, incompatible, inconsequential, inevitable, "
         + "integrity, intrepid, intuitive, jubilation, lobbyist, longevity, mundane, nonchalant, "
         + "novice, opulent, orator, ostentatious, parched, perfidious, precocious, pretentious, "
         + "procrastinate, prosaic, prosperity, provocative, prudent, querulous, rancorous, reclusive, "
         + "reconciliation, renovation, resilient, restrained, reverence, sagacity, scrutinize, "
         + "spontaneity, spurious, submissive, substantiate, subtle, superficial, superfluous, suppress, "
         + "surreptitious, tactful, tenacious, transient, venerable, vindicate, wary";
   public static int nMutate = 0;
   public static int nStretch = 1;
   public static double density = 1.0;
   public static int nWords = 1000;
   public static int nVocab;
   public static TreeMap<String, MyIntList> truth = new TreeMap<String, MyIntList>();

   static
   {
      StringTokenizer st = new StringTokenizer(allWords, ", ");
      vocab = new String[st.countTokens()];
      for(int i = 0; i < vocab.length; i++)
         vocab[i] = st.nextToken();
      nVocab = vocab.length;
   }
   
   /** log the true position of a word */
   public static void addWord(String word, int iPos)
   {
      MyIntList list = truth.get(word);
      if (list == null)
      {
         list = new MyIntList();
         truth.put(word, list);
      }
      list.add(iPos);
   }

   /** @return randomly mutated and stretched version of the given word  */
   public static String mungeWord(String word)
   {
      char[] base = word.toCharArray();
      StringBuffer sb = new StringBuffer();

      // mutate characters
      if (nMutate > 0)
      {
         int n = Library.random(nMutate + 1);
         int[] indices = Library.selectRandomIndices(n, base.length);
         for(int i = 0; i < indices.length; i++)
            base[indices[i]] = (char)('a' + Library.random(26));
      }

      // stretch characters and copy to string buffer
      for(int i = 0; i < base.length; i++)
      {
         int n = Library.random(nStretch) + 1;
         for(int j = 0; j < n; j++)
            sb.append(base[i]);
      }

      return sb.toString();
   }
   
   /** @return background characters of the given length */
   public static String genBackgroundString(int len)
   {
      char[] a = new char[len];
      for(int i=0; i<len; i++) a[i] = (char)('a' + Library.random(26));
      return new String(a);
   }

   /** Generate a sentence given the current parameters */
   public static String genSentence()
   {
      assert (vocab.length >= 1);
      assert (nWords > 1);
      assert (nStretch >= 1);
      assert (nMutate >= 0);
      assert (density >= 0.0001);

      // make sure that words are evenly distributed and randomly ordered
      String words[][] = new String[nWords][2];
      int[] ixw = new int[nWords];
      int j=0;     
      double fn = (double)nWords / nVocab;
      double f = fn;
      for(int i = 0; i < nVocab; i++, f+=fn)
      {
         for(int k=j; k<Math.round(f); k++) ixw[k] = i;
         j = (int)Math.round(f);
      }
      Library.shuffle(ixw);
      
      // generate the munged words                  
      int nWordLen = 0;
      for(int i = 0; i < nWords; i++)
      {
         words[i][0] = vocab[ixw[i]];
         words[i][1] = mungeWord(words[i][0]);
         nWordLen += words[i][1].length();
      }

      // now generate the sentence by adding random characters
      int nSentLen = (int)Math.ceil(nWordLen / density);      
      int nExtraLeft = nSentLen - nWordLen;
      int nGaps = nWords + 1;
      int nGapsLeft = nGaps;
      StringBuffer sb = new StringBuffer();
      for(int i=0; i<nWords; i++)
      {
         int nExtra = Math.round(nExtraLeft / nGapsLeft);
         if (nExtra > 0) sb.append(genBackgroundString(nExtra));
         addWord(words[i][0], sb.length());
         sb.append(words[i][1]);
         nGapsLeft--;
         nExtraLeft -= nExtra;
      }
      
      // there may be more extra characters at the end
      if (nExtraLeft > 0) sb.append(genBackgroundString(nExtraLeft));

      System.err.printf(" Total length of words: %d\n", nWordLen);
      System.err.printf(" Sentence length: %d (%d)\n", nSentLen, sb.length());
      
      return sb.toString();
   }
   
   /** save the ground truth information in the given file */
   public static boolean saveTruth(String sFile)
   {
      try{
      PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(sFile)));
      Iterator<String> it = truth.keySet().iterator();
      while(it.hasNext())
      {
         String word = it.next();
         MyIntList list = truth.get(word);
         out.printf("%s ", word);
         for(int i=0; i<list.size(); i++)
            out.printf("%d ", list.get(i));
         out.println();
      }
      out.close();
      return true;
      }
      catch(Exception e){ return false; }
   }

   public static void usage()
   {
      System.err.printf("\n");
      System.err.printf("USAGE: java ~.GenWordData [options]\n");
      System.err.printf(" -words <n>            number of words in string (%d)\n", nWords);
      System.err.printf(" -mutate <n>           mutate up to n letters in each word (%d)\n", nMutate);
      System.err.printf(" -stretch <n>          repeat each character up to n times (%d)\n", nStretch);
      System.err.printf(" -density <x>          density of motifs (%.1f)\n", density);
      System.err.printf(" -vocab <n>            specify number of vocab words to use (%d=max)\n", nVocab);
      System.err.printf(" -truth <file>         save the ground truth in this file\n");
      System.err.printf("\n");
   }

   public static void main(String args[])
   {
      String sTruth = null;
      
      int c;
      LongOpt[] longopts = new LongOpt[] { new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
            new LongOpt("words", LongOpt.REQUIRED_ARGUMENT, null, 1001),
            new LongOpt("mutate", LongOpt.REQUIRED_ARGUMENT, null, 1002),
            new LongOpt("stretch", LongOpt.REQUIRED_ARGUMENT, null, 1003),
            new LongOpt("density", LongOpt.REQUIRED_ARGUMENT, null, 1004),
            new LongOpt("vocab", LongOpt.REQUIRED_ARGUMENT, null, 1005),
            new LongOpt("truth", LongOpt.REQUIRED_ARGUMENT, null, 1006)
      };

      Getopt g = new Getopt("AssistEval", args, "?", longopts, true);
      while((c = g.getopt()) != -1)
      {
         String arg = g.getOptarg();
         switch(c){
         case '?':
         case 'h': // help
            usage();
            System.exit(0);
            break;
         case 1001: // words
            nWords = Integer.parseInt(arg);
            break;
         case 1002: // mutate
            nMutate = Integer.parseInt(arg);
            break;
         case 1003: // stretch
            nStretch = Integer.parseInt(arg);
            if (nStretch < 1) nStretch = 1;
            break;
         case 1004: // density
            density = Double.parseDouble(arg);
            if (density < 0) density = 0.0001;
            else if (density > 1) density = 1;
            break;
         case 1005: // vocab
            nVocab = Integer.parseInt(arg);
            if (nVocab < 1 || nVocab > vocab.length) nVocab = vocab.length;
            break;
         case 1006: // truth
            sTruth = arg;
            break;
         }
      }

      System.err.printf("Generating word data\n");
      System.err.printf(" Vocabulary size: %d words\n", nVocab);
      System.err.printf(" Sentence length: %d words\n", nWords);
      System.err.printf(" Word density: %.2f\n", density);
      System.err.printf(" Max number of mutations: %d\n", nMutate);
      System.err.printf(" Max stretch: %d\n", nStretch);
      System.err.printf(" Ground truth file: %s\n", sTruth==null?"none":sTruth);

      String sentence = genSentence();
      System.out.println(sentence);
      
      if (sTruth!=null && !saveTruth(sTruth))
         System.err.printf("Error: failed to save ground truth data\n (%s)\n", sTruth);
   }

}
