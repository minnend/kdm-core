package kdm.gui;

import kdm.util.*;
import kdm.gui.layouts.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.imageio.*;
import java.io.*;

import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.dom.GenericDOMImplementation;

import org.w3c.dom.Document;
import org.w3c.dom.DOMImplementation;

/**
 * dialog box that allows customization for saving the contents of a component
 * as an image
 */
public class SaveCompImage extends JDialog implements FocusListener, ActionListener {
   public static final InputVerifier posintVerifier = new InputVerifier() {

      @Override
      public boolean verify(JComponent input) {
         try {
            String s = ((JTextField) input).getText();
            int x = Integer.parseInt(s);
            return (x > 0);
         } catch (Exception e) {
            return false;
         }
      }
   };

   public static final InputVerifier posrealVerifier = new InputVerifier() {

      @Override
      public boolean verify(JComponent input) {
         try {
            String s = ((JTextField) input).getText();
            double x = Double.parseDouble(s);
            return (x > 0);
         } catch (Exception e) {
            return false;
         }
      }
   };

   protected Component comp;

   protected int compw, comph;

   protected BufferedImage img;

   protected JLabel lbImage;

   protected JTextField tfWidth, tfHeight, tfPercent;

   protected JButton btSave, btCancel;

   protected ComboPanel cp;

   public SaveCompImage(Window owner, Component comp) {
      super(owner, "Save Component Image", ModalityType.APPLICATION_MODAL);
      this.comp = comp;
      compw = comp.getWidth();
      comph = comp.getHeight();
      setSize(600, 400);
      Library.centerWin(this, owner);

      JPanel mainp = new JPanel(new BorderLayout());
      setContentPane(mainp);

      // setup the control panel
      JPanel leftp = new JPanel(new BorderLayout());
      mainp.add(leftp, BorderLayout.WEST);
      cp = new ComboPanel("Format: ");
      leftp.add(cp, BorderLayout.CENTER);
      JPanel controlp = new JPanel(new ParagraphLayout());
      controlp.add(new JLabel("Original Size:"), ParagraphLayout.NEW_PARAGRAPH);
      controlp.add(new JLabel(String.format("%d x %d", compw, comph)));
      controlp.add(new JLabel("Save Width:"), ParagraphLayout.NEW_PARAGRAPH);
      tfWidth = new JTextField(String.format("%d", compw), 4);
      tfWidth.selectAll();
      tfWidth.addFocusListener(this);
      tfWidth.requestFocusInWindow();
      tfWidth.setInputVerifier(posintVerifier);
      controlp.add(tfWidth);
      controlp.add(new JLabel("Save Height:"), ParagraphLayout.NEW_PARAGRAPH);
      tfHeight = new JTextField(String.format("%d", comph), 4);
      tfHeight.setInputVerifier(posintVerifier);
      tfHeight.addFocusListener(this);
      controlp.add(tfHeight);
      controlp.add(new JLabel("Save %:"), ParagraphLayout.NEW_PARAGRAPH);
      tfPercent = new JTextField("100.0", 4);
      tfPercent.setInputVerifier(posrealVerifier);
      tfPercent.addFocusListener(this);
      controlp.add(tfPercent);
      cp.addPanel(" png", controlp);
      cp.addPanel(" jpg", controlp);

      // svg controls
      controlp = new JPanel();
      // no special controls for SVG (yet)
      cp.addPanel(" svg", controlp);

      JPanel p = new JPanel();
      leftp.add(p, BorderLayout.SOUTH);
      btSave = new JButton("Save...");
      btSave.addActionListener(this);
      p.add(btSave);
      btCancel = new JButton("Cancel");
      btCancel.addActionListener(this);
      p.add(btCancel);

      // setup the preview image
      lbImage = new JLabel(new ImageIcon(getCompImage()));
      mainp.add(new JScrollPane(lbImage), BorderLayout.CENTER);
   }

   public Image getCompImage() {
      int w = comp.getWidth();
      int h = comp.getHeight();
      try {
         w = Integer.parseInt(tfWidth.getText());
      } catch (NumberFormatException nfe1) {
      }
      try {
         h = Integer.parseInt(tfWidth.getText());
      } catch (NumberFormatException nfe1) {
      }
      return getCompImage(w, h);
   }

