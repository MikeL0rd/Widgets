package org.expeditee.items.widgets.charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;

import org.expeditee.gui.Frame;
import org.expeditee.items.Text;
import org.jfree.chart.axis.Axis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueAxisPlot2;

public abstract class AbstractValueAxis extends AbstractChart {

	public AbstractValueAxis(Text source, String[] args) {
		super(source, args);
	}

	/**
	 * @param dataFrame
	 */
	@Override
	protected void refreshPlot(Frame dataFrame, Plot simplePlot) {
		ValueAxisPlot2 plot = (ValueAxisPlot2) simplePlot;
		if (dataFrame.hasAnnotation("xaxis")) {
			plot.getDomainAxis()
					.setLabel(dataFrame.getAnnotationValue("xaxis"));
		}
		if (dataFrame.hasAnnotation("yaxis")) {
			plot.getRangeAxis().setLabel(dataFrame.getAnnotationValue("yaxis"));
		}
		if (dataFrame.hasAnnotation("horizontal")) {
			plot.setOrientation(PlotOrientation.HORIZONTAL);
		} else {
			plot.setOrientation(PlotOrientation.VERTICAL);
		}
	}

	@Override
	public void setSourceColor(Color c) {
		super.setSourceColor(c);
		if (getChart() == null)
			return;

		ValueAxisPlot2 plot = ((ValueAxisPlot2) getChart().getPlot());
		if (c == null) {
			plot.getDomainAxis().setLabelPaint(Axis.DEFAULT_AXIS_LABEL_PAINT);
			plot.getRangeAxis().setLabelPaint(Axis.DEFAULT_AXIS_LABEL_PAINT);
			plot.getDomainAxis().setTickLabelPaint(
					Axis.DEFAULT_TICK_LABEL_PAINT);
			plot.getRangeAxis()
					.setTickLabelPaint(Axis.DEFAULT_TICK_LABEL_PAINT);
		} else {
			plot.getDomainAxis().setLabelPaint(c);
			plot.getRangeAxis().setLabelPaint(c);
			plot.getDomainAxis().setTickLabelPaint(c);
			plot.getRangeAxis().setTickLabelPaint(c);
		}
	}

	@Override
	public void setSourceBorderColor(Color c) {
		super.setSourceBorderColor(c);
		if (getChart() == null)
			return;
		ValueAxisPlot2 plot = ((ValueAxisPlot2) getChart().getPlot());
		if (c == null) {
			plot.setDomainGridlinePaint(Plot.DEFAULT_OUTLINE_PAINT);
			plot.setRangeGridlinePaint(Plot.DEFAULT_OUTLINE_PAINT);
			plot.getDomainAxis().setTickMarkPaint(Plot.DEFAULT_OUTLINE_PAINT);
			plot.getRangeAxis().setTickMarkPaint(Plot.DEFAULT_OUTLINE_PAINT);
		} else {
			Color brighter = c.brighter();
			plot.setDomainGridlinePaint(brighter);
			plot.setRangeGridlinePaint(brighter);
			plot.getDomainAxis().setTickMarkPaint(c);
			plot.getRangeAxis().setTickMarkPaint(c);
		}
	}

	@Override
	public void setSourceThickness(float newThickness, boolean setConnected) {
		super.setSourceThickness(newThickness, setConnected);
		if (getChart() == null)
			return;
		ValueAxisPlot2 plot = ((ValueAxisPlot2) getChart().getPlot());
		Stroke solid = new BasicStroke(newThickness);
		Stroke gridline = new BasicStroke(newThickness / 2,
				BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0.0f,
				new float[] { 2 * newThickness, 2 * newThickness }, 0.0f);
		
		updateAxis(plot.getDomainAxis(), solid, newThickness);
		updateAxis(plot.getRangeAxis(), solid, newThickness);

		plot.setDomainGridlineStroke(gridline);
		plot.setRangeGridlineStroke(gridline);
		plot.getRenderer().setBaseStroke(solid);

	}

	private void updateAxis(Axis axis, Stroke solid, float thickness) {
		axis.setAxisLineStroke(solid);
		axis.setTickMarkStroke(solid);
		axis.setLabelFont(axis.getLabelFont().deriveFont(getFontSize(thickness, Axis.DEFAULT_AXIS_LABEL_FONT.getSize2D())));
		axis.setTickLabelFont(axis.getTickLabelFont().deriveFont(getFontSize(thickness, Axis.DEFAULT_TICK_LABEL_FONT.getSize2D())));
	}
}
