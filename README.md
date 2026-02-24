# Car Catalog API - Proyecto DevOps Portfolio

## Descripción General

Car Catalog API es una REST API desarrollada con Spring Boot para la gestión de un catálogo de autos de lujo. El objetivo principal del proyecto no es solo la API, sino demostrar competencias reales en tecnologías DevOps: Docker, AWS (ECR, ECS Fargate, RDS) y Kubernetes.

La aplicación fue containerizada con Docker, desplegada en AWS usando servicios de producción, y orquestada localmente con Kubernetes usando Minikube.

---

## Stack Tecnológico

- **Backend:** Spring Boot 3.2.5, Java 17, Spring Data JPA, PostgreSQL
- **Documentación API:** Swagger / OpenAPI (springdoc-openapi)
- **Containerización:** Docker (Dockerfile multi-stage), Docker Compose
- **Cloud (AWS):**
  - ECR (Elastic Container Registry)
  - ECS Fargate (Elastic Container Service)
  - RDS PostgreSQL (Relational Database Service)
  - VPC, Security Groups
- **Orquestación:** Kubernetes (Minikube local)
- **CLI Tools:** AWS CLI, kubectl, Minikube

---

## Arquitectura del Proyecto

```
Spring Boot API
       │
       ▼
   Docker
       ├── Dockerfile multi-stage (imagen optimizada)
       └── Docker Compose (app + PostgreSQL local)
               │
               ▼
   AWS (deploy real en la nube)
       ├── ECR (imagen Docker almacenada)
       ├── ECS Fargate (contenedor corriendo)
       ├── RDS PostgreSQL (base de datos en la nube)
       └── VPC + Security Groups (networking)
               │
               ▼
   Kubernetes (Minikube local)
       ├── Deployment (2 réplicas)
       ├── Service (NodePort)
       ├── ConfigMap (configuración)
       └── Secret (credenciales)
```

---

## 1. API REST - Spring Boot

### Entidad: Car

| Campo | Tipo | Descripción |
|-------|------|-------------|
| id | UUID | Identificador único, generado automáticamente |
| brand | String | Marca del auto (max 50 caracteres) |
| model | String | Modelo del auto (max 50 caracteres) |
| year | Integer | Año del auto (1900-2026) |
| price | BigDecimal | Precio del auto |
| color | String | Color del auto (max 30 caracteres) |
| description | String | Descripción del auto (max 500 caracteres) |
| imageUrl | String | URL de imagen (opcional) |
| createdAt | LocalDateTime | Fecha de creación (automática) |
| updatedAt | LocalDateTime | Fecha de actualización (automática) |

### Endpoints

| Método | Endpoint | Descripción |
|--------|----------|-------------|
| GET | /api/v1/cars | Listar todos los autos (con paginación) |
| GET | /api/v1/cars/{id} | Obtener un auto por ID |
| POST | /api/v1/cars | Crear un nuevo auto |
| PUT | /api/v1/cars/{id} | Actualizar un auto existente |
| DELETE | /api/v1/cars/{id} | Eliminar un auto |
| GET | /actuator/health | Health check |
| GET | /swagger-ui.html | Documentación Swagger |

### Arquitectura del código

```
com.cobak.carcatalog
├── controller/        → CarController (endpoints REST)
├── service/           → Interfaz del servicio
│   └── impl/          → Implementación del servicio
├── repository/        → CarRepository (Spring Data JPA)
├── model/
│   ├── entity/        → Car (entidad JPA)
│   └── dto/           → CarRequestDTO, CarResponseDTO
├── exception/         → GlobalExceptionHandler
└── config/            → Configuraciones
```

### Configuración (application.yml)

La aplicación usa variables de entorno para la configuración, permitiendo diferentes valores según el entorno (local, Docker, AWS):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:car_catalog}?sslmode=require
    username: ${DB_USERNAME:postgres}
    password: ${DB_PASSWORD:postgres}
  jpa:
    hibernate:
      ddl-auto: update
