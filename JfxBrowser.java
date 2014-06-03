package org.expeditee.items.widgets;

/*
 * JavaFX is not on the default java classpath until Java 8 (but is still included with Java 7), so your IDE will probably complain that the imports below can't be resolved. 
 * In Eclipse hitting'Proceed' when told 'Errors exist in project' should allow you to run Expeditee without any issues (although the JFX Browser widget will not display),
 * or you can just exclude JfxBrowser, WebParser and JfxbrowserActions from the build path.
 *
 * If you are using Ant to build/run, 'ant build' will try to build with JavaFX jar added to the classpath. 
 * If this fails, 'ant build-nojfx' will build with the JfxBrowser, WebParser and JfxbrowserActions excluded from the build path.
 */
import java.awt.Point;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.JFXPanel;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventDispatchChain;
import javafx.event.EventDispatcher;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebEvent;
import javafx.scene.web.WebView;
import javafx.util.Duration;
import netscape.javascript.JSObject;

import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.gui.FreeItems;
import org.expeditee.gui.MessageBay;
import org.expeditee.io.WebParser;
import org.expeditee.items.Item;
import org.expeditee.items.Picture;
import org.expeditee.items.Text;
import org.expeditee.settings.network.NetworkSettings;
import org.w3c.dom.NodeList;

import com.sun.javafx.scene.control.skin.TextFieldSkin;
import com.sun.javafx.scene.text.HitInfo;

/**
 * Web browser using a JavaFX WebView.
 * 
 * @author ngw8
 * @author jts21
 */
/**
 * @author ngw8
 * 
 */
public class JfxBrowser extends DataFrameWidget {
	
	private static final String BACK = "back";
	private static final String FORWARD = "forward";
	private static final String REFRESH = "refresh";
	private static final String CONVERT = "convert";
	private static final String VIDEO = "video";
	
	private JFXPanel _panel;
	private WebView _webView;
	private WebEngine _webEngine;
	private Button _forwardButton;
	private Button _backButton;
	private Button _stopButton;
	private Button _goButton;
	private Button _convertButton;
	private ToggleButton _readableModeButton;
	private Label _statusLabel;
	private FadeTransition _statusFadeIn;
	private FadeTransition _statusFadeOut;

	private TextField _urlField;
	private ProgressBar _urlProgressBar;
	private StackPane _overlay;
	
	private boolean _parserRunning;
	
	private MouseButton _buttonDownId = MouseButton.NONE;
	private MouseEvent _backupEvent = null;
	private static Field MouseEvent_x, MouseEvent_y;

	static {
		Platform.setImplicitExit(false);
		
		Font.loadFont(ClassLoader.getSystemResourceAsStream("org/expeditee/assets/resources/fonts/fontawesome/fontawesome-webfont.ttf"), 12);
		
		try {
	        MouseEvent_x = MouseEvent.class.getDeclaredField("x");
    		MouseEvent_x.setAccessible(true);
    		MouseEvent_y = MouseEvent.class.getDeclaredField("y");
    		MouseEvent_y.setAccessible(true);
		} catch (Exception e) {
	        e.printStackTrace();
        }
	}

