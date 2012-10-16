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
 * @goal save-output-directory
 * @phase compile
 */
public class SaveOutputDirectory extends AbstractMojo {
    /**
     * @parameter expression="${project.build}"
     * @required
     * @readonly
     */
    protected Build build;
    
    // save the output folder
    @SuppressWarnings("unchecked")
    @Override
    public void execute() throws MojoExecutionException {
        String outputFolder= build.getOutputDirectory();
        getPluginContext().put(SaveOutputDirectory.class, outputFolder);
    }
}
