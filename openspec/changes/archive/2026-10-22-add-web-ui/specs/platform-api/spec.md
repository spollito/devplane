# Platform API

## ADDED Requirements

### Requirement: Requests from other sites can't change anything
The system SHALL reject, with 403 Forbidden and a problem details document, any `POST`, `PUT`, `PATCH`, or `DELETE` request under `/api/v1` that a browser sends from another site, and change nothing. A request comes from another site when its `Sec-Fetch-Site` header is `cross-site` or `same-site`. Without that header, it comes from another site when its `Origin` header isn't DevPlane's own address. Requests with neither header, such as from curl or scripts, are unaffected.

#### Scenario: Rejects a request from another site
- **GIVEN** `orders-api` is registered without a repository
- **WHEN** a client sends `POST /api/v1/services/orders-api/repository` without a body and with the `Sec-Fetch-Site` header `cross-site`
- **THEN** the response status is 403 Forbidden and the body is a problem details document
- **AND** nothing is sent to GitHub, and `orders-api` still has no repository

#### Scenario: Rejects another site's origin
- **WHEN** a client sends `POST /api/v1/services` with a valid body, the `Origin` header `https://example.com`, and no `Sec-Fetch-Site` header
- **THEN** the response status is 403 Forbidden, and no service is registered

#### Scenario: Accepts DevPlane's own pages and scripts
- **WHEN** a client sends `POST /api/v1/services` with a valid body and the `Sec-Fetch-Site` header `same-origin`, and another client registers a second service without any `Origin` or `Sec-Fetch-Site` header
- **THEN** both responses are 201 Created

#### Scenario: Reading is unaffected
- **GIVEN** `orders-api` is registered
- **WHEN** a client sends `GET /api/v1/services/orders-api` with the `Sec-Fetch-Site` header `cross-site`
- **THEN** the response status is 200 OK
