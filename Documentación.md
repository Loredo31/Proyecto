# Documentación Técnica: Integración del Servicio Externo de Catálogo de Productos

Esta documentación describe la solución implementada para la integración del nuevo servicio externo de catálogo de productos (**API GP Cat/product**), su almacenamiento en caché en **MongoDB** mediante una tarea programada a las 06:00 AM, y la exposición del catálogo a través de un servicio REST en formato **JSON**, respetando los estándares de arquitectura y Clean Code del proyecto.

---

## 1. Arquitectura y Flujo de la Solución

La solución implementada se estructura en dos flujos principales:

### Flujo de Datos
1. **Flujo de Ingesta (Sincronización Diaria):**
   - Una tarea programada (**Cron Task**) se dispara automáticamente todos los días a las **06:00 AM**.
   - Invoca al cliente de integración (**Client**) que consume el endpoint externo `GET /sistema/service/getProductList.do` con autenticación mediante **Bearer Token**.
   - El servicio externo responde con el catálogo en formato **XML** (*Cat Product XML* en la pizarra) o JSON.
   - Tras validar el código **HTTP 200**, el contenido se parsea y se guarda/actualiza en **MongoDB** en la colección `product_catalog_cache`.

2. **Flujo de Consulta (Servicio REST):**
   - Los consumidores internos invocan el endpoint REST `GET /api/catalog/products` del **Servicio**.
   - El servicio consulta directamente la caché en **MongoDB**.
   - **Si está disponible:** Responde con código **HTTP 200** y el catálogo en formato **JSON** (*Cat Product JSON*).
   - **Si no está disponible o falla:** Se captura la excepción y se devuelve una respuesta estandarizada de error (`!= 200`) a través del manejador global.

---

## 2. Componentes y Capas Implementadas

La solución sigue la arquitectura en capas existente en el proyecto Java Spring Boot:

### 2.1. Configuración de Propiedades (`application.properties`)
Se definieron las propiedades requeridas siguiendo las convenciones existentes:
- `catalog.external.base-url`: URL base del servicio externo (por defecto `https://gestopago.portalventas.net`, sobreescribible por `CATALOG_EXTERNAL_BASE_URL`).
- `catalog.external.token`: Bearer Token de autenticación (sobreescribible por variable de entorno `CATALOG_EXTERNAL_TOKEN`). **Nunca se hardcodea en código**.
- `catalog.external.cron`: Expresión cron para la ejecución programada (`0 0 6 * * *` = diario a las 06:00 AM).
- `catalog.external.retry.max-attempts`: Número máximo de reintentos ante fallas temporales (por defecto 3).
- `catalog.external.retry.delay-ms`: Retraso entre reintentos en milisegundos (por defecto 2000 ms).
- Timeouts y nivel de logs para Feign.
- Conexión a MongoDB exclusiva para la caché del catálogo (`spring.data.mongodb.uri`, sobreescribible por variable de entorno `MONGODB_URI` con valor por defecto local).

### 2.2. Interceptor de Autenticación (`FeignAuthInterceptor.java`)
- Implementa `RequestInterceptor` de OpenFeign.
- Inyecta el encabezado `Authorization: Bearer <token>` dinámicamente desde la configuración.
- Valida que el token esté configurado y evita interceptar llamadas a otros clientes como `GestoPagoAuthClient` (login de dispositivo).
- No imprime el token en los logs para prevenir fugas de información sensible.

### 2.3. Capa de Cliente / Integración
- **`GestoPagoCatProduct.java`**: Interfaz de contrato que modela la operación `"API GP Cat/product"` identificada en el diagrama.
- **`ProductCatalogClient.java`**: Interfaz `@FeignClient` que extiende de `GestoPagoCatProduct`, configurada con URL base y path `/sistema/service/getProductList.do`. Devuelve `feign.Response` en crudo para permitir validación explícita del código HTTP.

### 2.4. Modelos y DTOs
- **`ProductDto.java`**: DTO que representa un producto del catálogo (`id`, `sku`, `name`, `description`, `price`, `stock`, `category`, `active`). Incluye anotaciones tanto de Jackson como de JAXB (`@XmlRootElement`, `@XmlElement`) para permitir interoperabilidad XML/JSON.
- **`ProductCatalogResponseDto.java`**: Contenedor para respuestas estructuradas del servicio externo.
- **`ProductCatalogCache.java`**: Entidad `@Document` de MongoDB para persistir el catálogo en la colección `product_catalog_cache` con la clave fija `PRODUCT_LIST`.
- **`ApiResponse.java`** y **`ApiResponseEnum.java`**: Envoltorios de respuesta estandarizados (`code`, `message`, `data`, `timestamp`).

### 2.5. Capa de Parseo Seguro (`ProductCatalogXmlParser.java`)
- Componente especializado en procesar respuestas XML (*Cat Product XML*).
- Implementa protección contra ataques **XXE** (deshabilita `DOCTYPE`, entidades generales externas y entidades de parámetros).
- Soporta etiquetas estándar en inglés (`<product>`, `<id>`, `<name>`) y en español (`<producto>`, `<idProducto>`, `<nombre>`, etc.) para máxima tolerancia.

