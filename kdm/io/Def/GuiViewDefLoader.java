package kdm.io.Def;

import java.util.*;
import java.io.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import org.xml.sax.ext.*;
import kdm.util.*;
import kdm.data.*;
import kdm.data.transform.*;
import kdm.gui.*;
import kdm.io.*;
import kdm.io.DataLoader.*;

import java.awt.Color;

/**
 * Loads a file that defines a visualization, mapping data streams to display components
 */
public class GuiViewDefLoader extends DataDefLoader
{
   protected DefComp curComp;
   public ArrayList<DefComp> comps;   
     
   public boolean preLoad()
   {      
      if (!super.preLoad()) return false;
      comps = new ArrayList<DefComp>(); 
      return true;
   }
   
   public boolean postLoad()
   {
      if (!super.postLoad()) return false;
      
      // remove components marked as "ignore"
      for(int iComp=0; iComp<comps.size(); )
      {
         DefComp comp = comps.get(iComp);
         if (comp.bIgnore) comps.remove(iComp);
         else iComp++;
      }
      
      // fill in defcomp data pointers
      for(DefComp dc : comps)
      {
         if (dc.sData==null) continue;
         dc.data = findData(dc.sData);
         if (dc.data == null)
         {
            System.err.printf("Error (GuiVideDefLoader): unable to find data \"%s\" for component \"%s\"\n", dc.sData, dc.name);
            System.exit(1);
         }         
      }
      return true;
   }
   
   public void startElement(String uri, String localName, String qName, Attributes attributes)
   {
      if (localName.equals("comp"))
      {
         curComp = new DefComp(fBase);
      }
      else if (localName.equals("comp-def")){}
      else if (localName.equals("timelegend")){}
      else if (localName.equals("view")){}
      else super.startElement(uri, localName, qName, attributes);
   }

   public void endElement(String uri, String localName, String qName)
   {
      if (sbBlock != null)
      {
         if (Library.stricmp(localName, "comp-def"))
         {
            if (!curComp.init(sbBlock.toString()))
            {
               System.err.println("Error: failed to initialize component definition (<comp-def>)");
               System.exit(1);
            }
         }
         else if (Library.stricmp(localName, "timelegend"))
         {
            // TODO !! read in time legend info
         }
         else if (Library.stricmp(localName, "comp"))
         {
            comps.add(curComp);
            curComp = null;
         }
         else if (Library.stricmp(localName, "view"))
         {
            try
            {
               curComp.views.add(new DefView(fBase, sbBlock.toString()));
            } catch (NumberFormatException nfe)
            {
               nfe.printStackTrace();
               System.exit(1);
            }
         }
         else super.endElement(uri, localName, qName);
         sbBlock = null;
      }
   }
}
