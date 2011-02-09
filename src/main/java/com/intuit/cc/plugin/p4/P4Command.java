/*
 * ©2010 Intuit Inc. All rights reserved.
 * Unauthorized reproduction is a violation of applicable law
 */
package com.intuit.cc.plugin.p4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * base P4 command
 */
public abstract class P4Command
{
    /**
     * The perforce client name (P4CLIENT)
     */
    private final String p4client;
    
    /**
     * The perforce port value (P4PORT)
     */
    private final String p4port;
    
    /**
     * Build a P4 command 
     * @param p4client The optional P4CLIENT
     * @param p4port The option P4PORT
     */
	public P4Command(String p4client, String p4port) {
		this.p4client = p4client;
		this.p4port = p4port;
	}

	/**
	 * Invoke the P4 command 
	 * @return null, if no error; otherwise, the error message
	 * @throws IOException
	 */
	public String invoke() throws IOException {
		// start with standard arguments
		List<String> args= new ArrayList<String>();
		args.add("p4");
		if(p4client!=null && p4client.length()>0) {
			args.add("-c");
			args.add(p4client);
		}
		if(p4port!=null && p4port.length()>0) {
			args.add("-p");
			args.add(p4port);
		}
		// add command specific arguments
		addCommandSpecificArgs(args);

		ProcessBuilder pb = new ProcessBuilder(args);
		pb.redirectErrorStream(true);
		
		Process process= pb.start();
		
		BufferedReader br= new BufferedReader(new InputStreamReader(process.getInputStream(), Charset.forName("ISO-8859-1")));
		boolean matched= false;
		StringBuilder output= new StringBuilder("P4: ");
		for(;;){
			// read from the console output
			String line= br.readLine();
			if(line==null) {
				break;
			}
			output.append(line);
			// if previous lines did not match expected output,
			// check if this line matches
			if(!matched) {
				matched= isLineMatched(line);
			}
		}
		
		br.close();
		process.getOutputStream().close();
		
		for(;;) {
			try {
				process.waitFor();
				break;
			} catch (InterruptedException e) {
			}
		}
		// if error code, return collected output as an error message
		return process.exitValue()!=0 ?output.toString() :null;
    }
	
	/**
	 * Add the command specific arguments
	 * @param args The list to which addition command arguments are added
	 */
	protected abstract void addCommandSpecificArgs(List<String> args);
	
	/**
	 * Check if the line matches the expected command output
	 * @param line A console output line from the P4 command 
	 * @return true if, any only if, the line matches the expected output
	 */
	protected abstract boolean isLineMatched(String line);
}
