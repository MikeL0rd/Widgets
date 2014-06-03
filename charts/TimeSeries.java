package org.expeditee.items.widgets.charts;

import java.awt.Color;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;

import org.expeditee.gui.AttributeValuePair;
import org.expeditee.gui.Frame;
import org.expeditee.gui.MessageBay;
import org.expeditee.items.Text;
import org.expeditee.stats.Formatter;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Day;
import org.jfree.data.time.Hour;
import org.jfree.data.time.Millisecond;
import org.jfree.data.time.Minute;
import org.jfree.data.time.Month;
import org.jfree.data.time.Quarter;
import org.jfree.data.time.RegularTimePeriod;
import org.jfree.data.time.Second;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.time.Week;
import org.jfree.data.time.Year;
import org.jfree.data.xy.XYDataset;

public class TimeSeries extends AbstractValueAxis {

	public TimeSeries(Text source, String[] args) {
		super(source, args);
	}

	private TimeSeriesCollection _data;

	private Class<? extends RegularTimePeriod> _periodType;

	private Date _startDate;

	protected XYDataset getChartData() {
		if (_data == null)
			_data = new TimeSeriesCollection();
		return _data;
	}

	@Override
	public void refreshData(Frame dataFrame) {
		String period = dataFrame.getAnnotationValue("period");
		if (period == null) {
			period = "day";
		} else {
			period = period.toLowerCase();
		}
		if (period.startsWith("quarter")) {
			_periodType = Quarter.class;
		} else if (period.startsWith("year")) {
			_periodType = Year.class;
		} else if (period.startsWith("month")) {
			_periodType = Month.class;
		} else if (period.startsWith("week")) {
			_periodType = Week.class;
		} else if (period.startsWith("hour")) {
			_periodType = Hour.class;
		} else if (period.startsWith("min")) {
			_periodType = Minute.class;
		} else if (period.startsWith("sec")) {
			_periodType = Second.class;
		} else if (period.startsWith("milli")) {
			_periodType = Millisecond.class;
		} else if (period.startsWith("day")) {
			_periodType = Day.class;
		} else {
			MessageBay.errorMessage("Invalid time series period type: "
					+ period);
			_periodType = Day.class;
		}

		try {
			String startDateString = dataFrame.getAnnotationValue("start");
			if (startDateString != null) {
				_startDate = parseDate(startDateString);
			}
		} catch (Exception e) {
			// Use the current date
		}
		if (_startDate == null) {
			_startDate = new Date();
		}
		super.refreshData(dataFrame);
	}

	/**
	 * @param dataFrame
	 */
	@Override
	protected void clearData() {
		_data.removeAllSeries();
	}

	@Override
	protected JFreeChart createNewChart() {
		return ChartFactory.createTimeSeriesChart(DEFAULT_TITLE, DEFAULT_XAXIS,
				DEFAULT_YAXIS, getChartData(), true, // legend?
				true, // tooltips?
				false // URLs?
				);
	}

	@Override
	protected boolean addCategoryData(String categoryName,
			Collection<Text> items, boolean swap) {
		org.jfree.data.time.TimeSeries newSeries = new org.jfree.data.time.TimeSeries(
				categoryName, _periodType);

		boolean foundData = false;
		Color newColor = null;
		for (Text i : items) {
			if (!i.isLineEnd()) {
				Text t = i;
				AttributeValuePair avp = new AttributeValuePair(t.getText());
				if (avp != null) {
					Double attribute = null;
					try {
						attribute = avp.getDoubleAttribute();
					} catch (NumberFormatException e) {
					}
					Double value = null;
					try {
						// If the data is not valid move to the next item
						value = avp.getDoubleValue();
					} catch (NumberFormatException e) {
						continue;
					}

					try {
						RegularTimePeriod rtp = null;
						if (attribute == null) {
							Date date = parseDate(avp.getAttribute());
							rtp = _periodType.getConstructor(
									new Class[] { Date.class }).newInstance(
									new Object[] { date });
						} else {
							if (_periodType.equals(Year.class)) {
								int year = (int) Math.floor(attribute);
								Calendar c = Calendar.getInstance();
								c.setTime(_startDate);
								rtp = new Year(year + c.get(Calendar.YEAR));
							} else if (_periodType.equals(Quarter.class)) {
								int quarter = (int) Math.floor(attribute);
								rtp = new Quarter(quarter, new Year(_startDate));
							} else if (_periodType.equals(Month.class)) {
								int month = (int) Math.floor(attribute);
								rtp = new Month(month, new Year(_startDate));
							} else if (_periodType.equals(Week.class)) {
								int week = (int) Math.floor(attribute);
								rtp = new Week(week, new Year(_startDate));
							} else if (_periodType.equals(Day.class)) {
								int day = (int) Math.floor(attribute);
								Calendar c = Calendar.getInstance();
								c.setTime(_startDate);
								rtp = new Day(day
										+ c.get(Calendar.DAY_OF_MONTH), 1 + c
										.get(Calendar.MONTH), c
										.get(Calendar.YEAR));
							} else if (_periodType.equals(Hour.class)) {
								int hour = (int) Math.floor(attribute);
								rtp = new Hour(hour, new Day(_startDate));
							} else if (_periodType.equals(Minute.class)) {
								int minute = (int) Math.floor(attribute);
								rtp = new Minute(minute, new Hour(_startDate));
							} else if (_periodType.equals(Second.class)) {
								int second = (int) Math.floor(attribute);
								rtp = new Second(second, new Minute(_startDate));
							} else if (_periodType.equals(Millisecond.class)) {
								int milli = (int) Math.floor(attribute);
								rtp = new Millisecond(milli, new Second(
										_startDate));
							}
						}
						newSeries.add(rtp, value);
						foundData = true;
						if (newColor == null)
							newColor = i.getColor();
					} catch (Exception e) {
						// Ignore the data point if it cant be parsed
						e.printStackTrace();
					}
				}
			}
		}
		if (foundData) {
			_data.addSeries(newSeries);
			_paints.put(categoryName, newColor);
		}
		return foundData;
	}

	/**
	 * @param avp
	 * @return
	 * @throws ParseException
	 */
	public static Date parseDate(String dateString) throws ParseException {
		// Select the best match for a date or time format
		DateFormat df = null;
		if (dateString.length() > Formatter.DATE_FORMAT
				.length()) {
			df = new SimpleDateFormat(Formatter.DATE_TIME_FORMAT);
		} else if (dateString.length() <= Formatter.TIME_FORMAT.length()) {
			df = new SimpleDateFormat(Formatter.TIME_FORMAT);
		}else {
			df = new SimpleDateFormat(Formatter.DATE_FORMAT);
		}
		Date date = df.parse(dateString);
		return date;
	}
}
