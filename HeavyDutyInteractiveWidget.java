package org.expeditee.items.widgets;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.expeditee.gui.Browser;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FreeItems;
import org.expeditee.items.Item;
import org.expeditee.items.ItemParentStateChangedEvent;
import org.expeditee.items.Text;
import org.expeditee.taskmanagement.EntityLoadManager;
import org.expeditee.taskmanagement.EntitySaveManager;
import org.expeditee.taskmanagement.LoadableEntity;
import org.expeditee.taskmanagement.SaveableEntity;

/**
 * Interactive widgets that may require loading prior to usage, and/or saving.
 * 
 * HeavyDutyInteractiveWidget provides a conventional asynchronous loading and saving scheme.
 * 
 * When a loadable interactive widget is created. It is at a pending state - waiting to load
 * until expeditee allocates a thread to begin loading. The swing component is set as not-visible.
 * 
 * At the the loading stage, a loadable widget proccesses its load task and renders a
 * load message/bar.
 * 
 * Once loaded, the widget is like any other widget - and its visible state will be set as visible.
 * 
 * When a HeavyDutyInteractiveWidget becomes visible / is anchored, it will register itself for saving at the next
 * save point according to SaveEntityManager. Heavey duty widgets will only save at the save points 
 * if the widget still belongs to a frame (i.e. is not removed) and is anchored.
 * 
 * Heavey duty widgets can also have an expiry on how long they should stay cached for. If they do have an 
 * expiry then it they unload once expired, and will re-load once in view again. Must take care for
 * unloadable widgets because you must check your unloadable elements to whether they are unloaded or not
 * every time your widget accessess them.
 * 
 * When a heavy duty widget is deleted - any data consuming memory can be dumped temporarily if needed.
 * 
 * @author Brook Novak
 *
 */
public abstract class HeavyDutyInteractiveWidget extends InteractiveWidget implements LoadableEntity, SaveableEntity {
	
	/** load finished */
	public static final float LOAD_STATE_COMPLETED = 2.0f; 
	
	/** waiting to load. Could have been loaded previously but expired. */
	public static final float LOAD_STATE_PENDING = 3.0f;
	
	/** load failed */
	public static final float LOAD_STATE_FAILED = 4.0f; 
	
	/** load was cancelled and still requires loading */
	public static final float LOAD_STATE_INCOMPLETED = 5.0f; 
	
	// GUI Stuff
	private static final String DEFAULT_LOAD_MESSAGE = "Loading";
	private static final String PENDING_MESSAGE = "Pending";
	
	private static final Color LOAD_BAR_PROGRESS_COLOR = new Color(103, 171, 203);
	private static final Color LOAD_BAR_INDETERM_COLOR = Color.ORANGE;
	private static final Color LOAD_BAR_HIGHLIGHT_COLOR = new Color(240, 240, 240);
	private static final Color LOAD_SCREEN_COLOR = Color.LIGHT_GRAY;
	private static final Color LOAD_SCREEN_COLOR_FREESPACE = FREESPACE_BACKCOLOR;
	
	private static final Font LOAD_NORMAL_FONT = new Font("Arial", Font.BOLD, 12);
	private static final Font LOAD_INDERTMIN_FONT = new Font("Arial", Font.ITALIC, 12);
	
	private static final int BAR_HOROZONTIAL_MARGIN = 20;
	private static final int BAR_HEIGHT = 40;
	
	/** Unifies state transitions to a single thread. */
	private HDWEventDispatcher eventDispatcher = null;
	private LinkedList<HDWEvent> queuedEvents = new LinkedList<HDWEvent>();
	
	// Model data
	private float loadState; // handled by constructors and stateProccessor
	private String screenMessage = DEFAULT_LOAD_MESSAGE;
	private boolean hasCancelledBeenRequested;
	private boolean isExpired = false;
	private int cacheDepth = -1;

