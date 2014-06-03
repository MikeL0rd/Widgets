package org.expeditee.items.widgets.charts;

import java.awt.Color;

import org.expeditee.items.Text;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.plot.PolarPlot;

public class Polar extends AbstractXY {

	public Polar(Text source, String[] args) {
		super(source, args);
	}

	@Override
	protected JFreeChart createNewChart() {
		return ChartFactory.createPolarChart(DEFAULT_TITLE, getChartData(),
				true, // legend?
				true, // tooltips?
				false // URLs?
				);
	}

	@Override
	public void setSourceColor(Color c) {
		super.setSourceColor(c);
		if (getChart() == null)
			return;

		PolarPlot plot = ((PolarPlot) getChart().getPlot());

		if (c == null) {
			plot.setAngleLabelPaint(Axis.DEFAULT_AXIS_LABEL_PAINT);
		} else {
			plot.setAngleLabelPaint(c);
		}
	}
	
	@Override
	public void setSourceThickness(float newThickness, boolean setConnected) {
		super.setSourceThickness(newThickness, setConnected);
		if (getChart() == null)
			return;
		PolarPlot plot = ((PolarPlot) getChart().getPlot());
		plot.setAngleLabelFont(plot.getAngleLabelFont().deriveFont(getFontSize(newThickness, Axis.DEFAULT_TICK_LABEL_FONT.getSize2D())));
	}
}