```

---

## 2. Docker

### Dockerfile (Multi-Stage Build)

Se utilizó un Dockerfile multi-stage para optimizar el tamaño de la imagen final. El primer stage compila el proyecto con Maven, y el segundo stage solo copia el JAR resultante a una imagen liviana de Java.

```dockerfile
# Stage 1: Build
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/car-catalog-api-0.0.1-SNAPSHOT.jar /app/car-catalog-api.jar
ENV DB_HOST=localhost
ENV DB_PORT=5432
ENV DB_NAME=car_catalog
ENV DB_USERNAME=postgres
ENV DB_PASSWORD=changeme
ENV SERVER_PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "car-catalog-api.jar"]
```

**¿Por qué multi-stage?** La imagen de build (Maven + JDK) pesa ~800MB. La imagen final (solo JRE Alpine) pesa ~200MB. En producción solo necesitás el runtime, no el compilador.

### Docker Compose (Desarrollo Local)

Docker Compose levanta la aplicación y PostgreSQL juntos con un solo comando:

```yaml
services:
  db:
    image: postgres:16-alpine
    container_name: car-catalog-db
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: 12345
      POSTGRES_DB: car_catalog
    ports:
      - "5432:5432"
    volumes:
      - db_data:/var/lib/postgresql/data
    networks:
      - car-catalog-network

  app:
    image: car-catalog-api
    container_name: car-catalog-app
    depends_on:
      - db
    ports:
      - "8080:8080"
    environment:
      - DB_HOST=db
      - DB_PORT=5432
      - DB_NAME=car_catalog
      - DB_USERNAME=postgres
      - DB_PASSWORD=12345
      - SERVER_PORT=8080
    networks:
      - car-catalog-network

volumes:
  db_data:

networks:
  car-catalog-network:
```

### Comandos Docker utilizados

```bash
# Construir la imagen
docker build -t car-catalog-api .

# Levantar con Docker Compose
docker compose up

# Detener
docker compose down
```

---

## 3. AWS - Deploy en la Nube

### 3.1 RDS PostgreSQL

Se creó una instancia de PostgreSQL manejada por AWS con las siguientes características:

- **Identificador:** car-catalog-db
- **Motor:** PostgreSQL 17.6
- **Clase de instancia:** db.t4g.micro (Free Tier)
- **Almacenamiento:** 20 GiB gp2
- **Región:** us-east-1
- **Acceso público:** Habilitado (para desarrollo)
- **Endpoint:** car-catalog-db.cu5wy86i6ugs.us-east-1.rds.amazonaws.com

Se creó la base de datos `car_catalog` manualmente conectándose vía psql desde WSL:

```bash
psql "host=car-catalog-db.cu5wy86i6ugs.us-east-1.rds.amazonaws.com port=5432 dbname=postgres user=postgres password=CarCatalog2026 sslmode=require"
CREATE DATABASE car_catalog;
```

### 3.2 ECR (Elastic Container Registry)

Se creó un repositorio privado para almacenar la imagen Docker:

- **Repositorio:** car-catalog-api
- **URI:** 620525694411.dkr.ecr.us-east-1.amazonaws.com/car-catalog-api

Comandos para subir la imagen:

```bash
# Login a ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 620525694411.dkr.ecr.us-east-1.amazonaws.com

# Etiquetar la imagen
docker tag car-catalog-api:latest 620525694411.dkr.ecr.us-east-1.amazonaws.com/car-catalog-api:latest

# Subir la imagen
docker push 620525694411.dkr.ecr.us-east-1.amazonaws.com/car-catalog-api:latest
```

### 3.3 ECS Fargate

Se creó un cluster de ECS con Fargate para correr la aplicación sin administrar servidores.

**Cluster:**
- Nombre: car-catalog
- Tipo: AWS Fargate

**Task Definition:**
- Familia: car-catalog-task
- CPU: 0.5 vCPU
- Memoria: 1 GB
- Sistema operativo: Linux/X86_64
- Imagen: desde ECR
- Puerto: 8080
- Variables de entorno: DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD, SERVER_PORT

**Service:**
- Nombre: car-catalog-service
- Tipo: Réplica
- Tareas deseadas: 1
- IP pública: Activada
- Security group: default

**Security Group (sg-0a4d2e70a7fb1bc86):**

| Tipo | Protocolo | Puerto | Origen |
|------|-----------|--------|--------|
| Todo el tráfico | Todo | Todo | sg-0a4d2e70a7fb1bc86 |
| PostgreSQL | TCP | 5432 | 0.0.0.0/0 |
| TCP personalizado | TCP | 8080 | 0.0.0.0/0 |

**Resultado:** La API quedó accesible públicamente en http://<IP_PUBLICA>:8080/api/v1/cars

### Problemas encontrados y soluciones

1. **"database car_catalog does not exist"** → La base de datos no se creó automáticamente en RDS. Se creó manualmente con psql.

2. **"password authentication failed"** → La contraseña en la Task Definition no coincidía con la de RDS. Se modificó la contraseña de RDS y se creó una nueva revisión de la Task Definition.

3. **Conexión SSL requerida** → Se agregó `?sslmode=require` al connection string en el application.yml y se reconstruyó la imagen Docker.

---

## 4. Kubernetes (Minikube)

Se implementó Kubernetes localmente con Minikube para demostrar conocimiento en orquestación de contenedores.

### Instalación

```bash
# Minikube
curl -LO https://storage.googleapis.com/minikube/releases/latest/minikube-linux-amd64
sudo install minikube-linux-amd64 /usr/local/bin/minikube

