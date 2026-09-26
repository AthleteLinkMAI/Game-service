# Game Service: устройство и текущая работа

Документ описывает состояние `game-service` после перехода на read-only HTTP API.
Он предназначен для разработчика, который впервые открывает этот сервис.

## 1. Назначение сервиса

`game-service` хранит и отдаёт данные, связанные со спортивными играми:

- каталог видов спорта;
- локальный профиль игрока;
- статистику игрока отдельно по каждому виду спорта;
- результаты конкретной заявки на игру;
- результат конкретного игрока в этой игре;
- рейтинг и изменение рейтинга;
- ранги, вычисляемые из score;
- лидерборд;
- историю матчей.

Сервис не хранит данные авторизации и не знает настоящую бизнес-модель пользователя из Core/Auth Service. Он получает внешний `userId` и связывает его с локальной записью `player_profiles`.

HTTP-интерфейс сервиса сейчас состоит только из шести GET-ручек:

```text
GET /api/v1/players/{userId}
GET /api/v1/players/{userId}/sports/{sportId}
GET /api/v1/players/{userId}/matches?sportId=&cursor=&limit=
GET /api/v1/leaderboards/{sportId}?cursor=&limit=
GET /api/v1/results/by-request/{activityRequestId}
GET /api/v1/rank-tiers
```

HTTP POST для результатов удалён. Результаты игр должны поступать через Kafka. Kafka listener в текущем репозитории ещё не создан: его добавит другой разработчик. Внутренний метод `GameResultService.submitGameResult(...)` оставлен именно как возможная точка вызова из Kafka-консьюмера.

## 2. Как проходит запрос

Почти любой запрос проходит через четыре слоя:

```text
HTTP request
    ↓
Controller
    ↓
Service
    ↓
Repository / native SQL
    ↓
PostgreSQL
```

Контроллер отвечает только за HTTP-маршрут, параметры и тип ответа. Бизнес-правила находятся в сервисах. Репозитории знают, как загрузить Entity или projection из базы.

Entity — это Java-представление таблицы. DTO — объект ответа API. Projection — облегчённое представление строки, возвращённой SQL-запросом.

## 3. Пакеты проекта

### `controllers`

HTTP-вход в приложение.

- `PlayerProfileController` — сводка, статистика по спорту и история игрока.
- `LeaderboardController` — лидерборд.
- `GameResultQueryController` — результат по `activityRequestId`.
- `RankTierController` — статический список порогов рангов.

Контроллера для POST нет.

### `services`

Бизнес-логика:

- `PlayerProfileService` — профиль, статистика и связь `userId → profileId`.
- `GameResultService` — чтение истории/результатов и внутренняя обработка результата из Kafka.
- `LeaderboardService` — лидерборд и cursor-пагинация.

### `repositories`

Spring Data JPA repositories. Часть методов является derived query, часть использует native SQL.

### `entities`

JPA Entity, соответствующие таблицам PostgreSQL.

### `dto`

Объекты API-ответов, а также входная структура для внутренней обработки результата.

### `pagination`

Кодирование и декодирование cursor.

### `ranks`

Статическая Java-константа `RankTiers` с диапазонами score.

### `exceptions`

Доменные исключения и единый HTTP-handler.

## 4. Публичные ручки

### 4.1. Сводка игрока

```text
GET /api/v1/players/{userId}
```

Контроллер: `PlayerProfileController.getSummary`.

Цепочка вызовов:

```text
PlayerProfileController.getSummary(userId)
    → PlayerProfileService.getSummary(userId)
    → PlayerProfileRepository.findByUserId(userId)
    → PlayerSportStatsRepository.findTotalsByProfileId(profile.id)
```

Сначала находится локальный профиль:

```java
playerProfileRepository.findByUserId(userId)
```

Затем выполняется aggregate SQL по `player_sport_stats`:

