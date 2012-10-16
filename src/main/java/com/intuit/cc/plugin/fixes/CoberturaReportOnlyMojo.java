/*
 * ©2010 Intuit Inc. All rights reserved.
 * Unauthorized reproduction is a violation of applicable law
 */
package com.intuit.cc.plugin.fixes;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Locale;

import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReport;
import org.apache.maven.reporting.MavenReportException;
import org.codehaus.doxia.sink.Sink;
import org.codehaus.doxia.site.renderer.SiteRenderer;
import org.codehaus.mojo.cobertura.CoberturaReportMojo;

/**
 * Generates a Cobertura Report from previously instrumented classes
 * @goal report-only
 */
public class CoberturaReportOnlyMojo extends AbstractMavenReport
{
    /**
     * The format of the report. (supports 'html' or 'xml'. defaults to 'html')
     * 
     * @parameter expression="${cobertura.report.format}"
     * @deprecated
     */
    private String format;

    /**
     * The format of the report. (can be 'html' and/or 'xml'. defaults to 'html')
     * 
     * @parameter
     */
    private String[] formats = new String[] { "html" };

    /**
     * The encoding for the java source code files.
     * 
     * @parameter expression="${project.build.sourceEncoding}" default-value="UTF-8".
     * @since 2.4
     */
    private String encoding;

    /**
     * Maximum memory to pass to JVM of Cobertura processes.
     * 
     * @parameter expression="${cobertura.maxmem}"
     */
    private String maxmem = "64m";

    /**
     * <p>
     * The Datafile Location.
     * </p>
     * 
     * @parameter expression="${cobertura.datafile}" default-value="${project.build.directory}/cobertura/cobertura.ser"
     * @required
     * @readonly
     */
    protected File dataFile;

    /**
     * <i>Maven Internal</i>: List of artifacts for the plugin.
     * 
     * @parameter expression="${plugin.artifacts}"
     * @required
     * @readonly
     */
    protected List<?> pluginClasspathList;

    /**
     * The output directory for the report.
     * 
     * @parameter default-value="${project.reporting.outputDirectory}/cobertura"
     * @required
     */
    private File outputDirectory;

    /**
     * Only output cobertura errors, avoid info messages.
     * 
     * @parameter expression="${quiet}" default-value="false"
     * @since 2.1
     */
    private boolean quiet;

    /**
     * <i>Maven Internal</i>: The Doxia Site Renderer.
     * 
     * @component
     */
    private SiteRenderer siteRenderer;

    /**
     * <i>Maven Internal</i>: Project to interact with.
     * 
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    private CoberturaReportMojo delegate;

    @Override
    public boolean canGenerateReport() {
        return getDelegate().canGenerateReport();
    }

    public CoberturaReportMojo getDelegate() {
        if(delegate==null) {
            delegate= new CoberturaReportMojo();
            delegate.setLog(getLog());
            delegate.setPluginContext(getPluginContext());
            
            setDelegate("format", format);
            setDelegate("formats", formats);
            setDelegate("encoding", encoding);
            setDelegate("maxmem", maxmem);
            setDelegate("dataFile", dataFile);
            setDelegate("pluginClasspathList", pluginClasspathList);
            setDelegate("outputDirectory", outputDirectory);
            setDelegate("quiet", quiet);
            setDelegate("siteRenderer", siteRenderer);
            setDelegate("project", project);
        }
        return delegate;
    }
    
    void setDelegate(String fieldName, Object value) {
        Class<?> cls= delegate.getClass();
        do {
            try {
                Field field= cls.getDeclaredField(fieldName);
                field.setAccessible(true);
                try {
                    field.set(delegate, value);
                    return;
                } catch (IllegalArgumentException e) {
                    getLog().error("cannot set "+fieldName);
                    throw new IllegalArgumentException("cannot set "+fieldName, e);
                } catch (IllegalAccessException e) {
                    throw new IllegalArgumentException("cannot set "+fieldName, e);
                }
            } catch (NoSuchFieldException e) {
                cls= cls.getSuperclass();
            }
        }
        while(cls!=null);
        getLog().error("cannot set "+fieldName);
    }

    @Override
    public void generate(Sink sink, Locale locale) throws MavenReportException {
    getLog().info("+++ generate");
        getDelegate().generate(sink, locale);
    }

    @Override
    public String getCategoryName() {
        return getDelegate().getCategoryName();
    }

    @Override
    public String getDescription(Locale locale) {
        return getDelegate().getDescription(locale);
    }

    @Override
    public String getName(Locale locale) {
        return getDelegate().getName(locale);
    }

    @Override
    public String getOutputName() {
        return getDelegate().getOutputName();
    }

    @Override
    public File getReportOutputDirectory() {
        return getDelegate().getReportOutputDirectory();
    }

    @Override
    public Sink getSink() {
        return getDelegate().getSink();
    }

    @Override
    public boolean isExternalReport() {
        return getDelegate().isExternalReport();
    }

    @Override
    public void setReportOutputDirectory(File reportOutputDirectory) {
        getDelegate().setReportOutputDirectory(reportOutputDirectory);
    }

    @Override
    protected void executeReport(Locale locale) throws MavenReportException {
        CoberturaReportMojo report= getDelegate();
        Method mth;
        try {
            mth = report.getClass().getDeclaredMethod("executeReport", Locale.class);
            mth.setAccessible(true);
            mth.invoke(delegate, locale);
        } catch (SecurityException e) {
            throw new MavenReportException(e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            throw new MavenReportException(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new MavenReportException(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            Throwable t= e.getTargetException();
            if(t instanceof RuntimeException) {
                throw (RuntimeException)t;
            }
            if(t instanceof MavenReportException) {
                throw (MavenReportException)t;
            }
            throw new MavenReportException(e.getMessage(), e);
        }
    }

    @Override
    protected String getOutputDirectory() {
        CoberturaReportMojo report= getDelegate();
        Method mth;
        try {
            mth = report.getClass().getDeclaredMethod("getOutputDirectory");
            mth.setAccessible(true);
            return (String)mth.invoke(delegate);
        } catch (SecurityException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        } catch (InvocationTargetException e) {
            Throwable t= e.getTargetException();
            if(t instanceof RuntimeException) {
                throw (RuntimeException)t;
            }
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    @Override
    protected MavenProject getProject() {
        return project;
    }

    @Override
    protected SiteRenderer getSiteRenderer() {
        return siteRenderer;
    }
}
