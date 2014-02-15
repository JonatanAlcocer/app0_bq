package com.example.appdropboxbq;

import java.util.Date;

public class Book{
	private String name;
	private Date modified;
	
	public Book(String p_name, Date p_modified){
		name = p_name;
		modified = p_modified;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getModified() {
		return modified;
	}

	public void setModified(Date modified) {
		this.modified = modified;
	}
	
}