```sql
SELECT
    COALESCE(SUM(wins), 0) AS wins,
    COALESCE(SUM(losses), 0) AS losses,
    COALESCE(SUM(draws), 0) AS draws,
    COUNT(*) AS "sportsPlayed"
FROM player_sport_stats
WHERE player_profile_id = :profileId;
```

`gamesPlayed` отдельно в таблице не хранится. Сервис вычисляет его так:

```text
gamesPlayed = wins + losses + draws
```

Пример ответа:

```json
{
  "userId": "...",
  "summary": {
    "sportsPlayed": 2,
    "gamesPlayed": 12,
    "wins": 7,
    "losses": 4,
    "draws": 1
  }
}
```

Если профиль не найден, выбрасывается `ResourceNotFoundException`, который превращается в HTTP 404.

### 4.2. Статистика игрока по одному спорту

```text
GET /api/v1/players/{userId}/sports/{sportId}
```

Контроллер: `PlayerProfileController.getSportDetails`.

Цепочка:

```text
PlayerProfileController.getSportDetails(userId, sportId)
    → PlayerProfileService.getSportDetails(userId, sportId)
    → PlayerProfileRepository.findByUserId(userId)
    → SportRepository.findById(sportId)
    → PlayerSportStatsRepository.findByPlayerProfile_IdAndSport_Id(profileId, sportId)
    → PlayerSportStatsRepository.countPlayersAhead(sportId, score)
    → RankTiers.findByScore(score)
```

Derived query статистики:

```text
findByPlayerProfile_IdAndSport_Id(profileId, sportId)
```

SQL для места в лидерборде:

```sql
SELECT COUNT(*)
FROM player_sport_stats
WHERE sport_id = :sportId
  AND score > :score;
```

После этого место вычисляется как:

```text
place = количество игроков с большим score + 1
```

Ранг берётся из статического списка `RankTiers.VALUES`:

```text
scoreMin <= score <= scoreMax
```

Ответ содержит `userId`, спорт, количество игр, победы, поражения, ничьи, score, место и объект rank.

### 4.3. История матчей игрока

```text
GET /api/v1/players/{userId}/matches?sportId=&cursor=&limit=
```

Контроллер: `PlayerProfileController.getMatches`.

Цепочка:

```text
PlayerProfileController.getMatches(...)
    → GameResultService.getPlayerMatches(userId, sportId, cursor, limit)
    → PlayerProfileService.getProfileId(userId)
    → GameResultService.getPlayerHistory(profileId, sportId, cursor, limit)
    → CursorService.decode(cursor)
    → PlayerGameResultRepository.findHistory(...)
    → PlayerHistoryRowProjection
    → PlayerGameHistoryDto
```

`userId` из URL не является `player_profiles.id`. Поэтому сначала выполняется:

```java
playerProfileService.getProfileId(userId)
```

Native SQL истории:

```sql
SELECT
    gr.id AS "gameResultId",
    gr.sport_id AS "sportId",
    pgr.outcome::text AS outcome,
    pgr.rating_change AS "ratingChange",
    gr.created_at AS "createdAt"
FROM player_game_results pgr
JOIN game_results gr ON gr.id = pgr.game_result_id
WHERE pgr.player_profile_id = :profileId
  AND (
      CAST(:sportId AS bigint) IS NULL
      OR gr.sport_id = CAST(:sportId AS bigint)
  )
  AND (
      CAST(:cursorCreatedAt AS timestamptz) IS NULL
      OR gr.created_at < CAST(:cursorCreatedAt AS timestamptz)
      OR (
          gr.created_at = CAST(:cursorCreatedAt AS timestamptz)
          AND gr.id < CAST(:cursorGameResultId AS uuid)
      )
  )
ORDER BY gr.created_at DESC, gr.id DESC
LIMIT :limit;
```

Сервис передаёт в SQL `limit + 1`, а клиенту возвращает не больше `limit` строк. Дополнительная строка нужна только для вычисления `hasNext`.

