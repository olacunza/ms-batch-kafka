# Reto técnico — XML por eventos

Tres microservicios Java 21 / Spring Boot: A ingiere XML/ZIP con Spring Batch y publica mediante outbox; B consume Kafka y guarda PROCESSED/FAILED; C consulta conteos y tiempos mediante JPA.

## Ejecutar

Con Docker iniciado y Compose 2.20 o superior, desde esta carpeta:

```sh
docker compose up -d --build --wait
```

No requiere configurar variables, copiar .env ni instalar Java/Maven: los Dockerfiles compilan y ejecutan las pruebas. La primera construcción requiere Internet. SQL Server utiliza linux/amd64; el arranque se verificó en este equipo con Docker Desktop.

| Servicio | Dirección local |
|---|---|
| A / producer | http://localhost:8080 |
| B / consumer (Actuator) | http://localhost:8081 |
| C / monitor | http://localhost:8083 |
| SQL Server | localhost,21433 |
| Kafka | localhost:29092 |

SQL Server: autenticación SQL, usuario **sa**, contraseña **RetoLocal_2026!**, base **reto_db**. Las credenciales del reto están incluidas en los properties y Compose. Para el certificado local, habilitar confiar en certificado del servidor en el cliente SQL.

init-db y kafka-init son tareas de inicialización: terminar con código 0 es correcto. SQL Server, Kafka, producer, consumer y monitor deben quedar saludables. Los volúmenes conservan datos y offsets; no es necesario borrarlos entre cargas.

## Postman

Importar el único archivo `postman/reto-eventos.postman_collection.json` (18 solicitudes). No requiere Environment adicional.

1. En la solicitud de subida de A seleccionar Body → form-data → file → archivo XML o ZIP.
2. Enviar: recibe 202 y la colección guarda jobExecutionId/uploadId.
3. Consultar el job de A. COMPLETED significa fin de ingestión.
4. Consultar C: el resumen muestra pendientes/procesados/fallidos globales; ejecuciones muestra los tiempos del job de A. B consume automáticamente.

C tiene únicamente dos endpoints de negocio:

```sh
curl http://localhost:8083/api/monitoreo/resumen
curl 'http://localhost:8083/api/monitoreo/ejecuciones?page=0&size=25'
```

`durationMs` mide ingestión A, no el procesamiento asíncrono de B. Es null hasta disponer de inicio y fin. El reinicio de un job en A solo aplica a FAILED/STOPPED.

## IntelliJ y pruebas

Abrir **pom.xml de esta raíz** como proyecto Maven: contiene los tres módulos. Si el proyecto ya estaba abierto, usar Reload All Maven Projects. Antes el IDE solo tenía vinculados los pom de A y B, por eso ms-monitoreo aparecía como carpeta sin importar.

Con Java 21: `./mvnw verify` (Windows: `mvnw.cmd verify`). Las pruebas también corren durante la construcción Docker.

## Documentación

- [Kafka y batching JPA](docs/JUSTIFICACION_RENDIMIENTO.md)
- [API de C](ms-monitoreo/README.md)
- [Tablas y arquitectura](ms-batch-producer/docs/ARQUITECTURA.md)
- [Recuperación y reintentos](ms-batch-producer/docs/OPERACION.md)
- [Validación final](docs/VALIDACION_FINAL.md)

## Operación y entrega

`docker compose ps -a` muestra estados; `docker compose logs -f producer consumer monitor` muestra trazas. `docker compose down` conserva volúmenes; volver a ejecutar el comando de arranque para reanudar.

Subir a Git esta estructura completa. Se excluyen datos locales, target y archivos del IDE. Imágenes construidas: reto/ms-batch-producer:1.0.0, reto/ms-event-consumer:1.0.0 y reto/ms-monitoreo:1.0.0. No hace falta publicarlas en un registro para ejecutar un clon: Compose las construye. Para publicarlas en un registro propio, etiquetarlas con docker tag y hacer docker push tras autenticarse.

El despliegue local tiene un broker y una instancia SQL; no constituye alta disponibilidad ni una certificación de rendimiento en producción.
