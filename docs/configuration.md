# Configuration

!> **Jira Cloud URL Configuration** — when configuring the Jira URL in Jenkins, you must use the
API endpoint format `https://api.atlassian.com/ex/jira/{cloudId}/` instead of your standard
`https://yourcompany.atlassian.net/` address. Using the standard address can trigger automated
CAPTCHA security checks, which will block Jenkins and cause the connection to fail.

## Before you start

**Always use a service account**, not a personal account, to integrate Jenkins with Jira.

The **Use Bearer authentication instead of Basic authentication** checkbox controls how the
credential's password field is sent to Jira:

- **Unchecked (Basic authentication):** the credential's username and password are sent together,
  Base64-encoded. This is what a Jira Cloud API token or a traditional Data Center/Server
  username+password login use.
- **Checked (Bearer authentication):** only the credential's password field is sent, as an
  `Authorization: Bearer <token>` header — the username field is ignored entirely. This is what a
  Jira Data Center/Server Personal Access Token uses, and also what a Jira Cloud OAuth 2.0 access
  token uses.

In short: Bearer authentication isn't a Server-only thing — it depends on the *kind of token*
you're authenticating with, not on Cloud vs. Data Center/Server.

### Required Jira permissions

Make sure the service account has enough permissions for what you'll ask it to do — check via
Jira's Permission Helper tool:

- To create Jira issues, it needs **Create Issues** on the target project.
- If you also set the assignee or component fields, make sure:
  - both fields are on the corresponding Jira screen,
  - the account is **Assignable** on the project,
  - the account can **Assign Issues**.

## Jira Cloud

To integrate Jenkins with Atlassian Jira Cloud, you need to use an API token as a _service user_.
Jira Cloud requires an email address for all users, so you cannot create a user without one.

### Using an API token (Basic authentication)

This is the common case, and works whether the Jira URL is your standard
`https://yourcompany.atlassian.net/` address or the `https://api.atlassian.com/ex/jira/{cloudId}/`
gateway form.

