package kdm.gui;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;

import javax.swing.*;
import org.apache.commons.math.stat.*;
import java.util.*;
import kdm.models.*;
import kdm.util.*;

/**
 * Displays info about the stats of the given data
 */
public class SeqStatsView extends JMyComponent implements MouseMotionListener
{
   // TODO pop-up menu to select display specifics
   protected final static Stroke gaussStroke = new BasicStroke(1.25f);
   
   protected BufferedImage buf;
   protected Gaussian1D gmBase, gmMax;
   
   /** actual range of the data */
   protected double xmin, xmax, xrange;
   
   /** rendered range -- allows small padding to left and right of data */
   protected double xstart, xend, xprange;

   protected double wbin, kurt, skew;
   protected int bins[];
   protected int nBins, iMaxBin, bin_sum;
   protected ArrayList<Pair<Gaussian1D, Color>> gauss;
   protected boolean bRenderHist, bRenderGauss, bRenderExtraGauss, bAA, bShowTitle;
   protected double xMouse;
   protected String sTitle;
   protected Color cHistFill, cHistBorder, cGauss, cGaussFill;
   
   public SeqStatsView()
   {
      this(20);
   }
   
   public SeqStatsView(int nBins)
   {      
      this.nBins = nBins;
      setDoubleBuffered(true);
      setOpaque(true);      
      setBackground(Color.black);

      bRenderHist = true;
      bRenderGauss = true;
      bRenderExtraGauss = true;
      bAA = true;
      bShowTitle = true;
      
      cHistFill = Color.red;
      cHistBorder = cHistFill.darker().darker();
      cGauss = Color.green;
      cGaussFill = cGauss.darker().darker().darker();
      
      xstart = xmin = Double.NaN; 
      xMouse = Double.NaN;

      gauss = new ArrayList<Pair<Gaussian1D, Color>>();
      addMouseMotionListener(this);
   }
   
   public SeqStatsView(double[] x)
   {
      this();
      setData(x);           
   }
   
   public SeqStatsView(double[] x, int nBins)
   {
      this(nBins);
      setData(x);           
   }
   
   public SeqStatsView(int[] x)
   {
      this();
      setData(x);           
   }
   
   public SeqStatsView(int[] x, int nBins)
   {
      this(nBins);
      setData(x);           
   }
    
   public SeqStatsView(SeqStatsView ssv)
   {
      this();
      copyFrom(ssv);
   }
   
   public void copyFrom(SeqStatsView ssv)
   {
      gmBase = new Gaussian1D(ssv.gmBase);
      gmMax = new Gaussian1D(ssv.gmMax);
      xmin = ssv.xmin;
      xmax = ssv.xmax;
      xrange = ssv.xrange;
      xstart = ssv.xstart;
      xend = ssv.xend;
      xprange = ssv.xprange;
      
      cHistFill = ssv.cHistFill;
      cHistBorder = ssv.cHistBorder;
      cGauss = ssv.cGauss;
      cGaussFill = ssv.cGaussFill;
      
      wbin = ssv.wbin;
      kurt = ssv.kurt;
      skew = ssv.skew;
      bins = ssv.bins.clone();
      iMaxBin = ssv.iMaxBin;
      bin_sum = ssv.bin_sum;
      gauss = ssv.gauss;
      bRenderHist = ssv.bRenderHist;
      bRenderGauss = ssv.bRenderGauss;
      bRenderExtraGauss = ssv.bRenderExtraGauss;
      bAA = ssv.bAA; 
      bShowTitle = ssv.bShowTitle;
      sTitle = ssv.sTitle;
   }
   
   @Override
   public JPopupMenu buildPopup(boolean bAppend)
   {
      JPopupMenu menu = new JPopupMenu();
      JMenuItem mi = menu.add(sTitle != null ? sTitle : "Unknown");
      mi.setEnabled(false);
      if (bAppend) appendPopup(menu);
      return menu;
   }
   
   public void setTitle(String s)
   {
      sTitle = s;
      repaint();
   }
   
   public void showTitle(boolean b)
   {
      if (b != bShowTitle)
      {
         bShowTitle = b;
         repaint();
      }
   }
   
   public void setHistColor(Color color)
   {
      cHistFill = color;
      cHistBorder = cHistFill.darker().darker();
   }

   public void setData(double[] x)
   {      
      buf = null;
      
      if (x==null || x.length==0){
         gmBase = null;
         gmMax = null;
         bin_sum = 0;
         bins = null;
         xstart = xmin = 0;
         xend = xprange = xrange = xmax = 1;         
         repaint();
         return;
      }
      
      gmBase = new Gaussian1D(x);
      gmBase.setReport(ProbFVModel.Report.prob);
      gmMax = gmBase;
      xmin = StatUtils.min(x);
      xmax = StatUtils.max(x);

      xrange = xmax - xmin;
      xstart = xmin - 0.025 * xrange;
      xend = xmax + 0.025 * xrange;
      xprange = xend - xstart;
      
      createHist(nBins, x);
      repaint();
   }
   
