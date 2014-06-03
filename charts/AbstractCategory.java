package org.expeditee.items.widgets.charts;

import java.awt.Color;
import java.util.Collection;

import org.expeditee.gui.AttributeValuePair;
import org.expeditee.items.Text;
import org.jfree.data.category.DefaultCategoryDataset;

public abstract class AbstractCategory extends AbstractValueAxis {

	private DefaultCategoryDataset _data;

	protected DefaultCategoryDataset getChartData() {
		if (_data == null)
			_data = new DefaultCategoryDataset();
		return _data;
	}

	public AbstractCategory(Text source, String[] args) {
		super(source, args);
	}

	@Override
	protected void clearData() {
		_data.clear();
	}

	@Override
	protected boolean addCategoryData(String categoryName,
			Collection<Text> items, boolean swap) {
		boolean foundData = false;
		Color newColor = null;
		for (Text i : items) {
			try {
				if (!i.isLineEnd()) {
					Text t = i;
					AttributeValuePair avp = new AttributeValuePair(t.getText());
					if (avp != null) {
						if (swap) {
							String attribute = avp.getAttribute();
							_data.setValue(avp.getDoubleValue(), attribute,
									categoryName);
							if (_paints.get(attribute) == null) {
								_paints.put(attribute, i.getBackgroundColor());
							}
						} else {
							_data.setValue(avp.getDoubleValue(), categoryName,
									avp.getAttribute());
						}
						foundData = true;
						if (newColor == null)
							newColor = i.getBackgroundColor();
					}
				}
			} catch (Exception e) {

			}
		}
		if (foundData && !swap) {
			_paints.put(categoryName, newColor);
		}
		return foundData;
	}
}
