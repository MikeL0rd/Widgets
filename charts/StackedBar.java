package org.expeditee.items.widgets.charts;

import org.expeditee.items.Text;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;

public class StackedBar extends AbstractCategory {

	public StackedBar(Text source, String[] args) {
		super(source, args);
	}

	@Override
	protected JFreeChart createNewChart() {
		return ChartFactory.createStackedBarChart(DEFAULT_TITLE, DEFAULT_XAXIS, DEFAULT_YAXIS,
				getChartData(), PlotOrientation.VERTICAL, true, // legend?
				true, // tooltips?
				false // URLs?
				);
	}
}
