# Авторизация и разграничение доступа

ThermoSelect использует серверную `HttpSession` и cookie `JSESSIONID`. JWT и CORS
не применяются: production-фронтенд входит в тот же JAR и работает с API на том
же origin, а Vite проксирует `/api` при локальной разработке.

## Поток авторизации

1. Клиент вызывает `GET /api/auth/csrf` и запоминает `headerName` и `token`.
2. Все `POST`, `PUT`, `PATCH` и `DELETE`, включая регистрацию, вход и выход,
   отправляют этот токен в указанном заголовке с `credentials: include`.
3. `POST /api/auth/login` проверяет пароль, меняет идентификатор сессии и
   сохраняет `SecurityContext`. После входа клиент получает новый CSRF-токен.
4. `POST /api/auth/logout` обрабатывается единственным Spring Security logout
   filter: сессия инвалидируется, cookies удаляются, ответ имеет статус `204`.

Cookie сессии имеет `HttpOnly`, `SameSite=Lax` и срок бездействия 30 минут.
CSRF-cookie также имеет `HttpOnly`: SPA получает значение токена только из JSON,
а не читает cookie напрямую.
В профиле `postgres` флаг `Secure` включен по умолчанию; для локального HTTP его
можно явно отключить через `SESSION_COOKIE_SECURE=false`.

## Доступ

| Маршрут | Доступ |
| --- | --- |
| `GET /api/auth/csrf`, `POST /api/auth/register`, `POST /api/auth/login` | публичный |
| `/api/auth/me`, `/api/auth/logout`, `/api/heat-exchangers/**` | `USER` или `ADMIN` |
| `/api/admin/**` | только `ADMIN` |
| лендинг, auth-страницы и статические файлы React | публичный |

Перед каждым защищенным API-запросом `CurrentUserRefreshFilter` перечитывает
пользователя из БД. Удаление, блокировка и смена роли поэтому действуют для уже
открытой сессии немедленно.

## Стартовые учетные записи

Профиль `demo` создает `demo / demo12345` и `admin / admin12345`. Файловая H2
сохраняется в `data/`. Профиль `postgres` требует переменные `DB_URL`,
`DB_USERNAME`, `DB_PASSWORD`, `ADMIN_USERNAME`, `ADMIN_PASSWORD`, `ADMIN_EMAIL`;
демо-пользователь в нем отключен.

## Ошибки

Все JSON-ошибки имеют форму
`{timestamp,status,error,code,message,details,path}`. В частности, отсутствующий
и неверный CSRF-токены обозначаются `CSRF_MISSING` и `CSRF_INVALID`, занятый
логин или email — `USER_ALREADY_EXISTS`.
