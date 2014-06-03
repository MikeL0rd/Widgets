package org.expeditee.items.widgets;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.expeditee.actions.Actions;
import org.expeditee.gui.Browser;
import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameKeyboardActions;
import org.expeditee.gui.FreeItems;
import org.expeditee.gui.MouseEventRouter;
import org.expeditee.items.Item;
import org.expeditee.items.ItemParentStateChangedEvent;
import org.expeditee.items.ItemUtils;
import org.expeditee.items.Text;
import org.expeditee.items.UserAppliedPermission;

/**
 * The bridge between swing space and Expeditee space
 * 
 * @author Brook
 * 
 */
public abstract class InteractiveWidget implements ContainerListener, KeyListener {

	protected JComponent _swingComponent;

	/** A widget is comprised of dots and lines that basically form a rectangle */
	private WidgetCorner _d1, _d2, _d3, _d4;

	private WidgetEdge _l1, _l2, _l3, _l4;

	/*
	 * GUIDE: l1 d1-------d2 | | l4 | X | 12 | | d4-------d3 13
	 */
	private List<Item> _expediteeItems; // used for quickly returning item list

	// Widget size restrictions
	private int _minWidth = 50;

	private int _minHeight = 50;

	private int _maxWidth = 300;

	private int _maxHeight = 300;

	// The Expeditee item that is used for saving widget state in expeditee
	// world
	protected Text _textRepresentation;

	protected Float _anchorLeft;
	protected Float _anchorRight;
	protected Float _anchorTop;
	protected Float _anchorBottom;
	
	protected final static Color FREESPACE_BACKCOLOR = new Color(100, 100, 100);

	// A flag for signifying whether the swing components are ready to paint.
	// If the swing components has not been layed out, if they are painted they
	// will not draw in the correct positions.
	// Also for setting AWT and Swing -Related drawing options after
	// construction
	private boolean _isReadyToPaint = false;

	/** For ensuring only one event is listened to - instead of four. */
	private MouseEvent _lastParentStateChangedMouseEvent = null;

	/** For ensuring only one event is listened to - instead of four. */
	private ItemParentStateChangedEvent _lastParentStateChangedEvent = null;

	/** The minum border thickness for widgets. */
	public final static float DEFAULT_MINIMUM_BORDER_THICKNESS = 1.0f;

	/**
	 * Creates a InteractiveWidget from a text item.
	 * 
	 * @param source
	 *            Must not be null, first line of text used - which the format
	 *            must be as follows: "@iw: <<widget_class_name>> [<<width>>] [<<height>>] [: [<<arg1>>] [<<arg2>>]
	 *            [...]]".
	 * 
	 * e.g: "@iw: org.expeditee.items.SampleWidget1 100 20 : 2" creates a
	 * SampleWidget1 with width = 100 and height = 20 with 1 argument = "2"
	 * 
	 * @return An InteractiveWidget instance. Never null.
	 * 
	 * @throws NullPointerException
	 *             if source is null
	 * 
	 * @throws IllegalArgumentException
	 *             if source's text is in the incorrect format or if source's
	 *             parent is null
	 * 
	 * @throws InteractiveWidgetNotAvailableException
	 *             If the given widget class name in the source text doesn't
	 *             exist or not an InteractiveWidget or the widget does not
	 *             supply a valid constructor for creation.
	 * 
	 * @throws InteractiveWidgetInitialisationFailedException
	 *             The sub-constructor failed - depends on the type of widget
	 *             being instantainiated.
	 * 
	 * class names beginning with $, the $ will be replaced with
	 * "org.expeditee.items."
	 */
	public static InteractiveWidget createWidget(Text source)
			throws InteractiveWidgetNotAvailableException,
			InteractiveWidgetInitialisationFailedException {

		if (source == null)
			throw new NullPointerException("source");
		if (source.getParent() == null)
			throw new IllegalArgumentException(
					"source's parent is null, InteractiveWidget's must be created from Text items with non-null parents");

		String TAG = ItemUtils.GetTag(ItemUtils.TAG_IWIDGET);

		String text = source.getText();
		if (text == null) {
			throw new IllegalArgumentException("source does not have any text");
		}

		text = text.trim();

		// Check starts with the widget tag and separator
		if (!text.startsWith(TAG + ":"))
			throw new IllegalArgumentException("Source text must begin with \"" + TAG + ":\"");

		// skip over the '@iw:' preamble
		text = text.substring(TAG.length() + 1).trim();

		int index = 0;
		if (text.length() > 0) {
		    // Having removed @iw:, then next ':' is used for signifying start of arguments
		    index = text.indexOf(':'); 
		}

		if (index == 0) {
		    throw new IllegalArgumentException("Source text must begin with \"" + TAG + "\"");
		}

		//
		// Step 1: 
		//   For an X-rayable text item in the form:
		//     @iw: <class name> [options] width height : <rest...>
		//
		//   Parse the 'core' part of the X-rayable text item
		//   i.e, the text between the first ':' and the second ':'
		//  
		//
		
		String tokens_str = (index == -1) ? text : text.substring(0, index);

		String[] tokens = parseArgsApache(tokens_str);

		// make anything starting with a '-' lowercase
		for (int i=0; i<tokens.length; i++) {
			if (tokens[i].startsWith("-")) {
				tokens[i] = tokens[i].toLowerCase();
			}
		}
		
		// create the command line parser
		CommandLineParser parser = new GnuParser();

		// create the Options
		Options options = new Options();
		options.addOption( "al", "anchorleft",   true, "Anchor the vertical left-hand edge of the interactive widget to the value provided " );
		options.addOption( "ar", "anchorright",  true, "Anchor the vertical right-hand edge of the interactive widget to the value provided " );
		options.addOption( "at", "anchortop",    true, "Anchor the vertical top edge of the interactive widget to the value provided " );
		options.addOption( "ab", "anchorbottom", true, "Anchor the vertical bottom edge of the interactive widget to the value provided " );
		
		/*
		 * Alternative way to do the above, but with more control over how the values are set up and can be used
		 * 
		@SuppressWarnings("static-access")
		Option anchor_left_opt = OptionBuilder.withLongOpt( "anchorleft" )
				.withDescription( "use <x-pos> as left anchor for the vertical lefthand edge of the interactive widget" )
				.hasArg()
				.withArgName("<x-pos>")
				.create("al");
         
		@SuppressWarnings("static-access")
		Option anchor_right_opt = OptionBuilder.withLongOpt( "anchorright" )
				.withDescription( "use <x-pos> as right anchor for the vertical righthand edge of the interactive widget" )
				.hasArg()
				.withArgName("<x-pos>")
				.create("ar");
		
		
		@SuppressWarnings("static-access")
		Option anchor_top_opt = OptionBuilder.withLongOpt( "anchortop" )
				.withDescription( "use <y-pos> as top anchor for the horizontal top end of the interactive widget" )
				.hasArg()
				.withArgName("<x-pos>")
				.create("at");
         
		@SuppressWarnings("static-access")
		Option anchor_bottom_opt = OptionBuilder.withLongOpt( "anchorbottom" )
				.withDescription( "use <y-pos> as bottom anchor for the horizontal bottom edge of the interactive widget" )
				.hasArg()
				.withArgName("<x-pos>")
				.create("ab");
		
		options.addOption(anchor_left_opt);
		options.addOption(anchor_right_opt);
		options.addOption(anchor_top_opt);
		options.addOption(anchor_bottom_opt);
		*/

		CommandLine core_line;
		try {
		    // parse the command line arguments
		    core_line = parser.parse( options, tokens );
		    
		    // Update tokens to be version with the options removed
		    tokens = core_line.getArgs();
		    
		}
		catch( ParseException exp ) {
		    System.err.println( "Unexpected exception:" + exp.getMessage() );
		    core_line = null;
		    
		}
		
		HelpFormatter formatter = new HelpFormatter();
		StringWriter usage_str_writer = new StringWriter();
	    PrintWriter printWriter = new PrintWriter(usage_str_writer);
	    
	    String widget_name = tokens[0];
	    final String widget_prefix = "org.expeditee.items.widgets";
	    if (widget_name.startsWith(widget_prefix)) {
	    	widget_name = widget_name.substring(widget_prefix.length());
	    }
	    
		formatter.printHelp(printWriter, 80, TAG + ":" + widget_name + " [options] width height", null, options, 4, 0, null);
		//System.out.println(usage_str_writer.toString());
		
		float width = -1, height = -1;

		if (tokens.length < 1) {
			throw new IllegalArgumentException("Missing widget class name in source text");
		}

		try {

			if (tokens.length >= 2) { // parse optional width
				width = Integer.parseInt(tokens[1]);
				width = (width <= 0) ? width = -1 : width;
			}

			if (tokens.length >= 3) { // parse optional height
				height = Integer.parseInt(tokens[2]);
				height = (height <= 0) ? height = -1 : height;
			}

		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException(
					"Bad width or height given in source text", nfe);
		}

		if (tokens.length > 3)
			throw new IllegalArgumentException(
					"to many arguments given before \":\" in source text");

		String classname = tokens[0];
		if (classname.charAt(0) == '$'){
			classname = classname.substring(1);
		}
		
		// Attempt to locate the class using reflection
		Class<?> iwclass = findIWidgetClass(classname);

		if (iwclass == null) // ensure it exists
			throw new InteractiveWidgetNotAvailableException(classname
					+ " does not exist or is not an InteractiveWidget");

		
		//
		// Step 2: 
		//   For an X-rayable text item in the form:
		//     @iw: <class name> [options] width height : <rest...>
		//
		//   Parse the <rest ...> part
		
		// Extract out the parameters - if any
		String[] args = null;
		if (index > 0) { // index of the first ":"
			if (text.length()>(index+1)) {
				String args_str = text.substring(index + 1);
				
				 args = parseArgsApache(args_str);
		
			}	
		}

		InteractiveWidget inst = null;
		try {
			// Instantiate the widget - passing the params
			Class parameterTypes[] = new Class[] { Text.class, String[].class };
			Constructor ct = iwclass.getConstructor(parameterTypes);

			Object arglist[] = new Object[] { source, args };

			inst = (InteractiveWidget) ct.newInstance(arglist);
		} catch (Exception e) {
			throw new InteractiveWidgetNotAvailableException(
					"Failed to create instance via reflection: " + e.toString(),
					e);
		}

		// Step 3: 
		//    Set up the size and position of the widget

		// Use default dimensions if not provided (or provided as negative
		// values)
		if (width <= 0) {
			width = inst.getWidth();
		}
		if (height <= 0) {
			height = inst.getHeight();
		}

		inst.setSize(width, height);
		
		// Apply any anchor values supplied in the core part of the @iw item
		Float anchor_left   = null;
		Float anchor_right  = null;
		Float anchor_top    = null;
		Float anchor_bottom = null;
		
	    if(core_line.hasOption( "anchorleft" ) ) {
	    	String al_str = core_line.getOptionValue( "anchorleft" );
	    	
	    	anchor_left = Float.parseFloat(al_str);
	    }
	    
	    if(core_line.hasOption( "anchorright" ) ) {
	    	String ar_str = core_line.getOptionValue( "anchorright" );
	    	
	    	anchor_right = Float.parseFloat(ar_str);
	    }
	    
	    if(core_line.hasOption( "anchortop" ) ) {
	    	String at_str = core_line.getOptionValue( "anchortop" );
	    	
	    	anchor_top = Float.parseFloat(at_str);
	    }
	    
	    if(core_line.hasOption( "anchorbottom" ) ) {
	    	String ab_str = core_line.getOptionValue( "anchorbottom" );
	    	
	    	anchor_bottom = Float.parseFloat(ab_str);
	    }
	    
	    inst.setAnchorCorners(anchor_left,anchor_right,anchor_top,anchor_bottom);
	    
	
		
		return inst;
	}

