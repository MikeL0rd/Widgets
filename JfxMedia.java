package org.expeditee.items.widgets;

import java.io.File;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;

import org.expeditee.items.Text;

public class JfxMedia extends DataFrameWidget {

	protected JFXPanel _panel;
	protected MediaView _mediaView;
	protected String _media;
	
	public JfxMedia(Text source, String[] args) {
		super(source, new JFXPanel(), -1, 500, -1, -1, 300, -1);
		
		_panel = (JFXPanel) _swingComponent;
		_media = (args != null && args.length > 0) ? args[0] : "";
		System.out.println(_media);
		
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				initFx();
			}
		});
	}
	
	private void initFx() {
        try {
            Scene scene = new Scene(new Group());
            _panel.setScene(scene);
            
            Media media;
            File f;
            if((f = new File(this._media)).exists()) {
            	media = new Media(f.toURI().toString());
            } else {
            	media = new Media(this._media);
            }
            
            // Create the player
            final MediaPlayer mediaPlayer = new MediaPlayer(media);
            
            System.out.println("Initial: " + mediaPlayer.getStatus());
            mediaPlayer.statusProperty().addListener(new ChangeListener<MediaPlayer.Status>() {
                @Override
                public void changed(ObservableValue<? extends MediaPlayer.Status> observable, MediaPlayer.Status oldStatus, MediaPlayer.Status curStatus) {
                    System.out.println("Current: " + curStatus);
                }
            });
                    
            if (mediaPlayer.getError() != null) {
                System.out.println("Initial Error: " + mediaPlayer.getError());
                mediaPlayer.getError().printStackTrace();
            }
                            
            mediaPlayer.setOnError(new Runnable() {
                @Override public void run() {
                    System.out.println("Current Error: " + mediaPlayer.getError());
                    mediaPlayer.getError().printStackTrace();
                }
            });
            
            // Create the view and add it to the Scene.
            _mediaView = new MediaView(mediaPlayer);
            ((Group) scene.getRoot()).getChildren().add(_mediaView);
            mediaPlayer.play();
        } catch (Exception e) {
        	e.printStackTrace();
        }
	}

	@Override
	protected String[] getArgs() {
		return new String[] { this._media };
	}
}
