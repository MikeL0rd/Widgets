package org.expeditee.items.widgets;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.List;

import org.expeditee.items.Dot;
import org.expeditee.items.ItemParentStateChangedEvent;
import org.expeditee.items.Text;

public class WidgetCorner extends Dot {

	private InteractiveWidget _widgetSource;

	WidgetCorner(int x, int y, int id, InteractiveWidget widgetSource) {
		super(x, y, id);

		if (widgetSource == null)
			throw new NullPointerException("widgetSource");
		_widgetSource = widgetSource;
	}

	@Override
	public void updatePolygon() {
		super.updatePolygon();
		if (_widgetSource != null)
			_widgetSource.onBoundsChanged();
	}

	@Override
	public void setPosition(float x, float y) {
		invalidateFill();
		if (_widgetSource != null) { // _widgetSource == null on construction
			if (!_widgetSource.setPositions(this, x, y)) {
				super.setPosition(x, y);
			}
		} 
		else {
			super.setPosition(x, y);
		}
		invalidateFill();
	}

	@Override
	public void setXY(float x, float y) {
		invalidateFill();
		if (_widgetSource != null) { // _widgetSource == null on construction
			if (!_widgetSource.setPositions(this, x, y)) {
				super.setXY(x, y);
			}
		} 
		else {
			super.setXY(x, y);
		}
		invalidateFill();
	}

	// @Override
	// public void translate(Point2D origin, double ratio) {
	// super.translate(origin, ratio);
	// }

	@Override
	public void onParentStateChanged(ItemParentStateChangedEvent e) {
		super.onParentStateChanged(e);
		_widgetSource.onParentStateChanged(e);
	}

	public InteractiveWidget getWidgetSource() {
		return _widgetSource;
	}

	@Override
	public void paint(Graphics2D g) {
		// For fixed widgets, always have corner selected with connected context
		HighlightMode tmp = _mode; // save mode
		if (_mode == HighlightMode.Normal && _widgetSource.isFixedSize()) {
			_mode = HighlightMode.Connected; // draw as connected context for
			// fixed widgets
		}
		super.paint(g);
		_mode = tmp; // restore mode
	}

	@Override
	public void paintFill(Graphics2D g) {
		_widgetSource.paintFill(g); // only paints a fill if floating
	}

	@Override
	public void setAnnotation(boolean val) {
		// Ignore
	}

	@Override
	public boolean isAnnotation() {
		return false;
	}

	@Override
	public String toString() {
		return "WidgetCorner: [" + this.getX() + "," + this.getY() + "]";
	}

	@Override
	public void setArrow(float length, double ratio, double nib_perc) {
	}

	@Override
	public void setArrowhead(Polygon arrow) {
	}

	@Override
	public void setArrowheadLength(float length) {
	}

	@Override
	public void setArrowheadRatio(double ratio) {
	}

	@Override
	public void setBackgroundColor(Color c) {
		if (_widgetSource != null) {
			super.setBackgroundColor(c);
			_widgetSource.setBackgroundColor(c);
		}
	}

	@Override
	public void setBottomShadowColor(Color bottom) {
	}

	@Override
	public void setFillColor(Color c) {
	}

	@Override
	public void setFillPattern(String patternLink) {
	}

	@Override
	public void toggleDashed(int amount) {
	}

	@Override
	public void setSize(float size) {
	}

	@Override
	public String getLink() {
		return _widgetSource.getSource().getLink();
	}

	@Override
	public boolean isAnchored() {
		return _widgetSource.isAnchored();
	}

	@Override
	public boolean isAnchoredX() {
		return _widgetSource.isAnchoredX();
	}

	@Override
	public boolean isAnchoredY() {
		return _widgetSource.isAnchoredY();
	}

	public void setAnchorCornerX(Float anchorLeft, Float anchorRight) {
		_anchorLeft = anchorLeft;
		_anchorRight = anchorRight;
	}
	
	public void setAnchorCornerY(Float anchorTop, Float anchorBottom) {
		_anchorTop = anchorTop;
		_anchorBottom= anchorBottom;
	}
	
	@Override
	public void setAnchorLeft(Float anchor) {
		_widgetSource.setAnchorLeft(anchor);
		_anchorLeft = anchor;
		_anchorRight = null;
	}

	@Override
	public void setAnchorRight(Float anchor) {
		_widgetSource.setAnchorRight(anchor);
		_anchorLeft = null;
		_anchorRight = anchor;
	}
	
	@Override
	public void setAnchorTop(Float anchor) {
		_widgetSource.setAnchorTop(anchor);
		_anchorTop = anchor;
		_anchorBottom = null;
	}

	@Override
	public void setAnchorBottom(Float anchor) {
		_widgetSource.setAnchorBottom(anchor);
		_anchorTop = null;
		_anchorBottom = anchor;
	}

	/*
	@Override
	public Float getAnchorTop() {
		return _widgetSource.getSource().getAnchorTop();
	}

	@Override
	public Float getAnchorBottom() {
		return _widgetSource.getSource().getAnchorBottom();
	}

	@Override
	public Float getAnchorLeft() {
		return _widgetSource.getSource().getAnchorLeft();
	}

	@Override
	public Float getAnchorRight() {
		return _widgetSource.getSource().getAnchorRight();
	}
*/
	
	@Override
	public void setLink(String link) {
		_widgetSource.setLink(link, null);
	}
	
	/**
	 * @param link
	 *          The new frame link. Can be null (for no link)
	 * 
	 * @param linker
	 * 			The text item creating the link. Null if not created from
	 * 			a text item.
	 */
	public void setLink(String link, Text linker) {
		_widgetSource.setLink(link, linker);
	}

	@Override
	public void setFormula(String formula) {
	}

	@Override
	public void setData(List<String> data) {
		_widgetSource.setSourceData(data);
	}

	@Override
	public boolean contains(int x, int y) {
		return super.contains(x, y)
				&& !_widgetSource.getBounds().contains(x, y);
	}

	@Override
	public void setThickness(float newThickness, boolean setConnected) {
		if (_widgetSource != null) {
			float minThickness = _widgetSource.getMinimumBorderThickness();
			if (newThickness < minThickness)
				newThickness = minThickness;
			super.setThickness(newThickness, setConnected);
			_widgetSource.setSourceThickness(newThickness, false);
		}
	}

	@Override
	public void setColor(Color color) {
		if (_widgetSource != null) {
			super.setColor(color);
			_widgetSource.setSourceBorderColor(color);
		}
	}

	@Override
	public String getName() {
		return _widgetSource.getName();
	}
	
	
}
