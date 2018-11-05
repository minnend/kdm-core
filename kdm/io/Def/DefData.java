package kdm.io.Def;

import java.awt.Color;
import java.io.File;
import java.util.*;

import kdm.data.*;
import kdm.data.transform.*;
import kdm.io.BinaryData;
import kdm.io.DataLoader.DataLoader;
import kdm.util.*;

/**
 * Defines a data definition block
 */
public class DefData extends AbstractDef
{
   public String name;
   public String sDataFile;
   public String sLabelFile;
   public String sDataLoader;
   public String sLabelLoader;
   public String sDataParams;
   public String sLabelParams;
   public String sOrigFile;
   public String sVideo;
   public Sequence data;
   public MarkupSet labels;
   public Calendar startTime;
   public boolean bIgnore = false;
   public int iSeries;
   public double fps;
   public TreeMap<Integer, Integer> syncsOrig;
   public TreeMap<Integer, Integer> syncsScaled;
   public ArrayList<DataTransform> trans;
   public ArrayList<String> ignoreClasses;

   protected DataDefLoader parent;

   public String toString()
   {
      return String.format("Name: %s\nData: %s\nLabels: %s\n", name, sDataFile, sLabelFile);
   }

   public DefData(File fBase, int iSeries, DataDefLoader _parent)
   {
      super(fBase);
      this.iSeries = iSeries;
      parent = _parent;
      syncsOrig = new TreeMap<Integer, Integer>();
      syncsScaled = new TreeMap<Integer, Integer>();
      sDataLoader = "DLRaw";
      sLabelLoader = "MLGeneral";
      ignoreClasses = new ArrayList<String>();
      trans = new ArrayList<DataTransform>();
   }

   public boolean init(String sKey, String sVal)
   {
      if (Library.stricmp(sKey, "name")) name = sVal;
      else if (Library.stricmp(sKey, "fps")) fps = Double.parseDouble(sVal);
      else if (sKey.startsWith("sync"))
      {
         int i = sVal.indexOf("->");
         if (i < 0)
         {
            System.err.println("Error: malformed sync value: " + sVal);
            return false;
         }
         String sData = sVal.substring(0, i).trim();
         String sVideo = sVal.substring(i + 2).trim();
         int iData = Integer.parseInt(sData);
         int iVideo = Integer.parseInt(sVideo);
         syncsOrig.put(iData, iVideo);
      }
      else if (Library.stricmp(sKey, "images")) sVideo = sVal;
      else if (Library.stricmp(sKey, "data") || Library.stricmp(sKey, "file")) sDataFile = Library
            .qualifyPath(sVal, fBase);
      else if (Library.stricmp(sKey, "labels")) sLabelFile = Library.qualifyPath(sVal, fBase);
      else if (Library.stricmp(sKey, "dataLoader") || Library.stricmp(sKey, "loader")) sDataLoader = sVal;
      else if (Library.stricmp(sKey, "labelLoader")) sLabelLoader = sVal;
      else if (Library.stricmp(sKey, "loadDataParams") || Library.stricmp(sKey, "dataParams")
            || Library.stricmp(sKey, "params")) sDataParams = sVal;
      else if (Library.stricmp(sKey, "loadLabelParams") || Library.stricmp(sKey, "labelParams")) sLabelParams = sVal;
      else if (Library.stricmp(sKey, "ignoreClass"))
      {
         StringTokenizer st = new StringTokenizer(sVal, ",");
         while(st.hasMoreTokens())
            ignoreClasses.add(st.nextToken().trim());
      }
      else if (Library.stricmp(sKey, "orig-file")) sOrigFile = Library.qualifyPath(sVal, fBase);
      else if (Library.stricmp(sKey, "start-time"))
      {
         startTime = Library.str2cal(sVal);
         if (startTime == null)
         {
            System.err.printf("Error: failed to extract time from string (%s)\n", sVal);
            return false;
         }
      }
      else if (Library.stricmp(sKey, "transforms"))
      {
         StringTokenizer st = new StringTokenizer(sVal, ",");
         while(st.hasMoreTokens())
         {
            String sTrans = st.nextToken().trim();
            DataTransform tran = parent.findTrans(sTrans);
            if (tran == null)
            {
               System.err.println("Error: failed to find transformation (" + sTrans + ")");
               return false;
            }
            trans.add(tran);
         }
      }
      else if (Library.stricmp(sKey, "ignore")) bIgnore = ConfigHelper.isTrueString(sVal);
      else if (Library.stricmp(sKey, "series"))
      {
         System.err.printf("Warning: \"series\" is deprecated in data blocks, please remove it from your .def file.\n");         
      }
      else return super.init(sKey, sVal);
      return true;
   }
}
