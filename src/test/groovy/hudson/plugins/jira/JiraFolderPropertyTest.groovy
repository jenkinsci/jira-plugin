package hudson.plugins.jira

import net.sf.json.JSONObject
import org.kohsuke.stapler.Stapler
import org.kohsuke.stapler.StaplerRequest
import spock.lang.Specification
import spock.lang.Unroll

/**
 * @author saville
 */
@Unroll
class JiraFolderPropertyTest extends Specification {
    def "reconfigure"() {
        given:
        JiraFolderProperty subject = new JiraFolderProperty()
        JiraSite site = Mock()
        StaplerRequest req = Mock()
        JSONObject formData = new JSONObject()
        JSONObject sites = new JSONObject()
        formData.put("sites", sites)

        expect:
        subject.sites.length==0

        when:
        def result = subject.reconfigure(req, null)

        then:
        0 * _._
        result==null

        when:
        subject.reconfigure(req, formData)

        then:
        1 * req.bindJSONToList(JiraSite, sites) >> [site]
        0 * _._
        Stapler.CONVERT_UTILS.converters[URL.class] instanceof EmptyFriendlyURLConverter
        subject.sites.length==1
        subject.sites[0]==site
    }
}
