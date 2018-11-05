package kdm.gui;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import kdm.util.*;
import kdm.data.*;
import kdm.io.DataLoader.*;
import kdm.metrics.*;
import kdm.mlpr.dataTree.*;
import kdm.data.TimeMarker.Units;

/** Graph representing 2D data */
public class ScatterPlot extends JMyComponent implements MouseMotionListener, ActionListener,
      MouseWheelListener, KeyListener
{
   // TODO GUI interface for changing data and shape properties
   // TODO predefined color schemes (e.g., screen, print)
   // TODO axis labels
   // TODO legend
   // TODO title
   // TODO reasonable sigdigs for view
   
   public static final double zoom = 0.9;
   public static final char NO_SHAPE = (char)0;
   public static final Stroke DEF_STROKE = new BasicStroke(1.0f);
   public static final String sFixedGrid = "Fixed Grid";
   public static final String sAutoAxes = "Reset Axes";
   public static final String sCursorZoom = "Zoom to Mouse";
   public static final String sSavePlot = "Save Plot...";
   public static final String sRenderAxes = "Draw Axes";
   public static final String sRenderGrid = "Draw Grid";
   public static final String sPlotColor = "Plot Color";
   public static final String sBGColor = "Background";
   public static final String sGridColor = "Grid Color";
   public static final String sAxisColor = "Axis Color";

   /** list of data sets to display */
   protected ArrayList<DataInfo> data = new ArrayList<DataInfo>();

   /** true = fixed grid location, floating numbers; false = floating grid, fixed numbers */
   protected boolean bFixedGrid = false;

   /** true = automatically calculate axis range */
   protected boolean bAutoAxes = true;

   /** true = draw axes */
   protected boolean bRenderAxes = true;

   /** true = draw plot grid */
   protected boolean bRenderGrid = true;

   /** true = zoom in around cursor; false = zoom in to center of plot regardless of mouse location */
   protected boolean bCursorZoom = true;
   
   /** plot range for x (horizontal) and y (vertical) */
   protected double xmin, xmax, ymin, ymax;

   protected MyDoubleList gridx, gridy;

   /** background color for the plot */
   protected Color cPlotBG = Color.white;

   /** color used to draw the grid */
   protected Color cGrid = new Color(0.95f, 0.95f, 0.95f);
   
   /** color used to draw axes */
   protected Color cAxes = Color.black;

   /** default plot margins for no axes info */
   protected Insets plotDefMargin = new Insets(4, 4, 4, 4); // top, left, bottom, right
   
   /** margin around plot used for axes and visual padding */
   protected Insets plotMargin = new Insets(0,0,0,0);

   /** last location of mouse press */
   protected Point mousePressed = null;

   /** list of shapes to draw on the plot */
   protected ArrayList<AbstractShape> shapes;
   
   /** which key must be pressed to pan plot with mouse (0 for none) */
   protected int vkPanKey = 0;

   /** create an empty scatter plot */
   public ScatterPlot()
   {
      KeyState.setup();
      setFocusable(true);
      setBackground(new Color(0.8f, 0.8f, 0.8f));
      gridx = new MyDoubleList();
      gridy = new MyDoubleList();
      shapes = new ArrayList<AbstractShape>();
      addMouseMotionListener(this);
      addMouseWheelListener(this);
      addKeyListener(this);
   }
   
   /** require this key to be down when panning with the mouse (eg, KeyEvent.VK_SPACE); 0 = no key */
   public void setPanKey(int vk)
   {
      vkPanKey = vk;
   }

   /** add a data set to be rendered with lines only */
   public DataInfo addData(Sequence data, Color color)
   {
      DataInfo di = new DataInfo(data, color, DEF_STROKE, NO_SHAPE);
      this.data.add(di);
      repaint();
      return di;
   }

   /** add a data set to be rendered without lines */
   public DataInfo addData(Sequence data, Color color, char shape)
   {
      DataInfo di = new DataInfo(data, color, null, shape);
      this.data.add(di);
      repaint();
      return di;
   }

   /** add a data set to be rendered with lines only */
   public DataInfo addData(Sequence data, Color color, Stroke stroke)
   {
      DataInfo di = new DataInfo(data, color, stroke, NO_SHAPE);
      this.data.add(di);
      repaint();
      return di;
   }

   /** add a data set to be rendered (use null stroke for no lines, NO_SHAPE for lines only) */
   public DataInfo addData(Sequence data, Color color, Stroke stroke, char shape)
   {
      DataInfo di = new DataInfo(data, color, stroke, shape);
      this.data.add(di);
      repaint();
      return di;
   }

   /** set the axes to the specified values */
   public void setAxes(double xmin, double xmax, double ymin, double ymax)
   {
      bAutoAxes = false;
      this.xmin = xmin;
      this.xmax = xmax;
      this.ymin = ymin;
      this.ymax = ymax;
   }

   /** set whether the grid is fixed or floats with the data during panning */
   public void setFixedGrid(boolean bFixed)
   {
      if (bFixed == bFixedGrid) return;
      bFixedGrid = bFixed;
      calcGrid();
      repaint();
   }
   
   /** set whether zooming is centered on graph or around the mouse cursor */
   public void setCursorZoom(boolean bCursorZoom)
   {
      this.bCursorZoom = bCursorZoom;
   }

   /** set whether or not axis labels are automatically determined */
   public void setAutoAxes(boolean bAuto)
   {
      if (bAuto == bAutoAxes) return;
      bAutoAxes = bAuto;
      if (bAuto) repaint();
   }

   /** set whether or not axes should be drawn */
   public void showAxes(boolean bShow)
   {
      if (bShow == bRenderAxes) return;
      bRenderAxes = bShow;
      repaint();
   }

   /** set whether or not grid should be drawn */
   public void showGrid(boolean bShow)
   {
      if (bShow == bRenderGrid) return;
      bRenderGrid = bShow;
      repaint();
   }

   /** set the background color for the plot (use setBackground() for the exterior) */
   public void setPlotBGColor(Color c)
   {
      cPlotBG = c;
      repaint();
   }

   /** set color for the grid lines */
   public void setGridColor(Color c)
   {
      cGrid = c;
      repaint();
   }
   
   /** set color for the axes */
   public void setAxisColor(Color c)
   {
      cAxes = c;
      repaint();
   }

   @Override
   public void paintComponent(Graphics2D g, int cw, int ch)
   {
      // prepare for rendering
      if (bAutoAxes) calcAxes();
      calcGrid();
      
      // figure out plot margins
      plotMargin.left = plotDefMargin.left;
      plotMargin.bottom = plotDefMargin.bottom;
      plotMargin.top= plotDefMargin.top;
      plotMargin.right = plotDefMargin.right;

      if (bRenderAxes){
         g.setFont(Library.smallSansFont);
         FontMetrics fm = g.getFontMetrics();
         int sh = fm.getHeight();
         int sw = getMaxYLabelWidth(g);
         plotMargin.left += sw + 6;
         plotMargin.bottom += sh + 4;
         plotMargin.top += (sh+1)/2;
      }
      
      int wPlot = getPlotWidth();
      int hPlot = getPlotHeight();
      
      // clear the plot area
      g.setColor(cPlotBG);
      g.fillRect(plotMargin.left, plotMargin.top, wPlot, hPlot);

      // draw the plot
      g.setClip(plotMargin.left, plotMargin.top, wPlot, hPlot);
      if (bRenderGrid) renderGrid(g, plotMargin.left, plotMargin.top, wPlot, hPlot);
      renderShapes(g, plotMargin.left, plotMargin.top, wPlot, hPlot);
      renderData(g, plotMargin.left, plotMargin.top, wPlot, hPlot);
      
      // clear the background (exterior) -- we do this after rendering the plot because
      // some SVG renderers don't support clipping regions properly
      g.setClip(0, 0, cw, ch);
      g.setColor(getBackground());
      g.fillRect(0, 0, cw, plotMargin.top);
      g.fillRect(0, plotMargin.top, plotMargin.left, hPlot);
      g.fillRect(cw - plotMargin.right, plotMargin.top, plotMargin.right, hPlot);
      g.fillRect(0, ch - plotMargin.bottom, cw, plotMargin.bottom);
      
      // render axes last
      if (bRenderAxes) renderAxes(g, plotMargin.left, plotMargin.top, wPlot, hPlot);
      else{
         g.setColor(Color.darkGray);
         g.drawRect(plotMargin.left, plotMargin.top, wPlot, hPlot);
      }
   }

   protected void renderShapes(Graphics2D g, int cx, int cy, int cw, int ch)
   {
      Library.setAntiAlias(g, true);
      for(AbstractShape shape : shapes)
         shape.draw(g, cx, cy, cw, ch);
      Library.setAntiAlias(g, false);
   }

   protected void renderData(Graphics2D g, int cx, int cy, int cw, int ch)
   {
      Point p0 = new Point();
      Point p = new Point();
      for(DataInfo di : data){
         // render lines
         if (di.stroke != null){
            Library.setAntiAlias(g, true);
            int n = di.data.length();
            g.setColor(di.color);
            g.setStroke(di.stroke);
            transform(di.data.get(0), p0, cx, cy, cw, ch);
            for(int i = 1; i < n; i++){
               transform(di.data.get(i), p, cx, cy, cw, ch);
               g.drawLine(p0.x, p0.y, p.x, p.y);
               p0.x = p.x;
               p0.y = p.y;
            }
            Library.setAntiAlias(g, false);
         }

         // render shapes
         if (di.shape != NO_SHAPE){
            Library.setAntiAlias(g, "*+os".indexOf(di.shape) < 0);
            int n = di.data.length();
            for(int i = 0; i < n; i++){
               transform(di.data.get(i), p, cx, cy, cw, ch);
               renderPoint(g, p.x, p.y, di.color, di.shape, 2);
            }
         }
      }
      Library.setAntiAlias(g, false);
   }

   /** render a particular shape: .ox+*sdv^<> */
   protected void renderPoint(Graphics2D g, int x, int y, Color color, char shape, int r)
   {
      int d = 2 * r + 1;
      g.setColor(color);
      switch(shape){
      case '.': // point
         g.fillOval(x - r, y - r, d, d);
         break;
      case 'o': // circle
         g.drawOval(x - r, y - r, d, d);
         break;
      case 'x': // x-mark
         g.drawLine(x - r, y - r, x + r, y + r);
         g.drawLine(x - r, y + r, x + r, y - r);
         break;
      case '+': // plus
         g.drawLine(x, y - r, x, y + r);
         g.drawLine(x - r, y, x + r, y);
         break;
      case '*': // star
         g.drawLine(x - r, y - r, x + r, y + r);
         g.drawLine(x - r, y + r, x + r, y - r);
         g.drawLine(x, y - r, x, y + r);
         g.drawLine(x - r, y, x + r, y);
         break;
      case 's': // square
         g.drawRect(x - r, y - r, d, d);
         break;
      case 'd': // diamond
         g.drawLine(x, y - r, x - r, y);
         g.drawLine(x - r, y, x, y + r);
         g.drawLine(x, y + r, x + r, y);
         g.drawLine(x + r, y, x, y - r);
         break;
      case 'v': // triangle (down)
         g.drawLine(x, y + r, x + r, y - r);
         g.drawLine(x, y + r, x - r, y - r);
         g.drawLine(x - r, y - r, x + r, y - r);
         break;
      case '^': // triangle (up)
         g.drawLine(x, y - r, x + r, y + r);
         g.drawLine(x, y - r, x - r, y + r);
         g.drawLine(x - r, y + r, x + r, y + r);
         break;
      case '<': // triangle (left)
         g.drawLine(x - r, y, x + r, y - r);
         g.drawLine(x - r, y, x + r, y + r);
         g.drawLine(x + r, y - r, x + r, y + r);
         break;
      case '>': // triangle (right)
         g.drawLine(x + r, y, x - r, y - r);
         g.drawLine(x + r, y, x - r, y + r);
         g.drawLine(x - r, y - r, x - r, y + r);
         break;
      }
   }

   protected Point transform(FeatureVec v, int cx, int cy, int cw, int ch)
   {
      return transform(v, new Point(), cx, cy, cw, ch);
   }

   protected Point transform(FeatureVec v, Point p, int cx, int cy, int cw, int ch)
   {
      p.x = transformx(v.get(0), cx, cw);
      p.y = transformy(v.get(1), cy, ch);
      return p;
   }

   protected int transformx(double x, int cx, int cw)
   {
      double dx = (double)cw / (xmax - xmin);
      return cx + (int)Math.round((x - xmin) * dx);
   }

   protected double untransformx(int x, int cx, int cw)
   {
      double dx = (double)cw / (xmax - xmin);
      return (x - cx) / dx + xmin;
   }

   protected int transformy(double y, int cy, int ch)
   {
      double dy = (double)ch / (ymax - ymin);
      return cy + ch - (int)Math.round((y - ymin) * dy);
   }

   protected double untransformy(int y, int cy, int ch)
   {
      double dy = (double)ch / (ymax - ymin);
      return (cy + ch - y) / dy + ymin;
   }

   protected void calcGrid()
   {
      // calc x ticks
      gridx.clear();
      double dxgrid = Library.calcSmallerPow((xmax - xmin) / 2, 10);
      if ((xmax - xmin) / dxgrid < 6) dxgrid /= 2;
      double tick = xmin;
      if (!bFixedGrid) tick = dxgrid * Math.ceil(tick / dxgrid);
      while(tick <= xmax + 1e-9){
         gridx.add(tick);
         tick += dxgrid;
      }

      // calc y ticks
      gridy.clear();
      double dygrid = Library.calcSmallerPow((ymax - ymin) / 2, 10);
      if ((ymax - ymin) / dygrid < 6) dygrid /= 2;
      tick = ymin;
      if (!bFixedGrid) tick = dygrid * Math.ceil(tick / dygrid);
      while(tick <= ymax + 1e-9){
         gridy.add(tick);
         tick += dygrid;
      }
   }
   
   protected int getMaxYLabelWidth(Graphics2D g)
   {
      if (!bRenderAxes) return 0;
            
      FontMetrics fm = g.getFontMetrics();
      int ngrid = gridy.size();
      int wmax = 0;
      for(int i = 0; i < ngrid; i++){
         double tick = gridy.get(i);
         String s = PrettyPrint.printSigDig(tick, 3, true);
         int sw = fm.stringWidth(s);
         if (sw > wmax) wmax = sw;
      }
      return wmax;
   }

   protected void renderAxes(Graphics2D g, int cx, int cy, int cw, int ch)
   {
      // setup for drawing labels      
      FontMetrics fm = g.getFontMetrics();
      int fh = fm.getHeight();
      g.setColor(cAxes);

      // draw x ticks
      int ngrid = gridx.size();
      for(int i = 0; i < ngrid; i++){
         double tick = gridx.get(i);
         int x = transformx(tick, cx, cw);
         g.drawLine(x, cy + ch, x, cy + ch + 6);
         String s = PrettyPrint.printSigDig(tick, 3, true);
         int sw = (tick < 0) ? fm.stringWidth(s.substring(1)) + 2 * fm.stringWidth("-") : fm
               .stringWidth(s);
         g.drawString(s, x - sw / 2, cy + ch + 4 + fh);
      }

      // draw y ticks
      ngrid = gridy.size();
      for(int i = 0; i < ngrid; i++){
         double tick = gridy.get(i);
         int y = transformy(tick, cy, ch);
         g.drawLine(cx - 6, y, cx, y);
         String s = PrettyPrint.printSigDig(tick, 3, true);
         int sw = fm.stringWidth(s);
         g.drawString(s, cx - sw - 8, y + fh / 2 - 3);
      }

      // draw bounding rect
      g.setColor(Color.gray);
      g.drawLine(cx, cy, cx + cw, cy);
      g.drawLine(cx + cw, cy, cx + cw, cy + ch);
      g.setColor(Color.black);
      g.drawLine(cx, cy, cx, cy + ch);
      g.drawLine(cx, cy + ch, cx + cw, cy + ch);
   }
   
   protected void renderGrid(Graphics2D g, int cx, int cy, int cw, int ch)
   {      
      g.setColor(cGrid);

      // draw x ticks
      int ngrid = gridx.size();
      for(int i = 0; i < ngrid; i++){
         double tick = gridx.get(i);
         int x = transformx(tick, cx, cw);
         g.drawLine(x, cy, x, cy + ch);         
      }

      // draw y ticks
      ngrid = gridy.size();
      for(int i = 0; i < ngrid; i++){
         double tick = gridy.get(i);
         int y = transformy(tick, cy, ch);
         g.drawLine(cx, y, cx + cw, y);
      }
   }

   protected void calcAxes()
   {
      if (data.isEmpty()){
         xmin = ymin = 0;
         xmax = ymax = 1;
         return;
      }

      // we have data, so calc the actual bounds
      xmin = ymin = Library.INF;
      xmax = ymax = Library.NEGINF;
      int n = 0;
      for(DataInfo di : data){
         FeatureVec fv = di.data.getMin();
         xmin = Math.min(xmin, fv.get(0));
         ymin = Math.min(ymin, fv.get(1));

         fv = di.data.getMax();
         xmax = Math.max(xmax, fv.get(0));
         ymax = Math.max(ymax, fv.get(1));
         n += di.data.length();
      }

      if (n == 1){
         xmin -= 0.5;
         xmax += 0.5;
         ymin -= 0.5;
         ymax += 0.5;
      }

      // give the axes a little buffer room
      double r = (n == 1 ? 1.0 : (xmax - xmin) * 0.01);
      xmin -= r / 2;
      xmax += r / 2;

      r = (n == 1 ? 1.0 : (ymax - ymin) * 0.01);
      ymin -= r / 2;
      ymax += r / 2;

      makePrettyAxes();
   }

   protected void makePrettyAxes()
   {
      double r = xmax - xmin;      
      xmin = Library.getPrettyNumberSmaller(xmin, r/9, 10);
      xmax = Library.getPrettyNumberLarger(xmax, r/9, 10);
      
      r = ymax - ymin;
      ymin = Library.getPrettyNumberSmaller(ymin, r/2, 10);
      ymax = Library.getPrettyNumberLarger(ymax, r/2, 10);
   }

   /** add a line to the plot */
   public Line addLine(double x1, double y1, double x2, double y2, Color color)
   {
      Line line = new Line(x1, y1, x2, y2, color);
      shapes.add(line);
      repaint();
      return line;
   }

   /** add an axis aligned rectangle (can apply affine transform later) */
   public Rectangle addRectangle(double x, double y, double w, double h, Color cBorder, Color cFill)
   {
      Rectangle rect = new Rectangle(x, y, w, h, cBorder, cFill);
      shapes.add(rect);
      repaint();
      return rect;
   }

   /** add an axis aligned ellipse (can apply affine transform later) */
   public Ellipse addEllipse(double x, double y, double rx, double ry, Color cBorder, Color cFill)
   {
      Ellipse ellipse = new Ellipse(x, y, rx, ry, cBorder, cFill);
      shapes.add(ellipse);
      repaint();
      return ellipse;
   }

   /** add a Gaussian represented by its one standard deviation isocontour */
   public Ellipse addGauss(FeatureVec mean, double[] cov, Color cBorder, Color cFill)
   {
      Ellipse ellipse = new Ellipse(mean.get(0), mean.get(1), 1, 1, cBorder, cFill);
      ellipse.setAffine(cov);
      shapes.add(ellipse);
      repaint();
      return ellipse;
   }

   /** add a circle to the plot */
   public Ellipse addCircle(double x, double y, double radius, Color cBorder, Color cFill)
   {
      Ellipse ellipse = new Ellipse(x, y, radius, radius, cBorder, cFill);
      shapes.add(ellipse);
      repaint();
      return ellipse;
   }

   /** stores information about each visualized data set */
   class DataInfo
   {
      public Sequence data;

      public Color color;

      public Stroke stroke;

      public char shape;

      public DataInfo(Sequence data, Color color, Stroke stroke, char shape)
      {
         if (data.getNumDims() == 1){
            // augment 1D data to include index
            Sequence data1 = data;
            data = new Sequence(data1.getName());
            int T = data1.length();
            for(int t=0; t<T; t++)
               data.add(new FeatureVec(2, t, data1.get(t, 0)));
         }
         assert (data.getNumDims() == 2) : String.format("ScatterPlot only handles 2D data (input: %dD)",
               data.getNumDims());
         this.data = data;
         this.color = color;
         this.shape = shape;
         this.stroke = stroke;
      }

   }

   /** abstract base class of all shapes */
   abstract class AbstractShape
   {
      public Color cBorder, cFill;
      
      public Stroke stroke = ScatterPlot.DEF_STROKE;

      public double[] affine;

      public abstract void draw(Graphics2D g, int cx, int cy, int cw, int ch);

      public void setAffine(double a, double b, double c, double d)
      {
         if (affine == null) affine = new double[4];
         affine[0] = a;
         affine[1] = b;
         affine[2] = c;
         affine[3] = d;
      }
      
      public void setStroke(Stroke stroke)
      {
         this.stroke = stroke;
      }

      public void setAffine(double affine[])
      {
         assert (affine.length == 4) : String.format(
               "affine transform must be 2x2 matrix => 4 elements (not %d)", affine.length);
         this.affine = affine;
      }

      public double applyAffineX(double x, double y)
      {
         return affine[0] * x + affine[1] * y;
      }

      public double applyAffineY(double x, double y)
      {
         return affine[2] * x + affine[3] * y;
      }

      protected void draw(Graphics2D g, double mx, double my, double[] xp, double[] yp, int cx, int cy,
            int cw, int ch)
      {
         int N = xp.length;
         int[] ap = new int[N];
         int[] bp = new int[N];

         // apply affine transformation
         if (affine != null){
            double a = affine[0];
            double b = affine[1];
            double c = affine[2];
            double d = affine[3];
            for(int i = 0; i < N; i++){
               double x = xp[i];
               double y = yp[i];
               xp[i] = a * x + b * y;
               yp[i] = c * x + d * y;
            }
         }

         for(int i = 0; i < N; i++){
            ap[i] = transformx(xp[i] + mx, cx, cw);
            bp[i] = transformy(yp[i] + my, cy, ch);
         }

         if (cFill != null){
            if (cBorder != null) Library.setAntiAlias(g, false);
            g.setColor(cFill);
            g.fillPolygon(ap, bp, N);
            if (cBorder != null) Library.setAntiAlias(g, true);

         }
         if (cBorder != null){
            g.setColor(cBorder);
            g.setStroke(stroke);
            g.drawPolygon(ap, bp, N);
         }
      }
   }

   /** stores information about a line to be rendered */
   public class Line extends AbstractShape
   {
      public double x1, y1, x2, y2;

      public Line(double x1, double y1, double x2, double y2, Color color)
      {
         this.x1 = x1;
         this.y1 = y1;
         this.x2 = x2;
         this.y2 = y2;
         this.cBorder = color;
         this.cFill = null;
      }

      public void draw(Graphics2D g, int cx, int cy, int cw, int ch)
      {
         double p1, q1, p2, q2;
         if (affine != null){
            double xc = (x1 + x2) / 2;
            double yc = (y1 + y2) / 2;
            p1 = applyAffineX(x1 - xc, y1 - yc) + xc;
            q1 = applyAffineY(x1 - xc, y1 - yc) + yc;
            p2 = applyAffineX(x2 - xc, y2 - yc) + xc;
            q2 = applyAffineY(x2 - xc, y2 - yc) + yc;
         }
         else{
            p1 = x1;
            q1 = y1;
            p2 = x2;
            q2 = y2;
         }
         int a = transformx(p1, cx, cw);
         int b = transformy(q1, cy, ch);
         int c = transformx(p2, cx, cw);
         int d = transformy(q2, cy, ch);
         g.setColor(cBorder);
         g.setStroke(stroke);
         g.drawLine(a, b, c, d);
      }
   }

   /** stores information about a line to be rendered */
   public class Rectangle extends AbstractShape
   {
      public double x, y, w, h;

      public Rectangle(double x, double y, double w, double h, Color cBorder, Color cFill)
      {
         this.x = x;
         this.y = y;
         this.w = w;
         this.h = h;
         this.cBorder = cBorder;
         this.cFill = cFill;
      }

      public void draw(Graphics2D g, int cx, int cy, int cw, int ch)
      {
         double[] xp = new double[4];
         double[] yp = new double[4];

         double w2 = w / 2;
         double h2 = h / 2;

         xp[0] = xp[1] = -w2;
         xp[2] = xp[3] = w2;
         yp[0] = yp[3] = -h2;
         yp[1] = yp[2] = h2;

         draw(g, x + w2, y + h2, xp, yp, cx, cy, cw, ch);
      }
   }

   /** stores information about an ellipse to be rendered */
   public class Ellipse extends AbstractShape
   {
      public double x, y, rx, ry;

      public Ellipse(double x, double y, double rx, double ry, Color cBorder, Color cFill)
      {
         this.x = x;
         this.y = y;
         this.rx = rx;
         this.ry = ry;
         this.cBorder = cBorder;
         this.cFill = cFill;
      }

      @Override
      public void draw(Graphics2D g, int cx, int cy, int cw, int ch)
      {
         int N = 72;
         double[] xp = new double[N];
         double[] yp = new double[N];

         // generate ellipse: rotate [1;0] through 360 degrees and scale
         for(int i = 0; i < N; i++){
            double theta = Library.TWO_PI * i / N;
            double s = Math.sin(theta); // rotation matrix: [c, -s, s, c]
            double c = Math.cos(theta);
            xp[i] = c * rx;
            yp[i] = s * ry;
         }

         draw(g, x, y, xp, yp, cx, cy, cw, ch);
      }
   }

   @Override
   public JPopupMenu buildPopup(boolean bAppend)
   {
      JPopupMenu menu = new JPopupMenu();
      JMenuItem item;

      item = new JMenuItem(sAutoAxes);
      if (bAutoAxes) item.setEnabled(false);
      else item.addActionListener(this);
      menu.add(item);
      item = new JMenuItem(sSavePlot);
      item.addActionListener(this);
      menu.add(item);
      item = new JCheckBoxMenuItem(sCursorZoom, bCursorZoom);
      item.addActionListener(this);
      menu.add(item);
      menu.addSeparator();
      
      JMenu sub = new JMenu("Draw");
      menu.add(sub);
      item = new JCheckBoxMenuItem(sRenderAxes, bRenderAxes);
      item.addActionListener(this);
      sub.add(item);
      item = new JCheckBoxMenuItem(sRenderGrid, bRenderGrid);
      item.addActionListener(this);
      sub.add(item);
      item = new JCheckBoxMenuItem(sFixedGrid, bFixedGrid);
      item.addActionListener(this);
      sub.add(item);
      
      sub = new JMenu("Color");
      menu.add(sub);
      item = new JMenuItem(sPlotColor);
      item.addActionListener(this);
      sub.add(item);      
      item = new JMenuItem(sGridColor);
      item.addActionListener(this);
      sub.add(item);
      item = new JMenuItem(sAxisColor);
      item.addActionListener(this);
      sub.add(item);
      item = new JMenuItem(sBGColor);
      item.addActionListener(this);
      sub.add(item);

      if (bAppend) appendPopup(menu);
      return menu;
   }

   @Override
   public void mousePressed(MouseEvent e)
   {
      super.mousePressed(e);
      if (SwingUtilities.isLeftMouseButton(e)){
         mousePressed = e.getPoint();
      }
   }

   public void mouseDragged(MouseEvent e)
   {
      if (SwingUtilities.isLeftMouseButton(e) && (vkPanKey==0 || KeyState.getKeyState(vkPanKey))){
         Point p = e.getPoint();
         int cw = getPlotWidth();
         int ch = getPlotHeight();
         double x1 = untransformx(mousePressed.x, plotMargin.left, cw);
         double x2 = untransformx(p.x, plotMargin.left, cw);
         double dx = x1 - x2;
         double y1 = untransformy(mousePressed.y, plotMargin.top, ch);
         double y2 = untransformy(p.y, plotMargin.top, ch);
         double dy = y1 - y2;
         bAutoAxes = false;
         xmin += dx;
         xmax += dx;
         ymin += dy;
         ymax += dy;
         calcGrid();
         repaint();
         mousePressed = p;
         e.consume();
      }
   }

   public void mouseMoved(MouseEvent e)
   {}

   public void actionPerformed(ActionEvent e)
   {
      String cmd = e.getActionCommand();
      if (cmd == null) return;

      if (cmd.equals(sFixedGrid)){
         setFixedGrid(!bFixedGrid);
      }
      else if (cmd.equals(sAutoAxes)){
         setAutoAxes(!bAutoAxes);
      }
      else if (cmd.equals(sRenderAxes)){
         showAxes(!bRenderAxes);
      }
      else if (cmd.equals(sRenderGrid)){
         showGrid(!bRenderGrid);
      }
      else if (cmd.equals(sPlotColor)){
         Color color = JColorChooser.showDialog(this, "Choose Plot Color", cPlotBG);
         if (color != null) setPlotBGColor(color);
         repaint();
      }
      else if (cmd.equals(sBGColor)){
         Color color = JColorChooser.showDialog(this, "Choose Exterior Color", getBackground());
         if (color != null) setBackground(color);
         repaint();
      }
      else if (cmd.equals(sGridColor)){
         Color color = JColorChooser.showDialog(this, "Choose Grid Color", cGrid);
         if (color != null) setGridColor(color);
         repaint();
      }
      else if (cmd.equals(sAxisColor)){
         Color color = JColorChooser.showDialog(this, "Choose Axis Color", cAxes);
         if (color != null) setAxisColor(color);
         repaint();
      }
      else if (cmd.equals(sSavePlot)){
         SaveCompImage sci = new SaveCompImage(SwingUtilities.getWindowAncestor(this), this);
         sci.setVisible(true);
         repaint();
      }
      else if (cmd.equals(sCursorZoom)){
         setCursorZoom(!bCursorZoom);
      }
   }

   /** @return width in pixels of the actual plot */ 
   public int getPlotWidth()
   {
      return getWidth() - (plotMargin.left + plotMargin.right);
   }

   /** @return height in pixels of the actual plot */ 
   public int getPlotHeight()
   {
      return getHeight() - (plotMargin.top + plotMargin.bottom);
   }

   @Override
   public void mouseClicked(MouseEvent e)
   {
      Object src = e.getSource();
      if (src == this && SwingUtilities.isLeftMouseButton(e) && e.getClickCount()==2){
         int mousex = e.getX();
         int mousey = e.getY();
         int cw = getPlotWidth();
         int ch = getPlotHeight();         
         double mvx = untransformx(mousex, plotMargin.left, cw);
         double mvy = untransformy(mousey, plotMargin.top, ch);
         double r = (xmax-xmin)/2;
         xmin = mvx-r;
         xmax = mvx+r;
         r = (ymax-ymin)/2;
         ymin = mvy - r;
         ymax = mvy+r;
         calcGrid();
         repaint();
      }
   }
   
   public void mouseWheelMoved(MouseWheelEvent e)
   {
      if (e.getWheelRotation() < 0) zoomOut(false, e.getPoint());
      else zoomIn(bCursorZoom, e.getPoint());      
   }

   public void zoomOut(boolean bCursorZoom, Point p)
   {
      bAutoAxes = false;
            
      int cw = getPlotWidth();
      int ch = getPlotHeight();
      int plotx = plotMargin.left + cw / 2;
      int ploty = plotMargin.top + ch / 2;
      double mx = (xmin + xmax) / 2;
      double my = (ymin + ymax) / 2;      
      double mvx = (p==null ? 0 : untransformx(p.x, plotMargin.left, cw));
      double mvy = (p==null ? 0 : untransformy(p.y, plotMargin.top, ch));
      
      double r = xmax - xmin;
      r *= (0.5 / zoom);
      if (bCursorZoom){
         double dx = (2.0*r)/cw;
         mx = mvx - dx*(p.x-plotx);
      }
      xmin = mx - r;
      xmax = mx + r;

      r = ymax - ymin;
      r *= (0.5 / zoom);
      if (bCursorZoom){
         double dy = (2.0*r)/ch;
         my = mvy + dy*(p.y-ploty);
      }
      ymin = my - r;
      ymax = my + r;
      
      calcGrid();
      repaint();
   }
   
   public void zoomIn(boolean bCursorZoom, Point p)
   {
      bAutoAxes = false;
            
      int cw = getPlotWidth();
      int ch = getPlotHeight();
      int plotx = plotMargin.left + cw / 2;
      int ploty = plotMargin.top + ch / 2;
      double mx = (xmin + xmax) / 2;
      double my = (ymin + ymax) / 2;      
      double mvx = (p==null ? 0 : untransformx(p.x, plotMargin.left, cw));
      double mvy = (p==null ? 0 : untransformy(p.y, plotMargin.top, ch));
      
      // zoom in
      double r = xmax - xmin;
      r *= (zoom * 0.5);      
      if (bCursorZoom){
         double dx = (2.0*r)/cw;
         mx = mvx - dx*(p.x-plotx);
      }
      xmin = mx - r;
      xmax = mx + r;

      r = ymax - ymin;
      r *= (zoom * 0.5);
      if (bCursorZoom){
         double dy = (2.0*r)/ch;
         my = mvy + dy*(p.y-ploty);
      }
      ymin = my - r;
      ymax = my + r;

      calcGrid();
      repaint();
   }
   
   /** test / debug application */
   public static void main(String args[])
   {
      final Sequence data = new DLRaw().load(args[0]);

      SwingUtilities.invokeLater(new Runnable() {
         public void run()
         {
            JFrame frame = new JFrame("Scatter Plot");
            Sequence data1 = data.subseq(0, data.length() / 2);
            Sequence data2 = data.subseq(data.length() / 2, data.length());
            System.err.printf("Loaded data: %d (%dD)\n", data.length(), data.getNumDims());
            ScatterPlot sp = new ScatterPlot();
            sp.addData(data1, Color.blue, '+');
            sp.addData(data2, Color.green, '.');

            ScatterPlot sp2 = new ScatterPlot();
            sp2.addData(data1, Color.blue, '+');
            sp2.addData(data2, Color.green, '.');

            frame.setSize(1000, 500);
            Library.centerWin(frame, null);
            JPanel p = new JPanel(new GridFillLayout(1, 2));
            p.add(sp);
            p.add(sp2);
            frame.setContentPane(p);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setVisible(true);

            
            MetricFV metric = new EuclideanFV(false);

            int nMaxMembers = 10;
            System.err.printf("Building tree (naive)... ");
            TimerMS timer = new TimerMS();
            MetricTree mtree = new MetricTree();
            mtree.constructNaive(data.getData(), metric, nMaxMembers);
            System.err.printf(" done (%dms).\n", timer.time());
            System.err.printf("#nodes (naive): %d   Volume=%f\n", mtree.getNumNodes(), mtree.getTotalVolume());

            System.err.printf("Building tree (anchor hierarchy)... ");
            timer.reset();
            MetricTree mtree2 = new MetricTree();
            mtree2.constructAnchor(data.getData(), metric, nMaxMembers);
            System.err.printf("done (%dms).\n", timer.time());
            System.err.printf("#nodes (anchor): %d   Volume=%f\n", mtree2.getNumNodes(), mtree2.getTotalVolume());

            mtree.apply(new DataTreeApply() {
               public void apply(DataTreeNode node, Object param)
               {
                  ScatterPlot sp = (ScatterPlot)param;
                  FeatureVec mean = (FeatureVec)node.meta.get(MetricTree.KeyMean);
                  double radius = (Double)node.meta.get(MetricTree.KeyRadius);
                  radius = Math.sqrt(radius);
                  sp.addCircle(mean.get(0), mean.get(1), radius, new Color(0, 0, 0, 0.25f), new Color(1, 0,
                        0, 0.03f));
               }
            }, sp);

            mtree2.apply(new DataTreeApply() {
               public void apply(DataTreeNode node, Object param)
               {
                  ScatterPlot sp = (ScatterPlot)param;
                  FeatureVec mean = (FeatureVec)node.meta.get(MetricTree.KeyMean);
                  double radius = (Double)node.meta.get(MetricTree.KeyRadius);
                  radius = Math.sqrt(radius);
                  sp.addCircle(mean.get(0), mean.get(1), radius, new Color(0, 0, 0, 0.25f), new Color(1, 0,
                        0, 0.03f));
               }
            }, sp2);
         }
      });
   }

   public void keyPressed(KeyEvent e)
   {
      int vk = e.getKeyCode();
      if (vk == KeyEvent.VK_EQUALS || vk==KeyEvent.VK_ADD) zoomIn(false, null);
      else if (vk == KeyEvent.VK_MINUS || vk==KeyEvent.VK_SUBTRACT) zoomOut(false, null);  
   }

   public void keyReleased(KeyEvent e)
   {
   }

   public void keyTyped(KeyEvent e)
   {      
   }
}
