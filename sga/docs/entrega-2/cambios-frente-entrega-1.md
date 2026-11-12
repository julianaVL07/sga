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

Durante esta entrega también se agregaron operaciones que completan casos de uso existentes. Para que coincidan con el
documento de casos de uso se numeran como casos nuevos (CU-31 a CU-35). La consulta de una temporada sigue dentro
de CU-29, y la de un bloqueo o una novedad dentro de CU-13 y CU-17:

| Caso de uso | Qué hace | Actor | Clases |
|---|---|---|---|
| **CU-31 Consultar el folio** (completa CU-06 y CU-15) | Devuelve el folio de la reserva con sus cargos, pagos y saldo. Es el recurso al que apunta el `Location` de pagos, cargos y correcciones. | Recepcionista, administrador | `ConsultarFolioUseCase` |
| **CU-32 Registrar cargo de servicio adicional** (7.1) | Asienta en el folio el cargo de un servicio que ofrece el alojamiento (por ejemplo, la cena en el balcón). El valor y el concepto los pone el servicio; un servicio que el alojamiento no ofrece responde 409 (L-17). | Recepcionista | `RegistrarCargoServicioUseCase`, `Alojamiento.obtenerServicioAdicional`, `ServicioAdicional.generarCargoPara` |
| **CU-33 Registrar corrección del folio** (RN-16) | Registra el movimiento inverso del cargo que está en una posición del folio, con su motivo. El cargo original no se edita ni se borra. | Recepcionista, administrador | `RegistrarCorreccionFolioUseCase`, `Folio.cargoEn`, `Folio.registrarMovimientoInverso` |
| **CU-34 Consultar el historial de novedades** (7.6) | Novedades de un apartamento, de la más reciente a la más antigua, 10 por página. | Personal de servicio, recepcionista, administrador | `ConsultarNovedadesUseCase`, `NovedadRepository` |
| **CU-35 Consultar apartamentos pendientes de preparación** (7.1) | Apartamentos no retirados en PENDIENTE_PREPARACION o EN_PREPARACION, 10 por página: la lista de trabajo del personal de servicio. | Personal de servicio | `ConsultarApartamentosUseCase`, `ApartamentoRepository` |
| **Consultar una temporada** (dentro de CU-29) | Una temporada con sus rangos, por su id. Es el recurso al que apunta el `Location` al crearla o al agregarle un rango. | Administrador | `TemporadasUseCase` |
| **Consultar un bloqueo o una novedad** (dentro de CU-13 y CU-17) | Un bloqueo o una novedad del apartamento, por su id. Es el recurso al que apunta el `Location` al registrarlos. Responde 404 si no existe el apartamento, no existe el registro o es de otro apartamento; un bloqueo levantado se sigue consultando. | Administrador (bloqueo); personal de servicio, recepcionista y administrador (novedad) | `ConsultarBloqueoUseCase`, `ConsultarNovedadesUseCase.consultar`, `BloqueoNoEncontradoException`, `NovedadNoEncontradaException` |

