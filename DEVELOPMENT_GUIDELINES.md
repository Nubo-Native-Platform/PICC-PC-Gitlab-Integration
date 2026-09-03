# Development Guidelines and Contribution Standards: `PICC-PC-Gitlab-Integration`

This document defines the architectural standards, development workflows, coding conventions, and security requirements for contributors to **`PICC-PC-Gitlab-Integration`**.

---

## Table of Contents

1. [Architecture & Design Principles](#1-architecture--design-principles)
2. [Development Environment Setup](#2-development-environment-setup)
3. [Package Structure & Code Navigation](#3-package-structure--code-navigation)
4. [Coding Standards & Best Practices](#4-coding-standards--best-practices)
   - [Spring Cloud OpenFeign Conventions](#spring-cloud-openfeign-conventions)
   - [Hazelcast Cache Isolation & Bean Lifecycle](#hazelcast-cache-isolation--bean-lifecycle)
   - [Exception Handling & Error Decoding](#exception-handling--error-decoding)
   - [Logging & Sensitive Data Masking](#logging--sensitive-data-masking)
5. [Security, Code Quality & Compliance Tooling](#5-security-code-quality--compliance-tooling)
   - [SAST: SpotBugs & FindSecBugs](#sast-spotbugs--findsecbugs)
   - [SCA: OWASP Dependency-Check](#sca-owasp-dependency-check)
   - [SBOM: CycloneDX Aggregate Generation](#sbom-cyclonedx-aggregate-generation)
   - [Checkstyle: Google Java Style](#checkstyle-google-java-style)
6. [Git Workflow & Branching Strategy](#6-git-workflow--branching-strategy)
   - [Branch Naming Conventions](#branch-naming-conventions)
   - [Conventional Commits](#conventional-commits)
7. [Pull Request (PR) Checklist](#7-pull-request-pr-checklist)
8. [Release Lifecycle & Versioning](#8-release-lifecycle--versioning)

---

## 1. Architecture & Design Principles

`PICC-PC-Gitlab-Integration` follows clean architecture and domain-driven design principles for integration microservices:

1. **Declarative Integration**: All outbound HTTP communication with GitLab REST API v4 is encapsulated in declarative **Spring Cloud OpenFeign** clients. No manual `RestTemplate` or `HttpClient` plumbing should be used.
2. **Deterministic Caching**: Cache expensive hierarchical operations (e.g., recursive group membership resolution) in RAM using **Hazelcast 5.5**, ensuring zero remote network roundtrips during hot evaluation paths.
3. **Loopback Cache Isolation**: The embedded cache must operate strictly on the loopback network (`127.0.0.1:5701`) with multicast and discovery disabled to prevent port collisions with inbound HTTP services.
4. **Resilient Error Translation**: Upstream HTTP status codes and error payloads from GitLab must be decoded via `GitLabClientErrorDecoder` into explicit Java exception types with actionable messages.
5. **Zero-Trust Token Management**: Tokens and credentials must never be written to persistent logs or returned in non-sanitized error payloads.

---

## 2. Development Environment Setup

### Required Tools
- **JDK 21** (Eclipse Temurin 21 or OpenJDK 21 LTS).
- **Maven 3.9+** (or use `./mvnw`).
- **IDE**: IntelliJ IDEA, VS Code, or Eclipse with:
  - **Lombok Plugin** installed and *Annotation Processing* enabled.
  - **Google Java Format** plugin recommended.

### IDE Annotation Processing Setup
In IntelliJ IDEA:
- Navigate to **Settings > Build, Execution, Deployment > Compiler > Annotation Processors**.
- Check **Enable annotation processing**.

---

## 3. Package Structure & Code Navigation

```
src/main/java/com/numbons/gitlabintegration/
├── api/
│   ├── client/
│   │   ├── GitLabClient.java              # Primary OpenFeign client for GitLab API v4
│   │   └── NnpGitlabClient.java           # Secondary/Scoped GitLab Feign client
│   ├── configuration/
│   │   ├── GitLabClientConfiguration.java # RequestInterceptor (Header token injection)
│   │   ├── GitLabClientErrorDecoder.java  # Custom Feign HTTP Error Decoder
│   │   ├── HazelcastConfiguration.java    # Standalone Hazelcast Config bean (127.0.0.1:5701)
│   │   ├── CacheConfiguration.java        # Spring Cache scheduling configuration
│   │   └── NnpGitLabClientConfiguration.java # Secondary client configuration
│   ├── exception/
│   │   ├── GitLabAPIException.java        # Feign communication runtime exception with status
│   │   └── GitLabAPIExceptionMessage.java # Structured error message DTO
│   ├── model/
│   │   ├── GitAccessToken.java            # Personal Access Token model
│   │   ├── GitEnvironment.java            # Environment creation orchestration model
│   │   ├── GitGroup.java                  # Group creation payload DTO
│   │   ├── GitProject.java                # Project creation payload DTO
│   │   ├── GitUser.java                   # User creation payload DTO
│   │   ├── Group.java                     # GitLab Group domain model
│   │   ├── MemberGroupAssociation.java    # Membership assignment model
│   │   ├── Project.java                   # GitLab Project domain model
│   │   └── User.java                      # GitLab User domain model
│   ├── GitServicesAPI.java                # Primary REST API Controller
│   └── NnpGitServiceAPI.java              # Secondary REST API Controller
├── events/
│   ├── CacheRefreshEvent.java             # Spring application cache invalidation event
│   ├── EventBusConfiguration.java         # Event multicaster / executor configuration
│   └── EventListener.java                 # Cache invalidation event consumer
├── exception/
│   ├── IntegrationException.java          # General service exception
│   ├── IntegrationExceptionHandler.java   # Centralized @RestControllerAdvice
│   └── IntegrationExceptionMessage.java   # Standard JSON error response model
├── service/
│   ├── CacheService.java                  # Caching interface definition
│   ├── GitLabService.java                 # Primary business logic interface
│   ├── NnpGitLabService.java              # Secondary business logic interface
│   └── impl/
│       ├── CacheServiceImpl.java          # In-memory Hazelcast cache operations
│       ├── GitLabServiceImpl.java         # GitLab service implementation
│       └── NnpGitLabServiceImpl.java      # Secondary service implementation
├── utils/
│   └── LogUtils.java                      # Logging sanitization utilities
└── GitlabIntegrationApplication.java      # Application entry point
```

---

## 4. Coding Standards & Best Practices

### Spring Cloud OpenFeign Conventions

- Declare all endpoints using standard Spring MVC annotations (`@GetMapping`, `@PostMapping`, `@DeleteMapping`).
- Explicitly configure the client with a dedicated configuration class:
  ```java
  @FeignClient(
      name = "${feign.name}",
      url = "${feign.url}${feign.url.api}",
      configuration = GitLabClientConfiguration.class
  )
  public interface GitLabClient {
      @PostMapping(value = "/users", consumes = MediaType.APPLICATION_JSON_VALUE)
      ResponseEntity<User> createUser(Map<String, ?> queryMap);
  }
  ```
- Always inject authentication headers via `RequestInterceptor`:
  ```java
  @Bean
  public RequestInterceptor requestInterceptor() {
      return requestTemplate -> requestTemplate.header("PRIVATE-TOKEN", apiToken);
  }
  ```

---

### Hazelcast Cache Isolation & Bean Lifecycle

> [!IMPORTANT]
> **Preventing Circular Dependencies**:  
> `HazelcastConfiguration` creates the `Config` bean, from which `HazelcastInstance` is constructed. `CacheServiceImpl` depends on `HazelcastInstance`. Therefore, **`HazelcastConfiguration` must NEVER depend on `CacheService` directly or transitively**.

**Network Configuration Rules**:
1. Multicast discovery must be disabled: `joinConfig.getMulticastConfig().setEnabled(false)`.
2. Kubernetes discovery must be disabled: `joinConfig.getKubernetesConfig().setEnabled(false)`.
3. Auto-detection must be disabled: `joinConfig.getAutoDetectionConfig().setEnabled(false)`.
4. Interface must bind strictly to loopback: `networkConfig.getInterfaces().setEnabled(true).addInterface("127.0.0.1")`.
5. Port auto-increment must be disabled: `networkConfig.setPortAutoIncrement(false)` so that collisions fail fast.

---

### Exception Handling & Error Decoding

- Feign client responses with status codes `>= 400` are intercepted by `GitLabClientErrorDecoder`.
- Translate status codes to meaningful domain messages:
  - `400 Bad Request`: Validation failure.
  - `401 Unauthorized`: Token invalid or expired.
  - `403 Forbidden`: Insufficient permissions (requires admin).
  - `404 Not Found`: Resource not found.
  - `409 Conflict`: Resource already exists.
- Centralize all REST API responses via `IntegrationExceptionHandler` (`@RestControllerAdvice`).

---

### Logging & Sensitive Data Masking

- Use **Log4j2** via `@Slf4j` annotations.
- Use parameterized logging (`log.info("Provisioned user ID: {}", userId)`) rather than string concatenation.
- **Never** log sensitive credentials such as `feign.apiToken`, passwords, or private access tokens:
  ```java
  // INCORRECT:
  log.info("Token generated: {}", token.getToken());

  // CORRECT:
  log.info("Token generated for user ID: {} with token ID: {}", user.getId(), token.getId());
  ```

---

## 5. Security, Code Quality & Compliance Tooling

This project enforces automated security and compliance gates in `pom.xml`:

### SAST: SpotBugs & FindSecBugs
Static Analysis Security Testing is executed with maximum effort and low threshold:
```bash
# Run SpotBugs analysis
./mvnw spotbugs:check
```
*Note: False positives or intentional patterns can be safely excluded in [spotbugs-exclude.xml](spotbugs-exclude.xml).*

---

### SCA: OWASP Dependency-Check
Scans all runtime and compile dependencies against the National Vulnerability Database (NVD):
```bash
# Run dependency vulnerability scan
./mvnw dependency-check:check
```
*The build will fail if any dependency contains a CVSS score >= 7.0 (High/Critical).*

---

### SBOM: CycloneDX Aggregate Generation
Generates a CNCF-compliant Software Bill of Materials in JSON format:
```bash
# Generate SBOM (saved to target/bom.json)
./mvnw cyclonedx:makeAggregateBom
```

---

### Checkstyle: Google Java Style
Ensures uniform formatting and code style across the codebase:
```bash
# Run Checkstyle verification
./mvnw checkstyle:check
```

---

## 6. Git Workflow & Branching Strategy

We follow the standard GitHub Flow model:

```
main (stable releases & tags)
  ^
  | Pull Request
feature/* | fix/* | sec/* | chore/* (development branches)
```

### Branch Naming Conventions
- `feature/<description>`: New functional capabilities (e.g., `feature/group-avatar-upload`)
- `fix/<issue>`: Bug fixes (e.g., `fix/hazelcast-timeout`)
- `sec/<cve-or-issue>`: Security updates (e.g., `sec/patch-feign-cve`)
- `chore/<task>`: Build, CI/CD, or documentation updates (e.g., `chore/update-readme`)

### Conventional Commits
Format: `<type>(<scope>): <short summary>`

Types:
- `feat`: A new feature or capability.
- `fix`: A bug fix.
- `docs`: Documentation changes only.
- `refactor`: Code change that neither fixes a bug nor adds a feature.
- `sec`: Security patch or CVE remediation.
- `test`: Adding or correcting tests.
- `chore`: Build tools, dependencies, or configuration changes.

Examples:
- `feat(client): add personal access token revocation endpoint`
- `fix(hazelcast): bind cache strictly to 127.0.0.1 loopback interface`
- `docs(manual): add Kubernetes deployment and port isolation guide`

---

## 7. Pull Request (PR) Checklist

Before submitting a Pull Request, verify the following:

- [ ] **Build Validation**: `./mvnw clean package` compiles cleanly.
- [ ] **SAST Verification**: `./mvnw spotbugs:check` passes without errors.
- [ ] **Vulnerability Free**: `./mvnw dependency-check:check` shows zero CVSS >= 7.0 issues.
- [ ] **No Secrets**: No hardcoded API tokens, credentials, or private URLs in code or commits.
- [ ] **Documentation**: Updated `README.md` or `USER_MANUAL_AND_DEPLOYMENT_GUIDE.md` if APIs or properties changed.
- [ ] **Commit Messages**: Follow Conventional Commits format.

---

## 8. Release Lifecycle & Versioning

This project adheres to [Semantic Versioning 2.0.0](https://semver.org/):

- **`MAJOR` (e.g., 1.0.0)**: Incompatible API breaks, Java or Spring Boot major version migrations.
- **`MINOR` (e.g., 0.1.0)**: Backwards-compatible new features, new Feign endpoints.
- **`PATCH` (e.g., 0.0.2)**: Backwards-compatible bug fixes and security patches.

To release a new version:
1. Update `<version>` in `pom.xml`.
2. Commit and tag:
   ```bash
   git commit -am "chore(release): bump version to 0.1.0"
   git tag -a v0.1.0 -m "Release version 0.1.0"
   git push origin v0.1.0
   ```
