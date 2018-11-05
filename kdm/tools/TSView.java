package kdm.tools;

import kdm.data.*;
import kdm.data.transform.*;
import kdm.io.*;
import kdm.io.Def.*;
import kdm.util.*;
import kdm.gui.*;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;

import gnu.getopt.*;

import static java.awt.BorderLayout.*;
import static javax.swing.BoxLayout.*;

import static kdm.data.TimeMarker.*;

public class TSView
{
   JFrame frame;
   GraphComplexLite graphc[];
   ArrayList<Graph> winGraphs, allGraphs;

   public static void usage()
   {
      System.err.println();
      System.err.println("USAGE: java kdm.tools.TSView [options] <view file>");
      System.err.println();
      System.err.println(" Options:");
      System.err.println("  -?                      display this help message");
      System.err.println("  -marks <file>           add a label file to the view");
      System.err.println("  -scale <scale>          specify the scale of the marker file");
      System.err.println("  -D <key:value>          add an entry to the replace map");
      System.err.println();
   }

   public boolean create(String args[])
   {
      ArrayList<String> labelFiles = new ArrayList<String>();
      ArrayList<String> scales = new ArrayList<String>();
      HashMap<String, String> repMap = new HashMap<String, String>();

      int c;
      LongOpt[] longopts = new LongOpt[] { new LongOpt("help", LongOpt.NO_ARGUMENT, null, 1001),
            new LongOpt("marks", LongOpt.REQUIRED_ARGUMENT, null, 1002),
            new LongOpt("scale", LongOpt.REQUIRED_ARGUMENT, null, 1003),
            new LongOpt("D", LongOpt.REQUIRED_ARGUMENT, null, 1004) };

      Getopt g = new Getopt("TSView", args, "?", longopts, true);
      while((c = g.getopt()) != -1){
         String sArg = g.getOptarg();
         switch(c){
         case '?':
         case 1001: // help
            usage();
            System.exit(0);
            break;
         case 1002: // marks
            labelFiles.add(sArg);
            break;
         case 1003: // scale
            scales.add(sArg);
            break;
         case 1004: // map entry
         {
            StringTokenizer st = new StringTokenizer(sArg, ":");
            if (st.countTokens() != 2){
               System.err.printf("Error: malformed definition (should be <key>:<value>)\n (%s)\n", sArg);
               System.exit(0);
            }
            String sKey = st.nextToken();
            String sVal = st.nextToken();
            repMap.put(sKey, sVal);
         }
            break;
         }
      }

      // make sure that a view file was specified
      if (g.getOptind() >= args.length){
         System.err.println("Error: no view file specified.");
         System.err.println();
         return false;
      }

      GuiViewDefLoader gdef = new GuiViewDefLoader();
      String sView = args[g.getOptind()];
      System.err.printf("Loading GUI def file: %s\n", sView);
      if (!gdef.loadf(sView, repMap)){
         System.err.println("Error loading view definition file!");
         System.err.println(" (" + sView + ")");
         return false;
      }

      if (gdef.comps.size() < 1){
         System.err.println("Error: no gui components specified in definition file!");
         return false;
      }
      ArrayList<MarkupSet> labels = gdef.collectLabels(true);
      ArrayList<Sequence> data = gdef.collectData();

      // start building the gui
      String sName = sView;
      if (data.size()==1) sName = data.get(0).getName();
      frame = new JFrame("TSView: " + sName);
      frame.setLocation(120, 80);
      frame.setSize(1000, 600);
      frame.addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent e)
         {
            close();
         }
      });

      JPanel mainp = new JPanel(new BorderLayout());
      // TODO tabs with info about labeled data
      /*
       * JTabbedPane tabs = null; int nRealLabels = 0; for(MarkupSet ms : labels) if (ms!=null &&
       * !ms.isEmpty()) nRealLabels++; if (!labelFiles.isEmpty() || nRealLabels > 0) { tabs = new
       * JTabbedPane(JTabbedPane.BOTTOM); tabs.add("Data", mainp); frame.setContentPane(tabs); } else
       */
      frame.setContentPane(mainp);
      JPanel headerp = new JPanel(new VerticalLayout());
      mainp.add(headerp, BorderLayout.NORTH);

      // TODO build gui in GuiViewDefLoader?
      int nGraphs = 0;
      for(DefComp comp : gdef.comps)
         if (comp.location == DefComp.Location.Graph) nGraphs++;

      graphc = new GraphComplexLite[nGraphs];
      winGraphs = new ArrayList<Graph>();
      allGraphs = new ArrayList<Graph>();
      int iGraph = 0;
      for(DefComp compdef : gdef.comps){
         Graph graph = null;
         try{
            Class cls = Library.getClass(compdef.gui, "kdm.gui");
            graph = (Graph)cls.newInstance();
            allGraphs.add(graph);
            if (compdef.name != null) graph.setGraphName(compdef.name);
            if (compdef.data != null) graph.setData(compdef.data.data);
            if (!graph.config(gdef.getBasePath(), compdef.params)){
               System.err.println("Error: graph failed to config");
               return false;
            }
            if (!graph.finalSetup()){
               System.err.println("Error: graph failed final setup");
               return false;
            }
            graph.clearDimColors();
            for(DefView viewdef : compdef.views){
               graph.setDimColor(viewdef.dim, viewdef.color);
            }
            if (compdef.location == DefComp.Location.Window){
               JFrame frame = new JFrame(compdef.sData);
               frame.setSize(compdef.width, compdef.height);
               JPanel p = new JPanel(new BorderLayout());
               p.add(graph, BorderLayout.CENTER);
               if (graph.getLowerFramePanel() != null)
                  p.add(graph.getLowerFramePanel(), BorderLayout.SOUTH);
               frame.setContentPane(p);
               frame.setVisible(true);
               winGraphs.add(graph);
            }
            else if (compdef.location == DefComp.Location.Graph){
               graphc[iGraph++] = new GraphComplexLite(graph);
            }
            else{
               assert (compdef.location == DefComp.Location.Header);
               headerp.add(graph);
            }
         } catch (Exception e){
            e.printStackTrace();
            return false;
         }
      }

      // find master graph
      Graph masterGraph = null;
      for(Graph graph : allGraphs){
         // TODO better way to identify real graphs?
         if (!(graph instanceof SeriesSelector) && masterGraph == null) masterGraph = graph;
      }

      GridFlexLayout gfl = GraphScrollPane.constructGFL(gdef.comps);
      GraphScrollPane gsp = GraphScrollPane.construct(graphc, masterGraph, true, true, true, gfl,
            new ZoomReal(masterGraph));
      for(Graph graph : winGraphs)
         if (graph instanceof GraphListener) gsp.addGraphListener((GraphListener)graph);

      for(Graph graph : allGraphs){
         if (graph instanceof SeriesSelector){
            SeriesSelector ss = (SeriesSelector)graph;
            ss.setGraphs(allGraphs);
            ss.setData(data);
            ss.setGraphScrollPane(gsp);
         }
      }

      // create the event bars
      ArrayList<EventBar> eventBars = new ArrayList<EventBar>();
      JPanel ebp = new JPanel(new GridFillLayout(labelFiles.size(), 1));
      if (!labelFiles.isEmpty()){
         ConfigHelper chelp = new ConfigHelper(new File("."));
         for(int i = 0; i < labelFiles.size(); i++){
            // tabs.add(String.format("Labels %d", i+1), null); // TODO
            String sMarks = labelFiles.get(i);
            String sScaleMarks = scales.size() > i ? scales.get(i) : null;
            MarkupSet marks = null;
            if (sMarks != null){
               MLGeneral ml = new MLGeneral();
               if (sScaleMarks != null){
                  if (!ml.config(chelp, "scale", sScaleMarks)){
                     System.err.println("Error: failed to config label loader");
                     return false;
                  }
               }
               marks = ml.load(sMarks);
               if (marks == null){
                  System.err.println("Warning: failed to load label file");
                  System.err.printf(" (%s)\n", sMarks);
                  marks = new MarkupSet();
               }
               marks.setFile(new File(sMarks));
               System.err.printf("Loaded %d labels\n", marks.size());
            }

            EventBar eventBar = new EventBar(masterGraph, marks);
            eventBars.add(eventBar);
            for(int j = 0; j < graphc.length; j++)
               eventBar.addGraph(graphc[j]);
            eventBar.addGraphListener(gsp.getTimeLegend());
            eventBar.setScrollPane(gsp);
            ebp.add(eventBar);
         }
      }
      for(int iMark = 0; iMark < labels.size(); iMark++){
         MarkupSet marks = labels.get(iMark);
         if (marks == null) continue;
         if (!marks.isTimeBased()) marks.convertToTime();
      }
      EventBar ebDef = null;
      if (!labels.isEmpty()){
         ebDef = new EventBar(masterGraph, labels.get(0));
         eventBars.add(ebDef);
         for(int j = 0; j < graphc.length; j++)
            ebDef.addGraph(graphc[j]);
         ebDef.addGraphListener(gsp.getTimeLegend());
         ebDef.setScrollPane(gsp);
         ebp.add(ebDef);

         for(Graph graph : allGraphs)
            if (graph instanceof SeriesSelector){
               SeriesSelector ss = (SeriesSelector)graph;
               ss.setLabels(labels, ebDef);
            }
      }
      if (ebp.getComponentCount() > 0) gsp.addBorderComponent(ebp, BorderLayout.SOUTH);

      mainp.add(gsp, BorderLayout.CENTER);      
      frame.setVisible(true);
      return true;
   }

   public void close()
   {
      frame.setVisible(false);
      frame.dispose();
      System.exit(0);
   }

   public static void main(String args[])
   {
      TSView view = new TSView();
      view.create(args);
   }

}
