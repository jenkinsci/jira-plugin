package hudson.plugins.jira.extension;

import hudson.PluginManager;
import java.net.URI;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.RuntimeDelegate;

/**
 * Defends against the fact that {@link RuntimeDelegate#getInstance} is not safe when multiple copies of that library may be in {@link PluginManager#uberClassLoader}.
 * @see <a href="https://www.jenkins.io/doc/developer/plugin-development/dependencies-and-class-loading/#context-class-loaders">Dependencies and Class Loading » Context class loaders</a>
 */
class UriBuilderHelper {

    /**
     * Same as {@link UriBuilder#fromUri(URI)}.
     */
    static UriBuilder fromUri(URI uri) {
        Thread t = Thread.currentThread();
        ClassLoader orig = t.getContextClassLoader();
        t.setContextClassLoader(UriBuilderHelper.class.getClassLoader());
        try {
            return UriBuilder.fromUri(uri);
        } finally {
            t.setContextClassLoader(orig);
        }
    }

    private UriBuilderHelper() {}

}