	/**
	 * Locates the class from the classname of an InteractiveWidget class
	 * 
	 * @param classname
	 *            The name of the class to search
	 * @return Null if doesn't exist or not an InteractiveWidget
	 */
	private static Class findIWidgetClass(String classname) {
		if(classname == null)
			return null;
		// try just the classname
		try {
			Class c = Class.forName(classname); // attempt to find the class

			// If one is found, ensure that it is a descendant of an
			// InteractiveWidget
			for (Class superclass = c.getSuperclass(); superclass != null
					&& superclass != Item.class; superclass = superclass
					.getSuperclass()) {
				if (superclass == InteractiveWidget.class)
					return c;
			}

		} catch (ClassNotFoundException e) {
		}
		// see if the class is a widget with invalid capitalisation, or missing the widget package prefix
		if(classname.startsWith(Actions.WIDGET_PACKAGE)) {
			classname = classname.substring(Actions.WIDGET_PACKAGE.length());
		}
		try {
			Class c = Class.forName(Actions.getClassName(classname)); // attempt to find the class

			// If one is found, ensure that it is a descendant of an
			// InteractiveWidget
			for (Class superclass = c.getSuperclass(); superclass != null
					&& superclass != Item.class; superclass = superclass
					.getSuperclass()) {
				if (superclass == InteractiveWidget.class)
					return c;
			}

		} catch (ClassNotFoundException e) {
		}

		// Doesn't exist or not an InteractiveWidget
		return null;
	}

	/**
	 * Using Microsofts commandline convention: Args seperated with white
	 * spaces. Options with white spaces enclosed with quotes. Args with quotes
	 * must be double quoted args1 args2=sfasas args3="option with spaces"
	 * arg""4""
	 * 
	 * @param args
	 *            Null and empty excepted
	 * @return An array of args. null if none provided
	 */
	public static String[] parseArgs(String args) {

		if (args == null)
			return null;

		args = args.trim();
		if (args.length() == 0)
			return null;

		List<String> vargs = new LinkedList<String>();
		StringBuilder sb = new StringBuilder();
		boolean quoteOn = false;
		for (int i = 0; i < args.length(); i++) {

			char c = args.charAt(i);
			if (c == ' ' && !quoteOn) {

				// Extract arg
				vargs.add(sb.toString());
				sb = new StringBuilder();

				// Consume white spaces
				while (args.charAt(++i) == ' ' && i < args.length()) {
				}
				i--;

			} else if (c == '\"') {
				// If double quoted
				if (args.length() >= (i + 2) && args.charAt(i + 1) == '\"') {

					sb.append(c); // add escaped
					i++;

				} else {
					quoteOn = !quoteOn;
				}

			} else {
				sb.append(c);
			}
		}

		if (sb.length() > 0)
			vargs.add(sb.toString());

		if (vargs.size() == 0)
			return null;
		else
			return vargs.toArray(new String[vargs.size()]);
	}

	/**
	 * Reverse of parseArgs
	 * 
	 * @return Null if args is null or empty / all whitespace
	 */
	public static String formatArgs(String[] args) {
		if (args == null)
			return null;

		StringBuilder sb = new StringBuilder();

		for (String s : args) {
			if (s == null)
				continue;

			// Escape quotes
			StringBuilder formatted = new StringBuilder(s.replaceAll("\"",
					"\"\""));

			// Encapsulate spaces
			int index = formatted.indexOf(" ");
			if (index >= 0) {
				formatted.insert(index, "\"");
				formatted.append('\"');
			}

			if (sb.length() > 0)
				sb.append(' ');
			sb.append(formatted);
		}

		return sb.length() > 0 ? sb.toString() : null;
	}




