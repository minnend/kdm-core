package kdm.io.Def;

import java.util.*;
import java.io.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.xml.sax.ext.*;

import kdm.io.*;
import kdm.util.*;

/**
 * Abstract base class of all "definition file" loaders
 */
public abstract class AbstractDefLoader extends DefaultHandler2
{
   protected final static String sXMLRootTag = "def-xml";
   protected File fBase;

   /** 
    * Load data in the given file
    * @return true if load successful 
    */
   public boolean load(File file)
   {
      return load(file, null);
   }
   
   /**
    * Load data def from a file and use the given repmap for text replacement
    */
   public boolean loadf(String sFile, AbstractMap<String, String> map)
   {
      return load(new File(sFile), map);
   }
   
   /**
    * Load data def from a file and use the given repmap for text replacement
    */
   public boolean load(File file, AbstractMap<String, String> repmap)
   {      
      return parse(file, '#', repmap);
   } 
   
   /**
    * Load data def from string data
    */
   public boolean loadFromString(File _fBase, String sData)
   {
      fBase = _fBase;      
      return parse(sData);
   }

   /** Load data from the file with the given name */
   public boolean loadf(String sFile)
   {
      return load(new File(sFile));
   }

   public File getBasePath()
   {
      return fBase;
   }

   /**
    * Parse the xml contained in the given file. This file uses the given character to
    * denote line comments (or no comments if cComment==null). This function will add the
    * appropriate XML header (i.e., <?xml version="1.0" ?> and the root tag).
    */
   protected boolean parse(File file, char cComment, AbstractMap<String, String> repmap)
   {
      XMLReader xml = null;
      try
      {
         fBase = file.getParentFile();
         MyReader reader;
         CommentedFileReader cfr = new CommentedFileReader(new FileReader(file), cComment);
         if (repmap != null)
         {
            ReplaceReader repread = new ReplaceReader(cfr, repmap);
            reader = repread;
         }
         else reader = cfr;
         String sData = reader.readFile();
         cfr.close();
         reader.close();
         sData = TextProc.proc(sData);
         if (!parse(sData)) return false;
      }
      catch(FileNotFoundException fnfe){
         System.err.printf("Error: file does not exist\n (%s)\n", file);
         return false;
      }
      catch (Exception e)
      {
         e.printStackTrace();
         return false;
      }
      return true;
   }
   
   /**
    * Initialize variables for a new load
    */
   public boolean preLoad(){ return true; }
   
   /** Take care of things after the load */
   public boolean postLoad(){ return true; }

   /**
    * Parse the xml contained in the given string. This function will add the appropriate
    * XML header (i.e., <?xml version="1.0" ?> and the root tag).
    */
   protected boolean parse(String sData)
   {
      if (!preLoad())
      {
         System.err.printf("Error: parse pre-load failed\n");
         return false;
      }
      XMLReader xml = null;
      try
      {
         // convert the def file into a proper xml document
         String doc = "<?xml version=\"1.0\" ?>\n<" + sXMLRootTag + ">" + sData + "</" + sXMLRootTag + ">\n";

         // parse the xml
         xml = XMLReaderFactory.createXMLReader();
         xml.setContentHandler(this);
         xml.parse(new InputSource(new StringReader(doc)));
         if (!postLoad())
         {
            System.err.printf("Error: parse post-load failed\n");
            return false;
         }
      } catch (Exception e)
      {
         e.printStackTrace();
         return false;
      }
      return true;
   }
}