	/**
	 * Constructor.
	 * 
	 * @param source
	 * 
	 * @param component
	 * 
	 * @param minWidth
	 * 
	 * @param maxWidth
	 * 
	 * @param minHeight
	 * 
	 * @param maxHeight
	 * 
	 * @param cacheDepth
	 * 		Less or equal to zero for no cache management - i.e. use expeditees frame caching.
	 * 		Positive to limit cache; where the widget will be explicity unloaded
	 * 		({@link HeavyDutyInteractiveWidget#unloadWidgetData()}) once the user has traversed
	 * 		cacheDepth frames without seeing this instance.
	 * 		The general rule is: the more memory your widget may take, the smaller the cache depth.
	 * 
	 * @param skipLoad
	 * 		If true, the load state will be set to completed and the widget will not go through 
	 * 		the loading proccess. Otherwise if true then the widget will go the loading phase.
	 */
	protected HeavyDutyInteractiveWidget(Text source, JComponent component,
			int minWidth, int maxWidth, int minHeight, int maxHeight, int cacheDepth, boolean skipLoad) {
		super(source, component, minWidth, maxWidth, minHeight, maxHeight);
		
		this.cacheDepth = cacheDepth;
		
		if (skipLoad) {
			loadState = LOAD_STATE_COMPLETED;
		} else {
			loadState = LOAD_STATE_PENDING;
			component.setVisible(false); // not allowed to interact with it yet
			component.setEnabled(false);
		}
	}
	
	/**
	 * Chained constructor.
	 * 
	 * @param source
	 * @param component
	 * @param minWidth
	 * @param maxWidth
	 * @param minHeight
	 * @param maxHeight
	 */
	protected HeavyDutyInteractiveWidget(Text source, JComponent component,
			int minWidth, int maxWidth, int minHeight, int maxHeight, int cacheDepth) {
		this(source, component, minWidth, maxWidth, minHeight, maxHeight, cacheDepth, false);
	}
	
	/**
	 * Updates the percentage of the load. Should only call when in load phase.
	 * 
	 * @param percent 
	 * 		Must be between 0.0 and 1.0 inclusive for percentage. Or neagtive if indeterminant.
	 * 
	 * @throws IllegalArgumentException
	 * 		If percent is larger than 1.0
	 * 
	 * @throws IllegalStateException
	 * 		Load is not in progress
	 * 		
	 */
	protected final void updateLoadPercentage(float percent) {
		if (percent > 1.0f) 
			throw new IllegalArgumentException("loadState is larger than 1.0");
		
		else if (!isInLoadProgress() ||
				eventDispatcher == null ||
				!eventDispatcher.isAlive()) 
			throw new IllegalStateException("Load is not in progress");
		
		// Assuming that this is called from eventDispatcher.
		eventDispatcher.setLoadState(percent, false);
	}
	
	
	/**
	 * @return The current load state.
	 */
	protected float getLoadState() {
		return loadState;
	}
	
	/**
	 * @return True if in a loading phase.
	 */
	protected boolean isInLoadProgress() {
		return loadState <= 1.0f;
	}
	
	/**
	 * Sets the message which is displayed to the users while loading
	 * or when a load has failed.
	 * 
	 * @param message 
	 * 		A short human readable decription of the content being loaded
	 * 		or describing the failure.
	 * 		If null then the default load message will be assigned
	 */
	protected final void setLoadScreenMessage(String message) {
		this.screenMessage = message;
		if (this.screenMessage == null) {
			this.screenMessage = DEFAULT_LOAD_MESSAGE;
		}
		
		// Re-render loading state
		FrameGraphics.invalidateArea(_swingComponent.getBounds());
		FrameGraphics.requestRefresh(true);
	}
	
	@Override
	public void paintInFreeSpace(Graphics g) {
		if (loadState == LOAD_STATE_COMPLETED) super.paintInFreeSpace(g);
		else paintLoadScreen(g, LOAD_SCREEN_COLOR_FREESPACE);
	}

