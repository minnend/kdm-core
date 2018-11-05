package kdm.gui;

import kdm.data.*;
import kdm.util.*;
import kdm.io.*;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.*;
import java.text.*;
import java.io.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.imageio.*;

/**
 * A graph that displays frames from a video.
 */
public class VideoGraph extends LinearTimeGraph implements ActionListener, ChangeListener
{
   public static final int PREF_HEIGHT = 120;
   public static final int MIN_IMG_WIDTH = 40;

   public static final String sIndivFrames = "Show Individual Frames";
   public static final String sStrip = "Show Control Strip";
   public static final String sFrameNum = "Show Frame Number";
   public static final String sMouseFrame = "Show Mouse Frame";
   public static final String sMouseStaticFrame = "Show Static Mouse Frame";
   public static final String sImgName = "Show Image Name";
   public static final int hControl = 40;

   protected double aspectRatio = 4.0 / 3.0;
   protected int nCache = 16;
   protected String sPath = Library.ensurePathSep(".");
   protected String sOrigPath = null;
   protected String sFormatStr = null;
   protected File[] fFrames;
   protected double fps = 15.0;
   protected double vidPerData = 1.0;
   protected long offset = 0;
   protected TreeMap<Integer, Integer> syncs; // map from data to video frames
   protected LinkedList<String> imgNameCache;
   protected HashMap<String, BufferedImage> imgCache;
   protected final JPopupMenu menu = new JPopupMenu();
   protected boolean bShowControlStrip = false;
   protected boolean bShowFrameNum = false;
   protected boolean bShowImgName = false;
   protected boolean bShowMouseFrame = true;
   protected boolean bShowStaticMouseFrame = false;
   protected JCheckBoxMenuItem itemMF, itemSMF;
   protected DateExtracter dateExtract = null;
   protected JPanel pControlStrip;
   protected LabeledSlider lslImgHeight;

   public VideoGraph()
   {
      syncs = new TreeMap<Integer, Integer>();

      setOpaque(true);
      setDoubleBuffered(true);
      setLayout(null);
      setBackground(Color.black);

      // build popup menu
      JMenuItem item;
      // TODO: fix show indiv frames
      /*
       * item = new JMenuItem(sIndivFrames); item.addActionListener(this); menu.add(item);
       */
      itemMF = new JCheckBoxMenuItem(sMouseFrame, bShowMouseFrame);
      itemMF.addActionListener(this);
      menu.add(itemMF);
      itemSMF = new JCheckBoxMenuItem(sMouseStaticFrame, bShowStaticMouseFrame);
      itemSMF.addActionListener(this);
      menu.add(itemSMF);
      item = new JCheckBoxMenuItem(sFrameNum, bShowFrameNum);
      item.addActionListener(this);
      menu.add(item);
      item = new JCheckBoxMenuItem(sImgName, bShowImgName);
      item.addActionListener(this);
      menu.add(item);

      // TODO: make control strip useful?
      // item = new JCheckBoxMenuItem(sStrip, false);
      // item.addActionListener(this);
      // menu.add(item);

      // build control strip
      pControlStrip = new JPanel(new FlowLayout(FlowLayout.LEFT));
      lslImgHeight = new LabeledSlider("Image Height: %d", 0, 400);
      lslImgHeight.getSlider().setMinorTickSpacing(20);
      lslImgHeight.getSlider().setMajorTickSpacing(40);
      lslImgHeight.getSlider().setSnapToTicks(true);
      lslImgHeight.getSlider().setPaintTicks(false);
      lslImgHeight.addChangeListener(this);
      pControlStrip.add(lslImgHeight);
   }

