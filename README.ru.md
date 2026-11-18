# HealthConnect Export 📱

[![CI](https://github.com/kas-cor/healthconnect-export/actions/workflows/build-apk.yml/badge.svg)](https://github.com/kas-cor/healthconnect-export/actions/workflows/build-apk.yml)
[![Coverage](https://raw.githubusercontent.com/kas-cor/healthconnect-export/main/badges/coverage.svg)](https://github.com/kas-cor/healthconnect-export/actions/workflows/build-apk.yml)
[![Branches](https://raw.githubusercontent.com/kas-cor/healthconnect-export/main/badges/branches.svg)](https://github.com/kas-cor/healthconnect-export/actions/workflows/build-apk.yml)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)
[![Kotlin](https://img.shields.io/badge/kotlin-2.4.20-purple)](https://kotlinlang.org)
[![Версия](https://img.shields.io/badge/%D0%B2%D0%B5%D1%80%D1%81%D0%B8%D1%8F-1.11-blue)](CHANGELOG.md)
[![Release](https://img.shields.io/github/v/release/kas-cor/healthconnect-export)](https://github.com/kas-cor/healthconnect-export/releases)
[![English](https://img.shields.io/badge/README-English-blue)](README.md)

> Бейдж покрытия автообновляется при каждом пуше в `main` через CI.

Android-приложение для экспорта данных Google Health Connect в JSON с опциональной синхронизацией на Google Drive и отправкой на webhook. **Два языка**: английский и русский.

## Возможности ✨

- **Ежедневный экспорт** — один день = один JSON-файл (`health_YYYY-MM-DD.json`)
- **Локальное хранение** — файлы сохраняются на устройстве (внешняя директория)
- **Экспорт по расписанию** — ежедневно или еженедельно через WorkManager
- **Google Drive синхронизация** — опционально, автосинхронизация на Drive
- **Webhook доставка** — POST JSON по URL (с опциональным Bearer-токеном, повтор при ошибке)
- **Тест вебхука** — проверка соединения с данными за сегодня прямо из UI
- **Webhook каждые 2 часа** — отправка данных за сегодня на webhook каждые 2 часа (без Drive)
- **Надёжная фоновая выгрузка** — разрешение Health Connect на фоновое чтение (обязательно на Android 14+), ресивер загрузки, будильник-watchdog, работающий в Doze, автоматическая догоняющая выгрузка пропущенных дней (сразу при открытии приложения, иначе — по будильнику watchdog), журнал выгрузок во вкладке «Расписание» и уведомление, если доставка сломалась
- **20 типов данных** — шаги, пульс, сон, калории, тренировки, питание, скорость и другие
- **Детальные измерения** — давление, глюкоза, сатурация, вес, процент жира и пульс содержат отдельные замеры рядом с суточным агрегатом (`readings` / `samples`, только JSON, не более 2000 на тип в сутки)
- **Выбор периода** — последние 7/30 дней или произвольный диапазон с календарём
- **Выбор источника данных** — предпочитаемый источник (Google Fit, Samsung Health и т.д.)
- **Отмена экспорта** — отмена текущего экспорта одним нажатием
- **Сводка на дашборде** — карточка со статистикой после экспорта: шаги, пульс, калории, дистанция, сон
- **JSON просмотрщик** — полноэкранный просмотрщик на тёмной теме с подсветкой синтаксиса + копирование в буфер
- **Переключение темы** — светлая/тёмная/как в системе с анимированным переходом (вкладка «Настройки»)
- **Переключение языка** — английский / русский через вкладку «Настройки», сохраняется между сессиями
- **Навигация по вкладкам** — Экспорт / История / Интеграции / Расписание / Настройки
- **Проверка обновлений** — проверка релизов GitHub с точкой-уведомлением в шапке + диалог «Что нового»
- **Вход в Google Drive** — аутентификация через Credential Manager, прошлый аккаунт восстанавливается при запуске; доступ к Drive (scope) запрашивается лениво при первой синхронизации
- **Управление историей** — раскрывающийся список «Показать все файлы» (открывается любой файл), счётчик файлов и общий размер в шапке
- **Действия с файлами** — поделиться любым экспортированным файлом через системный share-меню или удалить (с подтверждением)
- **Формат экспорта** — JSON или CSV (одна строка на день, экранирование RFC 4180)
- **Автоочистка** — хранить файлы N дней, старые удаляются автоматически
- **Время запуска** — выбор часа для ежедневного/еженедельного экспорта
- **Ручной + автоматический** — экспорт по запросу или по расписанию

## Архитектура 🏗️

```
MainActivity → ExportScreen (Compose) → ExportViewModel
  ├─ ExportDataUseCase         — рабочий процесс экспорта как Flow<ExportStep>
  ├─ DriveManager              — вход/синхронизация/выход Google Drive
  ├─ GoogleAuthProvider        — вход через Credential Manager + авторизация Drive-скоупа
  ├─ WebhookManager            — настройки вебхука/отправка/тест
  ├─ ScheduleManager           — создание/отмена периодического экспорта
  ├─ HealthConnectRepository   — чтение Health Connect API (пакетно через TypeHandler)
  ├─ LocalExportRepository     — сохранение/чтение/удаление JSON-файлов
  ├─ GoogleDriveRepository     — Drive API (загрузка/список/удаление)
  ├─ WebhookRepository         — POST с повторной попыткой
  ├─ DailyExportWorker         — фоновый экспорт по расписанию (ежедневно/еженедельно)
  ├─ Every2HoursWebhookWorker  — периодическая webhook-отправка (каждые 2 часа)
  ├─ CatchUpWebhookWorker      — разовая выгрузка пропущенных дней
  └─ DeliveryWatchdog          — будильник-watchdog + ресивер загрузки
```

## Быстрый старт 🚀

### Build-скрипт (`./build.sh`)

Скрипт для сборки, установки, запуска и тестирования приложения.

| Флаг | Описание |
|---|---|
| *(без флагов)* | Собрать debug APK |
| `--run` | Собрать debug APK, установить на устройство и запустить |
| `--release` | Собрать release APK (подпись продакшн-ключом или debug-ключом) |
| `--install [apk]` | Установить APK на устройство (по умолч. debug APK) |
| `--pull` | Вытянуть экспортированные JSON-файлы (`health_*.json`) с устройства |
| `--test <url> [token]` | Отправить тестовый POST с демо-данными Health Connect на webhook |
| `--logs` | Показать logcat, отфильтрованный по приложению (`ExportScreen`, `HealthConnect`, `DriveManager`, etc.) |
| `--help` | Показать справку |

```bash
# Только сборка debug
./build.sh

# Сборка, установка, запуск
./build.sh --run

# Release сборка (подписанная или debug-подписанная)
./build.sh --release

# Вытянуть JSON-файлы с устройства
./build.sh --pull

# Отправить тестовые данные на webhook
./build.sh --test https://example.com/webhook

# С Bearer-токеном
./build.sh --test https://example.com/webhook my-token

# Показать логи приложения
./build.sh --logs

# Напрямую через Gradle
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Тестирование 🧪

```bash
# Запустить все модульные тесты (436)
./gradlew testDebugUnitTest

# Отчёт покрытия + проверка порогов
./gradlew jacocoTestReport jacocoTestCoverageVerification
# Открыть: app/build/reports/jacoco/jacocoTestReport/html/index.html
```

**Наборы тестов (всего 436):**

| Файл | Тестов | Что проверяет |
|---|---|---|
| `ExportViewModelTest` | 54 | Состояния ViewModel: экспорт, webhook, вход в Drive/восстановление сессии, флаг sign-out, согласие на Drive-скоуп, проверка обновлений, формат, час расписания, retention, действия с файлами |
| `WebhookRepositoryTest` | 42 | sendRecords через локальный HTTP-сервер (успех/ошибка/авторизация/спецсимволы/JSON-тело) + валидация URL + retry-политика по HTTP-кодам |
| `DataModelsSerializationTest` | 45 | Roundtrip-сериализация: DailyHealthRecord, ExportConfig (включая формат/час), enum-ы, SpeedData, sourceDisplayName, `hasAnyData()`, детальные измерения |
| `HealthConnectReadingsTest` | 8 | Детальные измерения: формат времени и смещение, порядок при разных зонах, лимит 2000 замеров в сутки |
| `LocalExportRepositoryTest` | 31 | Файловые операции: сохранение (JSON/CSV), список, очистка, удаление обоих форматов, формат имени |
| `HighlightJsonSyntaxTest` | 31 | Подсветка синтаксиса JSON: строки, числа, boolean, null, вложенные объекты, массивы, экранированные кавычки |
| `DailyExportWorkerTest` | 32 | doWork() (успех/нет данных/обработка разрешений/4xx без retry/исключения) + schedule() (daily/weekly/manual/cancel, час расписания) |
| `HumanReadableMapperTest` | 26 | 8 mapper-функций: bodyPosition, specimenSource, sleepStage, exerciseType (все официальные id) и др. |
| `GoogleDriveRepositoryTest` | 26 | Синхронизация Drive: загрузка, список, скачивание, удаление, работа с токеном, спецсимволы |
| `Every2HoursWebhookWorkerTest` | 20 | doWork (happy path, пустой URL, пустой день не отправляется, обработка разрешений, исключения) + schedule/cancel |
| `ExportDataUseCaseTest` | 16 | Сценарий экспорта: разрешения, проверка Health Connect, прогресс, webhook, Drive |
| `CsvMapperTest` | 13 | CSV-уплощение: синхронизация заголовка/строки, экранирование RFC 4180, форматирование чисел, отсутствующие секции |
| `LocaleManagerTest` | 12 | Все ветки localeDisplayName, saveLocale/getSavedLocale |
| `DeliveryLogTest` | 13 | Сохранение последней попытки/успеха, ограничение ring-буфера, учёт доставленных дат, полная очистка `clear()` |
| `CatchUpWebhookWorkerTest` | 14 | Окно догона (7 дней при первом запуске, максимум 30), точка возобновления, retry при ошибках/недоступном Health Connect, без retry без фонового разрешения |
| `DeliveryWatchdogTest` | 6 | Постановка/снятие alarm, повторная постановка обеих периодических задач при срабатывании, устойчивость к отсутствию AlarmManager |
| `DeliveryWatchdogSchedulingTest` | 6 | Первая задержка и интервал расписания watchdog-а |
| `ExportSettingsTest` | 7 | Конфиг экспорта в SharedPreferences: значения по умолчанию, roundtrip, сохранённый/очищенный URL, переключатель автодоставки |
| `ExportedFilesCardTest` | 5 | Срез visibleExportFiles: свёртка до N новых, showAll, ≤N файлов, пустой список |
| `DeliveryWatchdogCatchUpTest` | 6 | Когда watchdog ставит догон: устаревший успех, ничего не доставлено, автодоставка выключена |
| `DeliveryBootReceiverTest` | 4 | BOOT_COMPLETED / MY_PACKAGE_REPLACED восстанавливает расписание и ставит догон |
| `DateRangeCardTest` | 3 | Compose UI тесты: пресеты, свои даты, взаимодействие с пикером |
| `BatteryOptimizationTest` | 3 | Состояние Doze-исключения и intent запроса |
| `WatchdogAlarmReceiverTest` | 2 | Срабатывание alarm ставит догон, если доставка устарела; ничего — если автодоставка выключена |
| `HealthConnectRepositoryTest` | 7 | Окно атрибуции записей, фильтрация по источнику, набор разрешений для фонового чтения |
| `ExportForegroundNotifierTest` | 4 | Foreground-уведомление и канал, которые используют фоновые воркеры |

## CI/CD 🚀

Workflow: `.github/workflows/build-apk.yml`

### Триггеры

- **Push** в `main`, `develop`
- **Pull Request** в `main`
- **Push тега** `v*` (например, `v1.1`)
- **Вручную** через `workflow_dispatch`

### Конвейер

```
Push → Lint + Ktlint → Модульные тесты
     → Покрытие (JaCoCo отчёт + проверка 9 правил + бейдж)
     → Debug APK
     └─ Push тега → Расшифровка keystore → Подпись → Release APK → GitHub Release
```

### Проверка покрытия

После каждого пуша JaCoCo проверяет покрытие по 9 правилам. Если какое-то правило нарушено, задача `coverage` падает:

| Область | Счётчик | Порог |
|---|---|---|
| **Весь проект** | LINE | ≥ 30% |
| | BRANCH | ≥ 12% |
| | INSTRUCTION | ≥ 15% |
| | CLASS | ≥ 45% |
| **Пакет worker** | LINE | ≥ 95% |
| **Пакет data** | LINE | ≥ 70% |
| **Пакет viewmodel** | LINE | ≥ 65% |
| **Пакет util** | LINE | ≥ 50% |
| **Пакет repository** | LINE | ≥ 35% |

При пуше в `main` бейдж покрытия автообновляется в `badges/`.

### Артефакты

| Артефакт | Хранение | Содержимое |
|---|---|---|
| `test-results` | 7 дней | HTML отчёты тестов |
| `coverage-html` | 7 дней | JaCoCo HTML отчёт |
| `coverage-xml` | 30 дней | JaCoCo XML для Codecov/SonarCloud |
| `HealthConnectExport-debug` | 7 дней | Debug APK |
| `HealthConnectExport-release` | 30 дней | Подписанный release APK (только при пуше тега) |

### GitHub Secrets

| Secret | Описание |
|---|---|
| `KEYSTORE_BASE64` | `healthconnect-release.jks` в base64 |
| `KEYSTORE_PASSWORD` | Пароль keystore (используется и как пароль ключа; псевдоним по умолчанию — `healthconnect`) |

### Релиз

```bash
git tag v1.12
git push origin v1.12
# CI: увеличить версию → собрать → создать GitHub Release
```

[Workflow](.github/workflows/build-apk.yml)

## Настройка Google Drive 🔐

Для входа в Google (Credential Manager) и доступа к Drive нужно **два** OAuth Client ID в одном Google Cloud Project:

| Тип | Client ID | Назначение |
|---|---|---|
| **Android** (OAuth) | `730530422387-oaffqtrvfd1rqr6jn1uq8791mgbbpmlj` | Проверка SHA-1 + package name (Google Play Services) |
| **Web application** (OAuth) | `730530422387-dveo97h089iesh4etmj74q9dn8j221f1` | `setServerClientId()` в `BuildConfig.GOOGLE_CLIENT_ID` |

### SHA-1 отпечатки

| Тип сборки | SHA-1 отпечаток |
|---|---|
| **Release** | `2A:BF:A4:CA:62:59:78:A2:5D:78:FD:74:2D:CB:CA:07:D2:37:42:72` |
| **Debug** | `8B:BB:D1:45:E2:61:5B:02:57:E1:3F:26:29:1A:AF:F0:2C:40:77:73` |

### OAuth consent screen

1. Откройте [OAuth consent screen](https://console.cloud.google.com/apis/credentials/consent)
2. Тип: **External** → **Testing** (не требуется верификация)
3. Добавьте **Test users** — свой email
4. На шаге **Scopes** добавьте `https://www.googleapis.com/auth/drive.file`

### Требуемые API

- [Google Drive API](https://console.cloud.google.com/apis/library/drive.googleapis.com) — `ВКЛЮЧЁН`
- [Identity Toolkit API](https://console.cloud.google.com/apis/library/identitytoolkit.googleapis.com) — `ВКЛЮЧЁН`

### Проверка подписи

```bash
apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
# SHA-1: 2abfa4ca625978a25d78fd742dcbca07d2374272
```

### Почему релизный APK не может войти в Google

APK на странице релизов подписан собственным релизным ключом проекта, и его SHA-1 зарегистрирован в том Cloud-проекте — но приложение запрашивает `https://www.googleapis.com/auth/drive.file`, а Google относит этот scope к **restricted**. Раздавать restricted-scope произвольным пользователям можно только после OAuth-верификации, а для Drive она включает **ежегодную оценку безопасности (CASA) в авторизованной лаборатории за счёт разработчика**, плюс опубликованную политику приватности и подтверждённый домен. Без этого экран согласия остаётся в режиме **Testing**, куда допускаются только аккаунты из списка *test users* (до 100) — все остальные получают ошибку `[28444] Developer console is not set up correctly` из [issue #89](https://github.com/kas-cor/healthconnect-export/issues/89), какой бы SHA-1 ни был зарегистрирован. Проект — личный и открытый по исходникам, поэтому релиз выходит без входа в Google Drive.

### Сборка со своими данными Google (личное использование)

Бесплатно, без верификации, ~15 минут:

1. **Что нужно** — JDK 21 + Android SDK (или Android Studio); `git clone https://github.com/kas-cor/healthconnect-export`
2. **Cloud-проект** — [создайте свой](https://console.cloud.google.com/projectcreate) и включите **Google Drive API** и **Identity Toolkit API** (APIs & Services → Library)
3. **Экран согласия OAuth** — тип пользователей **External**, статус публикации **Testing**; добавьте свой Google-аккаунт в **Test users**; scope `https://www.googleapis.com/auth/drive.file`. Режим Testing не требует верификации, но пускает только тестовых пользователей, а выданный доступ живёт ~7 дней — диалог согласия для Drive может появляться раз в неделю
4. **Два OAuth-клиента** (Credentials → Create credentials → OAuth client ID):
   - **Android** — пакет `com.healthconnect.export` (оставьте `applicationId` в `app/build.gradle.kts` как есть либо зарегистрируйте свой пакет и поменяйте его) + **SHA-1 ключа, которым будете подписывать**
   - **Web application** — redirect URIs не нужны; скопируйте его client ID
5. **Направьте приложение в свой проект** — вставьте client ID **веб**-клиента в `defaultConfig` файла `app/build.gradle.kts`:
   ```kotlin
   buildConfigField("String", "GOOGLE_CLIENT_ID", "\"<YOUR_WEB_CLIENT_ID>.apps.googleusercontent.com\"")
   ```
6. **Подпишите своим ключом** — либо
   - *debug-сборка*: `./gradlew assembleDebug`, установите `app/build/outputs/apk/debug/app-debug.apk` и зарегистрируйте **debug**-SHA-1 из `./gradlew signingReport` (вариант `debug`); либо
   - *release-сборка*: `keytool -genkeypair -v -keystore healthconnect-release.jks -alias healthconnect -keyalg RSA -keysize 2048 -validity 10000`, затем `KEYSTORE_PASSWORD=… ./gradlew assembleRelease` — `signingConfigs.release` уже ждёт `healthconnect-release.jks` в корне репозитория и читает `KEYSTORE_PASSWORD` / `KEY_ALIAS` / `KEY_PASSWORD` из переменных окружения
7. **Зарегистрируйте отпечаток** — `./gradlew signingReport` (или `keytool -list -v -keystore healthconnect-release.jks -alias healthconnect`) → вставьте SHA-1 в **Android**-клиент. Консоль принимает его с двоеточиями и без; подождите пару минут, пока изменение разойдётся
8. **Запуск** — установите APK, выдайте разрешения Health Connect, откройте вкладку «Интеграции» → **Войти в Google**. Согласие на Drive появится при первой синхронизации, а `drive.file` ограничивает приложение только теми файлами, которые оно создало само
9. **Если не работает** — `[28444]` / «not registered» значит, что SHA-1 или пакет установленного APK не совпадает с Android-клиентом (проверьте `apksigner verify --print-certs <apk>`); `access_denied` / «app not verified» — аккаунта нет в **Test users**; ошибки Drive при синхронизации — в вашем проекте не включён Drive API

## Webhook 📡

После экспорта данные о здоровье можно отправить на любой URL через POST-запрос.

### Настройка

- **URL** — настраивается в UI, валидируется на стороне клиента (красная подсветка при неверном URL)
- **Авторизация** — опциональный Bearer-токен (хранится в `ExportConfig`)
- **Автоотправка** — переключатель в UI для автоматической отправки после каждого экспорта
- **Тест** — кнопка «Тест вебхука» отправляет данные за сегодня и показывает результат
- Работает как с ручным, так и с запланированным экспортом

### Формат запроса

| Атрибут | Значение |
|---|---|
| **Метод** | `POST` |
| **Content-Type** | `application/json` |
| **Authorization** | `Bearer <token>` (опционально) |
| **Таймаут** | 15 секунд |
| **Повтор** | 1 попытка через 1 с при ошибках 5xx/сетевых |

### Структура payload

Тело JSON обёрнуто в конверт `messages`:

```json
{
  "messages": [
    {
      "date": "2026-05-23",
      "steps": { "total_steps": 12453, "records_count": 480 },
      "heart_rate": { "avg_bpm": 72.5, "min_bpm": 55, "max_bpm": 142, "records_count": 18 },
      "sleep": {
        "total_duration_minutes": 420,
        "sleep_stages": { "Deep sleep": 90, "Light sleep": 195, "REM sleep": 105, "Awake": 30 },
        "records_count": 1
      },
      "metadata": {
        "app_version": "1.0.0",
        "export_timestamp": "2026-05-23T23:00:00",
        "timezone": "Europe/Moscow",
        "source_device": "test_device"
      }
    }
  ]
}
```

Каждый элемент массива `messages` — это `DailyHealthRecord` — одна запись за день, содержащая все выбранные типы данных о здоровье (шаги, пульс, сон, калории, дистанция, этажи, активные калории, вес, жир, давление, глюкоза, сатурация, температура, дыхание, гидратация, пульс покоя, тренировки, питание, скорость и цикл).

### Детальные измерения

Давление, глюкоза, сатурация, вес, процент жира и пульс дополнительно перечисляют отдельные замеры, из которых посчитан суточный агрегат:

```json
{
  "date": "2026-09-19",
  "blood_pressure": {
    "systolic_mmhg": 124.5,
    "diastolic_mmhg": 85.0,
    "records_count": 2,
    "readings": [
      { "time": "2026-09-19T06:22:41.123-05:00", "systolic_mmhg": 141.0, "diastolic_mmhg": 98.0, "source": "com.omronhealthcare.omronconnect" },
      { "time": "2026-09-19T20:10:00-05:00", "systolic_mmhg": 108.0, "diastolic_mmhg": 72.0, "source": "com.omronhealthcare.omronconnect" }
    ]
  },
  "heart_rate": {
    "avg_bpm": 72.5,
    "min_bpm": 55,
    "max_bpm": 142,
    "records_count": 18,
    "samples": [
      { "time": "2026-09-19T08:00:00+03:00", "bpm": 68, "source": "com.xiaomi.hm.health" }
    ]
  }
}
```

- `time` — локальное время ISO-8601 с тем смещением, в котором запись была создана (миллисекунды сохраняются)
- `source` — имя пакета приложения, записавшего замер
- списки идут от старых к новым и по умолчанию `[]`, поэтому JSON от старых версий читается без ошибок
- если за сутки больше **2000** замеров одного типа (устройство, пишущее пульс раз в секунду), они равномерно прореживаются — `records_count` и агрегаты по-прежнему учитывают все записи
- в CSV остаются только суточные итоги: детальные измерения есть только в JSON

## Расписание ⏰

- **Вручную**, **Ежедневно** (24ч) или **Еженедельно** (168ч)
- **Каждые 2 часа** — опциональная webhook-отправка данных за сегодня (чекбокс в разделе Расписание)
- По умолчанию: **Ежедневно** (автовключение при запуске)
- Использует WorkManager `PeriodicWorkRequest` **без** ограничения battery-not-low (экспорт дешёвый, а проверка заряда молча блокировала выгрузку)
- **Watchdog:** неточный будильник, которому разрешено срабатывать в Doze (`setAndAllowWhileIdle`), заново ставит периодические задачи и запускает разовую догоняющую выгрузку
- **После перезагрузки / обновления приложения:** `DeliveryBootReceiver` восстанавливает расписание из сохранённых настроек (разрешение `RECEIVE_BOOT_COMPLETED` было объявлено, но не использовалось)
- **Догоняющая выгрузка:** `CatchUpWebhookWorker` продолжает с последнего фактически доставленного дня и отправляет всё после него одним запросом (не больше 30 дней); заполняет и самый первый пропуск, если доставок ещё не было
- **Разрешение на фоновое чтение:** `android.permission.health.READ_HEALTH_DATA_IN_BACKGROUND` объявлено и запрашивается вместе с типовыми разрешениями; без него Android 14+ отклоняет любое чтение из фонового воркера
- **Политика повторов:** временные проблемы (сеть, 5xx, 408/429) повторяются, постоянные (нет фонового разрешения, HTTP 4xx) завершают запуск до следующего периода; ни один запуск не возвращает `failure`, потому что WorkManager после этого окончательно убивает периодическую задачу
- **Диагностика:** во вкладке «Расписание» видны время последней попытки, последней успешной выгрузки и журнал; первая ошибка за инцидент дополнительно показывается уведомлением, которое снимается первым успехом
- **Освобождение от оптимизации батареи:** вкладка «Расписание» предлагает опциональное исключение из Doze и показывает его текущее состояние

## Локализация 🌐

- **Языки**: английский (по умолчанию), русский
- **Переключение языка**: через вкладку «Настройки» → «Язык»
- **Покрытие**: Все **220 строк** переведены в `values-ru/strings.xml`
- **Безопасность форматов**: все плейсхолдеры `%d`, `%s`, `%.1f` совпадают между языками
- **Сохранение**: выбранный язык сохраняется в SharedPreferences

### Как переключить язык 🇷🇺

1. Откройте вкладку **Настройки** (шестерёнка в нижней панели) → раздел **Язык**
2. Выберите **Русский** (или **Системный** для автоматического выбора языка устройства)

### Что переведено

Все строки интерфейса:
- Названия типов данных (Шаги, Пульс, Сон, Калории, Дистанция и т.д.)
- Интерфейс экспорта и расписания
- Сообщения об ошибках и статусе
- Google Drive, Webhook
- JSON просмотрщик, сводка экспорта
- Все системные уведомления (WorkManager)

### Технические детали

- Файл перевода: `app/src/main/res/values-ru/strings.xml`
- Все **220 строк** переведены — ни одной пропущенной английской строки
- Форматные плейсхолдеры (`%d`, `%s`, `%.1f`) полностью совпадают с английской версией — никаких crash'ей при переключении языка
- Выбор языка сохраняется в `SharedPreferences` и восстанавливается после перезапуска
- На здоровье не влияет — JSON-данные экспортируются с английскими ключами независимо от языка интерфейса

### Добавить или исправить перевод

Отредактируйте `app/src/main/res/values-ru/strings.xml` и запустите валидатор:

```bash
python3 scripts/locale-validator.py
```

### Как добавить новый язык 🌍

Хотите перевести приложение на свой язык? Вот пошаговая инструкция:

#### 1. Создайте файл перевода

Скопируйте английский `strings.xml` в новую папку с кодом вашего языка.

Например, для французского:

```bash
# Создать папку для нового языка
mkdir -p app/src/main/res/values-fr

# Скопировать английский strings.xml
cp app/src/main/res/values/strings.xml app/src/main/res/values-fr/strings.xml
```

Подставьте свой [код языка](https://developer.android.com/guide/topics/resources/providing-resources#LocaleQualifier) вместо `fr`.

#### 2. Переведите все строки

Отредактируйте `app/src/main/res/values-fr/strings.xml` — переведите **все** значения (`<string>...</string>`), но **не трогайте** атрибуты `name`.

Важно:
- Сохраняйте форматные плейсхолдеры (`%d`, `%s`, `%.1f`) — они подставляются программно
- Брендовые названия (`HealthConnect Export`, `Google Drive`, `Health Connect`, `WorkManager`) можно оставить на английском
- Технические термины (`JSON`, `URL`, `Bearer`, `Webhook`, `WebSocket`) обычно не переводят

#### 3. Запустите валидатор

```bash
python3 scripts/locale-validator.py
```

Валидатор проверит, что все строки переведены и нет пропущенных.

#### 4. Добавьте язык в UI приложения

Отредактируйте `LocaleManager.kt` (`app/src/main/java/com/healthconnect/export/util/LocaleManager.kt`):

```kotlin
// Добавьте ваш язык в список доступных
enum class AppLocale(val code: String, val displayName: String) {
    SYSTEM("", "System"),
    EN("en", "English"),
    RU("ru", "Русский"),
    FR("fr", "Français"),  // ← ваш язык
}
```

#### 5. Создайте README на вашем языке

Скопируйте и переведите `README.md`:

```bash
cp README.md README.fr.md
# Переведите содержимое на французский
```

Добавьте бейдж-ссылку в `README.md`:

```markdown
[![Français](https://img.shields.io/badge/README-Fran%C3%A7ais-blue)](README.fr.md)
```

#### 6. Откройте Pull Request

Создайте PR на GitHub с файлами перевода. После мержа:
- В приложении появится новый язык в переключателе языков 🌐
- README на вашем языке будет доступен по бейджу

**Спасибо за ваш вклад!** 🙌

## Разрешения

| Разрешение | Назначение |
|---|---|
| `INTERNET` | Синхронизация Drive + webhook |
| `ACCESS_NETWORK_STATE` | Проверка сети |
| `FOREGROUND_SERVICE` | Фоновая работа |
| `RECEIVE_BOOT_COMPLETED` | Перезапуск после перезагрузки |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` | Опциональное исключение из Doze (предлагается во вкладке «Расписание») |
| `health.READ_*` | Типы данных Health Connect (20 типов) |
| `health.READ_HEALTH_DATA_IN_BACKGROUND` | Фоновое чтение Health Connect в задачах по расписанию (Android 14+) |
| `FOREGROUND_SERVICE_DATA_SYNC` | Best-effort foreground-сервис, пока воркер читает данные |
| `POST_NOTIFICATIONS` | Уведомление об ошибке доставки (запрашивается при включении автодоставки, Android 13+) |

## Технологический стек

| Компонент | Версия |
|---|---|
| Язык | Kotlin 2.4.21 |
| UI | Jetpack Compose + Material3 (BOM 2026.09.00) |
| Сборка | AGP 9.5.1 / Gradle 9.9.1 |
| Health Connect | `connect-client:1.1.0` |
| Google Drive | `google-api-services-drive:v3-rev20240123-2.0.0`, `google-api-client-android:2.9.2`, `google-http-client-gson:2.2.0` |
| Авторизация | `play-services-auth:22.0.0`, `androidx.credentials:1.6.0`, `googleid:1.2.1` |
| Фон | WorkManager `work-runtime-ktx:2.12.0` |
| Сериализация | `kotlinx-serialization-json:1.13.0` |
| minSdk / targetSdk / compileSdk | 28 / 37 / 37 |
| JVM | 21 |
| Линтинг | ktlint 14.2.0 |
| Тестирование | JUnit 4.13.2, Mockito 5.24.0, mockito-kotlin 6.4.0, Robolectric 4.18 |
| Покрытие | JaCoCo 0.8.12 |
| CI | GitHub Actions |

## Список изменений 📋

Полная история релизов в [CHANGELOG.md](CHANGELOG.md).

| Версия | Дата | Что нового |
|---|---|---|
| [v1.12](https://github.com/kas-cor/healthconnect-export/releases/tag/v1.12) | 2026-11-18 | Детальные измерения за день (давление, глюкоза, сатурация, вес, жир, пульс) рядом с агрегатами, названия тренировок теперь по официальным id Health Connect, 436 тестов |
| [v1.11](https://github.com/kas-cor/healthconnect-export/releases/tag/v1.11) | 2026-11-18 | Выгрузка по расписанию действительно починена: разрешение Health Connect на фоновое чтение, уведомление об ошибках, без повторов на 4xx, пустые дни больше не сдвигают точку догона, 426 тестов |
| [v1.10](https://github.com/kas-cor/healthconnect-export/releases/tag/v1.10) | 2026-09-27 | Вход в Google через Credential Manager, ленивый запрос Drive-скоупа, догон недоставленных дней при открытии приложения, 397 тестов |
| [v1.9](https://github.com/kas-cor/healthconnect-export/releases/tag/v1.9) | 2026-09-25 | Фоновая выгрузка переживает перезагрузки, Doze и агрессивную экономию батареи: будильник-watchdog, ресивер загрузки, догоняющая выгрузка пропущенных дней, журнал |
| [v1.8](https://github.com/kas-cor/healthconnect-export/releases/tag/v1.8) | 2026-08-10 | Действия с файлами в Истории (поделиться/удалить), экспорт JSON/CSV, автоочистка, час запуска расписания, 341 тест |
| [v1.7](https://github.com/kas-cor/healthconnect-export/releases/tag/v1.7) | 2026-08-10 | Проверка обновлений + диалог «Что нового», навигация по вкладкам, автовход в Google Drive, улучшения Истории |
| [v1.6](https://github.com/kas-cor/healthconnect-export/releases/tag/v1.6) | 2026-07-17 | Build-скрипт (`build.sh`), русский README (`README.ru.md`), проверка синхронизации README |
| [v1.5](https://github.com/kas-cor/healthconnect-export/releases/tag/v1.5) | 2026-06-08 | Webhook-воркер каждые 2 часа, кнопка теста webhook, сортировка файлов по убыванию, обновление зависимостей (Kotlin 2.3.21, Gradle 9.5.1) |
| [v1.4](https://github.com/kas-cor/healthconnect-export/releases/tag/v1.4) | 2026-06-07 | Исправление диапазона дат: endDate включает сегодня, исправлены пресеты 7/30 дней |
| [v1.3](https://github.com/kas-cor/healthconnect-export/releases/tag/v1.3) | 2026-05-27 | Формат webhook `{\"messages\": [...]}`, прогресс экспорта с барами по дням, кнопка отмены, чтение по дням |
| [v1.2](https://github.com/kas-cor/healthconnect-export/releases/tag/v1.2) | 2026-05-27 | **+99 тестов** (всего 295), покрытие ~55%, WebhookRepository переписан с локальным HTTP-сервером |
| [v1.1](https://github.com/kas-cor/healthconnect-export/releases/tag/v1.1) | 2026-05-26 | Проверка покрытия, ktlint, обновление API, авторизация webhook, +83 теста, русская локализация |
| [v1.0](https://github.com/kas-cor/healthconnect-export/releases/tag/v1.0) | — | Первый релиз: JSON экспорт, Drive синхронизация, webhook, WorkManager, Material3 UI |

## Лицензия 📄

MIT
