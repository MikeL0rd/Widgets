package org.expeditee.items.widgets;

import java.util.List;

import javax.swing.SwingUtilities;

import org.expeditee.gui.MessageBay;
import org.expeditee.items.Text;

import chrriis.common.UIUtils;
import chrriis.dj.nativeswing.NativeSwing;
import chrriis.dj.nativeswing.swtimpl.NSPanelComponent;
import chrriis.dj.nativeswing.swtimpl.NativeInterface;
import chrriis.dj.nativeswing.swtimpl.components.JWebBrowser;

/**
 * This class is used to create an embedded web browser
 * widget to be used within Expeditee. 
 * @author kgas1 - 31/01/2012
 **/
public class WebBrowser extends DataFrameWidget {
	
	private JWebBrowser _webBrowser;
	
	String[] _args = null;
	
	public WebBrowser(Text source, String[] args) {

		//pass the JWebBrowser a constructor to destroy on finalization so the 
		//web browser can be reconstructed every time we leave the frame that it is 
		//located on and want to go back to that frame. Also allows
		//us to move the browser around the frame and resize it. - kgas1 31/01/2012.
		
		super(source, new JWebBrowser(NSPanelComponent.destroyOnFinalization(),NSPanelComponent.constrainVisibility()), 
				100, 500, -1, 100, 300, -1);

			
		NativeSwing.initialize();
		UIUtils.setPreferredLookAndFeel();
		NativeInterface.open();
		_webBrowser = (JWebBrowser) _swingComponent;
		
		if(args != null){
			_args = args.clone();
			
			for(int i = 0; i < args.length; i++){
				String[] split = args[i].split("=");
				
				if(split[0].equals("locationBar")){
					_webBrowser.setLocationBarVisible(Boolean.parseBoolean(split[1]));
				}
				else if(split[0].equals("buttonBar")){
					_webBrowser.setButtonBarVisible(Boolean.parseBoolean(split[1]));
				}
				else if(split[0].equals("menuBar")){
					_webBrowser.setMenuBarVisible(Boolean.parseBoolean(split[1]));
				}
				else if(split[0].equals("statusBar")){
					_webBrowser.setStatusBarVisible(Boolean.parseBoolean(split[1]));
				}
			}
		}
		
		final String url;
				
		List<String> data = getData();
					
		if(data != null && data.size() > 0) {
				url = data.get(0);
		}
		else {
				System.out.println("no data can be found");
				url = "http://www.waikato.ac.nz";
		}

		navigate(url);
	}
	

	@Override
	protected String[] getArgs() {
		return _args;
		
	}
	
	public void navigate(final String url) {
		try
		{
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run(){
					getSource().setData(url);
					_webBrowser.navigate(url);
				}
			});
	
		}
		catch(Exception e) {
			MessageBay.errorMessage(e.getMessage());
		}
		
	}
	
	@Override
	public void setSourceData(List<String> data) {
		super.setSourceData(data);
		if(data != null && data.size() > 0) {
			navigate(data.get(0));
		}
		return;
	}

	
	@Override
	protected List<String> getData() {
		return _textRepresentation.getData();
	}
	
}