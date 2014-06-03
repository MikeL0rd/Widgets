package org.expeditee.items.widgets;

/**
 * Occurs if attempting to call a widget into existance but does not exist.
 * @author Brook Novak
 */
public class InteractiveWidgetNotAvailableException extends Exception {
	protected static final long serialVersionUID = 0L;
	
	public InteractiveWidgetNotAvailableException() {
		super();
	}
	
	public InteractiveWidgetNotAvailableException(String message) {
		super(message);
	}
	
	public InteractiveWidgetNotAvailableException(String message, Exception inner) {
		super(message, inner);
	}
}
