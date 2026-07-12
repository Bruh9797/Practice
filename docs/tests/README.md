# Тесты и отчёты

## Полная проверка

```powershell
cd .\Авторизация
.\mvnw.cmd clean verify
```

Команда последовательно выполняет миграции Flyway на тестовой H2, Java integration-тесты, `npm ci`, Vitest, production-сборку React и упаковку JAR.

## Выборочный запуск

```powershell
# Только Java
.\mvnw.cmd test

# Один Java-класс
.\mvnw.cmd "-Dtest=CatalogSeedIntegrationTest" test

# Только React
cd .\frontend
npm test
```

## Где смотреть результаты

- Java XML/TXT: `Авторизация/target/surefire-reports/`;
- React: вывод Vitest в терминале;
- production frontend: `Авторизация/target/frontend-dist/`;
- исполняемый файл: `Авторизация/target/heat-exchanger-selector-0.0.1-SNAPSHOT.jar`;
- сохраняемая сводка: [`reports/verify-summary.md`](reports/verify-summary.md);
- полнота данных: [`../backend/catalog-data-quality.csv`](../backend/catalog-data-quality.csv).

Подробные ручные сценарии находятся в [`test-cases.md`](test-cases.md).