   public boolean config(ConfigHelper chelp, String sKey, String sVal)
   {
      if (Library.stricmp(sKey, "cache")){
         nCache = Integer.parseInt(sVal);
      }
      else if (Library.stricmp(sKey, "date-extracter")){
         try{
            Class cls = Library.getClass(sVal, "kdm.io");
            dateExtract = (DateExtracter)cls.newInstance();
         } catch (Exception e){
            System.err.printf("Error: failed to load DateExtracter (%s)\n", sVal);
            return false;
         }
      }
      else if (Library.stricmp(sKey, "img-path")){
         sPath = Library.ensurePathSep(Library.qualifyPath(sVal, chelp.getBasePath()));
      }
      else if (Library.stricmp(sKey, "orig-path")){
         sOrigPath = Library.ensurePathSep(Library.qualifyPath(sVal, chelp.getBasePath()));
      }
      else if (Library.stricmp(sKey, "format-str")){
         sFormatStr = sVal;
      }
      else if (Library.stricmp(sKey, "fps")){
         fps = Double.parseDouble(sVal);
      }
      else if (sKey.startsWith("sync")){
         int i = sVal.indexOf("->");
         if (i < 0){
            System.err.println("Error: malformed sync value: " + sVal);
            return false;
         }
         String sData = sVal.substring(0, i).trim();
         String sVideo = sVal.substring(i + 2).trim();
         System.err.println("Found sync point (d->v): " + sData + " -> " + sVideo);
         int iData = Integer.parseInt(sData);
         int iVideo = Integer.parseInt(sVideo);
         syncs.put(iData, iVideo);
      }
      else if (Library.stricmp(sKey, "offset_ms")){
         offset = Long.parseLong(sVal);
      }
      else super.config(chelp, sKey, sVal);
      return true;
   }

   @Override
   public boolean finalSetup()
   {
      // create the caches
      imgNameCache = new LinkedList<String>();
      imgCache = new HashMap<String, BufferedImage>(nCache);

      if (sFormatStr == null){
         ArrayList<File> afiles = new ArrayList<File>();
         String sExts[] = new String[] { "jpg", "JPG", "png", "PNG", "gif", "GIF" };
         for(int iExt = 0; iExt < sExts.length; iExt++){
            File[] files = Library.getFilesWild(sPath, "*." + sExts[iExt]);
            if (files == null || files.length == 0) continue;
            for(File f : files)
               afiles.add(f);
         }
         fFrames = afiles.toArray(new File[0]);
         System.err.printf("VideoGraph) Found %d images\n", fFrames.length);

         // we need to sort the files by time stamp
         if (dateExtract != null){
            System.err.printf("Sorting image names by timestamp... ");
            Arrays.sort(fFrames, new Comparator() {
               public int compare(Object o1, Object o2)
               {
                  File a = (File)o1;
                  File b = (File)o2;

                  Date dateA = dateExtract.extractDate(a.getName());
                  Date dateB = dateExtract.extractDate(b.getName());
                  if (dateA == null || dateB == null){
                     System.err.printf("Failed to extract date: %s\n", (dateA == null ? a.getName() : b
                           .getName()));
                     System.exit(0);
                  }

                  long msA = dateA.getTime();
                  long msB = dateB.getTime();
                  if (msA < msB) return -1;
                  if (msA > msB) return 1;
                  return 0;
               }

            });
            System.err.printf(" done.\n");
         }
      }
      if ((fFrames == null || fFrames.length == 0) && sFormatStr == null){
         System.err.printf("Error: no frames found and no format string.\n");
         return false;
      }

      // get aspect ratio from first image
      BufferedImage bi = Library.readImage(fFrames[0].getAbsolutePath());
      if (bi != null){
         aspectRatio = (float)bi.getWidth() / bi.getHeight();
         System.err.printf("VideoGraph) New aspect ratio: %.3f   (from: %s)\n", aspectRatio, fFrames[0]
               .getName());
      }

      if (data == null){
         data = new Sequence("video", fps);
         if (dateExtract != null){
            System.err.printf("VideoGraph) extracting abs time from file names (%s)...\n", dateExtract
                  .getClass());
            for(int i = 0; i < fFrames.length; i++){
               Date date = dateExtract.extractDate(fFrames[i].getName());
               if (date == null){
                  System.err.printf("Failed to extract date: %s\n", fFrames[i].getName());
                  continue;
               }
               data.add(new FeatureVec(1, i), date.getTime() + offset);
            }
         }
         else{
            long ms = Library.AppStartTime;
            data.add(new FeatureVec(1, 0), ms);
            Calendar calEnd = Library.now();
            ms += (long)Math.round((getNumFrames() + 1) * 1000.0 / fps);
            data.add(new FeatureVec(1, getNumFrames() + 1), ms + offset);
         }
         vidPerData = 1.0;
         setData(data);
         System.err.printf("VideoGraph) #frames: %d  length_sec: %.2f (%.2fmin)   data_ms: %d\n",
               getNumFrames(), getNumFrames() / fps, getNumFrames() / fps / 60.0, data.getLengthMS());
         System.err.printf("VideoGraph) [%s] -> [%s]\n", Library.formatTime(data.getStartMS()), Library
               .formatTime(data.getEndMS()));
      }
      else{
         vidPerData = fps / data.getFreq();
         System.err.printf("vidperdata = %.4f   (fps=%.2f  data_freq=%.2f)\n", vidPerData, fps, data
               .getFreq());
      }

      // make sure that we have a mapping for the start of the data
      if (syncs.get(0) == null){
         if (syncs.size() == 0) syncs.put(0, 0);
         else{
            Set<Map.Entry<Integer, Integer>> entries = syncs.entrySet();
            Iterator<Map.Entry<Integer, Integer>> it = entries.iterator();
            Map.Entry<Integer, Integer> first = it.next();
            int iData = first.getKey();
            int iVideo = first.getValue();
            System.err.println("First sync: " + iData + " -> " + iVideo);
            int iSigStart = 0;
            int iVideoStart = (int)Math.round(iVideo - (iData * fps));
            if (iVideoStart > 0){
               iVideoStart = 0;
               iSigStart = (int)Math.round(iData - (iVideo / fps));
            }
            syncs.put(iSigStart, iVideoStart);
            System.err.println("Adding start sync: " + iSigStart + " -> " + iVideoStart);
         }
      }
      return true;
   }

