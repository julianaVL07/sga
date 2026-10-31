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

Además, **CU-01 Buscar apartamentos disponibles** se amplió con los filtros y la paginación de 7.5 (ver sección 2).

## 2. Endpoints

### Nuevos

| Método y ruta | Caso de uso | Respuesta | Errores |
|---|---|---|---|
| `GET /api/reservas?estado=&apartamento=&canal=&titular=&desde=&hasta=&pagina=` | CU-19 | 200 `PaginaResponse<ReservaResumenResponse>` | 400 (página negativa, enum o fecha mal escritos), 409 (`desde` > `hasta`) |
| `GET /api/apartamentos?pagina=` | CU-20 | 200 `PaginaResponse<ApartamentoResponse>` | 400 (página negativa) |
| `GET /api/apartamentos/{id}` | CU-20 | 200 `ApartamentoDetalleResponse` | 404 (no existe o fue retirado) |
| `PUT /api/apartamentos/{id}/retirar` | CU-21 | 200 `ApartamentoDetalleResponse` con `eliminado = true` | 404, 409 (reservas activas o futuras, o ya retirado) |

### Modificado

| Método y ruta | Cambio | Compatibilidad |
|---|---|---|
| `GET /api/apartamentos/disponibles` | Nuevos parámetros opcionales: `fechasNacimiento` (una por ocupante, para estimar solo a los facturables, RN-06), `capacidadMinima`, `precioMinimo`, `precioMaximo`, `caracteristica` y `pagina`. Cada resultado es una tarjeta (`ApartamentoDisponibleResponse`) con nombre, imagen principal, capacidad, **valor estimado de la estancia** y ubicación del alojamiento. Filtros inconsistentes responden 409 (7.5). | **Cambia la forma de la respuesta:** antes era una lista `List<ApartamentoResponse>`; ahora es una página `PaginaResponse<ApartamentoDisponibleResponse>` con `contenido`, `pagina`, `tamano`, `totalElementos` y `totalPaginas`. |

Todos los listados (`/api/reservas`, `/api/apartamentos`, `/api/apartamentos/disponibles`) devuelven 10 resultados por
página (`sga.paginacion.tamano=10`) y numeran las páginas desde 0. El contrato `docs/api/openapi.yaml` se actualizó
con los cuatro endpoints nuevos y el modificado, y se verificó que coincide con lo que publica Swagger
(`/swagger-ui.html`): rutas, métodos, parámetros, códigos de respuesta y esquemas.

## 3. Clases nuevas

### Dominio (Java puro, sin Spring, JPA ni Lombok)

| Clase | Tipo | Responsabilidad |
|---|---|---|
| `domain/repository/Pagina<T>` | record | Página de resultados que devuelven los puertos en lugar de `org.springframework.data.domain.Page`. Valida número ≥ 0, tamaño > 0 y contenido no nulo; copia el contenido de forma inmutable; calcula `totalPaginas()`. `Pagina.de(lista, numero, tamano)` pagina en memoria el resultado de la búsqueda de disponibilidad, que se decide en el dominio. |
| `domain/repository/FiltroReservas` | record | Filtros opcionales de CU-19. Rechaza `desde` posterior a `hasta` (7.5). |
| `domain/valueobject/CriterioBusqueda` | record | Filtros opcionales de CU-01: capacidad mínima, rango de precio sobre el valor estimado de la estancia y característica de la dotación. Rechaza capacidad ≤ 0, precios negativos y precio mínimo mayor que el máximo (7.5). |
| `domain/valueobject/ApartamentoDisponible` | record | Resultado de la búsqueda: el apartamento, su valor estimado y la ubicación del alojamiento. |
| `domain/service/RetiroApartamentoService` | servicio de dominio | CU-21: consulta si hay reservas activas o futuras y delega en `Apartamento.retirarDeLaVenta`. |

### Métodos nuevos en clases existentes del dominio

- `Apartamento.retirarDeLaVenta` y `Apartamento.validarSinReservasPendientes` (7.3). Un apartamento eliminado no se
  puede volver a activar.
