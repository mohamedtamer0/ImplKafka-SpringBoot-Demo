# Repository Technical Review

Review date: 2026-07-16

## Executive summary

The repository was a compact Spring Boot/Kafka example with working intent but several correctness and maintainability problems: incompatible serializer usage, unsafe JSON trust settings, no validation or API error contract, fire-and-forget publishes, no consumer recovery policy, a test that depended on a developer's Kafka broker, and documentation that did not match the project. The project has been modernized while preserving its original Java 17 baseline, topic names, consumer group, and legacy API routes.

The result separates text and JSON consumer configuration, safely selects producer serializers by value type, waits for broker acknowledgments, declares retry and dead-letter behavior, exposes lightweight Kafka health, and has deterministic unit and embedded-Kafka tests. Repository-level Docker, CI, dependency maintenance, formatting, configuration examples, and API examples were added.

## Audit scope

Every tracked source, test, configuration, wrapper, license, ignore, and documentation file was inspected, along with the repository tree, Git status, recent history, and dependency graph. No committed credentials or secrets were found. Ignored IDE state was not changed. The pre-existing executable-bit change on `mvnw` was preserved.

## Issues discovered and changes made

### Bugs and Kafka correctness

- The original producer was typed for strings while global configuration selected JSON serialization, and the JSON producer mixed a `User` value into that template. A type-delegating producer now handles `String`, `User`, and raw DLT bytes explicitly.
- Producer send futures were ignored, so the API could report success before Kafka rejected a record. Producers now wait for metadata or raise a domain exception that maps to HTTP 503.
- Kafka's synchronous metadata lookup could otherwise block an HTTP worker for its 60-second default. `max.block.ms` is bounded and synchronous Kafka client failures are mapped to the same HTTP 503 contract.
- Text messages had no key. They now receive UUID message IDs; user records use the user ID, preserving same-user partition ordering.
- Consumer auto-commit behavior and acknowledgment semantics were implicit. Auto-commit is disabled and record acknowledgment is explicit.
- There was no listener recovery path. `DefaultErrorHandler` now performs configurable fixed-backoff retries and `DeadLetterPublishingRecoverer` routes exhausted records to same-partition `.dlt` topics.
- Malformed JSON previously failed without a clear recovery contract. `ErrorHandlingDeserializer` makes deserialization and validation failures recoverable to the DLT.
- Topic names were scattered across older Spring properties. Validated `app.kafka` configuration properties now own names, group, concurrency, topology, timeouts, and retry settings.
- All four topics are individual `NewTopic` beans, with explicit partition and replication settings, so Spring's Kafka admin discovers them.
- The original test loaded Kafka clients pointing at `localhost:9092` and timed out when no broker existed. Unit tests disable listener/admin startup, while integration tests start an isolated KRaft broker on a random port.

### Spring Boot and API quality

- Field injection was replaced with constructor injection.
- Request validation was added for text and user payloads.
- A preferred POST text endpoint was added while retaining the original GET endpoint.
- The original JSON publish route remains and a clearer `/users` alias was added.
- A global controller advice returns Problem Details for invalid bodies, invalid parameters, malformed JSON, and broker failures without leaking parser internals.
- Producer and consumer logs now include message ID/key, topic, partition, and offset but omit complete payloads.
- Graceful shutdown and bounded Kafka admin/producer timeouts were configured.
- Actuator exposes only `health` and `info`; a Kafka health indicator reports broker availability.
- `User` remains a JavaBean instead of becoming a record to avoid breaking existing Java callers and its JSON shape. The new immutable text request is a record.

### Security improvements

- Removed `spring.json.trusted.packages=*`; JSON deserialization targets `User` and trusts only its exact package.
- Malformed request details are not returned to clients.
- Logs avoid complete message bodies and personal names.
- Environment-dependent configuration is externalized and `.env` is ignored.
- Compose binds host ports to loopback rather than every interface.
- Actuator exposure is restricted. No authentication layer was added because it would obscure the repository's Kafka teaching purpose.
- No hardcoded secrets or committed credentials were found.

## Dependency modernization

| Component | Before | After | Decision |
| --- | --- | --- | --- |
| Java target | 17 | 17 | Retained as the stable compatibility baseline |
| Spring Boot | 3.4.2 | 3.5.16 | Latest maintenance release in the 3.5 line; avoids a major Boot 4 migration |
| Spring Kafka | 3.3.2 | 3.3.16 | Managed by Spring Boot 3.5.16 |
| Kafka clients | 3.8.1 | 3.9.2 | Managed by Spring Boot and matched by local Docker Kafka |
| Maven | 3.9.9 | 3.9.9 | Current wrapper distribution retained |

Validation and Actuator starters were added. The optional configuration processor supports IDE metadata. DevTools was removed because it was unused and inappropriate for the normal runtime artifact. No direct versions were added where Spring Boot already manages compatibility.

Spring Boot 4 and Kafka 4 were intentionally not adopted: they are major-version changes with a wider Java/framework migration surface and no benefit required by this demo. That should be a separate owner-approved project.