	@Override
	public void paint(Graphics g) {
		if (loadState == LOAD_STATE_COMPLETED) {
			super.paint(g);
		} else {
			paintLoadScreen(g, LOAD_SCREEN_COLOR);
			this.paintLink((Graphics2D)g);
		}
		
	}
	
	/**
	 * Rendersthe load bar / load messages
	 * 
	 * @param g
	 */
	private void paintLoadScreen(Graphics g, Color backgroundColor) {

		if (Browser._theBrowser == null) return;
		
		// Render shaded window over widget
		g.setColor(backgroundColor);
		g.fillRect(getX(), getY(), getWidth(), getHeight());
		
		// Center the bar
		int barX = getX() + BAR_HOROZONTIAL_MARGIN;
		int barY = getY() + (getHeight() >> 1) - (BAR_HEIGHT >> 1);
		
		int barWidth = getWidth() - (BAR_HOROZONTIAL_MARGIN * 2);
		if (barWidth <= 0) barWidth = 10;
		
		// Center the text
		Font f = (loadState < 0.0f) ? LOAD_INDERTMIN_FONT : LOAD_NORMAL_FONT;
		String message = (loadState == LOAD_STATE_PENDING || loadState == LOAD_STATE_INCOMPLETED) ? 
				PENDING_MESSAGE : screenMessage;
		
		g.setFont(f);
		
		// If need to re-calc the message drawing area... do so

		FontMetrics fm   = g.getFontMetrics(f);
		Rectangle2D rect = fm.getStringBounds(message, g);
		int textHeight = (int)(rect.getHeight()); 
		int textWidth  = (int)(rect.getWidth());
		
		int textX = barX + ((barWidth - textWidth) >> 1);
		if (textX <= 0) textX = BAR_HOROZONTIAL_MARGIN + 10;
		int textY = barY +  ((BAR_HEIGHT - textHeight) >> 1);
		if (textY <= 0) textY = barY + 2;
		textY += textHeight;
		
		// Ensure that load bar and text doesn't spill over widgets invalidation area
		Shape clipBackUp = g.getClip();
		Rectangle tmpClip = (clipBackUp != null) ? clipBackUp.getBounds() : 
			new Rectangle(0, 0, 
					Browser._theBrowser.getContentPane().getWidth(), 
					Browser._theBrowser.getContentPane().getHeight());
			
		g.setClip(tmpClip.intersection(getBounds()));
	
		
		if (loadState < 0.0f) { // indeterminant

			GradientPaint gp = new GradientPaint(
					0, barY + (int)(BAR_HEIGHT * 0.8), LOAD_BAR_INDETERM_COLOR,
					0, barY, LOAD_BAR_HIGHLIGHT_COLOR);
			((Graphics2D)g).setPaint(gp);
		
			g.fillRect(barX, barY, barWidth, BAR_HEIGHT);
			
		} else if (isInLoadProgress()) {

			int progBarWidth = (int)(barWidth * loadState);
			
			GradientPaint gp = new GradientPaint(
					0, barY + (int)(BAR_HEIGHT * 0.8), LOAD_BAR_PROGRESS_COLOR,
					0, barY, LOAD_BAR_HIGHLIGHT_COLOR);
			((Graphics2D)g).setPaint(gp);
			
			g.fillRect(barX, barY, progBarWidth, BAR_HEIGHT);
			
		} 
		
		g.setColor(Color.DARK_GRAY);
		g.drawRect(barX, barY, barWidth, BAR_HEIGHT);
		
		if (loadState == LOAD_STATE_FAILED) 
			g.setColor(Color.RED);
		
		else g.setColor(Color.BLACK);
		

		
		g.drawString(message, textX, textY);
		
		g.setClip(clipBackUp);

	}
	
	
	
