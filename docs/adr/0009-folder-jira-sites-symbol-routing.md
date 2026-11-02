# ADR 0009: Symbol-Based Mapping for Folder-Level Jira Sites (Avoiding ContextExtensionPoint)

## Status
Accepted

## Context
Issue #1225 requires enabling Job DSL and Configuration as Code support for folder-level Jira site configurations (`JiraFolderProperty` and `JiraSite`).

During architecture evaluation, two primary integration routes were considered:
1. **Custom ContextExtensionPoint:** Implementing custom context extension handlers to resolve Job DSL scopes.
2. **Standard `@Symbol` Annotations & Describable Model:** Annotating `JiraFolderProperty.DescriptorImpl` and `JiraSite.DescriptorImpl` with standard Jenkins `@Symbol` annotations (`jiraFolderProperty` and `jiraSite` respectively) allowing the standard Job DSL and Stapler/JCasC engines to automatically discover and map the configuration hierarchy.

## Decision
We chose the **Standard `@Symbol` Annotations & Describable Model** route and **did not take** the `ContextExtensionPoint` approach.

### Reasons for not taking `ContextExtensionPoint`:
- **Unnecessary Complexity:** `ContextExtensionPoint` is typically required for complex nested DSL context lookups where standard introspection falls short. For simple folder properties and nested describables, standard `@Symbol` bindings provide full native introspection automatically.
- **Maintainability & Upgradability:** Standard annotations integrate natively with newer Jenkins core and Job DSL generator mechanisms without requiring custom extension point wiring that could break across plugin version upgrades.

## Consequences
- Job DSL scripts can seamlessly reference `jiraFolderProperty` and `jiraSite` blocks out-of-the-box.
- `DescribableModel` correctly exposes the property structure without extra runtime adapters.
- Reduced maintenance overhead and cleaner codebase.