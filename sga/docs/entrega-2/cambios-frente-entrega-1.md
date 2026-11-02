# Cambios de la entrega 2 frente a la entrega 1

La Clase 15 pide documentar toda agregación o cambio para mantener la trazabilidad entre entregas. Este documento
compara el estado del repositorio al cerrar la entrega 1 (commit `0da1e65`, *Merge pull request #15*, 29-oct-2026)
con el de la entrega 2. La relación regla → código → endpoint → prueba está en
[`docs/trazabilidad/matriz-trazabilidad.md`](../trazabilidad/matriz-trazabilidad.md) y el recorrido ejecutable de la API
en [`docs/api/sga.http`](../api/sga.http).

## 1. Casos de uso nuevos

| Caso de uso | Qué hace | Actor | Clases |
|---|---|---|---|
| **CU-19 Listar reservas** | Lista las reservas de la más reciente a la más antigua, 10 por página (7.5 "Listados", 12.4). Filtros opcionales: estado, apartamento, canal de origen, documento del titular y rango de fecha de entrada (`desde`/`hasta`, ambas incluidas). Si `desde` es posterior a `hasta` responde 409 (7.5). | Recepcionista, administrador | `ListarReservasUseCase`, `FiltroReservas`, `ReservaRepository.listar`, `ReservaResumenResponse` |
| **CU-20 Consultar apartamentos** | Lista paginada de los apartamentos no eliminados, ordenada por identificación, y detalle de uno con dotación, imágenes, capacidad, estado operativo, tarifas por temporada y bloqueos vigentes (7.3). Un apartamento retirado responde 404. | Administrador | `ConsultarApartamentosUseCase`, `ApartamentoRepository.listarNoEliminados`, `ApartamentoDetalleResponse` |
| **CU-21 Retirar apartamento** | Eliminación lógica: el apartamento deja de venderse y de aparecer en listados, detalle y búsquedas, pero su historia (reservas, folios) se conserva. Solo procede si no tiene reservas activas ni futuras, y no se retira dos veces (409, 7.3). No existe `DELETE` (12.4). | Administrador | `RetirarApartamentoUseCase`, `RetiroApartamentoService`, `Apartamento.retirarDeLaVenta` |
| **CU-22 Registrar apartamento** | Registra un apartamento con datos, dotación, imágenes (1 a 10, una principal) y, opcionalmente, tarifas por temporada. Nace PREPARADO e **inactivo**: solo se vende cuando tiene tarifa en todas las temporadas (7.4). Identificación repetida (L-04) o más de 15 apartamentos (L-03) responden 409. | Administrador | `RegistrarApartamentoUseCase`, `AdministracionApartamentoService.registrar` |
| **CU-23 Actualizar apartamento** | Cambia nombre, descripción, dormitorios, dotación y capacidad. Las reservas ya creadas no se modifican: la respuesta lista las activas o futuras cuyo grupo supera la nueva capacidad, con una advertencia (7.3). | Administrador | `ActualizarApartamentoUseCase`, `AdministracionApartamentoService.actualizar`, `ResultadoActualizacionApartamento` |
| **CU-24 Definir tarifas e histórico** | Define una o varias tarifas por temporada que rigen desde hoy; la anterior queda en el histórico con sus fechas de vigencia (ver sección 5). Consulta paginada del histórico. | Administrador | `TarifasApartamentoUseCase`, `AdministracionApartamentoService.definirTarifas`, `Apartamento.definirTarifa`, `RegistroTarifa` |
| **CU-25 Activar apartamento** | Pone el apartamento a la venta solo si tiene tarifa en todas las temporadas (7.4); uno retirado no se activa (7.3). | Administrador | `ActivarApartamentoUseCase`, `AdministracionApartamentoService.activar`, `Apartamento.activar` |
| **CU-26 Llegadas y salidas previstas** | Reservas CONFIRMADA que entran y EN_CURSO que salen en una fecha (hoy por defecto, en America/Bogota), ordenadas por apartamento y paginadas (7.1, 7.6). | Recepcionista | `ListarLlegadasYSalidasUseCase`, `ReservaRepository.listarLlegadasPrevistas` / `listarSalidasPrevistas` |
| **CU-27 Calendario de ocupación** | Estado de cada noche de un apartamento entre dos fechas: LIBRE, RESERVADA (con código y estado de la reserva) o BLOQUEADA (con el motivo). Máximo 62 noches por consulta (decisión del equipo, 7.1). | Recepcionista | `ConsultarCalendarioOcupacionUseCase`, `CalendarioOcupacionService`, `NocheCalendario`, `EstadoNoche` |
| **CU-28 Consultar y actualizar el alojamiento** | Consulta datos, ubicación, horario, normas, servicios adicionales y parámetros vigentes (solo lectura). Actualiza nombre, descripción, ciudad, dirección, coordenada, normas y, si se envía, el horario (L-12). | Administrador | `ConfiguracionAlojamientoUseCase`, `Alojamiento.actualizarDatos`, `Alojamiento.definirHorario` |
| **CU-29 Gestionar temporadas** | Lista las temporadas con sus rangos, crea una temporada con la tarifa de **cada apartamento activo** en una sola operación (7.4) y agrega rangos a una temporada existente. Rechaza solapes (7.4) y una segunda temporada base (F-05). | Administrador | `TemporadasUseCase`, `CreacionTemporadaService`, `Alojamiento.agregarTemporada`, `Alojamiento.agregarRangoATemporada` |
| **CU-30 Política de cancelación versionada** | Consulta la versión vigente y el histórico de versiones, y publica una versión nueva vigente desde hoy; la anterior se conserva y las reservas existentes siguen con la suya (7.4, RN-22). Ver sección 4. | Administrador | `PoliticaCancelacionUseCase`, `PoliticaCancelacionService`, `PoliticaCancelacionRepository` |

Además, **CU-01 Buscar apartamentos disponibles** se amplió con los filtros y la paginación de 7.5 (ver sección 2).

## 2. Endpoints

### Nuevos

| Método y ruta | Caso de uso | Respuesta | Errores |
|---|---|---|---|
| `GET /api/reservas?estado=&apartamento=&canal=&titular=&desde=&hasta=&pagina=` | CU-19 | 200 `PaginaResponse<ReservaResumenResponse>` | 400 (página negativa, enum o fecha mal escritos), 409 (`desde` > `hasta`) |
| `GET /api/apartamentos?pagina=` | CU-20 | 200 `PaginaResponse<ApartamentoResponse>` | 400 (página negativa) |
| `GET /api/apartamentos/{id}` | CU-20 | 200 `ApartamentoDetalleResponse` | 404 (no existe o fue retirado) |
| `PUT /api/apartamentos/{id}/retirar` | CU-21 | 200 `ApartamentoDetalleResponse` con `eliminado = true` | 404, 409 (reservas activas o futuras, o ya retirado) |
| `POST /api/apartamentos` | CU-22 | 201 `ApartamentoDetalleResponse` + `Location` | 400, 409 (L-03, L-04, imágenes 7.3, tarifas 7.4) |
| `PUT /api/apartamentos/{id}` | CU-23 | 200 `ActualizacionApartamentoResponse` (apartamento, `reservasSobreCapacidad` y `advertencia`) | 400, 404, 409 (capacidad, dormitorios o dotación inválidos) |
| `PUT /api/apartamentos/{id}/tarifas` | CU-24 | 200 `ApartamentoDetalleResponse` | 400, 404, 409 (temporada inexistente o repetida, valor ≤ 0) |
| `GET /api/apartamentos/{id}/tarifas/historico?pagina=` | CU-24 | 200 `PaginaResponse<RegistroTarifaResponse>` | 400, 404 |
| `PUT /api/apartamentos/{id}/activar` | CU-25 | 200 `ApartamentoDetalleResponse` | 404, 409 (sin tarifa en todas las temporadas, o retirado) |
| `GET /api/reservas/llegadas?fecha=&pagina=` y `GET /api/reservas/salidas?fecha=&pagina=` | CU-26 | 200 `PaginaResponse<ReservaResumenResponse>` | 400 |
| `GET /api/reservas/calendario?apartamento=&desde=&hasta=` | CU-27 | 200 `CalendarioOcupacionResponse` | 400, 404, 409 (`desde` ≥ `hasta` o más de 62 noches) |
| `GET /api/alojamiento` | CU-28 | 200 `AlojamientoResponse` (con `serviciosAdicionales` y `parametros`) | — |
| `PUT /api/alojamiento` | CU-28 | 200 `AlojamientoResponse` | 400, 409 (coordenada fuera de rango 7.3, horario incompleto L-12) |
| `GET /api/temporadas?pagina=` | CU-29 | 200 `PaginaResponse<TemporadaResponse>` | 400 |
| `POST /api/temporadas` | CU-29 | 201 `TemporadaResponse` | 400, 409 (falta la tarifa de un apartamento activo 7.4, solape 7.4, segunda base F-05) |
| `POST /api/temporadas/{id}/rangos` | CU-29 | 201 `TemporadaResponse` con todos sus rangos | 400, 404, 409 (solape 7.4, rango en la base F-05) |
| `GET /api/politica-cancelacion` | CU-30 | 200 `PoliticaCancelacionResponse` (versión vigente) | — |
| `GET /api/politica-cancelacion/versiones?pagina=` | CU-30 | 200 `PaginaResponse<PoliticaCancelacionResponse>`, de la más reciente a la más antigua | 400 |
| `PUT /api/politica-cancelacion` | CU-30 | 200 `PoliticaCancelacionResponse` (versión N+1) | 400, 409 (L-10) |

### Modificado

| Método y ruta | Cambio | Compatibilidad |
|---|---|---|
| `GET /api/apartamentos/disponibles` | Nuevos parámetros opcionales: `fechasNacimiento` (una por ocupante, para estimar solo a los facturables, RN-06), `capacidadMinima`, `precioMinimo`, `precioMaximo`, `caracteristica` y `pagina`. Cada resultado es una tarjeta (`ApartamentoDisponibleResponse`) con nombre, imagen principal, capacidad, **valor estimado de la estancia** y ubicación del alojamiento. Filtros inconsistentes responden 409 (7.5). | **Cambia la forma de la respuesta:** antes era una lista `List<ApartamentoResponse>`; ahora es una página `PaginaResponse<ApartamentoDisponibleResponse>` con `contenido`, `pagina`, `tamano`, `totalElementos` y `totalPaginas`. |

Todos los listados (`/api/reservas`, `/api/reservas/llegadas`, `/api/reservas/salidas`, `/api/apartamentos`,
`/api/apartamentos/disponibles`, `/api/apartamentos/{id}/tarifas/historico`, `/api/temporadas` y
`/api/politica-cancelacion/versiones`) devuelven 10 resultados por página (`sga.paginacion.tamano=10`) y numeran
las páginas desde 0. El contrato `docs/api/openapi.yaml` incluye todos los endpoints nuevos y el modificado, y se
verificó contra lo que publica Swagger (`/v3/api-docs`, `/swagger-ui.html`) con la aplicación recién arrancada: las
39 operaciones coinciden en ruta, método, parámetros, cuerpo, códigos de respuesta y campos de la petición y de la
respuesta exitosa.

## 3. Clases nuevas

### Dominio (Java puro, sin Spring, JPA ni Lombok)

| Clase | Tipo | Responsabilidad |
|---|---|---|
| `domain/repository/Pagina<T>` | record | Página de resultados que devuelven los puertos en lugar de `org.springframework.data.domain.Page`. Valida número ≥ 0, tamaño > 0 y contenido no nulo; copia el contenido de forma inmutable; calcula `totalPaginas()`. `Pagina.de(lista, numero, tamano)` pagina en memoria el resultado de la búsqueda de disponibilidad, que se decide en el dominio. |
| `domain/repository/FiltroReservas` | record | Filtros opcionales de CU-19. Rechaza `desde` posterior a `hasta` (7.5). |
| `domain/valueobject/CriterioBusqueda` | record | Filtros opcionales de CU-01: capacidad mínima, rango de precio sobre el valor estimado de la estancia y característica de la dotación. Rechaza capacidad ≤ 0, precios negativos y precio mínimo mayor que el máximo (7.5). |
| `domain/valueobject/ApartamentoDisponible` | record | Resultado de la búsqueda: el apartamento, su valor estimado y la ubicación del alojamiento. |
| `domain/service/RetiroApartamentoService` | servicio de dominio | CU-21: consulta si hay reservas activas o futuras y delega en `Apartamento.retirarDeLaVenta`. |
| `domain/service/AdministracionApartamentoService` | servicio de dominio | CU-22 a CU-25: registra (L-03, L-04), actualiza y calcula las reservas que superan la nueva capacidad (7.3), define tarifas con histórico y activa (7.4). |
| `domain/valueobject/ResultadoActualizacionApartamento` | record | Resultado de CU-23: el apartamento y los códigos de las reservas que superan la nueva capacidad. |
| `domain/valueobject/RegistroTarifa` | record | Entrada del histórico de tarifas: temporada, valor, `vigenteDesde` y `vigenteHasta` (null mientras rige). Ver sección 5. |
| `domain/service/CalendarioOcupacionService` | servicio de dominio | CU-27: estado de cada noche a partir de las reservas activas (RN-12) y los bloqueos vigentes (RN-07); rango máximo de 62 noches (7.1). |
| `domain/valueobject/NocheCalendario`, `domain/valueobject/EstadoNoche` | record y enum | Una noche del calendario: LIBRE, RESERVADA (código y estado de la reserva) o BLOQUEADA (motivo). |
| `domain/service/CreacionTemporadaService` | servicio de dominio | CU-29: valida la temporada, exige la tarifa de cada apartamento activo (7.4) y solo entonces agrega la temporada al alojamiento y asigna las tarifas, todo en una operación. |
| `domain/repository/PoliticaCancelacionRepository` | puerto | CU-30: `guardar` (solo versiones nuevas), `obtenerVigente` (la de mayor número) y `listarVersiones` (paginado). |
| `domain/service/PoliticaCancelacionService` | servicio de dominio | CU-30: entrega la política vigente que se congela en cada reserva (RN-22), guarda la versión inicial si no hay ninguna y crea la versión N+1. |

### Métodos nuevos en clases existentes del dominio

- `Apartamento.retirarDeLaVenta` y `Apartamento.validarSinReservasPendientes` (7.3). Un apartamento eliminado no se
  puede volver a activar.
- `Dotacion.mencionaCaracteristica`: busca la característica dentro de la dotación sin distinguir mayúsculas ni tildes.
- `DisponibilidadApartamentoService.buscarDisponibles(estancia, ocupantes, fechasNacimiento, criterio)`: primero decide
  la disponibilidad (RN-01, RN-02, RN-07, RN-20, RP-01) y después aplica los filtros.
- `CotizadorEstanciaService.estimar`: valor estimado de la estancia; sin fechas de nacimiento, cuenta a todos los
  ocupantes como facturables.
- Puertos: `ReservaRepository.listar(filtro, pagina, tamano)` y `ApartamentoRepository.listarNoEliminados(pagina, tamano)`.
- `Apartamento.definirTarifa(tarifa, vigenteDesde)`, `Apartamento.definirTarifas` (todo o nada) y
  `Apartamento.getHistoricoTarifas` (CU-24, 7.4).
- `ReservaRepository.listarLlegadasPrevistas` y `listarSalidasPrevistas` paginados (CU-26).
- `Alojamiento.agregarRangoATemporada` y `Alojamiento.buscarTemporada`, y `Temporada.seSolapaCon(RangoFechas)` (CU-29):
  un rango nuevo no puede solaparse con otra temporada. El solape entre temporadas completas y la base única ya los
  validaba `Alojamiento.agregarTemporada`; se reutilizan, no se duplican.
- `PoliticaCancelacion.nuevaVersion` (CU-30): crea la versión siguiente, que no puede regir antes que la vigente (F-07)
  y vuelve a validar L-10 en el constructor del record.

### Aplicación e infraestructura

- Casos de uso: `ListarReservasUseCase`, `ConsultarApartamentosUseCase`, `RetirarApartamentoUseCase`,
  `RegistrarApartamentoUseCase`, `ActualizarApartamentoUseCase`, `TarifasApartamentoUseCase`,
  `ActivarApartamentoUseCase`, `ListarLlegadasYSalidasUseCase`, `ConsultarCalendarioOcupacionUseCase`,
  `ConfiguracionAlojamientoUseCase`, `TemporadasUseCase` y `PoliticaCancelacionUseCase`. Los listados leen el tamaño
  de página de `sga.paginacion.tamano`.
- DTO: `PaginaResponse<T>` (envoltorio genérico), `ReservaResumenResponse`, `ApartamentoDisponibleResponse`,
  `ApartamentoDetalleResponse` y los de CU-22 a CU-30. Siguiendo la rúbrica, los DTO no usan tipos del dominio salvo
  enums: la conversión de `Pagina` del dominio a `PaginaResponse` la hace `infrastructure/rest/mapper/PaginaMapper`
  (el JSON no cambió).
- Controllers nuevos: `OcupacionController` (CU-27), `AlojamientoController` (CU-28), `TemporadaController` (CU-29)
  y `PoliticaCancelacionController` (CU-30), con el mapper `ConfiguracionMapper`. CU-22 a CU-25 están en
  `ApartamentoController` y CU-26 en `ReservaController`. `TemporadaNoEncontradaException` responde 404.
- Persistencia: tabla `apartamento_tarifa_historico` (`RegistroTarifaEmbedable`) para el histórico de tarifas, y
  tablas `politica_cancelacion` y `politica_cancelacion_tramo` (`PoliticaCancelacionEntity`, que reutiliza
  `TramoCancelacionEmbedable`) con su adaptador `PoliticaCancelacionJpaRepository`, su mapper y la versión en memoria
  `PoliticaCancelacionRepositoryInMemory` para las pruebas.
- Persistencia: consulta JPQL paginada y filtrada de reservas (`ReservaJpaDataRepository`) y listado paginado de
  apartamentos no eliminados; los adaptadores JPA traducen `Page` de Spring Data a `Pagina` del dominio.
- `DatosInicialesReservasConfig`: al arrancar crea reservas en los 6 estados y por los 3 canales (12.3), con fechas
  relativas a hoy. `DatosInicialesCoralConfig` agrega un apartamento fuera de servicio con bloqueo (APT-202) y un
  bloqueo vigente (APT-301).
- `PoliticaCancelacionProperties`, `DatosInicialesPoliticaCancelacionConfig` y `OpenApiConfig` (ver secciones 4 y 8).

## 4. Política de cancelación versionada y demás parámetros movidos a configuración

En la entrega 1, `DomainServiceConfig.politicaVigente()` construía la política de cancelación con valores escritos
en el código. Hubo dos cambios en esta entrega.

**Primero, a configuración.** La rúbrica pide los parámetros de la Ficha en `application.properties`, sin valores
del negocio quemados en el código. La política pasó a `sga.politica-cancelacion.version`, `.vigente-desde`,
`.consecuencia-no-show` y `.tramos[i].dias-antelacion` / `.porcentaje-retencion`, que lee el record
`infrastructure/config/PoliticaCancelacionProperties` (`@ConfigurationProperties`).

**Después, versionada y persistida (CU-30).** Con un `@Bean` fijo solo existía una versión y cambiarla exigía
reiniciar la aplicación. La sección 7.4 pide una sola política vigente, conservar la anterior al actualizarla y que
cada reserva quede con la versión vigente al crearse (RN-22). Ahora:

- Las versiones se guardan en la base de datos con el puerto `PoliticaCancelacionRepository` (tablas
  `politica_cancelacion` y `politica_cancelacion_tramo`). Solo se agregan: una versión guardada nunca se modifica.
  La vigente es la de mayor número.
- Al arrancar, `DatosInicialesPoliticaCancelacionConfig` guarda la **versión 1** con los valores de
  `sga.politica-cancelacion.*`, solo si todavía no hay ninguna. Si esos valores violan L-10 la aplicación no arranca.
- Se eliminó el `@Bean politicaVigente`. La creación de reservas (`CrearReservaUseCase`), el canal externo
  (`ConciliacionCanalExternoService`) y los datos de prueba de reservas toman la versión vigente del repositorio con
  `PoliticaCancelacionService.vigente()` en el momento de crear cada reserva.
- `PUT /api/politica-cancelacion` crea la versión N+1, vigente desde hoy, con `PoliticaCancelacion.nuevaVersion`,
  que valida L-10 en el dominio (409). La versión N queda igual y se consulta en
  `GET /api/politica-cancelacion/versiones`.
- Las reservas existentes conservan la versión que congelaron: `versionPoliticaCancelacion` sigue igual en su
  detalle y la retención de RN-13 se calcula con esos tramos. Lo comprueban
  `ConfiguracionAlojamientoIntegracionTest#actualizarPoliticaCreaVersion2` y las peticiones 11.15 a 11.19 de
  `sga.http`.

Las propiedades `sga.politica-cancelacion.*` ya no se editan para publicar una política nueva: solo definen la
versión inicial (ver sección 6).

En la misma revisión se movieron los demás valores del negocio que seguían escritos en el código:

| Valor | Antes | Ahora (`application.properties`) |
|---|---|---|
| Concepto del depósito de garantía (RP-02) | texto en `DomainServiceConfig.depositoGarantia` | `sga.alojamiento.deposito-garantia-concepto` |
| Estancia mínima de fin de semana (RP-01) | `new EstanciaMinima(2, true)` en `DatosInicialesCoralConfig` | `sga.alojamiento.estancia-minima-noches`, `sga.alojamiento.estancia-minima-activa` |
| Exigir anticipo (L-11) | `true` en `DatosInicialesCoralConfig` | `sga.alojamiento.exige-anticipo` (el porcentaje ya estaba en `sga.alojamiento.porcentaje-anticipo`) |
| Política de mascotas (L-16) | `new CargoAseoMascota(true, 110.000, 2, 10)` | `sga.alojamiento.mascotas.aceptadas`, `.cargo-aseo`, `.maximo`, `.peso-maximo-kg` |
| Valor de la recepción nocturna (RP-03) | `Dinero.de(70_000)` | `sga.alojamiento.valor-recepcion-nocturna` |

Se dejaron en el código, a propósito:

- El **catálogo de datos iniciales** (identidad del alojamiento, temporadas, tarifas, inventario de apartamentos y
  demás servicios adicionales, A.1 a A.3 y A.5). Son datos que se cargan, no parámetros.
- Las **reservas de demostración** (12.3).
- Las **reglas generales de la plataforma** del dominio (5 a 15 apartamentos, máximo 10 imágenes), que valen para
  cualquier alojamiento.

## 5. Histórico de tarifas (CU-24, 7.4)

En la entrega 1 un apartamento guardaba una sola tarifa por temporada: al cambiarla se perdía la anterior. La sección
7.4 pide conservar el histórico. Ahora:

- `Apartamento.definirTarifa(tarifa, vigenteDesde)` cierra el registro vigente de esa temporada con la fecha de
  reemplazo (`RegistroTarifa.reemplazadaEl`) y agrega uno nuevo, vigente desde hoy. Una tarifa no puede regir antes
  que la vigente (409, 7.4).
- Las tarifas cargadas sin fecha en los datos iniciales se conservan en el histórico sin fecha de inicio al
  reemplazarlas por primera vez.
- `Apartamento.definirTarifas` aplica varias tarifas a la vez y es todo o nada: si una temporada no existe o se
  repite, no se aplica ninguna.
- La cotización y las reservas usan solo la tarifa vigente (RN-05); el histórico es de consulta.
- El histórico se persiste en `apartamento_tarifa_historico` y se consulta en
  `GET /api/apartamentos/{id}/tarifas/historico`, de la más reciente a la más antigua.
- Las tarifas que asigna CU-29 al crear una temporada también quedan en el histórico, desde el día en que se creó.

## 6. Decisión: los parámetros de la sección 6 no se cambian por API

Los parámetros de la sección 6 de la Ficha viven en `application.properties` (`sga.alojamiento.*`) y **no hay
ningún endpoint que los modifique**:

- Umbral de edad facturable (L-09), anticipo (L-11), tiempo de preparación (L-13), plazo de confirmación (L-14),
  hora límite de no-show (L-15), política de mascotas (L-16), estancia mínima (RP-01), depósito de garantía
  (RP-02) y recepción nocturna (RP-03). Tampoco se cambia por API el tamaño de página (`sga.paginacion.tamano`,
  12.4).
- `GET /api/alojamiento` los muestra en `parametros` como **solo lectura**. `PUT /api/alojamiento` solo cambia los
  datos del alojamiento: nombre, descripción, ciudad, dirección, coordenada, normas de convivencia y horario.
  El horario (L-12) se carga al arrancar desde `sga.alojamiento.hora-entrada` y `hora-salida`, pero es un dato del
  alojamiento: el dominio lo permite cambiar con `Alojamiento.definirHorario`.
- Para cambiar un parámetro hay que editar `application.properties` y reiniciar. Así lo pide la rúbrica: son
  configuración del despliegue, no datos que se editen en operación.
- La política de cancelación es la excepción, porque 7.4 exige versionarla: `sga.politica-cancelacion.*` solo
  define la versión 1 y las siguientes se publican con CU-30 sin tocar las propiedades (ver sección 4).

## 7. Decisión: no hay filtro de servicios en la búsqueda

La sección 7.5 menciona filtrar por servicios. En el modelo del SGA los servicios son del **alojamiento**
(`ServicioAdicional`: recepción nocturna, paseo por la bahía, cena en el balcón) y se ofrecen igual en todos los
apartamentos. Un filtro por servicio devolvería siempre todos los apartamentos o ninguno, así que no sirve para
distinguir uno de otro.

Lo que sí distingue a un apartamento es su **dotación** (tina de hidromasaje, piscina de inmersión, cava de vinos…).
Por eso la búsqueda filtra con `caracteristica`, que busca el texto dentro de la dotación sin distinguir mayúsculas
ni tildes (`Dotacion.mencionaCaracteristica`).

La decisión está documentada en el Javadoc de `CriterioBusqueda` y en la descripción de
`GET /api/apartamentos/disponibles` en `openapi.yaml`. Si en una entrega futura los servicios pasaran a ser de cada
apartamento, se agregaría un campo a `CriterioBusqueda` sin cambiar el resto de la búsqueda.

## 8. Contrato y documentación de la API

- `docs/api/openapi.yaml` quedó alineado con los controllers. Se agregaron:
  - el 409 de `GET /api/reservas/{codigo}` (código mal formado),
  - el 404 de `POST /api/apartamentos/{id}/bloqueos`,
  - el 404 de `POST /api/cotizaciones`.

  También se igualaron los formatos y las descripciones de los tags.
- `infrastructure/config/OpenApiConfig` hace que Swagger UI muestre:
  - el título y la versión del contrato,
  - los tags en el orden del yaml,
  - `ErrorResponse` como cuerpo de los errores.

  Antes springdoc pintaba los 400/404/409 con el esquema de la respuesta exitosa. Las respuestas 409/422 del canal
  externo conservan `EventoCanalResponse`, que es lo que de verdad devuelven.
- Los endpoints de CU-22 a CU-30 se agregaron al contrato con sus esquemas. La comparación con Swagger da 39
  operaciones iguales (ver sección 2).
- `docs/api/sga.http`: recorrido completo y ejecutable de la API contra la aplicación recién arrancada:
  - el flujo de una reserva de punta a punta,
  - un 409 por cada regla expuesta,
  - ejemplos de 400 y 404,
  - la sección 9 (CU-22 a CU-25), la 10 (CU-26 y CU-27) y la 11 (CU-28 a CU-30, incluida la política versionada).

  Ejecutado completo y en orden contra la aplicación recién arrancada, pasan las 138 peticiones.
- `docs/trazabilidad/matriz-trazabilidad.md`: matriz regla → clase y método → caso de uso → endpoint → código →
  prueba.

## 9. Pendiente: seguridad con JWT (7.2)

La autenticación y autorización con JWT **no se implementa en esta entrega**. Todos los endpoints están abiertos.
Las restricciones por actor se documentan en los casos de uso, pero hoy no se aplican. Por ejemplo:

- CU-07 (autorizar el cierre del folio con saldo) y CU-21 (retirar un apartamento) corresponden solo al administrador.
- CU-16 (cambiar el estado operativo) corresponde al personal de servicio o al administrador.

Cuando se implemente, entrará como adaptador de infraestructura (filtro de Spring Security y configuración de
roles). No toca el dominio ni los casos de uso.
