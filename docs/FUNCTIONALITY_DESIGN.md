# KMA Larder – projekt funkcjonalny i model domeny

Aplikacja webowa (mobile-first, PWA) do śledzenia domowej spiżarni. Dwa cele:

1. **Wiedzieć, ile czego jest** – słoiki przetworów, miód, mąki, makarony, konserwy, woda.
2. **Wiedzieć, kiedy kończy się termin ważności** – żeby zużywać na czas i uzupełniać zapasy.

Spiżarnia jest z natury wspólna dla gospodarstwa: jedna spiżarnia, kilkoro domowników z tym samym widokiem.

---

## 1. Role użytkowników

| Rola | Uprawnienia |
|------|-------------|
| `USER` | Własna spiżarnia (tworzona przy rejestracji) i spiżarnie, do których został dodany; produkty, partie, zużycie, lista zakupów |
| `ADMIN` | To co `USER` + zarządzanie użytkownikami (lista, blokowanie, reset hasła, podgląd konta), ręczne wysłanie raportu ntfy |

Rejestracja: otwarta (z rate-limitingiem w nginx), z możliwością wyłączenia flagą `app.registration.enabled` po założeniu kont domownikom.

---

## 2. Bounded contexts

```
┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────────────────┐
│  Identity & Access   │  │   Pantry             │  │   Stock                      │
│                      │  │   (spiżarnia)        │  │   (zapasy)                   │
│  - User              │  │  - Pantry            │  │  - Product                   │
│  - UserRole          │  │  - PantryMember      │  │  - Batch (partia)            │
│  - JWT / refresh     │  │  - PantryAccessPolicy│  │  - StockMovement (historia)  │
│                      │  │                      │  │  - ApproximateDate, Freshness│
└──────────────────────┘  └──────────────────────┘  └──────────────────────────────┘
                                                      porty: ProductMetadataProvider (EAN),
                                                             NotificationService (ntfy)
```

Zasady:

- **Pantry** to gospodarstwo: właściciel i domownicy. Każdy użytkownik dostaje własną spiżarnię przy rejestracji; domownik dołącza do cudzej po e-mailu. Dostęp sprawdzany w jednym miejscu: `PantryAccessPolicy`.
- **Stock** należy do spiżarni (`Product.pantryId`, `Batch.pantryId`). Produkt jest słownikiem **jednej spiżarni**, nie wspólnym katalogiem wszystkich użytkowników: nie ma scalania duplikatów między gospodarstwami, a „Ogórki kiszone” u sąsiada to inny produkt.
- Integracje zewnętrzne to wyłącznie porty w warstwie aplikacji: `ProductMetadataProvider` (Open Food Facts) i `NotificationService` (ntfy). OCR nie ma w backendzie w ogóle – działa w przeglądarce (ADR 003).

---

## 3. Model domeny (Kotlin, warstwa `domain`)

### 3.1 Pantry

- `Pantry(id, name, ownerId, createdAt, updatedAt)`, `PantryMember(pantryId, userId, role: OWNER | MEMBER)`.
- Każdy członek: produkty, partie, zużycie, listy. Właściciel dodatkowo: domownicy i nazwa. Właściciela nie da się usunąć.

### 3.2 Stock