**CU-16 Cambiar el estado operativo** cambió de forma (ver sección 9), y **CU-03 Crear reserva** y **CU-11 Modificar
reserva** reciben las mascotas del grupo (L-16, ver sección 10).

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
| `GET /api/temporadas/{id}` | CU-29 | 200 `TemporadaResponse` | 404 |
| `POST /api/temporadas` | CU-29 | 201 `TemporadaResponse` + `Location` (`/api/temporadas/{id}`) | 400, 409 (falta la tarifa de un apartamento activo 7.4, solape 7.4, segunda base F-05) |
| `POST /api/temporadas/{id}/rangos` | CU-29 | 201 `TemporadaResponse` con todos sus rangos + `Location` (`/api/temporadas/{id}`) | 400, 404, 409 (solape 7.4, rango en la base F-05) |
| `GET /api/politica-cancelacion` | CU-30 | 200 `PoliticaCancelacionResponse` (versión vigente) | — |
| `GET /api/politica-cancelacion/versiones?pagina=` | CU-30 | 200 `PaginaResponse<PoliticaCancelacionResponse>`, de la más reciente a la más antigua | 400 |
| `PUT /api/politica-cancelacion` | CU-30 | 200 `PoliticaCancelacionResponse` (versión N+1) | 400, 409 (L-10) |
| `GET /api/reservas/{codigo}/folio` | CU-31 | 200 `FolioResponse` | 404, 409 (código de reserva mal formado) |
| `POST /api/reservas/{codigo}/folio/cargos` | CU-32 | 201 `FolioResponse` + `Location` (el folio) | 400 (falta `idServicio`), 404, 409 (servicio que el alojamiento no ofrece L-17, folio cerrado RN-16) |
| `POST /api/reservas/{codigo}/folio/correcciones` | CU-33 | 201 `FolioResponse` + `Location` (el folio) | 400 (faltan `indiceCargo` o `motivo`), 404, 409 (no hay cargo en esa posición o folio cerrado, RN-16) |
| `PUT /api/apartamentos/{id}/iniciar-preparacion` | CU-16 | 200 `ApartamentoResponse` | 404, 409 (transición no permitida 7.6) |
| `PUT /api/apartamentos/{id}/terminar-preparacion` | CU-16 | 200 `ApartamentoResponse` | 404, 409 (7.6) |
| `PUT /api/apartamentos/{id}/poner-fuera-de-servicio` | CU-16 | 200 `ApartamentoResponse` | 404, 409 (7.6) |
| `PUT /api/apartamentos/{id}/restablecer-servicio` | CU-16 | 200 `ApartamentoResponse` | 404, 409 (7.6) |
| `GET /api/apartamentos/pendientes-preparacion?pagina=` | CU-35 | 200 `PaginaResponse<ApartamentoResponse>` | 400 |
| `GET /api/apartamentos/{id}/novedades?pagina=` | CU-34 | 200 `PaginaResponse<NovedadResponse>`, de la más reciente a la más antigua | 400, 404 |
| `GET /api/apartamentos/{id}/bloqueos/{idBloqueo}` | CU-13 | 200 `BloqueoResponse` (también si está levantado) | 404 (no existe el apartamento o el bloqueo, o el bloqueo es de otro apartamento) |
| `GET /api/apartamentos/{id}/novedades/{idNovedad}` | CU-17 | 200 `NovedadResponse` | 404 (no existe el apartamento o la novedad, o la novedad es de otro apartamento) |

### Modificados

| Método y ruta | Cambio | Compatibilidad |
|---|---|---|
| `GET /api/apartamentos/disponibles` | Nuevos parámetros opcionales: `fechasNacimiento` (una por ocupante, para estimar solo a los facturables, RN-06), `capacidadMinima`, `precioMinimo`, `precioMaximo`, `caracteristica` y `pagina`. Cada resultado es una tarjeta (`ApartamentoDisponibleResponse`) con nombre, imagen principal, capacidad, **valor estimado de la estancia** y ubicación del alojamiento. Filtros inconsistentes responden 409 (7.5). | **Cambia la forma de la respuesta:** antes era una lista `List<ApartamentoResponse>`; ahora es una página `PaginaResponse<ApartamentoDisponibleResponse>` con `contenido`, `pagina`, `tamano`, `totalElementos` y `totalPaginas`. |
| `GET /api/canales/{canal}/disponibilidad` | Nuevo parámetro opcional `pagina` (ver sección 12). | **Cambia la forma de la respuesta:** antes era `List<ApartamentoResponse>`; ahora es `PaginaResponse<ApartamentoResponse>`. |
| `POST /api/reservas` y `PUT /api/reservas/{codigo}` | Nuevos campos opcionales `cantidadMascotas` y `pesoMascotaMasPesadaKg` (`@Min(0)`); `ReservaDetalleResponse` los devuelve (ver sección 10). 409 con L-16 si la política del alojamiento no admite las mascotas. | Compatible: sin esos campos la reserva queda sin mascotas, como antes. |
| `PUT /api/reservas/{codigo}/confirmar` | El cuerpo opcional (`ConfirmarReservaRequest`) ahora se valida con `@Valid`. | Compatible. |
| `POST /api/reservas/{codigo}/folio/pagos` | Responde 201 con `Location` al folio (`GET /api/reservas/{codigo}/folio`). | Compatible: antes era 201 sin `Location`. |
| `POST /api/apartamentos/{id}/bloqueos` y `/novedades` | Responden 201 con `Location` al bloqueo o a la novedad creada. | Compatible: antes era 201 sin `Location`. |

