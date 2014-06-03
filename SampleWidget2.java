package org.expeditee.items.widgets;


import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;

import org.expeditee.items.Text;

public class SampleWidget2 extends InteractiveWidget {
	
	private JComboBox _combo;
	private JTextArea _text;
	
	public SampleWidget2(Text source, String[] args) {
		super(source, new JSplitPane(JSplitPane.VERTICAL_SPLIT), 60, -1, 40, -1);

		Font bigFont = null;
		
		JSplitPane sp = (JSplitPane)super._swingComponent;
		
		JPanel p = new JPanel(new FlowLayout());
		p.setBackground(new Color(255,228,195));

		JLabel lbl = new JLabel("This is an example InteractiveWidget!");
		bigFont = lbl.getFont().deriveFont(24F);
		lbl.setFont(bigFont);
		
		JToggleButton button = new JToggleButton("Toggle Style");
		button.setFont(bigFont);
		
		JCheckBox checkBox = new JCheckBox("Big Font");
		checkBox.setBackground(null);
		checkBox.setSelected(true);
		checkBox.setFont(bigFont);
		
		
		//JButton button2 = new JButton("Example CheckBox");
		
		_text = new JTextArea();
		_text.setFont(bigFont);
		
		_combo = new JComboBox(new String[] {"Peach", "Item 2", "Item 3", "Item 21", "Item 22", "Item 23", "Item 31", "Item 32", "Item 33", "Item 41", "Item 42", "Item 43"});
		_combo.setFont(bigFont);
		
		p.add(lbl);
		p.add(_combo);
		p.add(button);
		p.add(checkBox);
		
		sp.setTopComponent(p);
		sp.setBottomComponent(_text);

		// Set state
		if (args != null && args.length >= 1) {
			
			int selectedItem = 0;
			
			// extract selected index
			if (args.length >= 1 && args[0] != null) {
				try {
					selectedItem = Integer.parseInt(args[0]);
				} catch (NumberFormatException e) {
					e.printStackTrace();
				}
			}
			
			if (selectedItem < 0) selectedItem = 0;
			else if (selectedItem >= _combo.getItemCount())
				selectedItem = _combo.getItemCount() - 1;
			
			_combo.setSelectedIndex(selectedItem);
			
			if (args.length >= 2 && args[1] != null) {
				_text.setText(args[1]);
			}
			
			if (args.length >= 3 && args[2] != null) {
				try {
					int div = Integer.parseInt(args[2]);
					((JSplitPane)super._swingComponent).setDividerLocation(div);
				} catch (NumberFormatException e) {}
				
			}
		}		
	}

	@Override
	protected String[] getArgs() {
		
		return new String[] { 
				Integer.toString(_combo.getSelectedIndex()),
				_text.getText(),
				Integer.toString(((JSplitPane)super._swingComponent).getDividerLocation())
			};
	}
}
