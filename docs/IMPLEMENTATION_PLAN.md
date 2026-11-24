# KMA Larder – plan implementacji

Plan pracy nad aplikacją do śledzenia domowej spiżarni. Projekt jest technicznym „klonem” [kma-library](https://github.com/krzycuh/kma-library): ten sam stack, te same konwencje, ten sam sposób wdrożenia. Opis funkcjonalny i model domeny: [FUNCTIONALITY_DESIGN.md](FUNCTIONALITY_DESIGN.md). Decyzje architektoniczne: [adr/](adr/).

**Status:** fazy 0–5 zrealizowane (patrz [PHASE_0-5_SUMMARY.md](PHASE_0-5_SUMMARY.md)). Każda faza kończy się działającą, wdrażalną wersją.

---

## Zasady pracy (jak w kma-library)

- DDD + Clean Architecture: `domain/` (czysty Kotlin, bez Springa), `application/` (use case'y, DTO, porty), `infrastructure/` (JPA, REST, adaptery), `config/`. Pilnowane testem ArchUnit.
- Domena jako niemutowalne `data class` z metodami zwracającymi kopię, ID jako `@JvmInline value class` na UUID, encje JPA osobno od domeny + mappery.
- Jeden use case = jedna klasa `@Service` z metodą `execute(...)`; wyjątki domenowe per moduł, mapowane w `GlobalExceptionHandler`.
- TDD dla domeny i use case'ów; testy use case'ów na repozytoriach in-memory (`StockTestSupport`), adaptery HTTP na MockWebServer; frontend: Vitest dla utili, Playwright dla ścieżki głównej.
- Nazewnictwo: pakiet `pl.kmazurek.larder`, obrazy `ghcr.io/krzycuh/kma-larder/{backend,frontend}`, kontenery `kmalarder-*`, baza `kmalarder`, port 8030.
- Mobile-first: dodanie partii w trzech polach (nazwa, ilość, termin), zużycie jednym tapnięciem; wszystko jedną ręką przy półce.

---

## Faza 0 – Szkielet (kopia kma-library)

Kopia repozytorium z przemianowaniem (`library` → `larder`, `Library` → `Pantry`), usunięcie kontekstów Catalog / Reading / Shelf, zostawione auth, admin, podgląd konta, PWA, CI, compose, skrypty. Nowy schemat `V1` z całą domeną spiżarni. Skaner kodów przestawiony z ISBN na EAN-13 / EAN-8 / UPC-A. Teksty UI od nowa.

## Faza 1 – Domena spiżarni (backend)

`ApproximateDate`, `Freshness`, `Product`, `Batch`, `StockMovement`; repozytoria; use case'y: produkty (lista z sortowaniem w pamięci, autouzupełnianie, karta, CRUD z deduplikacją), partie (dodaj, zużyj, wyrzuć, popraw, edytuj, usuń), zapytania (terminy, lista zakupów, historia, dashboard). REST pod `/api/pantries/{id}/…`, `/api/products`, `/api/batches`. Zegar `Clock` w strefie Europe/Warsaw jako bean.

## Faza 2 – Frontend

Kolorystyka „spiżarnia” / „piwnica” (ADR 001 §kolory). Strony: Start, Stan, karta produktu, edycja produktu i partii, Dodaj do spiżarni, Terminy, Do uzupełnienia, Domownicy. Komponenty: `ApproximateDateInput`, `QuantityStepper`, `QuantityModal`, `FreshnessBadge`, `BatchCard` z `BatchActions`, `ProductPicker`, `ProductFields`.

## Faza 3 – EAN i Open Food Facts

Port `ProductMetadataProvider`, adapter OFF z cache, `GET …/products/lookup?barcode=`. Skaner wybiera istniejący produkt albo wypełnia nowy.

## Faza 4 – OCR dat w przeglądarce

Tesseract.js z zasobami z własnego origin, przetwarzanie obrazu, parser dat (`utils/dateOcr.ts`), dialog z kandydatami do wyboru. Decyzja i alternatywy: [ADR 003](adr/003-ocr-in-browser.md).

## Faza 5 – Powiadomienia ntfy

`NtfyNotificationService`, `ExpiryDigest` + `SendExpiryDigests`, harmonogram (`@Scheduled`, cron z konfiguracji), ręczne wysłanie przez admina.

---

## Decyzje podjęte przed implementacją (2026-11-24)

| # | Pytanie | Decyzja |
|---|---------|---------|
| 1 | OCR: osobny kontener czy przeglądarka | **Przeglądarka** (Tesseract.js). Wynik i tak przychodzi z frontu potwierdzony przez człowieka; Pi nie dźwiga trzeciego JVM i silnika OCR. Port po stronie backendu niepotrzebny. Kontener wraca na stół, gdy zjawi się drugi konsument (ADR 003) |
| 2 | Produkt a partia | Produkt (słownik spiżarni) + partie (sztuki z datą); stan = suma partii |
| 3 | Precyzja dat | Rok + opcjonalny miesiąc + opcjonalny dzień (`ApproximateDate`), koniec okresu do porównań; dla domowych data zrobienia + przydatność produktu w miesiącach |
| 4 | Jednostki | Tylko sztuki (słoik, puszka, paczka) z opisową wielkością opakowania; otwarte opakowanie = zużyte |
| 5 | Zużycie | Zdarzenia (`stock_movements`), nie licznik: daje „ile dżemów rocznie” |
| 6 | Lokalizacje | Jedna spiżarnia na gospodarstwo z etykietą miejsca na partii („piwnica”, „zamrażarka”); osobne spiżarnie możliwe przez model członków |
| 7 | Współdzielenie | Spiżarnia wspólna (model Pantry / PantryMember z kma-library), produkty per spiżarnia, bez globalnego katalogu i scalania |
| 8 | Uzupełnianie | `minQuantity` na produkcie + lista „Do uzupełnienia” w MVP |
| 9 | Powiadomienia | ntfy w MVP: tygodniowy raport (cron), bo ntfy już działa na Pi |
| 10 | Kolejność metod wprowadzania | ręcznie → EAN + Open Food Facts → OCR daty; OCR całej etykiety (nazwa) później |
| 11 | Zdjęcia | Nie są przechowywane; żyją tylko na czas OCR w telefonie |
| 12 | Trzecia kopia szkieletu | Zaakceptowana świadomie (ADR 001); wspólnej biblioteki nie wydzielamy |
| 13 | Nazwa | `kma-larder` („larder” = spiżarnia, lepiej niż „pantry”); UI: „Spiżarnia” |

## Dalsze pomysły (poza MVP)

- OCR nazwy produktu z etykiety (ten sam silnik, inny parser) i rozpoznawanie „patrz wieczko”.
- Statystyki: zużycie roczne per kategoria, wykres sezonowości przetworów.
- Tryb offline (kolejka dodawania w piwnicy bez zasięgu).
- Osobne spiżarnie w jednym gospodarstwie (piwnica, zamrażarka) jako lokalizacje pierwszej klasy.
- Eksport CSV.
