# ADR 003: OCR dat ważności w przeglądarce

**Status:** Zaakceptowany
**Data:** 2026-11-24

## Kontekst

Wprowadzanie produktów ma być szybkie: zdjęcie nadruku z datą zamiast przepisywania. Rozważano wydzielenie serwisu OCR do osobnego kontenera, żeby inne aplikacje na Pi mogły z niego korzystać. Ograniczenia: Raspberry Pi już utrzymuje dwa JVM; każdy silnik OCR poza Tesseractem (EasyOCR, PaddleOCR) wymaga PyTorcha i 1–2 GB RAM; wynik OCR i tak musi potwierdzić człowiek, bo daty na puszkach są nadrukowane igłowo, na wieczkach wytłoczone, a na słoikach zakrzywione.

## Decyzja

OCR działa **w przeglądarce** (Tesseract.js, WebAssembly, język `pol` z tessdata_fast), bez udziału backendu:

- zasoby (worker, rdzenie WASM, dane językowe) serwowane z własnego origin spod `/ocr` – CSP nie dopuszcza CDN; worker i rdzenie kopiowane z `node_modules` przed `vite` / `vite build` (`scripts/copy-ocr-assets.mjs`), dane językowe (~2 MB) w repozytorium;
- ładowanie dopiero przy pierwszym użyciu (~8 MB, potem w cache przeglądarki; nginx: `Cache-Control: public, max-age 30d` dla `/ocr/`);
- obraz przed OCR jest zmniejszany do 1600 px, zamieniany na skalę szarości, lekko rozmywany (kropki nadruku igłowego zlewają się w kreski) i porównywany z lokalnym tłem, tak że cień, odblask i faktura folii odpadają, a blady atrament jest rozciągany do czerni (`lib/ocr.ts`);
- silnik dostaje listę dozwolonych znaków (cyfry i separatory) i tryb tekstu rozproszonego: szukamy wyłącznie dat, numer partii może się zepsuć;
- zdjęcie jest czytane w kilku orientacjach (jak zrobione, obrócone w obie strony, do góry nogami), w skali szarości i w czerni-bieli, z przerwaniem po pierwszej pełnej dacie; kandydaci z kilku prób są łączeni z liczbą głosów;
- rozpoznawanie dat to czysta funkcja `utils/dateOcr.ts` z testami: formaty z opakowań (12.05.2027, 12/05/27, 05.2027, 05/27, 2027-05-12, 2027-05, sam rok), naprawa pomylonych liter (O→0, l→1), odrzucanie dat niemożliwych i spoza kilku lat, kandydaci od najpewniejszego;
- UI pokazuje kandydatów do wyboru; **nic nie zapisuje się bez tapnięcia** użytkownika.

Backend nie ma portu OCR. Domena dostaje już potwierdzoną `ApproximateDate`.

## Alternatywy

| Wariant | RAM na Pi | Uwagi |
|---|---|---|
| Tesseract w kontenerze (Python + FastAPI) | ~200–300 MB | reużywalny przez inne aplikacje przez wspólną sieć Dockera; kilka sekund na zdjęcie na Pi 4; zdjęcie opuszcza telefon |
| EasyOCR / PaddleOCR | 1–2 GB | odpadają na Pi |
| Model wizyjny w chmurze | 0 | najlepsza jakość na pogniecionych etykietach i wynik strukturalny; internet i wysyłka zdjęć na zewnątrz |
| **Tesseract.js w przeglądarce** | **0** | telefon szybszy od Pi, prywatność, ten sam wzorzec co skaner (zxing-wasm); brak reużycia przez inne aplikacje |

## Konsekwencje

- Plus: zero obciążenia Pi, zdjęcia nie opuszczają telefonu, brak nowego kontenera i sieci; całość w jednym repozytorium.
- Minus: pierwszy użytek pobiera ~8 MB; jakość na trudnych nadrukach bywa słaba, np. cyfra z przerwaną pętlą czytana jako inna (stąd zawsze lista do wyboru i ręczne pola obok). Sprawdzone na zdjęciach nadruku igłowego z folii: ujęcie poziome czytane poprawnie, ujęcie z cieniem i obrotem daje pełną datę z jednym błędnym znakiem. Dane `eng` zamiast `pol` nie czytały cyfr lepiej.
- Gdy pojawi się drugi konsument OCR, parser dat i tak zostaje w aplikacji; kontener Tesseract wchodziłby jako druga implementacja za tym samym dialogiem (zdjęcie → tekst), bez zmian w domenie.
