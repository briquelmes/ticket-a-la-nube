# Ticket a la Nube

Microservicio Spring Boot para generar tickets PDF de eventos, guardarlos temporalmente en EFS y almacenarlos en Amazon S3.

## Datos de un ticket

Cada ticket contiene un identificador, un evento, un usuario, un estado (`RESERVADO`, `VENDIDO` o `CANCELADO`) y la fecha de creacion. El PDF se organiza en S3 por evento y usuario:

```text
events/{eventId}/users/{userId}/{ticketId}.pdf
```

Los metadatos se guardan como objetos JSON bajo `_ticket-metadata/` para consultar el historial y calcular estadisticas aun cuando el contenedor se reinicie.

## Ejecutar y compilar

Requiere Java 21 y Maven. Para compilar y ejecutar las pruebas:

```powershell
.\mvnw.cmd clean test
```

La aplicacion requiere credenciales AWS disponibles mediante el proveedor estandar de AWS SDK y estas variables:

| Variable | Descripcion | Valor local sugerido |
|---|---|---|
| `S3_BUCKET` | Bucket de S3 para guardar tickets y metadatos | Nombre del bucket del laboratorio |
| `AWS_REGION` | Region del bucket y de EFS | `us-east-1` |
| `EFS_PATH` | Ruta montada de EFS | `D:\ticket-a-la-nube-efs` en Windows o `/app/efs` en el contenedor |

En EC2, `EFS_PATH` debe apuntar al directorio donde se monte EFS. Crear `/app/efs` dentro del contenedor por si solo no conecta EFS.

## API

Base URL: `/api/tickets`

| Metodo | Ruta | Funcion |
|---|---|---|
| `POST` | `/api/tickets` | Genera un ticket PDF y lo guarda en EFS y S3 |
| `GET` | `/api/tickets?eventId=evento1&userId=usuario1&status=RESERVADO` | Consulta historial; los filtros son opcionales |
| `GET` | `/api/tickets/{ticketId}/download` | Descarga el PDF desde S3 |
| `PUT` | `/api/tickets/{ticketId}` | Actualiza evento, usuario o estado y regenera el PDF |
| `DELETE` | `/api/tickets/{ticketId}` | Elimina el ticket de S3 y EFS |
| `GET` | `/api/tickets/events/{eventId}/statistics` | Cuenta tickets reservados, vendidos y cancelados |

`eventId` y `userId` aceptan de 1 a 60 letras, numeros, guiones o guiones bajos. Si no se indica estado al crear, el ticket comienza como `RESERVADO`.

### Ejemplos con cURL

Crear un ticket:

```bash
curl -X POST http://localhost:8080/api/tickets \
  -H "Content-Type: application/json" \
  -d '{"eventId":"evento123","userId":"usuarioX"}'
```

Consultar el historial del evento:

```bash
curl "http://localhost:8080/api/tickets?eventId=evento123"
```

Marcarlo como vendido (reemplaza `{ticketId}` por el identificador de la respuesta al crear):

```bash
curl -X PUT http://localhost:8080/api/tickets/{ticketId} \
  -H "Content-Type: application/json" \
  -d '{"eventId":"evento123","userId":"usuarioX","status":"VENDIDO"}'
```

## Despliegue automatico

El workflow [deploy.yml](./.github/workflows/deploy.yml) se ejecuta al hacer `push` a `main` o manualmente desde GitHub Actions. Primero ejecuta las pruebas, construye y publica la imagen en Docker Hub y luego despliega esa imagen en EC2. El despliegue monta la ruta EFS del host en `/app/efs` dentro del contenedor.

Configura estos GitHub Actions repository secrets antes de ejecutar el workflow:

| Secret | Valor |
|---|---|
| `DOCKERHUB_USERNAME` | Usuario de Docker Hub; el repositorio de imagen debe llamarse `ticket-a-la-nube` |
| `DOCKERHUB_TOKEN` | Token de Docker Hub con permiso de escritura |
| `EC2_HOST` | IP publica o DNS de EC2 |
| `EC2_SSH_KEY` | Contenido completo de la clave privada PEM de EC2 |
| `EC2_KNOWN_HOSTS` | Clave de host SSH de EC2, en formato `known_hosts`, verificada antes de guardarla |
| `USER_SERVER` | Usuario Linux de EC2, normalmente `ec2-user` |
| `AWS_ACCESS_KEY_ID` | Access key vigente de AWS Academy |
| `AWS_SECRET_ACCESS_KEY` | Secret key vigente de AWS Academy |
| `AWS_SESSION_TOKEN` | Session token vigente de AWS Academy |
| `AWS_REGION` | Region de EC2, EFS y S3 |
| `S3_BUCKET` | Nombre exacto del bucket |
| `EFS_ID` | ID del sistema EFS, por ejemplo `fs-...` |

Las credenciales de AWS Academy son temporales; actualiza los tres secrets de AWS cuando se renueve la sesion del laboratorio. Nunca guardes esas credenciales ni la clave PEM en el repositorio. El workflow configura el montaje NFS si todavia no esta montado, y ejecuta el contenedor con el volumen `/home/{USER_SERVER}/efs:/app/efs`.

El grupo de seguridad de EC2 debe permitir SSH desde el runner de GitHub y TCP 8080 desde el origen autorizado para las pruebas. El grupo de seguridad del mount target EFS debe permitir NFS/TCP 2049 desde el grupo de seguridad de EC2.

El repositorio de imagen de Docker Hub debe ser publico para que EC2 pueda descargarlo sin guardar credenciales de Docker Hub en la instancia. Los runners hospedados por GitHub no tienen una IP fija en todos los planes; una regla SSH limitada a la IP de tu computador no permitirá el despliegue. No dejes SSH abierto a todo Internet: usa una alternativa aprobada por el curso, como un runner autohospedado, o configura acceso temporal y restringido para el runner.
