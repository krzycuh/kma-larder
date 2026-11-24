# Fazy 0–5 – podsumowanie realizacji

Data: 2026-11-24. Zakres: szkielet, domena spiżarni, frontend, EAN + Open Food Facts, OCR dat, powiadomienia ntfy.

## Co powstało

### Backend (`backend/`, pakiet `pl.kmazurek.larder`)
- Szkielet z kma-library: Gradle (JDK 21, ktlint, JaCoCo), Spring Boot 3.3, Security + JWT, Flyway, springdoc, bucket4j, request logging, audit log, `GlobalExceptionHandler`, podgląd konta przez admina.
- Konteksty: `User`; `Pantry` / `PantryMember` / `PantryAccessPolicy`; `Product` (+ `ProductName`, `Barcode`), `Batch`, `StockMovement`, `ApproximateDate`, `Freshness`.
- Use case'y: auth (rejestracja tworzy spiżarnię), user/admin, pantry (domownicy, nazwa), stock (`ListProducts`, `SearchProducts`, `GetProduct`, `CreateProduct`, `UpdateProduct`, `DeleteProduct`, `AddBatch`, `UpdateBatch`, `ConsumeBatch`, `DiscardBatch`, `AdjustBatch`, `RemoveBatch`, `ListExpiring`, `ListReplenishment`, `ListMovements`, `GetDashboard`, `LookupBarcode`), notification (`SendExpiryDigests`, `ExpiryDigestScheduler`).
- Adaptery: `OpenFoodFactsProductMetadataProvider` (+ cache, ponowienie przy 5xx), `NtfyNotificationService` / `NoOpNotificationService`.
- Persystencja: encje JPA osobno od domeny, mappery; `V1__initial_schema.sql` (pełny schemat, `pg_trgm`, indeks `(pantry_id, expires_on)`), `V2__insert_admin_user.sql`.
- REST: `/api/auth`, `/api/users/me`, `/api/admin/users`, `/api/admin/notifications/digest`, `/api/pantries/{id}/{members,products,products/search,products/lookup,expiring,replenishment,movements,dashboard}`, `/api/products/{id}[/batches]`, `/api/batches/{id}[/consume|/discard|/adjust]`.
- Testy: 101 (domena, use case'y na repozytoriach in-memory, adaptery na MockWebServer, ArchUnit) – `./gradlew test`.

### Frontend (`frontend/`)
- Szkielet z kma-library (Vite 6, React 18, Tailwind, TanStack Query, PWA, motyw jasny / ciemny), teksty PL w `src/i18n/pl.ts`.
- Strony: Start, Stan, karta produktu, edycja produktu, edycja partii, Dodaj do spiżarni (autouzupełnianie, skaner EAN, OCR daty), Terminy, Do uzupełnienia, Domownicy, Profil, Admin, Login / Rejestracja.
- Komponenty: `ApproximateDateInput`, `QuantityStepper`, `QuantityModal`, `FreshnessBadge`, `BatchCard` + `BatchActions`, `ProductPicker`, `ProductFields`, `BarcodeScanner`, `OcrDateDialog`.
- OCR: `lib/ocr.ts` (Tesseract.js, zasoby z `/ocr`), `utils/dateOcr.ts` (parser dat), `scripts/copy-ocr-assets.mjs`.
- Testy: Vitest (51: daty, parser OCR, kody, format, hooki), Playwright `e2e/larder-flow.spec.ts` (dodanie partii → zużycie → terminy → lista zakupów) na pełnym stacku.

### Infrastruktura
- `docker-compose.localhost.yml` (Postgres), `docker-compose.prod.yml` (Postgres, backend, frontend na 8030) z konfiguracją katalogu i ntfy; `.env.production.template`; nginx z CSP `'wasm-unsafe-eval'` (skaner, OCR), cache `/ocr/`.
- GitHub Actions: `backend-ci.yml`, `frontend-ci.yml`, `deploy-production.yml` (obrazy multi-arch do GHCR) z powiadomieniami ntfy.

## Sprawdzone
- Backend na lokalnym PostgreSQL 16: migracje, walidacja schematu, pełny przebieg API (produkty, partie, zużycie, 409 przy duplikacie i braku sztuk, terminy, lista zakupów, historia, dashboard, przeliczenie terminu po zmianie przydatności).
- Frontend w Chromium (390 px i 1280 px, oba motywy): zrzuty ekranu stron; OCR na syntetycznej etykiecie „Najlepiej spożyć przed: 12.05.2027” rozpoznany i wpisany w pola daty; zasoby OCR ładowane z własnego origin.

## Uwagi
- Fixture'y Open Food Facts w `src/test/resources/catalog/openfoodfacts/` odwzorowują udokumentowany format API v2; środowisko, w którym powstawała faza 3, nie miało dostępu do `world.openfoodfacts.org`. Warto podmienić je na prawdziwe odpowiedzi przy pierwszej okazji.
- Usunięcie produktu usuwa też jego historię (kaskada); archiwizacja ją zachowuje – stąd ostrzeżenie w UI.
- `plusMonths` dla daty z samym rokiem zaokrągla w górę do pełnych lat („2026” + 6 miesięcy = „2027”): bezpieczniej za późno przypomnieć niż stracić rocznik.
- Do sprawdzenia ręcznie na telefonach: aparat w PWA na iOS (zgoda, autofocus z bliska), jakość OCR na puszkach z nadrukiem igłowym i na wieczkach.
