# Wi-Fi CDMX AP Service

Service to query Wi-Fi Access Points in CDMX.

## Chosen stack:

- Scala 3.3
- http4s for backend
- PostgreSQL with POSTGis given the nature of the project
- MUnit for testing.
- tapir for api documentation


## Configuration

The following variables must be loaded from the environment:
- `DB_URL`: database url
- `DB_USER`: database user
- `DB_PASS`: password
- `DB_DRIVER`: db driver to use

## Folder structure based on clean architecture

- `com.wifi.cdmx.domain`: entities and repository definitions.
- `com.wifi.cdmx.application`: use cases and services.
- `com.wifi.cdmx.infrastructure`: implementations and routing.
- `com.wifi.cdmx.main`: entrypoint.

## Architecture Diagram
                         +----------------------+
                         |      Client          |
                         | (Browser / Postman)  |
                         +----------+-----------+
                                    |
                                    v
                         +----------------------+
                         |      http4s API      |
                         |  (Routes / Controllers)
                         +----------+-----------+
                                    |
                                    v
                         +----------------------+
                         |     Application      |
                         |   (Use Cases /       |
                         |    Services)         |
                         +----------+-----------+
                                    |
                                    v
                         +----------------------+
                         |       Domain         |
                         | (Entities + Repos    |
                         |   Interfaces)        |
                         +----------+-----------+
                                    |
                                    v
                         +----------------------+
                         |    Infrastructure    |
                         | (Repo Impl, DB,      |
                         |  PostGIS Queries)    |
                         +----------+-----------+
                                    |
                                    v
                         +----------------------+
                         | PostgreSQL + PostGIS |
                         |  (wifi.access_points)|
                         +----------------------+

## Tentative Ingestion Flow
        +----------------------+
        |      CSV File        |
        | (WiFi CDMX dataset)  |
        +----------+-----------+
                   |
                   v
        +----------------------+
        |  Ingestion Job       |
        | (on startup)         |
        +----------+-----------+
                   |
                   v
        +----------------------+
        |  Data Cleaning       |
        | (trim, cast, fix)    |
        +----------+-----------+
                   |
                   v
        +----------------------+
        | PostgreSQL + PostGIS |
        |   (Upsert data)      |
        +----------------------+

## Request Flow
```text
Client
  |
  v
GET /wifi/proximity?lat=...&lon=...
  |
  v
http4s Route
  |
  v
Use Case (FindNearbyAP)
  |
  v
Repository (interface)
  |
  v
PostGIS Query (ST_Distance / ST_DWithin)
  |
  v
PostgreSQL
  |
  v
Response (JSON)
```

## Ingestion Layer
Incomplete because of time. It works by running a job on startup
to upsert new information. The CSV may need some cleaning up beforehand though.


## Endpoints

### 1. Get all AP
`GET /wifi?limit=10&offset=0`
```json
{
  "items": [
    {
      "id": "\tAP1_Sitio_Publico_Explanada_San_Salvador_Cuauhtenco_18386-BMS-14",
      "programa": "Sitios Publicos (Albergues, Bibliotecas, Centros Culturales, Embarcaderos, Museos, Parques,etc)",
      "alcaldia": "Milpa Alta",
      "latitud": 19.192486,
      "longitud": -99.090225
    },
    {
      "id": "\tSitio_publico-Hospital Gregorio Salas_AP_01_18386-BMS-251",
      "programa": "Sitios Publicos (Albergues, Bibliotecas, Centros Culturales, Embarcaderos, Museos, Parques,etc)",
      "alcaldia": "Cuauhtemoc",
      "latitud": 19.437722,
      "longitud": -99.129447
    }
  ]
}
```

### 2. Get AP by Id
`GET /wifi/{id}`
```json
{
  "id": "MEX-AIM-AER-AICMT1-M-GW001",
  "programa": "Aeropuerto",
  "alcaldia": "Venustiano Carranza",
  "latitud": 19.432707,
  "longitud": -99.086743
}
```

### 3. Get all paginated by alcaldia
`GET /wifi/alcaldia/{nombre_alcaldia}?limit=10&offset=0`
```json
{
  "items": [
    {
      "id": "\tSitio_público-Médico Infantil Inguarán_AP_01_18386-BMS-129",
      "programa": "Sitios Publicos (Albergues, Bibliotecas, Centros Culturales, Embarcaderos, Museos, Parques,etc)",
      "alcaldia": "Venustiano Carranza",
      "latitud": 19.452109,
      "longitud": -99.113003
    },
    {
      "id": "100_1985_AP_01",
      "programa": "Unidades Habitacionales",
      "alcaldia": "Venustiano Carranza",
      "latitud": 19.430581,
      "longitud": -99.062342
    },
    {
      "id": "100_1985_AP_02",
      "programa": "Unidades Habitacionales",
      "alcaldia": "Venustiano Carranza",
      "latitud": 19.430581,
      "longitud": -99.062342
    }]
}
```

### 4. Get list paginated and by proximity
`GET /wifi/proximity?latitud=19.4327&longitud=-99.0867&limit=10&offset=0`
```json
{
  "items": [
    {
      "id": "CDSG-GM-AP11302",
      "programa": "Poste C5",
      "alcaldia": "Gustavo A. Madero",
      "latitud": 19.452283,
      "longitud": -99.062661
    },
    {
      "id": "CDSG-GM-AP01060",
      "programa": "Poste C5",
      "alcaldia": "Gustavo A. Madero",
      "latitud": 19.451344,
      "longitud": -99.062977
    }
  
  ],
  "total": 35350,
  "limit": 10,
  "offset": 0
}
```

### 5. API Docs
`GET /docs`
## Run

```bash
sbt run
```

## Test
```bash
sbt test
```


