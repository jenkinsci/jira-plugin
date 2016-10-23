package hudson.plugins.jira;

import hudson.plugins.jira.model.JiraVersion;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matcher;

public class JiraVersionMatcher {

	public static Matcher<JiraVersion> hasName(Matcher<String> subMatcher) {
		return new FeatureMatcher<JiraVersion, String>(subMatcher, 
				"expected version name", "actual version name") {
			@Override
			protected String featureValueOf(JiraVersion version) {
				return version.getName();
			}
		};
	}

}