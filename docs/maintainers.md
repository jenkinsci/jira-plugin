# For Maintainers

Notes for jira-plugin maintainers — release process and a couple of decisions that aren't obvious
from the code.

## Atlassian sources import

To resolve [some binary compatibility issues](https://github.com/jenkinsci/jira-plugin/pull/140),
sources from [`com.atlassian.httpclient:atlassian-httpclient-plugin:0.23`](https://packages.atlassian.com/maven-external/com/atlassian/httpclient/atlassian-httpclient-plugin/0.23.0/)
are imported directly into this project to control the HTTP(S) transport layer. Those downloaded
sources had no license headers, but per their
[POM](https://packages.atlassian.com/maven-external/com/atlassian/httpclient/atlassian-httpclient-plugin/0.23.0/atlassian-httpclient-plugin-0.23.0.pom)
(also vendored at `src/main/resources/atlassian-httpclient-plugin-0.23.0.pom`) they're
Apache-licensed.

## Releasing the plugin

See [releasing Jenkins plugins](https://www.jenkins.io/doc/developer/publishing/releasing-manually/).
