# Servicio C — Monitoreo

Java 21, Spring Boot 3.3.5, JPA y SQL Server. Consulta los datos del flujo A → Kafka → B sin conectarse a Kafka.

## Arranque y API

Desde la raíz del proyecto: `docker compose up -d --build --wait`. No requiere configurar variables ni archivos privados. Puerto local **8083**; puerto interno Docker 8080.

| Método | Ruta | Contenido |
|---|---|---|
| GET | /api/monitoreo/resumen | total, pending, processed y failed globales |
| GET | /api/monitoreo/ejecuciones?page=0&size=25 | Historial paginado de ejecuciones de A y sus tiempos |

`durationMs` es endTime menos startTime en milisegundos. Devuelve null si falta inicio o fin. Mide la ejecución del job de ingestión A; B termina de forma asíncrona y no está incluido en ese tiempo. `COMPLETED` en A no implica que todos los registros ya estén procesados por B.

Paginación: page entre 0 y 10000; size entre 1 y 100. Valores inválidos: HTTP 400. Base de datos no disponible: HTTP 503. Actuator: `/actuator/health`.

Ejemplo de resumen: `{"total":100,"pending":10,"processed":85,"failed":5}`. FAILED incluye rechazos de XML en A y fallos de negocio en B. Los conteos son globales, no exclusivos del último archivo.

## Consultas y pruebas

Los conteos se calculan en SQL con agregados JPA. El historial usa proyecciones y paginación SQL, sin cargar XML. Entidades inmutables, transacciones de solo lectura, pool de cuatro conexiones, timeout de consulta de cinco segundos y transacción de diez segundos. Hibernate valida el esquema; A lo inicializa. C no crea tablas ni modifica registros.

`./mvnw verify` ejecuta diez pruebas: dos de servicio con Mockito, cuatro de controlador, tres de consultas JPA/H2 y una de arranque de contexto. Cubren conteos, tablas vacías, paginación, ID cero y duración de ejecuciones completas/incompletas. La validación Docker comprueba además SQL Server real.

Para IntelliJ, abrir el pom.xml raíz e importar los tres módulos Maven. Para ejecutar C en el IDE, detener antes el monitor Docker para liberar 8083 y ejecutar MsMonitoreoApplication; las propiedades ya incluyen SQL localhost:21433, base reto_db, usuario sa y contraseña RetoLocal_2026!.
