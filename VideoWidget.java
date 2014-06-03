package org.expeditee.items.widgets;

import java.awt.GridLayout;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import org.expeditee.items.Text;

/**
 * Allows entering password data in non-visible form
 * TODO: encrypt this before storing it
 * @author jts21
 */
public class VideoWidget extends InteractiveWidget {

	
	
	public VideoWidget(Text source, String[] args) {
		super(source, new VideoPanel(), 500, 600, 500, 600);
		final VideoPanel p = (VideoPanel) super._swingComponent;
	    final Timer t = new Timer();
	    t.schedule(new TimerTask()
	    {
	    	@Override
	    	public void run()
	    	{
	    		p.setVideoStream();
	    		t.cancel();
	    	}
	    },3000);
		
	}

	@Override
	protected String[] getArgs() {
	return new String[0];
	}
	
	
}