   public int getNumFrames()
   {
      return (fFrames == null ? 0 : fFrames.length);
   }

   /**
    * Computes the locations of each horizontal (value axis) grid line including the screen coordinates
    * (relative to 0) and the actual value at that level.
    */
   public ArrayList<ValueY> getGridValues()
   {
      return null;
   }

   /**
    * Computes the screen coordinates that represents the (time,value)=(x,y) of the i-th data point. Note that
    * the value (y-coor) is always zero for a video component. Also, the dimension paramter (d) is ignored by
    * this function since video doesn't have dimensionality in this sense.
    */
   public Point getGraphPoint(int ix, int d, double timeStep)
   {
      long ms = data.getTimeMS(ix);
      long time0 = timeCompStart + timeOffset;
      int x = (int)Math.round((ms - time0) / timeStep);
      return new Point(x, 0);
   }

   /** Convenience method for getting the x-coord from the frame index via getGraphPoint */
   public int getFrameX(int ix, double timeStep)
   {
      return getGraphPoint(ix, 0, timeStep).x;
   }

   /**
    * Returns the sync entry that maps a data index to the given video frame. If an exact match can't be
    * found, the next smallest entry is returned.
    */
   protected SyncPoint getSyncVideo(int iVideo)
   {
      return SyncPoint.getSyncVideo(iVideo, syncs, vidPerData);
   }

   /**
    * Returns the sync entry that maps to the given data frame
    */
   protected SyncPoint getSyncData(int iData)
   {
      return SyncPoint.getSyncData(iData, syncs, vidPerData);
   }

   /**
    * Returns the image corresponding to the given video frame index.
    */
   protected BufferedImage getFrame(int iFrame)
   {
      if (iFrame < 0) return null;

      // construct the image name
      File file;
      if (fFrames != null){
         if (iFrame < 0 || iFrame >= fFrames.length) return null;
         file = fFrames[iFrame];
      }
      else file = new File(Library.qualifyPath(sPath, String.format(sFormatStr, iFrame)));

      // check to see if we've already cached this image
      BufferedImage img = imgCache.get(file.getAbsolutePath());
      // System.err.printf("(%s) size: %d in cache? %b free mem: %.2f\n", file.getName(), imgCache.size(),
      // img!=null, Runtime.getRuntime().freeMemory()/(1024.0*1024));
      if (img != null) return img;

      // not cached, so load it
      img = Library.readImage(file.getAbsolutePath());
      if (img == null){
         // System.err.println("Failed");
         System.err.println("Error loading file: ");
         System.err.println(" [" + file.getAbsolutePath() + "]");
         return null;
      }

      // remove oldest element from cache if necessary
      if (imgNameCache.size() >= nCache){
         String sRemove = imgNameCache.removeFirst();
         imgCache.remove(sRemove);
      }

      // add new image to cache
      imgNameCache.addLast(file.getAbsolutePath());
      BufferedImage imgCompat = Library.getGC().createCompatibleImage(img.getWidth(), img.getHeight());
      Graphics2D g = imgCompat.createGraphics();
      g.drawImage(img, 0, 0, null);
      g.dispose();
      imgCache.put(file.getAbsolutePath(), imgCompat);
      return imgCompat;
   }

