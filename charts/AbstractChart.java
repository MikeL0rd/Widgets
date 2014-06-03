package org.expeditee.items.widgets.charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Paint;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.JPanel;

import org.expeditee.gui.ColorUtils;
import org.expeditee.gui.Frame;
import org.expeditee.gui.FrameGraphics;
import org.expeditee.gui.FrameIO;
import org.expeditee.gui.FrameKeyboardActions;
import org.expeditee.gui.FrameMouseActions;
import org.expeditee.gui.FunctionKey;
import org.expeditee.items.Text;
import org.expeditee.items.widgets.DataFrameWidget;
import org.expeditee.settings.templates.TemplateSettings;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.title.Title;
import org.jfree.ui.RectangleInsets;

public abstract class AbstractChart extends DataFrameWidget {

	protected static final String DEFAULT_TITLE = "";

	protected static final String DEFAULT_XAXIS = "X";

	protected static final String DEFAULT_YAXIS = "Y";

	private JFreeChart _chart = null;

	private LegendTitle _legend = null;

	protected Map<String, Paint> _paints;

	protected JFreeChart getChart() {
		return _chart;
	}

	public AbstractChart(Text source, String[] args) {
		super(source, new JPanel(), 100, 300, -1, 100, 300, -1);
		_paints = new LinkedHashMap<String, Paint>();
		init();
		refresh();
	}

	@Override
	protected String[] getArgs() {
		return null;
	}