### Eliminado

| Método y ruta | Reemplazo |
|---|---|
| `PUT /api/apartamentos/{id}/estado-operativo` (con `CambiarEstadoOperativoRequest`) | Las cuatro operaciones de negocio de CU-16 (ver sección 9). |

Todos los listados (`/api/reservas`, `/api/reservas/llegadas`, `/api/reservas/salidas`, `/api/apartamentos`,
`/api/apartamentos/disponibles`, `/api/apartamentos/pendientes-preparacion`, `/api/apartamentos/{id}/novedades`,
`/api/apartamentos/{id}/tarifas/historico`, `/api/canales/{canal}/disponibilidad`, `/api/temporadas` y
`/api/politica-cancelacion/versiones`) devuelven 10 resultados por página (`sga.paginacion.tamano=10`) y numeran
las páginas desde 0. El contrato `docs/api/openapi.yaml` incluye todos los endpoints nuevos y modificados, y se
verificó contra lo que publica Swagger (`/v3/api-docs`, `/swagger-ui.html`) con la aplicación recién arrancada: las
50 operaciones coinciden en ruta, método, parámetros, cuerpo, códigos de respuesta y campos de la petición y de la
respuesta exitosa, y ningún endpoint de los controllers queda sin documentar.

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
| `domain/valueobject/Mascotas` | record | L-16: cantidad de mascotas y peso de la más pesada. `Mascotas.ninguna()` es la reserva sin mascotas; `validarContra(CargoAseoMascota)` rechaza las que la política del alojamiento no admite (ver sección 10). |

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
- `CreacionReservaService.registrarReservaPropia` (CU-03): las reglas del canal que antes estaban en
  `CrearReservaUseCase` pasaron al dominio. Rechaza el canal `EXTERNO` con RN-19 (entra por CU-08) y pone `PORTAL` si
  no llega canal. El caso de uso solo orquesta.
- `Reserva.getMascotas` y las variantes de `Reserva.crear`, `Reserva.reconstruir` y `Reserva.modificar` que reciben
  las mascotas (L-16). Las firmas anteriores siguen y equivalen a "sin mascotas".
- `Apartamento.cambiarEstadoOperativo` pasó a ser **privado** (CU-16, ver sección 9).
- `Folio.cargoEn(posicion)` (RN-16): el cargo que se va a corregir. `Alojamiento.obtenerServicioAdicional` (L-17): el
  servicio que se carga al folio, o 409 si el alojamiento no lo ofrece.
- Puertos: `NovedadRepository` con el historial paginado de un apartamento y `ApartamentoRepository` con los
  pendientes de preparación paginados.

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
- Otros casos de uso nuevos: `EstadoOperativoApartamentoUseCase` (reemplaza a `CambiarEstadoOperativoUseCase`,
  que se eliminó junto con `CambiarEstadoOperativoRequest`), `ConsultarNovedadesUseCase`, `ConsultarFolioUseCase`,
  `RegistrarCargoServicioUseCase` y `RegistrarCorreccionFolioUseCase`, con los DTO `RegistrarCargoServicioRequest` y
  `RegistrarCorreccionRequest`.
- Persistencia de las mascotas: `MascotasEmbedable` (`@Embeddable`) en la tabla `reserva`, columnas
  `mascotas_cantidad` y `mascotas_peso_mayor_kg`.

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
- Los endpoints de CU-22 a CU-35 y las operaciones que completan casos existentes (CU-16, temporada, bloqueo y novedad por
  id) se agregaron al contrato con sus esquemas. La comparación con Swagger da 50 operaciones iguales, en ruta,
  método, parámetros, códigos de respuesta y campos de la petición y de la respuesta exitosa (ver sección 2).