   public void setData(int[] x)
   {
      double xf[] = new double[x.length];
      for(int i=0; i<x.length; i++) xf[i] = (double)x[i];
      setData(xf);
   }

   public double getMean()
   {
      return gmBase.getMean();
   }

   public double getVar()
   {
      return gmBase.getVar();
   }

   public double getKurtosis(){ return kurt; }
   public double getSkew(){ return skew; }

   
   public void addGauss(Gaussian1D gm, Color color)
   {
      gauss.add(new Pair(gm, color));
      if (gm.getVar() < gmMax.getVar()) gmMax = gm;
      repaint();
   }

   public void setRenderHist(boolean b)
   {
      bRenderHist = b;
   }

   public void setRenderGauss(boolean b)
   {
      bRenderGauss = b;
   }

   public void setRenderExtraGauss(boolean b)
   {
      bRenderExtraGauss = b;
   }

   public void setAA(boolean b)
   {
      bAA = b;
   }

   @Override
   public void paintComponent(Graphics2D g, int w, int h)
   {      
      g.setFont(Library.smallSansFont);
      FontMetrics fm = g.getFontMetrics();
      
      // TODO: should only locally double-buffer when we're doing a lot of anti-aliased lines
      if (buf == null || buf.getWidth()!=w || buf.getHeight()!=h)
      {
         buf = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
         Graphics2D gb = buf.createGraphics();
         gb.setFont(Library.smallSansFont);
         
         // prepare min/max strings         
         int stringHeight = fm.getHeight();
         String smin = PrettyPrint.printSigDig(xmin, 5, false);
         int wsmin = (int)fm.getStringBounds(smin, gb).getWidth();
         String smax = PrettyPrint.printSigDig(xmax, 5, false);
         int wsmax = (int)fm.getStringBounds(smax, gb).getWidth();
         
         // clear background
         gb.setColor(getBackground());
         gb.fillRect(0, 0, w, h-stringHeight);
         gb.setColor(Color.gray);
         gb.fillRect(0,h-stringHeight, w, stringHeight);
         
         // draw the min/max strings
         if (!Double.isNaN(xstart))
         {
            gb.setColor(Color.black);
            gb.drawString(smin, 2, h-2);
            gb.drawString(smax, w-wsmax-2, h-2);
         }
         
         if (bRenderHist) renderHist(gb, w, h-stringHeight);
         if (bRenderGauss)
         {
            boolean bFill = (bRenderExtraGauss && !gauss.isEmpty());
            renderGauss(gmBase, gb, w, h-stringHeight, bFill ? cGaussFill : cGauss, bFill);
         }
         if (bRenderExtraGauss) for(Pair<Gaussian1D, Color> gmc : gauss)
            renderGauss(gmc.first, gb, w, h-stringHeight, gmc.second, false);
         gb.dispose();
      }
      
      // copy the buffer to the screen
      g.drawImage(buf, 0, 0, null);
      
      // render transient info over the buffer
      if (bShowTitle && sTitle != null)
      {
         Rectangle2D r = fm.getStringBounds(sTitle, g);
         int rw = (int)r.getWidth();
         int rh = (int)r.getHeight();
         
         g.setColor(getBackground());
         g.fillRect(4, 2, rw+2, rh+2);
         g.setColor(Color.darkGray);
         g.drawRect(4, 2, rw+2, rh+2);
         g.setColor(Color.lightGray);
         g.drawString(sTitle, 6, rh+2);
      }
      
      if (!Double.isNaN(xMouse))
      {
         String s = PrettyPrint.printSigDig(xMouse, 6);
         Rectangle2D r = fm.getStringBounds(s, g);
         int rw = (int)r.getWidth();
         int rh = (int)r.getHeight();
         
         g.setColor(getBackground());
         g.fillRect(w-rw-6, 2, rw+2, rh+2);
         g.setColor(Color.darkGray);
         g.drawRect(w-rw-6, 2, rw+2, rh+2);
         g.setColor(Color.lightGray);
         g.drawString(s, w-rw-4, rh+2);
      }
   }

   /**
    * Build a histogram by binning the given data
    * 
    * @param nBins number of bins to use
    * @param x data to histogram
    */
   protected void createHist(int nBins, double[] x)
   {
      bins = new int[nBins];

      wbin = xrange / nBins;
      for(int i = 0; i < x.length; i++)
      {
         int iBin = 0;
         double xm = xmin + wbin;
         while(iBin + 1 < nBins && x[i] > xm)
         {
            xm += wbin;
            iBin++;
         }
         bins[iBin]++;
      }
      
      // determine iMaxbin and bin_sum
      iMaxBin = 0;      
      bin_sum = x.length;
      for(int i = 1; i < bins.length; i++)
         if (bins[i] > bins[iMaxBin]) iMaxBin = i;
   }
   