	protected void init() {
		// create a chart...
		_chart = createNewChart();
		_chart.getPlot().setNoDataMessage("Add data to chart");
		_legend = _chart.getLegend();
		ChartPanel cp = new ChartPanel(_chart);
		cp.setPopupMenu(null);
		cp.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				int index = e.getKeyCode() - KeyEvent.VK_F1 + 1;
				// Make sure the key is within range
				if (index < 0 || index >= FunctionKey.values().length) {
					return;
				}

				FunctionKey key = FunctionKey.values()[index];
				switch (key) {
				case SizeUp:
					invalidateLink();
					FrameKeyboardActions.SetSize(getFirstCorner(), 1, false,
							true, false);
					invalidateSelf();
					FrameGraphics.refresh(true);
					// FrameGraphics.requestRefresh(true);
					break;
				case SizeDown:
					invalidateLink();
					FrameKeyboardActions.SetSize(getFirstCorner(), -1, false,
							true, false);
					invalidateSelf();
					FrameGraphics.refresh(true);
					// FrameGraphics.ForceRepaint();
					// FrameGraphics.refresh(true);
					// FrameGraphics.requestRefresh(true);
					break;
				case ChangeColor:
					if (e.isControlDown()) {

						if (e.isShiftDown()) {
							setSourceFillColor(null);
						} else {
							Color newColor = ColorUtils.getNextColor(
									getSource().getFillColor(),
									TemplateSettings.BackgroundColorWheel.get(), null);
							setSourceFillColor(newColor);
						}
					} else {
						if (e.isShiftDown()) {
							setBackgroundColor(null);
						} else {
							Color newColor = ColorUtils.getNextColor(
									getSource().getColor(), TemplateSettings.ColorWheel.get(),
									null);
							setSourceColor(newColor);
						}
					}
					break;
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
			}

			@Override
			public void keyTyped(KeyEvent e) {
				FrameKeyboardActions.processChar(e.getKeyChar(), e
						.isShiftDown());
			}
		});
		cp.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				FrameMouseActions.getInstance().mouseClicked(translateEvent(e));
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				// FrameMouseActions.getInstance().mouseEntered(translateEvent(e));
			}

			@Override
			public void mouseExited(MouseEvent e) {
				// FrameMouseActions.getInstance().mouseExited(translateEvent(e));
			}

			@Override
			public void mousePressed(MouseEvent e) {
				FrameMouseActions.getInstance().mousePressed(translateEvent(e));
			}

			private MouseEvent translateEvent(MouseEvent e) {
				Point origin = getOrigin();
				return new MouseEvent((Component) e.getSource(), e.getID(), e
						.getWhen(), e.getModifiers(), origin.x + e.getX(),
						origin.y + e.getY(), e.getXOnScreen(),
						e.getYOnScreen(), e.getClickCount(), false, e
								.getButton());
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				FrameMouseActions.getInstance()
						.mouseReleased(translateEvent(e));
			}

		});
		super._swingComponent = cp;

		setSourceColor(getSource().getColor());
		setSourceBorderColor(getSource().getBorderColor());
		setSourceFillColor(getSource().getFillColor());
		setBackgroundColor(getSource().getBackgroundColor());
		setSourceThickness(getSource().getThickness(), false);
	}

	protected abstract JFreeChart createNewChart();

	@Override
	public final void refresh() {
		super.refresh();
		clearData();
		// Get the data from the linked frame
		Frame dataFrame = getDataFrame();
		_paints.clear();
		_chart.clearSubtitles();

		if (dataFrame != null) {
			_chart.setTitle(dataFrame.getTitle());

			refreshData(dataFrame);

			if (dataFrame.hasAnnotation("legend")) {
				_chart.addLegend(_legend);
			}
			if (dataFrame.hasAnnotation("subtitle")) {
				getChart().addSubtitle(
						new TextTitle(dataFrame.getAnnotationValue("subtitle"),
								JFreeChart.DEFAULT_TITLE_FONT.deriveFont(
										Font.ITALIC,
										JFreeChart.DEFAULT_TITLE_FONT
												.getSize2D() * .7F)));
			}

			refreshPlot(dataFrame, _chart.getPlot());

		} else {
			_chart.setTitle(this.getClass().getSimpleName() + " Chart");
		}
		_chart.getPlot().setDataPaints(_paints);
		super._swingComponent.invalidate();
	}

	/**
	 * @param dataFrame
	 */
	protected abstract void refreshPlot(Frame dataFrame, Plot simplePlot);

	/**
	 * @param dataFrame
	 */
	protected void refreshData(Frame dataFrame) {
		clearSubjects();
		
		boolean swap = false;
		if (dataFrame.hasAnnotation("swap")) {
			swap = true;
		}
		boolean foundData = false;
		// First do a pass to see if there are any line ends
		Collection<Text> textItems = dataFrame.getNonAnnotationText(true);
		textItems.remove(dataFrame.getTitleItem());

		// Search for line ends
		for (Text text : textItems) {
			if (!text.isLineEnd()) {
				continue;
			}
			foundData |= addCategoryData(text.getText(), text
					.getEnclosedNonAnnotationText(), swap);
		}

		// As a second option look for linked items on the frame
		if (!foundData) {
			for (Text category : textItems) {
				if (category.isLineEnd() || category.getLink() == null)
					continue;
				Frame linkFrame = FrameIO.LoadFrame(category.getAbsoluteLink());
				if (linkFrame == null)
					continue;

				String categoryName = category.getText();
				if (foundData |= addCategoryData(categoryName, linkFrame
						.getNonAnnotationText(true), swap)) {
					Color backgroundColor = category.getBackgroundColor();
					if (backgroundColor != null)
						_paints.put(categoryName, backgroundColor);
					addSubject(linkFrame);
				}
			}
		}

		// If there were no groupings in boxes then just get all the values on
		// the frame
		if (!foundData) {
			foundData |= addCategoryData("", textItems, swap);
		}
		/* if (foundData) */{
			/*
			 * At minimum add the data frame as a subject even if there is no data
			 * on it... yet
			 */
			addSubject(dataFrame);
		}
	}

	protected boolean addCategoryData(String categoryName,
			Collection<Text> items, boolean swap) {
		return true;
	}

	@Override
	public float getMinimumBorderThickness() {
		return 1.0F;
	}

	protected abstract void clearData();

	@Override
	public void setBackgroundColor(Color c) {
		super.setBackgroundColor(c);
		if (_chart == null)
			return;
		if (c == null) {
			_chart.setBackgroundPaint(JFreeChart.DEFAULT_BACKGROUND_PAINT);
		} else {
			_chart.setBackgroundPaint(c);
		}
	}

	@Override
	public void setSourceColor(Color c) {
		super.setSourceColor(c);
		if (_chart == null)
			return;
		if (c == null) {
			_chart.getTitle().setPaint(TextTitle.DEFAULT_TEXT_PAINT);
		} else {
			_chart.getTitle().setPaint(c);
		}
		for (Title t : _chart.getSubtitles()) {
			if (c == null) {
				t.setPaint(TextTitle.DEFAULT_TEXT_PAINT);
			} else {
				t.setPaint(c);
			}
		}
		LegendTitle legend = _chart.getLegend();
		if (legend != null) {
			if (c == null) {
				legend.setFrame(new BlockBorder(Color.black));
			} else {
				legend.setFrame(new BlockBorder(c));
			}
		}
	}

	@Override
	public void setSourceBorderColor(Color c) {
		super.setSourceBorderColor(c);
		if (_chart == null)
			return;
		if (c == null) {
			_chart.getPlot().setOutlinePaint(Plot.DEFAULT_OUTLINE_PAINT);
		} else {
			_chart.getPlot().setOutlinePaint(c);
		}
	}

	@Override
	public void setSourceFillColor(Color c) {
		super.setSourceFillColor(c);
		if (_chart == null)
			return;
		LegendTitle legend = _chart.getLegend();
		if (c == null) {
			_chart.getPlot().setBackgroundPaint(Plot.DEFAULT_BACKGROUND_PAINT);
			if (legend != null)
				legend.setBackgroundPaint(null);
		} else {
			_chart.getPlot().setBackgroundPaint(c);
			if (legend != null)
				legend.setBackgroundPaint(c);
		}
	}

	@Override
	public void setSourceThickness(float newThickness, boolean setConnected) {
		super.setSourceThickness(newThickness, setConnected);
		if (_chart == null)
			return;
		_chart.setBorderStroke(new BasicStroke(newThickness));
		_chart.getPlot().setOutlineStroke(new BasicStroke(newThickness));

		float fontSize = getFontSize(newThickness, TextTitle.DEFAULT_FONT
				.getSize2D());
		LegendTitle legend = _chart.getLegend();
		if (legend != null) {
			legend.setFrame(new BlockBorder(newThickness, newThickness,
					newThickness, newThickness, getSource().getPaintColor()));
			legend.setItemFont(legend.getItemFont().deriveFont(fontSize));
			float pad = newThickness + 1;
			float pad2 = pad;
			legend.setItemLabelPadding(new RectangleInsets(pad, pad, pad, pad));
			legend.setLegendItemGraphicPadding(new RectangleInsets(pad2, pad2,
					pad2, pad2));
			legend.setMargin(newThickness, newThickness, newThickness,
					newThickness);
		}
		float pad0 = newThickness - 1;
		_chart.setPadding(new RectangleInsets(pad0, pad0, pad0, pad0));
		TextTitle title = _chart.getTitle();
		title.setFont(title.getFont().deriveFont(
				getFontSize(newThickness, JFreeChart.DEFAULT_TITLE_FONT
						.getSize2D())));

		for (Title t : _chart.getSubtitles()) {
			if (t instanceof TextTitle) {
				((TextTitle) t).setFont(legend.getItemFont().deriveFont(
						fontSize));
			}
			t.setPadding(pad0, pad0, pad0, pad0);
		}
	}

	static protected float getFontSize(float newThickness, float oldFontSize) {
		return (newThickness + 5) * oldFontSize / 6;
	}
}
