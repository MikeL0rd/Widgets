package org.expeditee.items.widgets;

import java.net.MalformedURLException;
import java.util.List;

import org.expeditee.gui.MessageBay;
import org.expeditee.items.Text;
import org.lobobrowser.gui.BrowserPanel;
import org.lobobrowser.main.PlatformInit;

public class Browser extends DataFrameWidget {

	private BrowserPanel _browser;

	public Browser(Text source, String[] args) {
		super(source, new BrowserPanel(), 100, 500, -1, 100, 300, -1);

		try {
			// This optional step initializes logging so only warnings
			// are printed out.
			PlatformInit.getInstance().initLogging(false);
			// This step is necessary for extensions to work:
			PlatformInit.getInstance().init(false, false);
			PlatformInit.getInstance().initLookAndFeel();
			PlatformInit.getInstance().initSecurity();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		_browser = (BrowserPanel) _swingComponent;
		String url = "http://www.google.com";
		List<String> data = getSource().getData();
		if (data != null && data.size() > 0) {
			url = data.get(0);
		}
		navigate(url);
	}

	@Override
	protected String[] getArgs() {
		return null;
	}

	public void navigate(String url) {
		try {
			getSource().setData(url);
			_browser.navigate(url);
		} catch (MalformedURLException e) {
			MessageBay.errorMessage("Could not navigate to " + url);
		}
	}

	@Override
	public void setSourceData(List<String> data) {
		super.setSourceData(data);
		if (data != null && data.size() > 0) {
			navigate(data.get(0));
		}
	}
	
	@Override
	protected List<String> getData() {
		return _textRepresentation.getData();
	}
	
}
