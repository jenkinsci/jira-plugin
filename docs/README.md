# Jenkins Jira plugin

This plugin integrates with Jenkins the [Atlassian Jira Software](http://www.atlassian.com/software/jira/) (both Cloud and Server versions). 

## Configuration

!> **Jira Cloud** does not support Bearer Authentication

To integrate Jenkins with Atlassian Jira Cloud, you need to use an API token as a _service user_. Jira Cloud requires an email address for all users, so you cannot create a user without one.

### Steps

1. **Create an API Token**

    Follow the [Atlassian API tokens documentation](https://confluence.atlassian.com/cloud/api-tokens-938839638.html) to generate a new API token.

2. **Add a Global Jenkins Credential**

    - **Username:** Your Atlassian ID email address
    - **Password:** The API token you created

3. **Test Your API Token**

    Verify your API token by running the following command (replace `<email>`, `<API token>`, `<YourCloudInstanceName>`, and `TEST-1` with your details):

    ```bash
    curl -X GET -u <email>:<API token> -H "Content-Type: application/json" \
      https://<YourCloudInstanceName>.atlassian.net/rest/api/latest/issue/TEST-1
    ```

    A successful response returns the issue details in JSON format.

4. **Check for CAPTCHA**

    Ensure that CAPTCHA is **not** triggered for your user, as this will prevent the API token from working. For more information, see the [CAPTCHA section in Atlassian REST API documentation](https://developer.atlassian.com/cloud/jira/platform/jira-rest-api-basic-authentication/).

5. **Test Connection**
    
    Finally, use the **Validate Settings** button on the plugin configuration page, to see if it can connect to the Jira instance.

![plugin-configuration](images/Plugin_Configuration.png)


## Something doesn't work?

First, check [Github Issues](https://github.com/jenkinsci/jira-plugin/issues) for already reported bugs.

Then, Contribute or Sponsor!

We all love Open Source, but... Open Source Software relies on contributions of fellow developers. Please contribute by [opening Pull Requests](#contributing) or if you are not a developer, consider sponsoring one of the maintainers - see ["Sponsor this project" section.](https://github.com/jenkinsci/jira-plugin)