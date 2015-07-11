package hudson.plugins.jira;

import hudson.Extension;
import hudson.model.User;
import hudson.tasks.MailAddressResolver;

import javax.xml.rpc.ServiceException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Resolve user email by searching his userId as username in JIRA.
 *
 * @author Honza Brázdil jbrazdil@redhat.com
 */
@Extension
public class JiraMailAddressResolver extends MailAddressResolver {
    private static final Logger LOGGER = Logger.getLogger(JiraMailAddressResolver.class.getName());

    /**
     * Boolean to disable the Jira mail address resolver.
     *
     * To disable set the System property "-Dhudson.plugins.jira.JiraMailAddressResolver.disabled=true"
     */
    public static boolean disabled = Boolean.getBoolean(JiraMailAddressResolver.class.getName() + ".disabled");

    @Override
    public String findMailAddressFor(User u) {
        if (disabled)
            return null;

        String username = u.getId();

        for (JiraSite site : JiraProjectProperty.DESCRIPTOR.getSites()) {
            try {
                JiraSession session = site.createSession();
                if (session != null) {
                    com.atlassian.jira.rest.client.api.domain.User user = session.service.getUser(username);
                    if (user != null) {
                        String email = user.getEmailAddress();
                        if (email != null) {
                            email = unmaskEmail(email);
                            return email;
                        }
                    }
                }
            } catch (IOException ex) {
                LOGGER.log(Level.WARNING, "Unable to create session with " + site.getName(), ex);
            } catch (ServiceException ex) {
                LOGGER.log(Level.WARNING, "Unable to create session with " + site.getName(), ex);
            }
        }
        return null;
    }

    private static final String PRE = "[( \\[<_{\"=]+";
    private static final String POST = "[) \\]>_}\"=]+";
    private static final Pattern AT = Pattern.compile(PRE + "[aA][tT]" + POST);
    private static final Pattern DOT = Pattern.compile(PRE + "[dD][oO0][tT]" + POST);

    // unmask emails like "john dot doe at example dot com" to john.doe@example.com
    static String unmaskEmail(String email) {
        email = AT.matcher(email).replaceAll("@");
        email = DOT.matcher(email).replaceAll(".");
        return email;
    }
}