## Tests added and improved

- Context/configuration test that does not require an external broker
- Producer unit tests for text keys, user keys, broker acknowledgment, and send failures
- MVC tests for both current and legacy routes, valid requests, validation, malformed JSON, and HTTP 503 mapping
- JSON serializer/deserializer round-trip test with restricted trusted packages
- Embedded KRaft integration tests for:
  - REST publish through actual Kafka consumption
  - Three-attempt listener retry behavior
  - Malformed JSON recovery to the user DLT with original bytes preserved
  - Kafka health through Actuator

Tests are split by Maven convention: `*Test` runs with Surefire and `*IT` runs with Failsafe during `verify`.

## Repository and documentation improvements

- Replaced stale `application.properties` with structured `application.yml` and a safe test profile.
- Added `.editorconfig`, improved `.gitattributes`, and expanded `.gitignore`.
- Added `.env.example` and `requests.http`.
- Added a non-root, multi-stage Java 17 `Dockerfile`.
- Added a single-node Apache Kafka 3.9.2 KRaft `compose.yaml` with a broker health check and an optional application profile.
- Added GitHub Actions CI on Java 17, Dependabot coverage for Maven/Docker/Actions, and a pull-request template.
- Rewrote the README with verified project behavior, architecture, message flow, configuration, endpoints, examples, test commands, DLT behavior, and troubleshooting.
- Corrected the documented license from MIT to the repository's actual Apache License 2.0.

## Files removed or replaced

- Removed `src/main/resources/application.properties`; its settings were replaced by `application.yml`.
- Removed no useful application behavior. Legacy routes and original default topic/group names remain available.

## Verification record

The following checks were performed during the review:

- Baseline `./mvnw test`: failed because the old context test attempted a real `localhost:9092` broker and older test tooling did not work correctly on the available JDK.
- Clean embedded-Kafka integration run: passed 4 tests against an actual in-process KRaft broker.
- Final `./mvnw --batch-mode --no-transfer-progress clean verify`: **BUILD SUCCESS** with 14 unit/MVC/configuration/serialization tests and 4 embedded-Kafka integration tests (18 total, no failures, errors, or skips).
- Resolved dependency check confirmed Spring Kafka 3.3.16, Kafka clients 3.9.2, and Mockito 5.17.0. Maven dependency analysis completed successfully; its warnings are expected false positives from starter/transitive APIs and were checked manually against source usage.
- The packaged JAR started on the configured HTTP port. With no broker, health returned 503/DOWN, invalid input returned 400 Problem Details, and publishing returned the bounded 503 contract after roughly five seconds without logging the payload.
- `docker compose config --quiet` passed against Docker 29.6.1 and Compose 5.3.0.
- Apache Kafka 3.9.2 was pulled and reached `healthy`; the documented topic-list command showed both application topics and both DLTs.
- The host-run application connected to Compose Kafka, returned health UP, published valid text and user records, logged both consumers receiving them, and rejected invalid input with 400.
- A deliberately malformed user record was routed to `kafkalearn_json.dlt`; the console consumer confirmed its original key/value and Kafka exception headers.
- The application image built successfully, started on Java 17 as the non-root `spring` user, connected to `kafka:19092`, returned health UP, and consumed a container-to-container message.
- The documented Compose down command stopped and removed the project containers and network cleanly.
- YAML, XML, shell syntax, and `git diff --check` validation passed.

## Remaining limitations

- The Docker environment is intentionally single-node, plaintext, and ephemeral with replication factor one.
- At-least-once delivery means duplicate consumer calls remain possible. Durable business-level idempotency requires a datastore and domain rules that are outside this demo.
- DLT records can be inspected but there is no replay or operator workflow.
- User JSON has no formal schema/version registry.
- The API waits synchronously for Kafka delivery. This gives callers an honest publish result but caps request throughput under broker latency.
- The Java package uses uppercase letters and underscores. Renaming it would affect imports and any external callers, so it was not changed.
- Local verification used a newer installed JDK while compiling with `--release 17`; Java 17 is used in CI to validate the supported runtime.

## Recommended future improvements

1. Add the locally verified Docker smoke scenario as a separate CI job if runner time and image-download cost are acceptable.
2. Define a DLT ownership, alerting, retention, and replay policy before production use.
3. Add durable consumer idempotency only when a real processing datastore is introduced.
4. Add TLS/SASL, authorization, secrets management, and multiple brokers for any shared environment.
5. Introduce schema evolution tooling if messages are consumed by independent applications.
6. Consider OpenAPI and trace propagation if the demo grows beyond its current focused scope.

## Decisions requiring repository-owner approval

- Remove the legacy GET and JSON `/publish` routes after a deprecation period.
- Rename the nonstandard Java package and artifact casing.
- Migrate to Spring Boot 4/Java 21+ as a separate compatibility change.
- Select production broker count, security protocol, retention, and replication policies.
- Choose business-level duplicate detection and DLT replay semantics once real processing is added.