	public JfxBrowser(Text source, final String[] args) {
		// Initial page is either the page stored in the arguments (if there is one stored) or the homepage
		super(source, new JFXPanel(), -1, 500, -1, -1, 300, -1);

		_panel = (JFXPanel) _swingComponent;

		// Quick & easy way of having a cancel function for the web parser.
		// Can't just have a JFX button, as the JFX thread is occupied with running JavaScript so it wouldn't receive the click event straight away
		_swingComponent.addKeyListener(new KeyListener() {
			
			@Override
			public void keyReleased(java.awt.event.KeyEvent e) {
				if(e.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE) {
					JfxBrowser.this.cancel();
				}
			}
			
			@Override
			public void keyPressed(java.awt.event.KeyEvent e) {				
			}
			
			@Override
			public void keyTyped(java.awt.event.KeyEvent e) {				
			}
		});
		
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				initFx((args != null && args.length > 0) ? args[0] : NetworkSettings.HomePage.get());
			}
		});
	}
	
	/**
	 * Sets up the browser frame. JFX requires this to be run on a new thread.
	 * 
	 * @param url
	 *            The URL to be loaded when the browser is created
	 */
	private void initFx(String url) {
		try {
			StackPane mainLayout = new StackPane();
			mainLayout.setId("jfxbrowser");
			
			VBox vertical = new VBox();
			HBox horizontal = new HBox();
			horizontal.getStyleClass().add("custom-toolbar");
			
			this._backButton = new Button("\uf060");
			this._backButton.setTooltip(new Tooltip("Back"));
			this._backButton.setMinWidth(Control.USE_PREF_SIZE);
			this._backButton.setMaxHeight(Double.MAX_VALUE);
			this._backButton.setFocusTraversable(false);
			this._backButton.getStyleClass().addAll("first", "fa");

			this._backButton.setDisable(true);
			
			this._forwardButton = new Button("\uf061");
			this._forwardButton.setTooltip(new Tooltip("Forward"));
			this._forwardButton.setMinWidth(Control.USE_PREF_SIZE);
			this._forwardButton.setMaxHeight(Double.MAX_VALUE);
			this._forwardButton.setFocusTraversable(false);
			this._forwardButton.getStyleClass().addAll("last", "fa");

			this._urlField = new TextField(url);
			this._urlField.getStyleClass().add("url-field");
			this._urlField.setMaxWidth(Double.MAX_VALUE);
			this._urlField.setFocusTraversable(false);
			
			this._stopButton = new Button("\uF00D");
			this._stopButton.setTooltip(new Tooltip("Stop loading the page"));
			this._stopButton.getStyleClass().addAll("url-button", "url-cancel-button", "fa");
			this._stopButton.setMinWidth(Control.USE_PREF_SIZE);
			this._stopButton.setMaxHeight(Double.MAX_VALUE);
			StackPane.setAlignment(this._stopButton, Pos.CENTER_RIGHT);
			this._stopButton.setFocusTraversable(false);

			this._goButton = new Button("\uf061");
			this._goButton.setTooltip(new Tooltip("Load the entered address"));
			this._goButton.getStyleClass().addAll("url-button", "url-go-button", "fa");
			this._goButton.setMinWidth(Control.USE_PREF_SIZE);
			this._goButton.setMaxHeight(Double.MAX_VALUE);
			StackPane.setAlignment(this._goButton, Pos.CENTER_RIGHT);
			this._goButton.setFocusTraversable(false);
			
			this._readableModeButton = new ToggleButton();
			this._readableModeButton.setMinWidth(Control.USE_PREF_SIZE);
			this._readableModeButton.setFocusTraversable(false);
			this._readableModeButton.setTooltip(new Tooltip("Switch to an easy-to-read view of the page"));
			
			Image readableModeIcon = new Image(ClassLoader.getSystemResourceAsStream("org/expeditee/assets/images/readableModeIcon.png"));
			this._readableModeButton.setGraphic(new ImageView(readableModeIcon));
			
			this._convertButton = new Button("Convert");
			this._convertButton.setMinWidth(Control.USE_PREF_SIZE);
			this._convertButton.setFocusTraversable(false);
						
			this._urlProgressBar = new ProgressBar();		
			this._urlProgressBar.getStyleClass().add("url-progress-bar");
			this._urlProgressBar.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
			
			// Status label that displays the URL when a link is hovered over
			this._statusLabel = new Label();
			this._statusLabel.getStyleClass().addAll("browser-status-label");
			this._statusLabel.setVisible(false);
			
			this._statusFadeIn = new FadeTransition();
			this._statusFadeIn.setDuration(Duration.millis(200));
			this._statusFadeIn.setNode(this._statusLabel);
			this._statusFadeIn.setFromValue(0);
			this._statusFadeIn.setToValue(1);
			this._statusFadeIn.setCycleCount(1);
			this._statusFadeIn.setAutoReverse(false);
			
			this._statusFadeOut = new FadeTransition();
			this._statusFadeOut.setDuration(Duration.millis(400));
			this._statusFadeOut.setNode(this._statusLabel);
			this._statusFadeOut.setFromValue(1);
			this._statusFadeOut.setToValue(0);
			this._statusFadeOut.setCycleCount(1);
			this._statusFadeOut.setAutoReverse(false);
			
			this._statusFadeOut.setOnFinished(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent arg0) {
					JfxBrowser.this._statusLabel.setVisible(false);
				}
			});
			

			StackPane urlbar = new StackPane();
			urlbar.getChildren().addAll(_urlProgressBar, this._urlField, this._stopButton, this._goButton);
						
			horizontal.getChildren().addAll(this._backButton, this._forwardButton, urlbar, this._readableModeButton, this._convertButton);

			HBox.setHgrow(this._backButton, Priority.NEVER);
			HBox.setHgrow(this._forwardButton, Priority.NEVER);
			HBox.setHgrow(this._convertButton, Priority.NEVER);
			HBox.setHgrow(this._readableModeButton, Priority.NEVER);
			HBox.setHgrow(urlbar, Priority.ALWAYS);
			
			HBox.setMargin(this._readableModeButton, new Insets(0, 5, 0, 5));
			HBox.setMargin(this._forwardButton, new Insets(0, 5, 0, 0));

			this._webView = new WebView();
			this._webView.setMaxWidth(Double.MAX_VALUE);
			this._webView.setMaxHeight(Double.MAX_VALUE);
			this._webEngine = this._webView.getEngine();
			
			this._urlProgressBar.progressProperty().bind(_webEngine.getLoadWorker().progressProperty());


			// Pane to hold just the webview. This seems to be the only way to allow the webview to be resized to greater than its parent's
			// size. This also means that the webview's prefSize must be manually set when the Pane resizes, using the event handlers below
			Pane browserPane = new Pane();
			browserPane.getChildren().addAll(_webView, this._statusLabel);
			
			HBox.setHgrow(browserPane, Priority.ALWAYS);
			VBox.setVgrow(browserPane, Priority.ALWAYS);

			browserPane.widthProperty().addListener(new ChangeListener<Object>() {

				@Override
				public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
					JfxBrowser.this._webView.setPrefWidth((Double) newValue);
				}
			});

			browserPane.heightProperty().addListener(new ChangeListener<Object>() {

				@Override
				public void changed(ObservableValue<?> observable, Object oldValue, Object newValue) {
					JfxBrowser.this._webView.setPrefHeight((Double) newValue);
					JfxBrowser.this._statusLabel.setTranslateY((Double) newValue - JfxBrowser.this._statusLabel.heightProperty().doubleValue());
				}
			});

			vertical.getChildren().addAll(horizontal, browserPane);

			this._overlay = new StackPane();
			this._overlay.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

			// Class for CSS styling
			this._overlay.getStyleClass().add("browser-overlay");

			// Don't show the overlay until processing the page
			this._overlay.setVisible(false);

			Label overlayLabel = new Label("Importing page to Expeditee...");

			ProgressIndicator prog = new ProgressIndicator();
			prog.setMaxSize(25, 25);

			this._overlay.getChildren().addAll(overlayLabel, prog);

			this._overlay.setAlignment(Pos.CENTER);
			StackPane.setMargin(overlayLabel, new Insets(-50, 0, 0, 0));
			StackPane.setMargin(prog, new Insets(50, 0, 0, 0));

			mainLayout.getChildren().addAll(vertical, this._overlay);
			
			final Scene scene = new Scene(mainLayout);
			
			final String cssPath = ClassLoader.getSystemResource("org/expeditee/assets/style/jfx.css").toString();
			
			scene.getStylesheets().add(cssPath);
		
			this._panel.setScene(scene);
			
			// Disable right click menu
			this._webView.setContextMenuEnabled(false);
			
			// Showing the status label when a link is hovered over
			this._webEngine.setOnStatusChanged(new EventHandler<WebEvent<String>>() {
				
				@Override
				public void handle(WebEvent<String> arg0) {
					if (arg0.getData() != null && hasValidProtocol(arg0.getData())) {
						JfxBrowser.this._statusLabel.setText(arg0.getData());
						
						JfxBrowser.this._statusFadeOut.stop();
						
						if(JfxBrowser.this._statusLabel.isVisible()) {
							// Don't play the fade in if the label is already partially visible
							JfxBrowser.this._statusLabel.setOpacity(1);
						} else {
							JfxBrowser.this._statusLabel.setVisible(true);
							JfxBrowser.this._statusFadeIn.play();
						}
					} else {
						JfxBrowser.this._statusFadeIn.stop();

						JfxBrowser.this._statusFadeOut.play();
					}
				}
			});
			
			
			final EventDispatcher initial = this._urlField.getEventDispatcher();
			
			this._urlField.setEventDispatcher(new EventDispatcher() {
				@Override
				public Event dispatchEvent(Event e, EventDispatchChain tail) {
					if (e instanceof MouseEvent) {
						MouseEvent m = (MouseEvent) e;
                        if (m.getButton() == MouseButton.SECONDARY && m.getEventType() == MouseEvent.MOUSE_RELEASED) {
                            e.consume();
                            JfxBrowser.this._urlField.getOnMouseReleased().handle(m);
                        }
                    }
                    return initial.dispatchEvent(e, tail);
				}
			});
			
			this._backButton.setOnAction(new EventHandler<ActionEvent>() {
				@Override
                public void handle(ActionEvent e) {
                    navigateBack();
                }
			});

			_forwardButton.setOnAction(new EventHandler<ActionEvent>() {
				@Override
                public void handle(ActionEvent e) {
                    navigateForward();
                }
			});
			
			this._stopButton.setOnAction(new EventHandler<ActionEvent>() {
				
				@Override
				public void handle(ActionEvent arg0) {
					JfxBrowser.this._webEngine.getLoadWorker().cancel();
				}
			});
			
			this._goButton.setOnAction(new EventHandler<ActionEvent>() {
				
				@Override
				public void handle(ActionEvent arg0) {
                    navigate(JfxBrowser.this._urlField.getText());
				}
			});
			
			this._readableModeButton.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent arg0) {
					if (arg0.getSource() instanceof ToggleButton) {
						ToggleButton source = (ToggleButton) arg0.getSource();
						
						// This seems backwards, but because the button's just been clicked, its state has already changed
						if(!source.isSelected()) {
							// Disable readable mode by refreshing the page
							JfxBrowser.this._webEngine.reload();
						} else {
							JfxBrowser.this.enableReadableMode();
						}
					}
				}
			});
			
			this._convertButton.setOnAction(new EventHandler<ActionEvent>() {
				@Override
                public void handle(ActionEvent e) {
					getFrameNew();
                }
			});

			this._urlField.setOnAction(new EventHandler<ActionEvent>() {
				@Override
                public void handle(ActionEvent e) {
                    navigate(JfxBrowser.this._urlField.getText());
                }
			});

			this._urlField.setOnKeyTyped(new EventHandler<KeyEvent>() {
				@Override
				public void handle(KeyEvent e) {
					// Hiding the cursor when typing, to be more Expeditee-like
					DisplayIO.setCursor(org.expeditee.items.Item.HIDDEN_CURSOR);
				}
			});

			this._urlField.setOnMouseMoved(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent e) {
					JfxBrowser.this._backupEvent = e;
					// make sure we have focus if the mouse is moving over us
					if(!JfxBrowser.this._urlField.isFocused()) {
						JfxBrowser.this._urlField.requestFocus();
					}
					// Checking if the user has been typing - if so, move the cursor to the caret position
					if (DisplayIO.getCursor() == Item.HIDDEN_CURSOR) {
						DisplayIO.setCursor(org.expeditee.items.Item.TEXT_CURSOR);
						DisplayIO.setCursorPosition(getCoordFromCaret(JfxBrowser.this._urlField));
					} else {
						// Otherwise, move the caret to the cursor location
						// int x = FrameMouseActions.getX() - JfxBrowser.this.getX(), y = FrameMouseActions.getY() - JfxBrowser.this.getY();
						JfxBrowser.this._urlField.positionCaret(getCaretFromCoord(JfxBrowser.this._urlField, e));
					}
				}
			});
			
			this._urlField.setOnMouseEntered(new EventHandler<MouseEvent>() {
				@Override
                public void handle(MouseEvent arg0) {
					JfxBrowser.this._urlField.requestFocus();
				}
			});
			
			this._urlField.setOnMouseExited(new EventHandler<MouseEvent>() {
				@Override
                public void handle(MouseEvent arg0) {
					JfxBrowser.this._webView.requestFocus();
                }
			});
			
			this._urlField.setOnMouseDragged(new EventHandler<MouseEvent>() {
				@Override
				public void handle(MouseEvent e) {
					TextFieldSkin skin = (TextFieldSkin) JfxBrowser.this._urlField.getSkin();
					
					if (!JfxBrowser.this._urlField.isDisabled()) {
                        if (JfxBrowser.this._buttonDownId == MouseButton.MIDDLE || JfxBrowser.this._buttonDownId == MouseButton.SECONDARY) {
                            if (!(e.isControlDown() || e.isAltDown() || e.isShiftDown() || e.isMetaDown())) {
                                skin.positionCaret(skin.getIndex(e), true);
                            }
                        }
                    }
				}
			});
			
			this._urlField.focusedProperty().addListener(new ChangeListener<Boolean>() {
				@Override
                public void changed(ObservableValue<? extends Boolean> property, Boolean oldValue, Boolean newValue) {
					if(newValue.booleanValue()) {
						DisplayIO.setCursor(org.expeditee.items.Item.TEXT_CURSOR);
					} else {
						// Restoring the standard cursor, since it is changed to a text cursor when focus is gained
						DisplayIO.setCursor(org.expeditee.items.Item.DEFAULT_CURSOR);
					}
                }
			});

			this._urlField.setOnMouseReleased(new EventHandler<MouseEvent>() {
				@Override
                public void handle(MouseEvent e) {
                    JfxBrowser.this._buttonDownId = MouseButton.NONE;

					Text item;

					// If nothing is selected, then select all the text so that it will be copied/moved
					if (JfxBrowser.this._urlField.getSelectedText() == null || JfxBrowser.this._urlField.getSelectedText().length() == 0) {
						JfxBrowser.this._urlField.selectAll();
					}

					if (e.getButton() == MouseButton.SECONDARY) {
						// Right mouse button released, so copy the selection (i.e. don't remove the original)
						item = DisplayIO.getCurrentFrame().createNewText(JfxBrowser.this._urlField.getSelectedText());
						FrameMouseActions.pickup(item);
					} else if (e.getButton() == MouseButton.MIDDLE) {
						// Middle mouse button released, so copy the selection then remove it from the URL field
						item = DisplayIO.getCurrentFrame().createNewText(JfxBrowser.this._urlField.getSelectedText());
						JfxBrowser.this._urlField.setText(
								JfxBrowser.this._urlField.getText().substring(0, JfxBrowser.this._urlField.getSelection().getStart())
								+ JfxBrowser.this._urlField.getText().substring(JfxBrowser.this._urlField.getSelection().getEnd(),
										JfxBrowser.this._urlField.getText().length()));

						FrameMouseActions.pickup(item);
					}
                }
			});
			
			this._urlField.setOnMousePressed(new EventHandler<MouseEvent>() {
				@Override
                public void handle(MouseEvent e) {
					JfxBrowser.this._buttonDownId = e.getButton();
				}
			});

			this._webEngine.getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {

				@Override
				public void changed(ObservableValue<? extends State> ov, State oldState, State newState) {

					switch (newState) {
					case READY: // READY
						// MessageBay.displayMessage("WebEngine ready");
						break;
					case SCHEDULED: // SCHEDULED
						// MessageBay.displayMessage("Scheduled page load");
						break;
					case RUNNING: // RUNNING
						System.out.println("Loading page!");

						// Updating the URL bar to display the URL of the page being loaded
						JfxBrowser.this._urlField.setText(JfxBrowser.this._webEngine.getLocation());
						
						// Removing the style from the progress bar that causes it to hide
						JfxBrowser.this._urlProgressBar.getStyleClass().remove("completed");

						JfxBrowser.this._stopButton.setVisible(true);
						JfxBrowser.this._goButton.setVisible(false);
						
						if (JfxBrowser.this._webEngine.getHistory().getCurrentIndex() + 1 >= JfxBrowser.this._webEngine.getHistory().getEntries().size()) {
							JfxBrowser.this._forwardButton.setDisable(true);
						} else {
							JfxBrowser.this._forwardButton.setDisable(false);
						}

						// Unless the history is empty (i.e. this is the first page being loaded), enable the back button.
						// The only time the back button should be disbaled is on the first page load (which this statement deals with)
						// and if the user has just hit the back button taking them to the first page in the history (dealt with in the
						// navigateBack method)
						if (JfxBrowser.this._webEngine.getHistory().getEntries().size() > 0) {
							JfxBrowser.this._backButton.setDisable(false);
						}

						JfxBrowser.this._convertButton.setDisable(true);
						JfxBrowser.this._readableModeButton.setDisable(true);

						break;
					case SUCCEEDED: // SUCCEEDED
						MessageBay.displayMessage("Finished loading page");
						JfxBrowser.this._urlProgressBar.getStyleClass().add("completed");
						
						if(JfxBrowser.this._readableModeButton.isSelected()) {
							JfxBrowser.this.enableReadableMode();
						}

					case CANCELLED: // CANCELLED
						JfxBrowser.this._convertButton.setDisable(false);
						JfxBrowser.this._readableModeButton.setDisable(false);
						JfxBrowser.this._stopButton.setVisible(false);
						JfxBrowser.this._goButton.setVisible(true);
						break;
					case FAILED: // FAILED
						MessageBay.displayMessage("Failed to load page");
						JfxBrowser.this._stopButton.setVisible(false);
						JfxBrowser.this._goButton.setVisible(true);
						break;
					}
				}
			});
			
			// Captures mouse click events on webview to enable expeditee like behavior for JavaFX browser.
			this._webView.setOnMouseClicked(new EventHandler<javafx.scene.input.MouseEvent>() {
				@Override
				public void handle(javafx.scene.input.MouseEvent e) {
					if(e.getButton() == MouseButton.SECONDARY) {
						// Gets text currently selected in webview
						String selection = (String) JfxBrowser.this._webEngine.executeScript("window.getSelection().toString()");

						// If no text is selected, see if an image is under the cursor
						if (selection.length() == 0) {
							JSObject window = (JSObject) JfxBrowser.this._webEngine.executeScript("window");
							Object o = JfxBrowser.this._webEngine.executeScript("document.elementFromPoint(" + e.getX() + "," + e.getY() + ");");
							
							if(o instanceof org.w3c.dom.Node) {
								org.w3c.dom.Node node = (org.w3c.dom.Node) o;
								JSObject style = (JSObject) window.call("getComputedStyle", node);
								
								if(node.getNodeName().toLowerCase().equals("img") ||
										((String) style.call("getPropertyValue", "background-image")).startsWith("url")) {
    								
									try {
    									JSObject bounds = (JSObject) ((JSObject) node).call("getBoundingClientRect", new Object[] {});
    									float width = Float.valueOf(bounds.getMember("width").toString());
    									float height = Float.valueOf(bounds.getMember("height").toString());
    									
    									Picture pic;
    									
    									if (((String) style.call("getPropertyValue", new Object[] { "background-image" })).startsWith("url(")) {
    										pic = WebParser.getBackgroundImageFromNode(node, style, DisplayIO.getCurrentFrame(), null,
    											FrameMouseActions.getX(), FrameMouseActions.getY(), width, height);
    										
    									} else {
    										String imgSrc;
    										if(node.getNodeName().toLowerCase().equals("img") && 
    												(imgSrc = ((JSObject) node).getMember("src").toString()) != null) {
    											pic = WebParser.getImageFromUrl(imgSrc, null, DisplayIO.getCurrentFrame(),
    													FrameMouseActions.getX(), FrameMouseActions.getY(), (int) width, null, null, null, null, null, 0, 0);
    										} else {
    											return;
    										}
    									}
    									
    									String linkUrl;
    									
    									// Check the image and its immediate parent for links
    									if ((node.getNodeName().toLowerCase().equals("a") && (linkUrl = (String) ((JSObject)node).getMember("href")) != null)
    											|| (node.getParentNode().getNodeName().toLowerCase().equals("a") && (linkUrl = (String)((JSObject)node.getParentNode()).getMember("href")) != null)) {
	    									
    										if(hasValidProtocol(linkUrl)) {
	    										pic.getSource().setAction("createFrameWithBrowser "  + linkUrl);
	    									}
    									}
    									
    									pic.setXY(FrameMouseActions.getX(), FrameMouseActions.getY());
                                		FrameMouseActions.pickup(pic);
                            		} catch (Exception e1) {
    	                                // TODO Auto-generated catch block
    	                                e1.printStackTrace();
                                    }
									
								} else if(node.getNodeName().toLowerCase().equals("video")) {
									String src = ((JSObject)node).getMember("src").toString();
									if(src == null || src.trim().length() == 0) {
										NodeList children = node.getChildNodes();
										for(int i = 0; i < children.getLength(); i++) {
											org.w3c.dom.Node child = children.item(i);
											if(child.getNodeName().toLowerCase().equals("source")) {
												src = ((JSObject)child).getMember("src").toString();
												if(src != null && src.trim().length() > 0) {
													break;
												}
											}
											
										}
										if(src == null || src.trim().length() == 0) {
											return;
										}
									}
									Text t = new Text("@iw: org.expeditee.items.widgets.jfxmedia "
											+ ((JSObject)node).getMember("width")
											+ ((JSObject)node).getMember("height")
											+ ":" + src);
									t.setParent(DisplayIO.getCurrentFrame());
									t.setXY(FrameMouseActions.getX(), FrameMouseActions.getY());
									JfxMedia media = new JfxMedia(t, new String[] { src });
									FrameMouseActions.pickup(media.getItems());
									
								} else if(node.getNodeName().toLowerCase().equals("a") && ((JSObject)node).getMember("href") != null) {
									// If a link is right clicked, copy the text content and give it an action to create 
									// a new frame containing a browser pointing to the linked page
									Text t = DisplayIO.getCurrentFrame().createNewText(((String) ((JSObject)node).getMember("textContent")).trim());
									t.addAction("createFrameWithBrowser " + (String) ((JSObject)node).getMember("href"));
									t.setXY(FrameMouseActions.getX(), FrameMouseActions.getY());
									FrameMouseActions.pickup(t);
								}
							}
						} else {
							// Copy text and attach to cursor
							Text t = DisplayIO.getCurrentFrame().createNewText(selection);
							t.setXY(FrameMouseActions.getX(), FrameMouseActions.getY());
							FrameMouseActions.pickup(t);
						}
					}
				}
			});

			this.navigate(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void navigate(String url) {
		final String actualURL;

		// check if protocol is missing
		if (!hasValidProtocol(url)) {
			// check if it's a search
			int firstSpace = url.indexOf(" ");
			int firstDot = url.indexOf(".");
			int firstSlash = url.indexOf('/');
			int firstQuestion = url.indexOf('?');
			int firstSQ;
			if(firstSlash == -1) {
				firstSQ = firstQuestion;
			} else if(firstQuestion == -1) {
				firstSQ = firstSlash;
			} else {
				firstSQ = -1;
			}
			if(firstDot <= 0 ||                                        // no '.' or starts with '.' -> search
			        (firstSpace != -1 && firstSpace < firstDot + 1) || // ' ' before '.'            -> search
			        (firstSpace != -1 && firstSpace < firstSQ)) {      // no '/' or '?'             -> search
				// make it a search
				actualURL = NetworkSettings.SearchEngine.get() + url;
			} else {
				// add the missing protocol
				actualURL = "http://" + url;
			}
		} else {
			actualURL = url;
		}
		System.out.println(actualURL);
		try {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					try {
						JfxBrowser.this._webEngine.load(actualURL);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Navigates JfxBrowser back through history. If end of history reached the user is notified via the MessageBay.
	 * Max size of history is 100 by default.
	 */
	public void navigateBack() {
		try {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					try {
						JfxBrowser.this._webEngine.getHistory().go(-1);

						// Disable the back button if we're at the start of history
						if (JfxBrowser.this._webEngine.getHistory().getCurrentIndex() <= 0) {
							JfxBrowser.this._backButton.setDisable(true);
						} else {
							JfxBrowser.this._backButton.setDisable(false);
						}

						FreeItems.getInstance().clear();
					} catch (IndexOutOfBoundsException e) {
						MessageBay.displayMessage("Start of History");
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Navigates JfxBrowser forward through history. If end of history reached the user is notified via the MessageBay.
	 * Max size of history is 100 by default.
	 */
	public void navigateForward() {
		try {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				try {
					JfxBrowser.this._webEngine.getHistory().go(1);
					FreeItems.getInstance().clear();
				} catch (IndexOutOfBoundsException e) {
					MessageBay.displayMessage("End of History");
					}
			}
		});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Refreshes webview by reloading the page.
	 */
	@Override
	public void refresh() {
		try {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				try {
					JfxBrowser.this._webEngine.reload();
					FreeItems.getInstance().clear();
					MessageBay.displayMessage("Page Reloading");
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	/**
	 * Traverses DOM an turns elements into expeditee items.
	 */
	public void getFrame() {
		try {
			WebParser.parsePageSimple(this, _webEngine, _webView, DisplayIO.getCurrentFrame());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void getFrameNew() {

		this._parserRunning = true;
		
		try {
			// hack to make sure we don't try parsing the page from within the JavaFX thread,
			// because doing so causes deadlock
			new Thread(new Runnable() {
				@Override
				public void run() {
					WebParser.parsePageSimple(JfxBrowser.this, JfxBrowser.this._webEngine, JfxBrowser.this._webView, DisplayIO.getCurrentFrame());
				}
			}).start();
		} catch (Exception e) {
			e.printStackTrace();
			this._parserRunning = false;
		}
	}

	/**
	 * Used to drop text items onto JfxBrowser widget. Does nothing if a text item is not attached to cursor. <br>
	 * "back" -> navigates back a page in browser's session history <br>
	 * "forward" -> navigates forward a page in browser's session history <br>
	 * "refresh" -> reloads current page <br>
	 * "getFrame" -> attempts to parse page into an expeditee frame <br>
	 * url -> all other text is assumed to be a url which browser attempts to navigate to
	 * 
	 * @return Whether a JfxBrowser specific event is run.
	 * 
	 */
	@Override
	public boolean ItemsLeftClickDropped() {
		Text carried = null;
		if ((carried = FreeItems.getTextAttachedToCursor()) == null) { // fails if no text is attached to cursor.
			return false;
		}
		
		if (carried.getText().toLowerCase().equals(BACK)) {
			navigateBack();
		} else if (carried.getText().toLowerCase().equals(FORWARD)) {
			navigateForward();
		} else if (carried.getText().toLowerCase().equals(REFRESH)) {
			refresh();
		} else if (carried.getText().toLowerCase().equals(CONVERT)) {
			getFrame();
		} else {
			String text = carried.getText().trim();
			this.navigate(text);
			FreeItems.getInstance().clear();
		}

		return true;
	}

	/**
	 * Used to enable expeditee like text-widget interaction for middle mouse clicks. Does nothing if a text item is not attached to cursor.
	 * @return false if a text-widget interaction did not occur, true if a text-widget interaction did occur.
	 */
	@Override
	public boolean ItemsMiddleClickDropped() {
		if(ItemsRightClickDropped()) {
			FreeItems.getInstance().clear();						// removed held text item - like normal expeditee middle click behaviour.
			return true;
		}
		return false;
	}
	
	/**
	 * Used to enable expeditee like text-widget interaction for right mouse clicks. Does nothing if a text item is not attached to cursor.
	 * @return false if a text-widget interaction did not occur, true if a text-widget interaction did occur.
	 */
	@Override
	public boolean ItemsRightClickDropped() {
		Text t = null;
		if((t = FreeItems.getTextAttachedToCursor()) == null) {	// fails if no text item is attached to the cursor.
			return false;
		}

		final int x = FrameMouseActions.getX() - this.getX(), y = FrameMouseActions.getY() - this.getY();
		if(!this._urlField.getBoundsInParent().contains(x, y)) {
			// fails if not clicking on urlField
			return false;
		}

		final String insert = t.getText();
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				// Inserts text in text item into urlField at the position of the mouse.
        		String s = JfxBrowser.this._urlField.getText();
        		int index = getCaretFromCoord(JfxBrowser.this._urlField, getMouseEventForPosition(JfxBrowser.this._backupEvent, JfxBrowser.this._urlField, x, y));
        		if(index < s.length()) {
        			s = s.substring(0, index) + insert + s.substring(index);
        		} else {
        			s = s + insert;
        		}
        		JfxBrowser.this._urlField.setText(s);
			}
		});

		return true;
	}
	
	/**
	 * Shows/hides a message reading 'Importing page' over the widget
	 * 
	 * @param visible
	 */
	public void setOverlayVisible(boolean visible) {
		this._overlay.setVisible(visible);
	}

	public void setScrollbarsVisible(boolean visible) {
		if (!visible) {
			this._webView.getStyleClass().add("scrollbars-hidden");
		} else {
			this._webView.getStyleClass().remove("scrollbars-hidden");
		}
	}

	/**
	 * Sets the size of the webview element of the widget
	 * 
	 * @param width
	 * @param height
	 */
	public void setWebViewSize(double width, double height) {
		this._webView.setPrefSize(width, height);
	}

	/**
	 * Resizes the webview back to the size of its parent element
	 */
	public void rebindWebViewSize() {
		this._webView.getParent().resize(0, 0);
	}

	@Override
	protected String[] getArgs() {
		String[] r = null;
		if (this._webView != null) {
			try {
				r = new String[] { this._webEngine.getLocation() };
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return r;
	}
	
	private Point getCoordFromCaret(TextField text) {
		TextFieldSkin skin = (TextFieldSkin) text.getSkin();
		
		Point2D onScene = text.localToScene(0, 0);
		
		double x = onScene.getX() + JfxBrowser.this.getX();// - org.expeditee.gui.Browser._theBrowser.getOrigin().x;
		double y = onScene.getY() + JfxBrowser.this.getY();// - org.expeditee.gui.Browser._theBrowser.getOrigin().y;
		
		Rectangle2D cp = skin.getCharacterBounds(text.getCaretPosition());
		
		return new Point((int) (cp.getMinX() + x), (int) (cp.getMinY() + y));
	}
	
	private int getCaretFromCoord(TextField text, MouseEvent e) {
		//TextFieldSkin skin = (TextFieldSkin) text.getSkin();
		//HitInfo hit = skin.getIndex(e);
		return 0;//hit.getInsertionIndex();
	}
	
	/**
	 * @param src The MouseEvent to clone
	 * @param node The node the position will be relative to
	 * @param x The position in Expeditee space
	 * @param y The position in Expeditee space
	 * @return A fake MouseEvent for a specific position relative to a Node
	 */
	private MouseEvent getMouseEventForPosition(MouseEvent src, Node node, int x, int y) {
		MouseEvent dst = (MouseEvent) ((Event) src).copyFor(null, null);
		try {
	        MouseEvent_x.set(dst, x - node.localToScene(0, 0).getX());
	        MouseEvent_y.set(dst, y - node.localToScene(0, 0).getY());
        } catch (Exception e) {
	        e.printStackTrace();
        }
		return dst;
	}
	
	private void enableReadableMode() {
		String readabilityJs;
		String readabilityCss;

		readabilityJs = readResourceFile("org/expeditee/assets/scripts/browserreadablemode/readability.min.js");
		readabilityCss = readResourceFile("org/expeditee/assets/scripts/browserreadablemode/readability.css");
			
		JSObject window = (JSObject)JfxBrowser.this._webEngine.executeScript("window");
		window.setMember("readabilityJs", readabilityJs);
		window.setMember("readabilityCss", readabilityCss);
		
		JfxBrowser.this._webEngine.executeScript(""
				+ "javascript:("
					+ "function(){	"
						+ "readStyle = '';"
						+ "readSize = 'size-medium';"
						+ "readMargin = 'margin-medium';"
						+ "_readability_script = document.createElement('SCRIPT');"
						+ "_readability_script.type = 'text/javascript';"
						+ "_readability_script.appendChild(document.createTextNode(readabilityJs));"
						+ "document.head.appendChild(_readability_script);"
						+ "readability.init();"
						
						// readability.init() removes all css, so have to add the stylesheet after init
						+ "_readability_css = document.createElement('STYLE');"
						+ "_readability_css.type='text/css';"
						+ "_readability_css.appendChild(document.createTextNode(readabilityCss));"
						+ "document.head.appendChild(_readability_css);"
						
						// Font Awesome CSS from the Bootstrap CDN
						+ "_fontawesome_css = document.createElement('LINK');"
						+ "_fontawesome_css.rel = 'stylesheet';	"
						+ "_fontawesome_css.href = '//netdna.bootstrapcdn.com/font-awesome/4.0.3/css/font-awesome.css';"
						+ "_fontawesome_css.type = 'text/css';"
						+ "document.head.appendChild(_fontawesome_css);"
					+ "}"
				+ ")();"
		   );
	}
	
	/**
	 * Reads a resource file into a string
	 * @return The contents of the specified file as a string
	 */
	private static String readResourceFile(String path) {
		BufferedReader bufferedReader = null;
		StringBuilder stringBuilder = new StringBuilder();
 
		String line;
		
		try {
			bufferedReader = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(path)));

			while ((line = bufferedReader.readLine()) != null) {
				stringBuilder.append(line + "\n");
			}
 
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (bufferedReader != null) {
				try {
					bufferedReader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
 
		return stringBuilder.toString();
	}
	
	/**
	 * Checks if a URL string starts with a protocol that can be loaded by the webview
	 * @param url URL string to check
	 * @return
	 */
	private static boolean hasValidProtocol(String url) {
		String urlLower = url.toLowerCase();
		
		// check if protocol is present
		return (urlLower.startsWith("http://") || url.startsWith("https://") || urlLower.startsWith("ftp://") || urlLower.startsWith("file://"));
	}
	
	/**
	 * @return Whether the parser is running. If this is true then the parser is running, 
	 *  however even if it is false, the parser may still be running (but it has been requested to stop)
	 */
	public boolean isParserRunning() {
		return this._parserRunning;
	}
	
	/**
	 * Should be called when the web parser has finished converting a page
	 */
	public void parserFinished() {
		this._parserRunning = false;
	}
	
	/**
	 * Cancels the current action being performed by the browser, such as loading a page or converting a page
	 */
	public void cancel() {
		if(isParserRunning()) {
			this._parserRunning = false;
		} else {
			Platform.runLater(new Runnable() {
				
				@Override
				public void run() {
					JfxBrowser.this._webEngine.getLoadWorker().cancel();					
				}
			});
		}
	}
}