```kotlin
data class Product(
    val id: ProductId,
    val pantryId: PantryId,
    val name: ProductName,               // VO: trim, zbite spacje; normalized = lowercase bez diakrytyków i interpunkcji (deduplikacja)
    val kind: ProductKind,               // STORE_BOUGHT (termin z opakowania) | HOMEMADE (data zrobienia + przydatność)
    val category: ProductCategory,       // PRESERVES, HONEY, FLOUR_GRAINS, PASTA_RICE, CANNED, BEVERAGES, OILS_SAUCES, SWEETS_SNACKS, SPICES_BAKING, DRIED, FROZEN, OTHER
    val barcode: Barcode? = null,        // VO: EAN-13 / EAN-8 / UPC-A (jako EAN-13), walidacja cyfry kontrolnej; unikalny w spiżarni
    val packageSize: String? = null,     // "0,9 l", "400 g" – opis, nie liczba
    val defaultShelfLifeMonths: Int?,    // przydatność domowych przetworów od daty zrobienia
    val minQuantity: Int = 0,            // poniżej tej liczby produkt trafia na listę zakupów; 0 = bez minimum
    val notes: String? = null,
    val archived: Boolean = false,       // znika z list, historia zostaje
    ...
)

data class Batch(                        // partia: sztuki tego samego produktu z tą samą datą
    val id: BatchId,
    val pantryId: PantryId,
    val productId: ProductId,
    val quantity: Int,                   // >= 0; partia z zerem zostaje jako historia, aż ktoś ją usunie
    val bestBefore: ApproximateDate?,    // z opakowania
    val producedOn: ApproximateDate?,    // przetwory
    val expiresOn: LocalDate?,           // ostatni dzień okresu: bestBefore, a bez niego producedOn + przydatność produktu; zapisywany
    val location: String? = null,        // "piwnica", "zamrażarka"
    val notes: String? = null,
    val addedOn: LocalDate,
    ...
)

data class StockMovement(                // każda zmiana ilości, na zawsze
    val id: StockMovementId,
    val pantryId: PantryId, val productId: ProductId, val batchId: BatchId?,
    val type: MovementType,              // ADDED (+), CONSUMED (−), DISCARDED (−), ADJUSTED (±)
    val quantity: Int,                   // zmiana ze znakiem
    val occurredOn: LocalDate,
    val userId: UserId?,
)
```

Value objects:

- **`ApproximateDate(year, month?, day?)`** – data tak, jak jest nadrukowana: sam rok („2027”), miesiąc i rok („05.2027”) albo pełna data. Do porównań czytana jako **ostatni dzień okresu** (`endDate`): „2027” to 31.12.2027, „05.2027” to 31.05.2027. `plusMonths(n)` zachowuje precyzję: „2026” + 12 = „2027”, „06.2026” + 12 = „06.2027”.
- **`Freshness`** – polityka statusu: `EXPIRED` (koniec okresu przed dziś), `EXPIRING_SOON` (do N dni, domyślnie 30), `OK`, `UNKNOWN` (bez daty). `worst(...)` dla produktu z wieloma partiami.
- **`Barcode`**, **`ProductName`** – jak wyżej.

Niezmienniki:

- Ilość partii nigdy nie spada poniżej zera; `take(n)` z n > quantity to błąd (HTTP 409).
- Nowa partia ma co najmniej jedną sztukę. Każde dodanie tworzy ruch `ADDED`, zużycie `CONSUMED`, wyrzucenie `DISCARDED`; korekta stanu (`adjustTo`) zapisuje tylko różnicę jako `ADJUSTED`.
- Zmiana przydatności produktu przelicza `expiresOn` partii z datą wyliczoną; daty z opakowania nie rusza.
- Nazwa (po normalizacji) i kod kreskowy są unikalne w obrębie spiżarni.

Autoryzacja: każda operacja wymaga członkostwa w spiżarni produktu / partii (`PantryAccessGuard` → `PantryAccessPolicy`).

---

## 4. Historyjki użytkownika (MVP)

**Dodawanie**
- Wpisuję nazwę: podpowiedź z moich produktów (bez diakrytyków, „ogor” znajduje „Ogórki kiszone”) albo nowy produkt. Podaję ilość (duże −/+), datę z opakowania w tylu polach, ile widzę (rok / miesiąc / dzień), dla przetworów datę zrobienia.
- Skanuję kod EAN: jeśli produkt z tym kodem już jest w spiżarni, zostaje wybrany; jeśli nie, nazwę i opakowanie podpowiada Open Food Facts.
- Robię zdjęcie nadruku z datą: aplikacja rozpoznaje daty w telefonie i pokazuje je do wyboru. Nic nie zapisuje się bez mojego tapnięcia.
- „Dodaj kolejny” zostawia mnie w formularzu, bo zwykle rozpakowuję całe zakupy.

