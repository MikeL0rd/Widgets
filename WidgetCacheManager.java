package org.expeditee.items.widgets;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;

import org.expeditee.gui.DisplayIOObserver;
import org.expeditee.gui.FrameIO;


/**
 * Some heavy duty widgets can hold alot of data and must only be in Expeditee memory for
 * a little while ... e.g. Not cached once the user has traversed 50 or so frames after
 * where the widget was first loaded.
 * 
 * @author Brook Novak
 *
 */
public final class WidgetCacheManager implements DisplayIOObserver {
	
	private static HashMap<HeavyDutyInteractiveWidget, Integer> transientWidgets = new HashMap<HeavyDutyInteractiveWidget, Integer>();
	
	private static WidgetCacheManager _instance = new WidgetCacheManager();
	
	private WidgetCacheManager() {
	}
	
	public static WidgetCacheManager getInstance() {
		return _instance;
	}
	
	/**
	 * @return
	 * 		An unmodifiable set of the current transientWidgets that are registered.
	 */
	public static Set<HeavyDutyInteractiveWidget> getTransientWidgets() {
		return Collections.unmodifiableSet(transientWidgets.keySet());
	}
	
	public static void cacheWidget(HeavyDutyInteractiveWidget widget) {
		if (widget == null) throw new NullPointerException("widget");
		
		if (widget.getCacheDepth() > FrameIO.MAX_CACHE ||
				widget.getCacheDepth() <= 0) return;
		
		// If widget already exists, then the current count will be reset (overriden to zero)
		transientWidgets.put(widget, new Integer(0));
		
	}
	
	public static void uncacheWidget(HeavyDutyInteractiveWidget widget) {
		if (widget == null) throw new NullPointerException("widget");
		transientWidgets.remove(widget);
	}


	/**
	 * invoked when the frame changes. After the new frame is actually set.
	 * Expires heavy duty widgets that have been cached for to long....
	 * 
	 * Intention: to be called from the DisplayIO frame change code...
	 *
	 */
	@Override
	public void frameChanged() {

		// Increment all cache counters
		Collection<HeavyDutyInteractiveWidget> toCheck = new LinkedList<HeavyDutyInteractiveWidget>(
				transientWidgets.keySet());
		
		// Check all heavyduty widgets that have limited cache
		for (HeavyDutyInteractiveWidget tw : toCheck) {
			
			Integer count = transientWidgets.remove(tw); // get rid of reference
			if (count == null) continue;
			
			int newCount = count;
			newCount++;

			if (tw.isVisible()) { // Reset cache counter if visible
				
				transientWidgets.put(tw, new Integer(0));
				
			} else if (newCount >= tw.getCacheDepth()) { // Expire if stale
	
				// Ensure that the heavy duty widgets data is handled correctly (i.e. saving)
				tw.expire();
				
			} else { // keep reference, with new count
				
				transientWidgets.put(tw, newCount);
				
			}
			
		}
	}
	
	
}
	