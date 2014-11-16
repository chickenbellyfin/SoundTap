package com.example.vtest;

public class ContentItem {
	
	private String name;
	private String xml;
	private String logo;

	public ContentItem(String name, String xml, String logo){
		this.name = name;
		this.xml = xml;
		this.logo = logo;
	}

	
	public String getName() {
		return name;
	}


	public void setName(String name) {
		this.name = name;
	}


	public String getXml() {
		return xml;
	}

	public void setXml(String xml) {
		this.xml = xml;
	}

	public String getLogo() {
		return logo;
	}

	public void setLogo(String logo) {
		this.logo = logo;
	}
	
	
	
}
