package org.expeditee.items.widgets.charts;

import java.awt.BasicStroke;
import java.awt.Color;

import org.expeditee.gui.AttributeValuePair;
import org.expeditee.gui.Frame;
import org.expeditee.items.Text;
import org.jfree.chart.labels.PieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.Plot;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.ui.RectangleInsets;

public abstract class AbstractPie extends AbstractChart {

	protected DefaultPieDataset _data;

	private PieSectionLabelGenerator _labelGenerator;

	protected DefaultPieDataset getChartData() {
		if (_data == null)
			_data = new DefaultPieDataset();
		return _data;
	}

	public AbstractPie(Text source, String[] args) {
		super(source, args);
	}

	@Override
	protected void init() {
		super.init();
		_labelGenerator = ((PiePlot) getChart().getPlot()).getLabelGenerator();
	}

	/**
	 * @param dataFrame
	 */
	@Override
	protected void refreshPlot(Frame dataFrame, Plot plot) {
		if (dataFrame.hasAnnotation("labels")) {
			((PiePlot) plot).setLabelGenerator(_labelGenerator);
		} else {
			((PiePlot) plot).setLabelGenerator(null);
		}
	}

	@Override
	protected void clearData() {
		_data.clear();
	}

	/**
	 * @param dataFrame
	 */
	@Override
	protected void refreshData(Frame dataFrame) {
		for (Text t : dataFrame.getBodyTextItems(false)) {
			// Ignore line ends for pie charts
			//TODO find out HOW text became null when i was doing a graph!!
			String text = t.getText();
			if (t.isLineEnd() || text == null)
				continue;
			AttributeValuePair avp = new AttributeValuePair(text);
			if (avp != null && avp.hasPair()) {
				try {
					_data.setValue(avp.getAttribute(), avp.getDoubleValue());
					_paints.put(avp.getAttribute(), t.getBackgroundColor());
				} catch (NumberFormatException nfe) {

				}
			}
		}
	}

	@Override
	public void setSourceBorderColor(Color c) {
		super.setSourceBorderColor(c);
		if (getChart() == null)
			return;
		PiePlot plot = ((PiePlot) getChart().getPlot());
		if (c == null) {
			plot.setBaseSectionOutlinePaint(Plot.DEFAULT_OUTLINE_PAINT);
			plot.setLabelLinkPaint(PiePlot.DEFAULT_LABEL_OUTLINE_PAINT);
			plot.setLabelOutlinePaint(PiePlot.DEFAULT_LABEL_OUTLINE_PAINT);
			plot.setLabelShadowPaint(PiePlot.DEFAULT_LABEL_SHADOW_PAINT);
			plot.setShadowPaint(PiePlot.DEFAULT_SHADOW_PAINT);
		} else {
			plot.setBaseSectionOutlinePaint(c);
			plot.setLabelLinkPaint(c);
			plot.setLabelOutlinePaint(c);
			Color newC = c.darker();
			Color faded = new Color(newC.getRed(), newC.getGreen(), newC
					.getBlue(), 128);

			plot.setLabelShadowPaint(faded);
			plot.setShadowPaint(c);
		}
	}

	@Override
	public void setSourceColor(Color c) {
		super.setSourceColor(c);
		if (getChart() == null)
			return;
		PiePlot plot = ((PiePlot) getChart().getPlot());
		if (c == null) {
			plot.setLabelPaint(PiePlot.DEFAULT_LABEL_PAINT);
		} else {
			plot.setLabelPaint(c);
		}
	}

	@Override
	public void setSourceThickness(float newThickness, boolean setConnected) {
		super.setSourceThickness(newThickness, setConnected);
		if (getChart() == null)
			return;
		PiePlot plot = ((PiePlot) getChart().getPlot());
		plot.setBaseSectionOutlineStroke(new BasicStroke(newThickness));

		plot.setLabelLinkStroke(new BasicStroke(newThickness));
		plot.setLabelOutlineStroke(new BasicStroke(newThickness));

		plot.setLabelFont(plot.getLabelFont().deriveFont(
				getFontSize(newThickness, PiePlot.DEFAULT_LABEL_FONT
						.getSize2D())));
		plot.setLabelPadding(new RectangleInsets(newThickness + 1,
				newThickness + 1, newThickness + 1, newThickness + 1));
		plot.setLegendItemShape(Plot.getCircle((newThickness + 1) * 2));
	}
}
