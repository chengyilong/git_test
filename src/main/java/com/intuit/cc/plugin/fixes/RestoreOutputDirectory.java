/*
 * ©2010 Intuit Inc. All rights reserved.
 * Unauthorized reproduction is a violation of applicable law
 */
package com.intuit.cc.plugin.fixes;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;

/**
 * Prevent the codehaus mojo from permanently messing up the build folder 
 * (otherwise, the jars are made of the instrumented classes) 
 * @phase generate-test-sources
 * @goal restore-output-directory
 */
public class RestoreOutputDirectory extends AbstractMojo {
    /**
     * @parameter expression="${project.build}"
     * @required
     * @readonly
     */
    protected Build build;
    
    // restore the output folder
    @Override
    public void execute() throws MojoExecutionException {
        String outputFolder= (String) getPluginContext().get(SaveOutputDirectory.class);
        build.setOutputDirectory( outputFolder );
        System.getProperties().remove("project.build.outputDirectory");
    }
}