- `Dotacion.mencionaCaracteristica`: busca la característica dentro de la dotación sin distinguir mayúsculas ni tildes.
- `DisponibilidadApartamentoService.buscarDisponibles(estancia, ocupantes, fechasNacimiento, criterio)`: primero decide
  la disponibilidad (RN-01, RN-02, RN-07, RN-20, RP-01) y después aplica los filtros.
- `CotizadorEstanciaService.estimar`: valor estimado de la estancia; sin fechas de nacimiento, cuenta a todos los
  ocupantes como facturables.
- Puertos: `ReservaRepository.listar(filtro, pagina, tamano)` y `ApartamentoRepository.listarNoEliminados(pagina, tamano)`.

### Aplicación e infraestructura

- Casos de uso: `ListarReservasUseCase`, `ConsultarApartamentosUseCase`, `RetirarApartamentoUseCase`. Leen el tamaño
  de página de `sga.paginacion.tamano`.
- DTO: `PaginaResponse<T>` (envoltorio genérico con `de(Pagina, conversor)`), `ReservaResumenResponse`,
  `ApartamentoDisponibleResponse`, `ApartamentoDetalleResponse`.
- Persistencia: consulta JPQL paginada y filtrada de reservas (`ReservaJpaDataRepository`) y listado paginado de
  apartamentos no eliminados; los adaptadores JPA traducen `Page` de Spring Data a `Pagina` del dominio.
- `DatosInicialesReservasConfig`: al arrancar crea reservas en los 6 estados y por los 3 canales (12.3), con fechas
  relativas a hoy. `DatosInicialesCoralConfig` agrega un apartamento fuera de servicio con bloqueo (APT-202) y un
  bloqueo vigente (APT-301).
- `PoliticaCancelacionProperties` y `OpenApiConfig` (ver secciones 4 y 6).

## 4. Política de cancelación y demás parámetros movidos a configuración

En la entrega 1, `DomainServiceConfig.politicaVigente()` construía la política de cancelación con valores escritos
en el código. La rúbrica pide los parámetros de la Ficha en `application.properties`, sin valores del negocio
quemados en el código. Ahora:

- La política se declara en `application.properties` (`sga.politica-cancelacion.version`, `.vigente-desde`,
  `.consecuencia-no-show` y `.tramos[i].dias-antelacion` / `.porcentaje-retencion`). La lee el record
  `infrastructure/config/PoliticaCancelacionProperties` (`@ConfigurationProperties`), y el `@Bean politicaVigente`
  construye con esos valores el `PoliticaCancelacion` del dominio. **El dominio no cambió**: si los tramos violan
  L-10, el constructor de `PoliticaCancelacion` lanza `ReglaDominioException` y la aplicación no arranca.
- Para publicar una política nueva basta subir `version` y `vigente-desde` y ajustar los tramos. Las reservas ya
  creadas conservan la versión que congelaron (RN-22).

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

## 5. Decisión: no hay filtro de servicios en la búsqueda

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

## 6. Contrato y documentación de la API

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
- `docs/api/sga.http`: recorrido completo y ejecutable de la API contra la aplicación recién arrancada:
  - el flujo de una reserva de punta a punta,
  - un 409 por cada regla expuesta,
  - ejemplos de 400 y 404.
- `docs/trazabilidad/matriz-trazabilidad.md`: matriz regla → clase y método → caso de uso → endpoint → código →
  prueba.

## 7. Pendiente: seguridad con JWT (7.2)

La autenticación y autorización con JWT **no se implementa en esta entrega**. Todos los endpoints están abiertos.
Las restricciones por actor se documentan en los casos de uso, pero hoy no se aplican. Por ejemplo:

- CU-07 (autorizar el cierre del folio con saldo) y CU-21 (retirar un apartamento) corresponden solo al administrador.
- CU-16 (cambiar el estado operativo) corresponde al personal de servicio o al administrador.

Cuando se implemente, entrará como adaptador de infraestructura (filtro de Spring Security y configuración de
roles). No toca el dominio ni los casos de uso.
