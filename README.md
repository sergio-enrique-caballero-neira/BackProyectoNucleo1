# Back — API Gestor de Horarios

API REST (Spring Boot 3.3.5, Java 21, MySQL, JWT) para la gestión de programas, asignaturas,
horarios y bloques, con generación automática de horarios, según el *Informe de Avance Proyecto
Núcleo 1* (RF-01 a RF-12, RNF-01 a RNF-04, R-01 a R-03).

## Ejecutar

1. Copiar `ProBack/src/main/resources/application.properties.example` como `application.properties`
   (está en `.gitignore`) y completar base de datos, `JWT_SECRET` y `ADMIN_PASSWORD`.
2. Desde `ProBack/`:

```bash
./mvnw spring-boot:run
```

Swagger queda en `http://localhost:8080/` (solo en desarrollo). CORS admite los orígenes de
`app.cors.allowed-origins`. Al arrancar con la tabla `usuario` vacía se crea el
administrador `app.admin.username` / `app.admin.password`.

Pruebas (H2 en memoria, no tocan MySQL): `./mvnw test`

## Despliegue (Azure App Service)

- **URL:** https://tomca.serbux.qzz.io (API en `/api`; Swagger desactivado en producción).
- **Pila:** Azure App Service Linux, Java 21 y Tomcat 10.1; el WAR se publica como `app.war` en la raíz.
- **CI/CD:** `.github/workflows/main_apachetomcat.yml` compila `ProBack/` con Maven (pruebas sobre H2)
  y despliega en cada push a `main`. Usa OIDC con los secretos `AZUREAPPSERVICE_*` del repositorio.
- **Configuración:** perfil `prod` (`application-prod.properties`, sin secretos). En Azure, en
  Variables de entorno, se definen `SPRING_PROFILES_ACTIVE=prod`, `DB_URL`, `DB_USERNAME`,
  `DB_PASSWORD` y `JWT_SECRET`; opcional `ADMIN_PASSWORD`.
- **Base de datos:** MySQL 8.4 externo; su firewall debe aceptar las IP de salida de la Web App.
- **Cliente:** https://horarios.sergio.games y https://horarios.serbux.qzz.io son los únicos orígenes aceptados: en producción
  la API responde 403 a cualquier otra petición (`OrigenPermitidoFilter`).

## Contrato

Todos los endpoints salvo el login exigen `Authorization: Bearer <token>`.

| Método | Ruta | Respuesta |
|---|---|---|
| POST | `/api/auth/login` | 200 `{token, tipo, expiraEn, username, rol}` · 401 |
| GET | `/api/programas` | 200 programas con su número de asignaturas y horarios |
| GET | `/api/programas/{id}` | 200 · 404 |
| POST | `/api/programas` | 201 · 400 · 409 código o nombre duplicado |
| PUT | `/api/programas/{id}` | 200 · 400 · 404 · 409 |
| DELETE | `/api/programas/{id}` | 204 · 404 · 409 si tiene horarios |
| GET | `/api/asignaturas` | 200 lista ordenada por código |
| GET | `/api/asignaturas/{id}` | 200 · 404 |
| POST | `/api/asignaturas` | 201 · 400 · 409 código duplicado |
| PUT | `/api/asignaturas/{id}` | 200 · 400 · 404 · 409 |
| DELETE | `/api/asignaturas/{id}` | 204 · 404 · 409 si tiene bloques |
| GET | `/api/programaciones` | 200 horarios (programaciones) con sus bloques |
| GET | `/api/programaciones/{id}` | 200 · 404 |
| POST | `/api/programaciones` | 201 · 400 · 409 código duplicado |
| PUT | `/api/programaciones/{id}` | 200 · 400 · 404 · 409 |
| DELETE | `/api/programaciones/{id}` | 204 (borra también sus bloques) · 404 |
| POST | `/api/programaciones/{id}/propuesta` | 200 propuesta sin guardar · 400 · 404 |
| POST | `/api/programaciones/{id}/propuesta/aplicar` | 200 horario con sus bloques · 400 · 404 · 409 (no guarda nada si un bloque falla) |
| GET | `/api/horarios?programacionId=&asignaturaId=` | 200 bloques ordenados por día y hora |
| GET | `/api/horarios/{id}` | 200 · 404 |
| POST | `/api/horarios` | 201 · 400 regla horaria · 404 programación/asignatura · 409 |
| PUT | `/api/horarios/{id}` | 200 · 400 · 404 · 409 |
| DELETE | `/api/horarios/{id}` | 204 · 404 |

Programa: `{ "codigo": "SIS", "nombre": "Ingeniería de Sistemas" }` (la respuesta añade `id`,
`asignaturas` y `horarios`).

Asignatura: `{ "codigo": "SIS101", "nombre": "Programación I", "creditos": 4, "sesionesSemana": 3, "programaIds": [1, 2] }`

Programación ("Horario" en la interfaz): `{ "codigo": "SIS-2026-2-S3", "programaId": 1, "descripcion": "Semestre 3" }`
(`descripcion` es opcional; la respuesta añade `id`, `programaCodigo`, `programaNombre` y `bloques`).

Horario (bloque): `{ "programacionId": 1, "asignaturaId": 1, "diaSemana": "Lunes", "horaInicio": "07:00", "horaFin": "09:00" }`
(la respuesta añade `id`, `asignaturaCodigo` y `asignaturaNombre`).

Propuesta: se pide con `{ "asignaturaIds": [1, 2], "reorganizar": false, "semilla": 42 }` (`semilla`
opcional; repetirla repite la propuesta) y responde `{ semilla, reorganizar, bloques, sinUbicar }`. Para
guardarla se envía `{ "reorganizar": false, "bloques": [ { "asignaturaId", "diaSemana", "horaInicio", "horaFin" } ] }`.

Errores: `{ fecha, estado, error, mensaje, ruta, campos? }`, donde `campos` relaciona cada campo
con su mensaje para mostrarlo junto al campo en el formulario.

## Reglas validadas en el servidor

- Sesión de exactamente 2 h; lunes a viernes 07:00–21:00 (última sesión a las 19:00), sábado 07:00–13:00; sin cruzar 12:00–13:00.
- Asignatura: código único (se guarda en mayúsculas), créditos 1–6, sesiones por semana 1–4.
- Dentro de una programación, una asignatura no puede tener más bloques que sus sesiones
  semanales y ningún bloque puede cruzarse con otro, sea de la asignatura que sea. Programaciones
  distintas son independientes.
- No se elimina una asignatura con bloques asociados ni un programa con horarios.
- Generación automática: nunca incumple las reglas anteriores; prefiere sesiones de una asignatura
  en días distintos y a la misma hora, reparte la carga entre los días y solo usa el sábado o la
  franja de 19:00 a 21:00 cuando no queda otro espacio. Con `reorganizar: false` conserva los
  bloques actuales y completa las sesiones que faltan.
