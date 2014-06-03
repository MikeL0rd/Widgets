package org.expeditee.items.widgets;

import java.awt.GridLayout;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import org.expeditee.items.Text;

/**
 * Allows entering password data in non-visible form
 * TODO: encrypt this before storing it
 * @author jts21
 */
public class Password extends InteractiveWidget {

	private JPasswordField passwordField;
	private JCheckBox savePassword;
	
	public Password(Text source, String[] args) {
		super(source, new JPanel(new GridLayout(0, 1)), 200, 200, 60, 60);
		JPanel p = (JPanel) super._swingComponent;
		this.passwordField = new JPasswordField();
		this.savePassword = new JCheckBox("Save password?");
		p.add(passwordField);
		p.add(savePassword);
		if(args != null && args.length > 0) {
			String[] password = args[0].split(":");
			StringBuilder s = new StringBuilder();
			for(String str : password) {
				char c = (char) Byte.parseByte(str);
				s.append(c);
			}
			this.passwordField.setText(s.toString());
			this.savePassword.setSelected(true);
		}
	}

	@Override
	protected String[] getArgs() {
		if(!savePassword.isSelected()) {
			return null;
		}
		char[] password = this.passwordField.getPassword();
		StringBuilder s = new StringBuilder();
		for(char b : password) {
			s.append((byte)b + ":");
		}
		return new String[] { s.toString() };
	}
	
	public String getPassword() {
		return new String(this.passwordField.getPassword());
	}
	
	public void setPassword(String password) {
		this.passwordField.setText(password);
	}

}