- `docs/api/sga.http`: recorrido completo y ejecutable de la API contra la aplicación recién arrancada:
  - el flujo de una reserva de punta a punta,
  - un 409 por cada regla expuesta, incluido L-16 (5.39),
  - ejemplos de 400 y 404,
  - en la sección 4, el historial de novedades, los pendientes de preparación, una reserva con mascotas, quitarle las
    mascotas y seguir el `Location` de un bloqueo y de una novedad (4.9 a 4.17),
  - la sección 9 (CU-22 a CU-25), la 10 (CU-26 y CU-27), la 11 (CU-28 a CU-30, incluida la política versionada) y la
    12 (cargos de servicios adicionales y correcciones del folio).

  Ejecutado completo y en orden contra la aplicación recién arrancada, pasan las 164 peticiones con sus 164
  `client.test`.
- `docs/trazabilidad/matriz-trazabilidad.md`: matriz regla → clase y método → caso de uso → endpoint → código →
  pruebas unitarias y de integración, con los nombres reales de las pruebas.
- Pruebas: `./gradlew test --rerun` ejecuta 250 pruebas (158 unitarias y 92 de integración) y todas pasan. Se
  agregaron pruebas unitarias de RN-04, RN-10, RN-12, RN-14, RN-21, RN-22, RP-03, L-15 y L-16 (`ReservaTest`,
  `ModificacionReservaServiceTest`, `VencimientoReservasServiceTest` y `CreacionReservaServiceTest`), de RN-01,
  RN-20, RP-01 y las transiciones de 7.6 (`DisponibilidadApartamentoServiceTest`, `ApartamentoTest`) y de RN-12,
  RN-13, RN-16, RN-18, RN-19 y RP-02 (`CancelarReservaServiceTest`, `ConciliacionCanalExternoServiceTest`,
  `FolioTest`), del estado de origen de 7.6 y de la reversión del cargo de aseo (`ApartamentoTest`,
  `ModificacionReservaServiceTest`), y de las imágenes y la ubicación de 7.3 (`ApartamentoTest`,
  `CoordenadaTest`). Con esto ninguna regla de 7.3 queda sin prueba unitaria. También se agregaron pruebas unitarias de L-03, L-06, L-09, L-12, L-13, L-14, L-17, 3.2, 3.3 y del formato del código de reserva, así que todas las reglas de la matriz tienen prueba unitaria.

## 9. CU-16: operaciones de estado operativo en lugar del endpoint genérico

En la entrega 1, CU-16 era `PUT /api/apartamentos/{id}/estado-operativo` con el estado destino en el cuerpo
(`CambiarEstadoOperativoRequest`), y el dominio exponía `Apartamento.cambiarEstadoOperativo(nuevoEstado)`. Así el
cliente decidía el estado y el dominio solo podía decir si la transición era válida.

Ahora cada transición es una operación del negocio con su nombre:

| Operación | Transición (7.6) | Quién la hace |
|---|---|---|
| `PUT /api/apartamentos/{id}/iniciar-preparacion` → `Apartamento.iniciarPreparacion` | PENDIENTE_PREPARACION → EN_PREPARACION | Personal de servicio |
| `PUT /api/apartamentos/{id}/terminar-preparacion` → `Apartamento.terminarPreparacion` | EN_PREPARACION → PREPARADO | Personal de servicio |
| `PUT /api/apartamentos/{id}/poner-fuera-de-servicio` → `Apartamento.ponerFueraDeServicio` | PREPARADO, PENDIENTE_PREPARACION o EN_PREPARACION → FUERA_DE_SERVICIO (no si está OCUPADO) | Administrador |
| `PUT /api/apartamentos/{id}/restablecer-servicio` → `Apartamento.restablecerServicio` | FUERA_DE_SERVICIO → PENDIENTE_PREPARACION | Administrador |

