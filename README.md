# SGA - Sistema de Gestión de Alojamiento

Proyecto final de Programación Avanzada (Ingeniería de Sistemas y Computación, 2026-2).

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

Usamos **arquitectura hexagonal** (puertos y adaptadores):

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

Antes del primer arranque crea la clave del JWT (ver [Seguridad](#seguridad)): sin ella la aplicación no arranca.

Cuando en la consola salga `Started SgaApplication`, la API queda en `http://localhost:8080`.

| Qué | Dónde |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| Consola H2 | http://localhost:8080/h2-console (URL `jdbc:h2:mem:sgadb`, usuario `sa`, sin contraseña) |
| Contrato OpenAPI | `sga/docs/api/openapi.yaml` |

Para correr las pruebas:

```bash
./gradlew clean test       # en Windows (PowerShell): .\gradlew.bat clean test
```

El reporte queda en `sga/build/reports/tests/test/index.html`.

## Datos iniciales

La base es en memoria, así que cada vez que arranca la app se cargan solos estos datos. Las fechas de los bloqueos y de las reservas son relativas al día en que arranca (hoy en Colombia):

- El alojamiento Coral, con sus horarios (entrada 15:00, salida 11:00), normas, parámetros y la política de mascotas (máximo 2 de hasta 10 kg, aseo de $110.000) (Anexo A).
- Tres servicios adicionales: recepción nocturna ($70.000, RP-03), paseo privado por la bahía ($420.000) y cena en balcón ($260.000).
- Las temporadas Base, Media y Alta, con sus rangos de 2026 y comienzos de 2027.
- La versión 1 de la política de cancelación, con los tramos de `application.properties` (16 días o más 0 %, de 7 a 15 días 25 %, menos de 7 días o no-show 50 %).
- Los 5 apartamentos, con tarifa en todas las temporadas:

| Apartamento | Nombre | Capacidad | Estado al arrancar |
|---|---|---|---|
| APT-101 | Coral Luminoso | 2 | PREPARADO |
| APT-102 | Arrecife Rosado | 2 | PREPARADO |
| APT-201 | Marea Dorada | 4 | OCUPADO (tiene la reserva EN_CURSO) |
| APT-202 | Bahía Cristal | 4 | FUERA_DE_SERVICIO, con un bloqueo de hoy+3 a hoy+15 |
| APT-301 | Gran Coral Imperial | 6 | PREPARADO, con un bloqueo de hoy+20 a hoy+25 |

- 7 reservas en todos los estados y por los tres canales, cada una con su folio:

| Estado | Apartamento | Estancia | Canal |
|---|---|---|---|
| FINALIZADA | APT-101 | hoy-10 a hoy-7 | DIRECTO |
| NO_SHOW | APT-102 | hoy-5 a hoy-3 | PORTAL |
| EN_CURSO | APT-201 | hoy-1 a hoy+2 | DIRECTO |
| PENDIENTE | APT-101 | hoy+10 a hoy+12 | PORTAL |
| CONFIRMADA | APT-102 | hoy+12 a hoy+15 | DIRECTO |
| CANCELADA | APT-101 | hoy+20 a hoy+22 | PORTAL |
| PENDIENTE | APT-201 | hoy+15 a hoy+18 | EXTERNO (sin hora de llegada) |

## Endpoints

Son 52 operaciones: las 50 de esta lista y las 2 de autenticación (ver [Seguridad](#seguridad)). Todos los listados devuelven 10 resultados por página y las páginas empiezan en 0 (`?pagina=`).

**Reservas**
- `POST /api/reservas` crear una reserva (201 con `Location`); acepta mascotas opcionales (`cantidadMascotas`, `pesoMascotaMasPesadaKg`)
- `GET /api/reservas` listar con filtros (estado, apartamento, canal, titular y fechas), de la más reciente a la más antigua
- `GET /api/reservas/{codigo}` consultar una reserva
- `PUT /api/reservas/{codigo}` modificar fechas, ocupantes, apartamento o mascotas
- `PUT /api/reservas/{codigo}/confirmar` confirmar (el cuerpo con la hora de llegada es opcional)
- `PUT /api/reservas/{codigo}/cancelar` cancelar con la retención de la política congelada
- `PUT /api/reservas/{codigo}/no-show` declarar no-show
- `GET /api/reservas/llegadas` y `GET /api/reservas/salidas` llegadas y salidas previstas de un día (hoy si no se manda `fecha`)

**Operación en recepción**
- `PUT /api/reservas/{codigo}/registrar-llegada` registrar la llegada (el apartamento pasa a OCUPADO)
- `PUT /api/reservas/{codigo}/registrar-salida` registrar la salida (el apartamento pasa a PENDIENTE_PREPARACION)
- `GET /api/reservas/calendario` calendario de ocupación de un apartamento noche por noche (máximo 62 noches)

**Apartamentos**
- `GET /api/apartamentos/disponibles` buscar disponibles por fechas y grupo, con filtros (capacidad, característica, precio y fechas de nacimiento)
- `GET /api/apartamentos` listado de los no retirados
- `GET /api/apartamentos/{id}` detalle con dotación, imágenes, tarifas y bloqueos vigentes
- `POST /api/apartamentos` registrar un apartamento (nace inactivo)
- `PUT /api/apartamentos/{id}` actualizar; si baja la capacidad, avisa qué reservas la superan
- `PUT /api/apartamentos/{id}/tarifas` definir tarifas por temporada
- `GET /api/apartamentos/{id}/tarifas/historico` histórico de tarifas
- `PUT /api/apartamentos/{id}/activar` ponerlo a la venta (exige tarifa en todas las temporadas)
- `PUT /api/apartamentos/{id}/retirar` retirar de la venta (eliminación lógica)
- `PUT /api/apartamentos/{id}/iniciar-preparacion`, `/terminar-preparacion`, `/poner-fuera-de-servicio` y `/restablecer-servicio` operaciones del estado operativo
- `GET /api/apartamentos/pendientes-preparacion` los que están en PENDIENTE_PREPARACION o EN_PREPARACION
- `POST /api/apartamentos/{id}/bloqueos` registrar un bloqueo (201 con `Location`)
- `GET /api/apartamentos/{id}/bloqueos/{idBloqueo}` consultar un bloqueo, también si ya se levantó (es el recurso del `Location`)
- `POST /api/apartamentos/{id}/novedades` registrar una novedad (201 con `Location`)
- `GET /api/apartamentos/{id}/novedades/{idNovedad}` consultar una novedad (es el recurso del `Location`)
- `GET /api/apartamentos/{id}/novedades` historial de novedades, de la más reciente a la más antigua

**Cotizaciones**
- `POST /api/cotizaciones` valor de la estancia noche por noche

**Folio**
- `GET /api/reservas/{codigo}/folio` consultar el folio con sus cargos, pagos y saldo
- `POST /api/reservas/{codigo}/folio/pagos` registrar un pago (201 con `Location` al folio)
- `POST /api/reservas/{codigo}/folio/cargos` cargar un servicio adicional (201 con `Location` al folio)
- `POST /api/reservas/{codigo}/folio/correcciones` corregir un cargo con un movimiento inverso (201 con `Location` al folio)
- `PUT /api/reservas/{codigo}/folio/cerrar` cerrar con saldo cero
- `POST /api/reservas/{codigo}/folio/autorizacion-cierre` el administrador autoriza cerrar con saldo

**Canal externo**
- `GET /api/canales/{canal}/disponibilidad` disponibilidad paginada para el canal
- `POST /api/canales/{canal}/reservas` recibir una reserva del canal
- `PUT /api/canales/{canal}/reservas/{identificadorExterno}/cancelar` cancelar una reserva del canal

**Configuración del alojamiento**
- `GET /api/alojamiento` y `PUT /api/alojamiento` consultar y actualizar los datos del alojamiento (los parámetros son de solo lectura)
- `GET /api/temporadas`, `GET /api/temporadas/{id}` y `POST /api/temporadas` listar, consultar y crear temporadas (201 con `Location`)
- `POST /api/temporadas/{id}/rangos` agregar un rango a una temporada (201 con `Location`)
- `GET /api/politica-cancelacion` política vigente
- `GET /api/politica-cancelacion/versiones` versiones de la política
- `PUT /api/politica-cancelacion` publicar una versión nueva

El detalle de cada endpoint (parámetros, respuestas y errores) está en Swagger, que muestra las 52 operaciones, y en el `openapi.yaml`, que todavía describe las 50 sin autenticación.

Los errores siempre vuelven con el mismo formato (`ErrorResponse`): **400** cuando el mensaje está mal escrito, **404** cuando el recurso no existe y **409** cuando una regla del negocio no lo permite.

## Probar la API

En `sga/docs/api/sga.http` dejamos una colección de 179 peticiones que recorre el flujo completo de una reserva y prueba un 409 por cada regla que expone la API, además de los 400 y 404. Cada petición valida su respuesta con `client.test`.

1. Arranca la app desde cero.
2. Abre `sga.http` en IntelliJ y dale a **Run All Requests in File**. En VS Code se puede usar la extensión REST Client.
3. Si la quieres volver a correr, reinicia la app primero: la colección cambia los datos.

## Documentación

- `sga/docs/api/openapi.yaml` contrato de la API
- `sga/docs/trazabilidad/matriz-trazabilidad.md` qué regla se protege dónde y qué endpoint la expone
- `sga/docs/entrega-2/cambios-frente-entrega-1.md` qué agregamos o cambiamos frente a la Entrega 1

## Estado del proyecto

- **Entrega 1:** dominio con sus reglas y pruebas unitarias.
- **Entrega 2:** casos de uso, API REST (50 operaciones), persistencia en H2 con datos iniciales, consultas paginadas y documentación. Además, frente a la Entrega 1 hicimos estos cambios (el detalle está en `sga/docs/entrega-2/cambios-frente-entrega-1.md`):
    - Reservas: reglas de canal en el dominio, mascotas en la reserva (L-16) y pruebas unitarias de RN-04, RN-10, RN-12, RN-14, RN-21, RN-22, RP-03, L-15 y L-16.
    - Apartamentos y operación: operaciones del estado operativo en lugar del cambio genérico, cada una con su estado de origen, `Location` en bloqueos y novedades con su consulta por id, historial de novedades y apartamentos pendientes de preparación.
    - Folio y transversal: depósito como `AJUSTE_MODIFICACION`, consulta del folio, cargos de servicios adicionales y correcciones (RN-16), `Location` en pagos y temporadas, consulta de una temporada y disponibilidad del canal paginada.
    - Pruebas unitarias de las reglas de configuración (L-03, L-06, L-09, L-12, L-13, L-14 y L-17), del titular facturable (3.2), del apartamento activo (3.3) y del formato del código de reserva.
- **Verificación:** las 262 pruebas pasan (`./gradlew test --rerun`), las 179 peticiones de `sga.http` pasan contra la app recién arrancada y la matriz de trazabilidad dice qué prueba unitaria y de integración cubre cada regla.

## Seguridad

Los endpoints exigen un token JWT, salvo el login, el registro y las herramientas de desarrollo (Swagger y consola H2). Cada endpoint define qué roles lo usan y, en reservas y folios, el huésped solo accede a lo suyo: **401** es que falta el token o no sirve, **403** que el rol no lo permite o la reserva no es suya.

**Crear la clave.** La aplicación no arranca sin ella y nunca va al repositorio. En `sga/`:

```bash
echo "sga.jwt.secret=$(openssl rand -base64 48)" > src/main/resources/application-secrets.properties
```

La plantilla es `application-secrets.properties.example`. En despliegue se usa la variable de entorno `SGA_JWT_SECRET`.

**Usuarios iniciales.** Son datos de prueba de H2, no credenciales reales:

| Rol | Correo | Contraseña |
|---|---|---|
| ADMINISTRADOR | `admin@sga.local` | `Admin2026` |
| RECEPCIONISTA | `recepcion@sga.local` | `Recepcion2026` |
| PERSONAL_SERVICIO | `servicio@sga.local` | `Servicio2026` |
| HUESPED (Laura Gomez) | `huesped1@sga.local` | `Huesped2026` |
| HUESPED (Valentina Rios) | `huesped2@sga.local` | `Huesped2026` |

**Login y registro**, ambos públicos:

- `POST /api/auth/login` recibe `correo` y `contrasena` y devuelve el token, que dura 30 minutos.
- `POST /api/auth/registro` crea un huésped (201) con `nombre`, `correo`, `contrasena`, `telefono`, `documento` y `fechaNacimiento`. Responde 409 si el correo o el documento ya existen.

```bash
curl -X POST http://localhost:8080/api/auth/login -H "Content-Type: application/json" \
  -d '{"correo":"recepcion@sga.local","contrasena":"Recepcion2026"}'
```

**Usar el token.** Envíelo en el encabezado `Authorization: Bearer <token>`. En Swagger UI, péguelo en el botón **Authorize**.

## Forma de trabajo

Trabajamos sobre `develop` con una rama por integrante (`feature/sara-reservas`, `feature/laura-apartamentos` y `feature/juliana-folio`) y Pull Requests para integrar los cambios. `master` guarda las versiones entregadas.

| Integrante | Módulo |
|---|---|
| Sara | Reservas: creación, ciclo de vida, listados y datos de prueba de reservas |
| Laura | Apartamentos y operación: disponibilidad, cotización, bloqueos, estado operativo y datos del Coral |
| Juliana | Folio, canal externo y transversal: errores, configuración y documentación |