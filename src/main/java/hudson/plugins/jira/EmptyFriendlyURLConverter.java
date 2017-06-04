package hudson.plugins.jira;

import org.apache.commons.beanutils.Converter;
import org.kohsuke.accmod.Restricted;
import org.kohsuke.accmod.restrictions.NoExternalUse;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * It's little hackish.
 */
@Restricted(NoExternalUse.class)
public class EmptyFriendlyURLConverter implements Converter {
    private static final Logger LOGGER = Logger
            .getLogger(JiraProjectProperty.class.getName());

    public Object convert(Class aClass, Object o) {
        if (o == null || "".equals(o) || "null".equals(o)) {
            return null;
        }
        try {
            return new URL(o.toString());
        } catch (MalformedURLException e) {
            LOGGER.log(Level.WARNING, "{0} is not a valid URL", o);
            return null;
        }
    }
}