   public Image getCompImage(int w, int h) {
      if (img != null && img.getWidth(this) == w && img.getHeight(this) == h)
         return img;
      int w0 = comp.getWidth();
      int h0 = comp.getHeight();
      comp.setSize(w, h);
      img = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
      Graphics g = img.getGraphics();
      comp.paint(g);
      g.dispose();
      comp.setSize(w0, h0);
      return img;
   }

   public void focusGained(FocusEvent e) {
      Object src = e.getSource();

      if (src == tfWidth) {
         tfWidth.selectAll();
      } else if (src == tfHeight) {
         tfHeight.selectAll();
      } else if (src == tfPercent) {
         tfPercent.selectAll();
      }

   }

   public void focusLost(FocusEvent e) {
      Object src = e.getSource();

      if (src == tfWidth) {
         int w = Integer.parseInt(tfWidth.getText());
         int h = (int) Math.round((double) w * comph / compw);
         tfHeight.setText("" + h);
         double p = 100.0 * w / compw;
         tfPercent.setText(String.format("%.1f", p));
      } else if (src == tfHeight) {
         int h = Integer.parseInt(tfHeight.getText());
         int w = (int) Math.round((double) h * compw / comph);
         tfWidth.setText("" + w);
         double p = 100.0 * w / compw;
         tfPercent.setText(String.format("%.1f", p));
      } else if (src == tfPercent) {
         double p = Double.parseDouble(tfPercent.getText());
         p = Math.round(p * 10.0) / 1000.0;
         int w = (int) Math.round(compw * p);
         tfWidth.setText("" + w);
         int h = (int) Math.round(comph * p);
         tfHeight.setText("" + h);
         tfPercent.setText(String.format("%.1f", 100.0 * p));
      }

      lbImage.setIcon(new ImageIcon(getCompImage()));
   }

   protected boolean savePlot(File f, String ext) {
      if (ext.equals("png") || ext.equals("jpg")) {
         try {
            ImageIO.write(img, ext, f);
         } catch (IOException e) {
            System.err.println(e);
            return false;
         }
      } else if (ext.equals("svg")) {
         // Get a DOMImplementation
         DOMImplementation domImpl = GenericDOMImplementation.getDOMImplementation();
         String svgNamespaceURI = "http://www.w3.org/2000/svg";

         // Create an instance of org.w3c.dom.Document
         Document document = domImpl.createDocument(svgNamespaceURI, "svg", null);

         // Create an instance of the SVG Generator
         SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

         // Render into the SVG Graphics2D implementation
         comp.paint(svgGenerator);

         // Finally, stream out SVG to the standard output using UTF-8
         // character to byte encoding
         boolean useCSS = true; // we want to use CSS style attribute
         try {
            Writer out = new BufferedWriter(new FileWriter(f));
            svgGenerator.stream(out, useCSS);
            out.close();
         } catch (Exception e) {
            System.err.println(e);
            return false;
         }
      } else {
         System.err.printf("Error: unrecognized extension (%s)\n", ext);
         return false;
      }

      return true;
   }

   public void actionPerformed(ActionEvent e) {
      Object src = e.getSource();

      if (src == btCancel) {
         this.setVisible(false);
         this.dispose();
      } else if (src == btSave) {
         String ext = ((String) cp.getComboBox().getSelectedItem()).substring(1);
         JFileChooser fc = Library.buildFileChooser(ext, String.format("Plot Image (*.%s)", ext));
         Window win = SwingUtilities.getWindowAncestor(this);
         if (fc.showSaveDialog(win) == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            if (!f.getName().endsWith("." + ext))
               f = new File(f.getAbsolutePath() + "." + ext);
            if (savePlot(f, ext))
               JOptionPane.showMessageDialog(win, String.format("Saved Plot Image: %s", f.getName()),
                     "Saved Plot Image", JOptionPane.INFORMATION_MESSAGE);
            else
               JOptionPane.showMessageDialog(win, String.format("Failed to saved plot image: %s", f.getName()),
                     "Save Error", JOptionPane.ERROR_MESSAGE);
         }
      }

   }

}
