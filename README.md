# PICC-PC-Gitlab-Integration

[![Java Version](https://img.shields.io/badge/Java-21%20LTS-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.4-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Cloud](https://img.shields.io/badge/Spring%20Cloud-2025.0.0-6DB33F?logo=spring&logoColor=white)](https://spring.io/projects/spring-cloud)
[![Hazelcast](https://img.shields.io/badge/Hazelcast-5.5.0-red.svg)](https://hazelcast.com/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/Build-Passing-brightgreen.svg)]()

Enterprise GitLab REST API v4 integration microservice establishing standard user provisioning, group governance, project lifecycle automation, access token management, and distributed caching across the **Platform Infrastructure and Core Components (PICC)** suite of the **Nubo Native Platform (NNP)**.

---

## Table of Contents

- [Overview](#overview)
- [Key Architectural Features](#key-architectural-features)
- [Architecture and Ecosystem](#architecture-and-ecosystem)
- [Technology Matrix](#technology-matrix)
- [Quick Start](#quick-start)
  - [Prerequisites](#prerequisites)
  - [Configuration](#configuration)
  - [Local Execution](#local-execution)
  - [Docker Container Execution](#docker-container-execution)
- [REST API Capabilities](#rest-api-capabilities)
- [Project Documentation](#project-documentation)
- [Repository Structure](#repository-structure)
- [Security and Vulnerability Management](#security-and-vulnerability-management)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

**`PICC-PC-Gitlab-Integration`** provides a resilient, declarative integration gateway between internal platform services and the **GitLab REST API v4**. It automates identity lifecycle management, multi-tier group and subgroup hierarchies, repository initialization, and personal access token (PAT) provisioning while maximizing throughput via embedded loopback **Hazelcast 5.5** caching.

Inheriting from the platform's standardized BOM ([`PICC-PC-Abstract-NNP-Platform`](https://github.com/nubons/PICC-PC-Abstract-NNP-Platform)), `PICC-PC-Gitlab-Integration` enforces Java 21 LTS runtime standards, Spring Cloud OpenFeign patterns, and automated DevSecOps compliance scanning.

---

## Key Architectural Features

- **Declarative REST Integration**: Powered by **Spring Cloud OpenFeign** with dedicated `RequestInterceptor` authorization and custom `GitLabClientErrorDecoder` exception translation.
- **Identity & Access Governance**: Automated user provisioning with security constraints (`external=true`, `projects_limit=0`, `can_create_group=false`).
- **Group Hierarchy Orchestration**: Multi-tier group and subgroup provisioning with configurable creation levels and membership assignments.
- **Project Lifecycle Automation**: Automated repository creation, namespace assignment, visibility controls, and default README initialization.
- **Personal Access Token (PAT) Management**: Dynamic token generation, discovery, and revocation for automated CI/CD workflows and image pull secrets.
- **Loopback In-Memory Caching**: High-throughput single-node Hazelcast caching (`127.0.0.1:5701`) eliminating redundant network round-trips for group membership resolution.
- **Event-Driven Cache Invalidation**: Asynchronous cache invalidation and re-indexing powered by Spring Event Multicaster (`CacheRefreshEvent`).
- **DevSecOps Security Pipeline**:
  - **SAST**: SpotBugs + FindSecBugs security rules (`spotbugs-exclude.xml`).
  - **SCA**: OWASP Dependency-Check enforcing zero CVSS >= 7.0 vulnerabilities.
  - **SBOM**: CycloneDX plugin generating immutable Software Bill of Materials (`application.cdx.json`).

---

## Architecture and Ecosystem

```mermaid
graph TD
    classDef client fill:#0284C7,stroke:#38BDF8,stroke-width:2px,color:#FFFFFF;
    classDef app fill:#1E293B,stroke:#64748B,stroke-width:2px,color:#F8FAFC;
    classDef comp fill:#0F172A,stroke:#38BDF8,stroke-width:1px,color:#E2E8F0;
    classDef ext fill:#D97706,stroke:#F59E0B,stroke-width:2px,color:#FFFFFF;

    Clients["<b>Platform Microservices & Clients</b><br/>REST API Consumers"]:::client

    subgraph CoreService ["<b>PICC-PC-Gitlab-Integration</b>"]
        Controller["<b>REST API Gateway (Port 8080)</b><br/>GitServicesAPI | NnpGitServiceAPI"]:::comp
        Svc["<b>Service Layer</b><br/>GitLabService | NnpGitLabService"]:::comp
        Cache["<b>Hazelcast Cache Grid (127.0.0.1:5701)</b><br/>all-group-members Map"]:::comp
        EventBus["<b>Spring Event Bus</b><br/>CacheRefreshEvent Consumer"]:::comp
        Feign["<b>Spring Cloud OpenFeign Engine</b><br/>GitLabClient | NnpGitlabClient<br/>GitLabClientErrorDecoder"]:::comp
    end

    GitLab["<b>GitLab REST API v4</b><br/>https://gitlab.example.com/api/v4"]:::ext

    Clients --> Controller
    Controller --> Svc
    Svc --> Cache
    Svc --> EventBus
    EventBus --> Cache
    Svc --> Feign
    Feign --> GitLab
```

---

## Technology Matrix

| Category | Component / Library | Version | Role / Description |
| :--- | :--- | :--- | :--- |
| **Runtime** | Java JDK | `21` (LTS) | Long-Term Support Java runtime environment |
| **Parent BOM** | `abstract-nnp` | `1.0.0` | Standardized enterprise parent and dependency governance |
| **Framework** | `spring-boot-starter-web` | `3.5.4` | Enterprise microservice application framework |
| **Cloud / RPC** | `spring-cloud-starter-openfeign` | `2025.0.0` | Declarative HTTP client for GitLab API v4 |
| **Caching** | `hazelcast` | `5.5.0` | Distributed in-memory caching and compute |
| **Logging** | `spring-boot-starter-log4j2` | Managed | High-throughput asynchronous logging |
| **Object Mapping**| `modelmapper` / `json` | Managed | DTO transformation and JSON processing |
| **API Specs** | `springdoc-openapi-starter-webmvc-ui` | Managed | OpenAPI 3.0 / Swagger interactive API docs |
| **SAST** | `spotbugs-maven-plugin` + `findsecbugs` | `4.8.6.0` / `1.13.0` | Static code security analysis |
| **SCA** | `dependency-check-maven` | `10.0.4` | Automated dependency CVE vulnerability scanner |
| **SBOM** | `cyclonedx-maven-plugin` | `2.9.1` | CNCF/CycloneDX Software Bill of Materials generator |

---

## Quick Start

### Prerequisites
- **JDK 21** (Temurin or OpenJDK LTS).
- **Maven 3.9+** (or use included `./mvnw`).
- A GitLab instance (GitLab.com or self-hosted GitLab CE/EE 15+) with an **Admin / Personal Access Token** having `api` and `read_user` scopes.

### Configuration
Configure your target GitLab instance in `src/main/resources/application.properties` or via environment variables:

```properties
# Server Configuration
server.port=8080
spring.application.name=gitlab-int

# Primary GitLab Feign Client Configuration
feign.name=gitlab-client
feign.url=https://gitlab.example.com
feign.url.api=/api/v4
feign.apiToken=glpat-yourSecretAdminTokenHere

# Hazelcast Configuration (Single-Node Loopback)
hazelcast.port=5701
```

### Local Execution

```bash
# Clone the repository
git clone https://github.com/nubons/PICC-PC-Gitlab-Integration.git
cd PICC-PC-Gitlab-Integration

# Validate POM structure
./mvnw validate

# Compile and package application
./mvnw clean package

# Run locally with Spring Boot
./mvnw spring-boot:run
```

### Docker Container Execution

```bash
# Build Docker image
docker build -t picc-pc-gitlab-integration:latest .

# Run container with environment overrides
docker run -d -p 8080:8080 \
  -e FEIGN_URL=https://gitlab.example.com \
  -e FEIGN_APITOKEN=glpat-yourSecretAdminTokenHere \
  --name gitlab-integration picc-pc-gitlab-integration:latest
```

---

## REST API Capabilities

| Method | Endpoint Path | Description |
| :--- | :--- | :--- |
| `POST` | `/api/createUser` | Provisions a new GitLab user with restricted permissions |
| `GET` | `/api/getUser?username={name}` | Retrieves user details by username |
| `DELETE` | `/api/deleteUser?id={id}` | Deletes a user by ID |
| `POST` | `/api/createGroups` | Creates a top-level group or subgroup |
| `DELETE` | `/api/deleteGroup/?id={id}` | Deletes a group by ID |
| `POST` | `/api/associateUser` | Associates a user with a group and role |
| `GET` | `/api/getListOfMembers/{groupId}` | Gets members of a specified group |
| `POST` | `/api/v1/onboard/user` | Complete user onboarding workflow with group association |
| `GET` | `/api/getGroups/{userId}` | Gets all accessible groups for a user |
| `GET` | `/api/getAllGroups` | Gets all top-level groups |
| `GET` | `/api/exist/group/{groupName}` | Checks if a group exists |
| `GET` | `/api/getGroupDetails/{groupName}` | Gets detailed group information |
| `GET` | `/api/getLcncGitPAT/{userId}` | Retrieves or rotates a Personal Access Token |
| `POST` | `/api/createEnvironment` | End-to-end environment setup (Groups, Repos, ArgoCD structure) |
| `GET` | `/api/refreshCache` | Manually triggers immediate cache re-indexing |
| `GET` | `/nnp/api/getNNPGitLabPAT/{userId}/{env}` | Retrieves or rotates scoped NNP PAT token |

---

## Project Documentation

Comprehensive documentation is provided in the repository:

- **[User Manual and Deployment Guide](USER_MANUAL_AND_DEPLOYMENT_GUIDE.md)**: Operational capabilities, Docker containerization, Kubernetes pod manifests, port isolation architecture, and troubleshooting FAQ.
- **[Development Guidelines and Contribution Standards](DEVELOPMENT_GUIDELINES.md)**: Architecture governance, Feign client conventions, Hazelcast bean lifecycle, PR checklists, and branching standards.
- **[Contributing Guide](CONTRIBUTING.md)**: Open source contribution workflow and issue templates.
- **[Maintainers Registry](MAINTAINERS.md)**: Core maintainers and project leadership.
- **[Security Policy](SECURITY.md)**: Vulnerability disclosure workflow and security reporting.
- **[Code of Conduct](CODE_OF_CONDUCT.md)**: Community participation guidelines.

---

## Repository Structure

```
PICC-PC-Gitlab-Integration/
├── .github/
│   └── workflows/
│       └── ci-cd.yml                        # GitHub Actions CI/CD automation
├── .mvn/
│   └── wrapper/                             # Maven Wrapper binaries & properties
├── src/
│   ├── main/
│   │   ├── java/com/numbons/gitlabintegration/
│   │   │   ├── api/
│   │   │   │   ├── client/                  # OpenFeign HTTP Clients (GitLab REST API)
│   │   │   │   ├── configuration/           # Feign Interceptors, ErrorDecoders, Hazelcast Config
│   │   │   │   ├── exception/               # Custom API Exception definitions
│   │   │   │   ├── model/                   # Request & Response DTO Models
│   │   │   │   ├── GitServicesAPI.java      # Primary REST API Controller
│   │   │   │   └── NnpGitServiceAPI.java    # Secondary/Scoped REST API Controller
│   │   │   ├── events/                      # Spring Event Listeners & EventBus Config
│   │   │   ├── exception/                   # Global Controller Advice & Error Handlers
│   │   │   ├── service/                     # Service Interfaces & Implementations
│   │   │   ├── utils/                       # Utility Helpers (Logging sanitization)
│   │   │   └── GitlabIntegrationApplication.java # Spring Boot Main Entry Point
│   │   └── resources/
│   │       ├── application.properties       # Base Application Configuration
│   │       ├── application-dev.properties   # Development Profile Properties
│   │       ├── application-local.properties # Local Profile Properties
│   │       ├── application-main.properties  # Main Profile Properties
│   │       └── hazelcast.yaml               # Hazelcast Declarative Config
│   └── test/                                # Automated Unit & Integration Tests
├── Dockerfile                               # Container Image Definition
├── pom.xml                                  # Project Object Model & Build Plugins
├── spotbugs-exclude.xml                     # SpotBugs SAST Filter Rules
├── README.md                                # Project Landing Page & Quick Start
├── USER_MANUAL_AND_DEPLOYMENT_GUIDE.md      # Comprehensive Operator & Deployment Manual
├── DEVELOPMENT_GUIDELINES.md                # Developer Guidelines & Contribution Standards
├── CONTRIBUTING.md                          # Open source contribution workflow
├── MAINTAINERS.md                           # Project maintainers and governance
├── CODE_OF_CONDUCT.md                       # Community code of conduct
├── SECURITY.md                              # Vulnerability reporting and policy
└── LICENSE                                  # Apache 2.0 Open Source License
```

---

## Security and Vulnerability Management

This project maintains a zero-tolerance policy for critical CVEs. To report security issues, please refer to [SECURITY.md](SECURITY.md) or contact **contribution@nubons.com**.

---

## Contributing

Contributions are welcome under the Apache 2.0 License. Please review [CONTRIBUTING.md](CONTRIBUTING.md) and [DEVELOPMENT_GUIDELINES.md](DEVELOPMENT_GUIDELINES.md) prior to submitting pull requests.

---

## License

This project is licensed under the [Apache License, Version 2.0](LICENSE).
