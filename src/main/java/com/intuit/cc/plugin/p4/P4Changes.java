/*
 * ©2010 Intuit Inc. All rights reserved.
 * Unauthorized reproduction is a violation of applicable law
 */
package com.intuit.cc.plugin.p4;

import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * P4 changes command
 */
public class P4Changes extends P4Command {   
    /**
     * The project base directory
     */
    protected final File baseDir;

    // the extracted changelist number
	private String changelist;
    // the extracted changelist time
	private String changeTime;

	/**
	 * Determine the most recent changelist for a given folder 
	 * @param baseDir The base folder
	 * @param p4client optional P4CLIENT value
	 * @param p4port optional P4PORT value
	 */
	public P4Changes(File baseDir, String p4client, String p4port) {
		super(p4client, p4port);
		this.baseDir= baseDir;
	}
	
    public String getChangelist() {
		return changelist;
	}

	public String getChangeTime() {
		return changeTime;
	}
    
	// e.g. Change 203692 on 2010/04/28 by brco3837@DI9873 'updating error page '
    private static final Pattern changesPattern= Pattern.compile("Change\\s(\\d+)\\son\\s(\\S+)\\sby .*");
    
    @Override
    protected boolean isLineMatched(String line) {
		Matcher matcher= changesPattern.matcher(line);
		if(!matcher.matches()) {
			return false;
		}
		changelist= matcher.group(1);
		changeTime= matcher.group(2);
		return true; 
	}

	@Override
    protected void addCommandSpecificArgs(List<String> args) {
		args.add("changes");
		args.add("-m");
		args.add("1");
		args.add("-s");
		args.add("submitted");
		args.add(baseDir.getPath()+"...");
	}
}
