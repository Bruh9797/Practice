# Итог полной проверки

Дата проверки: 13.07.2026.

Команда:

```powershell
cd .\Авторизация
.\mvnw.cmd clean verify
```

Результат: `BUILD SUCCESS`.

| Контур | Результат |
|---|---:|
| Java / Spring integration tests | 23 passed, 0 failed, 0 errors, 0 skipped |
| React / Vitest | 5 passed, 0 failed |
| Frontend production build | Vite 8.1.4, успешно |
| Исполняемый JAR | `target/heat-exchanger-selector-0.0.1-SNAPSHOT.jar` |

Сборка также выполнила `npm ci`, проверила отсутствие известных npm-уязвимостей и поместила production-сборку React в Spring Boot JAR.

XML-результаты Java-тестов после запуска находятся в `Авторизация/target/surefire-reports/`. React выводит результат Vitest в консоль Maven; отдельно тесты можно повторить командами из `docs/tests/README.md`.
