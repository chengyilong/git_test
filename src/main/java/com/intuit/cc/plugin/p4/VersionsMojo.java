/*
 * ©2010 Intuit Inc. All rights reserved.
 * Unauthorized reproduction is a violation of applicable law
 */
package com.intuit.cc.plugin.p4;

import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

/**
 * Get the p4 version for this project
 * @goal p4-version
 * @phase validate
 */
public class VersionsMojo extends AbstractMojo
{    
    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;

    /**
     * The perforce client name (P4CLIENT)
     *
     * @parameter expression="${p4.client}"
     */
    private String p4client;
    
    /**
     * The perforce port value (P4PORT)
     *
     * @parameter expression="${p4.port}"
     */
    private String p4port;
    
    /**
     * skip getting the version
     *
     * @parameter expression="${p4.skip}" default-value="false"
     */
    private boolean p4skip;
    
	@Override
	public void execute() throws MojoExecutionException {
		if(p4skip) {
			project.getProperties().setProperty("p4.changelist", "unknown");
			return;
		}
		try {
			P4Changes changes= new P4Changes(project.getBasedir(), p4client, p4port);
			String error= changes.invoke();
			if(error!=null) {
				throw new MojoExecutionException(error);
			}
			
			P4Info info= new P4Info(p4client, p4port);
			error= info.invoke();
			if(error!=null) {
				throw new MojoExecutionException(error);
			}
			
			String changelist= changes.getChangelist()+'@'+info.getServer();
			project.getProperties().setProperty("p4.changelist", changelist);
			getLog().info(getCoordinate()+" has changelist "+changelist);
		}
		catch(IOException ex) {
			throw new MojoExecutionException(ex.getMessage(), ex);
		}
    }
	/**
	 * Get a maven coordinate for an Artifact, Dependency, or Project
	 * @param groupId The group name
	 * @param artifactId The artifact name
	 * @param type The artifact type
	 * @return The maven coordinate
	 */
	private static String getCoordinate(String groupId, String artifactId, String type) {
		return groupId+':'+artifactId+':'+type;
	}

	/**
	 * Get a maven coordinate for the project
	 * @return The maven coordinate
	 */
	private String getCoordinate() {
		return getCoordinate(project.getGroupId(), project.getArtifactId(), project.getPackaging());
	}}
