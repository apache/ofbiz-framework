<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

# Contributing to Apache OFBiz

Thank you for your interest in contributing to the Apache OFBiz project! Community contributions help improve the project for everyone. This document outlines the typical workflow for submitting code, documentation, and other improvements.

## Before You Start

The Apache OFBiz project follows a few common practices:

* Code must comply with the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) requirements.
* Source files should include the appropriate [Apache license header](https://github.com/apache/ofbiz-framework/blob/trunk/APACHE2_HEADER), where applicable.
* Contributors making large contributions must complete, sign, and submit a [Contributor License Agreement](https://www.apache.org/licenses/contributor-agreements.html).
* Contributors should follow the [ASF Generative Tooling Guidance](https://www.apache.org/legal/generative-tooling.html).
* For larger changes, opening an [issue](https://issues.apache.org/jira/projects/OFBIZ) or starting a discussion first is recommended.
* Contributions are submitted through GitHub Pull Requests, according to the workflows described in this document.

## Contribution Workflow (trunk)

### 1. Fork the Repository

Click **Fork** in the top-right corner of the page

https://github.com/apache/ofbiz-framework

If you want to work with the `ofbiz-plugins` repository, which is included in OFBiz releases, repeat the same step for

https://github.com/apache/ofbiz-plugins

### 2. Clone Your Fork

Clone your fork locally:

```bash
git clone https://github.com/YOUR_GITHUB_USERNAME/ofbiz-framework.git
cd ofbiz-framework
```

Add the upstream repository:

```bash
git remote add upstream https://github.com/apache/ofbiz-framework.git
```

If you have forked the `ofbiz-plugins` repository, you can clone it directly into a `plugins` folder inside the `ofbiz-framework` folder:

```bash
git clone https://github.com/YOUR_GITHUB_USERNAME/ofbiz-plugins.git plugins
```

Add the upstream repository:

```bash
cd plugins
git remote add upstream https://github.com/apache/ofbiz-plugins.git
```

### 3. Create a Branch

Create a dedicated branch for your work:

```bash
git switch -c my-feature-or-fix
```

### 4. Make Your Changes

Implement your fix, improvement, or feature.

Please try to:

* Follow the existing coding style.
* Keep changes small and focused.
* Add or update tests when appropriate.
* Ensure that all automated tasks, tests, and verification steps are successful. For example, run:
  * `./gradlew cleanAll loadAll testIntegration`
  * `./gradlew check`
  * `./gradlew javadoc`

### 5. Commit Your Changes

Write clear and descriptive commit messages so reviewers can easily understand the intent of a change and the project history remains easy to navigate.

Use a short title line that summarizes the change. Write it in the imperative mood (for example, "Fix bug", "Add feature", or "Improve validation"), keep it concise, and do not end it with a period. Focus on what the commit does rather than how it was implemented.

If additional context is needed, leave a blank line after the title and add a short paragraph explaining the reason for the change, important implementation details, or possible side effects.

Example:

```text
Fix rounding issue in order calculation

The previous implementation could produce incorrect totals in edge
cases due to double rounding. This change ensures rounding occurs
only once at the final calculation stage.
```

Issue or ticket numbers do not need to be included in commit messages; they can instead be referenced in the Pull Request description. Keep commits focused and avoid mixing unrelated changes.

### 6. Push Your Branch

Push your branch to your fork:

```bash
git push origin my-feature-or-fix
```

### 7. Open a Pull Request

Open a Pull Request against the **trunk branch** of the Apache OFBiz repository.

Follow the instructions in the [pull request template](https://github.com/apache/ofbiz-framework/blob/trunk/.github/pull_request_template.md) for the title and description of the pull request.

### 8. Review Process

Project maintainers and community members will review your pull request.

You may be asked to clarify parts of the implementation or make additional improvements. If required, please update your branch and push the changes to the same pull request.

Once the review is complete and the pull request is approved, the contribution will be merged.

### 9. Committer Review and Merge

When reviewing a Pull Request submitted by a contributor, committers will review both the contributed code changes and the quality of the commit messages. Committers should ensure that the commit messages comply with the guidelines described in this document.

The committer or reviewer should also verify that the Pull Request title and description comply with the guidelines and accurately describe the proposed changes. If necessary, they should edit the title and/or description to ensure that they are clear, accurate, and consistent with the project's guidelines before merging the Pull Request.

Before merging, the committer should verify that all CI/CD workflows associated with the Pull Request have completed successfully. A Pull Request should not be merged while any required CI/CD workflow is failing or has not yet completed, unless the failure is known to be unrelated to the proposed changes and has been explicitly assessed and addressed by a committer.

If the commit messages comply with these guidelines, the Pull Request will be merged using the **Rebase and merge** strategy. This preserves all of the original commits and their individual commit messages.

If the commit messages do not comply with these guidelines, the Pull Request will be merged using the **Squash and merge** strategy. This consolidates the Pull Request's commits into a single commit and allows to edit the commit message and description that comply with the project's guidelines.

## Contributing to Release Branches (Backports)

In some cases, a fix may also need to be applied to a maintained **release branch**.

The recommended approach is:

1. **Apply the fix to `trunk` first.**
2. **Backport the change to the release branch.**

This keeps the main development branch as the source of truth.

### Backport Workflow

1. Identify the commit that was merged into `trunk`.

2. Create a branch starting from the target release branch.

   Example for `releaseXX.YY`:

   ```bash
   git switch releaseXX.YY
   git switch -c backport-my-fix-XX.YY
   ```

3. Cherry-pick the original commit:

   ```bash
   git cherry-pick -x <commit-sha>
   ```

   The `-x` flag automatically records the original commit reference.

4. Resolve any conflicts if necessary and ensure that all verification steps are successful.

5. Push the branch:

   ```bash
   git push origin backport-my-fix-XX.YY
   ```
6. Open a Pull Request targeting the **release branch**.

## Keeping Your Fork Updated

To synchronize your fork's `trunk` branch with the upstream repository:

```bash
git fetch upstream
git switch trunk
git pull
```

You can use the same procedure for any other branch.

## Documentation Contributions

Documentation improvements are always welcome. This includes:

* Fixing typos or unclear explanations.
* Improving examples.
* Expanding guides or developer documentation.

Documentation-only pull requests follow the same workflow described above.
