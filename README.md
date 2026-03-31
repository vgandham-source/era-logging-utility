# ERA Logging Utility

A Spring Boot library that provides structured JSON logging, HTTP request/response tracing, correlation ID propagation, sensitive data masking, and a custom AUDIT log level — designed for ingestion by centralized log aggregation platforms (Splunk, ELK, etc.).

---

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Quick Start](#quick-start)
- [Configuration](#configuration)
  - [Log4j2 Layout](#log4j2-layout)
  - [Service Metadata (Environment Variables)](#service-metadata-environment-variables)
  - [Dynamic Log Level Configuration](#dynamic-log-level-configuration)
  - [Async Request Dispatch Bypass](#async-request-dispatch-bypass)
- [HTTP Header Handling](#http-header-handling)
  - [Supported Headers](#supported-headers)
  - [Sensitive Data Masking](#sensitive-data-masking)
  - [Health Check Suppression](#health-check-suppression)
- [JSON Log Output Format](#json-log-output-format)
- [AUDIT Log Level](#audit-log-level)
- [Request / Response Logging](#request--response-logging)
- [Components Reference](#components-reference)

---

## Features

| Feature | Description |
|---|---|
| Structured JSON logs | Every log event serialized as a single-line JSON object |
| Correlation ID propagation | `x-wu-correlationId` header extracted and attached to all log events |
| HTTP header enrichment | Standard WU headers placed in Log4j2 ThreadContext automatically |
| Sensitive data masking | API keys masked in all log output (last 4 chars visible) |
| Request/Response logging | Full HTTP details logged at DEBUG; non-200 details logged at INFO |
| Custom AUDIT level | `AUDIT` log level (priority 250) for compliance audit trail events |
| Dynamic log levels | Configure log levels per-package via `application.properties` |
| Health check suppression | Missing correlation ID warnings skipped for health endpoints |
| Async request bypass | Async-dispatched requests skip wrapping to avoid stream conflicts |
| ThreadContext cleanup | Context cleared after every request — safe for thread-pool reuse |
| Service metadata | `SERVICEID`, `SERVICENAME`, `SERVICEBUILDID` included in every log event |

---

## Requirements

- Java 21+
- Spring Boot 3.5.x
- Log4j2 (the library excludes `spring-boot-starter-logging` — Logback must not be on the classpath)

---

## Installation

### Step 1 — Add the dependency

**Maven:**
```xml
<dependency>
    <groupId>com.wu.era.library</groupId>
    <artifactId>era-logging-utility</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Gradle:**
```groovy
implementation 'com.wu.era.library:era-logging-utility:1.0.0'
```

### Step 2 — Exclude Logback

Because the library uses Log4j2, Logback must be excluded from your project's Spring Boot starter:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
    <exclusions>
        <exclusion>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

### Step 3 — Add Log4j2 configuration

Create `src/main/resources/log4j2.xml` in your application:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<Configuration status="WARN" packages="com.wu.era.library.layout">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <SimpleJSONLayout/>
        </Console>
    </Appenders>
    <Loggers>
        <Root level="INFO">
            <AppenderRef ref="Console"/>
        </Root>
    </Loggers>
</Configuration>
```

> The `packages` attribute on `<Configuration>` is required so Log4j2 can discover the `SimpleJSONLayout` plugin.

That's it. Auto-configuration registers `AppRequestFilter` and `LogLevelConfig` automatically — no `@Import` or `@Bean` declarations needed.

---

## Quick Start

Once the dependency is added and Log4j2 is configured, every inbound HTTP request will:

1. Extract WU headers and populate the logging context
2. Emit a `WARN` if `x-wu-correlationId` is missing (non-health-check endpoints only)
3. Log the full request and response at `DEBUG` level
4. Log request details at `INFO` for any non-200 response
5. Clear the logging context after the request completes

Sample log output for a successful request:

```json
{"timestamp":"2026-03-30T10:15:42.123Z","level":"INFO","logger":"com.example.OrderService","thread":"http-nio-8080-exec-1","message":"Order created","context":{"crId":"abc-123","tenantId":"tenant-001","apiKey":"************5678","svrIp":"10.0.1.5","svrPort":"8080","svcId":"order-svc","svcNm":"Order Service","svcBld":"2.4.1"}}
```

---

## Configuration

### Log4j2 Layout

`SimpleJSONLayout` is a Log4j2 plugin registered under the name `SimpleJSONLayout`. Use it in any appender in your `log4j2.xml`:

```xml
<Console name="Console" target="SYSTEM_OUT">
    <SimpleJSONLayout/>
</Console>

<!-- or write to a rolling file -->
<RollingFile name="File" fileName="logs/app.log" filePattern="logs/app-%d{yyyy-MM-dd}.log.gz">
    <SimpleJSONLayout/>
    <Policies>
        <TimeBasedTriggeringPolicy/>
    </Policies>
</RollingFile>
```

### Service Metadata (Environment Variables)

Set these environment variables (or JVM system properties) to embed service identification in every log event:

| Environment Variable | JSON Key | Description |
|---|---|---|
| `SERVICEID` | `svcId` | Unique service identifier |
| `SERVICENAME` | `svcNm` | Human-readable service name |
| `SERVICEBUILDID` | `svcBld` | Build or version identifier |

**Example — Docker / Kubernetes:**
```yaml
env:
  - name: SERVICEID
    value: "order-service"
  - name: SERVICENAME
    value: "Order Service"
  - name: SERVICEBUILDID
    value: "2.4.1-abc123"
```

**Example — JVM flags:**
```
-DSERVICEID=order-service -DSERVICENAME="Order Service" -DSERVICEBUILDID=2.4.1
```

### Dynamic Log Level Configuration

Add `loglevel.*` properties to `application.properties` or `application.yml`. The library applies them as Log4j2 level overrides at startup — no restart required when using Spring Cloud Config or similar.

```properties
# application.properties
loglevel.root=INFO
loglevel.com.wu.era=DEBUG
loglevel.org.springframework.web=WARN
loglevel.com.example.payments=DEBUG
```

```yaml
# application.yml
loglevel:
  root: INFO
  com.wu.era: DEBUG
  org.springframework.web: WARN
  com.example.payments: DEBUG
```

Valid level values: `TRACE`, `DEBUG`, `INFO`, `WARN`, `ERROR`, `FATAL`, `OFF`, `AUDIT`.

### Async Request Dispatch Bypass

When an async dispatch is detected (`DispatcherType.ASYNC`), or the override flag is set, the filter passes through without wrapping the request/response — preventing stream conflicts in reactive or async controllers.

**To force bypass globally** (e.g., during migration or testing):

```bash
# Environment variable
ASYNC_REQUEST_DISPATCH=true

# Or JVM system property
-DASYNC_REQUEST_DISPATCH=true
```

---

## HTTP Header Handling

### Supported Headers

The filter reads the following headers from every non-async inbound request and places them into the Log4j2 `ThreadContext`, making them available in every log event emitted during that request:

| HTTP Header | ThreadContext Key | Transformation |
|---|---|---|
| `x-wu-correlationId` | `crId` | None |
| `x-wu-apiKey` | `apiKey` | Masked — all characters except last 4 replaced with `*` |
| `x-wu-tenantId` | `tenantId` | None |
| `x-wu-externalRefId` | `extRef` | None |
| `x-wu-authPrincipal` | `cIntId` | None |
| *(auto-detected)* | `svrIp` | `InetAddress.getLocalHost().getHostAddress()` |
| *(auto-detected)* | `svrPort` | `request.getLocalPort()` |

All context values are automatically cleared after each request (in a `finally` block) to prevent leakage across thread-pool reuse.

### Sensitive Data Masking

API keys are masked in **all** log output — both in the `ThreadContext` stored in JSON events and in any inline header dumps logged during debug/error flows.

**Masking rule:** All characters except the last 4 are replaced with `*`.

```
Input:  abcdef123456789
Output: ***********6789
```

```
Input:  1234  (4 chars or fewer)
Output: 1234  (unchanged)
```

### Health Check Suppression

The missing correlation ID warning (`WARN: No correlationId found in Header {uri}`) is suppressed for the following URI prefixes:

| URI Pattern |
|---|
| `/actuator/health` |
| `/actuator` |
| `/health` |
| `/ping` |

Requests to these endpoints pass through silently even without a correlation ID header.

---

## JSON Log Output Format

Each log event is a single-line JSON object with the following structure:

```json
{
  "timestamp": "2026-03-30T10:15:42.123Z",
  "level": "INFO",
  "logger": "com.example.OrderService",
  "thread": "http-nio-8080-exec-1",
  "message": "Order processed successfully",
  "context": {
    "crId": "d4f3a1b2-...",
    "apiKey": "************5678",
    "tenantId": "tenant-001",
    "extRef": "REF-9987",
    "cIntId": "user@example.com",
    "svrIp": "10.0.1.5",
    "svrPort": "8080",
    "svcId": "order-svc",
    "svcNm": "Order Service",
    "svcBld": "2.4.1"
  }
}
```

When an exception is logged, an `exception` object is added:

```json
{
  "timestamp": "2026-03-30T10:15:44.001Z",
  "level": "ERROR",
  "logger": "com.example.PaymentService",
  "thread": "http-nio-8080-exec-3",
  "message": "Payment processing failed",
  "exception": {
    "class": "java.lang.IllegalStateException",
    "message": "Insufficient funds"
  },
  "context": { ... }
}
```

**Field reference:**

| Field | Type | Description |
|---|---|---|
| `timestamp` | string | ISO-8601 UTC timestamp (`yyyy-MM-dd'T'HH:mm:ss.SSS'Z'`) |
| `level` | string | Log level name (`DEBUG`, `INFO`, `WARN`, `ERROR`, `FATAL`, `AUDIT`) |
| `logger` | string | Fully-qualified logger name |
| `thread` | string | Thread name |
| `message` | string | Formatted log message |
| `exception.class` | string | Exception class name (present only when an exception is logged) |
| `exception.message` | string | Exception message |
| `context.*` | object | ThreadContext values + service metadata |

---

## AUDIT Log Level

The library registers a custom `AUDIT` log level with numeric priority **250**, positioned between `FATAL` (100) and `INFO` (400) — ensuring audit events are always emitted unless the logger is configured at `FATAL` or `OFF`.

### Usage

```java
import com.wu.era.library.logger.WUCustomLogger;

public class PaymentService {

    // Obtain a WUCustomLogger instead of the standard Logger
    private static final WUCustomLogger log = WUCustomLogger.getLogger(PaymentService.class);

    public void processPayment(String transactionId, String userId) {
        // Standard log levels work as normal
        log.info("Processing payment for transaction {}", transactionId);

        // Use audit() for compliance/audit trail events
        log.audit("Payment authorised: transactionId={} userId={}", transactionId, userId);

        // With a Throwable
        try {
            // ...
        } catch (Exception e) {
            log.audit("Payment failed: transactionId=" + transactionId, e);
        }
    }
}
```

### API Reference

| Method | Description |
|---|---|
| `WUCustomLogger.getLogger(Class<?>)` | Create logger by class |
| `WUCustomLogger.getLogger(String)` | Create logger by name |
| `WUCustomLogger.getLogger(Logger)` | Wrap an existing Log4j2 logger |
| `log.audit(String message)` | Emit audit event |
| `log.audit(String message, Object... params)` | Emit audit event with parameters |
| `log.audit(String message, Throwable t)` | Emit audit event with exception |
| `log.audit(Marker marker, String message)` | Emit audit event with marker |
| `log.isAuditEnabled()` | Check if AUDIT level is enabled |

### Log4j2 Configuration for AUDIT

To capture AUDIT events to a dedicated appender:

```xml
<Configuration status="WARN" packages="com.wu.era.library.layout">
    <Appenders>
        <Console name="Console" target="SYSTEM_OUT">
            <SimpleJSONLayout/>
        </Console>
        <File name="AuditFile" fileName="logs/audit.log">
            <SimpleJSONLayout/>
            <LevelRangeFilter minLevel="AUDIT" maxLevel="AUDIT" onMatch="ACCEPT" onMismatch="DENY"/>
        </File>
    </Appenders>
    <Loggers>
        <Root level="INFO">
            <AppenderRef ref="Console"/>
            <AppenderRef ref="AuditFile"/>
        </Root>
    </Loggers>
</Configuration>
```

---

## Request / Response Logging

### DEBUG Level (FR-05)

When the root or package logger is at `DEBUG`, the filter logs full request and response details:

```
REQUEST:  method=POST uri=/api/v1/payments headers={x-wu-correlationId=abc-123, x-wu-tenantId=tenant-001, x-wu-apiKey=************5678} queryString=null body={"amount":100}
RESPONSE: status=201 body={"transactionId":"txn-999"}
```

### INFO Level for Non-200 Responses (FR-06)

Regardless of the configured log level, when the HTTP response status is not `200`, the filter logs the full request context at `INFO`:

```
Non-200 response [500] for POST /api/v1/payments headers={...} body={...}
```

This ensures error context is always captured even in production environments running at `WARN` or `INFO` log levels.

---

## Components Reference

| Class | Package | Purpose |
|---|---|---|
| `SimpleJSONLayout` | `com.wu.era.library.layout` | Log4j2 plugin — serializes log events as single-line JSON |
| `AppRequestFilter` | `com.wu.era.library.filter` | Servlet filter — header extraction, ThreadContext enrichment, request/response logging |
| `WuContentCachingResponseWrapper` | `com.wu.era.library.wrapper` | Response wrapper — buffers response body for logging without consuming the output stream |
| `WUCustomLogger` | `com.wu.era.library.logger` | Custom logger wrapper providing the `AUDIT` log level |
| `LogLevelConfig` | `com.wu.era.library.config` | Spring bean — applies `loglevel.*` properties as Log4j2 overrides at startup |
| `EraLoggingAutoConfiguration` | `com.wu.era.library.config` | Spring Boot auto-configuration — registers `AppRequestFilter` and `LogLevelConfig` |
