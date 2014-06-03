package org.expeditee.items.widgets;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.util.ArrayList;

import javax.swing.JPanel;

import uk.co.caprica.vlcj.binding.LibVlc;
import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.runtime.RuntimeUtil;
import uk.co.caprica.vlcj.test.basic.PlayerControlsPanel;
import uk.co.caprica.vlcj.test.basic.PlayerVideoAdjustPanel;

import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;


public class VideoPanel extends JPanel{

	//Class-scope variables
	String CAMERA_URL = "qtcapture://0x1a11000005ac8509";
	EmbeddedMediaPlayerComponent mediaPlayerComponent;
	EmbeddedMediaPlayer embeddedMediaPlayer;
	Canvas videoSurface;
	
	//Inner Panels
	PlayerControlsPanel controlsPanel;
    PlayerVideoAdjustPanel videoAdjustPanel;
    
	public VideoPanel()
	{
		//import and initialize project libraries
		initLibs();
		//Construct the panel
		buildPanel();
	}
	
	private void initLibs()
	{
		//System.loadLibrary("jawt");
		NativeLibrary.addSearchPath(
	           RuntimeUtil.getLibVlcLibraryName(), "/Applications/VLC/VLC.app/Contents/MacOS/lib"
	       );
	       Native.loadLibrary(RuntimeUtil.getLibVlcLibraryName(), LibVlc.class);
	      
	}
	private boolean buildPanel()
	{
		try{
		    //Create embedded player
			mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
		    embeddedMediaPlayer = mediaPlayerComponent.getMediaPlayer();

		    buildVideoSurface();
		    setVLCPreferences();
		   
		    
		    setLayout();		  
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}
	public void setVideoStream()
	{
    	embeddedMediaPlayer.playMedia("qtcapture://0x1a11000005ac8509"); 
	}
	private void buildVideoSurface() throws Exception
	{
	    //Create video surface
	    videoSurface = new Canvas();
	    videoSurface.setBackground(Color.black);
	    videoSurface.setSize(800, 600);
	    videoSurface.setVisible(true);
	}
	private void setVLCPreferences()
	{
	    //Setting up VLC
	    ArrayList<String> vlcArgs = new ArrayList<String>();
	    	//setting up initial args/VLC run settings
	    vlcArgs.add("--no-plugins-cache");
	    vlcArgs.add("--no-video-title-show");
	    vlcArgs.add("--no-snapshot-preview");
	    //initializing VLC interface
	    MediaPlayerFactory mediaPlayerFactory = new MediaPlayerFactory(vlcArgs.toArray(new String[vlcArgs.size()]));
	    mediaPlayerFactory.setUserAgent("vlcj test player");
	    embeddedMediaPlayer.setVideoSurface(mediaPlayerFactory.newVideoSurface(videoSurface));
	    embeddedMediaPlayer.setPlaySubItems(true);
	    
	}
	
	
	
	private void addControlsPanel()
	{
	    controlsPanel = new PlayerControlsPanel(embeddedMediaPlayer);
	}
	private void addVideoAdjustPanel()
	{
		videoAdjustPanel = new PlayerVideoAdjustPanel(embeddedMediaPlayer);
	}
	
	public void setLayout()
	{
		//Add controls to the main frame
	    setLayout(new BorderLayout()); //MainPanel should never be null
	    if(videoSurface != null)
	    	add(videoSurface, BorderLayout.CENTER);
	    if(controlsPanel != null)
	    	add(controlsPanel, BorderLayout.SOUTH);
	    if(videoAdjustPanel != null)
	    	add(videoAdjustPanel, BorderLayout.EAST);
	}
}
