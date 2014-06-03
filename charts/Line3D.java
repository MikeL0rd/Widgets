package org.expeditee.items.widgets.charts;

import org.expeditee.items.Text;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;

public class Line3D extends AbstractCategory {

	public Line3D(Text source, String[] args) {
		super(source, args);
	}

	@Override
	protected JFreeChart createNewChart() {
		return ChartFactory.createLineChart3D(DEFAULT_TITLE, DEFAULT_XAXIS, DEFAULT_YAXIS,
				getChartData(), PlotOrientation.VERTICAL, true, // legend?
				true, // tooltips?
				false // URLs?
				);
	}
}
