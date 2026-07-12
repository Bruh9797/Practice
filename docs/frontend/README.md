# Frontend

Интерфейс находится в `Авторизация/frontend/` и построен на React 19, React Router и Vite. Состояния авторизации и сравнения реализованы через Context API; относительный API-клиент всегда использует `credentials: include` и CSRF-токен из памяти.

## Основные маршруты

- публичные: `/`, `/login`, `/register`;
- пользовательские: `/catalog`, `/heat-exchangers/:slug`, `/compare`, `/account`;
- административные: `/admin`, `/admin/catalog`, `/admin/catalog/:id`, `/admin/users`.

## Локальная разработка

Сначала запустите backend на порту 8080, затем:

```powershell
cd .\Авторизация\frontend
npm ci
npm run dev
```

Vite откроет интерфейс на <http://localhost:5173> и проксирует `/api` на Spring Boot.

## Тесты и production-сборка

```powershell
npm test
npm run build
```

Vitest покрывает CSRF/login, преобразование фильтров и ограничение сравнения четырьмя позициями. Полная Maven-сборка запускает эти команды автоматически.

Числовые поля с демонстрационными значениями помечаются бейджем `DEMO`. Они предназначены для проверки интерфейса и не используются как паспортные данные или результат теплового расчёта.
