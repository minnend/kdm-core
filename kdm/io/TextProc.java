package kdm.io;

import java.io.*;
import java.util.*;
import kdm.util.*;
import java.util.regex.*;

/**
 * Class for processing text using a simple markup language.
 */
public class TextProc
{
   protected static VariableStore map;
   protected final static Pattern patPrn = Pattern.compile("\\!\\((\\%.+?)\\,(.+?)\\)");
   protected static boolean bTrace = false;

   public static String proc(String s)
   {
      map = new VariableStore();
      StringBuffer sout = new StringBuffer();
      if (proc(s, sout, 0, null) < 0) return null;
      else return sout.toString();
   }

   public static void main(String args[])
   {
      try
      {
         CommentedFileReader reader = new CommentedFileReader(new FileReader(args[0]), '#');
         String s = reader.readFile();
         reader.close();
         String s2 = proc(s);
         System.err.println(s2);
      } catch (Exception e)
      {
         e.printStackTrace();
      }
   }

   protected static int proc(String sin, StringBuffer sout, int iStart, Cmd cmd)
   {
      if (bTrace) System.err.println("proc: " + iStart);

      Matcher mCmd, mPrn;
      int iCmd, iPrn, iCmdEnd;
      String sCmdText;

      while(true)
      {
         // find next command
         iCmd = sin.indexOf("!(", iStart);
         if (iCmd >= iStart)
         {
            iCmdEnd = sin.indexOf(')', iCmd + 2);
            if (iCmdEnd < 0)
            {
               System.err.printf("Error: incomplete cmd (no matching close-paren)\n");
               return -1;
            }
            iCmdEnd++; // include the close-paren
            sCmdText = sin.substring(iCmd, iCmdEnd);
         }
         else
         {
            iCmdEnd = -1;
            sCmdText = null;
         }

         // find next prn request
         mPrn = patPrn.matcher(sin);
         if (!mPrn.find(iStart)) iPrn = -1;
         else iPrn = mPrn.start();

         // nothing left to do?
         if (iCmd < 0 && iPrn < 0) break;

         // which first: cmd or print?
         if ((iCmd >= 0 && iCmd < iPrn) || iPrn < 0) // cmd
         {
            // copy intermediate text
            sout.append(sin.substring(iStart, iCmd));

            String sCmd = sCmdText.substring(2, sCmdText.length() - 1);

            // get the command name
            StringTokenizer st = new StringTokenizer(sCmd, ",");
            String sName = st.nextToken();

            if (bTrace) System.err.println("sCmd: " + sCmd + "   sName: " + sName);

            if (Library.stricmp(sName, "for"))
            {
               CmdFor ncfor = new CmdFor(sCmd, map);
               assert ncfor.isValid() : "Invalid for loop: " + sCmd;
               int ret = 0;
               while(!ncfor.done())
               {
                  ret = proc(sin, sout, iCmdEnd, ncfor);
                  if (ret < 0) return ret;
                  ncfor.inc();
               }
               iStart = ret;
            }
            else if (Library.stricmp(sName, "endfor"))
            {
               assert cmd != null;
               assert cmd instanceof CmdFor;
               return iCmdEnd;
            }
            else if (Library.stricmp(sName, "skip"))
            {
               assert cmd != null;
               assert cmd instanceof CmdFor;
               CmdSkip skip = new CmdSkip(sCmd, map);
               assert (skip.isValid()) : String.format("invalid skip cmd (%s)", sCmd);
               if (skip.shouldSkip()) return iCmdEnd;
               else iStart = iCmdEnd;
            }
            else if (Library.stricmp(sName, "def"))
            {
               CmdDef def = new CmdDef(sCmd, map);
               assert (def.isValid()) : String.format("invalid def cmd (%s)", sCmd);
               iStart = iCmdEnd + 1;
            }
            else if (Library.stricmp(sName, "count"))
            {
               CmdCount count = new CmdCount(sCmd, map);
               assert (count.isValid()) : String.format("invalid count cmd (%s)", sCmd);
               sout.append(count.getCount());
               iStart = iCmdEnd + 1;
            }
            else
            {
               System.err.println("unknown command: " + sName);
               assert false : String.format("unknown command: " + sName);
               return -1;
            }
         }
         else
         { // print
            // copy intermediate text
            sout.append(sin.substring(iStart, mPrn.start()));

            String sFormat = mPrn.group(1);
            String sVar = map.resolveVar(mPrn.group(2));
            assert map.containsKey(sVar) : "unknown variable: " + sVar;
            String sRep = String.format(sFormat, map.get(sVar));
            if (bTrace) System.err.println("format: " + sFormat + "    var: " + sVar);
            sout.append(sRep);
            iStart = mPrn.end();
         }
      }

      // copy trailing text
      sout.append(sin.substring(iStart));

      return 0;
   }
}

