# ADR 001: Stack technologiczny i architektura

**Status:** Zaakceptowany
**Data:** 2026-11-24

## Kontekst

KMA Larder to trzecia aplikacja domowa po [kma-poker-stats](https://github.com/krzycuh/kma-poker-stats) i [kma-library](https://github.com/krzycuh/kma-library). Ma działać na tym samym Raspberry Pi, być utrzymywana przez tę samą osobę i korzystać z tych samych narzędzi, żeby wiedza, skrypty i pipeline'y przenosiły się 1:1. Sednem aplikacji jest sygnalizacja świeżości zapasów i szybkie wprowadzanie produktów z telefonu (skaner kodów, OCR dat).

## Decyzja

### Stack identyczny z kma-library

- **Frontend:** React 18 + TypeScript + Vite + Tailwind CSS + TanStack Query + React Hook Form + Zod + react-router; Vitest + Testing Library; Playwright; PWA. Dodatkowo: `tesseract.js` (OCR), `barcode-detector` + `zxing-wasm` (kody).
- **Backend:** Kotlin + Spring Boot 3.x (Web, Security, Validation, Data JPA, Actuator), JWT (access + refresh), Flyway, springdoc-openapi, bucket4j, `@EnableScheduling`; JUnit 5 + MockK + ArchUnit + MockWebServer; Gradle Kotlin DSL, JDK 21, ktlint.
- **Baza:** PostgreSQL 16.
- **Infra:** Docker multi-stage + `Dockerfile.prebuilt`, docker-compose (`localhost`, `prod`), nginx wbudowany w obraz frontendu (SPA + proxy `/api`), GitHub Actions (CI + obrazy do GHCR), Raspberry Pi z danymi na SSD, port 8030.

Uzasadnienie: zero kosztu nauki, kopiowanie całych modułów (auth, layout, CI, deploy, backup), spójne operacje dla trzech aplikacji. Trzecia kopia szkieletu oznacza rozjazd między aplikacjami (poprawka w jednej nie trafia do pozostałych); akceptujemy to świadomie zamiast wydzielać wspólną bibliotekę, której utrzymanie kosztowałoby więcej niż trzy kopie.

### Odstępstwa od kma-library

| Element | kma-library | kma-larder | Powód |
|---------|-------------|------------|-------|
| Wspólny katalog | `Book` współdzielony między użytkownikami, scalanie duplikatów przez admina | `Product` należy do spiżarni | gospodarstwo ma własne nazwy („Ogórki babci”); bez katalogu nie ma polityki edycji ani scalania |
| Katalogi zewnętrzne | łańcuch trzech providerów ISBN | jeden `ProductMetadataProvider` (Open Food Facts) | wystarcza; to tylko podpowiedź nazwy |
| OCR | brak | w przeglądarce, bez udziału backendu | ADR 003 |
| Powiadomienia | `NoOp` | ntfy + tygodniowy raport z `@Scheduled` | ntfy już działa na Pi; „zużywać na czas” wymaga przypomnienia |
| Zegar | `LocalDate.now()` | bean `Clock` (Europe/Warsaw) w use case'ach | terminy liczone w dniu gospodarstwa, testy z zegarem stałym |
| Listowanie | paginacja w SQL | sortowanie i stronicowanie w pamięci dla listy produktów | kilkaset produktów, sortowanie po zagregowanym terminie; prościej niż podzapytania w JPQL |
| Nazwy | `Library`, `ShelfItem`, `Loan` | `Pantry`, `Product`, `Batch`, `StockMovement` | domena |
| Motyw | niebieski / neon | terakota / piwnica, zielony-bursztynowy-czerwony tylko dla świeżości | patrz niżej |

### Architektura: DDD + Clean Architecture, trzy bounded contexts

```
pl.kmazurek.larder
├── domain/            # model/{user,pantry,stock}, repository/, service/PantryAccessPolicy
├── application/       # usecase/{auth,user,pantry,stock,notification}, dto/, service/ (porty: ProductMetadataProvider, NotificationService)
├── infrastructure/    # persistence/, api/rest/, security/, catalog/ (Open Food Facts), notification/ (ntfy)
└── config/
```

Kluczowe decyzje modelu:

1. **Produkt opisuje, partia liczy.** `Product` to słownik spiżarni (nazwa, rodzaj, kategoria, kod, opakowanie, przydatność, minimum). Sztuki są w `Batch` z jedną datą. Stan = suma partii; status świeżości produktu = najgorszy z partii.
2. **Data z taką precyzją, jaką ma opakowanie.** `ApproximateDate(year, month?, day?)` zamiast `LocalDate`; porównania po ostatnim dniu okresu. `expiresOn` jest zapisywany w partii, żeby SQL sortował i filtrował.
3. **Domowe przetwory: data zrobienia + przydatność produktu.** Termin wynika z `producedOn.plusMonths(defaultShelfLifeMonths)`; zmiana przydatności przelicza partie z datą wyliczoną, nie rusza dat z opakowania.
4. **Historia jako zdarzenia.** `StockMovement` (dodano / zużyto / wyrzucono / korekta) zamiast licznika: odpowiada na „ile słoików rocznie”, czyli ile zrobić w przyszłym roku.
5. **Pełny schemat w `V1`.** Kolejne migracje tylko dla rozszerzeń.
6. **Dostęp w jednym miejscu.** `PantryAccessPolicy` + `PantryAccessGuard`; każdy use case stocku ładuje produkt / partię przez `StockAccess`, które sprawdza członkostwo.

### Kolory

Zielony, bursztynowy i czerwony są zarezerwowane dla statusu świeżości (w porządku / kończy się / po terminie) **w obu motywach**, bo to jest sedno aplikacji. Akcent (w klasach Tailwinda nadal `blue-*`, przez zmienne CSS) to terakota; neutralne są ciepłe (stone, krem zamiast bieli). Ciemny motyw „piwnica” to brązy z bursztynowym blaskiem, nie neon. Mechanizm z kma-library (`theme.css` + `tailwind.config.js`) skopiowany bez zmian, mapowanie kolorów od nowa.

## Konsekwencje

- Plus: szybki start, jedna metoda utrzymania trzech aplikacji, model gotowy na lokalizacje i statystyki bez przebudowy.
- Minus: trzeci JVM na Pi (~400–600 MB RAM). Przy trzech stosach warto zmierzyć `docker stats` i rozważyć wspólnego Postgresa (osobne bazy).
- Minus: brak wspólnego katalogu produktów oznacza, że ta sama puszka w dwóch spiżarniach to dwa produkty. Dla jednego gospodarstwa bez znaczenia.
