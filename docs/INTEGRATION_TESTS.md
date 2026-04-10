# Интеграционные тесты

## Назначение

Интеграционные тесты проверяют работу приложения на уровне Spring-контекста, HTTP-слоя, security и persistence с реальной PostgreSQL в контейнере.

## Библиотеки

Используются:

- Spring Boot Test (`@SpringBootTest`, `@AutoConfigureMockMvc`)
- MockMvc для HTTP-вызовов контроллеров
- Spring Security Test (`jwt()` post-processor)
- Testcontainers (`org.testcontainers:junit-jupiter`, `org.testcontainers:postgresql`)
- JUnit 5
- AssertJ

## Общий подход

- Каждый сервис поднимает `PostgreSQLContainer` (`postgres:16-alpine`).
- Через `@DynamicPropertySource` в рантайме подставляются `spring.datasource.*`.
- Discovery/Eureka отключаются в тестах:
  - `eureka.client.enabled=false`
  - `spring.cloud.discovery.enabled=false`
- Для защищённых endpoint используются JWT в тестах:
  - либо реальный login (auth-service),
  - либо `spring-security-test` с `jwt()` и ролями.

## Тестовые сценарии

### Auth Service

Файл: `auth-service/src/test/java/com/masunya/auth/AuthServiceIntegrationTest.java`

- `registerShouldPersistClientUserAndReturnJwt`
  - `POST /auth/register`
  - проверка, что вернулся JWT
  - проверка, что пользователь сохранён с ролью `CLIENT` и пароль захеширован
- `loginShouldReturnJwtAndCreateAdminAuditLogForAdmin`
  - `POST /auth/login` для admin-пользователя
  - проверка токена и записи `ADMIN_LOGIN` в audit log
- `adminAuditLogsEndpointShouldRequireAdminJwt`
  - `GET /admin/audit-logs` без токена -> `401`
  - с admin JWT -> `200` и массив

### Tariff Service

Файл: `tariff-service/src/test/java/com/masunya/tariff/TariffServiceIntegrationTest.java`

- `publicListShouldReturnOnlyNonArchivedTariffs`
  - архивированный тариф не должен попадать в `GET /tariffs`
- `createShouldPersistTariffAndWriteAuditLog`
  - `POST /tariffs` (admin)
  - проверка сохранения полей в БД
  - проверка записи `TARIFF_CREATED` в audit log
- `archiveShouldHideTariffFromPublicListButKeepItInAdminList`
  - после архивации тариф скрыт из публичного списка
  - но виден в `GET /tariffs/admin` для админа

### Order Service

Файл: `order-service/src/test/java/com/masunya/order/OrderServiceIntegrationTest.java`

- `clientShouldCreateOrderAndSeeItInMyOrders`
  - клиент создаёт заявку `POST /orders`
  - проверка trim полей и появления в `GET /orders/my`
- `adminShouldActivateOrderCreateClientTariffAndWriteAuditLog`
  - админ переводит заявку в `IN_PROGRESS` и `ACTIVE`
  - проверка создания клиентского тарифа в БД
  - проверка аудита `ORDER_STATUS_CHANGED`
- `adminEndpointsShouldRequireAdminRole`
  - client на admin endpoint получает `403`
  - admin получает `200`

### Service Request Service

Файл: `service-request-service/src/test/java/com/masunya/servicerequest/ServiceRequestIntegrationTest.java`

- `clientShouldCreateRequestAndSeeItInMyList`
  - создание сервисной заявки клиентом
  - проверка trim полей и доступности в `GET /service-requests/my`
- `adminShouldUpdateStatusAndWriteAuditLog`
  - смена статуса админом через `PATCH /service-requests/{id}/status`
  - проверка записи `SERVICE_REQUEST_STATUS_CHANGED` в audit log
- `adminEndpointsShouldRequireAdminRole`
  - client на admin endpoint получает `403`
  - admin получает `200`

## Запуск

Все тесты проекта:

```bash
mvn test
```

Интеграционные тесты конкретного сервиса (пример):

```bash
mvn -pl order-service test
```