Пример:

```text
limit = 20
SQL LIMIT = 21
21 строк найдено → клиент получает 20, hasNext=true
20 или меньше найдено → hasNext=false
```

Cursor истории привязан к:

- типу cursor;
- `playerProfileId`;
- выбранному `sportId` или отсутствию фильтра;
- `createdAt` последнего элемента страницы;
- `gameResultId` последнего элемента страницы.

Это не позволяет использовать cursor одного игрока или одного фильтра для другого запроса.

### 4.4. Лидерборд

```text
GET /api/v1/leaderboards/{sportId}?cursor=&limit=
```

Контроллер: `LeaderboardController.getLeaderboard`.

Цепочка:

```text
LeaderboardController.getLeaderboard(...)
    → LeaderboardService.getLeaderboard(sportId, cursor, limit)
    → SportRepository.findById(sportId)
    → CursorService.decode(cursor)
    → PlayerSportStatsRepository.findLeaderboard(...)
    → LeaderboardRowProjection
    → RankTiers.findByScore(score)
    → LeaderboardEntryDto
```

SQL использует CTE и оконную функцию:

```sql
WITH ranked AS (
    SELECT
        pss.player_profile_id AS player_profile_id,
        pp.user_id AS user_id,
        pss.score AS score,
        pss.wins AS wins,
        pss.losses AS losses,
        pss.draws AS draws,
        RANK() OVER (ORDER BY pss.score DESC) AS place
    FROM player_sport_stats pss
    JOIN player_profiles pp ON pp.id = pss.player_profile_id
    WHERE pss.sport_id = :sportId
)
SELECT ...
FROM ranked
WHERE ...cursor conditions...
ORDER BY score DESC, player_profile_id ASC
LIMIT :limit;
```

В коде этот сокращённый фрагмент записан полностью так:

```sql
SELECT
    player_profile_id AS "playerProfileId",
    user_id AS "userId",
    score AS score,
    wins AS wins,
    losses AS losses,
    draws AS draws,
    place AS place
FROM ranked
WHERE
    CAST(:cursorScore AS integer) IS NULL
    OR score < CAST(:cursorScore AS integer)
    OR (
        score = CAST(:cursorScore AS integer)
        AND player_profile_id > CAST(:cursorProfileId AS uuid)
    )
ORDER BY score DESC, player_profile_id ASC
LIMIT :limit;
```

`RANK()` означает, что игроки с одинаковым score занимают одно место. Вторичная сортировка по `player_profile_id` нужна для стабильного cursor.

Cursor лидерборда содержит:

- тип `leaderboard`;
- `sportId`;
- score последнего элемента;
- `playerProfileId` последнего элемента.

Сервис ограничивает `limit` диапазоном от 1 до 100.

### 4.5. Результат по заявке

```text
GET /api/v1/results/by-request/{activityRequestId}
```

Контроллер: `GameResultQueryController.getByActivityRequest`.

Цепочка:

```text
GameResultQueryController.getByActivityRequest(activityRequestId)
    → GameResultService.getGameResultByActivityRequestId(activityRequestId)
    → GameResultRepository.findByActivityRequestId(activityRequestId)
    → PlayerGameResultRepository.findAllByGameResultId(gameResult.id)
    → GameResultDetailsDto
```

`activityRequestId` — идентификатор заявки, пришедший из другого сервиса. `gameResult.id` — внутренний идентификатор записи результата в Game Service. Это разные сущности.

Ответ содержит:

- `id` результата;
- `activityRequestId`;
- `sportId`;
- `status`;
- `createdAt`;
- список игроков с `userId`, `outcome`, `ratingChange`.

### 4.6. Пороги рангов

```text
GET /api/v1/rank-tiers
```

Контроллер: `RankTierController`.

Данные не читаются из базы. Они находятся в Java-константе:

