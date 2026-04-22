# bank-account-reputation-third-party-cache

A small backend service that stores and retrieves encrypted third-party cache entries for BARS (Bank Account Reputation Service) confirmation-of-payee flows.

## What it does

- Stores encrypted payloads by encrypted key
- Retrieves encrypted payloads by encrypted key
- Maintains separate caches for:
  - `confirmation-of-payee/business`
  - `confirmation-of-payee/personal`

## API

All endpoints are `POST` and expect/return JSON.

### Authentication

Endpoints are protected with HTTP Basic auth:

- Username: `bars`
- Password: value of `basicAuth.token` (default in `conf/application.conf` is `token` for local dev)

### Routes

- `POST /confirmation-of-payee/business/store`
- `POST /confirmation-of-payee/business/retrieve`
- `POST /confirmation-of-payee/personal/store`
- `POST /confirmation-of-payee/personal/retrieve`

### Request/response shapes

Store request:

```json
{
  "encryptedKey": "<string>",
  "encryptedData": "<string>"
}
```

Store response:

```json
{
  "stored": true,
  "description": null
}
```

Retrieve request:

```json
{
  "encryptedKey": "<string>"
}
```

Retrieve response:

```json
{
  "encryptedData": "<string|null>",
  "description": "<string|null>"
}
```

## Unit testing
To run the unit tests for the application, use the following command:

```sbt test ```


## Integration testing
The integration tests depends on external services such as a Postgres database and AWS Secrets Manager. These tests
make use of TestContainers to spin up the required dependencies in Docker containers.

To run the integration tests, use the following command:

```sbt it/test```

## Code coverage

```sbt clean coverage test it/test coverageReport```

## Running locally
To run the service locally, you can use the following command:

```sbt run```

By default it runs on port `9899`.

## Local configuration

Defaults in `conf/application.conf` include:

- `mongodb.uri = mongodb://localhost:27017/bank-account-reputation-third-party-cache`
- `mongodb.payee.personal.cacheItemExpiryDays = 40`
- `mongodb.payee.business.cacheItemExpiryDays = 120`
- `basicAuth.token = token`

## License

This code is open source software licensed under the [Apache 2.0 License](http://www.apache.org/licenses/LICENSE-2.0.html).

