package org.expeditee.items.widgets.charts;

import org.expeditee.items.Text;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;

public class Ring extends AbstractPie {

	public Ring(Text source, String[] args) {
		super(source, args);
	}

	@Override
	protected JFreeChart createNewChart() {
		return ChartFactory.createRingChart(DEFAULT_TITLE, getChartData(), true, // legend?
				true, // tooltips?
				false // URLs?
				);
	}
}
