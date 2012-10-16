/*
 * ©2010 Intuit Inc. All rights reserved.
 * Unauthorized reproduction is a violation of applicable law
 */
package com.intuit.cc.plugin.p4;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * P4 info command
 */
public class P4Info extends P4Command {   
    
	// the extracted server name
	private String server;

	/**
	 * Obtain the P4 server information
	 * @param p4client optional P4CLIENT value
	 * @param p4port optional P4PORT value
	 */
	public P4Info(String p4client, String p4port) {
		super(p4client, p4port);
	}
	
    public String getServer() {
		return server;
	}
    
	// e.g. Server address: p4:1790
    private static final Pattern changesPattern= Pattern.compile("Server\\saddress:\\s(.+)\\s*");
    
    @Override
    protected boolean isLineMatched(String line) {
		Matcher matcher= changesPattern.matcher(line);
		if(!matcher.matches()) {
			return false;
		}
		server= matcher.group(1);
		return true; 
	}

	@Override
    protected void addCommandSpecificArgs(List<String> args) {
		args.add("info");
	}
}
