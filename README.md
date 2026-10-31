# SGA - Sistema de Gestión de Alojamiento

Proyecto final de Programación Avanzada (Ingeniería de Sistemas y Computación, Universidad del Quindío, 2026-2).

El SGA es el backend que maneja la operación del Apartahotel Coral, en Bocagrande (Cartagena): apartamentos, temporadas y tarifas, disponibilidad, cotizaciones, reservas, la llegada y la salida de los huéspedes, el folio de cada reserva y las reservas que llegan desde un canal externo.

**Equipo LoopDivas:** Juliana, Sara y Laura.

---

## Tecnologías

- Java 25
- Spring Boot 4 con Gradle
- Spring Data JPA y H2 en memoria
- Bean Validation
- MapStruct (DTO ↔ dominio)
- springdoc-openapi (Swagger UI)
- JUnit 5 y MockMvc para las pruebas

## Arquitectura

Usamos **arquitectura hexagonal** (puertos y adaptadores) :

```
domain/           El negocio. No conoce Spring, JPA ni HTTP.
  entity/           Agregados y entidades (Reserva, Apartamento, Folio, Alojamiento...)
  valueobject/      Objetos de valor como record o enum (Estancia, Dinero, Ocupante...)
  service/          Servicios de dominio (disponibilidad, cotización, creación de reservas...)
  repository/       Puertos: las interfaces que necesita el dominio
  exception/        ReglaDominioException
application/      Casos de uso (@Service, @Transactional), DTO y excepciones de aplicación
infrastructure/   Lo que conecta el dominio con el mundo
  rest/             Controllers, mappers de MapStruct y manejo global de errores
  persistence/      Adaptadores JPA (entidades, mappers y repositorios Spring Data) y en memoria
  config/           Registro de beans, datos iniciales y parámetros
  scheduler/        Tarea programada que vence las reservas pendientes
```

Algunas decisiones que tomamos:

- Todas las reglas de negocio (RN, RP y L) se validan en el dominio. El DTO solo valida la forma del mensaje.
- El dominio no tiene setters ni anotaciones. Para guardarlo en H2 usamos entidades JPA aparte y un mapper de persistencia.
- Los parámetros del alojamiento (horarios, umbral de edad, plazo de confirmación, depósito, política de cancelación, tamaño de página...) están en `application.properties`, no quemados en el código.
- Las eliminaciones son lógicas: un apartamento retirado deja de salir en los listados, pero las reservas viejas lo siguen referenciando.

## Cómo ejecutarlo

Necesitas tener instalado el **JDK 25**.

```bash
git clone https://github.com/julianaVL07/sga.git
cd sga/sga
./gradlew bootRun          # en Windows (PowerShell): .\gradlew.bat bootRun
```

Cuando en la consola salga `Started SgaApplication`, la API queda en `http://localhost:8080`.

| Qué | Dónde |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Consola H2 | http://localhost:8080/h2-console (URL `jdbc:h2:mem:sgadb`, usuario `sa`, sin contraseña) |
| Contrato OpenAPI | `sga/docs/api/openapi.yaml` |

Para correr las pruebas:

```bash
./gradlew test
```

## Datos iniciales

La base es en memoria, así que cada vez que arranca la app se cargan solos estos datos:

- El alojamiento Coral, con sus horarios, normas, parámetros y servicios adicionales (Anexo A).
- Las temporadas Base, Media y Alta.
- Los 5 apartamentos (APT-101, APT-102, APT-201, APT-202 y APT-301), con tarifa en todas las temporadas.
- APT-202 fuera de servicio y con un bloqueo, y APT-301 con un bloqueo vigente.
- Reservas en todos los estados (PENDIENTE, CONFIRMADA, EN_CURSO, FINALIZADA, CANCELADA y NO_SHOW) y por los tres canales (PORTAL, DIRECTO y EXTERNO), cada una con su folio.

## Endpoints principales

**Reservas**
- `POST /api/reservas` crear una reserva
- `GET /api/reservas` listar con filtros (estado, apartamento, canal, titular y fechas), de 10 en 10
- `GET /api/reservas/{codigo}` consultar una reserva
- `PUT /api/reservas/{codigo}` modificar
- `PUT /api/reservas/{codigo}/confirmar`, `/cancelar` y `/no-show`
- `PUT /api/reservas/{codigo}/registrar-llegada` y `/registrar-salida`

**Apartamentos y cotización**
- `GET /api/apartamentos/disponibles` buscar disponibles por fechas y grupo, con filtros y paginación
- `GET /api/apartamentos` y `GET /api/apartamentos/{id}` listado y detalle
- `PUT /api/apartamentos/{id}/retirar` retirar de la venta (eliminación lógica)
- `PUT /api/apartamentos/{id}/estado-operativo`
- `POST /api/apartamentos/{id}/bloqueos` y `/novedades`
- `POST /api/cotizaciones` valor de la estancia noche por noche

**Folio**
- `POST /api/reservas/{codigo}/folio/pagos`
- `PUT /api/reservas/{codigo}/folio/cerrar`
- `POST /api/reservas/{codigo}/folio/autorizacion-cierre`

**Canal externo**
- `GET /api/canales/{canal}/disponibilidad`
- `POST /api/canales/{canal}/reservas`
- `PUT /api/canales/{canal}/reservas/{identificadorExterno}/cancelar`

El detalle de cada endpoint (parámetros, respuestas y errores) está en Swagger y en el `openapi.yaml`.

Los errores siempre vuelven con el mismo formato (`ErrorResponse`): **400** cuando el mensaje está mal escrito, **404** cuando el recurso no existe y **409** cuando una regla del negocio no lo permite.

## Probar la API

En `sga/docs/api/sga.http` dejamos una colección que recorre el flujo completo de una reserva y prueba un 409 por cada regla que expone la API, además de los 400 y 404.

1. Arranca la app desde cero.
2. Abre `sga.http` en IntelliJ y dale a **Run All Requests in File**. En VS Code se puede usar la extensión REST Client.
3. Si la quieres volver a correr, reinicia la app primero: la colección cambia los datos.

## Documentación

- `sga/docs/api/openapi.yaml` contrato de la API
- `sga/docs/trazabilidad/matriz-trazabilidad.md` qué regla se protege dónde y qué endpoint la expone
- `sga/docs/entrega-2/cambios-frente-entrega-1.md` qué agregamos o cambiamos frente a la Entrega 1

## Estado del proyecto

- **Entrega 1:** dominio con sus reglas y pruebas unitarias.
- **Entrega 2:** casos de uso, API REST, persistencia en H2 con datos iniciales, consultas paginadas y documentación.
- **Pendiente:** la seguridad (registro, inicio de sesión con JWT y roles), el frontend en React y el despliegue, que van en los siguientes cortes.

## Forma de trabajo

Trabajamos sobre `develop` con una rama por integrante (`feature/sara-reservas`, `feature/laura-apartamentos` y `feature/juliana-folio`) y Pull Requests para integrar los cambios. `master` guarda las versiones entregadas.

| Integrante | Módulo |
|---|---|
| Sara | Reservas: creación, ciclo de vida, listados y datos de prueba de reservas |
| Laura | Apartamentos y operación: disponibilidad, cotización, bloqueos, estado operativo y datos del Coral |
| Juliana | Folio, canal externo y transversal: errores, configuración y documentación |
