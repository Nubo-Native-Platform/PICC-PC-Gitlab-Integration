# User Manual and Deployment Guide: `PICC-PC-Gitlab-Integration`

This document provides a comprehensive operational and deployment manual for the **`PICC-PC-Gitlab-Integration`** microservice within the **Nubo Native Platform (NNP)**. It covers configuration, operational workflows, containerization, production deployment on Kubernetes, and troubleshooting.

---

## Table of Contents

1. [Service Architecture & Role](#1-service-architecture--role)
2. [Prerequisites & System Requirements](#2-prerequisites--system-requirements)
3. [Configuration Reference & Profiles](#3-configuration-reference--profiles)
   - [Application Properties Matrix](#application-properties-matrix)
   - [Configuration Profiles](#configuration-profiles)
   - [Centralized Config Server Integration](#centralized-config-server-integration)
4. [Functional Operations & Service Workflows](#4-functional-operations--service-workflows)
   - [User Management](#user-management)
   - [Group & Subgroup Governance](#group--subgroup-governance)
   - [Project Provisioning](#project-provisioning)
   - [Personal Access Token (PAT) Lifecycle](#personal-access-token-pat-lifecycle)
   - [In-Memory Caching & Event-Driven Invalidation](#in-memory-caching--event-driven-invalidation)
   - [REST API Endpoints Reference](#rest-api-endpoints-reference)
5. [Local Build & Containerization](#5-local-build--containerization)
   - [Local Build with Maven](#local-build-with-maven)
   - [Docker Container Build & Execution](#docker-container-build--execution)
6. [Production Deployment & CI/CD](#6-production-deployment--cicd)
   - [Kubernetes Deployment & Pod Isolation](#kubernetes-deployment--pod-isolation)
   - [Automated Deployment Pipelines](#automated-deployment-pipelines)
7. [Troubleshooting & Frequently Asked Questions](#7-troubleshooting--frequently-asked-questions)

---

## 1. Service Architecture & Role

`PICC-PC-Gitlab-Integration` functions as the integration gateway between internal microservices/platforms and the **GitLab REST API v4**.

```mermaid
graph TD
    classDef client fill:#0284C7,stroke:#38BDF8,stroke-width:2px,color:#FFFFFF;
    classDef app fill:#1E293B,stroke:#64748B,stroke-width:2px,color:#F8FAFC;
    classDef comp fill:#0F172A,stroke:#38BDF8,stroke-width:1px,color:#E2E8F0;
    classDef ext fill:#D97706,stroke:#F59E0B,stroke-width:2px,color:#FFFFFF;

    Clients["<b>Downstream Microservices & Platforms</b><br/>JSON REST Requests"]:::client

    subgraph ServiceLayer ["<b>PICC-PC-Gitlab-Integration Engine</b>"]
        Controller["<b>REST API Gateway (Port 8080)</b><br/>GitServicesAPI | NnpGitServiceAPI"]:::comp
        Svc["<b>Service Logic</b><br/>GitLabService | NnpGitLabService"]:::comp
        Cache["<b>Hazelcast In-Memory Grid (127.0.0.1:5701)</b><br/>all-group-members Cache Map"]:::comp
        Events["<b>Spring Event Multicaster</b><br/>CacheRefreshEvent Consumer"]:::comp
        Feign["<b>Spring Cloud OpenFeign Clients</b><br/>GitLabClient | NnpGitlabClient<br/>GitLabClientErrorDecoder"]:::comp
    end

    GitLab["<b>GitLab Instance (REST API v4)</b><br/>https://gitlab.example.com/api/v4"]:::ext

    Clients --> Controller
    Controller --> Svc
    Svc --> Cache
    Svc --> Events
    Events --> Cache
    Svc --> Feign
    Feign --> GitLab
```

---

## 2. Prerequisites & System Requirements

| Requirement | Minimum Version | Recommended Version | Details |
| :--- | :--- | :--- | :--- |
| **Java Development Kit** | JDK 21 | Eclipse Temurin 21 (LTS) | Baseline runtime target |
| **Build Tool** | Maven 3.9.0+ | Maven 3.9.6+ | Maven Wrapper (`./mvnw`) included |
| **Parent Platform** | `abstract-nnp:1.0.0` | `abstract-nnp:1.0.0` | BOM dependency management |
| **GitLab Instance** | GitLab CE/EE 15.0+ | GitLab CE/EE 16.x / 17.x | Compatible with REST API v4 |
| **GitLab API Token** | Admin or Scoped PAT | Scopes: `api`, `read_user`, `read_api`, `read_repository`, `write_repository` | Required for administrative calls |
| **Container Engine** | Docker 20.10+ | Docker Engine 24+ / Podman | For containerized execution |
| **Memory / CPU** | 512 MB RAM / 0.5 CPU | 1 GB RAM / 1.0 CPU | Typical production footprint |

---

## 3. Configuration Reference & Profiles

### Application Properties Matrix

The service is configured via `src/main/resources/application.properties` or environment variables:

| Property Key | Environment Variable | Default Value | Description |
| :--- | :--- | :--- | :--- |
| `server.port` | `SERVER_PORT` | `8080` | Microservice HTTP server port |
| `spring.application.name` | - | `gitlab-int` | Application identification name |
| `spring.profiles.active` | `SPRING_PROFILES_ACTIVE` | `main` | Active Spring environment profile |
| `feign.name` | `FEIGN_NAME` | `gitlab-client` | Primary Feign client registration name |
| `feign.url` | `FEIGN_URL` | `https://gitlab.example.com` | Base URL of target GitLab instance |
| `feign.url.api` | `FEIGN_URL_API` | `/api/v4` | GitLab API endpoint prefix |
| `feign.apiToken` | `FEIGN_APITOKEN` | - | Primary GitLab Admin / Personal Access Token |
| `nnp.feign.name` | `NNP_FEIGN_NAME` | `nnp-gitlab-client` | Secondary/NNP Feign client registration name |
| `nnp.feign.url` | `NNP_FEIGN_URL` | `https://gitlab.example.com` | Base URL for secondary GitLab instance |
| `nnp.feign.apiToken`| `NNP_FEIGN_APITOKEN` | - | Secondary GitLab Access Token |
| `hazelcast.port` | `HAZELCAST_PORT` | `5701` | Dedicated Hazelcast loopback listener port |
| `cache.refresh.cron.expression` | `CACHE_REFRESH_CRON` | `0 0 * * * *` | Hourly scheduled cache refresh cron |

---

### Configuration Profiles

The application includes pre-configured profile templates:

- **`application-local.properties`**: For local workstation development.
- **`application-dev.properties`**: For development cluster integration.
- **`application-main.properties`**: For staging/production enterprise deployment.

To specify a profile at runtime:
```bash
java -jar -Dspring.profiles.active=dev target/gitlab-integration-0.0.1-SNAPSHOT.jar
```

---

### Centralized Config Server Integration

`PICC-PC-Gitlab-Integration` supports Spring Cloud Config Server dynamically:
```properties
spring.application.name=gitlab-int
spring.cloud.config.label=v1
spring.config.import=optional:configserver:${CONFIG_SERVER_URL:http://config-server:8080}/nnp-config
```

> [!NOTE]
> Setting `optional:configserver:...` ensures that if a Config Server is absent during local testing, the application falls back safely to local `application.properties`.

---

## 4. Functional Operations & Service Workflows

### User Management

The `GitLabService` exposes programmatic user management methods with built-in security policies:

- **User Provisioning (`createUser`)**:
  - Automatically marks provisioned users as `external = true` to restrict view access to unauthorized parent groups.
  - Sets `projects_limit = 0` to ensure projects can only be created within designated group hierarchies.
  - Sets `can_create_group = false` to enforce enterprise group naming governance.
- **User Discovery (`getUser`)**: Queries users by username.
- **Hard Deletion (`deleteUser`)**: Permanently purges user accounts when decommissioned.

---

### Group & Subgroup Governance

- **Group Provisioning (`createGroup`)**:
  - Automatically resolves normalized paths (e.g., lowercase URL-safe slug).
  - Configures `project_creation_level` and `subgroup_creation_level`.
  - Supports nesting under existing parent groups using `parent_id`.
- **Member Association (`associateUserWithGroup`)**:
  - Assigns users to groups with fine-grained access levels (Guest: `10`, Reporter: `20`, Developer: `30`, Maintainer: `40`, Owner: `50`).
- **Group Hierarchy Queries (`getGroupsByUser`, `getAllGroups`, `getGroupByGroupName`)**:
  - Evaluates user membership across top-level and subgroup hierarchies with cache optimization.

---

### Project Provisioning

- **Repository Provisioning (`createProject`)**:
  - Automatically associates new repositories with a parent group or subgroup `namespace_id`.
  - Automatically initializes repositories with a default `README.md` (`initialize_with_readme=true`).
  - Supports visibility configuration (`private`, `internal`, `public`).

---

### Personal Access Token (PAT) Lifecycle

Enables dynamic service account credential provisioning:

- **Generate Token (`createToken`)**:
  - Creates tokens with standard enterprise scopes: `api`, `read_user`, `read_api`, `read_repository`, `write_repository`, `read_registry`, `write_registry`.
  - Naming pattern: `<username>_token` or `<environment>_token`.
- **Retrieve Active Token (`getToken`)**: Discovers currently active tokens for a specific user ID.
- **Revoke Token (`revokeToken`)**: Instantly revokes personal access tokens when rotating credentials or offboarding.

---

### In-Memory Caching & Event-Driven Invalidation

To eliminate redundant REST API roundtrips during group permission evaluation, `PICC-PC-Gitlab-Integration` integrates an embedded **Hazelcast 5.5** in-memory data grid:

1. **Warmup (`CacheService.initCache`)**:
   - Queries all groups and subgroups recursively.
   - Populates the `all-group-members` distributed map with JSON serialized member lists.
2. **Lookup (`CacheService.getAllGroupMembers`)**:
   - Retrieves members directly from RAM in sub-millisecond latency.
3. **Event-Driven Refresh (`EventListener`)**:
   - Listens for Spring Application `CacheRefreshEvent`.
   - Triggers asynchronous re-indexing of group memberships.

### Multi-Tier Environment Setup (`/api/createEnvironment`)

The `/api/createEnvironment` endpoint orchestrates a full automated multi-tier workspace layout in a single atomic operation:

```mermaid
sequenceDiagram
    autonumber
    participant C as Client
    participant API as GitServicesAPI
    participant GL as GitLab REST API v4
    participant HZ as Hazelcast Cache

    C->>API: POST /api/createEnvironment (GitEnvironment JSON)
    API->>GL: 1. Create Root Group (envName, envPath)
    API->>GL: 2. Create GitLab User (userName, email, password)
    API->>GL: 3. Associate User to Root Group (Maintainer - 40)
    API->>GL: 4. Generate Personal Access Token (PAT)
    API->>GL: 5. Create Subgroups (gitops, 3pp-comp, {envName}-code)
    API->>GL: 6. Create ArgoCD Subgroups (infra-ops, deployments)
    API->>GL: 7. Create GitOps Repositories (gitops-sync-repo, git-infra-comp-apps-repo)
    API->>GL: 8. Create Sync Project ({envName}-synch in env-replication-gitops)
    API->>HZ: 9. Publish CacheRefreshEvent & Re-index Hierarchy
    API-->>C: Return 200 OK with Generated PAT String
```

#### Request Payload (`application/json`):
```json
{
  "email": "operator@example.com",
  "name": "Platform Operator",
  "userName": "platform_operator",
  "password": "SecurePassword#2026",
  "envName": "staging-us-east",
  "envPath": "staging-us-east"
}
```

#### Example `curl` Invocation:
```bash
curl -X POST "http://localhost:8080/api/createEnvironment" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "operator@example.com",
    "name": "Platform Operator",
    "userName": "platform_operator",
    "password": "SecurePassword#2026",
    "envName": "staging-us-east",
    "envPath": "staging-us-east"
  }'
```

#### Successful Response:
```
glpat-xYz9876543210AbCdEfGhIjKlMnOpQrStUv
```

---

### REST API Endpoints Reference

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

## 5. Local Build & Containerization

### Local Build with Maven

```bash
# Clone the repository
git clone https://github.com/Nubo-Native-Platform/PICC-PC-Gitlab-Integration.git
cd PICC-PC-Gitlab-Integration

# Validate POM and configurations
./mvnw validate

# Compile, run tests, and package executable JAR
./mvnw clean package

# Run locally with Spring Boot
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The application will start on `http://localhost:8080`.

---

### Docker Container Build & Execution

#### 1. Containerfile / Dockerfile Reference

```dockerfile
FROM eclipse-temurin:21-jre-jammy AS runtime

WORKDIR /app

# Create a non-root system user for security
RUN groupadd -r appgroup && useradd -r -g appgroup -u 1001 appuser

COPY target/gitlab-integration-0.0.1-SNAPSHOT.jar app.jar

USER appuser

EXPOSE 8080 5701

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
```

#### 2. Building & Running the Image

```bash
# Build Docker image
docker build -t picc-pc-gitlab-integration:latest .

# Run container with environment variables
docker run -d \
  --name gitlab-integration \
  -p 8080:8080 \
  -e SPRING_PROFILES_ACTIVE=main \
  -e FEIGN_URL=https://gitlab.example.com \
  -e FEIGN_APITOKEN=glpat-yourAdminTokenHere \
  picc-pc-gitlab-integration:latest
```

#### 3. Running with Docker Compose

An open-source ready `docker-compose.yml` is included in the project root:

```bash
# 1. Copy the environment configuration template
cp .env.example .env

# 2. Configure your FEIGN_URL and FEIGN_APITOKEN in .env
nano .env

# 3. Build and start the service with Docker Compose
docker compose up -d --build

# 4. View application logs
docker compose logs -f gitlab-integration
```

---

## 6. Production Deployment & CI/CD

### Kubernetes Deployment & Pod Isolation

`PICC-PC-Gitlab-Integration` includes special network configuration to prevent Hazelcast port collisions. In Kubernetes environments, Hazelcast is bound strictly to `127.0.0.1` on port `5701` with multicast and auto-discovery disabled.

#### Kubernetes Deployment Manifest (`deployment.yaml`)

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: gitlab-integration
  namespace: platform-services
  labels:
    app: gitlab-integration
spec:
  replicas: 2
  selector:
    matchLabels:
      app: gitlab-integration
  template:
    metadata:
      labels:
        app: gitlab-integration
    spec:
      containers:
        - name: gitlab-integration
          image: picc-pc-gitlab-integration:latest # Built locally via Dockerfile, or replace with your private registry image
          imagePullPolicy: IfNotPresent
          ports:
            - containerPort: 8080
              name: http
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: "main"
            - name: FEIGN_URL
              value: "https://gitlab.example.com"
            - name: FEIGN_APITOKEN
              valueFrom:
                secretKeyRef:
                  name: gitlab-credentials
                  key: api-token
          resources:
            requests:
              memory: "512Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
          readinessProbe:
            httpGet:
              path: /actuator/health/readiness
              port: 8080
            initialDelaySeconds: 20
            periodSeconds: 10
          livenessProbe:
            httpGet:
              path: /actuator/health/liveness
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 15
---
apiVersion: v1
kind: Service
metadata:
  name: gitlab-integration-service
  namespace: platform-services
spec:
  selector:
    app: gitlab-integration
  ports:
    - protocol: TCP
      port: 8080
      targetPort: 8080
      name: http
  type: ClusterIP
```

---

### Automated Deployment Pipelines

#### GitHub Actions (`.github/workflows/ci-cd.yml`)

The repository includes a GitHub Actions CI/CD workflow that validates builds, runs security scans, and publishes releases:
- Validates Maven POM and executes tests.
- Executes **SpotBugs** SAST analysis.
- Scans dependencies with **OWASP Dependency-Check**.
- Generates CNCF-compliant **CycloneDX** SBOM.
- Deploys packages to GitHub Packages registry on `main` release tags.

---

## 7. Troubleshooting & Frequently Asked Questions

### Q1: Hazelcast logs `Unknown protocol: HTT` or connection resets on port 8080.
**Cause**: When Hazelcast auto-discovery and multicast are enabled in containerized environments, Hazelcast may attempt to bind dynamically across ports or listen on `0.0.0.0`, intercepting HTTP ingress traffic.  
**Resolution**: Ensure `HazelcastConfiguration.java` is active. It explicitly pins Hazelcast to `127.0.0.1:5701`, disables multicast/Kubernetes discovery, and turns off port auto-increment.

---

### Q2: Feign client throws `401 Unauthorized`.
**Cause**: The configured `feign.apiToken` is missing, expired, or invalid.  
**Resolution**:
1. Verify the value of `feign.apiToken` in `application.properties` or your environment secret.
2. In GitLab, navigate to **User Settings > Access Tokens** and confirm that the token is active and has the `api` scope.

---

### Q3: Feign client throws `403 Forbidden` on user/group operations.
**Cause**: The token belongs to a standard user without GitLab Administrator privileges.  
**Resolution**: Administrative endpoints (such as `POST /api/v4/users` or `POST /api/v4/users/{id}/personal_access_tokens`) require GitLab Administrator privileges. Ensure the token is generated by an instance administrator.

---

### Q4: Group member list in `getGroupsByUser` is outdated or stale.
**Cause**: Group members were modified directly in GitLab without notifying the cache.  
**Resolution**: Trigger a cache refresh by publishing a `CacheRefreshEvent` via the internal Spring EventBus, or call `CacheService.initCache()` or the `/api/refreshCache` endpoint to re-index all groups.

---

*For developer contribution instructions and coding standards, refer to [DEVELOPMENT_GUIDELINES.md](DEVELOPMENT_GUIDELINES.md).*
