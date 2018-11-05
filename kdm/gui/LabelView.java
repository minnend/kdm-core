package kdm.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import java.util.*;
import kdm.data.*;
import kdm.io.*;
import kdm.util.*;

import gnu.getopt.*;

/** Visualizes sequence labels */
public class LabelView extends JMyComponent implements ActionListener, LabelEditor
{
   public static enum Display {
      Screen, Paper
   }

   public static final char CBASE = (char)('A' - 1);
   public static final String Unknown = "Unknown";

   public static final String sPopScreen = "Screen";
   public static final String sPopPaper = "Paper";
   public static final String sPopOneCol = "1 Col Legend";
   public static final String sPopTwoCol = "2 Col Legend";
   public static final String sPopCropGT = "Crop to GT";
   public static final String sPopIncNull = "Include Null";
   public static final String sPopNoGT = "None";

   /** height of each row */
   public static final int RowHeight = 32;

   /** space between rows */
   public static final int RowGap = 8;

   /** space at top/bottom of view */
   public static final int VMargin = 8;

   /** space at left/right of view */
   public static final int HMargin = 4;

   /** space between row label to vbar and then to start of row data */
   public static final int LabelGap = 4;

   /** gap between data and legend vline (both sides) */
   public static final int LegendDataGap = 4;

   /** gap between swab and name */
   public static final int LegendSwabGap = 2;

   /** size of swab square */
   public static final int LegendSwab = 16;

   /** height of legend entry */
   public static final int LegendRowHeight = 20;

   /** vspace between legend entries */
   public static final int LegendRowGap = 2;

   /** space between legend columns (if more than one) */
   public static final int LegendColGap = 14;

   protected ArrayList<MarkupSet> labels;
   protected Color[] wcolors;
   protected String[] wnames;
   protected HashMap<String, Integer> wn2i;
   protected long msStart, msEnd;
   protected Display display = Display.Screen;
   protected boolean bOneCol = false;
   protected int iGT = -1;
   protected boolean bCropGT = true;
   protected boolean bIncNull = true;

   protected Color textColor, gridColor;

   public LabelView(ArrayList<MarkupSet> labels)
   {
      init();
      setLabels(labels, this);
   }

   public LabelView(ArrayList<MarkupSet> labels, LabelEditor led)
   {
      init();
      setLabels(labels, led);
   }

   protected void init()
   {
      setOpaque(true);
      setDoubleBuffered(false);
      setDisplay(display);
   }

   public void setLabels(ArrayList<MarkupSet> labels, LabelEditor led)
   {
      this.labels = labels;

      // figure out how many classes there are, and time boundaries
      TreeSet<String> classes = new TreeSet<String>();
      for(MarkupSet marks : labels){
         for(int i = 0; i < marks.size(); i++){
            TimeMarker tm = marks.get(i);
            String sLabel = tm.getTag();
            if (!led.keepLabel(sLabel)){
               marks.remove(i--);
               continue;
            }
            sLabel = led.adjustLabel(sLabel);
            tm.setTag(sLabel);
            classes.add(sLabel);
         }
      }
      setGroundTruth(iGT);
      // System.err.printf("[%s] -> [%s] (%s)\n", Library.formatTime(msStart), Library.formatTime(msEnd),
      // Library.formatDuration(msEnd - msStart));

      int nClasses = classes.size();
      wnames = new String[nClasses];
      wn2i = new HashMap<String, Integer>();
      Iterator<String> it = classes.iterator();
      for(int i = 0; i < nClasses; i++){
         wnames[i] = it.next();
         wn2i.put(wnames[i], i);
      }

      updateColors();
      repaint();
   }

   public void updateColors()
   {
      if (wnames == null) return;
      int nClasses = wnames.length;
      if (wn2i.containsKey(Unknown)){
         Color[] a = Library.generateColors(nClasses - 1);
         wcolors = new Color[nClasses];
         int iuk = wn2i.get(Unknown);
         for(int i = 0; i < nClasses; i++){
            if (i < iuk) wcolors[i] = a[i];
            else if (i == iuk) wcolors[i] = getBackground();
            else wcolors[i] = a[i - 1];
         }
      }
      else wcolors = Library.generateColors(nClasses);

   }

   public String adjustLabel(String sLabel)
   {
      return sLabel;
   }

