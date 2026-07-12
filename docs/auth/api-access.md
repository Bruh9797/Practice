# Матрица доступа и REST API

## Матрица ролей

| Возможность | Гость | USER | ADMIN |
| --- | :---: | :---: | :---: |
| Лендинг, регистрация, вход | ✅ | ✅ | ✅ |
| Получение CSRF-токена | ✅ | ✅ | ✅ |
| Поиск и фильтрация каталога | ❌ | ✅ | ✅ |
| Просмотр карточки и сравнение | ❌ | ✅ | ✅ |
| Просмотр собственного аккаунта | ❌ | ✅ | ✅ |
| CRUD каталога и производителей | ❌ | ❌ | ✅ |
| Роли, блокировка и удаление пользователей | ❌ | ❌ | ✅ |

## Авторизация

| Метод и путь | Тело / результат |
| --- | --- |
| `GET /api/auth/csrf` | `{headerName, token}`; создаёт/обновляет pre-auth session |
| `POST /api/auth/register` | `{username,password,email?}` → `201 UserResponse`; не выполняет автоматический вход |
| `POST /api/auth/login` | `{username,password}` → `200 UserResponse`; меняет session ID |
| `GET /api/auth/me` | текущий `UserResponse` либо `401` |
| `POST /api/auth/logout` | `204`, invalidates session и удаляет cookie |

`UserResponse`:

```json
{
  "id": 2,
  "username": "demo",
  "email": "demo@thermoselect.local",
  "role": "USER",
  "enabled": true,
  "createdAt": "2026-07-12T12:00:00Z"
}
```

## Каталог

| Метод и путь | Назначение |
| --- | --- |
| `GET /api/heat-exchangers/lookups` | производители, семейства, применения и материалы |
| `POST /api/heat-exchangers/search` | строгая фильтрация, ранжирование и пагинация |
| `GET /api/heat-exchangers/{slug}` | опубликованная паспортная карточка |
| `POST /api/heat-exchangers/compare` | `{ids:[...]}`, 2–4 уникальных опубликованных ID |

Пример запроса:

```json
{
  "query": "NX",
  "families": ["PLATE"],
  "manufacturerIds": [3],
  "applicationCodes": ["HEATING"],
  "materialCodes": ["STAINLESS_STEEL"],
  "requiredFlowM3h": 80,
  "requiredPressureBar": 16,
  "page": 0,
  "size": 12
}
```

Ответ `SearchPage` содержит `items`, `page`, `size`, `totalElements`, `totalPages` и `excludedUnknownCount`. У каждого результата есть `score`, `completeness` и человекочитаемые `reasons`.

## Администрирование

- `GET/POST /api/admin/heat-exchangers`
- `GET/PUT/DELETE /api/admin/heat-exchangers/{id}`
- `PATCH /api/admin/heat-exchangers/{id}/status`
- `GET/POST /api/admin/manufacturers`
- `PUT/DELETE /api/admin/manufacturers/{id}`
- `GET /api/admin/users`
- `GET /api/admin/users/{id}`
- `PATCH /api/admin/users/{id}/role`
- `PATCH /api/admin/users/{id}/enabled?enabled=true|false`
- `DELETE /api/admin/users/{id}`

PUT, status change и архивирование используют `version`. Устаревшая версия возвращает `409`.

## HTTP статусы

| Статус | Значение |
| ---: | --- |
| 400 | Bean Validation, malformed JSON/query или неверный диапазон |
| 401 | отсутствует/отозвана сессия |
| 403 | недостаточно роли или неверный CSRF |
| 404 | сущность не найдена либо запись не опубликована |
| 409 | duplicate, self-modification или optimistic lock |

Все изменяющие методы требуют CSRF header. SPA получает его через `GET /api/auth/csrf`, хранит только в памяти и обновляет после login/logout.
