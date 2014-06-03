package org.expeditee.items.widgets.charts;

import java.awt.Color;
import java.util.Collection;
import java.util.HashSet;

import org.expeditee.gui.AttributeValuePair;
import org.expeditee.items.Item;
import org.expeditee.items.Text;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

public abstract class AbstractXY extends AbstractValueAxis {

	private XYSeriesCollection _data;

	protected XYDataset getChartData() {
		_data = new XYSeriesCollection();
		return _data;
	}

	public AbstractXY(Text source, String[] args) {
		super(source, args);
	}

	/**
	 * @param dataFrame
	 */
	@Override
	protected void clearData() {
		_data.removeAllSeries();
	}

	@Override
	protected boolean addCategoryData(String categoryName,
			Collection<Text> items, boolean swap) {
		XYSeries newSeries = new XYSeries(categoryName);
		Color categoryColor = null;
		boolean foundData = false;
		Collection<Item> seen = new HashSet<Item>();
		for (Text i : items) {
			if (i.isLineEnd() || seen.contains(i)) {
				continue;
			}
			
			Text t = i;
			AttributeValuePair avp = new AttributeValuePair(t.getText());
			if (avp != null) {
				Double attribute = null;
				Double value = null;
				try {
					value = avp.getDoubleValue();
					attribute = avp.getDoubleAttribute();
				} catch (Exception e) {

				}
				// If the data is not valid move to the next item
				if (attribute == null && value != null) {
					String stringAttribute = avp.getAttribute();
					if (stringAttribute == null)
						continue;
					// Check if there is another item within the same
					// enclosure
					Collection<Item> sameEnclosure = t
							.getItemsInSameEnclosure();
					seen.addAll(sameEnclosure);
					for (Item enclosed : sameEnclosure) {
						if (enclosed instanceof Text && enclosed != t) {
							AttributeValuePair avp2 = new AttributeValuePair(
									enclosed.getText());
							String stringAtt2 = avp2.getAttribute();
							Double value2 = null;
							try {
								value2 = avp2.getDoubleValue();
							} catch (Exception e) {
							}
							if (stringAtt2 != null && value2 != null) {
								if (String.CASE_INSENSITIVE_ORDER.compare(
										stringAttribute, stringAtt2) < 0) {
									attribute = value;
									value = value2;
								} else {
									attribute = value2;
								}
								break;
							}
						}
					}
				}

				if (value == null)
					continue;
				if (swap) {
					newSeries.add(value, attribute);
				} else {
					newSeries.add(attribute, value);
				}
				foundData = true;
				if (categoryColor == null) {
					categoryColor = i.getColor();
				}
			}
		}
		if (foundData) {
			_data.addSeries(newSeries);
			_paints.put(categoryName, categoryColor);
		}
		return foundData;
	}
}
