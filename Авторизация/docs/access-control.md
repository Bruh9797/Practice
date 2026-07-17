# Авторизация и права доступа ThermoSelect

Актуальная матрица ролей, REST endpoints, CSRF и форматы ответов поддерживаются в общей документации проекта:

- [`../../docs/auth/api-access.md`](../../docs/auth/api-access.md) — доступ и API;
- [`../../docs/backend/architecture.md`](../../docs/backend/architecture.md) — session/CSRF и компоненты;
- [`../../docs/defense/use-cases.md`](../../docs/defense/use-cases.md) — UML сценариев.

Ключевое отличие от первоначального прототипа: каталог доступен только после входа, CSRF включён, production работает same-origin без wildcard CORS, а изменение роли или блокировка проверяется на каждом защищённом запросе.
