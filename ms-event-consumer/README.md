# Servicio B — consumidor de eventos

Java 21, Spring Boot 3.3.5, Spring Kafka, JPA/Hibernate y SQL Server.

Consume `record-ready-topic-v2` como parte del grupo `record-processor-v2`.
Procesa lotes de hasta 25 mensajes, consulta XML en batch_record y guarda PROCESSED/FAILED junto con consumed_event en una transacción.
El offset se confirma después del commit SQL.
Los duplicados no repiten el efecto persistido.

Los mensajes malformados, incompatibles o huérfanos se conservan en rejected_event (payload, tópico, partición, offset, motivo y fecha).
No bloquean mensajes válidos.
Los errores técnicos revierten el lote y se reintentan cada 2 segundos; si no se puede guardar cuarentena tampoco se confirma el offset.

Este servicio B expone Actuator para salud/métricas. Health UP no equivale a lag = 0: revisar también offsets y tablas.

## Docker

Conserva esta carpeta junto a `ms-batch-producer`. Desde la raíz `reto-eventos`:

```sh
docker compose up -d --build --wait
```

El comando construye los tres microservicios y levanta SQL Server y Kafka. No requiere `.env` ni configurar credenciales. B expone salud/métricas en http://localhost:8081; su puerto interno Docker es 8080.

`src/main/resources/application.properties` contiene el usuario `sa` y contraseña `RetoLocal_2026!` directamente. Compose configura la misma contraseña en SQL Server y pasa a B únicamente la dirección interna de la base y las demás opciones de ejecución.

Consulta [el README principal](../README.md) para Windows, endpoints, credenciales y persistencia.

## IntelliJ / terminal (opcional)

El arranque completo se realiza con Docker. Para ejecutar B por separado desde el IDE, primero detén el consumer de Docker para liberar el puerto y evitar consumidores adicionales. SQL Server y A deben haber inicializado `reto_db`.

Ejecuta `MsEventConsumerApplication`; las propiedades incluidas apuntan a SQL Server en `localhost:21433` y Kafka en `localhost:29092`, con las credenciales locales ya escritas. El puerto HTTP predeterminado es 8081. Este modo requiere Java 21.

```sh
./mvnw verify
./mvnw spring-boot:run
```

En CMD usa `mvnw.cmd`. El grupo puede consumir mensajes históricos si no tiene offsets válidos. Hibernate usa `validate`; los scripts crean inbox/cuarentena sin eliminar tablas.
