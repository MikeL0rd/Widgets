package org.expeditee.items.widgets;

import javax.swing.JComboBox;

import org.expeditee.gui.Frame;
import org.expeditee.items.Text;

public class DataFrameWidget1 extends DataFrameWidget {

	static String testItems[] = new String[] { "dog", "fish", "cat", "pig" };

	private JComboBox _combo;

	public DataFrameWidget1(Text source, String[] args) {
		super(source, new JComboBox(testItems), 200, 200, 50, 50);
		_combo = (JComboBox) super._swingComponent;

		refresh();
	}

	@Override
	protected String[] getArgs() {
		String[] stateArgs = new String[1];
		stateArgs[0] = Integer.toString(_combo.getSelectedIndex());
		return stateArgs;
	}

	@Override
	public void refresh() {
		super.refresh();
		Frame frame = getDataFrame();
		if (frame != null) {
			_combo.removeAllItems();
			for (Text text : frame.getBodyTextItems(false)) {
				_combo.addItem(text.getText());
			}
			if (_combo.getItemCount() > 0)
				_combo.setSelectedIndex(0);
		}
	}

	@Override
	public float getMinimumBorderThickness() {
		return 0.0F;
	}
}
