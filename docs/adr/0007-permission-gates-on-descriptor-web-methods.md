# Permission gates and POST on descriptor web methods

* Status: accepted
* Date: 2026-09-07

## Context and Problem Statement

Every `doCheck*` and `doFill*` method on a descriptor is a Stapler web method: a URL under
`/descriptorByName/<class>/...`, routed independently of the configuration form the field appears
on. The [Jenkins form validation guidance](https://www.jenkins.io/doc/developer/security/form-validation/)
is that such a method declares its HTTP verb and answers according to what the caller is entitled to
see, rather than assuming it was reached from the form.

This plugin's descriptors did neither, and did so inconsistently. Ten web methods across four
descriptors had no verb annotation and no permission check, while `JiraSite.doValidate`,
`JiraVersionParameterDefinition.doFillVersionItems` and `JiraIssueParameterDefinition` already used
`@RequirePOST`, and `CredentialsHelper` already applied the standard credentials gate. A contributor
adding the eleventh had two contradictory examples to copy.

They also differ a lot in what they do. `JiraCreateIssueNotifier.DescriptorImpl.doFillPriorityIdItems`
and `doFillTypeIdItems` resolve the site's credentials and call the configured Jira instance to build
their list. Others only report whether a URL parses, whether a JQL string is empty, or the names of
the configured Jira sites. The convention should not depend on which end of that range a method sits
at, or every new one becomes a judgement call.

## Decision Drivers

* One rule, applied uniformly, so the pattern to copy is unambiguous.
* A web method should not do work on behalf of a caller who could not have opened the form it belongs
  to, least of all work that calls out to Jira.
* The configuration UI must keep working exactly as before for users who *can* configure.
* Whatever is written has to be recognised by the static analysis that runs on every pull request,
  or the plugin keeps being told it is missing.

## Considered Options

* **Leave them as they are.** Keeps the inconsistency, and the analysis keeps reporting it on every
  pull request that touches these files.
* **`@RequirePOST` only.** Declares the verb, but says nothing about what the caller may see: a POST
  is no harder to send than a GET.
* **`checkPermission`, throwing `AccessDeniedException`.** Answers the second half, but a read-only
  user opening a config page then gets an error rendered under every gated field.
* **`hasPermission`, returning an empty answer** (`FormValidation.ok()` / an empty `ListBoxModel`),
  plus `@RequirePOST`. Chosen.

## Decision Outcome

Every `doCheck*` / `doFill*` method on this plugin's descriptors is annotated `@RequirePOST` and
gated on a permission check that returns an empty answer rather than throwing:

```java
@RequirePOST
public FormValidation doCheckJqlSearch(@AncestorInPath Item item, @QueryParameter String value) {
    if (!DescriptorPermissions.canSeeConfiguration(item)) {
        return FormValidation.ok();
    }
    ...
}
```

`DescriptorPermissions.canSeeConfiguration` is the single definition of the gate: `Item.CONFIGURE` on
the item in the request path, or `Jenkins.ADMINISTER` when there is no item, meaning global
configuration. It exists as one helper rather than ten inline copies so that changing the gate is a
one-line change; the analysis follows the delegation, as it already does for `CredentialsHelper`.

`@RequirePOST` is safe here and does not need a matching `checkMethod="post"` in any `config.jelly`.
Inline form validation has posted the check request since **Jenkins 2.285**, far below this plugin's
baseline: core's own `lib/form/textbox.jelly` and `lib/form/select.jelly` document `checkMethod` as
the way to opt *back* to GET, and core's `lib/form/select/select.js` fetches `fillUrl` with
`method: "post"`. The `c:select` tag from the credentials plugin delegates to `f:select`, so the
credentials dropdown posts too.

Methods whose permission check lives in a delegate keep it there. `doFillCredentialsIdItems` and
`doCheckCredentialsId` on `JiraSite.DescriptorImpl` pass straight through to `CredentialsHelper`,
which already applies the standard credentials gate (`Item.EXTENDED_READ` or
`CredentialsProvider.USE_ITEM`); those two only gained the annotation.

### Consequences

* A user with read access to a job but not `Item/Configure` sees empty Jira dropdowns and no inline
  validation on that job's configuration page. This is the intended outcome and is written up in
  [Troubleshooting](../troubleshooting.md), because the symptom looks like a broken plugin.
* Adding a new `doCheck*` / `doFill*` to a descriptor in this plugin without both the annotation and
  the gate is a regression, and the analysis will say so on the pull request.
* Returning `ok()` rather than throwing means a denied caller cannot tell a permission problem apart
  from a valid value. That is deliberate: the form is not theirs to submit either way.