```java
RankTiers.VALUES
```

Каждый элемент преобразуется в `RankTierDto` с полями:

- `name`;
- `tier`;
- `scoreMin`;
- `scoreMax`.

## 5. Внутренняя обработка результата из Kafka

HTTP POST для этого сценария удалён. В `GameResultService` оставлен метод:

```java
UUID submitGameResult(SubmitGameResultDto dto)
```

Его предполагаемый будущий вызывающий код — Kafka listener.

Алгоритм метода:

1. Проверить, что список игроков не пуст.
2. Проверить, что один пользователь не указан дважды.
3. Проверить, что для `activityRequestId` ещё нет результата.
4. Найти спорт.
5. Проверить `minPlayers` и `maxPlayers`.
6. Создать `GameResult` со статусом `CONFIRMED`.
7. Для каждого пользователя получить или создать `PlayerProfile`.
8. Для каждого пользователя получить или создать `PlayerSportStats`.
9. Рассчитать изменение рейтинга.
10. Сохранить `PlayerGameResult`.
11. Увеличить wins/losses/draws.
12. Обновить score и сохранить статистику.

Метод выполняется в одной транзакции `@Transactional`. Если сохранение одной из строк завершится ошибкой, транзакция откатывается.

Сейчас `calculateRatingChange(...)` является заглушкой и возвращает `0`. Поэтому score пока не меняется. Это согласованное временное состояние до утверждения формулы рейтинга.

## 6. Таблицы базы данных

Авторитетная схема создаётся Flyway-миграцией:

```text
src/main/resources/db/migration/V1__initial_schema.sql
```

`docs/current-schema.sql` — человекочитаемая копия структуры. При расхождении использовать Flyway-файл.

### `sports`

Каталог видов спорта.

| Поле | Значение |
|---|---|
| `id` | `BIGSERIAL`, первичный ключ |
| `name` | уникальное имя |
| `min_players` | минимальное число игроков |
| `max_players` | максимальное число игроков |
| `icon_url` | необязательная иконка |

Entity: `Sport`.

### `player_profiles`

Связь между внешним пользователем и локальным игровым профилем.

| Поле | Значение |
|---|---|
| `id` | внутренний UUID профиля |
| `user_id` | UUID пользователя из Auth/Core Service, уникальный |

Entity: `PlayerProfile`.

Один `user_id` соответствует максимум одному `player_profiles.id`.

### `game_results`

Одна запись на завершённую заявку/игру.

| Поле | Значение |
|---|---|
| `id` | внутренний UUID результата |
| `activity_request_id` | ID заявки из внешнего сервиса, уникальный |
| `sport_id` | FK на `sports.id` |
| `status` | PostgreSQL enum `PENDING/CONFIRMED/DISPUTED` |
| `created_at` | время обработки результата |

`activity_request_id` не является внешним SQL FK, потому что заявка хранится в другом сервисе.

Entity: `GameResult`.

### `player_game_results`

Одна строка на одного игрока внутри одной игры.

| Поле | Значение |
|---|---|
| `id` | UUID строки результата игрока |
| `game_result_id` | FK на `game_results.id` |
| `player_profile_id` | FK на `player_profiles.id` |
| `outcome` | `WIN`, `LOSS` или `DRAW` |
| `rating_change` | изменение score после игры |

Уникальная пара `(game_result_id, player_profile_id)` не позволяет записать одного игрока дважды в одну игру.

Entity: `PlayerGameResult`.

### `player_sport_stats`

Агрегированная статистика одного профиля по одному виду спорта.

| Поле | Значение |
|---|---|
| `id` | UUID строки статистики |
| `player_profile_id` | FK на `player_profiles.id` |
| `sport_id` | FK на `sports.id` |
| `wins` | победы |
| `losses` | поражения |
| `draws` | ничьи |
| `score` | текущие очки рейтинга |