	/**
	 * Invoked by the load queue manager only.
	 */
	@Override
	public final void performLoad() {
		
		try {
			runEventAndWait(HDWEvent.Load);
		} catch (InterruptedException e) {
			loadState = LOAD_STATE_INCOMPLETED; // safety
			e.printStackTrace();
		}
	}

	/**
	 * Invoked by load manager
	 */
	@Override
	public final void cancelLoadRequested() {
		hasCancelledBeenRequested = true;
		cancelLoadWidgetData();
	}
	
	/**
	 * @return
	 * 		True if cancel has been requested. This is reset before a new load phase.
	 */
	protected boolean hasCancelBeenRequested() {
		return hasCancelledBeenRequested;
	}
	
	/**
	 * 
	 * @return
	 * 		True if this widget is in an expired state
	 */
	protected boolean isExpired() {
		return isExpired;
	}
	
	protected boolean isLoaded() {
		return this.loadState == LOAD_STATE_COMPLETED;
	}

	/**
	 * Important: Must be called otherwise the widget states may become unstable.
	 */
	@Override
	public void onDelete() {
		super.onDelete();
		// Evenetually - unload the data - with the intention of possible recovery, but not saving...
		queueEvent(HDWEvent.UnloadTMP);
	}

	/**
	 * 
	 * Invoked when it is time to perform all asynchronous (heavey duty) loading for the widget.
	 * 
	 * The convention is to load until cancelLoadWidgetData is invoked. Once this is invoked it is
	 * the widgets load logic's choice to whether to acknowledge it. Note that proceeding
	 * loadable entities must wait until this returns - so it is best to heed the as soon as possible. 
	 * 
	 * Use updateLoadPercentage to update the load bar and setLoadScreenMessage to set the message
	 * for load feedback to users. It will start with the default loading message and progress at 0%.
	 * 
	 * @return 
	 * 		The final load state. Must be either:
	 * 		<ul>
	 * 			<li>LOAD_STATE_COMPLETED
	 * 			<li>LOAD_STATE_FAILED
	 * 			<li>LOAD_STATE_INCOMPLETED
	 * 		</ul>
	 * 		If not, then LOAD_STATE_FAILED is assumed - and an exception trace will be printed.
	 */
	protected abstract float loadWidgetData();
	
	/**
	 * @see loadWidgetData, hasCancelBeenRequested
	 */
	protected void cancelLoadWidgetData() {}
	
	@Override
	protected void onParentStateChanged(int eventType) {

		switch (eventType) {

			case ItemParentStateChangedEvent.EVENT_TYPE_ADDED:
			case ItemParentStateChangedEvent.EVENT_TYPE_ADDED_VIA_OVERLAY:
			case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN:
			case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN_VIA_OVERLAY:
				
				// When anchored to the window, then requeue for loading iff load state 
				// is currently pending or incomplete 
				if (hasCancelledBeenRequested || 
						loadState == LOAD_STATE_INCOMPLETED || 
						loadState == LOAD_STATE_PENDING) { // if needing to load - then load
					EntityLoadManager.getInstance().queue(this, getLoadDelayTime());
				} 
				
				// Ensure that registered for saving at next save point
				EntitySaveManager.getInstance().register(this);
				
				// Ensure is cached 
				WidgetCacheManager.cacheWidget(this);
				
			break; 
			

			case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED:
			case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED_VIA_OVERLAY:
			case ItemParentStateChangedEvent.EVENT_TYPE_HIDDEN:

				// Whenever the widget is not longer in view then cancel current loading proccess
				// if currently loading. This must be performed later because this event occurs
				// before the widget has had a chance to bee moved into free space.
				SwingUtilities.invokeLater(new DoCancelLoad()); // proccess on this thread later.
				
				// Always unregister from save point.
				EntitySaveManager.getInstance().unregister(this);
				
				// If the widget has been removed then unregister from caching
				// So that if deleted then won't hang around in cache
				if (eventType == ItemParentStateChangedEvent.EVENT_TYPE_REMOVED) {
					WidgetCacheManager.uncacheWidget(this);
					// If removed via overlay, then cached frames may still contain overlay with the widget...
				}
				break;
		}
	}

