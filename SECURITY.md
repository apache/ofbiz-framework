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

# Apache OFBiz Security Policy and Security Model

This document describes the security assumptions, trust boundaries, and responsibilities of the Apache OFBiz project. It is intended to help users, deployers, and security reviewers understand the scope of security guarantees provided by OFBiz and the responsibilities that remain with those who deploy and operate it.

Apache OFBiz is an enterprise application framework designed to run in controlled environments and administered by trusted operators.

## Security Assumptions

Apache OFBiz assumes that:
* The hosting infrastructure (operating system, JVM, network, and storage) is controlled and secured by the deploying organization.
* Administrative users are fully trusted.
* Customizations, plugins, and extensions are trusted code.
* External systems (databases, integrations, identity providers) are secured independently.

OFBiz does not attempt to defend against attackers who have obtained administrative access to the application or high-privilege access to the host system.

## Administrative Access

Users granted administrative-level privileges in OFBiz are considered fully trusted.

Administrative access should be treated as equivalent to full control over the OFBiz runtime, including effective code execution capabilities within the JVM. OFBiz does not sandbox or restrict administrator actions.

Administrative credentials must therefore be granted only to highly trusted individuals.

## Logging and Sensitive Information

OFBiz log files may contain sensitive information, including user identifiers, request parameters, business data, or internal error details.

OFBiz does not guarantee that log output is free of sensitive data. It is the responsibility of deployers and operators to:
* Restrict access to log files
* Prevent exposure of logs via web servers or shared systems
* Apply appropriate log retention and protection policies

Improperly protected logs may result in information disclosure.

## Plugins and Optional Components

Apache OFBiz is distributed with a variety of plugins and optional components.

Some plugins are:
* Experimental
* Intended for testing, demonstration, or development
* Provided as templates or reference implementations
* Disabled by default due to incomplete functionality or security assumptions

Other plugins may be suitable for production use only under specific deployment assumptions or after non-default configuration and hardening.

Distributed plugins must not be implicitly trusted solely because they are included with OFBiz. Plugins—especially those disabled by default—should be deployed in production only after verification and risk assessment.

The ecommerce component is one example of a plugin provided as a reference implementation rather than a production-ready solution.

## Operating System Privilege Boundary

OFBiz runs as a Java application within a single JVM process owned by a specific operating system user account.

All OFBiz code executes with the privileges granted to that operating system user. Restricting the permissions of this user represents an important defense-in-depth control and can limit the impact of compromised administrative accounts or vulnerabilities.

OFBiz does not manage or enforce operating system privilege separation.

## Extensions and Customizations

Custom components, extensions, scripts, and templates execute with the same privileges as core OFBiz components.

OFBiz does not provide sandboxing or privilege separation for extensions. Vulnerabilities or malicious behavior in custom code can compromise the entire application.

Deployers are responsible for reviewing, validating, and controlling all extensions deployed in production.

## External Systems and Integrations

OFBiz commonly integrates with external systems such as databases, payment providers, messaging systems, and identity services.

Security of these systems and the data they provide is outside the control of OFBiz. Deployers are responsible for securing integrations, validating external inputs, and protecting credentials.

## Network Exposure

OFBiz does not automatically restrict network exposure of its interfaces.

Deployers are responsible for controlling access to public, internal, and administrative endpoints using firewalls, reverse proxies, network segmentation, and encrypted transport.

## Non-Goals

The Apache OFBiz project does not aim to:
* Protect against malicious or compromised administrators
* Sandbox administrator actions, plugins, or extensions
* Automatically redact sensitive information from logs
* Provide production-ready functionality for all distributed plugins
* Secure infrastructure components outside OFBiz control

## Reporting Security Issues

Security vulnerabilities should be reported privately to the Apache OFBiz Security Team following Apache Software Foundation [security reporting guidelines](https://www.apache.org/security/) at: [security@ofbiz.apache.org](security@ofbiz.apache.org).

Please do not report security issues through public issue trackers or mailing lists.