    /**
     * Similar to parseArgs() above.  
     * Code based on routine used in Apache Ant:
     *   org.apache.tools.ant.types.Commandline::translateCommandline()
     * @param toProcess the command line to process.
     * @return the command line broken into strings.
     * An empty or null toProcess parameter results in a zero sized array.
     */
    public static String[] parseArgsApache(String toProcess) {
        if (toProcess == null || toProcess.length() == 0) {
            //no command? no string
            return new String[0];
        }
        // parse with a simple finite state machine

        final int normal = 0;
        final int inQuote = 1;
        final int inDoubleQuote = 2;
        int state = normal;
        final StringTokenizer tok = new StringTokenizer(toProcess, "\"\' ", true);
        final ArrayList<String> result = new ArrayList<String>();
        final StringBuilder current = new StringBuilder();
        boolean lastTokenHasBeenQuoted = false;

        while (tok.hasMoreTokens()) {
            String nextTok = tok.nextToken();
            switch (state) {
            case inQuote:
                if ("\'".equals(nextTok)) {
                    lastTokenHasBeenQuoted = true;
                    state = normal;
                } else {
                    current.append(nextTok);
                }
                break;
            case inDoubleQuote:
                if ("\"".equals(nextTok)) {
                    lastTokenHasBeenQuoted = true;
                    state = normal;
                } else {
                    current.append(nextTok);
                }
                break;
            default:
                if ("\'".equals(nextTok)) {
                    state = inQuote;
                } else if ("\"".equals(nextTok)) {
                    state = inDoubleQuote;
                } else if (" ".equals(nextTok)) {
                    if (lastTokenHasBeenQuoted || current.length() != 0) {
                        result.add(current.toString());
                        current.setLength(0);
                    }
                } else {
                    current.append(nextTok);
                }
                lastTokenHasBeenQuoted = false;
                break;
            }
        }
        if (lastTokenHasBeenQuoted || current.length() != 0) {
            result.add(current.toString());
        }
        if (state == inQuote || state == inDoubleQuote) {
	    System.err.println("Error: Unbalanced quotes -- failed to parse '" + toProcess + "'");
	    return null;
        }

        return result.toArray(new String[result.size()]);
    }




	/**
	 * Arguments represent the widgets <i>current state</i> state. They are
	 * used for saving, loading, creating and cloning Special formatting is done
	 * for you.
	 * 
	 * @see #getData()
	 * 
	 * @return Can be null for no params.
	 */
	protected abstract String[] getArgs();

	/**
	 * Data represents the widgets <i>current state</i> state. For any state
	 * information you do not wish to be defined as arguments (e.g. metadata),
	 * you can set as the widgets source items data.
	 * 
	 * The default implementation returns null. Override to make use of.
	 * 
	 * @see #getArgs()
	 * 
	 * @return Null for for data. Otherwise the data that represent this widgets
	 *         <i>current state</i>
	 */
	protected List<String> getData() {
		return null;
	}

	/**
	 * Constructor
	 * 
	 * @param source
	 *            Must not be null. Neither must it's parent
	 * 
	 * @param component
	 *            Must not be null.
	 * 
	 * @param minWidth
	 *            The min width restriction for the widget. If negative, then
	 *            there is no restriction.
	 * 
	 * @param maxWidth
	 *            The max width restriction for the widget. If negative, then
	 *            there is no restriction.
	 * 
	 * @param minHeight
	 *            The min height restriction for the widget. If negative, then
	 *            there is no restriction.
	 * 
	 * @param maxHeight
	 *            The max height restriction for the widget. If negative, then
	 *            there is no restriction.
	 * 
	 * @throws NullPointerException
	 *             If source, component if null.
	 * 
	 * @throws IllegalArgumentException
	 *             If source's parent is null. If maxWidth smaller than minWidth
	 *             and maxWidth larger or equal to zero or if maxHeight smaller
	 *             than minHeight && maxHeight is larger or equal to zero
	 * 
	 */
	protected InteractiveWidget(Text source, JComponent component,
			int minWidth, int maxWidth, int minHeight, int maxHeight) {


		if (component == null)
			throw new NullPointerException("component");
		if (source == null)
			throw new NullPointerException("source");
		if (source.getParent() == null)
			throw new IllegalArgumentException(
					"source's parent is null, InteractiveWidget's must be created from Text items with non-null parents");

		_swingComponent = component;
		_swingComponent.addContainerListener(this);
		keyListenerToChildren(_swingComponent, true);
		
		_textRepresentation = source;

		setSizeRestrictions(minWidth, maxWidth, minHeight, maxHeight); // throws IllegalArgumentException's

		int x = source.getX();
		int y = source.getY();
		int width = (_minWidth < 0) ? 10 : _minWidth;
		int height = (_minHeight < 0) ? 10 : _minHeight;

		Frame idAllocator = _textRepresentation.getParent();

		// create WidgetCorners
		_d1 = new WidgetCorner(x, y, idAllocator.getNextItemID(), this);
		_d2 = new WidgetCorner(x + width, y, idAllocator.getNextItemID(), this);
		_d3 = new WidgetCorner(x + width, y + height, idAllocator.getNextItemID(), this);
		_d4 = new WidgetCorner(x, y + height, idAllocator.getNextItemID(), this);

		// create WidgetEdges
		_l1 = new WidgetEdge(_d1, _d2, idAllocator.getNextItemID(), this);
		_l2 = new WidgetEdge(_d2, _d3, idAllocator.getNextItemID(), this);
		_l3 = new WidgetEdge(_d3, _d4, idAllocator.getNextItemID(), this);
		_l4 = new WidgetEdge(_d4, _d1, idAllocator.getNextItemID(), this);

		Collection<Item> enclist = new ArrayList<Item>(4);
		enclist.add(_d1);
		enclist.add(_d2);
		enclist.add(_d3);
		enclist.add(_d4);
		_d1.setEnclosedList(enclist);
		_d2.setEnclosedList(enclist);
		_d3.setEnclosedList(enclist);
		_d4.setEnclosedList(enclist);

		_expediteeItems = new ArrayList<Item>(8); // Note: order important
		_expediteeItems.add(_d1);
		_expediteeItems.add(_d2);
		_expediteeItems.add(_d3);
		_expediteeItems.add(_d4);
		_expediteeItems.add(_l1);
		_expediteeItems.add(_l2);
		_expediteeItems.add(_l3);
		_expediteeItems.add(_l4);

		setWidgetEdgeColor(source.getBorderColor());
		setWidgetEdgeThickness(source.getThickness());
	}

	/**
	 * Sets the restrictions - checks values.
	 * 
	 * @param minWidth
	 *            The min width restriction for the widget. If negative, then
	 *            there is no restriction.
	 * 
	 * @param maxWidth
	 *            The max width restriction for the widget. If negative, then
	 *            there is no restriction.
	 * 
	 * @param minHeight
	 *            The min height restriction for the widget. If negative, then
	 *            there is no restriction.
	 * 
	 * @param maxHeight
	 *            The max height restriction for the widget. If negative, then
	 *            there is no restriction.
	 * 
	 * @throws IllegalArgumentException
	 *             If maxWidth smaller than minWidth and maxWidth larger or
	 *             equal to zero or if maxHeight smaller than minHeight &&
	 *             maxHeight is larger or equal to zero
	 * 
	 */
	private void setSizeRestrictions(int minWidth, int maxWidth, int minHeight,
			int maxHeight) {

		_minWidth = minWidth;

		if (maxWidth < _minWidth && maxWidth >= 0)
			throw new IllegalArgumentException(
					"maxWidth smaller than the min Width");
		_maxWidth = maxWidth;

		_minHeight = minHeight;
		if (maxHeight < _minHeight && maxHeight >= 0)
			throw new IllegalArgumentException(
					"maxHeight smaller than the min Height");
		_maxHeight = maxHeight;
	}

