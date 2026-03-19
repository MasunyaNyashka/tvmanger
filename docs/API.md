# Справочник API

## Базовые URL

API можно вызывать:
- напрямую по портам сервисов (удобно для локальной разработки),
- или через API Gateway (`http://localhost:8080`) с префиксом service-id при включенном роутинге через discovery.

Прямые адреса сервисов:
- Auth: `http://localhost:8081`
- Tariff: `http://localhost:8082`
- Order: `http://localhost:8083`
- Service Request: `http://localhost:8084`

## Авторизация

- Регистрация и вход доступны без токена.
- Для защищенных эндпоинтов нужен заголовок:

```http
Authorization: Bearer <JWT>
```

JWT выдается `auth-service`.

---

## Auth Service (`/auth`)

### `POST /auth/register`
Регистрация нового пользователя с ролью `CLIENT`.

Тело запроса:
```json
{
  "username": "client1",
  "password": "secret123"
}
```

### `POST /auth/login`
Вход и получение JWT.

Тело запроса:
```json
{
  "username": "client1",
  "password": "secret123"
}
```

Ответ:
```json
{
  "token": "eyJ..."
}
```

### `GET /admin/audit-logs?limit=200` (ADMIN)
Получение аудита админских входов.

---

## Tariff Service (`/tariffs`)

### Публичные эндпоинты
- `GET /tariffs`
- `GET /tariffs/{id}`

### Админские эндпоинты
- `GET /tariffs/admin`
- `POST /tariffs`
- `PUT /tariffs/{id}`
- `PATCH /tariffs/{id}/archive`
- `PATCH /tariffs/{id}/unarchive`
- `DELETE /tariffs/{id}`
- `GET /admin/audit-logs?limit=200`

Тело запроса для создания/обновления тарифа:
```json
{
  "name": "Premium TV",
  "price": 499.99,
  "connectionType": "CABLE",
  "description": "Пакет с лучшими каналами",
  "channels": ["HBO", "Discovery", "National Geographic"]
}
```

---

## Order Service (`/orders`, `/client-tariffs`)

### Клиентские заявки на подключение
- `POST /orders`
- `GET /orders/my`
- `GET /orders/my/{id}`

Тело запроса на создание заявки:
```json
{
  "tariffId": "00000000-0000-0000-0000-000000000000",
  "fullName": "Иван Иванов",
  "address": "Москва, Тверская 1",
  "phone": "+79991234567"
}
```

### Админские эндпоинты по заявкам
- `GET /orders/admin?status=SUBMITTED&dateFrom=2026-03-01&dateTo=2026-03-31`
- `PATCH /orders/{id}/status`
- `GET /admin/audit-logs?limit=200`

Тело запроса на смену статуса:
```json
{
  "status": "IN_PROGRESS",
  "adminComment": "Назначен техник"
}
```

### Клиентские тарифы
- `POST /client-tariffs/admin` (ADMIN)
- `GET /client-tariffs/my` (CLIENT)
- `GET /client-tariffs/admin?userId={uuid}` (ADMIN)
- `PATCH /client-tariffs/admin/{id}/tariff` (ADMIN)

Тело запроса на назначение тарифа:
```json
{
  "userId": "00000000-0000-0000-0000-000000000000",
  "tariffId": "00000000-0000-0000-0000-000000000000"
}
```

Тело запроса на изменение клиентского тарифа:
```json
{
  "tariffId": "00000000-0000-0000-0000-000000000000",
  "customPrice": 399.99,
  "customConditions": "Скидка для постоянного клиента"
}
```

---

## Service Request Service (`/service-requests`)

### Клиентские эндпоинты
- `POST /service-requests`
- `GET /service-requests/my`
- `GET /service-requests/my/{id}`

Тело запроса на создание сервисной заявки:
```json
{
  "type": "REPAIR",
  "tariffId": "00000000-0000-0000-0000-000000000000",
  "address": "Москва, Арбат 10",
  "phone": "+79991234567",
  "details": "Нет сигнала с утра"
}
```

### Админские эндпоинты
- `GET /service-requests/admin?status=SUBMITTED&type=REPAIR&dateFrom=2026-03-01&dateTo=2026-03-31`
- `PATCH /service-requests/{id}/status`
- `GET /admin/audit-logs?limit=200`

Тело запроса на смену статуса:
```json
{
  "status": "IN_PROGRESS",
  "adminComment": "Заявка принята в работу"
}
```

---

## Примечания по валидации

- Формат телефона: `^\\+?\\d{10,15}$`
- Ошибки возвращаются в едином формате из общего `GlobalExceptionHandler`.

## Основные enum-значения

Определены в `common-lib`:
- `OrderStatus`
- `ServiceRequestStatus`
- `ServiceRequestType`
- `ConnectionType`
- `Role`
