# Модуль авторизации и прав доступа

Модуль отвечает за регистрацию, вход/выход пользователей и разграничение прав
между обычными пользователями и администратором информационно-поисковой
системы теплообменных аппаратов.

Реализация: **Spring Security + сессионная аутентификация** (cookie `JSESSIONID`),
без JWT — состояние авторизации хранится на сервере в `HttpSession`.

## Содержание

- [Структура модуля](#структура-модуля)
- [Роли и права доступа](#роли-и-права-доступа)
- [API](#api)
- [Быстрый старт](#быстрый-старт)
- [Настройка](#настройка)
- [Тестирование](#тестирование)
- [Как это работает](#как-это-работает)
- [Интеграция с другими модулями команды](#интеграция-с-другими-модулями-команды)
- [Возможные улучшения](#возможные-улучшения)

## Структура модуля

```
security/
├── Role.java                      # enum USER / ADMIN
├── User.java                      # JPA-сущность учетной записи
├── UserRepository.java            # Spring Data JPA репозиторий
├── UserPrincipal.java             # адаптер User -> UserDetails
├── CustomUserDetailsService.java  # загрузка пользователя для Spring Security
├── SecurityConfig.java            # правила доступа, CORS, шифрование паролей
├── AdminAccountInitializer.java   # создание учетки ADMIN при первом старте
├── JsonAuthenticationEntryPoint.java  # 401 в формате JSON
├── JsonAccessDeniedHandler.java       # 403 в формате JSON
├── controller/
│   ├── AuthController.java        # регистрация, вход, выход, /me
│   └── AdminUserController.java   # управление пользователями (только ADMIN)
├── dto/
│   ├── RegisterRequest.java
│   ├── LoginRequest.java
│   ├── ChangeRoleRequest.java
│   └── UserResponse.java
└── exception/
    ├── DuplicateUsernameException.java
    ├── SelfModificationForbiddenException.java
    ├── ApiError.java
    └── GlobalExceptionHandler.java
```

Подробное описание ролей, матрицы доступа и сценариев использования (для
UML-диаграмм use case) — в [`docs/access-control.md`](../../../../../../../docs/access-control.md).

## Роли и права доступа

| Роль | Кто это | Ключевые права |
| --- | --- | --- |
| Гость | не авторизован | регистрация, вход, просмотр/поиск каталога (только чтение) |
| `USER` | зарегистрированный пользователь | + свой профиль, выход |
| `ADMIN` | администратор | + управление каталогом, пользователями, журналами, отчётами |

Роль `ADMIN` нельзя получить через публичную регистрацию — только через
существующего администратора или служебную учетную запись, созданную
при первом запуске (см. [Настройка](#настройка)).

## API

| Метод | Путь | Доступ | Описание |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | все | регистрация нового пользователя (роль `USER`) |
| POST | `/api/auth/login` | все | вход, создаёт серверную сессию |
| POST | `/api/auth/logout` | авторизован | завершение сессии |
| GET | `/api/auth/me` | авторизован | данные текущего пользователя |
| GET | `/api/admin/users` | `ADMIN` | список всех пользователей |
| GET | `/api/admin/users/{id}` | `ADMIN` | данные одного пользователя |
| PATCH | `/api/admin/users/{id}/role` | `ADMIN` | изменить роль пользователя |
| PATCH | `/api/admin/users/{id}/enabled?enabled=true\|false` | `ADMIN` | заблокировать/разблокировать |
| DELETE | `/api/admin/users/{id}` | `ADMIN` | удалить пользователя |

Администратор не может изменить роль, заблокировать или удалить **самого себя**
(защита от потери единственного администратора) — вернётся `409 Conflict`.

### Пример: регистрация и вход

```bash
curl -X POST localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"ivan","password":"password123","email":"ivan@example.com"}'

curl -c cookies.txt -X POST localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"ivan","password":"password123"}'

curl -b cookies.txt localhost:8080/api/auth/me
```

### Формат ошибок

Все ошибки модуля возвращаются в едином JSON-формате:

```json
{
  "timestamp": "2026-07-12T10:00:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "Недостаточно прав для выполнения действия",
  "details": []
}
```

| Код | Когда возвращается |
| --- | --- |
| 400 | ошибка валидации входных данных (`@Valid`) |
| 401 | не выполнен вход / неверный логин-пароль |
| 403 | вход выполнен, но не хватает роли |
| 404 | пользователь не найден |
| 409 | логин уже занят / попытка само-удаления или само-блокировки |

## Быстрый старт

1. Поднять PostgreSQL (либо переопределить `DB_URL`/`DB_USERNAME`/`DB_PASSWORD`):
   ```bash
   docker run -d --name heat-exchanger-db \
     -e POSTGRES_DB=heat_exchanger_db \
     -e POSTGRES_PASSWORD=postgres \
     -p 5432:5432 postgres
   ```
2. Запустить приложение:
   ```bash
   ./mvnw spring-boot:run
   ```
3. При первом старте автоматически создаётся администратор `admin` / `admin12345`
   (см. лог с предупреждением о смене пароля).

Таблица `users` создаётся автоматически Hibernate'ом (`ddl-auto: update`),
отдельная миграция не нужна.

## Настройка

Все настройки — через переменные окружения (значения по умолчанию — в `application.yaml`):

| Переменная | По умолчанию | Назначение |
| --- | --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/heat_exchanger_db` | строка подключения к БД |
| `DB_USERNAME` | `postgres` | пользователь БД |
| `DB_PASSWORD` | `postgres` | пароль БД |
| `ADMIN_USERNAME` | `admin` | логин администратора по умолчанию |
| `ADMIN_PASSWORD` | `admin12345` | пароль администратора по умолчанию (сменить в проде!) |
| `ADMIN_EMAIL` | `admin@heat-exchanger.local` | email администратора по умолчанию |

## Тестирование

```bash
./mvnw test
```

Тесты используют отдельный профиль `test` с in-memory БД H2
(`src/test/resources/application-test.yaml`), реальный PostgreSQL для тестов не нужен.

Основной тест модуля — `AuthAccessControlIntegrationTest`:
- анонимный запрос к защищённому ресурсу отклоняется (`401`);
- пользователь может зарегистрироваться, войти и получить свой профиль;
- обычный пользователь не может попасть в `/api/admin/**` (`403`);
- администратор (создан автоматически при старте) получает доступ (`200`);
- вход с неверным паролем отклоняется (`401`).

## Как это работает

1. Пароль при регистрации хешируется `BCryptPasswordEncoder`, в открытом виде нигде не хранится.
2. При входе `AuthenticationManager` (через `DaoAuthenticationProvider` +
   `CustomUserDetailsService`) сверяет пароль с хешем в БД.
3. Успешный вход кладёт `SecurityContext` в `HttpSession`
   (`HttpSessionSecurityContextRepository`) — браузер получает cookie сессии.
4. На каждый следующий запрос Spring Security достаёт контекст из сессии
   по cookie и определяет, авторизован ли пользователь и какая у него роль.
5. Правила доступа заданы декларативно в `SecurityConfig.securityFilterChain()`
   и продублированы `@PreAuthorize("hasRole('ADMIN')")` на `AdminUserController`
   (defense-in-depth).

## Интеграция с другими модулями команды

- **Фронтенд (Артём)**: запросы должны отправляться с `credentials: 'include'`
  (fetch) / `withCredentials: true` (axios) — иначе cookie сессии не передаётся
  и авторизация не сработает. CORS настроен в `SecurityConfig.corsConfigurationSource()`
  (в проде нужно сузить `allowedOriginPatterns` до реального домена фронтенда).
- **Бэкенд/БД (Сережа)**: GET-эндпоинты каталога теплообменников уже открыты
  всем (`/api/heat-exchangers/**`, `/api/search/**`). Для изменяющих запросов
  (`POST`/`PUT`/`DELETE`) их нужно добавить в правило `hasRole("ADMIN")`
  в `SecurityConfig`, либо разместить их под `/api/admin/**`.
- **Журналы и отчёты (Милан)**: любые эндпоинты вида `/api/admin/logs`,
  `/api/admin/reports` автоматически попадут под правило `hasRole("ADMIN")`
  без дополнительных настроек.

## Возможные улучшения

- Подтверждение email при регистрации.
- Ограничение количества попыток входа (защита от перебора паролей).
- Более гибкая ролевая модель (несколько ролей на пользователя, кастомные permissions).
- Refresh/access-токены (JWT), если понадобится мобильный клиент без cookie-сессий.
