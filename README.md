# FitBook API

[![CI](https://github.com/neubauerlukas207-cell/fitbook-api/actions/workflows/ci.yml/badge.svg)](https://github.com/neubauerlukas207-cell/fitbook-api/actions/workflows/ci.yml)

**REST API pro rezervace lekcí ve fitness centru** – Java 21, Spring Boot 4, JPA/Hibernate, PostgreSQL, Flyway. Hlídá kapacitu sálů i při souběžných rezervacích, nedovolí kolize v rozvrhu a umí se zeptat ML služby na předpověď obsazenosti.

Součást ekosystému **FitBook**:

```
┌─────────────────┐  REST   ┌──────────────────────┐  REST   ┌───────────────────────────┐
│ FitBook Admin   │ ──────► │ FitBook API (tady)   │ ──────► │ FitBook Insights          │
│ C# / ASP.NET    │         │ Java / Spring Boot   │         │ Python / FastAPI / sklearn│
└─────────────────┘         └──────────┬───────────┘         └───────────────────────────┘
                                       │ JPA
                                 ┌─────▼──────┐
                                 │ PostgreSQL │   schéma navazuje na fitness-reservation-db
                                 └────────────┘
```

## Endpointy

| Metoda | URL | Popis |
|---|---|---|
| `GET` | `/api/lessons?from=&to=&type=` | rozvrh s počtem volných míst (výchozí: příštích 7 dní) |
| `GET` | `/api/lessons/{id}` | detail lekce |
| `POST` | `/api/lessons` | vypsání lekce – **409**, když je sál nebo instruktor obsazený |
| `DELETE` | `/api/lessons/{id}` | smazání lekce bez rezervací |
| `GET` | `/api/lessons/{id}/forecast` | předpověď obsazenosti z [FitBook Insights](https://github.com/neubauerlukas207-cell/fitbook-insights) (**503**, když služba neběží) |
| `POST` | `/api/members` | registrace člena (unikátní e-mail) |
| `GET` | `/api/members/{id}/reservations` | rezervace člena |
| `POST` | `/api/reservations` | rezervace – **409** plná lekce / už začala / duplicitní |
| `DELETE` | `/api/reservations/{id}` | storno – nejpozději 2 h před začátkem |
| `GET` | `/api/stats/occupancy` | obsazenost podle typu lekce |
| `GET` | `/api/rooms`, `/api/instructors` | číselníky |

Interaktivní dokumentace: **`/swagger-ui.html`** (OpenAPI 3).

Chyby vrací jednotně ve formátu **RFC 9457** (`application/problem+json`):

```json
{ "status": 409, "title": "Porušení pravidla", "detail": "Lekce je plná", "code": "LESSON_FULL" }
```

## Zajímavé části

- **Souběžné rezervace** – `SELECT … FOR UPDATE` (`@Lock(PESSIMISTIC_WRITE)`) na řádku lekce: dva lidé, kteří kliknou na poslední místo ve stejnou chvíli, se seřadí a kapacita se nepřekročí. Pojistkou je unikátní omezení `(member_id, lesson_id)` v databázi.
- **Žádný problém N+1** – rozvrh se načte **dvěma dotazy** bez ohledu na počet lekcí (`@EntityGraph` + jeden `GROUP BY` na počty rezervací).
- **Čas přes `Clock`** – pravidla jako „storno 2 h předem“ jsou testovatelná s pevným časem.
- **Mikroservisy** – `RestClient` s timeouty volá Python ML službu; když neodpovídá, API vrátí 503 a zbytek funguje dál.

## Technologie

Java 21 · Spring Boot 4 (Web MVC, Data JPA, Validation, Actuator) · Hibernate 7 · PostgreSQL 17 / H2 · Flyway · springdoc-openapi · JUnit 5 · Mockito · MockMvc · Docker Compose · GitHub Actions

## Spuštění

**Celý systém v Dockeru** (PostgreSQL + API + ML služba):

```bash
docker compose up --build
# API:      http://localhost:8080/swagger-ui.html
# Insights: http://localhost:8000/docs
```

**Bez Dockeru** (in-memory H2 databáze + ukázková data):

```bash
./mvnw spring-boot:run
# s ML službou: ./mvnw spring-boot:run -Dspring-boot.run.arguments=--fitbook.insights.url=http://localhost:8000
```

**Testy:** `./mvnw verify` – 27 testů (jednotkové testy pravidel s Mockito + integrační testy přes HTTP proti databázi s Flyway migracemi). CI navíc spustí celý systém v Docker Compose a ověří ho smoke testem.

## Struktura (balíčky podle funkcí)

```
cz.fitbook.api
  lesson/       entity Lesson, Room, Instructor · LessonService (kolize v rozvrhu) · LessonController
  member/       registrace členů
  reservation/  ReservationService (kapacita, storno, zámky) · ReservationController
  forecast/     InsightsClient (HTTP klient na Python službu) · ForecastController
  stats/        statistiky obsazenosti
  common/       výjimky a jednotné chybové odpovědi
  demo/         ukázkový rozvrh na 14 dní
resources/db/migration/   Flyway migrace (verzované schéma)
```

