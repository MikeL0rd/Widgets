package org.expeditee.items.widgets;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics2D;
import java.util.LinkedList;
import java.util.List;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.swing.JPanel;

import org.expeditee.actions.Javascript2;
import org.expeditee.gui.DisplayIO;
import org.expeditee.items.ItemParentStateChangedEvent;
import org.expeditee.items.JSThreadable;
import org.expeditee.items.Text;

/**
 * Class for extending Expeditee with widgets coded in Javascript
 * 
 * @author jts21
 *
 */
public class JSWidget extends DataFrameWidget implements JSThreadable {
	
	private static final Object global = Javascript2.eval("new Object()");
	
	// a method to run that will set up and return the root JComponent for this Widget
	private final String init;
	// a method to run to populate a List<String> with our state
	private final String save;
	// a method to run that will load state from a String[]
	private final String load;
	
	// we have our own script context since it needs to have some global variables which are specific to each widget
	private final ScriptEngine scriptEngine;
	private final Invocable invocable;
	// component created by running our constructor
	private final Component component;
	// container for our component
	private final JPanel container;
	
	private static Text getSauce() {
		Text source = new Text(DisplayIO.getCurrentFrame().getNextItemID(), "@iw: org.expeditee.items.widgets.JSWidget");
		source.setParent(DisplayIO.getCurrentFrame());
		return source;
	}
	
	private JSWidget(Text source, int width, int height, String init, String save, String load) throws Exception {
		super(source, new JPanel(new BorderLayout()), -1, width, -1, -1, height, -1);
		this.init = init;
		this.save = save;
		this.load = load;
		this.container = (JPanel) super._swingComponent;
		this.scriptEngine = Javascript2.scriptEngineManager.getEngineByMimeType("application/javascript");
		this.invocable = (Invocable) this.scriptEngine;
		this.scriptEngine.put("global", global);
		this.scriptEngine.put("invocable", this.invocable);
		this.scriptEngine.put("widget", this);
		this.scriptEngine.put("container", this.container);
    	System.out.println(this.init);
    	this.component = (Component) this.scriptEngine.eval("var init = " + this.init + "\ninit()");
    	this.container.add(component);
    	this.scriptEngine.put("component", this.component);
    	this.scriptEngine.eval("save = " + this.save);
    	this.scriptEngine.eval("load = " + this.load);
	}
	
	private JSWidget(Text source, String init, String save, String load) throws Exception {
		this(source, 100, 100, init, save, load);
	}
	
	public JSWidget(Text source, String[] args) throws Exception {
		this(source, source.getData().get(0).replaceAll("\\\\n", "\n"),
				source.getData().get(1).replaceAll("\\\\n", "\n"),
				source.getData().get(2).replaceAll("\\\\n", "\n"));
		this.invocable.invokeFunction("load", (Object) args);
	}
	
	public JSWidget(int width, int height, String init, String save, String load) throws Exception {
		this(getSauce(), width, height, init, save, load);
	}
	
	public JSWidget(String init, String save, String load) throws Exception {
		this(100, 100, init, save, load);
	}
	
	@Override
	protected List<String> getData() {
		List<String> value = new LinkedList<String>();
		value.add(this.init.replaceAll("[\n\r]", "\\\\n"));
		value.add(this.save.replaceAll("[\n\r]", "\\\\n"));
		value.add(this.load.replaceAll("[\n\r]", "\\\\n"));
		return value;
	}

    @Override
	protected String[] getArgs() {
		try {
			List<String> args = new LinkedList<String>();
			this.invocable.invokeFunction("save", (Object)args);
			return args.toArray(new String[0]);
        } catch (Exception e) {
        	e.printStackTrace();
	        return null;
        }
	}
    
    private List<JSThread> threads = new LinkedList<JSThread>();
    
    @Override
	public JSThread addThread(String code) {
		JSThread t = new JSThread(scriptEngine, code);
		this.threads.add(t);
		return t;
	}
    
    @Override
	public void onParentStateChanged(int e) {
		switch (e) {
    		case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED:
    		case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED_VIA_OVERLAY:
    		case ItemParentStateChangedEvent.EVENT_TYPE_HIDDEN:
    			for(JSThread t : this.threads) {
    				t.kill();
    			}
    			break;
    
    		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED:
    		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED_VIA_OVERLAY:
    		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN:
    		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN_VIA_OVERLAY:
    			for(JSThread t : this.threads) {
					t.resume();
    			}
    			break;
		}
	}
    
    @Override
	protected void paintLink(Graphics2D g) {
    	return;
	}
}
