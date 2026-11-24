# NPP Payments Notification stub

A Spring Boot 4.1 (Java 21) stub of the Cuscal **NPP Payments Notification** webhook API. It implements every
operation in the OpenAPI spec and returns valid sample responses. Each call is logged when it
arrives and when its response goes out, **without the request or response body**.

The spec is kept at [`src/main/resources/openapi/npp-payments-notification.yaml`](src/main/resources/openapi/npp-payments-notification.yaml).
The file supplied is `info.version: 4.0.3`, which includes all v4.0.2 changes.

## Make Payment with status callback (npp_payments.yaml)

`POST /{orgId}/payments/{paymentId}` is the NPP Payments API **Make Payment** call
([`npp_payments.yaml`](src/main/resources/openapi/npp_payments.yaml)). The stub:

1. answers **202 Accepted with no body** straight away, echoing `x-traceId` (the spec's only success response);
2. **3 seconds later** sends `POST http://localhost:4002/{orgId}/payments/{paymentId}/status` with the
   same `x-traceId` (a new UUID if none came in) and a `PaymentStatusReportResponse1` body:

```json
{"transaction_status_information":{
  "original_transaction_identification":"HBSLAU4TXXXN20261109000000000000123",
  "transaction_status":"ACSC",
  "acceptance_date_time":"2026-11-24T22:31:51.303Z",
  "instructed_amount":"6.36",
  "posting_flag":"PSTY",
  "service_level":"sct.05",
  "settlement_method_identification":"OFFU"}}
```

`instructed_amount` and `service_level` are copied from the Make Payment body's `payment_information`
when present (the body is still never logged); otherwise the configured defaults are used.
`transaction_status_reason_code` is added only when the status is `RJCT` or `PDNG`.
The body validates against `PaymentStatusReportResponse1` in the notification spec.

The log shows the whole flow, without bodies:

```
>>> INCOMING POST /PCCNAU53XXX/payments/test1234 traceId=abc contentType=application/json contentLength=16 from=127.0.0.1
... SCHEDULED status callback for PCCNAU53XXX/test1234 in 3000 ms traceId=abc
<<< RESPONSE POST /PCCNAU53XXX/payments/test1234 status=202 traceId=abc durationMs=12
>>> OUTGOING POST http://localhost:4002/PCCNAU53XXX/payments/test1234/status traceId=abc (payment status callback)
<<< OUTGOING RESPONSE POST http://localhost:4002/PCCNAU53XXX/payments/test1234/status status=200 traceId=abc durationMs=56
```

If nothing is listening on 4002 you get `<<< OUTGOING FAILED ... error=...` instead; the stub keeps running.

| Property | Default |
|---|---|
| `stub.status-callback.enabled` | `true` |
| `stub.status-callback.base-url` | `http://localhost:4002` |
| `stub.status-callback.delay-ms` | `3000` |
| `stub.status-callback.transaction-status` | `ACSC` (e.g. `RJCT` to simulate a rejection) |
| `stub.status-callback.reason-code` | `AC07` (sent only with `RJCT` / `PDNG`) |
| `stub.status-callback.instructed-amount` | `6.36` (fallback when the request has none) |
| `stub.status-callback.service-level` | `sct.05` (fallback when the request has none) |
| `stub.status-callback.posting-flag` | `PSTY` (`PSTG`, `PSTY` or `PSTD`) |
| `stub.status-callback.settlement-method` | `OFFU` (`OFFU`, `CCOM` or `ONUS`) |

Example: `java -jar target\npp-payments-notification-stub-1.0.0.jar --stub.status-callback.base-url=http://localhost:5000`

Note this path is the same as the notification API's Receive Payment, so that webhook now also
gets 202 with no body and triggers the callback. Every other operation in `npp_payments.yaml`
is answered by the catch-all with 200 `{}` and logged as `UNMAPPED`.

## Endpoints

All operations are `POST`, take an `x-traceId` header, and accept a JSON body that the stub does not inspect.

| Operation | Path | 200 response |
|---|---|---|
| Receive Payment / Make Payment | `/{orgId}/payments/{paymentId}` | 202, empty, then status callback (see above) |
| Receive IFTI Payment and IFTI Clearing Requests | `/{orgId}/payments/ifti/{paymentId}` | `{}` |
| Receive RTRF | `/{orgId}/requests/returns/{paymentId}` | `{}` |
| Receive Payment Status | `/{orgId}/payments/{paymentId}/status` | `{}` |
| Receive Payment Initiation Status (MAPAIN) | `/{orgId}/instructions/{pmtIdInstrId}/status` | `{}` |
| Receive Payment Initiation (RAPAIN) | `/{orgId}/instructions/{pmtIdInstrId}` | `{"reason_code":"ACCP"}` |

Errors use the spec's `Error1` shape: `{"error_code":"CUS.API.400","error_message":"..."}`.
The `x-traceId` received is echoed back as a response header.

### 200-only mode (default)

`stub.always-ok` is `true` by default, so **every endpoint returns 200** with its success body
whatever the request looks like. The `x-stub-response-status` header and the `x-traceId` check are ignored.
`x-stub-reason-code` and `x-stub-delay-ms` still apply.

Any other method or path (for example a `GET` on `/{orgId}/payments/{paymentId}`, or a URL with an
extra base path) also gets 200 `{}` in this mode, and is flagged in the log as
`!!! UNMAPPED GET /PCCNAU53XXX/payments/test1234 ...`. With the switch off, those get 404.

To get error responses back, start the stub with the switch off:

```bat
java -jar target\npp-payments-notification-stub-1.0.0.jar --stub.always-ok=false
```

or set the environment variable `STUB_ALWAYS_OK=false`, or change it in `application.yml`.
With it off, a call without `x-traceId` gets a 400, because the spec marks it required. Set
`stub.require-trace-id=false` to accept such calls.

## Logging

Every call produces two lines, on the console and in `logs/npp-stub.log` (rolled daily or at 10 MB, kept 14 days):

```
2026-11-18T01:46:51.703Z INFO  [http-nio-8080-exec-2] [traceId=abc-123] npp.calls - >>> INCOMING POST /CUSCAU2SXXX/instructions/X1 traceId=abc-123 contentType=application/json contentLength=14 from=127.0.0.1
2026-11-18T01:46:51.716Z INFO  [http-nio-8080-exec-2] [traceId=abc-123] npp.calls - <<< RESPONSE POST /CUSCAU2SXXX/instructions/X1 status=200 traceId=abc-123 durationMs=13
```

Only metadata is logged: method, path, trace id, content type and length, client address, status
and duration. Bodies are never read into the log (a test checks this).

## Steering responses (test headers)

These request headers are not part of the Cuscal spec. They let you test the caller's handling of other outcomes.

| Header | Effect |
|---|---|
| `x-stub-response-status: 503` | Only when `stub.always-ok=false`. Return that status with an `Error1` body. Allowed: 200, 400, 401, 404, 429, 500, 503, 504. |
| `x-stub-reason-code: AC05` | RAPAIN only: return this `reason_code` instead of the default. |
| `x-stub-delay-ms: 5000` | Wait this long before responding (capped at 120000). |

## Configuration

`src/main/resources/application.yml`, or override on the command line, e.g. `--server.port=9090`.

| Property | Default | Meaning |
|---|---|---|
| `server.port` | `8080` | HTTP port |
| `server.servlet.context-path` | empty | Base path, e.g. `/b2b/npp/payments/v4`, if your registered webhook URL has one |
| `stub.always-ok` | `true` | Return 200 for every call, ignoring error overrides and the trace id check |
| `stub.rapain-reason-code` | `ACCP` | Default RAPAIN `reason_code` |
| `stub.require-trace-id` | `true` | With `always-ok` off, reject calls without `x-traceId` |
| `logging.file.name` | `logs/npp-stub.log` | Log file |

Health check: `GET /actuator/health`.

## Prebuilt jar

A ready-to-run jar is committed at `target/npp-payments-notification-stub-1.0.0.jar`
(built with Java 21, Spring Boot 4.1.2). With Java 21 installed you can run it without building:

```bat
cd C:\workspace\stub-sig
java -jar target\npp-payments-notification-stub-1.0.0.jar
```

Rebuilding with `mvnw.cmd clean package` overwrites it.

## Run on Windows (C:\workspace)

Needs Java 21 or later (`java -version`). Maven is not needed, the wrapper downloads it.

```bat
cd C:\workspace
git clone https://github.com/suhas-mohamedali/stub-sig.git
cd stub-sig
mvnw.cmd spring-boot:run
```

Or build a jar and run it:

```bat
mvnw.cmd clean package
java -jar target\npp-payments-notification-stub-1.0.0.jar
```

On Linux or macOS use `./mvnw` instead of `mvnw.cmd`.

## Try it

```bash
curl -i -X POST http://localhost:8080/CUSCAU2SXXX/payments/BANKNTSTXXXN20180501000000000000010 \
  -H "x-traceId: 5323b85f-a7cb-49ba-acfb-03abc7e07fb0" -H "Content-Type: application/json" -d "{}"

curl -i -X POST http://localhost:8080/CUSCAU2SXXX/instructions/INSTR0001 \
  -H "x-traceId: 5323b85f-a7cb-49ba-acfb-03abc7e07fb0" -H "x-stub-reason-code: AM04" \
  -H "Content-Type: application/json" -d "{}"
```

PowerShell:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/CUSCAU2SXXX/payments/P1 `
  -Headers @{ "x-traceId" = "5323b85f-a7cb-49ba-acfb-03abc7e07fb0" } -ContentType "application/json" -Body "{}"
```

## Tests

```bat
mvnw.cmd test
```
