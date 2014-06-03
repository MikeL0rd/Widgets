package org.expeditee.items.widgets;

/**
 * Thrown if a widget fails to be instantiated. 
 * Must encapsulate all exceptions in constructor with this exception for all types of 
 * InteractiveWidgets
 * @author Brook Novak
 */
public class InteractiveWidgetInitialisationFailedException extends Exception {

		protected static final long serialVersionUID = 0L;
		
		public InteractiveWidgetInitialisationFailedException() {
			super();
		}
		
		public InteractiveWidgetInitialisationFailedException(String message) {
			super(message);
		}
		
		public InteractiveWidgetInitialisationFailedException(String message, Exception inner) {
			super(message, inner);
		}
	}