- OCUPADO y la salida no tienen endpoint propio: los mueven la llegada y la salida de la reserva
  (`Apartamento.recibirGrupo` y `Apartamento.liberarTrasSalida`, CU-04 y CU-05).
- `Apartamento.cambiarEstadoOperativo` pasó a ser privado. Todas las operaciones pasan por él, así que las
  transiciones no permitidas siguen respondiendo 409 con 7.6.
- **Cada operación exige su estado de origen (7.6).** No basta con que el destino sea alcanzable: el método privado
  `Apartamento.exigirEstadoDeOrigen` rechaza la operación si el apartamento no está en el estado del que parte.
  Liberar tras la salida exige OCUPADO, iniciar la preparación PENDIENTE_PREPARACION, terminarla EN_PREPARACION y
  restablecer el servicio FUERA_DE_SERVICIO. Antes, por ejemplo, `restablecerServicio` funcionaba desde OCUPADO
  (porque OCUPADO → PENDIENTE_PREPARACION es una transición válida) y `liberarTrasSalida` desde FUERA_DE_SERVICIO.
  Desde otro estado responden 409 con 7.6 y el estado no cambia.
- Las operaciones no reciben cuerpo y responden 200 con el apartamento. Un cambio de estado no crea ni levanta
  bloqueos (3.3).
- Se agregaron `GET /api/apartamentos/pendientes-preparacion`, la lista de trabajo del personal de servicio (CU-35), y
  `GET /api/apartamentos/{id}/novedades`, el historial de novedades (CU-34).
- En `sga.http` las peticiones 5.19, 5.20 y 5.22 usan las operaciones nuevas. Lo prueban
  `ApartamentoTest` (once pruebas de 7.6; cuatro de ellas sobre el estado de origen:
  `cadaMetodoFuncionaDesdeSuEstadoDeOrigen`, `cadaMetodoRechazaLosDemasEstadosDeOrigen`,
  `rechazarLiberarTrasSalidaDesdeFueraDeServicio` y `rechazarRestablecerServicioDesdeOcupado`) y
  `ApartamentoControllerIntegracionTest#cicloDeEstadoOperativo`.

## 10. Mascotas en la reserva (L-16)

En la entrega 1 la política de mascotas (`CargoAseoMascota`) existía en el dominio y en la configuración
(`sga.alojamiento.mascotas.*`), pero la reserva no sabía si el grupo traía mascotas y la API no las recibía. Ahora:

- La reserva guarda sus mascotas en el objeto de valor `Mascotas` (cantidad y peso de la más pesada, en kg). Sin
  mascotas, los dos valores son 0.
- Al crear (CU-03) y al modificar (CU-11), `CreacionReservaService` y `ModificacionReservaService` validan las
  mascotas con la política del alojamiento (`CargoAseoMascota.admite`). En el Coral se aceptan máximo 2 de hasta
  10 kg; si no, responde 409 con L-16.
- Si el grupo trae mascotas, el folio recibe el cargo único de aseo de $110.000 como `SERVICIO_ADICIONAL`
  (`CargoAseoMascota.generarCargoServicio`). En la modificación solo se asienta si antes la reserva no traía
  mascotas, porque el cargo es único por estancia.
- API: `cantidadMascotas` y `pesoMascotaMasPesadaKg` son opcionales en `CrearReservaRequest` y
  `ModificarReservaRequest` (`@Min(0)`: un negativo es 400) y salen en `ReservaDetalleResponse`.

Decisiones del equipo:

- **Una combinación incoherente responde 409 con L-16.** Un peso con cantidad 0, o una cantidad sin peso, no
  describe un grupo real. Lo rechaza el constructor de `Mascotas` (regla del dominio), no la validación del DTO.
- **En la modificación parcial, lo que no se envía se conserva.** Si llega solo la cantidad, se mantiene el peso
  guardado, y al revés (`ModificarReservaUseCase`). Si no llega ninguno de los dos, las mascotas no cambian.
- **`cantidadMascotas` 0 quita las mascotas.** Si llega la cantidad en 0 sin el peso, el peso también pasa a 0, así
  que no hace falta enviarlo para quitarlas.
