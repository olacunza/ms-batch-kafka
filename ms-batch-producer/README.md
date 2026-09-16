# Procesamiento de XML por eventos

Java 21
Spring Boot 3.3.5
Spring Batch
JPA/Hibernate
SQL Server 2022
Kafka 3.9.1.

## Alcance

- **A — ms-batch-producer:** recibe XML/ZIP, lee chunks, persiste registros y publica eventos mediante outbox.
- **B — ms-event-consumer:** consume lotes de Kafka, procesa XML, guarda resultados y deduplica eventos. Los mensajes irrecuperables se conservan en cuarentena SQL.

## Arranque con Docker

Desde la raíz `reto-eventos` o desde esta carpeta:

```sh
docker compose up -d --build --wait
```

Se construyen los tres microservicios y se levantan SQL Server 2022 y Kafka. No se necesita `.env`, Java/Maven en el equipo ni configurar credenciales. Docker Desktop debe estar iniciado; el Compose de la raíz requiere Compose 2.20.0 o superior.

Puertos: A 8080, B 8081, C 8083, SQL Server 21433 y Kafka 29092. Los servicios esperan la salud e inicialización de sus dependencias. Los volúmenes pertenecen al entorno independiente `reto-eventos` y conservan sus datos entre arranques.

Credenciales incluidas: `sa` / `RetoLocal_2026!`. Están escritas en `src/main/resources/application.properties` de cada microservicio y en `docker-compose.yml`; no se leen de un archivo privado ni se inyectan a las aplicaciones mediante variables.

Windows Intel/AMD x64: Docker Desktop con contenedores Linux y WSL 2. SQL Server utiliza `linux/amd64`; su emulación en ARM no está soportada por Microsoft. Consulta [el README principal](../README.md) para requisitos, explicación de credenciales y ejemplos desde CMD/PowerShell.

```sh
docker compose ps -a
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
docker compose logs --tail=100 producer consumer
docker compose down
```

El último comando conserva los datos.

## Enviar un archivo

En Postman importa la colección única `../postman/reto-eventos.postman_collection.json`.
Usa **POST** `http://localhost:8080/api/batch/launch`, **Body → form-data**, campo **file**, tipo **File**, seleccionando XML o ZIP.
Deja que Postman genere el Content-Type multipart.

La respuesta inicial es 202. `COMPLETED` describe la ingestión de A, `pendingEvents=0` describe la publicación a Kafka
y cero registros PENDING describe el fin del procesamiento secundario de esa carga.

Un XML inválido queda FAILED desde A y no genera evento.

El ZIP puede contener carpetas y metadatos de macOS.
Se omiten `__MACOSX/`, `.DS_Store`, `._*` y directorios.
Se conserva el XML original, incluidos rechazos; no se extraen rutas ZIP.

Límites: subida 25 MiB, XML 1 MiB, 100 MiB descomprimidos, 10000 XML y 30000 entradas totales.
Los metadatos también respetan límites de bytes.

`stop`, `start` y `down` sin `--volumes` conservan los volúmenes. No se necesita limpiar Kafka después de una carga: conserva mensajes por retención y cada grupo recuerda su avance.

## Código e imágenes

Conserva los tres proyectos como carpetas hermanas junto al `compose.yaml` raíz al publicar o clonar el repositorio. Cada Dockerfile compila y ejecuta `mvn verify` antes de empaquetar; no necesitas publicar imágenes previamente para arrancar desde Git.

Las imágenes locales son `reto/ms-batch-producer:1.0.0` y `reto/ms-event-consumer:1.0.0`. Las imágenes incluyen las credenciales locales documentadas arriba. Kafka y SQL Server usan imágenes oficiales.
