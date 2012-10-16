package com.intuit.cc.plugin.eclipse;

import org.apache.maven.artifact.Artifact;

/**
 * Filter to only retain objects in the given scope or better.
 */
public class ScopeFilter {

    private final boolean compileScope;

    private final boolean runtimeScope;

    private final boolean testScope;

    private final boolean providedScope;

    private final boolean systemScope;

    /**
     * Create a filter
     * @param artifactScope The scope of the artifact's build
     */
    public ScopeFilter(String artifactScope) {
        if (Artifact.SCOPE_COMPILE.equals(artifactScope) || artifactScope==null) {
            systemScope = true;
            providedScope = true;
            compileScope = true;
            runtimeScope = false;
            testScope = false;
        } else if (Artifact.SCOPE_RUNTIME.equals(artifactScope)) {
            systemScope = false;
            providedScope = false;
            compileScope = true;
            runtimeScope = true;
            testScope = false;
        } else if (Artifact.SCOPE_TEST.equals(artifactScope)) {
            systemScope = true;
            providedScope = true;
            compileScope = true;
            runtimeScope = true;
            testScope = true;
        } else {
            systemScope = false;
            providedScope = false;
            compileScope = false;
            runtimeScope = false;
            testScope = false;
        }
    }

    /**
     * Should this dependency be included in this scope?
     * @param dependencyScope The dependency's scope 
     * @return true, if the dependency should be included in the current build scope
     */
    public boolean isIncluded(String dependencyScope) {
        if (Artifact.SCOPE_COMPILE.equals(dependencyScope) || dependencyScope==null) {
            return compileScope;
        } else if (Artifact.SCOPE_RUNTIME.equals(dependencyScope)) {
            return runtimeScope;
        } else if (Artifact.SCOPE_TEST.equals(dependencyScope)) {
            return testScope;
        } else if (Artifact.SCOPE_PROVIDED.equals(dependencyScope)) {
            return providedScope;
        } else if (Artifact.SCOPE_SYSTEM.equals(dependencyScope)) {
            return systemScope;
        } else {
            return false;
        }
    }
}