// /////////////////////////////////////////////////////////

abstract class Cmd
{
   protected boolean bError;
   protected VariableStore map;

   public Cmd(VariableStore _map)
   {
      map = _map;
      bError = false;
   }

   public boolean isValid()
   {
      return !bError;
   }
}

// /////////////////////////////////////////////////////////

/**
 * Command to create a for loop
 */
class CmdFor extends Cmd
{
   public String var;
   public int val;
   public int step;
   public int stop;

   public CmdFor(String s, VariableStore map)
   {
      super(map);

      StringTokenizer st = new StringTokenizer(s, " \r\n\t,");
      String cmd = st.nextToken();
      assert cmd.equals("for");
      var = st.nextToken();
      val = map.getAsInt(st.nextToken());
      step = map.getAsInt(st.nextToken());
      assert step != 0 : "Step size can't be zero";
      String sStop = st.nextToken();
      stop = map.getAsInt(sStop);

      // verify that the loop parameters are valid (TODO: skip loop?)
      if ((step > 0 && val > stop) || (step < 0 && val < stop)) bError = true;

      // create the variable
      if (!bError) map.put(var, val);
   }

   public int inc()
   {
      val = val + step;
      if (done()) map.remove(var);
      else map.put(var, val);
      return val;
   }

   public boolean done()
   {
      if (bError) return true;
      if (step > 0) return (val > stop);
      else return (val < stop);
   }
}

// /////////////////////////////////////////////////////////

/**
 * Skip command - basically, a list of values for a variable that causes a loop to repeat
 * immediately
 */
class CmdSkip extends Cmd
{
   protected boolean bSkip;

   public CmdSkip(String s, VariableStore map)
   {
      super(map);
      try
      {
         StringTokenizer st = new StringTokenizer(s, ",");
         String sCmdName = st.nextToken().trim();
         if (!Library.stricmp(sCmdName, "skip"))
         {
            bError = true;
            return;
         }
         String sVarName = st.nextToken().trim();
         int val = map.getAsInt(sVarName);
         while(st.hasMoreTokens())
         {
            String sn = st.nextToken().trim();

            // this could be a range
            if (sn.contains("-"))
            {
               StringTokenizer st2 = new StringTokenizer(sn, " -\t ");
               String s1 = st2.nextToken();
               String s2 = st2.nextToken();
               int x = map.getAsInt(s1);
               int y = map.getAsInt(s2);
               if (val >= x && val <= y)
               {
                  bSkip = true;
                  break;
               }

            }
            else
            {
               // this could be an individual number
               int x = map.getAsInt(sn);
               if (x == val)
               {
                  bSkip = true;
                  break;
               }
            }
         }
      } catch (Exception e)
      {
         System.err.printf("Error: failed to parse \"skip\" command (%s)\n", s);
         bError = true;
      }
   }

   public boolean shouldSkip()
   {
      return bSkip;
   }
}

// /////////////////////////////////////////////////////////

class CmdDef extends Cmd
{
   public CmdDef(String s, VariableStore map)
   {
      super(map);
      try
      {
         StringTokenizer st = new StringTokenizer(s, ",");
         String sCmdName = st.nextToken().trim();
         if (!Library.stricmp(sCmdName, "def"))
         {
            bError = true;
            return;
         }
         String sVarName = st.nextToken().trim();

         int nTokens = st.countTokens();
         for(int i = 0; i < nTokens; i++)
         {
            String sn = st.nextToken().trim();
            String sVar;
            if (nTokens > 1) sVar = String.format("%s[%d]", sVarName, i);
            else sVar = sVarName;
            map.put(sVar, sn);
         }
      } catch (Exception e)
      {
         System.err.printf("Error: failed to parse \"def\" command (%s)\n", s);
         bError = true;
      }
   }
}

// /////////////////////////////////////////////////////////

class CmdCount extends Cmd
{
   protected int count = 0 ;
   
   public CmdCount(String s, VariableStore map)
   {
      super(map);
      try
      {
         StringTokenizer st = new StringTokenizer(s, ",");
         String sCmdName = st.nextToken().trim();
         if (!Library.stricmp(sCmdName, "count"))
         {
            bError = true;
            return;
         }
         String sVarName = st.nextToken().trim();
         String sRex = String.format("^%s\\[.+?\\]$", sVarName);
         Pattern pat = Pattern.compile(sRex);         
         Iterator<String> it = map.keySet().iterator();
         while(it.hasNext())
         {
            String sVar = it.next();
            Matcher m = pat.matcher(sVar);
            if (m.matches()) count++;
         }
      } catch (Exception e)
      {
         System.err.printf("Error: failed to parse \"count\" command (%s)\n", s);
         bError = true;
      }
   }
   
   public int getCount(){ return count; }
}
