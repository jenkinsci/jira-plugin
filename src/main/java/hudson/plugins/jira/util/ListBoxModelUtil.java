package hudson.plugins.jira.util;

import hudson.plugins.jira.JiraComponent;
import hudson.plugins.jira.JiraIssueType;
import hudson.plugins.jira.JiraPriority;
import hudson.util.ListBoxModel;

import java.util.Collection;

/**
 * Created with IntelliJ IDEA.
 * User: jdewinne
 * Date: 3/28/13
 * Time: 11:12 AM
 * To change this template use File | Settings | File Templates.
 */
public class ListBoxModelUtil {

    public static ListBoxModel of(Collection<String> types) {
        ListBoxModel m = new ListBoxModel();
        if (types != null) {
            for (String s : types)
                m.add(s, s);
        }
        return m;
    }

    public static ListBoxModel convertJiraIssueType(Collection<JiraIssueType> types) {
        ListBoxModel m = new ListBoxModel();
        if (types != null) {
            for (JiraIssueType jiraIssueType: types)
                m.add(jiraIssueType.name, jiraIssueType.id);
        }
        return m;
    }

    public static ListBoxModel  convertJiraPriority(Collection<JiraPriority> priorities) {
        ListBoxModel m = new ListBoxModel();
        if (priorities != null) {
            for (JiraPriority jiraPriority: priorities)
                m.add(jiraPriority.name, jiraPriority.id);
        }
        return m;
    }
    public static ListBoxModel  convertJiraComponent(Collection<JiraComponent> components) {
        ListBoxModel m = new ListBoxModel();
        if (components != null) {
            for (JiraComponent jiraComponent: components)
                m.add(jiraComponent.name, jiraComponent.id);
        }
        return m;
    }





    public static ListBoxModel emptyModel() {
        return new ListBoxModel();
    }
}