	private class DoCancelLoad implements Runnable {
		@Override
		public void run() {
			
			for (Item i : getItems()) { // Only cancel if the widget is not floating. Allow loading while in free space
				if (FreeItems.getInstance().contains(i))
					return;
			}
			
			// TODO: One problem with loading in free space is that there is no way of cancelling
			// if the heavy duty widget is deleted in free space while loading.
			// This is just an inefficiency.
			EntityLoadManager.getInstance().cancel(HeavyDutyInteractiveWidget.this);
			
		}
	}

	/**
	 * @return The time to delay before loading begin when first in view.
	 */
	public abstract int getLoadDelayTime();

	/**
	 * Invoked by save manager
	 */
	@Override
	public final void performSave() {
		try {
			runEventAndWait(HDWEvent.Save);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Called by dedicated thread to save data at a save point.
	 * See EntitySaveManager for more inforamtion about save points.
	 * 
	 * Heavey duty widgets will only save at the save points if the widget
	 * still belongs to a frame and is anchored.
	 */
	protected abstract void saveWidgetData();
	
	/**
	 * Only invoked if has cache expiry and has expired. Only if this is no longer visible.
	 */
	void expire() {
		runEventLater(HDWEvent.Unload);
	}
	
	/**
	 * Invoked if this widget has a cache expiry and has been expired.
	 * The protocol is that you should unload anything in memory...
	 * This should be quick, e.g. setting references to null / disposing.
	 * Pupose is to free memory.
	 * 
	 * Important note:
	 * It is possible for a widget to expire before it had a chance to be saved,
	 * or even when saving. Thus must be careful.
	 * A simple approach would be to check the saving flag
	 * 
	 * 
	 */
	protected abstract void unloadWidgetData();

	/**
	 * This is invoked asynronously when the item is deleted. This widget
	 * will be added to an undo list in memory.
	 *
	 * The intention is that the implementation should release its memory as if it were
	 * expired - But it will not be saved. Thus the data should be dumped to a temporary file
	 * so it can be recovered if it is recalled (un-deleted). Note that this is not the same as saving -
	 * the user would not expect the widget to save state if deleted.
	 * 
	 * This is never invoke <i>while</i> loading / saving / expiring. The operations
	 * are mutually exclusive. It can be invoked when the widget is not yet loaded.
	 * 
	 */
	protected abstract void tempUnloadWidgetData();

	/**
	 * The cache depth is measured by how many frames the user can traverse through until
	 * the widget should expire.
	 * 
	 * This is immutable.
	 * 
	 * @return
	 * 			The cache depth for this interactive widget. less or equal to zero for no cache expiry.
	 */
	public final int getCacheDepth() {
		return cacheDepth;
	}

	private synchronized void runEventAndWait(HDWEvent event) throws InterruptedException {
		queueEvent(event);
		eventDispatcher.join();
	}
	
	private void runEventLater(HDWEvent event) {
		queueEvent(event);
	}
	
	private void queueEvent(HDWEvent event) {
		
		synchronized(queuedEvents) {
			
			if (!queuedEvents.contains(event))
				queuedEvents.add(event);
		
	
			if (eventDispatcher == null || !eventDispatcher.isAlive()) {
				eventDispatcher = new HDWEventDispatcher();
				eventDispatcher.start();
			}
		
		}
		
	}
	
	
	/**
	 * Unified state management. Load states are handled by one thread always.
	 * Only has one instance - per widget instance, of this at any given time,
	 * 
	 * @author Brook
	 *
	 */
	private class HDWEventDispatcher extends Thread {
		
		/**
		 * @param loadState 
		 * 	Can be any of the following values.
		 * 	<ul>
		 * 		<li>PENDING_STATE if pending.
		 * 		<li>COMPLETED_STATE if loaded.
		 * 		<li>between 0.0f and 1.0f inclusive if loading: represents progress percent complete.
		 * 		<li>FAILED_STATE if failed.
		 * 		<li>INCOMPLETED_STATE if load was unable to complete.
		 *  	<li>negative if loading but indeterminant.
		 * 	</ul>
		 * 		
		 */
		private void setLoadState(float state, boolean expired) {
			assert (state == LOAD_STATE_FAILED || state != LOAD_STATE_INCOMPLETED ||
					state != LOAD_STATE_PENDING || state != LOAD_STATE_COMPLETED ||
					state < 1.0f) ;
			
			assert (!expired || 
					(expired && state == LOAD_STATE_PENDING));
			
			
			isExpired = expired;
			loadState = state;
			
			if (loadState == LOAD_STATE_COMPLETED) { // set enabled state - show the swing components
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						_swingComponent.setVisible(true);
						_swingComponent.setEnabled(true);
					}
				});
				
			} else if(expired) { // disable/hide swing GUI when expires, like a reset
				
				SwingUtilities.invokeLater(new Runnable() {
					@Override
					public void run() {
						_swingComponent.setVisible(false);
						_swingComponent.setEnabled(false);
					}
				});
			}

			// Re-render loading state - if not expired
			if (!expired) {
				FrameGraphics.invalidateArea(new Rectangle(getX(), getY(), getWidth(), getHeight()));
				FrameGraphics.requestRefresh(true);
			}
		}
		
