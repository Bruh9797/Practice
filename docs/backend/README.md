# Backend и каталог

Backend находится в `Авторизация/src/main/` и использует Spring Boot 4.1, Spring Data JPA, Flyway и Hibernate в режиме `ddl-auto=validate`.

## Состав

- `catalog/` — сущности, DTO, поиск, ранжирование, сравнение и ADMIN CRUD;
- `security/` — пользователи, роли, session-аутентификация и CSRF;
- `web/` — точный SPA fallback;
- `resources/db/migration/` — схема БД;
- `resources/data/demo-catalog.psv` — 42 записи каталога.

## Данные каталога

Каждая запись содержит все общие поля карточки: площадь, расход, мощность, температуру, давление, размеры, массу и температурно-барическую кривую. Подтверждённые значения сохранены из официальных источников. Отсутствующие характеристики заполнены детерминированными mock-значениями и перечислены в факте `mockFields`.

Повторное воспроизводимое заполнение:

```powershell
.\tools\catalog\enrich-demo-catalog.ps1
```

Скрипт идемпотентен и одновременно обновляет [`catalog-data-quality.csv`](catalog-data-quality.csv). Политика источников описана в [`catalog-sources.md`](catalog-sources.md).

## Запуск API

```powershell
cd .\Авторизация
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=demo"
```

Для рабочего запуска рекомендуется единый JAR через `run-demo.ps1`, описанный в корневом README.
