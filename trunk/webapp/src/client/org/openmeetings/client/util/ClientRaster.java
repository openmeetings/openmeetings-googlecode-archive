package org.openmeetings.client.util;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import org.apache.log4j.Logger;
import org.openmeetings.client.beans.ClientConnectionBean;
import org.openmeetings.client.beans.ClientImageFrame;
import org.openmeetings.client.beans.ClientVirtualScreenBean;

//import com.sun.imageio.plugins.png.

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

/**
 * @author sebastianwagner
 *
 */
public class ClientRaster {
	
	private static Logger log = Logger.getLogger(ClientRaster.class);
	
	public static synchronized List<ClientImageFrame> tileScreen() {
		try {
			
			List<ClientImageFrame> clientImageFrames = new LinkedList<ClientImageFrame>();
			
			int tileNumberWidth = Double.valueOf(Math.floor(ClientVirtualScreenBean.vScreenSpinnerWidth / ClientConnectionBean.tileWidth)).intValue();
			int tileNumberHeight = Double.valueOf(Math.floor(ClientVirtualScreenBean.vScreenSpinnerHeight / ClientConnectionBean.tileHeight)).intValue();
			
			int xOffset = ClientVirtualScreenBean.vScreenSpinnerX;
			int yOffset = ClientVirtualScreenBean.vScreenSpinnerY;
			
			log.debug("tileNumberWidth,tileNumberHeight "+tileNumberWidth+","+tileNumberHeight);
			log.debug("xOffset,yOffset "+xOffset+","+yOffset);
			
			Robot robot = ClientVirtualScreenBean.robot;
			if (robot==null) robot = new Robot();
			Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
			
			for (int x=0;x<=tileNumberWidth;x++) {
				
				for (int y=0;y<=tileNumberHeight;y++) {
					
					int rect_x = xOffset + ( x * ClientConnectionBean.tileWidth );
					int rect_y = yOffset + ( y * ClientConnectionBean.tileHeight );
					
					log.debug("rect_x,rect_y,tileWidth,tileHeight "+rect_x+","+rect_y+","+ClientConnectionBean.tileWidth+","+ClientConnectionBean.tileHeight);
					
					int rectWidth = ClientConnectionBean.tileWidth;
					int rectHeight = ClientConnectionBean.tileHeight;
					
					if (rect_x + rectWidth > screenSize.width) {
						rectWidth = screenSize.width - rect_x;
					}
					if (rect_y + rectHeight > screenSize.height) {
						rectHeight = screenSize.height - rect_y;
					}
					
					Rectangle screenRectangle = new Rectangle(rect_x,rect_y,rectWidth,rectHeight);
					
					Rectangle shrinkedRectAngle =  new Rectangle(Math.round(rect_x * ClientConnectionBean.imgQuality) , Math.round(rect_y * ClientConnectionBean.imgQuality),
							Math.round(rectWidth * ClientConnectionBean.imgQuality), Math.round(rectHeight * ClientConnectionBean.imgQuality));
					
					
					BufferedImage imageScreen = robot.createScreenCapture(screenRectangle);
					
					int scaledWidth = Math.round(rectWidth * ClientConnectionBean.imgQuality);
					int scaledHeight = Math.round(rectHeight * ClientConnectionBean.imgQuality);
					
					Image img = imageScreen.getScaledInstance(scaledWidth, scaledHeight,Image.SCALE_SMOOTH);
					
					//BufferedImage.TYPE_INT_RGB
					//TYPE_3BYTE_BGR
					BufferedImage image = new BufferedImage(scaledWidth, scaledHeight,BufferedImage.TYPE_3BYTE_BGR);
					
					Graphics2D biContext = image.createGraphics();
					biContext.drawImage(img, 0, 0, null);
					
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					
					JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
					JPEGEncodeParam encpar = encoder.getDefaultJPEGEncodeParam(image);
					
					encpar.setQuality(ClientConnectionBean.imgQualityDefault, false);
					encoder.setJPEGEncodeParam(encpar);
					encoder.encode(image);
					
					imageScreen.flush();
					
					byte[] payload = out.toByteArray();
					
					//GZip
					ByteArrayOutputStream byteGzipOut = new ByteArrayOutputStream();
			    	GZIPOutputStream gZipOut = new GZIPOutputStream(byteGzipOut);
			    	
			    	gZipOut.write(payload);
			    	gZipOut.close();
			    	
			    	image.flush();
			    	if (img!=null)img.flush();
			    	
			    	log.debug("byteGzipOut LENGTH "+byteGzipOut.toByteArray().length);
					log.debug("payload LENGTH "+payload.length);
					
					clientImageFrames.add(new ClientImageFrame(shrinkedRectAngle,byteGzipOut.toByteArray()));
				}
				
			}
			
			return clientImageFrames;
			
		} catch (Exception err) {
			log.error("[checkFrame]",err);
		}
		return null;
	}

}
