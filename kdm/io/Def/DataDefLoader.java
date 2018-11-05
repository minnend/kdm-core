package kdm.io.Def;

import java.util.*;
import java.io.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.xml.sax.ext.*;
import kdm.util.*;
import kdm.data.*;
import kdm.data.transform.*;
import kdm.io.*;
import kdm.io.DataLoader.*;

/**
 * Loads a data definition
 */
public class DataDefLoader extends AbstractDefLoader
{
   protected StringBuffer sbBlock = null;
   public ArrayList<DefData> data;
   public ArrayList<DefTransform> trans;
   protected DefData curData;
   protected DefTransform curTrans;
   public File fBinary = null;
   Sequence[] binData = null;
   public boolean bLoadData = true;
   public String sSkip = null;
   public HashSet<String> skips;
   public static boolean bVerbose = false;
   protected int iData;

   public DataDefLoader()
   {
      skips = new HashSet<String>();
      skips.add("comp");
      skips.add("comp-def");
      iData = 0;
   }

   public boolean hasBinary()
   {
      return fBinary != null;
   }

   public boolean isBinaryLoaded()
   {
      return (binData != null);
   }

   public File getBinaryFile()
   {
      return fBinary;
   }

   /** @return list of data sequences */
   public ArrayList<Sequence> collectData()
   {
      ArrayList<Sequence> tseries = new ArrayList<Sequence>();
      for(DefData d : data)
         if (d.data != null) tseries.add(d.data);
      return tseries;
   }

   /** @return list of labels from each data component (list may contain null if data has no labels) */
   public ArrayList<MarkupSet> collectLabels(boolean bOnlyIfData)
   {
      ArrayList<MarkupSet> marks = new ArrayList<MarkupSet>();
      for(DefData d : data){
         if (bOnlyIfData && d.data == null) continue;
         marks.add(d.labels); // add even if null, to match the data list
      }
      return marks;
   }

   public void setLoadData(boolean _bLoadData)
   {
      bLoadData = _bLoadData;
   }

   public DefData findData(String sData)
   {
      for(DefData d : data)
         if (d.name.equals(sData)) return d;
      return null;
   }

   public DataTransform findTrans(String sTrans)
   {
      for(DefTransform d : trans)
         if (d.name.equals(sTrans)) return d.trans;
      return null;
   }

   public boolean preLoad()
   {
      data = new ArrayList<DefData>();
      trans = new ArrayList<DefTransform>();
      return super.preLoad();
   }

