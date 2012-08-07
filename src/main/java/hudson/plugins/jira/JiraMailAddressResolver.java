package hudson.plugins.jira;

import hudson.Extension;
import hudson.model.User;
import hudson.plugins.jira.soap.RemoteUser;
import hudson.tasks.MailAddressResolver;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.rpc.ServiceException;

/**
 *
 * @author Honza Brázdil <jbrazdil@redhat.com>
 */
@Extension
public class JiraMailAddressResolver extends MailAddressResolver{
    private static final Logger LOGGER = Logger.getLogger(JiraMailAddressResolver.class.getName());

    @Override
    public String findMailAddressFor(User u) {
        String username = u.getId();

        for(JiraSite site : JiraProjectProperty.DESCRIPTOR.getSites()){
            try {
                JiraSession session = site.createSession();
                if(session != null){
                    RemoteUser user = session.service.getUser(session.token, username);
                    if(user != null){
                        String email = user.getEmail();
                        if(email != null){
                            if(email.contains(" ")){
                                email = email.replaceAll(" at ", "@");
                                email = email.replaceAll(" dot ", ".");
                            }
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
}