   public boolean keepLabel(String sLabel)
   {
      return true;
   }

   public void setGroundTruth(int iGT)
   {
      if (this.iGT >= 0) labels.get(this.iGT).setName(String.format("%c", CBASE + this.iGT));
      if (iGT >= 0) labels.get(iGT).setName("GT");

      this.iGT = iGT;
      msStart = Long.MAX_VALUE;
      msEnd = Long.MIN_VALUE;
      int nLabels = labels.size();
      for(int il = 0; il < nLabels; il++){
         MarkupSet marks = labels.get(il);
         if (bCropGT && iGT >= 0 && il != iGT) continue;
         for(int i = 0; i < marks.size(); i++){
            TimeMarker tm = marks.get(i);
            if (tm.getStart() < msStart) msStart = tm.getStart();
            if (tm.getStop() > msEnd) msEnd = tm.getStop();
         }
      }
      repaint();
   }

   public void setCropToGT(boolean bCropGT)
   {
      this.bCropGT = bCropGT;
      setGroundTruth(iGT);
   }

   public void setDisplay(Display display)
   {
      this.display = display;
      if (display == Display.Screen){
         setBackground(Color.black);
         textColor = Color.lightGray;
         gridColor = Color.gray;
      }
      else if (display == Display.Paper){
         setBackground(Color.white);
         textColor = Color.black;
         gridColor = Color.darkGray;
      }
      updateColors();
      repaint();
   }

   public void drawString(Graphics2D g, String s, int x, int y)
   {
      Library.setAntiAlias(g, true);
      g.drawString(s, x, y);
      Library.setAntiAlias(g, false);
   }