	/**
	 * This can be overrided for creating custom copies. The default
	 * implementation creates a new widget based on the current state of the
	 * widget (via getArgs).
	 * 
	 * @see InteractiveWidget#getArgs().
	 * 
	 * @return A copy of this widget.
	 * 
	 */
	public InteractiveWidget copy()
			throws InteractiveWidgetNotAvailableException,
			InteractiveWidgetInitialisationFailedException {

		Text t = _textRepresentation.copy();
		String clonedAnnotation = getAnnotationString();
		t.setText(clonedAnnotation);
		t.setData(getData());
		return InteractiveWidget.createWidget(t);

	}

	/**
	 * Notifies widget of delete
	 */
	public void onDelete() {

		// Allocate new ID's
		Frame parent = getParentFrame();
		if (parent == null)
			parent = DisplayIO.getCurrentFrame();

		if (parent != null) {
			for (Item i : _expediteeItems) {
				i.setID(parent.getNextItemID());
			}
		}

		invalidateLink();

	}

	/**
	 * Note updates the source text with current state info
	 * 
	 * @return The Text item that this widget was created from.
	 */
	public Item getSource() {

		// Build the annotation string such that it represents this widgets
		// current state
		String newAnnotation = getAnnotationString();

		// Set the new text
		_textRepresentation.setText(newAnnotation);

		// Set the data
		_textRepresentation.setData(getData());

		return _textRepresentation;
	}

	/**
	 * @return The current representation for this widget. The representation
	 *         stores link information, data etc... It is used for saving and
	 *         loading of the widget. Never null.
	 * 
	 */
	protected Item getCurrentRepresentation() {
		return _textRepresentation;
	}

	/**
	 * @return The Expeditee annotation string.
	 */
	protected String getAnnotationString() {

		// Create tag and append classname
		StringBuilder sb = new StringBuilder(ItemUtils
				.GetTag(ItemUtils.TAG_IWIDGET));
		sb.append(':');
		sb.append(' ');
		sb.append(getClass().getName());

		if (_anchorLeft != null) {
		    sb.append(" --anchorLeft " + Math.round(_anchorLeft));
		}
		if (_anchorRight != null) {
		    sb.append(" --anchorRight " + Math.round(_anchorRight));
		}

		if (_anchorTop != null) {
		    sb.append(" --anchorTop " + Math.round(_anchorTop));
		}

		if (_anchorBottom != null) {
		    sb.append(" --anchorBottom " + Math.round(_anchorBottom));
		}

		// Append size information if needed (not an attribute of text items)
		if (!isFixedSize()) {
			sb.append(' ');
			sb.append(getWidth());
			sb.append(' ');
			sb.append(getHeight());
		}

		// Append arguments if any
		String stateArgs = InteractiveWidget.formatArgs(getArgs());
		if (stateArgs != null) {
			sb.append(':');
			sb.append(stateArgs);
		}

		return sb.toString();
	}

	/**
	 * Sets both the new size as well as the new min/max widget/height
	 * restrictions.
	 * 
	 * @param minWidth
	 *            The min width restriction for the widget. If negative, then
	 *            there is no restriction.
	 * 
	 * @param maxWidth
	 *            The max width restriction for the widget. If negative, then
	 *            there is no restriction.
	 * 
	 * @param minHeight
	 *            The min height restriction for the widget. If negative, then
	 *            there is no restriction.
	 * 
	 * @param maxHeight
	 *            The max height restriction for the widget. If negative, then
	 *            there is no restriction.
	 * 
	 * @param newWidth
	 *            Clamped to new restrictions.
	 * 
	 * @param newHeight
	 *            Clamped to new restrictions.
	 * 
	 * @throws IllegalArgumentException
	 *             If maxWidth smaller than minWidth and maxWidth larger or
	 *             equal to zero or if maxHeight smaller than minHeight &&
	 *             maxHeight is larger or equal to zero
	 * 
	 * @see #setSize(float, float)
	 * 
	 */
	public void setSize(int minWidth, int maxWidth, int minHeight,
			int maxHeight, float newWidth, float newHeight) {

		setSizeRestrictions(minWidth, maxWidth, minHeight, maxHeight); // throws
		// IllegalArgumentException's
		setSize(newWidth, newHeight);
	}

	/**
	 * Clamped to current min/max width/height.
	 * 
	 * @param width
	 *            Clamped to current restrictions.
	 * @param height
	 *            Clamped to current restrictions.
	 * 
	 * @see #setSize(int, int, int, int, float, float)
	 */
	public void setSize(float width, float height) {

		// If 'width' and 'height' exceed the min/max values for width/height
		// => clamp to the relevant min/max value 
		if (width < _minWidth && _minWidth >= 0) {
			width = _minWidth;
		}
		else if (width > _maxWidth && _maxWidth >= 0) {
			width = _maxWidth;
		}
		
		if (height < _minHeight && _minHeight >= 0) {
			height = _minHeight;
		}
		else if (height > _maxHeight && _maxHeight >= 0) {
			height = _maxHeight;
		}
		
		// Remember current isFloating() values
		boolean vfloating[] = new boolean[] { _d1.isFloating(),
				_d2.isFloating(), _d3.isFloating(), _d4.isFloating() };

		_d1.setFloating(true);
		_d2.setFloating(true);
		_d3.setFloating(true);
		_d4.setFloating(true);

		float xr = _d1.getX() + width;
		float yb = _d1.getY() + height;

		_d2.setX(xr);
		_d3.setX(xr);
		_d3.setY(yb);
		_d4.setY(yb);

		// Restore isFloating() values
		_d1.setFloating(vfloating[0]);
		_d2.setFloating(vfloating[1]);
		_d3.setFloating(vfloating[2]);
		_d4.setFloating(vfloating[3]);

		onSizeChanged();
	}

    public void setAnchorCorners(Float left, Float right, Float top, Float bottom)
    {
    	setAnchorLeft(left);
    	setAnchorRight(right);
    	setAnchorTop(top);
    	setAnchorBottom(bottom);
    }
    

	public void setPosition(int x, int y) {
		if (x == getX() && y == getY())
			return;

		// Remember current isFloating() values
		boolean vfloating[] = new boolean[] { _d1.isFloating(),
				_d2.isFloating(), _d3.isFloating(), _d4.isFloating() };

		int width = getWidth();
		int height = getHeight();

		invalidateLink();

		_d1.setFloating(true);
		_d2.setFloating(true);
		_d3.setFloating(true);
		_d4.setFloating(true);

		_d1.setPosition(x, y);
		_d2.setPosition(x + width, y);
		_d3.setPosition(x + width, y + height);
		_d4.setPosition(x, y + height);

		// Restore isFloating() values
		_d1.setFloating(vfloating[0]);
		_d2.setFloating(vfloating[1]);
		_d3.setFloating(vfloating[2]);
		_d4.setFloating(vfloating[3]);

		invalidateLink();

		onMoved();

	}

	private boolean _settingPositionFlag = false; // used for recursion

