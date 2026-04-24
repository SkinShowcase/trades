# trades (Skins Showcase)

Сервис «витрины обмена»:

- хранит **набор предметов** (до 5) на пользователя;
- валидирует, что предметы реально есть в инвентаре CS2 через `steam-gateway`;
- обогащает предметы данными/ценами через `items`;
- учитывает **приватность профиля** через internal API `auth`.

Репозиторий: https://github.com/SkinShowcase/trades  
Инфраструктура (compose): https://github.com/SkinShowcase/infrastructure

## Порт

- По умолчанию **8083** (`SERVER_PORT` в `application.yml`)

## Хранение SteamID в БД (hash + encryption at rest)

Сущность `TradeSelection` хранит:

- `steam_id` — **SHA-256 hex** SteamID64 (см. `SteamIdProtectionService.hash`)
- `steam_id_enc` — **AES-GCM** от SteamID64 (base64 payload; см. `SteamIdProtectionService.encrypt`)

Наружу API отдаёт **настоящий SteamID64** владельца.

Ключ: `TRADES_STEAM_ID_CRYPTO_KEY` — **base64 32 байта** (см. `application.yml` → `app.steam-id-protection.key`).

## JWT и владелец набора

Используется тот же `AUTH_JWT_SECRET`, что в `auth`/`messaging`.

Фильтр `TradesOwnerJwtFilter` защищает мутации набора для владельца (путь совпадает с `/api/v1/trades/selection/{steamId}` или `.../items`):

- `PUT /api/v1/trades/selection/{steamId}` — требуется Bearer и `sub == steamId`
- `DELETE /api/v1/trades/selection/{steamId}` — требуется Bearer и `sub == steamId`
- `DELETE /api/v1/trades/selection/{steamId}/items` — требуется Bearer и `sub == steamId`

## REST API (актуально)

Источник: `TradeSelectionController`.

- `PUT /api/v1/trades/selection/{steamId}` — upsert набора (макс 5 предметов; проверка инвентаря)
- `GET /api/v1/trades/selection/{steamId}` — получить набор; для чужих при приватном профиле — **404**, владелец может приложить `Authorization`
- `GET /api/v1/trades/showcase/{steamId}` — то же по смыслу, отдельный путь для витрины
- `DELETE /api/v1/trades/selection/{steamId}` — удалить набор целиком (`204`)
- `DELETE /api/v1/trades/selection/{steamId}/items` — удалить часть предметов из набора
- `GET /api/v1/trades/feed` — пагинируемая лента наборов (`excludeSteamId` опционально)
- `GET /api/v1/trades/feed/sets/filtered` — лента с фильтрами по предметам (float/name/price/special и т.д.)

## Интеграции

| Переменная | Зачем |
|------------|------|
| `STEAM_GATEWAY_BASE_URL` | Проверка инвентаря |
| `ITEMS_BASE_URL` | Каталог/цены/обогащение |
| `AUTH_SERVICE_BASE_URL` + `AUTH_INTERNAL_SERVICE_KEY` | Приватность (`/auth/internal/users/...`) |

## Наблюдаемость

- `/actuator/health`, `/actuator/prometheus`
- `/swagger-ui.html`, `/api-docs`

## Запуск локально

```bash
./gradlew bootRun
# или:
./gradlew bootRun --args="--spring.profiles.active=local"
```

## Docker

```bash
docker build -t skins-showcase/trades .
docker run --rm -p 8083:8083 -e SERVER_PORT=8083 -e SPRING_PROFILES_ACTIVE=docker skins-showcase/trades
```
