# Журналы

Demo-профиль пишет журнал одновременно в консоль и файл:

```text
Авторизация/logs/thermoselect.log
```

Просмотр последних записей и слежение в реальном времени:

```powershell
Get-Content .\Авторизация\logs\thermoselect.log -Tail 100
Get-Content .\Авторизация\logs\thermoselect.log -Tail 50 -Wait
```

Поиск ошибок:

```powershell
Select-String -Path .\Авторизация\logs\*.log -Pattern 'ERROR|WARN|Exception'
```

Файлы ротируются по размеру, хранятся семь дней и не добавляются в Git. Журнал полной сборки можно сохранить командой:

```powershell
cd .\Авторизация
.\mvnw.cmd clean verify *>&1 | Tee-Object -FilePath .\logs\verify.log
```

Результаты тестов следует смотреть в `target/surefire-reports`, а не искать в runtime-журнале.