		/**
		 * All state transitions performed by one thread.
		 */
		@Override
		public void run() {
			
			while (true) { // keep proccessing tasks
				
				HDWEvent event = null;

				synchronized(queuedEvents) {
					if (queuedEvents.isEmpty()) return;
					event = queuedEvents.remove();
				}

				if (event == HDWEvent.Load) {
					doLoad();
				} else if (event == HDWEvent.Save) {
					doSave(); // does not change state
				} else if (event == HDWEvent.Unload){
					doUnload();
				} else {
					assert(event == HDWEvent.UnloadTMP);
					doTempUnload();
				}
				
				
			}
		}
		
		private void doLoad() {

			// Check that not already loaded.
		//	if (loadState == LOAD_STATE_COMPLETED ||
		//			loadState == LOAD_STATE_FAILED) return;
			if (loadState == LOAD_STATE_COMPLETED) return;
			
			// Only load if in view
			if (!isFloating() && getParentFrame() != DisplayIO.getCurrentFrame())
				return;
			
			// Reset flag.
			hasCancelledBeenRequested = false;
			
			// Set the load state as loading... 0%
			setLoadState(0.0f, false);
		
			float finalState = LOAD_STATE_FAILED;
			
			try {
				finalState = loadWidgetData();
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			// Safety check for return state
			try {
				if (finalState != LOAD_STATE_COMPLETED
						&& finalState != LOAD_STATE_FAILED
						&& finalState != LOAD_STATE_INCOMPLETED) {
					throw new Exception("ERROR: Bad return state: " + finalState);
				}
			} catch (Exception e) {
				e.printStackTrace();
				finalState = LOAD_STATE_FAILED;
			}
			
			// Set the final state
			setLoadState(finalState, false); 
			
		}
		
		private void doSave() {

			// Only save if still belongs to a frame
			if (!isFloating() && getParentFrame() != null) {
				saveWidgetData();
			}

		}
		
		private void doUnload() {

			// Reset the load state
			setLoadState(LOAD_STATE_PENDING, true); 
			
			// Get rid of memory
			unloadWidgetData();
			
		}
		
		private void doTempUnload() {
			// Reset the load state
			setLoadState(LOAD_STATE_PENDING, true); 
			
			// Get rid of memory
			tempUnloadWidgetData();
		}
		
		
		
	}
	
	private enum HDWEvent {
		Save,
		Load,
		Unload,
		UnloadTMP
	}
	
	
}
