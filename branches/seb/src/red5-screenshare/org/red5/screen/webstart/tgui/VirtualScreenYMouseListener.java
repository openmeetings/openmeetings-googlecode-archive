package org.red5.screen.webstart.tgui;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import javax.swing.event.MouseInputAdapter;

import org.red5.screen.webstart.ScreenShareRTMPT;

public class VirtualScreenYMouseListener extends MouseInputAdapter  {

	private double y = 0;

	public void mouseEntered(MouseEvent e) {
		// TODO Auto-generated method stub
		ScreenShareRTMPT.instance.t.setCursor( Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR) ) ;
	}

	public void mouseExited(MouseEvent e) {
		// TODO Auto-generated method stub
		ScreenShareRTMPT.instance.t.setCursor( Cursor.getPredefinedCursor( Cursor.DEFAULT_CURSOR ) ) ;
	}

	public void mousePressed(MouseEvent e) {
		// TODO Auto-generated method stub
		VirtualScreen.instance.showWarning=false;
		this.y = e.getY();
//		System.out.println(this.x+" "+this.y);
	}

	public void mouseReleased(MouseEvent e) {
		// TODO Auto-generated method stub
		VirtualScreen.instance.showWarning=true;
	}

	public void mouseDragged(MouseEvent e) {
		double newY = e.getY();

		int delta = Long.valueOf(Math.round(this.y-newY)).intValue();
		int newYPosition = VirtualScreenBean.vScreenSpinnerY-delta;
		int newHeight = VirtualScreenBean.vScreenSpinnerHeight+delta;

//		System.out.println(delta+" "+newYPosition+" "+newHeight);
		if (newYPosition>=0 && newHeight>=0) {
			VirtualScreen.instance.doUpdateBounds=false;
			ScreenShareRTMPT.instance.jVScreenYSpin.setValue(newYPosition);
			ScreenShareRTMPT.instance.jVScreenHeightSpin.setValue(newHeight);
			VirtualScreen.instance.doUpdateBounds=true;
			VirtualScreen.instance.updateVScreenBounds();
			VirtualScreen.instance.calcRescaleFactors();
		}

	}

}