# kubectl
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install kubectl /usr/local/bin/kubectl

# Iniciar cluster
minikube start --driver=docker
```

### Manifiestos Kubernetes

Se crearon 4 manifiestos en la carpeta `k8s/`:

**ConfigMap (k8s/configmap.yaml)** - Configuración no sensible:

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: car-catalog-config
data:
  DB_HOST: car-catalog-db.cu5wy86i6ugs.us-east-1.rds.amazonaws.com
  DB_PORT: "5432"
  DB_NAME: car_catalog
  DB_USERNAME: postgres
  SERVER_PORT: "8080"
```

**Secret (k8s/secret.yaml)** - Datos sensibles:

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: car-catalog-secret
type: Opaque
stringData:
  DB_PASSWORD: CarCatalog2026
```

**Deployment (k8s/deployment.yaml)** - 2 réplicas con health checks:

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: car-catalog-api
  labels:
    app: car-catalog-api
spec:
  replicas: 2
  selector:
    matchLabels:
      app: car-catalog-api
  template:
    metadata:
      labels:
        app: car-catalog-api
    spec:
      containers:
        - name: car-catalog-api
          image: car-catalog-api:latest
          imagePullPolicy: Never
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: car-catalog-config
            - secretRef:
                name: car-catalog-secret
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "512Mi"
              cpu: "500m"
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 180
            periodSeconds: 15
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 240
            periodSeconds: 20
```

**Service (k8s/service.yaml)** - Exponer la aplicación:

```yaml
apiVersion: v1
kind: Service
metadata:
  name: car-catalog-service
spec:
  type: NodePort
  selector:
    app: car-catalog-api
  ports:
    - protocol: TCP
      port: 8080
      targetPort: 8080
      nodePort: 30080
```

### Comandos Kubernetes utilizados

```bash
# Cargar imagen local en Minikube
minikube image load car-catalog-api:latest

# Aplicar todos los manifiestos
kubectl apply -f k8s/

# Ver pods corriendo
kubectl get pods

# Ver todos los recursos
kubectl get all

# Ver logs de un pod
kubectl logs <nombre-del-pod>

# Acceder a la aplicación
minikube service car-catalog-service --url
```

### Resultado

```
NAME                               READY   STATUS    RESTARTS   AGE
car-catalog-api-866b9cc7f7-bl5gt   1/1     Running   0          4m25s
car-catalog-api-866b9cc7f7-zj8r5   1/1     Running   0          8m9s
```

2 réplicas corriendo exitosamente, conectadas a RDS en AWS, accesibles vía NodePort.

---

## Estructura del Proyecto

```
car-api/
├── src/
│   └── main/
│       ├── java/com/cobak/carcatalog/
│       │   ├── controller/
│       │   ├── service/
│       │   ├── repository/
│       │   ├── model/
│       │   ├── exception/
│       │   └── config/
│       └── resources/
│           └── application.yml
├── k8s/
│   ├── configmap.yaml
│   ├── secret.yaml
│   ├── deployment.yaml
│   └── service.yaml
├── Dockerfile
├── docker-compose.yml
├── .dockerignore
├── pom.xml
└── README.md
```

---

## Cómo ejecutar el proyecto

### Opción 1: Local con Docker Compose

```bash
docker build -t car-catalog-api .
docker compose up
# Acceder: http://localhost:8080/swagger-ui.html
```

### Opción 2: Kubernetes con Minikube

```bash
minikube start --driver=docker
docker build -t car-catalog-api .
minikube image load car-catalog-api:latest
kubectl apply -f k8s/
minikube service car-catalog-service --url
```

### Opción 3: AWS ECS Fargate

La aplicación está desplegada en AWS ECS Fargate conectada a RDS PostgreSQL. Ver sección 3 para detalles de configuración.

---

## Tecnologías y conceptos demostrados

- REST API con Spring Boot y JPA
- Containerización con Docker (multi-stage builds)
- Orquestación local con Docker Compose
- AWS ECR para almacenamiento de imágenes Docker
- AWS ECS Fargate para deployment serverless de contenedores
- AWS RDS para base de datos manejada en la nube
- AWS VPC y Security Groups para networking
- Kubernetes: Deployments, Services, ConfigMaps, Secrets
- Health checks (readiness y liveness probes)
- Variables de entorno para configuración por entorno
- Conexión SSL a base de datos en producción

---

## Autor

**Luis Rodriguez** - Backend Developer & DevOps Junior 