	/**
	 * Updates position of given WidgetCorner to the given (x,y), 
	 *   and updates related values (connected corners, width and height)
	 * 
	 * @param src
	 * @param x
	 * @param y
	 * @return False if need to call super.setPosition
	 */
	boolean setPositions(WidgetCorner src, float x, float y) {

		if (_settingPositionFlag)
			return false;
		_settingPositionFlag = true;

		invalidateLink();

		// Check to see if the widget is fully being picked up
		boolean isAllPickedUp = (_d1.isFloating() && _d2.isFloating()
				&& _d3.isFloating() && _d4.isFloating());

		// If so, then this will be called one by one ..
		if (isAllPickedUp) {
			src.setPosition(x, y);
		} else {

			float newX = x;

			// Reference:
			// D1 D2
			// D3 D4

			//
			// GUIDE:
			// l1
			// d1-------d2
			// | |
			// l4 | X | 12
			// | |
			// d4-------d3
			// 13
			//

			// X Positions
			if (src == _d1 || src == _d4) {

				// Check min X constraint
				if (_minWidth >= 0) {
					if ((_d2.getX() - x) < _minWidth) {
						newX = _d2.getX() - _minWidth;
					}
				}
				// Check max X constraint
				if (_maxWidth >= 0) {
					if ((_d2.getX() - x) > _maxWidth) {
						newX = _d2.getX() - _maxWidth;
					}
				}

				if (!(src == _d4 && _d1.isFloating() && _d4.isFloating()))
					_d1.setX(newX);
				if (!(src == _d1 && _d4.isFloating() && _d1.isFloating()))
					_d4.setX(newX);

			} else if (src == _d2 || src == _d3) {

				// Check min X constraint
				if (_minWidth >= 0) {
					if ((x - _d1.getX()) < _minWidth) {
						newX = _d1.getX() + _minWidth;
					}
				}
				// Check max X constraint
				if (_maxWidth >= 0) {
					if ((x - _d1.getX()) > _maxWidth) {
						newX = _d1.getX() + _maxWidth;
					}
				}

				if (!(src == _d3 && _d2.isFloating() && _d3.isFloating()))
					_d2.setX(newX);
				if (!(src == _d2 && _d3.isFloating() && _d2.isFloating()))
					_d3.setX(newX);
			}

			float newY = y;

			// Y Positions
			if (src == _d1 || src == _d2) {

				// Check min Y constraint
				if (_minHeight >= 0) {
					if ((_d4.getY() - y) < _minHeight) {
						newY = _d4.getY() - _minHeight;
					}
				}
				// Check max Y constraint
				if (_maxHeight >= 0) {
					if ((_d4.getY() - y) > _maxHeight) {
						newY = _d4.getY() - _maxHeight;
					}
				}

				if (!(src == _d2 && _d1.isFloating() && _d2.isFloating()))
					_d1.setY(newY);
				if (!(src == _d1 && _d2.isFloating() && _d1.isFloating()))
					_d2.setY(newY);

			} else if (src == _d3 || src == _d4) {

				// Check min Y constraint
				if (_minHeight >= 0) {
					if ((y - _d1.getY()) < _minHeight) {
						newY = _d1.getY() + _minHeight;
					}
				}
				// Check max Y constraint
				if (_maxHeight >= 0) {
					if ((y - _d1.getY()) > _maxHeight) {
						newY = _d1.getY() + _maxHeight;
					}
				}

				if (!(src == _d4 && _d3.isFloating() && _d4.isFloating()))
					_d3.setY(newY);
				if (!(src == _d3 && _d4.isFloating() && _d3.isFloating()))
					_d4.setY(newY);
			}
		}

		// Update source text position so position is remembered from loading
		float newTextX = getX();
		float newTextY = getY();
		if (_textRepresentation.getX() != newTextX
				|| _textRepresentation.getY() != newTextY)
			_textRepresentation.setPosition(newTextX, newTextY);

		_settingPositionFlag = false;

		invalidateLink();

		onMoved();

		return true;
	}

	public int getX() {
		return Math.min(_d1.getX(), _d2.getX());
	}

	public int getY() {
		return Math.min(_d1.getY(), _d4.getY());
	}

	public int getWidth() {

		return Math.abs(_d2.getX() - _d1.getX());
	}

	public int getHeight() {
		return Math.abs(_d4.getY() - _d1.getY());
	}
	
	public Point getPosition() {
		return new Point(getX(), getY());
	}

	/**
	 * The order of the items in the list is as specified: _d1 _d2 _d3 _d4 _l1
	 * _l2 _l3 _l4
	 * 
	 * @return All of the Expeditee items that form the bounderies of this
	 *         widget in an unmodifiable list
	 */
	public List<Item> getItems() {
		return Collections.unmodifiableList(_expediteeItems);
	}

	public JComponent getComponant() {
		return _swingComponent;
	}

	public final void onParentStateChanged(ItemParentStateChangedEvent e) {

		// Because widgets are comprised of four corners - they all report this
		// event one after the
		// other. So must filter out redundant notifications like so:
		if (_lastParentStateChangedEvent != null
				&& _lastParentStateChangedEvent.getEventType() == e
						.getEventType()
				&& _lastParentStateChangedMouseEvent == MouseEventRouter
						.getCurrentMouseEvent())
			return; // already dealt with this event

		_lastParentStateChangedEvent = e;
		_lastParentStateChangedMouseEvent = MouseEventRouter
				.getCurrentMouseEvent();
		
		switch (e.getEventType()) {

		case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED:
		case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED_VIA_OVERLAY:
		case ItemParentStateChangedEvent.EVENT_TYPE_HIDDEN:
			if (_swingComponent.getParent() != null) {
				_swingComponent.getParent().remove(_swingComponent);
			}
			break;

		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED:
		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED_VIA_OVERLAY:
		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN:
		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN_VIA_OVERLAY:
			if (_swingComponent.getParent() == null) {
				addJComponantToFrame(e);
			}
			break;

		}

		FrameGraphics.invalidateItem(_d1, _swingComponent.getBounds());

		// Forward filtered event to upper classeses...
		onParentStateChanged(e.getEventType());
	}

	/**
	 * Override to make use of. Internally this is reported once by all corners,
	 * but is filterted out so that this method is invoked once per event.
	 * 
	 * @param eventType
	 *            The {@link ItemParentStateChangedEvent#getEventType()} that
	 *            occured.
	 * 
	 */
	protected void onParentStateChanged(int eventType) {
	}

	protected void addJComponantToFrame(ItemParentStateChangedEvent e) {

		if ((e.getEventType() == ItemParentStateChangedEvent.EVENT_TYPE_ADDED_VIA_OVERLAY || e
				.getEventType() == ItemParentStateChangedEvent.EVENT_TYPE_SHOWN_VIA_OVERLAY)
				&& e.getOverlayLevel().equals(UserAppliedPermission.none)) {
			return; // item belongs to a non-active overlay
		}

		if (_swingComponent.getParent() == null) {

			if (Browser._theBrowser != null) {
				// Due to precaching - before adding physical swing
				// componant must check to see that this widget belongs to a
				// frame that is
				// considered current. If the widget is shown however this does
				// not apply -
				// since it has been explicitly made clear the the widget is
				// shown.
				if (e.getEventType() == ItemParentStateChangedEvent.EVENT_TYPE_SHOWN
						|| e.getEventType() == ItemParentStateChangedEvent.EVENT_TYPE_SHOWN_VIA_OVERLAY
						|| e.getSource() == DisplayIO.getCurrentFrame()) {
	
					onBoundsChanged();
					Browser._theBrowser.getContentPane().add(_swingComponent);
					layout(_swingComponent);
				}

			} else { // if widgets exist on startup frame this will occur

				synchronized (_widgetsToAddLater) {
					_widgetsToAddLater.add(new DelayedWidgetEvent(this, e));
				}
				SwingUtilities.invokeLater(new AddToFrameLater());
			}

		}

	}

	/**
	 * @return True if at least one corner is floating
	 */
	public boolean isFloating() {
		return _d1.isFloating() || _d2.isFloating() || _d3.isFloating()
				|| _d4.isFloating();
	}

	public boolean areCornersFullyAnchored() {
		return _d1.getParent() != null && _d2.getParent() != null
				&& _d3.getParent() != null && _d4.getParent() != null;
	}

