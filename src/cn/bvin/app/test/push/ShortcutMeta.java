package cn.bvin.app.test.push;

public class ShortcutMeta {

public String name;
	
	public String openUrl;
	
	public String iconUrl;
	
	public boolean sendAsNotification;

	public ShortcutMeta(String name, String openUrl, String iconUrl, boolean sendAsNotification) {
		super();
		this.name = name;
		this.openUrl = openUrl;
		this.iconUrl = iconUrl;
		this.sendAsNotification = sendAsNotification;
	}
}
