package org.expeditee.items.widgets;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.expeditee.items.ItemParentStateChangedEvent;
import org.expeditee.items.Text;

/**
 * A widget for displaying the heap space uasage at runtime
 * 
 * @author Brook Novak
 *
 */
public class MemoryMonitor extends InteractiveWidget {
	
	private long totalMemoryInBytes;
	private float currentMemoryUsage;
	private MonitorThread monitorThread = null;

	private static final Font USAGE_FONT = new Font("Arial", Font.BOLD, 12);
	

	public MemoryMonitor(Text source, String[] args) {
		super(source, new JPanel(), 40, 40, 40, 40);
		updateMemoryUsage();
	}
	
	@Override
	protected String[] getArgs() {
		return null;
	}
	
	private void updateMemoryUsage()
	{
		totalMemoryInBytes = Runtime.getRuntime().totalMemory();
		currentMemoryUsage =  (totalMemoryInBytes - Runtime.getRuntime().freeMemory()) / (float)totalMemoryInBytes;
		invalidateSelf();
	}
	
	@Override
	public void paint(Graphics g) {
		
		int height = getHeight();
		int width = getWidth();
		
		int memHeight = (int)(height * currentMemoryUsage);
		
		Color memColor;
		if (currentMemoryUsage > 0.8f) {
			memColor = Color.RED;
		} else if (currentMemoryUsage > 0.5f) {
			memColor = Color.ORANGE;
		} else  {
			memColor = Color.GREEN;
		}
		
		g.setColor(memColor);
		g.fillRect(getX(), getY() + height - memHeight, width, memHeight);
		
		g.setColor(Color.WHITE);
		g.fillRect(getX(), getY(), width, height - memHeight);
		
		int percent = (int)(currentMemoryUsage * 100.0f);
		
		g.setColor(Color.BLACK);
		g.setFont(USAGE_FONT);
		g.drawString(percent + "%", getX() + 12, getY() + (height / 2) + 8);
		
		paintLink((Graphics2D)g);
	}


	@Override
	protected void onParentStateChanged(int eventType) {
		super.onParentStateChanged(eventType);

		switch (eventType) {
		case ItemParentStateChangedEvent.EVENT_TYPE_HIDDEN:
		case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED:
		case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED_VIA_OVERLAY:
			if (monitorThread != null) {
				monitorThread.destroy();
				monitorThread = null;
			} 
			break;

		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED:
		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED_VIA_OVERLAY:
		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN:
		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN_VIA_OVERLAY:
			if (monitorThread == null) {
				monitorThread = new MonitorThread();
				monitorThread.start();
			}
			break;
			
		}

	}

	private class MonitorThread extends Thread
	{
		private MonitorGUIUpdator guiRunner = new MonitorGUIUpdator();

		public MonitorThread()
		{
			setDaemon(true);
		}
		
		@Override
		public void destroy()
		{
			interrupt();
		}
		
		@Override
		public void run()
		{
			try {
				while(!isInterrupted())
				{
					SwingUtilities.invokeLater(guiRunner);
					sleep(5000);
				}
			} catch (InterruptedException e) {
			}
		}
		
		private class MonitorGUIUpdator implements Runnable
		{
			@Override
			public void run()
			{
				updateMemoryUsage();
			}
		}
	}
}