	/**
	 * Used for passing info to the swing thread
	 * 
	 * @author Brook Novak
	 */
	private class DelayedWidgetEvent {

		DelayedWidgetEvent(InteractiveWidget widget,
				ItemParentStateChangedEvent e) {
			_widget = widget;
			_e = e;
		}

		InteractiveWidget _widget;

		ItemParentStateChangedEvent _e;
	}

	/**
	 * Must be able to add widgets on first loaded frame: these are loaded
	 * before the browser singleton is made available.
	 */
	private static List<DelayedWidgetEvent> _widgetsToAddLater = new LinkedList<DelayedWidgetEvent>();

	/**
	 * Ensures widgets are added correctly to first loaded frame
	 */
	class AddToFrameLater implements Runnable {
		@Override
		public void run() {
			if (!_widgetsToAddLater.isEmpty()) {
				List<DelayedWidgetEvent> tmp = null;
				synchronized (_widgetsToAddLater) {
					tmp = new LinkedList<DelayedWidgetEvent>(_widgetsToAddLater);
				}
				_widgetsToAddLater.clear();
				for (DelayedWidgetEvent iwi : tmp) {
					iwi._widget.addJComponantToFrame(iwi._e);
					iwi._widget.invalidateSelf();
				}
			}
		}
	}

	final void onBoundsChanged() {
		if (isFixedSize())
			_swingComponent.setBounds(getX(), getY(), _maxWidth, _maxHeight);
		else
			_swingComponent.setBounds(getX(), getY(), getWidth(), getHeight());
	}

	/**
	 * 
	 * @return The current bounds for this widget. Never null.
	 */
	public Rectangle getBounds() {
		return new Rectangle(getX(), getY(), getWidth(), getHeight());
	}

	/**
	 * Due to absolute positioning...
	 * 
	 * @param parent
	 */
	protected void layout(Component parent) {

		parent.validate();

		if (parent instanceof Container) {
			for (Component c : ((Container) parent).getComponents()) {

				if (c instanceof Container)
					layout(c);
				else
					c.validate();
			}
		}

	}

	private void ignoreAWTPainting(Component c) {

		if (c instanceof JComponent) {
			((JComponent) c).setDoubleBuffered(false);
		}

		c.setIgnoreRepaint(true);

		if (c instanceof Container) {
			for (Component child : ((Container) c).getComponents()) {

				if (child instanceof Container) {
					ignoreAWTPainting(child);
				} else {
					if (child instanceof JComponent) {
						((JComponent) child).setDoubleBuffered(false);
					}

					child.setIgnoreRepaint(true);
				}
			}
		}
	}

	private void prepareToPaint() {
		_isReadyToPaint = true;
		layout(_swingComponent);
		ignoreAWTPainting(_swingComponent);
	}

	/**
	 * Paints the widget excluding the boundries. That is, the Swing graphics
	 * 
	 * @param g
	 */
	public void paint(Graphics g) {

		if (!_isReadyToPaint) {
			prepareToPaint();
		}

		Point loc = _swingComponent.getLocation();

		g.translate(loc.x, loc.y);
		_swingComponent.paint(g);
		g.translate(-loc.x, -loc.y);

		paintLink((Graphics2D) g);

	}

	protected void paintLink(Graphics2D g) {
		// If this widget is linked .. then draw the link icon
		if (_textRepresentation.getLink() != null
				|| _textRepresentation.hasAction()) {
			// System.out.println("Painted link");
			_textRepresentation.paintLinkGraphic(g, getLinkX(), getLinkY());

		}

	}

	private int getLinkX() {
		return getX() - Item.LEFT_MARGIN;
	}

	private int getLinkY() {
		return getY() + (getHeight() / 2);
	}

	/**
	 * Invoked whenever the widget is to be repainted in free space.
	 * 
	 * @param g
	 */
	protected void paintInFreeSpace(Graphics g) {
		g.setColor(FREESPACE_BACKCOLOR);
		g.fillRect(getX(), getY(), getWidth(), getHeight());
	}

	/**
	 * Called from the widgets corners: Whenever one of the corners are invoked
	 * for a refill of the enclosed area.
	 * 
	 * If the widget is floating (e.g. currently picked up / rubberbanding) then
	 * a shaded area is drawn instead of the actual widget so the manipulation
	 * of the widget is as smooth as possible.
	 * 
	 * @param g
	 * @param notifier
	 */
	void paintFill(Graphics g) {
		if (_swingComponent.getParent() == null) {
			// Note that frames with @f may want to paint the widgets so do not
			// paint over the widget interface in these cases: must only
			// paint if an object is floating
			if (isFloating()) {
				paintInFreeSpace(g);
				paintLink((Graphics2D) g);
			}
		}
	}

	/**
	 * @return True if this widget cannot be resized in either directions
	 */
	public boolean isFixedSize() {
		return this._minHeight == this._maxHeight
				&& this._minWidth == this._maxWidth && this._minHeight >= 0
				&& this._minWidth >= 0;
	}

	/**
	 * Removes this widget from the parent frame or free space.
	 * 
	 * @return True if removed from a parent frame. Thus a parent changed event
	 *         will be invoked.
	 * 
	 * False if removed purely from free space.
	 */
	protected boolean removeSelf() {

		Frame parent = getParentFrame();

		if (parent != null) {
			parent.removeAllItems(_expediteeItems);
		}

		FreeItems.getInstance().removeAll(_expediteeItems);

		return (parent != null);

	}

	/**
	 * @return The parent frame. Null if has none. Note: Based on corners
	 *         parents.
	 */
	public Frame getParentFrame() {

		Frame parent = null;
		if (_d1.getParent() != null)
			parent = _d1.getParent();
		else if (_d2.getParent() != null)
			parent = _d2.getParent();
		else if (_d3.getParent() != null)
			parent = _d3.getParent();
		else if (_d4.getParent() != null)
			parent = _d4.getParent();

		return parent;
	}

	protected void invalidateSelf() {
		Rectangle dirty = new Rectangle(getX(), getY(),
				getWidth(), getHeight());
		FrameGraphics.invalidateArea(dirty);
		invalidateLink();
		//FrameGraphics.refresh(true);
	}

	/**
	 * Invalidates the link for this widget - if it has one.
	 */
	protected void invalidateLink() {
		if (_textRepresentation.getLink() != null
				|| _textRepresentation.hasAction()) {
			Rectangle linkArea = _textRepresentation.getLinkDrawArea(
					getLinkX(), getLinkY());
			FrameGraphics.invalidateArea(linkArea);
		}

	}

	/**
	 * @see ItemUtils#isVisible(Item)
	 * 
	 * @return True if this widget is visible from the current frame. Considers
	 *         overlays and vectors.
	 * 
	 */
	public boolean isVisible() {
		return ItemUtils.isVisible(_d1);
	}

	/**
	 * Invoked whenever the widget have moved. Can override.
	 * 
	 */
	protected void onMoved() {
	}

	/**
	 * Invoked whenever the widget have moved. Can override.
	 * 
	 */
	protected void onSizeChanged() {
	}

	/**
	 * Override to have a custom min border thickness for your widget.
	 * 
	 * @see #DEFAULT_MINIMUM_BORDER_THICKNESS
	 * 
	 * @return The minimum border thickness. Should be larger or equal to zero.
	 * 
	 */
	public float getMinimumBorderThickness() {
		return DEFAULT_MINIMUM_BORDER_THICKNESS;
	}

