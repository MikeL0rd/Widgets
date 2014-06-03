package org.expeditee.items.widgets;

import java.awt.FlowLayout;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import org.expeditee.items.Text;

public class SampledHDWidget1 extends HeavyDutyInteractiveWidget {
	
	private boolean isIndeterminant = false;
	private Object loadInterrupter = new Object();
	
	public SampledHDWidget1(Text source, String[] args) {
		super(source, new JPanel(new FlowLayout()), 50, 600, 50, 400, 1);
		
		JPanel p = (JPanel)super._swingComponent;

		p.add(new JButton("Click me"));
		p.add(new JButton("Click me # 2"));
		p.add(new JLabel("I am a label"));
		p.add(new JComboBox(new String[] {"dog", "fish", "cat", "pig"}));

		if (args != null) {
			for (String arg : args) {
				if (arg != null && arg.equalsIgnoreCase("indi")) {
					isIndeterminant = true;
					break;
				}
			}
		}
	}

	@Override
	protected String[] getArgs() {
		if (isIndeterminant) return new String[] {"indi"};
		return null;
	}
	
	@Override
	protected void cancelLoadWidgetData() {
		System.out.println(getClass().getName() + ":cancelLoadWidgetData");
		synchronized(loadInterrupter) {
			loadInterrupter.notify();
		}
		
	}

	@Override
	protected float loadWidgetData() {
		
		Random rand = new Random();
		rand.setSeed(System.currentTimeMillis());
		
		// fail some of them
		if ((rand.nextInt() % 4) == 0) {
			setLoadScreenMessage("Failed to load metadata");
			return LOAD_STATE_FAILED;
		}
		
		setLoadScreenMessage("Resolving paths...");
		
		if (isIndeterminant) updateLoadPercentage(-1.0f);

		int totalLoadTime = 2000 + (Math.abs(rand.nextInt()) % 10000);
		int loadTimeLeft = totalLoadTime;
		
		while (loadTimeLeft > 0) {
			int waitTime = 100 + (Math.abs(rand.nextInt()) % 2000);
			
			try {
				synchronized(loadInterrupter) {
					loadInterrupter.wait(waitTime);
				}
			} catch (InterruptedException e) { /* Consume */ }

			if (hasCancelBeenRequested()) {
				// Release resources
				System.out.println(getClass().getName() + ":INTERUPTED LOAD - EXITING");
				return LOAD_STATE_INCOMPLETED;
			}
			
			float perc = ((float)(totalLoadTime - loadTimeLeft)) / ((float)totalLoadTime);
			if (perc <= 0.0f) perc = 0.01f;
			updateLoadPercentage(perc);
			
			if (perc > 0.4f && perc < 1.0f) {
				setLoadScreenMessage("Loading metadata...");
			}
			
			loadTimeLeft -= waitTime;
		}

		return LOAD_STATE_COMPLETED;
	}
	
	@Override
	public int getLoadDelayTime() {
		return 1000;
	}

	@Override
	public boolean doesNeedSaving() {
		return false;
	}

	@Override
	public String getSaveName() {
		return "";
	}

	@Override
	protected void saveWidgetData() {
		System.out.println(getClass().getName() + ":saveWidgetData");
	}

	@Override
	protected void unloadWidgetData() {
		System.out.println(getClass().getName() + ":unloadWidgetData");
	}

	@Override
	protected void tempUnloadWidgetData() {
		System.out.println(getClass().getName() + ":tempUnloadWidgetData");
	}


}