### 2.6. Capa de Servicio (`ProductCatalogService` y `ProductCatalogServiceImpl`)
- **Tarea Programada (`@Scheduled`)**: Ejecuta `syncCatalog()` con el cron configurado a las 06:00 AM.
- **Política de Reintentos (`RetryTemplate`)**: Reintenta ante fallas de comunicación, timeouts o respuestas distintas de 200 hasta el máximo configurado.
- **Detección Automática de Formato**: Si la respuesta comienza con `<`, se procesa como XML; en caso contrario, se procesa como JSON.
- **Persistencia en MongoDB**: Guarda la lista de productos y la fecha de actualización.
- **Consulta Local**: `getCatalogFromCache()` recupera los productos de MongoDB. Si la colección está vacía o no existe el documento, lanza `CatalogException(ApiResponseEnum.CATALOGO_NO_DISPONIBLE)`.
- **Registro y Monitoreo**: Registra con `log.info` el inicio, fin y tiempo transcurrido (en ms) de cada invocación al servicio externo.

### 2.7. Capa de Controlador (`ProductCatalogController.java`)
- `GET /api/catalog/products`: Expone la consulta del catálogo desde MongoDB. Retorna **HTTP 200** con payload JSON (`ApiResponse<List<ProductDto>>`).
- `POST /api/catalog/sync`: Endpoint auxiliar para ejecutar la sincronización bajo demanda sin tener que esperar a las 06:00 AM (útil para pruebas y monitoreo operativo).

### 2.8. Manejo Global de Errores (`GlobalExceptionHandler.java`)
Captura y transforma excepciones en respuestas HTTP estandarizadas:
- `CatalogException`: Retorna el estado asociado (ej. 404 NOT FOUND con código `40401` si no está en caché).
- `ExternalServiceException`: Mapea errores 401/403 a `ERROR_AUTENTICACION` (401) y fallos a `SERVICIO_EXTERNO_NO_DISPONIBLE` (503).
- `RetryableException` / `ResourceAccessException`: Mapea a `TIMEOUT_SERVICIO` (504).
- `FeignException`: Mapea según el código recibido del cliente externo.

---

## 3. Pruebas Unitarias

Se implementaron pruebas unitarias exhaustivas con **JUnit 5** y **Mockito** en `src/test/java`:

1. **`ProductCatalogServiceTest.java`**:
   - `testSyncCatalog_Successful_JsonResponse`: Simula respuesta HTTP 200 en JSON y valida la persistencia en MongoDB.
   - `testSyncCatalog_Successful_XmlResponse`: Simula respuesta HTTP 200 en XML (*Cat Product XML*) y valida el parseo y almacenamiento.
   - `testSyncCatalog_ExternalServiceReturns500_RetriesAndFails`: Simula error 500 del servicio externo, valida los 3 reintentos y confirma que la caché de MongoDB NO se modifica.
   - `testSyncCatalog_Unauthorized401`: Simula respuesta 401 Unauthorized y valida el flujo de error.
   - `testSyncCatalog_Timeout`: Simula `RetryableException` (timeout de red) y valida reintentos controlados.
   - `testGetCatalogFromCache_Success`: Valida retorno exitoso de productos desde MongoDB.
   - `testGetCatalogFromCache_NotFound_ThrowsCatalogException`: Valida excepción cuando no existe la caché en MongoDB.
   - `testGetCatalogFromCache_EmptyProducts_ThrowsCatalogException`: Valida excepción cuando la lista de productos está vacía.

2. **`ProductCatalogControllerTest.java`**:
   - `testGetProducts_Success`: Valida que el controlador retorne HTTP 200 OK con estructura `ApiResponse` y los productos en formato JSON.
   - `testGetProducts_EmptyCache_ThrowsException`: Valida la propagación de `CatalogException`.
   - `testSyncCatalog_Success`: Valida la invocación manual de sincronización.

3. **`ProductCatalogXmlParserTest.java`**:
   - Valida parseo de XML en inglés y español, manejo de valores nulos y detección de XML mal formado.

---

## 4. Decisiones Técnicas y Principios Aplicados

1. **Separación de Bases de Datos (PostgreSQL vs. MongoDB):**
   - PostgreSQL se mantiene como la base de datos relacional para entidades de negocio transaccionales (migraciones Flyway, JPA).
   - MongoDB se utiliza exclusivamente como almacén no relacional para la caché del catálogo externo, permitiendo lecturas ultra-rápidas y desacopladas de la disponibilidad del servicio externo.

2. **Soporte Híbrido XML / JSON en Ingesta:**
   - La pizarra del profesor indicaba específicamente *"Cat Product XML"*, mientras que servicios REST modernos emplean JSON. El parser detecta el formato en tiempo de ejecución, garantizando compatibilidad ante cualquier fuente.

3. **Inyección de Dependencias por Constructor:**
   - Se utilizó `@RequiredArgsConstructor` de Lombok para garantizar inmutabilidad (`final`), facilidad de testing unitario e inversión de control limpia.

4. **Seguridad en Logs:**
   - Ningún Bearer Token, contraseña ni dato sensible de autenticación es impreso en los logs de la aplicación.

---

## 5. Instrucciones de Compilación y Ejecución

### Ejecutar Pruebas Unitarias
Para correr la suite de pruebas unitarias implementada:
```bash
./gradlew test
```

### Ejecutar la Aplicación Localmente
```bash
./gradlew bootRun
```

### Probar Endpoints
- **Consultar catálogo desde caché (JSON):**
  ```bash
  curl -X GET http://localhost:8080/api/catalog/products
  ```
- **Disparar sincronización manual:**
  ```bash
  curl -X POST http://localhost:8080/api/catalog/sync
  ```