	/**
	 * Looks fors a dataline in the current representation of the widget.
	 * 
	 * @see #getCurrentRepresentation
	 * @see #getStrippedDataInt(String, int)
	 * @see #getStrippedDataLong(String, long)
	 * 
	 * @param tag
	 *            The prefix of a dataline that will be matched. Must be larger
	 *            the zero and not null.
	 * 
	 * @return The <i>first</i> dataline that matched the prefix - without the
	 *         prefix. Null if their was no data that matched the given prefix.
	 * 
	 * @throws IllegalArgumentException
	 *             If tag is null.
	 * 
	 * @throws NullPointerException
	 *             If tag is empty.
	 */
	protected String getStrippedDataString(String tag) {
		if (tag == null)
			throw new NullPointerException("tag");
		else if (tag.length() == 0)
			throw new IllegalArgumentException("tag is empty");

		if (getCurrentRepresentation().getData() != null) {
			for (String str : getCurrentRepresentation().getData()) {
				if (str != null && str.startsWith(tag)
						&& str.length() > tag.length()) {
					return str.substring(tag.length());
				}
			}
		}
		return null;
	}

	/**
	 * Looks fors a dataline in the current representation of the widget.
	 * 
	 * @see #getCurrentRepresentation
	 * @see #getStrippedDataString(String)
	 * @see #getStrippedDataLong(String, long)
	 * 
	 * @param tag
	 *            The prefix of a dataline that will be matched. Must be larger
	 *            the zero and not null.
	 * 
	 * @param defaultValue
	 *            The default value if the tag does not exist or contains
	 *            invalid data.
	 * 
	 * @return The <i>first</i> dataline that matched the prefix - parsed as an
	 *         int (after the prefix). defaultValue if their was no data that
	 *         matched the given prefix or the data was invalid.
	 * 
	 * @throws IllegalArgumentException
	 *             If tag is null.
	 * 
	 * @throws NullPointerException
	 *             If tag is empty.
	 * 
	 */
	protected Integer getStrippedDataInt(String tag, Integer defaultValue) {

		String strippedStr = getStrippedDataString(tag);

		if (strippedStr != null) {
			strippedStr = strippedStr.trim();
			if (strippedStr.length() > 0) {
				try {
					return Integer.parseInt(strippedStr);
				} catch (NumberFormatException e) { /* Consume */
				}
			}
		}

		return defaultValue;
	}

	/**
	 * Looks fors a dataline in the current representation of the widget.
	 * 
	 * @see #getCurrentRepresentation
	 * @see #getStrippedDataString(String)
	 * @see #getStrippedDataInt(String, int)
	 * 
	 * @param tag
	 *            The prefix of a dataline that will be matched. Must be larger
	 *            the zero and not null.
	 * 
	 * @param defaultValue
	 *            The default value if the tag does not exist or contains
	 *            invalid data.
	 * 
	 * @return The <i>first</i> dataline that matched the prefix - parsed as a
	 *         long (after the prefix). defaultValue if their was no data that
	 *         matched the given prefix or the data was invalid.
	 * 
	 * @throws IllegalArgumentException
	 *             If tag is null.
	 * 
	 * @throws NullPointerException
	 *             If tag is empty.
	 * 
	 */
	protected Long getStrippedDataLong(String tag, Long defaultValue) {
		String strippedStr = getStrippedDataString(tag);

		if (strippedStr != null) {
			strippedStr = strippedStr.trim();
			if (strippedStr.length() > 0) {
				try {
					return Long.parseLong(strippedStr);
				} catch (NumberFormatException e) { /* Consume */
				}
			}
		}

		return defaultValue;
	}

	/**
	 * All data is removed that is prefixed with the given tag.
	 * 
	 * @param tag
	 *            The prefix of the data lines to remove. Must be larger the
	 *            zero and not null.
	 * 
	 * @throws IllegalArgumentException
	 *             If tag is null.
	 * 
	 * @throws NullPointerException
	 *             If tag is empty.
	 * 
	 */
	protected void removeData(String tag) {
		updateData(tag, null);
	}

	protected void addDataIfCaseInsensitiveNotExists(String tag) {
		if (tag == null) throw new NullPointerException("tag");
		
		List<String> data = getCurrentRepresentation().getData();
		
		if (data == null) {
			data = new LinkedList<String>();
		}
		
		for (String s : data) {
			if (s != null && s.equalsIgnoreCase(tag)) {
				return;
			}
		}
		
		data.add(tag);
		getCurrentRepresentation().setData(data);
	}

	
	/**
	 * Updates the data with a given tag. All data is removed that is prefixed
	 * with the given tag. Then a new line is added (if not null).
	 * 
	 * Note that passing newData with null is the equivelant of removing tag
	 * lines.
	 * 
	 * @param tag
	 *            The prefix of the data lines to remove. Must be larger the
	 *            zero and not null.
	 * 
	 * @param newData
	 *            The new line to add. Can be null - for not adding anything.
	 * 
	 * @throws IllegalArgumentException
	 *             If tag is null.
	 * 
	 * @throws NullPointerException
	 *             If tag is empty.
	 * 
	 * @see #removeData(String)
	 * 
	 */
	protected void updateData(String tag, String newData) {
		if (tag == null)
			throw new NullPointerException("tag");
		else if (tag.length() == 0)
			throw new IllegalArgumentException("tag is empty");

		// Get current data
		List<String> data = getCurrentRepresentation().getData();

		if (data != null) {
			for (int i = 0; i < data.size(); i++) {
				String str = data.get(i);
				if (str != null && str.startsWith(tag)) {
					data.remove(i);
				}
			}
		}

		if (newData != null) {
			if (data != null)
				data.add(newData);
			else {
				data = new LinkedList<String>();
				data.add(newData);
				getCurrentRepresentation().setData(data);

			}
		}
	}
	
	public boolean containsData(String str) {
		assert(str != null);
		if (getCurrentRepresentation().getData() != null) 
			return getCurrentRepresentation().getData().contains(str);
		return false;
	}
	
	public boolean containsDataTrimmedIgnoreCase(String str) {
		assert(str != null);
		if (getCurrentRepresentation().getData() != null) {
			for (String data : getCurrentRepresentation().getData()) {
				if (data != null && data.trim().equalsIgnoreCase(str)) {
					return true;
				}
			}
		}
			
		return false;
	}

	/**
	 * Sets the link for this widget.
	 * 
	 * @param link
	 *          The new frame link. Can be null (for no link)
	 * 
	 * @param linker
	 * 			The text item creating the link. Null if not created from
	 * 			a text item.
	 */
	public void setLink(String link, Text linker) {
		// Make sure the link is redrawn when a link is added
		if (link == null)
			invalidateLink();
		getSource().setLink(link);
		if (link != null)
			invalidateLink();
	}

	public void setBackgroundColor(Color c) {
		getSource().setBackgroundColor(c);
	}

	/**
	 * @return The link for this widget. Null if none.
	 */
	public String getLink() {
		return _textRepresentation.getLink();
	}

	/**
	 * <b>Note:</b> That if the widget has no parent (e.g. the widget is a
	 * free-item) then the absolute link returned will be for the frameset of
	 * the current frame.
	 * 
	 * @return The absolute link for this item. Null if there is no link, or if
	 *         there is no parent for this widget and the current frame is
	 *         unavailable.
	 * 
	 */
	public String getAbsoluteLink() {

		// Note: cannot return the source absolute link since it does not have
		// a parent ... thus must manually format link

		String link = getLink();

		if (link == null || link.length() == 0)
			return null;

		if (FrameIO.isPositiveInteger(link)) { // relative - convert to
												// absolute

			// Get the frameset of this item
			Frame parent = getParentFrame();
			if (parent == null)
				parent = DisplayIO.getCurrentFrame();
			if (parent == null)
				return null;

			String framesetName = parent.getFramesetName();

			if (framesetName == null)
				return null;

			return framesetName + link;

		} else if (FrameIO.isValidFrameName(link)) { // already absolute
			return link;
		}

		return null;
	}
	
