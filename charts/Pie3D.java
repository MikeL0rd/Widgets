package org.expeditee.items.widgets.charts;

import org.expeditee.items.Text;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.util.Rotation;

public class Pie3D extends AbstractPie {

	public Pie3D(Text source, String[] args) {
		super(source, args);
	}

	@Override
	protected JFreeChart createNewChart() {
		JFreeChart chart = ChartFactory.createPieChart3D(DEFAULT_TITLE, getChartData(), true, // legend?
				true, // tooltips?
				false // URLs?
				);
		final PiePlot3D plot = (PiePlot3D) chart.getPlot();
        plot.setStartAngle(290);
        plot.setDirection(Rotation.CLOCKWISE);
        plot.setForegroundAlpha(0.5f);
        return chart;
	}
}
