package hudson.plugins.jira

import com.atlassian.jira.rest.client.api.domain.Issue
import com.cloudbees.hudson.plugins.folder.AbstractFolder
import com.cloudbees.hudson.plugins.folder.AbstractFolderProperty
import hudson.model.ItemGroup
import hudson.model.Job
import hudson.util.DescribableList
import jenkins.model.Jenkins
import org.powermock.api.mockito.PowerMockito
import org.junit.runner.RunWith
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.PowerMockRunner
import org.powermock.modules.junit4.PowerMockRunnerDelegate
import org.spockframework.runtime.Sputnik
import spock.lang.Specification
import spock.lang.Unroll

import static org.mockito.Mockito.*

/**
 * @author saville
 */
@Unroll
@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(Sputnik.class)
@PrepareForTest([JiraGlobalConfiguration.class])
class JiraSiteTest extends Specification {

    def "get"() {
        given:
        Jenkins jenkins = Mock()
        Jenkins.theInstance = jenkins
        PowerMockito.mockStatic(JiraGlobalConfiguration.class)
        JiraGlobalConfiguration jiraGlobalConfiguration = Mock()
        when(JiraGlobalConfiguration.get()).thenReturn(jiraGlobalConfiguration)
        jiraGlobalConfiguration.getSites() >> Collections.emptyList()
        Job<?, ?> job = Mock()
        JiraProjectProperty jpp = Spy(JiraProjectProperty)
        JiraFolderProperty jfp = Mock()
        ItemGroup nonFolderParent = Mock()
        AbstractFolder folder1 = Mock()
        AbstractFolder folder2 = Mock()
        AbstractFolder folder3 = Mock()
        DescribableList<AbstractFolderProperty> folder1Properties = Mock()
        DescribableList<AbstractFolderProperty> folder2Properties = Mock()
        DescribableList<AbstractFolderProperty> folder3Properties = Mock()
        JiraSite site1 = Mock()
        JiraSite site2 = Mock()

        when: "site is configured on project property"
        def result = JiraSite.get(job)

        then:
        1 * job.getProperty(JiraProjectProperty) >> jpp
        1 * jpp.getSite() >> site1
        0 * _._
        result==site1

        when: "project property site and parent are both null"
        result = JiraSite.get(job)

        then:
        1 * job.getProperty(JiraProjectProperty) >> jpp
        1 * jpp.getSite() >> null
        1 * job.getParent() >> null
        1 * jiraGlobalConfiguration.getSites() >> Collections.emptyList()
        0 * _._
        result==null

        when: "no project property, parent is not a folder and is not an item"
        result = JiraSite.get(job)

        then:
        1 * job.getProperty(JiraProjectProperty) >> null
        1 * job.getParent() >> nonFolderParent
        1 * jiraGlobalConfiguration.getSites() >> Collections.emptyList()
        0 * _._
        result==null

        when: "no project property, go up folders with no property"
        result = JiraSite.get(job)

        then:
        1 * job.getProperty(JiraProjectProperty.class) >> null
        1 * job.getParent() >> folder1
        1 * folder1.getProperties() >> folder1Properties
        1 * folder1Properties.get(JiraFolderProperty) >> null
        1 * folder1.getParent() >> folder2
        1 * folder2.getProperties() >> folder2Properties
        1 * folder2Properties.get(JiraFolderProperty) >> null
        1 * folder2.getParent() >> nonFolderParent
        1 * jiraGlobalConfiguration.getSites() >> Collections.emptyList()
        0 * _._
        result==null

        when: "no project property, find folder property with null, 0 length, and valid sites"
        result = JiraSite.get(job)

        then:
        1 * job.getProperty(JiraProjectProperty.class) >> null
        1 * job.getParent() >> folder1
        1 * folder1.getProperties() >> folder1Properties
        1 * folder1Properties.get(JiraFolderProperty) >> jfp
        1 * folder1.getParent() >> folder2
        1 * folder2.getProperties() >> folder2Properties
        1 * folder2Properties.get(JiraFolderProperty) >> jfp
        1 * folder2.getParent() >> folder3
        1 * folder3.getProperties() >> folder3Properties
        1 * folder3Properties.get(JiraFolderProperty) >> jfp
        3 * jfp.getSites() >>> [[], [], [site2, site1]]
        0 * _._
        result==site2

        when: "site is configured globally"
        result = JiraSite.get(job)

        then:
        1 * job.getProperty(JiraProjectProperty.class) >> null
        1 * job.getParent() >> nonFolderParent
        1 * jiraGlobalConfiguration.getSites() >> [site2]
        0 * _._
        result==site2
    }


    def "getIssue"() {
        given:
        def site = new JiraSite(null, null, null, false, false, null, false, null, null, false)
        def session = Mock(JiraSession)
        def issue = Mock(Issue)
        site.jiraSession = session

        when: "issue exists"
        def result = site.getIssue("FOO-1")

        then:
        1 * issue.getKey() >> "FOO-1"
        1 * issue.getSummary() >> "commit message"
        1 * session.getIssue("FOO-1") >> issue
        0 * _._
        result != null
        result.getKey() == "FOO-1"

        when: "no issue found"
        result = site.getIssue("BAR-2")

        then:
        1 * session.getIssue("BAR-2") >> null
        0 * _._
        result == null
    }
}
