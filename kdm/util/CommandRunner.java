package kdm.util;

import java.io.*;

/**
 * Execute a program via the command line
 */
public class CommandRunner
{
   private int exitCode = Integer.MIN_VALUE;
   protected StringWriter swErr, swOut;

   /**
    * @return exit code of process
    */
   public int getExitCode()
   {
      return exitCode;
   }

   public String getErrStream()
   {
      return (swErr == null ? null : swErr.getBuffer().toString());
   }

   public String getOutStream()
   {
      return (swOut == null ? null : swOut.getBuffer().toString());
   }

   /** run the given command */
   public boolean run(String[] args, File path)
   {
      try{
         Process proc = Runtime.getRuntime().exec(args, null, path);
         return capture(proc);
      } catch (Exception e){
         e.printStackTrace();
         return false;
      }

   }

   /** run the given command */
   public boolean run(String arg, File path)
   {
      try{
         Process proc = Runtime.getRuntime().exec(arg, null, path);
         return capture(proc);
      } catch (Exception e){
         e.printStackTrace();
         return false;
      }
   }

   /** capture the output of the process and wait for it to finish */
   protected boolean capture(Process proc) throws InterruptedException
   {
      swErr = new StringWriter(0);
      swOut = new StringWriter(0);
      Thread err = new Thread(new IOBridge(new PrintWriter(swErr), proc.getErrorStream()));
      Thread out = new Thread(new IOBridge(new PrintWriter(swOut), proc.getInputStream()));
      err.start();
      out.start();
      exitCode = proc.waitFor();
      err.join();
      out.join();
      return true;
   }

   /**
    * Threaded class that reads from specified input stream and writes to specified print stream.
    */
   class IOBridge implements Runnable
   {
      InputStream in;
      PrintWriter out;

      public IOBridge(PrintWriter out, InputStream in)
      {
         this.out = out;
         this.in = in;
      }

      public void run()
      {
         try{
            BufferedReader inb = new BufferedReader(new InputStreamReader(in));
            String temp = null;
            while((temp = inb.readLine()) != null)
               out.println(temp);
            out.flush();
            inb.close();
            out.close();
         } catch (Exception e){
            e.printStackTrace();
         }
      }
   }
}