   @Override
   public void paintComponent(Graphics2D g, int cw, int ch)
   {
      g.setColor(getBackground());
      g.fillRect(0, 0, cw, ch);

      long msDur = msEnd - msStart;
      int nSets = labels.size();
      int legendHalf = (wnames.length + 1) / 2;
      Font font = new Font("SanSerif", Font.BOLD, 14);
      g.setFont(font);
      FontMetrics fm = g.getFontMetrics();

      // figure out how much room is needed for labels
      int maxLabelLen = 0;
      for(int i = 0; i < nSets; i++){
         MarkupSet marks = labels.get(i);
         int w = fm.stringWidth(marks.getName());
         if (w > maxLabelLen) maxLabelLen = w;
      }

      // figure out how much room is needed for legend
      int maxClassLen1 = 0;
      int maxClassLen2 = 0;
      for(int i = 0; i < wnames.length; i++){
         int w = fm.stringWidth(wnames[i]);
         if (i < legendHalf) maxClassLen1 = Math.max(w, maxClassLen1);
         else maxClassLen2 = Math.max(w, maxClassLen2);
      }
      int maxClassLen = Math.max(maxClassLen1, maxClassLen2);

      // calc width stats
      int wLabel = maxLabelLen + LabelGap * 2 + 1;
      int wLegend1 = 0, wLegend2 = 0;
      int wLegend = maxClassLen + LegendDataGap * 2 + LegendSwab + LegendSwabGap + 1;
      int nLegendRows = wnames.length;
      if (!bOneCol){
         wLegend1 = maxClassLen1 + LegendDataGap * 2 + LegendSwab + LegendSwabGap + 1;
         wLegend2 = maxClassLen2 + LegendDataGap + LegendSwab + LegendSwabGap;
         wLegend = wLegend1 + wLegend2 + LegendColGap;
         nLegendRows = (wnames.length + 1) / 2;
      }
      int wData = cw - 2 * HMargin - wLabel - wLegend;
      int xData = HMargin + wLabel;
      int yBotLabel = VMargin + nSets * (RowHeight + RowGap);
      int yBotLegend = VMargin + nLegendRows * (LegendRowHeight + LegendRowGap);

      // draw the legend
      int x = cw - HMargin - wLegend + LegendDataGap;
      g.setColor(gridColor);
      g.drawLine(x, VMargin, x, Math.max(yBotLegend, yBotLabel));
      x += 1 + LegendDataGap;
      for(int i = 0; i < wnames.length; i++){
         int yTop = VMargin + i * (LegendRowHeight + LegendRowGap);
         if (!bOneCol && i >= legendHalf){
            yTop = VMargin + (i - legendHalf) * (LegendRowHeight + LegendRowGap);
            if (i == legendHalf) x += wLegend1;
         }
         int hswab2 = LegendSwab / 2;
         int h2 = LegendRowHeight / 2;
         g.setColor(wcolors[i]);
         g.fillRect(x, yTop + h2 - hswab2, LegendSwab, LegendSwab);
         if (wcolors[i] == Color.black) g.setColor(Color.darkGray);
         else g.setColor(wcolors[i].darker());
         g.drawRect(x, yTop + h2 - hswab2, LegendSwab - 1, LegendSwab - 1);
         int sx = x + LegendSwab + LegendSwabGap;
         int sy = yTop + (LegendRowHeight + fm.getAscent()) / 2 - 2;
         g.setColor(textColor);
         drawString(g, wnames[i], sx, sy);
      }

      // render label separating line
      g.setColor(gridColor);
      g.drawLine(xData - LabelGap - 1, VMargin, xData - LabelGap - 1, yBotLabel);

      // draw each row
      for(int iSet = 0; iSet < nSets; iSet++){
         MarkupSet marks = labels.get(iSet);
         int yTop = VMargin + iSet * (RowHeight + RowGap);

         // draw separating hbar
         if (iSet + 1 < nSets){
            int y = yTop + RowHeight + RowGap / 2;
            g.setColor(gridColor);
            g.drawLine(HMargin, y, cw - (HMargin + wLegend - LegendDataGap), y);
         }

         // draw the row label
         int sw = fm.stringWidth(marks.getName());
         int sy = yTop + (RowHeight + fm.getAscent()) / 2 - 2;
         g.setColor(textColor);
         drawString(g, marks.getName(), HMargin + maxLabelLen - sw, sy);

         // draw label data
         double xpms = (double)(wData) / msDur;
         int nMarks = marks.size();
         for(int iMark = 0; iMark < nMarks; iMark++){
            TimeMarker tm = marks.get(iMark);
            long msLabelStart = tm.getStart();
            long msLabelStop = tm.getStop();
            if (msLabelStart >= msEnd || msLabelStop < msStart) continue;
            msLabelStart = Math.max(msLabelStart, msStart);
            msLabelStop = Math.min(msLabelStop, msEnd);

            ArrayList<Range> ranges = new ArrayList<Range>();
            if (iGT >= 0 && iSet != iGT && !bIncNull){
               ArrayList<TimeMarker> list = tm.intersect(labels.get(iGT));
               if (list.isEmpty()) continue;
               for(TimeMarker tmi : list){
                  int a = (int)Math.round((tmi.getStart() - msStart) * xpms);
                  int b = (int)Math.round((tmi.getStop() - msStart) * xpms);
                  ranges.add(new Range(a, b));
               }
            }
            else{
               int a = (int)Math.round((msLabelStart - msStart) * xpms);
               int b = (int)Math.round((msLabelStop - msStart) * xpms);
               ranges.add(new Range(a, b));
            }

            for(Range r : ranges){
               String sLabel = tm.getTag();
               int ix = wn2i.get(sLabel).intValue();
               g.setColor(wcolors[ix]);
               int x1 = xData + r.a;
               int x2 = xData + r.b;
               g.fillRect(x1, yTop, x2 - x1, RowHeight);

               if (wcolors[ix] != getBackground()){
                  g.setColor(wcolors[ix].darker());
                  g.drawRect(x1, yTop, x2 - x1 - 1, RowHeight - 1);
               }
            }
         }
      }
   }

   @Override
   public JPopupMenu buildPopup(boolean bAppend)
   {
      JPopupMenu menu = new JPopupMenu();
      JMenuItem item;
      item = new JCheckBoxMenuItem(sPopScreen, display == Display.Screen);
      item.addActionListener(this);
      menu.add(item);
      item = new JCheckBoxMenuItem(sPopPaper, display == Display.Paper);
      item.addActionListener(this);
      menu.add(item);
      menu.addSeparator();
      item = new JCheckBoxMenuItem(sPopOneCol, bOneCol);
      item.addActionListener(this);
      menu.add(item);
      item = new JCheckBoxMenuItem(sPopTwoCol, !bOneCol);
      item.addActionListener(this);
      menu.add(item);
      menu.addSeparator();

      JMenu menuGT = new JMenu("Ground Truth");
      for(int i = 0; i < labels.size(); i++){
         item = new JCheckBoxMenuItem(String.format("Labels %c", (char)(CBASE + i)), iGT == i);
         item.addActionListener(this);
         menuGT.add(item);
      }
      menuGT.addSeparator();
      item = new JCheckBoxMenuItem(sPopNoGT, iGT < 0);
      item.addActionListener(this);
      menuGT.add(item);
      menu.add(menuGT);
      item = new JCheckBoxMenuItem(sPopCropGT, bCropGT);
      item.addActionListener(this);
      menu.add(item);
      item = new JCheckBoxMenuItem(sPopIncNull, bIncNull);
      item.addActionListener(this);
      menu.add(item);

      if (bAppend) appendPopup(menu);
      return menu;
   }

