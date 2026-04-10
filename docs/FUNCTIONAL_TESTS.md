# Functional API тесты

Модуль `functional-tests` содержит внешние API-тесты, которые выполняют HTTP-запросы в поднятое тестовое окружение.

Тесты не используют UI и не поднимают Spring-контекст внутри JVM. Это проверка реальных API-сценариев между сервисами.

## Что нужно поднять

Перед запуском тестов нужно поднять тестовый стенд из корня проекта:

```bash
docker compose -f docker-compose.test.yml up --build
```

Стенд включает:

- `postgres`
- `discovery-service`
- `api-gateway`
- `auth-service`
- `tariff-service`
- `order-service`
- `service-request-service`

## Куда идут запросы

По умолчанию functional-тесты идут напрямую в сервисы:

```text
auth-service            http://localhost:18081
tariff-service          http://localhost:18082
order-service           http://localhost:18083
service-request-service http://localhost:18084
```

## Что покрыто

- `AuthApiFunctionalTest`
  - регистрация клиента;
  - логин клиента;
  - негативный логин с неверным паролем.
- `AdminOperationsApiFunctionalTest`
  - просмотр админом заявок;
  - смена статуса заявки на подключение;
  - создание/редактирование/архивация тарифа;
  - просмотр audit log по тарифам и сервисным заявкам.
- `OrderFlowApiFunctionalTest`
  - создание тарифа админом;
  - создание заявки клиентом;
  - перевод заявки в `IN_PROGRESS` и `ACTIVE`;
  - проверка появления клиентского тарифа.
- `ServiceRequestFlowApiFunctionalTest`
  - создание сервисной заявки клиентом;
  - просмотр заявки админом;
  - смена статуса админом;
  - проверка нового статуса у клиента.

## Библиотеки

- JUnit 5 (`org.junit.jupiter`)
- Rest Assured (`io.rest-assured`)
- AssertJ (`org.assertj`)
- JJWT (`io.jsonwebtoken`) для генерации test JWT в admin-сценариях

## Запуск

Из корня проекта:

```bash
mvn -pl functional-tests test
```

## Переопределение адресов

Если стенд поднят на других портах:

```bash
mvn -pl functional-tests test ^
  -Dauth.baseUrl=http://localhost:18081 ^
  -Dtariff.baseUrl=http://localhost:18082 ^
  -Dorder.baseUrl=http://localhost:18083 ^
  -DserviceRequest.baseUrl=http://localhost:18084
```

## Важно по admin-сценариям

В functional-тестах admin JWT генерируется прямо в тестовом коде с тем же `JWT_SECRET`, что и в тестовом окружении.
Это сделано, чтобы тесты не зависели от заранее созданного admin-пользователя и его пароля.