   public int getFrameWidth()
   {
      int lslHeight = lslImgHeight.getValue();
      return (int)Math.round(aspectRatio * (lslHeight == 0 ? getFrameHeight() : lslHeight));
   }

   public int getFrameHeight()
   {
      int height = lslImgHeight.getValue();
      if (height == 0) height = getHeight();
      return height;
   }

   /**
    * Draws the actual video frames in the component
    */
   public void paintComponent(Graphics2D g, int cw, int ch)
   {
      int height = getFrameHeight();
      final int nFrames = getNumFrames();

      // clear the background
      g.setColor(getBackground());
      g.fillRect(0, 0, cw, height);

      // what's the first (potentially) visible frame
      int iFrame = getIndexFromX(0, true);
      if (iFrame < 1) iFrame = 0;
      else iFrame--;

      // get rendering info
      int imgw = getFrameWidth();
      int w2 = imgw / 2;
      int xmin = -imgw + 1;
      double timeStep = calcTimeStep();

      while(true){
         int iNext = getNextFullFrame(iFrame, xmin, w2);
         if (iNext < 0 || iNext >= nFrames) break;
         int xNext = getFrameX(iNext, timeStep) - w2;

         /*
          * int iShrink = (iFrame-1+iNext) / 2; int wshrink = xNext - xmin; if (wshrink >= MIN_IMG_WIDTH &&
          * iShrink>=iFrame && iShrink>0) drawFrame(g2, iShrink, xmin, wshrink);
          */

         if (xNext >= cw) break;
         xmin = xNext + imgw;
         drawFrame(g, iNext, xNext, imgw);

         // go to the next frame
         iFrame = iNext + 1;
      }

      if ((bShowMouseFrame || bShowStaticMouseFrame) && mouseTime != null && mouseTime.index >= 0){
         long ms = mouseTime.time;
         int index = mouseTime.index;
         int xCenter = getFrameX(index, timeStep);
         int xLeft = xCenter - w2;

         if (mouseTime.x >= xLeft && mouseTime.x < xLeft + imgw){
            if (bShowStaticMouseFrame) xLeft = cw / 2 - w2;
            drawFrame(g, index, xLeft, imgw);
            g.setColor(Color.black);
            g.drawLine(xLeft, 0, xLeft, ch);
            g.drawLine(xLeft + imgw, 0, xLeft + imgw, ch);
            g.setColor(Color.orange);
            g.drawLine(xLeft - 1, 0, xLeft - 1, ch);
            g.drawLine(xLeft + imgw + 1, 0, xLeft + imgw + 1, ch);
         }
      }
   }

   protected void drawFrame(Graphics2D g, int iFrame, int x, int imgw)
   {
      BufferedImage img = getFrame(iFrame);
      if (img != null){
         final FontMetrics fm = g.getFontMetrics();
         int imgh = img.getHeight() * imgw / img.getWidth();
         int y = (getHeight() - imgh) / 2;
         g.drawImage(img, x, y, imgw, imgh, this);

         Shape clip = g.getClip();
         g.setClip(x, 0, imgw, imgh);
         Library.setAntiAlias(g, true);
         if (bShowFrameNum){
            String s = String.format("%d", iFrame);
            int sw = fm.stringWidth(s);
            int sx = x + imgw - sw - 2;
            int sy = y + imgh - 2;
            g.setColor(Color.black);
            g.drawString(s, sx, sy);
            g.setColor(Color.black);
            g.drawString(s, sx - 2, sy);
            g.setColor(Color.black);
            g.drawString(s, sx, sy - 2);
            g.setColor(Color.black);
            g.drawString(s, sx - 2, sy - 2);
            g.setColor(Color.orange);
            g.drawString(s, sx - 1, sy - 1);
         }

         if (bShowImgName){
            String s = fFrames[iFrame].getName();
            int sh = fm.getHeight();
            int sx = x + 2;
            int sy = sh + 2;
            g.setColor(Color.black);
            g.drawString(s, sx, sy);
            g.drawString(s, sx - 2, sy);
            g.setColor(Color.black);
            g.drawString(s, sx, sy - 2);
            g.setColor(Color.black);
            g.drawString(s, sx - 2, sy - 2);
            g.setColor(Color.magenta);
            g.drawString(s, sx - 1, sy - 1);
         }
         Library.setAntiAlias(g, false);
         g.setClip(clip);
      }
   }