	/**
	 * Sets the border color for the widget.
	 * That is, for the source (so it is remembered) and also for all the
	 * corners/edges.
	 * 
	 * @param c
	 * 		The color to set.
	 */
	public void setWidgetEdgeColor(Color c) {
		for (Item i : _expediteeItems) i.setColor(c);
		// Above indirectly invokes setSourceBorderColor accordingly
	}
	
	/**
	 * Sets the thickness of the widget edge.
	 * 
	 * @see Item#setThickness(float)
	 * 
	 * @param thickness
	 * 		The new thickness to set.
	 */
	public void setWidgetEdgeThickness(float thickness) {
		_l1.setThickness(thickness, true);
		//for (Item i : _expediteeItems) i.setThickness(thickness);
//		 Above indirectly invokes setSourceThickness accordingly
	}
	
	/**
	 * Override to dis-allow widget thickness manipulation from the user.
	 * @return
	 */
	public boolean isWidgetEdgeThicknessAdjustable() {
		return true;
	}

	// TODO: Maybe rename setSource* ..  to update* ... These should actually be friendly!
	public void setSourceColor(Color c) {
		_textRepresentation.setColor(c);
	}

	public void setSourceBorderColor(Color c) {
		_textRepresentation.setBorderColor(c);
	}

	public void setSourceFillColor(Color c) {
		_textRepresentation.setFillColor(c);
	}

	public void setSourceThickness(float newThickness, boolean setConnected) {
 		_textRepresentation.setThickness(newThickness, setConnected);
	}

	public void setSourceData(List<String> data) {
		_textRepresentation.setData(data);
	}

	protected Point getOrigin() {
		return _d1.getPosition(); // NOTE FROM BROOK: This flips around ... the origin can be any point 
	}

	protected Item getFirstCorner() {
		return _d1;
	}

	public void setAnchorLeft(Float anchor) {
		_anchorLeft = anchor;
		// Anchor left-edge corners (dots) as well
		_d1.setAnchorCornerX(anchor,null); 
		_d4.setAnchorCornerX(anchor,null); 

		if (anchor != null) {
		    setPositions(_d1, anchor, _d1.getY());
		    onResized();
		}
		
		// Move X-rayable item as well
		getCurrentRepresentation().setAnchorLeft(anchor);
	}

	public void setAnchorRight(Float anchor) {
		_anchorRight = anchor;
		// Anchor right-edge corners (dots) as well
		_d2.setAnchorCornerX(null,anchor); // right
		_d3.setAnchorCornerX(null,anchor); // right

		if (anchor != null) {
			setPositions(_d2, FrameGraphics.getMaxFrameSize().width - anchor, _d2.getY());
			onResized();
		}
		
		if (_anchorLeft == null) {
			// Prefer having the X-rayable item at anchorLeft position (if defined) over moving to anchorRight
			getCurrentRepresentation().setAnchorRight(anchor);
		}
	}
	
	public void setAnchorTop(Float anchor) {
		_anchorTop = anchor;
		// Anchor top-edge corners (dots) as well
		_d1.setAnchorCornerY(anchor,null);
		_d2.setAnchorCornerY(anchor,null); 
		
		if (anchor != null) {
		    setPositions(_d2, _d2.getX(), anchor);
		    onResized();
		}
    	
		// Move X-rayable item as well
		getCurrentRepresentation().setAnchorTop(anchor);
	}

	public void setAnchorBottom(Float anchor) {
		_anchorBottom = anchor;
		// Anchor bottom-edge corners (dots) as well
		_d3.setAnchorCornerY(null,anchor); 
		_d4.setAnchorCornerY(null,anchor); 
		
		if (anchor != null) {
		    setPositions(_d3, _d3.getX(), FrameGraphics.getMaxFrameSize().height  - anchor);
		    onResized();
		}
		
		if (_anchorTop == null) {
		    // Prefer having the X-rayable item at anchorTop position (if defined) over moving to anchorBottom
			getCurrentRepresentation().setAnchorBottom(anchor);
		}
	}


	public boolean isAnchored() {
			return (isAnchoredX()) || (isAnchoredY());
			
	        //return getSource().isAnchored();
	}

	public boolean isAnchoredX() {
		return (_anchorLeft != null) || (_anchorRight != null);
	        //return getSource().isAnchoredX();
	}

	public boolean isAnchoredY() {
		return (_anchorTop != null) || (_anchorBottom != null);
	        //return getSource().isAnchoredY();
	}
	
	/**
	 * Call from expeditee for representing the name of the item.
	 * Override to return custom name.
	 * 
	 * Note: Used for the new frame title when creating links for widgets.
	 * 
	 * @return
	 * 		The name representing this widget
	 */
	public String getName() {
		return this.toString();
	}
	
	/**
	 * Event called when the widget is left clicked while there are items attached to the FreeItems buffer.
	 * Used to enable expeditee like text-widget interaction for left mouse clicks.
	 * @return true if event was handled (no pass through), otherwise false.
	 */
	public boolean ItemsLeftClickDropped() {
		return false;
	}
	
	/**
	 * Event called when the widget is middle clicked while there are items attached to the FreeItems buffer.
	 * Used to enable expeditee like text-widget interaction for middle mouse clicks.
	 * @return true if event was handled (no pass through), otherwise false.
	 */
	public boolean ItemsMiddleClickDropped() {
		return false;
	}
	
	/**
	 * Event called when the widget is left clicked while there are items attached to the FreeItems buffer.
	 * Used to enable expeditee like text-widget interaction for right mouse clicks
	 * @return true if event was handled (no pass through), otherwise false.
	 */
	public boolean ItemsRightClickDropped() {
		return false;
	}
	
	/**
	 * Makes sure we add our KeyListener to every child component,
	 * since it seems that's the only way to make sure we capture key events in Java
	 * (without using KeyBindings which seem to only support the keyTyped event)
	 */
	@Override
	public void componentAdded(ContainerEvent e) {
		if(e == null || e.getChild() == null) {
			return;
		}
		keyListenerToChildren(e.getChild(), true);
	}
	
	@Override
	public void componentRemoved(ContainerEvent e) {
		if(e == null || e.getChild() == null) {
			return;
		}
		keyListenerToChildren(e.getChild(), false);
	}
	
	@Override
    public void keyTyped(KeyEvent e) {
		
		int keyCode = e.getKeyCode();
    	
    	if (keyCode >= KeyEvent.VK_F1 && keyCode <= KeyEvent.VK_F12) {
    		FrameKeyboardActions.getInstance().keyTyped(e);
    	}
    }

	@Override
    public void keyPressed(KeyEvent e) {
    	int keyCode = e.getKeyCode();
    	
    	if (keyCode >= KeyEvent.VK_F1 && keyCode <= KeyEvent.VK_F12) {
    		FrameKeyboardActions.getInstance().keyPressed(e);
    	}
    }
    
	@Override
    public void keyReleased(KeyEvent e) {
		int keyCode = e.getKeyCode();
    	
    	if (keyCode >= KeyEvent.VK_F1 && keyCode <= KeyEvent.VK_F12) {
    		FrameKeyboardActions.getInstance().keyReleased(e);
    	}
    }
	
	private void keyListenerToChildren(Component parent, boolean add) {
		List<Component> components = new LinkedList<Component>();
		components.add(parent);
		while(!components.isEmpty()) {
			Component c = components.remove(0);
			if(c instanceof Container) {
				components.addAll(Arrays.asList(((Container)c).getComponents()));
			}
			if(add && !Arrays.asList(c.getKeyListeners()).contains(this)) {
				c.addKeyListener(this);
			} else if (!add && Arrays.asList(c.getKeyListeners()).contains(this)) {
				c.removeKeyListener(this);
        	}
		}
	}
	
	public void onResized() {
		invalidateSelf();
    	onBoundsChanged();
    	layout(_swingComponent);
	}
	
}