   public boolean loadData(DefData d)
   {
      if (bLoadData && hasBinary() && !isBinaryLoaded()) binData = BinaryData.load(fBinary);

      if (d.bIgnore){
         System.err.printf("Ignoring data: \"%s\"\n", d.name);
         return true;
      }
      try{
         if (binData != null){
            if (binData.length != data.size()){
               System.err.printf("Warning: found binary data, but length not equal to number "
                     + "of data def blocks (%d vs. %d)\n", binData.length, data.size());
               binData = null;
            }
         }

         // load the data
         if (bLoadData){
            if (binData != null) d.data = binData[d.iSeries];
            else{
               Class cls = Library.getClass(d.sDataLoader, "kdm.io.DataLoader");
               DataLoader loader = (DataLoader)cls.newInstance();
               if (d.startTime != null) loader.setStarTime(d.startTime);
               if (!loader.config(fBase, d.sDataParams)){
                  System.err.println("Error: failed to configure data loader!");
                  System.err.println(" class = " + d.sDataLoader);
                  System.err.println(" params = \"" + d.sDataParams + "\"");
                  System.exit(1);
               }
               // System.err.printf("Loading data: %s\n", d.sDataFile);
               d.data = loader.load(d.sDataFile);
               // System.err.printf(" %s\n", d.data);
            }
            if (d.data == null) return false;
            if (d.data.getOrigFile() == null)
               d.data.setOrigFile(d.sOrigFile != null ? d.sOrigFile : d.sDataFile);
            if (bVerbose)
               System.err.printf(" \"%s\" has %d tranformations; freq=%.2f (%.5f).\n", d.name, d.trans
                     .size(), d.data.getFreq(), d.data.getPeriod());
            for(DataTransform tran : d.trans)
               d.data = tran.transform(d.data);
            if (bVerbose)
               System.err.printf(" %dD  #frames=%d  [%s] -> [%s]\n", d.data.getNumDims(), d.data.length(),
                     Library.formatTime(d.data.getStartMS()), Library.formatTime(d.data.getEndMS()));
         }

         // load the labels
         if (d.sLabelFile != null){
            Class cls = Library.getClass(d.sLabelLoader, "kdm.io");
            MarkupLoader mloader = (MarkupLoader)cls.newInstance();
            if (!mloader.config(d.getBasePath(), d.sLabelParams)){
               System.err.println("Error: failed to configure markup loader!");
               System.err.println(" class = " + d.sLabelLoader);
               System.err.println(" params = \"" + d.sLabelParams + "\"");
               System.exit(1);
            }

            d.labels = mloader.load(d.sLabelFile);
            if (d.labels != null){
               d.labels.setName(d.name);
               d.labels.setSeq(d.data);
               d.labels.setFile(new File(d.sLabelFile));
               d.labels.removeClasses(d.ignoreClasses);
            }
         }
      } catch (Exception e){
         e.printStackTrace();
         return false;
      }

      // set the name of the sequence to the name given in the def file
      if (d.name != null && d.data!=null) d.data.setName(d.name);
      return true;
   }

   public boolean postLoad()
   {
      for(int i = 0; i < data.size(); i++){
         assert (data.get(i).iSeries == i);
         loadData(data.get(i));
      }
      return true;
   }

   public void startElement(String uri, String localName, String qName, Attributes attributes)
   {
      if (Library.stricmp(localName, "data")){
         curData = new DefData(fBase, iData++, this);
      }
      else if (Library.stricmp(localName, "transform")){
         curTrans = new DefTransform(fBase);
      }
      else if (Library.stricmp(localName, "binary")){
         // nothing to do until end of tag
      }
      else if (!localName.equals(sXMLRootTag)){
         if (!skips.contains(localName))
            System.err.println("Warning: unknown data definition tag: " + localName);
         sSkip = localName;
         skips.add(sSkip);
      }
   }

   public void endElement(String uri, String localName, String qName)
   {
      if (sSkip != null && localName.equals(sSkip)){
         sSkip = null;
         return;
      }

      if (sbBlock != null){
         if (Library.stricmp(localName, "data")){
            if (!curData.init(sbBlock.toString())){
               System.err.println("Error: Failed to initialize data definition!");
               System.exit(1);
            }
            data.add(curData);
         }
         else if (Library.stricmp(localName, "transform")){
            trans.add(new DefTransform(fBase, sbBlock.toString()));
         }
         else if (Library.stricmp(localName, "binary")){
            fBinary = new File(fBase, sbBlock.toString());
         }
         sbBlock = null;
      }
   }

   public void characters(char[] ch, int start, int length)
   {
      String s = new String(ch, start, length);
      if (sbBlock == null) sbBlock = new StringBuffer(s);
      else sbBlock.append(s);
   }

   /**
    * Load all of the sequences found in the given data def file
    * 
    * @param file name of file to load
    * @param repmap replacement map for text replacement
    * @return list of Sequences, null on error
    */
   public static ArrayList<Sequence> loadSeqs(File file, AbstractMap<String, String> repmap)
   {
      // load the data def file
      DataDefLoader ddl = new DataDefLoader();
      if (!ddl.load(file, repmap)){
         System.err.println("Error: failed to load data definition file");
         System.exit(1);
      }

      // extract sequences from data def objects
      return ddl.collectData();
   }
}
