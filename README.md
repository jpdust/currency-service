# Currency Service

A Spring Boot 4 microservice that fetches live USD exchange rates from [allratestoday.com](https://allratestoday.com) and exposes them via a single REST endpoint. Responses are cache-annotated for distribution through an Amazon CloudFront edge network, keeping upstream API calls to a minimum.

---

## Table of Contents

- [Architecture](#architecture)
- [Technical Specifications](#technical-specifications)
- [Project Structure](#project-structure)
- [Configuration Reference](#configuration-reference)
- [Build & Run](#build--run)
- [API Reference](#api-reference)
- [Testing](#testing)
- [AWS Infrastructure](#aws-infrastructure)
- [Security Notes](#security-notes)

---

## Architecture

```
Browser / API Consumer
        │
        ▼
┌───────────────────┐
│  CloudFront Edge  │  Cache-Control: public, max-age=300
│  (CDN / Cache)    │  Min TTL: 300s  │  Max TTL: 86400s
└────────┬──────────┘
         │ cache miss only
         ▼
┌───────────────────┐
│   API Gateway     │  HTTPS origin
└────────┬──────────┘
         │
         ▼
┌─────────────────────────────────────────────┐
│           Currency Service (Spring Boot)     │
│                                             │
│  GET /api/rates                             │
│       │                                     │
│  CurrencyController                         │
│       │  sets Cache-Control header          │
│  CurrencyService                            │
│       │  RestClient                         │
│       ▼                                     │
│  allratestoday.com/api/v1/rates?source=USD  │
└─────────────────────────────────────────────┘
```

**Request flow:**

1. A consumer requests `GET /api/rates` through the CloudFront distribution.
2. CloudFront serves a cached response if one exists and is within its TTL.
3. On a cache miss, CloudFront forwards the request through API Gateway to the Spring Boot service.
4. The service calls the upstream currency API, attaches a `Cache-Control: public, max-age=300` header, and returns the response.
5. CloudFront caches the response at the edge for subsequent requests.

---

## Technical Specifications

| Component | Technology |
|---|---|
| Language | Java 25 |
| Framework | Spring Boot 4.0.6 |
| Web layer | Spring MVC (servlet-based) |
| HTTP client | `RestClient` (Spring Framework 7) |
| JSON | Jackson 3 (`tools.jackson.core:jackson-databind`) |
| Build tool | Gradle 9.5 (Kotlin DSL) |
| Test framework | JUnit 5, Mockito, WireMock 3.9.1 |
| Test client | `RestTestClient` (Spring Framework 7) |
| Observability | Spring Boot Actuator (`/actuator/health`, `/actuator/info`) |
| CDN | Amazon CloudFront |
| Infrastructure-as-code | AWS CloudFormation |
| Upstream API | [allratestoday.com](https://allratestoday.com) |

---

## Project Structure

```
currency-service/
├── build.gradle.kts                        # Build definition and dependencies
├── local.properties                        # Local dev secrets (git-ignored)
├── local.properties.example                # Template — copy and fill in
├── infrastructure/
│   └── cloudfront.yaml                     # CloudFormation template
└── src/
    ├── main/
    │   ├── java/com/unstampedpages/currencyservice/
    │   │   ├── CurrencyServiceApplication.java     # Entry point
    │   │   ├── config/
    │   │   │   ├── CurrencyApiProperties.java       # Typed @ConfigurationProperties
    │   │   │   └── RestClientConfig.java            # RestClient bean
    │   │   ├── controller/
    │   │   │   └── CurrencyController.java          # GET /api/rates
    │   │   ├── exception/
    │   │   │   ├── ExternalApiException.java        # Upstream HTTP error
    │   │   │   └── GlobalExceptionHandler.java      # @RestControllerAdvice → ProblemDetail
    │   │   ├── model/
    │   │   │   └── CurrencyRatesResponse.java       # API response record
    │   │   └── service/
    │   │       └── CurrencyService.java             # Upstream HTTP call logic
    │   └── resources/
    │       └── application.properties
    └── test/
        └── java/com/unstampedpages/currencyservice/
            ├── CurrencyServiceApplicationTests.java         # Context smoke test
            ├── controller/
            │   └── CurrencyControllerTest.java              # Controller unit tests
            ├── exception/
            │   └── GlobalExceptionHandlerTest.java          # Exception handler tests
            ├── integration/
            │   └── CurrencyRatesIntegrationTest.java        # Full-stack integration tests
            └── service/
                └── CurrencyServiceTest.java                 # Service unit tests
```

---

## Configuration Reference

All properties are under the `currency.api` prefix.

| Property | Default | Description |
|---|---|---|
| `currency.api.base-url` | `https://allratestoday.com` | Base URL of the upstream API |
| `currency.api.path` | `/api/v1/rates` | Path appended to the base URL |
| `currency.api.source` | `USD` | Base currency for exchange rates |
| `currency.api.cache-max-age` | `300` | Seconds written into `Cache-Control: max-age` |
| `currency.api.key` | *(required)* | API key for allratestoday.com |

### Supplying the API key

**Production** — set the `CURRENCY_API_KEY` environment variable. Spring's relaxed binding maps it to `currency.api.key` automatically.

**Local development** — copy `local.properties.example` to `local.properties` and fill in the key:

```bash
cp local.properties.example local.properties
# Edit local.properties and set currency.api.key=<your key>
```

`local.properties` is git-ignored and loaded automatically via `spring.config.import=optional:file:local.properties`.

---

## Build & Run

### Prerequisites

- Java 25 (`java -version`)
- No other tools required — the Gradle wrapper (`./gradlew`) is included

### Run locally

```bash
./gradlew bootRun
```

The service starts on **http://localhost:8080**.

### Build a deployable JAR

```bash
./gradlew bootJar
```

The fat JAR is written to `build/libs/currency-service-0.0.1-SNAPSHOT.jar`.

### Run the JAR (production-style)

```bash
CURRENCY_API_KEY=your_key java -jar build/libs/currency-service-0.0.1-SNAPSHOT.jar
```

### Run tests

```bash
./gradlew test
```

HTML test report: `build/reports/tests/test/index.html`

---

## API Reference

### `GET /api/rates`

Returns the current USD exchange rates from the upstream provider.

**Response headers**

| Header | Value |
|---|---|
| `Content-Type` | `application/json` |
| `Cache-Control` | `public, max-age=300` |

**Success response — `200 OK`**

```json
{
  "success": true,
  "source": "USD",
  "date": "2024-11-15",
  "rates": {
    "EUR": 0.9235,
    "GBP": 0.7885,
    "JPY": 154.32,
    "CAD": 1.3956
  }
}
```

**Error response — `503 Service Unavailable`**

Returned when the upstream API is unreachable or responds with an error. Body conforms to [RFC 9457 Problem Detail](https://www.rfc-editor.org/rfc/rfc9457).

```json
{
  "type": "urn:currency-service:upstream-error",
  "title": "Upstream API Error",
  "status": 503,
  "detail": "The currency rates provider returned an error. Please retry shortly."
}
```

**Network error — `503 Service Unavailable`**

```json
{
  "type": "urn:currency-service:network-error",
  "title": "Upstream Unreachable",
  "status": 503,
  "detail": "Unable to connect to the currency rates provider. Please retry shortly."
}
```

### `GET /actuator/health`

Spring Boot Actuator health check. Returns `{"status":"UP"}` when the service is running.

---

## Testing

The test suite has **22 tests across 5 classes** with no external dependencies at test time — all upstream HTTP calls are intercepted by WireMock.

| Class | Strategy | Tests |
|---|---|---|
| `CurrencyServiceApplicationTests` | `@SpringBootTest` — context smoke test | 1 |
| `CurrencyControllerTest` | `@SpringBootTest(MOCK)` + `RestTestClient` + `@MockitoBean` | 3 |
| `GlobalExceptionHandlerTest` | `@SpringBootTest(MOCK)` + `RestTestClient` + `@MockitoBean` | 10 |
| `CurrencyServiceTest` | No Spring context — real `RestClient` + `WireMockServer` | 4 |
| `CurrencyRatesIntegrationTest` | `@SpringBootTest(RANDOM_PORT)` + WireMock + `RestTestClient` | 4 |

**Test layers explained:**

- **Unit (controller + exception handler):** The web layer starts with a mock servlet environment. `CurrencyService` is replaced by a Mockito mock, so tests exercise only the controller and exception-handling pipeline.
- **Unit (service):** A `WireMockServer` is started on a random port without loading Spring. A real `RestClient` is pointed at it, so the test exercises the actual HTTP round-trip and deserialization without any Spring overhead.
- **Integration:** A full Spring Boot server starts on a random port. `@DynamicPropertySource` redirects `currency.api.base-url` to WireMock before the context initialises, so all outbound calls are intercepted. `RestTestClient.bindToServer()` drives assertions over real HTTP.

---

## AWS Infrastructure

The CloudFormation template at `infrastructure/cloudfront.yaml` provisions:

- **CloudFront distribution** — HTTPS-only, HTTP/2+3, Brotli + gzip compression
- **Cache policy** — `MinTTL=300s`, `DefaultTTL=300s`, `MaxTTL=86400s`; path-only cache key (no query strings, cookies, or headers in key)
- **Origin request policy** — forwards only the `Host` header to API Gateway

### Deploy

```bash
aws cloudformation deploy \
  --template-file infrastructure/cloudfront.yaml \
  --stack-name currency-service-cdn \
  --parameter-overrides \
      ApiGatewayDomainName=abc123.execute-api.us-east-1.amazonaws.com \
      ApiGatewayStageName=prod
```

### Parameters

| Parameter | Required | Default | Description |
|---|---|---|---|
| `ApiGatewayDomainName` | Yes | — | API Gateway execute-API hostname |
| `ApiGatewayStageName` | No | `prod` | Stage name used as origin path prefix |
| `PriceClass` | No | `PriceClass_100` | Edge location coverage (100 = NA + EU) |
| `LogBucketName` | No | *(empty)* | S3 bucket for access logs; blank disables logging |

### Outputs

| Output | Description |
|---|---|
| `DistributionId` | Use with `aws cloudfront create-invalidation` |
| `DistributionDomainName` | CNAME target for your custom domain in Route 53 |
| `CurrencyRatesEndpoint` | Full CloudFront URL to `GET /api/rates` |

### Invalidating the cache

```bash
aws cloudfront create-invalidation \
  --distribution-id <DistributionId> \
  --paths "/api/rates"
```

---

## Security Notes

- **The API key is never committed to source control.** It is supplied via the `CURRENCY_API_KEY` environment variable in production and `local.properties` locally. Both mechanisms are handled transparently by Spring's property resolution.
- `local.properties` is listed in `.gitignore`. `local.properties.example` (with a placeholder value) is committed instead.
- The CloudFront distribution enforces HTTPS for all viewer connections (`redirect-to-https`) and communicates with the API Gateway origin over TLS 1.2+ only.
- The cache key excludes query strings and cookies, preventing cache-poisoning via crafted query parameters.
