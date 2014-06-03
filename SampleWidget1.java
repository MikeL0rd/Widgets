package org.expeditee.items.widgets;

import javax.swing.JComboBox;

import org.expeditee.items.Text;

public class SampleWidget1 extends DataFrameWidget {

	static String testItems[] = new String[] { "dog", "fish", "cat", "pig" };

	private JComboBox _combo;

	public SampleWidget1(Text source, String[] args) {
		super(source, new JComboBox(testItems), 200, 200, 50, 50);
		_combo = (JComboBox) super._swingComponent;
	}

	@Override
	protected String[] getArgs() {
		String[] stateArgs = new String[1];
		stateArgs[0] = Integer.toString(_combo.getSelectedIndex());
		return stateArgs;
	}
	
	/** TODO: REVISE
	@Override
	public void refresh() {
		super.refresh();
		Frame frame = getDataFrame();
		if(frame != null) {
			_combo.removeAllItems();
			for(Text text: frame.getBodyTextItems(false)) {
				_combo.addItem(text.getText());
			}
			_combo.setSelectedIndex(0);
		}
	}
	*/

}
