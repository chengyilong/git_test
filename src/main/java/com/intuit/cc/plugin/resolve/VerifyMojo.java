/*
 * ©2010 Intuit Inc. All rights reserved.
 * Unauthorized reproduction is a violation of applicable law
 */
package com.intuit.cc.plugin.resolve;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.interpolation.EnvarBasedValueSource;
import org.codehaus.plexus.interpolation.InterpolationException;
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource;
import org.codehaus.plexus.interpolation.RecursionInterceptor;
import org.codehaus.plexus.interpolation.RegexBasedInterpolator;
import org.codehaus.plexus.interpolation.SimpleRecursionInterceptor;


/**
 * Fixes the Artifact to use updated pom with all versions resolved  
 * @goal fix-pom
 * @phase verify
 */
public class VerifyMojo extends AbstractMojo
{
    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;
    
    /**
     * The current build session instance.
     *
     * @parameter expression="${session}"
     * @required
     * @readonly
     */
    private MavenSession session;
    
	private File resolvedPom;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		File target= new File(project.getBasedir(), "target");
		if(!target.exists()) {
			target.mkdir();
		}
		resolvedPom= new File(target, "resolved.pom");    	
    	
		PomResolver resolver= new PomResolver();
		resolver.resolvePom();
        project.setFile(resolvedPom);
    }
	
	class PomResolver {
		private final RegexBasedInterpolator interpolator;
	    private final RecursionInterceptor recursionInterceptor;

	    public PomResolver() throws MojoExecutionException  {
	        interpolator = new RegexBasedInterpolator();
	        try {
                interpolator.addValueSource( new EnvarBasedValueSource() );
            } catch (IOException e) {
                throw new MojoExecutionException(e.getMessage(), e);
            }
	        interpolator.addValueSource( new PropertiesBasedValueSource( System.getProperties() ) );
			interpolator.addValueSource( new PropertiesBasedValueSource( project.getProperties() ) );
			interpolator.addValueSource( new PropertiesBasedValueSource( session.getExecutionProperties() ) );
			recursionInterceptor = new SimpleRecursionInterceptor();
	    }
	    
		public void resolvePom() throws MojoExecutionException {
			try {
				filter(project.getFile(), resolvedPom);
			} catch (IOException e) {
				throw new MojoExecutionException(e.getMessage(), e);
			}
	    }
		
		private void filter(File src, File dest) throws IOException {
			InputStream fis= null;
			InputStreamReader isr= null;
			BufferedReader br= null;
			try {
				fis= new FileInputStream(src);
				isr= new InputStreamReader(fis, Charset.forName("UTF-8"));
				br= new BufferedReader(isr);
				filter(br, dest);
			}
			finally {
				try {
					if(br!=null) {
						br.close();
					}
					else if(isr!=null) {
						isr.close();
					}
					else if(fis!=null) {
						fis.close();
					}
				} catch (IOException e) {
					getLog().error("could not close "+src, e);
				}
			}
		}
		
		private void filter(BufferedReader src, File dest) throws IOException {
			OutputStream fos= null;
			BufferedWriter bw= null;
			OutputStreamWriter osw= null;
			try {
				fos= new FileOutputStream(dest);
				osw= new OutputStreamWriter(fos, Charset.forName("UTF-8"));
				bw= new BufferedWriter(osw);
				filter(src, bw);
			}
			finally {
				try {
					if(bw!=null) {
						bw.close();
					}
					else if(osw!=null) {
						osw.close();
					} 
					else if(fos!=null) {
						fos.close();
					}
				} catch (IOException e) {
					getLog().error("could not close "+dest, e);
				}
			}
		}

		private void filter(BufferedReader src, BufferedWriter dest) throws IOException {
			for(;;) {
				String is= src.readLine();
				if(is==null) {
					break;
				}
				dest.write(interpolate(is));
				dest.newLine();
			}
		}
		
		public String interpolate(String value) throws IOException {
			try {
				return interpolator.interpolate( value, recursionInterceptor );
			} catch (InterpolationException e) {
				throw new IOException(e.getMessage(), e); 
			}
		}
	}
}
