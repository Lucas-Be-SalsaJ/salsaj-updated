//EU_HOU
package ij.plugin;
import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.io.*;
import java.net.URL;
import java.awt.image.*;

/** This plugin implements the Help/About ImageJ command by opening
	the about.jpg in ij.jar, scaling it 400% and adding some text. */
	public class AboutBox implements PlugIn {
		//EU_HOU CHANGES : default:LARGE_FONT=30
		static final int SMALL_FONT=14, LARGE_FONT=40;

	public void run(String arg) {
		System.gc();
		int lines = 7;
		String[] text = new String[lines];
        /*
         * EU_HOU Bundle CHANGES
         */
		text[0] = "SalsaJ "+ImageJ.VERSION+ImageJ.BUILD;
        //EU_HOU Bundle
		text[1] = IJ.getBundle().getString("About1");
		text[2] = IJ.URL;
        //EU_HOU Bundle
		text[3] = IJ.getBundle().getString("About2");
        text[4] = IJ.getBundle().getString("About3");
		text[5] =  "Java "+System.getProperty("java.version")+(IJ.is64Bit()?" (64-bit)":" (32-bit)");
		text[6] = IJ.freeMemory();
		/*
		 * EU_HOU Bundle CHANGES END
		 */
		ImageProcessor ip = null;
		ImageJ ij = IJ.getInstance();
		/*
		 * EU_HOU CHANGES FIXME : url "/about.jpg" --> "images/about2.jpg" + [...].getClassLoader()[...]
		 */
		URL url = ij.getClass().getClassLoader().getResource("images/about2.jpg");
		/*
		 * EU_HOU CHANGES END
		 */
		if (url!=null) {
			Image img = null;
			try {img = ij.createImage((ImageProducer)url.getContent());}
			catch(Exception e) {}
			if (img!=null) {
				ImagePlus imp = new ImagePlus("", img);
				ip = imp.getProcessor();
			}
		}
		if (ip==null) 
			ip =  new ColorProcessor(55,45);
		ip = ip.resize(ip.getWidth()*4, ip.getHeight()*4);
		ip.setFont(new Font("SansSerif", Font.PLAIN, LARGE_FONT));
		ip.setAntialiasedText(true);
		int[] widths = new int[lines];
		widths[0] = ip.getStringWidth(text[0]);
		ip.setFont(new Font("SansSerif", Font.PLAIN, SMALL_FONT));
		for (int i=1; i<lines-1; i++)
			widths[i] = ip.getStringWidth(text[i]);
		int max = 0;
		for (int i=0; i<lines-1; i++) 
			if (widths[i]>max)
				max = widths[i];
		/*
		 * EU_HOU CHANGES
		 */
		//ip.setColor(new Color(255,255, 140));
		ip.setColor(new Color(255,255, 255));
		ip.setFont(new Font("SansSerif", Font.PLAIN, LARGE_FONT));
		//int y = 45 --> 40
		int y  = 40;
		ip.drawString(text[0], x(text[0],ip,max), y);
		ip.setFont(new Font("SansSerif", Font.PLAIN, SMALL_FONT));
		//y += 30;
		y += 100;
		ip.drawString(text[1], x(text[1],ip,max), y);
		y += 18;
		ip.drawString(text[2], x(text[2],ip,max), y);
		//y += 18;
		y += 25;
		ip.drawString(text[3], x(text[3],ip,max), y);
		y += 18;
		ip.drawString(text[4], x(text[4],ip,max), y);
		if (IJ.maxMemory()>0L) {
			y += 18;
			ip.drawString(text[5], x(text[5],ip,max), y);
		}
		/*
		 * EU_HOU CHANGES END
		 */
		ip.drawString(text[6], ip.getWidth()-ip.getStringWidth(text[6])-10, ip.getHeight()-3);
		ImageWindow.centerNextImage();
		//EU_HOU Bundle
		ImagePlus imp = new ImagePlus(IJ.getBundle().getString("AboutIJ"), ip);
		String info = text[0] +"\n" + text[4] +"\n" + text[5];
		imp.setProperty("Info", info);
		imp.show();
	}

	int x(String text, ImageProcessor ip, int max) {
		return ip.getWidth() - max + (max - ip.getStringWidth(text))/2 - 10;
	}

}
