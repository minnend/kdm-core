package kdm.gui;

import java.awt.*;
import java.io.*;
import java.net.*;
import javax.swing.*;
import java.awt.image.*;
import kdm.util.*;
/*import javax.media.*;
import javax.media.control.*;
import javax.media.util.*;
import javax.media.format.*;*/

/** window with an image in it */
public class VideoFrame extends JFrame {
	
   public VideoFrame(File file) {
		setContentPane(new ImageComp(file));
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	}
   
   public VideoFrame(Image image) {
      setContentPane(new ImageComp(image));
      setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
   }

	public static void main(String args[]) { // TODO: jmf experiments -- it's a mess
		/*VideoFrame frame = new VideoFrame(null);
		frame.setSize(400, 400);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		String sFile = args[0];
		URL url = null;
		try {
			url = new URL("file://" + sFile);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			System.exit(0);
		}

		Player player = null;
		try {
			player = Manager.createRealizedPlayer(url);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(0);
		}

		FramePositioningControl fpc = (FramePositioningControl) player
				.getControl("javax.media.control.FramePositioningControl");
		FrameGrabbingControl fg = (FrameGrabbingControl) player
				.getControl("javax.media.control.FrameGrabbingControl");

		System.err.printf("fpc: %s\n", fpc);
		System.err.printf("fg: %s\n", fg);
		
		
		JPanel p = new JPanel(new BorderLayout());
		frame.setContentPane(p);
		Component vc = player.getVisualComponent();
		Component cp = player.getControlPanelComponent();
		p.add(vc, BorderLayout.CENTER);
		p.add(cp, BorderLayout.SOUTH);
		
		Component c = vc;
		while(c!=p)
		{
			System.err.printf(" %s\n", c);
			c = c.getParent();
		}
		
		player.start();
		
		
		int seekto = fpc.seek(1000);
		System.err.printf("seekto: %d\n", seekto);
		*/
		

		
		
/*
		Buffer buf = fg.grabFrame();
		VideoFormat vf = (VideoFormat) buf.getFormat();
		BufferToImage bufferToImage = new BufferToImage(vf);
		Image im = bufferToImage.createImage(buf);
		int w = im.getWidth(null);
		int h = im.getHeight(null);
		System.err.printf("Frame: %d x %d\n", w, h);
		BufferedImage formatImg = new BufferedImage(w, h,
				BufferedImage.TYPE_3BYTE_BGR);
*/
		
		/*for(int i=0; i<100; i++)
		{
			Library.sleep(1000);
			Graphics g = vc.getGraphics();
			g.
			System.err.printf("vc: ")
		}*/
	}
}