Уникальная пара `(player_profile_id, sport_id)` гарантирует одну статистику игрока на один спорт.

Entity: `PlayerSportStats`.

`games_played`, `total_points`, `rating`, `place`, `created_at` и `updated_at` в этой модели не хранятся.

## 7. Связи таблиц

```text
sports 1 ──── N player_sport_stats
sports 1 ──── N game_results

player_profiles 1 ──── N player_sport_stats
player_profiles 1 ──── N player_game_results

game_results 1 ──── N player_game_results
```

Полный путь результата игрока:

```text
user_id
  → player_profiles.id
  → player_game_results.player_profile_id
  → player_game_results.game_result_id
  → game_results.activity_request_id
```

## 8. Репозитории и их методы

### `PlayerProfileRepository`

```java
Optional<PlayerProfile> findByUserId(UUID userId)
```

Находит профиль по внешнему ID пользователя.

### `SportRepository`

Наследует стандартные методы `JpaRepository<Sport, Long>`:

```java
findById(sportId)
```

Используется для проверки спорта и его ограничений.

### `GameResultRepository`

```java
Optional<GameResult> findByActivityRequestId(UUID activityRequestId)
```

Используется для поиска результата по заявке и для проверки повторной обработки Kafka-события.

### `PlayerSportStatsRepository`

```java
Optional<PlayerSportStats> findByPlayerProfile_IdAndSport_Id(...)
```

Получает статистику конкретного игрока по спорту.

```java
PlayerTotalsProjection findTotalsByProfileId(UUID profileId)
```

Возвращает aggregate-статистику для сводки профиля.

```java
long countPlayersAhead(Long sportId, Integer score)
```

Считает игроков с большим score.

```java
List<LeaderboardRowProjection> findLeaderboard(...)
```

Возвращает страницу лидерборда через native SQL и `RANK()`.

### `PlayerGameResultRepository`

```java
List<PlayerGameResult> findAllByGameResultId(UUID gameResultId)
```

Возвращает всех игроков в конкретном результате.

```java
List<PlayerHistoryRowProjection> findHistory(...)
```

Возвращает cursor-страницу истории через native SQL.

## 9. Projection

### `PlayerTotalsProjection`

Колонки aggregate-запроса:

- `wins`;
- `losses`;
- `draws`;
- `sportsPlayed`.

### `LeaderboardRowProjection`

Колонки SQL-лидерборда:

- `playerProfileId` — нужен cursor, наружу не возвращается;
- `userId`;
- `score`;
- `wins`;
- `losses`;
- `draws`;
- `place`.

### `PlayerHistoryRowProjection`

Колонки SQL-истории:

- `gameResultId`;
- `sportId`;
- `outcome` как строка PostgreSQL enum;
- `ratingChange`;
- `createdAt`.

Затем `outcome` преобразуется в Java enum `GameOutcome`.

## 10. DTO

- `PlayerSummaryDto` — общая статистика игрока по всем видам спорта.
- `PlayerSportDetailsDto` — статистика, место и rank по одному спорту.
- `PlayerGameHistoryDto` — одна строка истории матча.
- `GameResultDetailsDto` — результат заявки и результаты всех игроков.
- `LeaderboardEntryDto` — одна строка лидерборда.
- `RankDto` — текущий rank игрока.
- `RankTierDto` — порог из справочника рангов.
- `CursorPageDto<T>` — универсальная страница с `items`, `nextCursor`, `hasNext`.
- `SubmitGameResultDto` — внутренний вход для будущего Kafka-консьюмера.

Старые `PlayerProfileDto` и `PlayerSportStatsDto` удалены, потому что старый endpoint профиля больше не существует.

## 11. Cursor-пагинация

Cursor — это непрозрачная строка, которую сервер возвращает клиенту для следующей страницы. Клиент не должен самостоятельно разбирать или изменять её.

`CursorService` делает следующее:

