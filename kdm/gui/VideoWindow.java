package kdm.gui;

import kdm.util.*;
import kdm.data.*;
import kdm.io.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import javax.imageio.*;

public class VideoWindow extends JPanel
{
    protected BufferedImage img = null;

    public void setImage(BufferedImage _img)
    {
        img = _img;
        repaint();
    }

    public void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D)g;
        Dimension dims = getSize();

        if (img == null)
        {
            g.setColor(Color.blue);
            g.fillRect(0,0,dims.width,dims.height);
            return;
        }

        int vw = img.getWidth();
        int vh = img.getHeight();

        g2.drawImage(img, 0, 0, vw, vh, null);
    }
}
