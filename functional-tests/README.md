# Функциональные API-тесты

Этот модуль содержит функциональные тесты, которые ходят по HTTP в поднятое тестовое окружение.

Тесты не используют UI и не поднимают Spring-контекст внутри JVM. Это именно внешняя проверка API-сценариев:
- регистрация и логин клиента,
- создание тарифа администратором,
- создание и активация заявки на подключение,
- создание и обработка сервисной заявки.

## Что нужно поднять

Перед запуском тестов нужно поднять тестовый стенд из корня проекта:

```bash
docker compose -f docker-compose.test.yml up --build
```

Этот compose-файл поднимает:
- `postgres`
- `discovery-service`
- `api-gateway`
- `auth-service`
- `tariff-service`
- `order-service`
- `service-request-service`

## Куда идут запросы

По умолчанию функциональные тесты ходят напрямую в сервисы:

```text
auth-service            http://localhost:18081
tariff-service          http://localhost:18082
order-service           http://localhost:18083
service-request-service http://localhost:18084
```

Такой вариант проще и стабильнее для functional tests, чем ходить через gateway с discovery-префиксами.

Примеры запросов:

```text
POST http://localhost:18081/auth/login
POST http://localhost:18082/tariffs
POST http://localhost:18083/orders
POST http://localhost:18084/service-requests
```

## Что уже покрыто

Сейчас в модуле есть:
- `AuthApiFunctionalTest`:
  - регистрация клиента
  - логин клиента
  - негативный логин с неверным паролем
- `AdminOperationsApiFunctionalTest`:
  - просмотр заявок администратором
  - смена статуса заявки на подключение
  - создание, редактирование и архивирование тарифа
  - просмотр audit log для тарифов и сервисных заявок
- `OrderFlowApiFunctionalTest`:
  - создание тарифа админом
  - создание заявки клиентом
  - перевод заявки в `IN_PROGRESS` и `ACTIVE`
  - проверка появления клиентского тарифа
- `ServiceRequestFlowApiFunctionalTest`:
  - создание сервисной заявки клиентом
  - просмотр заявки админом
  - смена статуса админом
  - проверка нового статуса у клиента

## Запуск тестов

Из корня проекта:

```bash
mvn -pl functional-tests test
```

## Переопределение адресов

Если тестовое окружение поднято не на стандартных портах, адреса можно передать через system properties:

```bash
mvn -pl functional-tests test ^
  -Dauth.baseUrl=http://localhost:18081 ^
  -Dtariff.baseUrl=http://localhost:18082 ^
  -Dorder.baseUrl=http://localhost:18083 ^
  -DserviceRequest.baseUrl=http://localhost:18084
```

## Важный момент по admin-сценариям

Для admin-запросов в functional tests JWT генерируется прямо в тестах с тем же `JWT_SECRET`, что и в тестовом окружении.

Это сделано специально, чтобы:
- не зависеть от заранее созданного admin-пользователя,
- не зависеть от его пароля,
- держать functional tests самодостаточными.

Из-за этого админские functional tests сейчас проверяют реальные admin API-операции, но не проверяют полноценный сценарий `login -> получить admin JWT` через `auth-service`.