   /**
    * Build a histogram by binning the given (integer) data
    * 
    * @param x data to histogram
    */
   protected void createHist(int[] x)
   {
      int xmin = x[0];
      int xmax = x[0];
      for(int i=1; i<x.length; i++)
      {
         if (x[i] < xmin) xmin = x[i];
         if (x[i] > xmax) xmax = x[i];
      }
      int nBins = xmax - xmin + 1;
      wbin = xrange / nBins;
      bins = new int[nBins];
      for(int i = 0; i < x.length; i++) bins[x[i]-xmin]++;
      
      // determine iMaxbin and bin_sum
      iMaxBin = 0;      
      bin_sum = x.length;
      for(int i = 1; i < bins.length; i++)
         if (bins[i] > bins[iMaxBin]) iMaxBin = i;
   }

   /** @return return the max height to use; basically, use full height minus small padding at top */
   protected double getProbHeight(int h)
   {
      return Math.max(1, Math.min(h - 2, Math.round(h * .95)));
   }
   
   /**
    * Render the histogram data
    */
   protected void renderHist(Graphics2D g, int w, int h)
   {
      if (bins == null) return;
      double x2p = (double)w / xprange;
      int pstart = (int)Math.round(x2p * (xmin - xstart));
      int pend = w - (int)Math.round(x2p * (xend - xmax));
      int wdata = pend - pstart;
      double pwbin = (double)wdata / bins.length;
      int gap = (pwbin > 5) ? 1 : 0;
      double fh = getProbHeight(h) / Math.max(1, bins[iMaxBin]);
      for(int iBin = 0; iBin < bins.length; iBin++)
      {
         int a = pstart + (int)Math.round(iBin * pwbin);
         int b = pstart + (int)Math.round((iBin + 1) * pwbin);
         int hBin = (int)Math.round(fh * bins[iBin]);
         g.setColor(cHistFill);
         g.fillRect(a + gap, h - hBin, b - a - 1-gap, hBin);
         if (cHistBorder != null)
         {
            g.setColor(cHistBorder);
            g.drawRect(a + gap, h - hBin, b - a - 1-gap, hBin-1);
         }
      }
   }

   /**
    * Render the gaussian estimate
    */
   protected void renderGauss(Gaussian1D gm, Graphics2D g, int w, int h, Color color, boolean bFill)
   {
      if (gm == null) return;
      Stroke stroke = null;
      if (bAA && !bFill)
      {
         g.addRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING,
               RenderingHints.VALUE_ANTIALIAS_ON));
         stroke = g.getStroke();
         g.setStroke(gaussStroke);
      }

      double y2p; // map prob to y-position
      if (bin_sum > 0 && bRenderHist) y2p = getProbHeight(h) / ((double)bins[iMaxBin] / bin_sum) * wbin;
      else y2p = getProbHeight(h) / gmMax.eval(gmMax.getMean());

      g.setColor(color);
      int yPrev = h - (int)Math.round(gm.eval(xstart) * y2p);
      int dx = 2;
      for(int x = dx; x < w; x += dx)
      {
         double rx = xstart + (xprange * (double)x / w);
         int y = h - (int)Math.round(gm.eval(rx) * y2p);
         g.drawLine(x - dx, yPrev, x, y);
         if (bFill) g.drawLine(x, y, x, h);
         yPrev = y;
      }

      if (bAA && !bFill)
      {
         g.setStroke(stroke);
         g.addRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING,
               RenderingHints.VALUE_ANTIALIAS_DEFAULT));
      }
   }

   public Dimension getPreferredSize()
   {
      return new Dimension(100, 100);
   }

   public void mouseClicked(MouseEvent e)
   {
      if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2)
      {
         JFrame frame = new JFrame(sTitle==null?"SeqStatsView":sTitle);
         frame.setSize(800, 600);
         Library.centerWin(frame, null);
         SeqStatsView ssv = new SeqStatsView(this);
         ssv.showTitle(false);
         frame.add(ssv);
         frame.setVisible(true);
      }
   }

   public void mouseExited(MouseEvent e)
   {
      xMouse = Double.NaN;
      repaint();
   }

   public void mouseMoved(MouseEvent e)
   {      
      xMouse = xstart + (xprange * (double)e.getX() / getWidth());
      repaint();      
   }

   public void mouseDragged(MouseEvent e)
   {
      // TODO Auto-generated method stub
      
   }
}
