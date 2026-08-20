# Configuration

!> **Jira Cloud** does not support Bearer Authentication

!> **Jira Cloud URL Configuration** — when configuring the Jira URL in Jenkins, you must use the
API endpoint format `https://api.atlassian.com/ex/jira/{cloudId}/` instead of your standard
`https://yourcompany.atlassian.net/` address. Using the standard address can trigger automated
CAPTCHA security checks, which will block Jenkins and cause the connection to fail.

## Before you start

**Always use a service account**, not a personal account, to integrate Jenkins with Jira.

To integrate Jenkins with Atlassian Jira Cloud, that service account needs an API token. Jira Cloud
requires an email address for all users, so you cannot create a user without one.

### Required Jira permissions

Make sure the service account has enough permissions for what you'll ask it to do — check via
Jira's Permission Helper tool:

- To create Jira issues, it needs **Create Issues** on the target project.
- If you also set the assignee or component fields, make sure:
  - both fields are on the corresponding Jira screen,
  - the account is **Assignable** on the project,
  - the account can **Assign Issues**.

## Steps

1. **Create an API Token**

   Follow the [Atlassian API tokens documentation](https://confluence.atlassian.com/cloud/api-tokens-938839638.html) to generate a new API token.

2. **Add a Global Jenkins Credential**

   - **Username:** Your Atlassian ID email address
   - **Password:** The API token you created

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

![plugin-configuration](images/Plugin_Configuration.png)

Connection failing? See [Troubleshooting](troubleshooting.md).

## Next steps

- **[Usage Examples](usage-examples.md)** — ready-to-copy Pipeline snippets for each step.
- **[System Properties](system-properties.md)** — settings not exposed in the UI.