1. **Create an API Token**

   Follow the [Atlassian API tokens documentation](https://confluence.atlassian.com/cloud/api-tokens-938839638.html) to generate a new API token.

2. **Add a Global Jenkins Credential**

   - **Username:** Your Atlassian ID email address
   - **Password:** The API token you created
   - Leave **Use Bearer authentication** unchecked.

3. **Test Your API Token**

   Verify your API token by running the following command (replace `<email>`, `<API token>`,
   `<YourCloudInstanceName>`, and `TEST-1` with your details):

   ```bash
   curl -X GET -u <email>:<API token> -H "Content-Type: application/json" \
     https://<YourCloudInstanceName>.atlassian.net/rest/api/latest/issue/TEST-1
   ```

   A successful response returns the issue details in JSON format.

4. **Check for CAPTCHA**

   Ensure that CAPTCHA is **not** triggered for your user, as this will prevent the API token from
   working. For more information, see the
   [CAPTCHA section in Atlassian REST API documentation](https://developer.atlassian.com/cloud/jira/platform/jira-rest-api-basic-authentication/).

5. **Test Connection**

   Finally, use the **Validate Settings** button on the plugin configuration page, to see if it can
   connect to the Jira instance.

### Using an OAuth 2.0 access token (Bearer authentication)

If instead you're authenticating against the `https://api.atlassian.com/ex/jira/{cloudId}/`
gateway with an OAuth 2.0 access token (for example, one obtained by a Connect or Forge app)
rather than a classic API token:

1. **Add a Global Jenkins Credential**

   - **Username:** any value — it's ignored when Bearer authentication is used
   - **Password:** the OAuth 2.0 access token
   - Check **Use Bearer authentication instead of Basic authentication**.

2. **Test Connection**

   Use the **Validate Settings** button to confirm Jenkins can connect.

![plugin-configuration](images/Plugin_Configuration.png)

## Jira Data Center / Server

Jira Data Center/Server supports both a traditional username+password login and, since Jira 8.14,
[Personal Access Tokens (PATs)](https://confluence.atlassian.com/enterprise/using-personal-access-tokens-1026032365.html).

### Using a username and password (Basic authentication)

- **Username:** your Jira login username
- **Password:** your Jira login password
- Leave **Use Bearer authentication** unchecked.

### Using a Personal Access Token (Bearer authentication)

1. **Create a Personal Access Token** in your Jira user profile under **Personal Access Tokens**.

2. **Add a Global Jenkins Credential**

   - **Username:** any value — it's ignored when Bearer authentication is used
   - **Password:** the Personal Access Token
   - Check **Use Bearer authentication instead of Basic authentication**.

3. **Test Connection**

   Use the **Validate Settings** button to confirm Jenkins can connect.

Connection failing? See [Troubleshooting](troubleshooting.md).

## Folder-level Jira sites

Jira sites are normally configured once globally, under **Manage Jenkins** -> **System** -> **Jira**.
They can also be attached to a folder, so the jobs inside it talk to a different Jira instance
without touching the global list. On the folder's **Configure** page, add the **Associated Jira**
property and define its sites there.

For a given job the plugin picks the site in this order:

1. The site selected on the job itself (**Jira site** on the job's configuration page), chosen from
   the global list.
2. The first site found walking up the folder chain, nearest folder first, so a site on a subfolder
   wins over one on its parent.
3. The single globally configured site, if there is exactly one.
4. Otherwise none, and the build step logs that no Jira site is configured.

A folder can hold more than one site, but **a job talks to exactly one of them**. Every step resolves
its site through the order above and no step takes a site parameter, so a single pipeline cannot
update issues on one instance and create a version on another. Which site a job gets is decided at
configuration time, by the **Jira site** field on the job: leave it unset and the first site on the
nearest folder wins; name one and that site is used, whether it comes from the folder or from the
global list. Two jobs in the same folder can therefore target different instances, but one job
cannot.

The extra sites do widen what the forms offer: the **Jira site** dropdown on a job lists the global
sites plus the folder's, and the **Issue Priority** and **Issue Type** dropdowns of the
*Jira: Create issue* post-build action list entries from each site in scope, labelled by site name.

### Configuring sites as code

Global sites are configurable with
[Configuration as Code](https://github.com/jenkinsci/configuration-as-code-plugin):

```yaml
unclassified:
  jiraglobalconfiguration:
    sites:
      - url: "https://issues.example.org/"
      - url: "https://jira.example.com/"
```

Folder sites are not reachable from a JCasC document. JCasC only creates items through the `jobs:`
element contributed by the Job DSL plugin, and there is no Job DSL binding for the **Associated
Jira** property. Build them from a Groovy init script in `$JENKINS_HOME/init.groovy.d/` instead:

```groovy
import com.cloudbees.hudson.plugins.folder.Folder
import hudson.plugins.jira.JiraFolderProperty
import hudson.plugins.jira.JiraSite
import jenkins.model.Jenkins

def folder = Jenkins.get().getItemByFullName('platform', Folder)

def property = new JiraFolderProperty()
property.setSites([new JiraSite('https://issues.example.org/')])   // set the list
property.setSites(new JiraSite('https://jira.example.com/'))       // append another site

folder.properties.replace(property)
folder.save()
```

`setSites(List)` replaces the list, `setSites(site)` appends one, and both take their own copy, so
the list you pass in stays yours and the script can run against a freshly created folder or an
existing one.

## System Properties

Some plugin behaviour is only changeable globally, by overriding
[Jenkins system properties](https://www.jenkins.io/doc/book/managing/system-properties/) — for
settings that aren't exposed in the UI.

- `-Dhudson.plugins.jira.JiraMailAddressResolver.disabled=true`

  Disables resolving a user's email address from their Jira username.