   public void actionPerformed(ActionEvent e)
   {
      String cmd = e.getActionCommand();

      if (cmd.equals(sPopScreen)){
         setDisplay(Display.Screen);
      }
      else if (cmd.equals(sPopPaper)){
         setDisplay(Display.Paper);
      }
      else if (cmd.equals(sPopOneCol)){
         bOneCol = true;
      }
      else if (cmd.equals(sPopTwoCol)){
         bOneCol = false;
      }
      else if (cmd.startsWith("Labels ")){
         setGroundTruth((int)(cmd.charAt(7) - CBASE));
      }
      else if (cmd.equals(sPopNoGT)){
         setGroundTruth(-1);
      }
      else if (cmd.equals(sPopCropGT)){
         setCropToGT(!bCropGT);
      }
      else if (cmd.equals(sPopIncNull)){
         bIncNull = !bIncNull;
      }
      repaint();
   }

   public static void usage()
   {
      System.err.println("USAGE: java ~.LabelView [Options] <label files>+");
      System.err.println(" -gt <index>");
      System.err.println(" -led <LabelEditor>");
   }

   public static void main(String[] args)
   {
      int iGT = Integer.MIN_VALUE;
      String sLed = null;

      int c;
      LongOpt[] longopts = new LongOpt[] { new LongOpt("help", LongOpt.NO_ARGUMENT, null, 'h'),
            new LongOpt("gt", LongOpt.REQUIRED_ARGUMENT, null, 1001),
            new LongOpt("led", LongOpt.REQUIRED_ARGUMENT, null, 1002) };
      Getopt g = new Getopt("LabelView", args, "?", longopts, true);
      while((c = g.getopt()) != -1){
         String sArg = g.getOptarg();
         switch(c){
         case '?':
         case 'h': // help
            usage();
            System.exit(0);
            break;
         case 1001: // gt
            iGT = Integer.parseInt(sArg);
            break;
         case 1002: // led
            sLed = sArg;
            break;
         }
      }

      ArrayList<MarkupSet> labels = new ArrayList<MarkupSet>();
      MLGeneral loader = new MLGeneral();
      int iArg0 = g.getOptind();
      for(int iArg = iArg0; iArg < args.length; iArg++){
         MarkupSet marks = loader.load(args[iArg]);
         if (marks == null) System.err.printf("Warning: failed to load label file (%s)\n", args[iArg]);
         if (marks.getName() == null) marks.setName(String.format("%c", (char)(CBASE + iArg - iArg0)));
         System.err.printf("%s -> %s\n", args[iArg], marks.getName());
         labels.add(marks);
      }
      if (labels.isEmpty()){
         System.err.printf("Error: no label files to view.\n");
         System.exit(1);
      }

      LabelEditor led = null;
      if (sLed != null){
         try{
            Class cls = Library.getClass(sLed, "kdm.io");
            led = (LabelEditor)cls.newInstance();
         } catch (Exception e){
            System.err.printf("Error: failed to load LabelEditor (%s)\n", sLed);
            System.exit(1);
         }
      }

      LabelView lview = new LabelView(labels, led);

      if (iGT != Integer.MIN_VALUE){
         if (iGT < 0) iGT += labels.size();
         lview.setGroundTruth(iGT);
      }

      JFrame frame = new JFrame("Label Viewer");
      frame.setSize(980, 300);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      JPanel mainp = new JPanel(new BorderLayout());
      frame.setContentPane(mainp);
      mainp.add(lview, BorderLayout.CENTER);
      frame.setVisible(true);
   }
}
