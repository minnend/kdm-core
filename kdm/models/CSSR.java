package kdm.models;

import java.util.regex.*;
import java.util.*;
import java.io.*;

/**
 * Finite state machine with discrete, probabilistic emissions as learned by the CSSR algorithm of Shalizi and
 * Shalizi.
 */
public class CSSR
{
   protected String alpha;
   ArrayList<CSSRNode> nodes;
   
   protected CSSR(String alpha, ArrayList<CSSRNode> nodes)
   {
      this.alpha = alpha;
      this.nodes = nodes;
   }
   
   public static CSSR load(File file)
   {
      Pattern patProb = Pattern.compile("P\\((.)\\) = (\\d(?:\\.\\d+)?)");
      Pattern patTran = Pattern.compile("T\\((.)\\) = (\\d+)");
      
      try{
         BufferedReader in = new BufferedReader(new FileReader(file));
         
         ArrayList<CSSRNode> nodes = new ArrayList<CSSRNode>();
         Set<Character> alphabet = new HashSet<Character>();
         while(true)
         {
            String line = in.readLine();
            if (line == null) break;
            line.trim();
            if (line.length()==0) continue;
            if (!line.startsWith("State number: "))
            {
               System.err.printf("File format error: expected \"State number: \", found \"%s\"\n", line);
               return null;
            }
            int stateNum = Integer.parseInt(line.substring(14));
            
            // read the suffixes
            HashSet<String> suffs = new HashSet<String>();
            while(true)
            {
               line = in.readLine();
               if (line == null) return null;
               line.trim();
               if (line.length()==0) continue;
               if (line.startsWith("distribution:")) break;
               suffs.add(line);
            }
            if (!line.startsWith("distribution:")) return null;
            
            // grab the symbol distribution
            HashMap<Character,Double> probs = new HashMap<Character, Double>();
            Matcher m = patProb.matcher(line);
            while(m.find())
            {
               char symbol = m.group(1).charAt(0);
               double prob = Double.parseDouble(m.group(2));
               probs.put(symbol, prob);
            }
            
            // grab the transition table
            line = in.readLine();
            HashMap<Character,Integer> trans = new HashMap<Character, Integer>();
            if (!line.startsWith("transitions:")) return null;            
            m = patTran.matcher(line);
            while(m.find())
            {
               char symbol = m.group(1).charAt(0);
               alphabet.add(symbol);
               int state = Integer.parseInt(m.group(2));
               trans.put(symbol, state);
            }
            
            // build the node
            CSSRNode node = new CSSRNode(stateNum);
            node.suffs = suffs;
            node.trans = trans;
            node.setEmitProbs(probs);
            nodes.add(node);
            
            
            // skip the P(state)
            in.readLine();
         }         
         in.close();
         
         // build alphabet string
         Iterator<Character> it = alphabet.iterator();
         char[] a = new char[alphabet.size()];
         for(int i=0; i<a.length; i++) a[i] = it.next();
         Arrays.sort(a);
         
         return new CSSR(new String(a), nodes);
      }
      catch(Exception e)
      {
         System.err.println(e);
      }
      
      return null;
   }
   
   protected int getSymbolIndex(char c)
   {
      return alpha.indexOf(c);
   }
   
   public void scan(String data)
   {
      int T = data.length()-1;
      int N = nodes.size();
      int A = alpha.length();
      int state = -1;
      StringBuffer sb = new StringBuffer();
      for(int t=0; t<T; t++)
      {
         char symbol = data.charAt(t);
         char next = data.charAt(t+1);
         if (state == -1)
         {
            sb.append(symbol);
            String suff = sb.toString();
            for(int i=0; i<N; i++)
            {
               CSSRNode node = nodes.get(i);
               if (node.contains(suff))
               {
                  state = i;
                  System.err.printf("Found start node (%d), suff=%s\n", i, suff);
                  break;
               }
            }
            if (state == -1) continue; // didn't find start node yet
         }
         
         CSSRNode node = nodes.get(state);
         System.err.printf("t=%03d  node=%03d  predict=[%.0f", t, state, node.prob[0]*100);
         for(int i=1; i<A; i++)
            System.err.printf(",%.0f", node.prob[i]*100);
         System.err.printf("]  next=%c\n", next);
         state = node.trans.get(symbol);
         if (t>=100) break;
      }
   }
   
   public void dump()
   {
      int n = nodes.size();
      System.err.printf("CSSR: %d nodes\n", n);
      for(int i=0; i<n; i++)
      {
         CSSRNode node = nodes.get(i);
         System.err.printf(" %d) %d, %d suffixes\n", i, node.name,  node.suffs.size());
      }
   }
   
   public static void main(String args[])
   {
      CSSR fsm = CSSR.load(new File("/home/dminn/papers/cssr/CSSR-v0.1/data.txt_results"));
      try{
         BufferedReader in = new BufferedReader(new FileReader("/home/dminn/papers/cssr/CSSR-v0.1/data.txt"));
         String line = in.readLine();
         fsm.scan(line);
      }
      catch(Exception e)
      {
         System.err.println(e);         
      }
   }
}

///////////////////////////////////////////////////////////

/** a node in a CSSR fsm */
class CSSRNode implements Comparable
{
   protected int name;
   protected Set<String> suffs;   
   protected HashMap<Character,Integer> trans;
   protected double[] prob;
   protected char[] emit;
   
   public CSSRNode(int name)
   {
      this.name = name;
      suffs = new HashSet<String>();
      trans = new HashMap<Character,Integer>();
   }
   
   public void setEmitProbs(HashMap<Character,Double> map)
   {
      Character[] a = map.keySet().toArray(new Character[0]);
      Arrays.sort(a);
      prob = new double[a.length];
      emit = new char[a.length];
      for(int i=0; i<a.length; i++)
      {
         emit[i] = a[i];
         prob[i] = map.get(a[i]);
      }
   }

   public boolean contains(String s)
   {
      return suffs.contains(s);
   }
   
   public boolean equals(Object o)
   {
      CSSRNode x = (CSSRNode)o;
      return (name == x.name);
   }

   public int compareTo(Object o)
   {
      CSSRNode x = (CSSRNode)o;
      if (name < x.name) return -1;
      if (name > x.name) return 1;
      return 0;
   }
}