   /**
    * Get the index of the next frame
    * 
    * @param iFrame current frame
    * @param xmin minimum x coordinate of next frame
    * @param w2 half width of image
    * @return index of the next frame, or -1 if none
    */
   protected int getNextFullFrame(int iFrame, int xmin, int w2)
   {
      int nFrames = getNumFrames();
      double timeStep = calcTimeStep();
      while(iFrame < nFrames){
         int xCenter = getFrameX(iFrame, timeStep);
         int x = xCenter - w2;
         if (x >= xmin) return iFrame;
         iFrame++;
      }
      return -1;
   }

   /**
    * Returns the size that the component would like to have based on the dataset (if it exists).ad
    */
   public Dimension getPreferredSize()
   {
      if (data == null) return new Dimension(400, PREF_HEIGHT);
      else{
         int lslHeight = lslImgHeight.getValue();
         int imgh = lslHeight == 0 ? PREF_HEIGHT : lslHeight;
         int imgw = (int)Math.round(aspectRatio * imgh);
         int w = getNumFrames() * imgw;
         return new Dimension(w, imgh);
      }
   }

   public Dimension getPreferredScrollableViewportSize()
   {
      return getPreferredSize();
   }

   public void componentResized(ComponentEvent e)
   {
      if (bShowControlStrip){
         pControlStrip.setBounds(0, getHeight() - hControl, getWidth(), hControl);
      }
      super.componentResized(e);
   }

   public void mousePressed(MouseEvent e)
   {
      if (e.isPopupTrigger()){
         menu.show(e.getComponent(), e.getX(), e.getY());
      }
      else super.mousePressed(e);
   }

   public void mouseReleased(MouseEvent e)
   {
      if (e.isPopupTrigger()){
         menu.show(e.getComponent(), e.getX(), e.getY());
      }
      else super.mouseReleased(e);
   }

   public void actionPerformed(ActionEvent e)
   {
      String cmd = e.getActionCommand();

      if (cmd.equals(sStrip)){
         bShowControlStrip = !bShowControlStrip;
         if (bShowControlStrip){
            add(pControlStrip);
            pControlStrip.setBounds(0, getHeight() - hControl, getWidth(), hControl);
            pControlStrip.revalidate();
         }
         else{
            remove(pControlStrip);
         }
      }
      else if (cmd.equals(sIndivFrames)){
         int lslHeight = lslImgHeight.getValue();
         int imgh = (lslHeight == 0 ? getHeight() : lslHeight);
         int imgw = (int)Math.round(aspectRatio * imgh);
         int nFrames = getNumFrames();
         int vw = nFrames * imgw - imgw;
         setVirtualWidth(vw);
      }
      else if (cmd.equals(sFrameNum)){
         bShowFrameNum = !bShowFrameNum;
         repaint();
      }
      else if (cmd.equals(sMouseFrame)){
         bShowMouseFrame = !bShowMouseFrame;
         itemSMF.setSelected(false);
         bShowStaticMouseFrame = false;
         repaint();
      }
      else if (cmd.equals(sMouseStaticFrame)){
         bShowStaticMouseFrame = !bShowStaticMouseFrame;
         itemMF.setSelected(false);
         bShowMouseFrame = false;
         repaint();
      }
      else if (cmd.equals(sImgName)){
         bShowImgName = !bShowImgName;
         repaint();
      }
   }

   public void stateChanged(ChangeEvent e)
   {
      Object src = e.getSource();

      if (src == lslImgHeight){
         repaint();
      }
   }

   public void mouseClicked(MouseEvent e)
   {
      if (e.getClickCount() == 2){
         int x = e.getX();
         int iFrame = getIndexFromX(x, true);
         int imgx = getFrameX(iFrame, calcTimeStep());
         int w = getFrameWidth();
         int w2 = w / 2;
         if (x >= imgx - w2 && x < imgx + w2){
            String sImgPath = fFrames[iFrame].getAbsolutePath();
            if (sOrigPath != null) sImgPath = sOrigPath + fFrames[iFrame].getName();
            VideoFrame vf = new VideoFrame(new File(sImgPath));
            vf.setSize(800, 600);
            vf.setVisible(true);
         }
      }
      super.mouseClicked(e);
   }

   @Override
   public long getTimeFromX(int x)
   {
      return 0;
   }
}