**Stan i zużywanie**
- Widzę listę produktów z liczbą sztuk i sygnalizacją świeżości; filtruję po kategorii i rodzaju, sortuję po nazwie, terminie, ilości.
- Na karcie produktu widzę partie od najpilniejszej; „Zużyj 1” jednym tapnięciem, „Zużyj…” kilka, „Wyrzuć” z powodem (po terminie), „Popraw stan” po inwentaryzacji.
- Widzę historię i ile sztuk zużyliśmy w tym i w poprzednim roku.

**Terminy i zakupy**
- Na Starcie widzę: ile po terminie, ile kończy się w 30 dni, ile produktów do uzupełnienia, oraz listy „zużyć w pierwszej kolejności” i „brakuje”.
- Zakładka Terminy: wszystkie / po terminie / 30 dni / 90 dni / bez terminu, z przyciskami zużycia na miejscu.
- Zakładka Zakupy: produkty poniżej minimum z liczbą brakujących sztuk.
- W niedzielę dostaję na telefon (ntfy) raport: po terminie, kończy się, do kupienia.

**Domownicy**
- Właściciel dodaje domownika po e-mailu (konto musi istnieć); domownik widzi tę samą spiżarnię i może zużywać i dodawać.

---

## 5. Schemat bazy (Flyway `V1__initial_schema.sql`)

`users`, `pantries`, `pantry_members`, `products` (unikalne `(pantry_id, barcode)` i indeks na `normalized_name`, `pg_trgm` na nazwie), `batches` (daty jako `*_year / *_month / *_day` + `expires_on` z indeksem `(pantry_id, expires_on)`), `stock_movements` (`batch_id ON DELETE SET NULL`, `product_id ON DELETE CASCADE`). Usunięcie produktu usuwa partie i jego historię; archiwizacja ją zachowuje.

---

## 6. REST API

| Metoda | Ścieżka | Opis |
|---|---|---|
| GET | `/api/pantries` | moje spiżarnie z rolą i liczbą domowników |
| PATCH | `/api/pantries/{id}` | nazwa (właściciel) |
| GET / POST / DELETE | `/api/pantries/{id}/members[/{userId}]` | domownicy |
| GET | `/api/pantries/{id}/products?search&category&kind&includeArchived&sort&page&pageSize` | stan |
| GET | `/api/pantries/{id}/products/search?q=` | autouzupełnianie |
| GET | `/api/pantries/{id}/products/lookup?barcode=` | produkt ze spiżarni i/lub dane z Open Food Facts (400 / 404 / 503) |
| POST | `/api/pantries/{id}/products` | nowy produkt (409 duplikat nazwy lub kodu) |
| GET / PUT / DELETE | `/api/products/{id}` | karta produktu (partie, historia, zużycie roczne) / edycja / usunięcie |
| POST | `/api/products/{id}/batches` | nowa partia |
| PUT / DELETE | `/api/batches/{id}` | daty, miejsce, notatka / usunięcie |
| POST | `/api/batches/{id}/consume` · `/discard` · `/adjust` | zużycie, wyrzucenie, korekta (409 za mało sztuk) |
| GET | `/api/pantries/{id}/expiring?withinDays&expiredOnly&undatedOnly&soonDays` | terminy |
| GET | `/api/pantries/{id}/replenishment` | lista zakupów |
| GET | `/api/pantries/{id}/movements?productId&page&pageSize` | historia |
| GET | `/api/pantries/{id}/dashboard?soonDays` | Start |
| POST | `/api/admin/notifications/digest` | raport ntfy od ręki (admin) |

Daty w JSON: `{ "year": 2027, "month": 5, "day": null }`; `expiresOn` jako `YYYY-MM-DD`; `freshness` jako `OK | EXPIRING_SOON | EXPIRED | UNKNOWN`; `daysLeft` ze znakiem.