1. сериализует record cursor в JSON;
2. кодирует JSON в URL-safe Base64 без padding;
3. при следующем запросе декодирует Base64;
4. восстанавливает record;
5. сервис проверяет тип и область действия cursor.

Для лидерборда область действия — `sportId`.

Для истории область действия — `playerProfileId` и выбранный `sportId`.

При ошибочном cursor выбрасывается `InvalidCursorException`, который превращается в HTTP 400.

## 12. Ошибки и HTTP-коды

`GlobalExceptionHandler` преобразует исключения так:

| Исключение | HTTP |
|---|---:|
| `ResourceNotFoundException` | 404 |
| `InvalidCursorException` | 400 |
| `ConflictException` | 409 |
| `DataIntegrityViolationException` | 409 |
| `ConstraintViolationException` | 400 |
| `IllegalArgumentException` | 400 |
| `MethodArgumentNotValidException` | 400 |

## 13. Ранги и рейтинг

Ранги задаются в `RankTiers`:

```text
BRONZE  0..999
SILVER  1000..1199
GOLD    1200..1399
DIAMOND 1400..1599
MYTHIC  1600..1799
LEGEND  1800..1999
MASTER  2000..Integer.MAX_VALUE
```

Сейчас все записи используют `tier = 1`, поскольку точная внутренняя формула уровней ещё не утверждена.

Начальный score:

```java
RankTiers.INITIAL_SCORE = 0
```

Формула `calculateRatingChange(...)` пока возвращает `0`. Это заглушка, поэтому реальные изменения score ещё не происходят.

## 14. Конфигурация и запуск

Основная конфигурация находится в `src/main/resources/application.yaml`.

По умолчанию:

```text
database: jdbc:postgresql://localhost:5432/game_db
user: ${DB_USER:postgres}
password: ${DB_PASSWORD:postgres}
port: 8082
```

Hibernate работает в режиме:

```text
ddl-auto: validate
```

Это означает, что Hibernate только проверяет схему, но не создаёт и не изменяет таблицы.

Таблицы создаёт Flyway из:

```text
src/main/resources/db/migration/V1__initial_schema.sql
```

Для запуска через Docker используется `Dockerfile`, который собирает проект Maven и запускает JAR на порту 8082.

## 15. Проверка проекта

Запуск тестов из IntelliJ IDEA или Maven:

```text
mvn test
```

Тесты проверяют:

- границы и поиск `RankTiers`;
- обработку результата игры;
- запрет повторного игрока;
- неизвестный спорт;
- ограничения количества игроков;
- `limit + 1` для истории;
- `limit + 1` и rank для лидерборда;
- сводку профиля;
- статистику игрока по спорту.

## 16. Что важно знать новому разработчику

1. Не добавляйте `@Column` в Entity без необходимости. Схема управляется Flyway.
2. Не путайте `userId` и `playerProfileId`.
3. Не путайте `activityRequestId` и `gameResultId`.
4. Не возвращайте Entity непосредственно из контроллера — используйте DTO.
5. Не передавайте cursor от одного игрока, спорта или фильтра другому.
6. Для больших списков используйте cursor-пагинацию и запрашивайте `limit + 1`.
7. Новые входящие результаты должны приходить через Kafka, а не через HTTP POST.
8. Перед изменением таблиц редактируйте Flyway-миграцию согласно правилам окружения. Сейчас проект находится до production, поэтому используется единая V1-миграция.

## 17. Известные ограничения

- Kafka listener в этом проекте ещё отсутствует.
- Формула изменения рейтинга является заглушкой.
- Автоматизированных repository integration tests с PostgreSQL пока нет; native SQL следует дополнительно проверять на тестовой базе.
- Cursor кодируется Base64 и не подписывается. Он непрозрачен для обычного клиента, но не является криптографически защищённым токеном.
- При изменении формата cursor старые cursor станут недействительными — это нормально для текущего MVP без production-данных.
