# 🔧 Wdrożenie produkcyjne (Raspberry Pi)

Obrazy budowane są przez GitHub Actions (`.github/workflows/deploy-production.yml`) automatycznie po każdym merge'u
do `main`, który zmienia `backend/` lub `frontend/` (tag `main`), po opublikowaniu release'a (tag `vX.Y.Z`) lub ręcznie
(`workflow_dispatch`). Testy nie są tu powtarzane: uruchamia je CI na każdym PR. Obrazy trafiają do GHCR:

- `ghcr.io/krzycuh/kma-larder/backend:<tag>`
- `ghcr.io/krzycuh/kma-larder/frontend:<tag>`

## Pierwsze uruchomienie na Pi

1. Skopiuj do **osobnego katalogu** na Pi (np. `/opt/kma-larder`): `docker-compose.prod.yml`, `scripts/`, `.env.production.template`.
   Nie trzymaj tu plików compose innych aplikacji: współdzieliłyby `.env` i względne katalogi (`logs/`, `data/`).
   Projekt ma stałą nazwę `name: kma-larder`, więc nie łączy się z innymi projektami compose.
2. `cp .env.production.template .env.production` i uzupełnij hasła (`openssl rand -base64 32`) oraz `DATA_PATH` (SSD).
3. Utwórz katalogi danych: `(source .env.production && mkdir -p "$DATA_PATH/postgres" "$BACKUP_PATH" logs)`.
4. Zaloguj się do GHCR: `echo $GITHUB_TOKEN | docker login ghcr.io -u $GITHUB_USER --password-stdin`.
5. `docker compose -f docker-compose.prod.yml --env-file .env.production up -d`.
   Aplikacja jest dostępna na `http://<pi>:8030` (port zmienisz przez `FRONTEND_PORT`). Backend nie publikuje portu:
   `/api` przechodzi przez nginx w kontenerze frontendu do `backend:8080` w sieci compose.
6. Zaloguj się jako `admin@example.com` / `admin123` i **od razu zmień hasło** (Profil → Zmiana hasła).
7. HTTPS: patrz [docs/adr/002-https.md](docs/adr/002-https.md) (wymagane pod aparat: skaner kodów i OCR dat). Terminator TLS kieruje ruch na port 8030.

## Aktualizacja do nowej wersji

```bash
# w .env.production ustaw VERSION=vX.Y.Z (lub main)
docker compose -f docker-compose.prod.yml --env-file .env.production pull
docker compose -f docker-compose.prod.yml --env-file .env.production up -d
docker compose -f docker-compose.prod.yml --env-file .env.production logs -f --tail 100 backend   # Flyway zastosuje migracje przy starcie
```

## Ręczne zbudowanie obrazów (bez CI)

```bash
docker build -t ghcr.io/$GITHUB_USER/kma-larder/backend:main ./backend
docker build -t ghcr.io/$GITHUB_USER/kma-larder/frontend:main ./frontend
docker push ghcr.io/$GITHUB_USER/kma-larder/backend:main
docker push ghcr.io/$GITHUB_USER/kma-larder/frontend:main
```

## Logi

Backend i nginx logują na stdout, a Docker trzyma to w plikach `json-file` z rotacją (3 × 10 MB na kontener).
Logi giną przy odtworzeniu kontenera (`up -d` po zmianie konfiguracji lub wersji).

```bash
docker logs -f --tail 100 kmalarder-backend-prod
docker logs -f --tail 100 kmalarder-frontend-prod
```

## Baza danych

Postgres nie publikuje portu na hoście; backend łączy się z nim przez sieć compose (`postgres:5432`).
Konsola SQL: `docker exec -it kmalarder-postgres-prod psql -U kmalarder`.

## Backup

`scripts/backup-database.sh` robi `pg_dump` (skompresowany gzipem) do `$BACKUP_PATH/postgres` i usuwa kopie
starsze niż `BACKUP_RETENTION_DAYS`. Skrypty same przechodzą do katalogu projektu i czytają `.env.production`,
więc działają z crona:

```bash
# crontab -e: codziennie o 3:00
0 3 * * * /opt/kma-larder/scripts/backup-database.sh >> /opt/kma-larder/logs/backup.log 2>&1
```

Odtwarzanie: `scripts/restore-database.sh` (interaktywnie wybiera kopię, przed odtworzeniem robi kopię bieżącej bazy
`pre_restore_*.sql.gz` i na czas odtwarzania zatrzymuje backend).

Kopie zrobione przed poprawką skryptu (podwójnie skompresowane) odtworzysz tak:
`gunzip -c plik.sql.gz | gunzip | docker exec -i kmalarder-postgres-prod psql -U kmalarder -d postgres`.

## Przejście ze starej konfiguracji (bez `name:`)

Kontenery uruchomione przed dodaniem `name: kma-larder` należą do projektu nazwanego od katalogu, więc nowe
`docker compose ... down` ich nie widzi. Zatrzymaj je raz po nazwie: `docker rm -f kmalarder-frontend-prod
kmalarder-backend-prod kmalarder-postgres-prod`, potem `up -d` jak wyżej. Dane bazy zostają w `$DATA_PATH/postgres`.
