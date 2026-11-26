# KMA Larder

Aplikacja webowa (mobile-first, PWA) do śledzenia domowej spiżarni: ile czego jest i kiedy kończy się termin ważności, żeby zużywać na czas i uzupełniać zapasy. Techniczny bliźniak [kma-library](https://github.com/krzycuh/kma-library): ten sam stack, te same konwencje i sposób wdrożenia.

## Funkcje

- **Stan** – produkty spiżarni (kupne i domowe) z liczbą sztuk, kategorią, wielkością opakowania; filtry i sortowanie po nazwie, terminie, ilości.
- **Partie** – sztuki tego samego produktu z tą samą datą: termin „najlepiej spożyć przed” z opakowania (sam rok, miesiąc i rok albo pełna data) albo data zrobienia przetworów, z której termin wynika z przydatności produktu. Zużyj 1 / zużyj kilka / wyrzuć / popraw stan jednym tapnięciem.
- **Terminy** – wszystkie partie od najpilniejszej: po terminie, kończy się (domyślnie 30 dni), w porządku, bez terminu.
- **Do uzupełnienia** – lista zakupów z produktów poniżej minimalnego zapasu.
- **Historia** – każda zmiana (dodano, zużyto, wyrzucono, korekta) z osobą i datą; zużycie roczne na karcie produktu („ile słoików dżemu schodzi w roku”).
- **Dodawanie** – autouzupełnianie z własnych produktów, skaner kodów EAN z podpowiedzią nazwy, kategorii i miniatury z Open Food Facts (miniatura to tylko link, aplikacja nie przechowuje zdjęć), **OCR daty ważności ze zdjęcia** (w telefonie, bez wysyłania zdjęcia), duże przyciski ilości.
- **Powiadomienia** – tygodniowy raport na ntfy: co po terminie, co kończy się wkrótce, czego brakuje.
- **Domownicy** – spiżarnia wspólna dla gospodarstwa: właściciel dodaje domowników po e-mailu.
- **Konta** – rejestracja (wyłączalna flagą), logowanie JWT z refresh tokenem, profil, panel admina (blokada konta, reset hasła, podgląd konta użytkownika tylko do odczytu).

## Stack

- **Frontend:** React 18 + TypeScript + Vite + Tailwind CSS + TanStack Query + React Hook Form + Zod; Tesseract.js (OCR w przeglądarce), ZXing-WASM / BarcodeDetector (kody kreskowe); Vitest, Playwright; PWA
- **Backend:** Kotlin + Spring Boot 3 + Spring Security (JWT) + Spring Data JPA + Flyway; JUnit 5 + MockK + ArchUnit; ktlint
- **Baza:** PostgreSQL 16
- **Infra:** Docker + Docker Compose (nginx wbudowany w obraz frontendu serwuje SPA i proxy'uje `/api`) + GitHub Actions (obrazy w GHCR), produkcja na Raspberry Pi

## Struktura

```
kma-larder/
├── backend/     # Kotlin + Spring Boot (pakiet pl.kmazurek.larder: domain / application / infrastructure / config)
├── frontend/    # React + TypeScript (src/pages, components, api, types, lib/ocr, i18n)
├── docs/        # projekt funkcjonalny, plan, ADR, podsumowania faz
├── scripts/     # backup / restore / admin
└── .github/workflows/
```

## Szybki start (lokalnie)

```bash
docker compose -f docker-compose.localhost.yml up -d   # PostgreSQL
cd backend && ./gradlew bootRun                         # http://localhost:8080 (Swagger: /swagger-ui.html)
cd frontend && npm install && npm run dev               # http://localhost:5173
```

Domyślny administrator: `admin@example.com` / `admin123` (zmień hasło po pierwszym logowaniu). Adres i nazwę konta
ustawisz przed pierwszym startem zmiennymi `APP_ADMIN_EMAIL` / `APP_ADMIN_NAME` (patrz `.env.production.template`).

## Testy

```bash
cd backend && ./gradlew ktlintCheck test     # testy jednostkowe + ArchUnit
cd frontend && npm run lint && npm run test  # ESLint + Vitest
cd frontend && npm run test:e2e              # Playwright, wymaga działającego backendu + bazy
```

## Wdrożenie

Produkcja to trzy kontenery: PostgreSQL, backend (bez publikowanego portu) i frontend wystawiony na porcie 8030 (`FRONTEND_PORT`), którego nginx serwuje SPA i proxy'uje `/api` do backendu. Aparat (skaner, OCR) wymaga HTTPS. Zobacz [prod-deploy.md](prod-deploy.md), [ADR 002 – HTTPS](docs/adr/002-https.md) i [docs/NTFY_NOTIFICATIONS.md](docs/NTFY_NOTIFICATIONS.md).

## Dokumentacja

- [Projekt funkcjonalny i model domeny](docs/FUNCTIONALITY_DESIGN.md)
- [Plan implementacji](docs/IMPLEMENTATION_PLAN.md) – fazy 0–5 i podjęte decyzje
- [ADR 001 – stack i architektura](docs/adr/001-tech-stack-and-architecture.md), [ADR 002 – HTTPS](docs/adr/002-https.md), [ADR 003 – OCR w przeglądarce](docs/adr/003-ocr-in-browser.md)
- [Podsumowanie faz 0–5](docs/PHASE_0-5_SUMMARY.md)

## Status

Fazy 0–5 zrealizowane: działające MVP (auth, stan, partie, terminy, lista zakupów, skaner EAN + Open Food Facts, OCR dat, raport ntfy, domownicy, PWA, CI, konfiguracja wdrożenia).