- **Al quitarlas, el cargo de aseo se revierte con un movimiento inverso (RN-14, RN-16).**
  `ModificacionReservaService.modificar` busca el cargo de aseo vigente (`CargoAseoMascota.esCargoDeAseo` y
  `esReversoDeCargoDeAseo`) y lo corrige con `Folio.registrarMovimientoInverso`: el cargo original no se borra y el
  saldo baja $110.000. Si el folio no tiene ese cargo vigente, responde 409 con RN-16 y no inventa un movimiento.
  Si después el grupo vuelve a traer mascotas, el cargo se cobra otra vez. Cambiar solo la cantidad o el peso no
  cobra ni revierte nada.

- Persistencia: `MascotasEmbedable` con columnas que admiten null, así que las reservas guardadas antes se leen
  como "sin mascotas". Los datos iniciales y el canal externo crean reservas sin mascotas.
- `sga.http`: 4.12 y 4.13 (reserva con 2 mascotas), 4.14 y 4.15 (quitarlas y ver el movimiento inverso en el folio)
  y 5.39 (409 por una mascota de 15 kg). Pruebas: `CreacionReservaServiceTest` (cuatro pruebas de L-16),
  `ModificacionReservaServiceTest` (cuatro pruebas de la reversión del cargo) y
  `ReservaControllerIntegracionTest#quitarMascotasRevierteElCargoDeAseoEnElFolio`.

## 11. Depósito de garantía como `AJUSTE_MODIFICACION` (RP-02)

En la entrega 1 el depósito de garantía se asentaba en el folio como un cargo `AJUSTE_MODIFICACION`. Durante esta
entrega se le dio un tipo propio y después se **volvió a `AJUSTE_MODIFICACION`**, como lo
definen el Anexo A, el glosario y 7.7. Así quedó:

- `TipoCargo` tiene los cuatro tipos de la entrega 1: `ALOJAMIENTO`, `SERVICIO_ADICIONAL`, `AJUSTE_MODIFICACION` y
  `PENALIDAD_CANCELACION_NOSHOW`. `AJUSTE_MODIFICACION` cubre la diferencia de una modificación (RN-14) y el depósito.
- `DepositoGarantia.generarCargo` asienta el depósito ($250.000) al abrir el folio y
  `DepositoGarantia.generarDevolucion` asienta su devolución con valor negativo, los dos como `AJUSTE_MODIFICACION`.
- El depósito y el ajuste de una modificación se distinguen por su concepto. Por eso
  `ModificacionReservaServiceTest` cuenta como ajuste de RN-14 solo el cargo cuyo concepto empieza por "Ajuste por
  modificación".
- Lo prueban `FolioTest#abrirFolioAsientaElDeposito` y `FolioControllerIntegracionTest#pagarTodoYCerrar`.

## 12. Disponibilidad paginada del canal externo (12.4)

`GET /api/canales/{canal}/disponibilidad` devolvía una lista sin límite. Ahora recibe `pagina` (desde 0, 400 si es
negativa) y responde `PaginaResponse<ApartamentoResponse>` de 10 en 10, como los demás listados. Las reglas de
disponibilidad no cambian: se decide la disponibilidad primero y después se pagina. Lo prueba
`CanalExternoControllerIntegracionTest#disponibilidadPaginada`, y en `canal-externo.http` el paso 1.

## 13. No incluido en esta entrega

Lo siguiente **no se implementa en esta entrega**:

- **Usuarios y seguridad con JWT (7.2).** Registro, inicio de sesión y roles. Hoy todos los endpoints están abiertos:
  las restricciones por actor se documentan en los casos de uso pero no se aplican. Por ejemplo, CU-07 (autorizar el
  cierre del folio con saldo), CU-21 (retirar un apartamento) y poner fuera de servicio un apartamento corresponden
  solo al administrador. Entrará como adaptador de infraestructura (filtro de Spring Security y configuración de
  roles), sin tocar el dominio ni los casos de uso.
