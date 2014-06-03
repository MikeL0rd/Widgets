package org.expeditee.items.widgets;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Collection;
import java.util.HashSet;

import javax.swing.JComponent;

import org.expeditee.gui.DisplayIO;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameObserver;
import org.expeditee.items.Item;
import org.expeditee.items.ItemParentStateChangedEvent;
import org.expeditee.items.Text;

public abstract class DataFrameWidget extends InteractiveWidget implements
		FrameObserver {

	private boolean _needsUpdating;

	protected Collection<Frame> _subjects = new HashSet<Frame>();

	protected Frame _dataFrame;

	protected DataFrameWidget(Text source, JComponent component, int minWidth,
			int maxWidth, int minHeight, int maxHeight) {
		this(source, component, minWidth, minWidth, maxWidth, minHeight,
				minHeight, maxHeight);
	}

	public DataFrameWidget(Text source, JComponent component, int minWidth,
			int defaultWidth, int maxWidth, int minHeight, int defaultHeight,
			int maxHeight) {
		super(source, component, minWidth, maxWidth, minHeight, maxHeight);
		setSize(defaultWidth, defaultHeight);

		String link = source.getAbsoluteLink();
		_dataFrame = link != null ? FrameIO.LoadFrame(link) : null;
		if (_dataFrame != null) {
			addSubject(_dataFrame);
		}
	}

	protected void setDataFrame(Frame newDataFrame) {
		_dataFrame = newDataFrame;
	}

	public Frame getDataFrame() {
		if (_dataFrame == null && getSource().getLink() != null) {
			_dataFrame = FrameIO.LoadFrame(getSource().getAbsoluteLink());
			if (_dataFrame != null) {
				addSubject(_dataFrame);
			} else {
				/**
				 * If the dataFrame has not been saved yet because it has just
				 * been created via TDFC this widget needs to be marked as
				 * changed so it will be refreshed when the user goes back
				 */
				assert (false);
				// _needsUpdating = true;
				// update();
			}
		}
		return _dataFrame;
	}

	protected void clearSubjects() {
		for (Frame frame : _subjects) {
			frame.removeObserver(this);
		}
		_subjects.clear();
	}

	@Override
	public void removeSubject(Frame frame) {
		assert (frame != null);
		_subjects.remove(frame);
		frame.removeObserver(this);
		// Reset the dataFrame if it is being removed from the cache to avoid
		// memory leaks
		if (frame == _dataFrame)
			_dataFrame = null;
	}

	public void addSubject(Frame frame) {
		assert (frame != null);
		_subjects.add(frame);
		frame.addObserver(this);
	}

	public boolean needsRefresh() {
		return _needsUpdating;
	}

	public void refresh() {
		_needsUpdating = false;
	}

	@Override
	public void update() {
		Frame parent = getParentFrame();
		if (parent != null && parent == DisplayIO.getCurrentFrame()) {
			refresh();
		}

		_needsUpdating = true;
	}

	@Override
	public void setLink(String link, Text linker) {
		String newLink = Item.convertToAbsoluteLink(link);
		String oldLink = getSource().getAbsoluteLink();
		if ((newLink == null && oldLink == null)
				|| (newLink != null && newLink.equals(oldLink)))
			return;
		super.setLink(link, linker);
		clearSubjects();
		setDataFrame(null);
		if (oldLink == null) {
			_needsUpdating = true;
			// Need to refresh imediately so that the data appears when adding a
			// link to a graph
			refresh();
		} else {
			refresh();
		}
	}

	@Override
	protected void onParentStateChanged(int eventType) {

		switch (eventType) {

		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED:
		case ItemParentStateChangedEvent.EVENT_TYPE_ADDED_VIA_OVERLAY:
		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN:
		case ItemParentStateChangedEvent.EVENT_TYPE_SHOWN_VIA_OVERLAY:
			if (needsRefresh()) {
				refresh();
			}
			break;

		case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED:
		case ItemParentStateChangedEvent.EVENT_TYPE_REMOVED_VIA_OVERLAY:
		case ItemParentStateChangedEvent.EVENT_TYPE_HIDDEN:
			break;
		}
	}

	@Override
	protected void paintInFreeSpace(Graphics g) {
		super.paintInFreeSpace(g);
		g.setFont(((Text) getSource()).getFont());
		g.setColor(Color.WHITE);
		g.drawString(this.getClass().getSimpleName(), getX() + 10, getY() + 20);

	}
}
