# Hướng dẫn chạy dự án UDPT (chi tiết)

Tài liệu này hướng dẫn chạy **backend (Spring Boot + CockroachDB + Redis + Redpanda + Nginx)** bằng Docker Compose và chạy **frontend (React/Vite)** ở chế độ dev.

## 1) Yêu cầu môi trường

### Bắt buộc

- **Docker Desktop** (Windows/macOS) hoặc Docker Engine (Linux)
- **Docker Compose v2** (lệnh `docker compose ...`)
- **Node.js 18+** (khuyến nghị Node 20) và **npm**

### Tùy chọn (khi muốn chạy backend ngoài Docker)

- **Java 21**
- **Maven** (`mvn`)

## 2) Chạy backend stack bằng Docker Compose

Tại thư mục root của repo:

```bash
docker compose up -d --build
docker compose ps
```

### Chờ service healthy

`docker-compose.yml` có healthcheck cho CockroachDB/Redis/Redpanda. Nếu mới chạy lần đầu, hãy đợi ~1–2 phút rồi test API.

### Test nhanh

- Health của Nginx:

```bash
curl -sS http://localhost/nginx-health
```

- Gọi thử API:

```bash
curl -sS -i http://localhost/api/quizzes
```

### Xem logs khi cần debug

```bash
docker compose logs --tail=200 nginx
docker compose logs --tail=200 app1 app2 app3
docker compose logs --tail=200 kafka
docker compose logs --tail=200 crdb-1 crdb-2 crdb-3
docker compose logs --tail=200 redis
```

### Dừng stack

```bash
docker compose down
```

Nếu muốn xóa luôn volume dữ liệu DB/cache:

```bash
docker compose down -v
```

## 3) Chạy frontend (React/Vite)

Frontend nằm trong thư mục `frontend/`.

### Cài dependencies

```bash
cd frontend
npm install
```

### Chạy dev server

```bash
npm run dev
```

Mặc định:

- Frontend chạy ở `http://localhost:5173`
- Các request `/api` được proxy về `http://localhost` (Nginx/API) theo cấu hình trong `frontend/vite.config.ts`

## 4) Thông tin cấu hình backend (Docker)

### Profile `docker`

Các container backend chạy với:

- `SPRING_PROFILES_ACTIVE=docker`

File cấu hình chính: `src/main/resources/application-docker.properties` (kết nối CockroachDB/Redis/Redpanda).

### Flyway migrations khi chạy nhiều node

Trong `docker-compose.yml`:

- `app1` bật `SPRING_FLYWAY_ENABLED=true` để chạy migrations
- `app2`, `app3` tắt flyway để tránh tranh chấp (distributed lock contention)

## 5) Cổng dịch vụ (mặc định)

- **Nginx/API**: `http://localhost` (port 80)
- **Frontend dev**: `http://localhost:5173`
- **CockroachDB Admin UI**:
  - `http://localhost:8081` (crdb-1)
  - `http://localhost:8082` (crdb-2)
  - `http://localhost:8083` (crdb-3)
- **Kafka (Redpanda)**: `localhost:9092`
- **Redis**: `localhost:6379`

## 6) Troubleshooting (thường gặp)

### 6.1 API 502/504 qua Nginx

1. Kiểm tra container có chạy không:

```bash
docker compose ps
```

2. Xem logs:

```bash
docker compose logs --tail=200 nginx
docker compose logs --tail=200 app1 app2 app3
```

### 6.2 Redpanda/Kafka chưa healthy

Đợi thêm 30–60s rồi kiểm tra logs:

```bash
docker compose logs --tail=200 kafka
```

### 6.3 Port bị chiếm (80/5173/9092/6379/8081..8083)

- Đổi port mapping trong `docker-compose.yml` (hoặc tắt service đang dùng port đó).

### 6.4 Frontend gọi API không được

- Đảm bảo backend stack đang chạy và test được:

```bash
curl -sS http://localhost/nginx-health
```

- Kiểm tra proxy trong `frontend/vite.config.ts` (mặc định proxy `/api` → `http://localhost`).

## 7) Triển khai phân tán nhiều máy (public demo) – Cách B

Mục tiêu: chạy đúng mô hình phân tán **nhiều máy**, public ra Internet qua **Nginx**, đồng thời **tách riêng**:

- 3× CockroachDB node
- 1× Redis
- 1× Redpanda (Kafka)
- 3× Spring Boot app node
- 1× Nginx load balancer (public)

### 7.1 Nguyên tắc an toàn khi demo public

- **Chỉ public Nginx (80/443)**.
- **Không public DB/Redis/Kafka** trực tiếp ra Internet.
  - Tốt nhất: đặt các node trong **private network/VPC** và chỉ mở inbound từ app nodes.
  - Nếu buộc phải dùng public IP: cấu hình firewall để **chỉ cho phép IP của các máy app** truy cập port nội bộ.
- CockroachDB trong `docker-compose.yml` hiện chạy `--insecure`. Với demo public, hãy coi đây là **demo kỹ thuật**, không dùng cho production.

### 7.2 Triển khai khi chỉ có 3 máy (khuyến nghị)

Với **3 máy**, mô hình tối thiểu nhưng vẫn “phân tán” hợp lý là:

- **Máy 1 (PUBLIC)**: `nginx` + `app-1` + `redpanda` (Kafka)
- **Máy 2 (PRIVATE)**: `crdb-1` + `app-2` + `redis`
- **Máy 3 (PRIVATE)**: `crdb-2` + `crdb-3` + `app-3`

Giải thích nhanh:

- CockroachDB cần **3 node** để chịu lỗi (mất 1 node vẫn còn quorum). Với 3 máy, bạn có thể đặt **2 node DB trên cùng 1 máy** (Máy 3) để đủ 3 node cho demo; đây **không tối ưu HA** nhưng đủ cho mô phỏng “cluster” và failover cơ bản.
- Redis/Redpanda không nhất thiết phải tách ra máy riêng khi demo; ưu tiên tách khỏi Nginx hoặc DB để tránh “chết cùng nhau”.

Tất cả máy cần cài:

- **Docker** + **Docker Compose v2**

> Nếu bạn có thể mở rộng lên 4–5 máy, mình khuyến nghị tách DB 3 node mỗi máy 1 node, và Redis/Redpanda/Nginx/app tách rời hơn.

### 7.2.1 Ports và firewall (cực quan trọng khi public)

- Public Internet chỉ mở:
  - **Máy 1**: `80` (và `443` nếu sau này cấu hình TLS)
- Mở giữa các máy (chỉ allow theo IP của các máy trong cụm):
  - **CockroachDB**: `26257/tcp` giữa Máy 2 ↔ Máy 3 ↔ Máy 1 (app nodes cần truy cập)
  - **Redis**: `6379/tcp` từ Máy 1 & 3 → Máy 2
  - **Redpanda/Kafka**: `9092/tcp` từ Máy 2 & 3 → Máy 1
  - **App nodes**: `8080/tcp` giữa các máy (ít nhất: Máy 1 Nginx → Máy 1/2/3 app)

### 7.3 Chạy CockroachDB cluster (3 máy)

Trên từng máy `crdb-*`, chạy container CockroachDB và join vào cluster.

Ví dụ trên `crdb-1` (thay `PUBLIC_IP_CRDB_1/2/3` bằng IP thật):

```bash
docker run -d --name crdb-1 --restart unless-stopped ^
  -p 26257:26257 -p 8080:8080 ^
  cockroachdb/cockroach:v23.2.5 start --insecure ^
  --advertise-addr=PUBLIC_IP_CRDB_1:26257 ^
  --http-addr=0.0.0.0:8080 ^
  --listen-addr=0.0.0.0:26257 ^
  --join=PUBLIC_IP_CRDB_1:26257,PUBLIC_IP_CRDB_2:26257,PUBLIC_IP_CRDB_3:26257
```

Tương tự cho `crdb-2`, `crdb-3` (đổi `--name` và `--advertise-addr`).

#### Chạy 2 node CockroachDB trên cùng 1 máy (khi chỉ có 3 máy)

Nếu bạn chọn đặt `crdb-2` và `crdb-3` trên **Máy 3**, hãy chạy 2 container với **port host khác nhau**:

Ví dụ trên Máy 3:

`crdb-2`:

```bash
docker run -d --name crdb-2 --restart unless-stopped ^
  -p 26258:26257 -p 8082:8080 ^
  cockroachdb/cockroach:v23.2.5 start --insecure ^
  --advertise-addr=PUBLIC_IP_MAY_3:26258 ^
  --http-addr=0.0.0.0:8080 ^
  --listen-addr=0.0.0.0:26257 ^
  --join=PUBLIC_IP_MAY_2:26257,PUBLIC_IP_MAY_3:26258,PUBLIC_IP_MAY_3:26259
```

`crdb-3`:

```bash
docker run -d --name crdb-3 --restart unless-stopped ^
  -p 26259:26257 -p 8083:8080 ^
  cockroachdb/cockroach:v23.2.5 start --insecure ^
  --advertise-addr=PUBLIC_IP_MAY_3:26259 ^
  --http-addr=0.0.0.0:8080 ^
  --listen-addr=0.0.0.0:26257 ^
  --join=PUBLIC_IP_MAY_2:26257,PUBLIC_IP_MAY_3:26258,PUBLIC_IP_MAY_3:26259
```

Khi đó, chuỗi JDBC từ app sẽ dùng:

- Máy 2: `PUBLIC_IP_MAY_2:26257`
- Máy 3: `PUBLIC_IP_MAY_3:26258` và `PUBLIC_IP_MAY_3:26259`

Ví dụ:

`jdbc:postgresql://PUBLIC_IP_MAY_2:26257,PUBLIC_IP_MAY_3:26258,PUBLIC_IP_MAY_3:26259/quizdb?sslmode=disable`

Sau khi 3 node lên, tạo database logical `quizdb` (chạy 1 lần trên bất kỳ node):

```bash
docker exec -it crdb-1 cockroach sql --insecure --host=PUBLIC_IP_CRDB_1:26257 -e "CREATE DATABASE IF NOT EXISTS quizdb;"
```

### 7.4 Chạy Redis (1 máy)

Với mô hình 3 máy (mục 7.2), chạy Redis trên **Máy 2**:

```bash
docker run -d --name redis --restart unless-stopped ^
  -p 6379:6379 ^
  redis:7-alpine redis-server --appendonly yes --maxmemory 256mb --maxmemory-policy allkeys-lru
```

### 7.5 Chạy Redpanda/Kafka (1 máy)

Với mô hình 3 máy (mục 7.2), chạy Redpanda trên **Máy 1**:

```bash
docker run -d --name redpanda --restart unless-stopped ^
  -p 9092:9092 ^
  docker.redpanda.com/redpandadata/redpanda:v23.3.18 ^
  redpanda start --smp=1 --memory=512M --reserve-memory=0M --overprovisioned --node-id=0 --check=false ^
  --kafka-addr=PLAINTEXT://0.0.0.0:9092 ^
  --advertise-kafka-addr=PLAINTEXT://PUBLIC_IP_KAFKA_1:9092
```

### 7.6 Chạy 3 app nodes (3 máy)

Với mô hình 3 máy (mục 7.2), chạy:

- **Máy 1**: `app-1`
- **Máy 2**: `app-2`
- **Máy 3**: `app-3`

Trên mỗi máy:

1) Clone code:

```bash
git clone https://github.com/hoang23028-lgtm/UDPT.git
cd UDPT
```

2) Build image (Dockerfile multi-stage, không cần Maven trên host):

```bash
docker build -t udpt-app:latest .
```

3) Run container và trỏ tới DB/Redis/Kafka bằng biến môi trường.

Ví dụ `app-1` (Máy 1):

```bash
docker run -d --name app-1 --restart unless-stopped ^
  -p 8080:8080 ^
  -e SPRING_PROFILES_ACTIVE=docker ^
  -e SPRING_FLYWAY_ENABLED=true ^
  -e "SPRING_DATASOURCE_URL=jdbc:postgresql://PUBLIC_IP_MAY_2:26257,PUBLIC_IP_MAY_3:26258,PUBLIC_IP_MAY_3:26259/quizdb?sslmode=disable" ^
  -e SPRING_DATA_REDIS_HOST=PUBLIC_IP_MAY_2 ^
  -e SPRING_DATA_REDIS_PORT=6379 ^
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=PUBLIC_IP_MAY_1:9092 ^
  -e APP_JWT_SECRET="your-256-bit-secret-here" ^
  udpt-app:latest
```

Trên `app-2` và `app-3`, chạy tương tự nhưng đặt:

- `SPRING_FLYWAY_ENABLED=false`

### 7.7 Chạy Nginx public (1 máy)

Với mô hình 3 máy (mục 7.2), chạy Nginx trên **Máy 1**:

1) Clone repo để lấy file `nginx/nginx.conf` làm mẫu, sau đó **sửa upstream** trỏ tới 3 app node theo IP public:

Trong `nginx/nginx.conf`, thay:

- `server app1:8080` → `server 127.0.0.1:8080` (vì `app-1` cùng máy với Nginx)
- `server app2:8080` → `server PUBLIC_IP_MAY_2:8080`
- `server app3:8080` → `server PUBLIC_IP_MAY_3:8080`

2) Run Nginx:

```bash
docker run -d --name nginx --restart unless-stopped ^
  -p 80:80 ^
  -v "%cd%\\nginx\\nginx.conf:/etc/nginx/nginx.conf:ro" ^
  nginx:1.25-alpine
```

Test từ máy bất kỳ:

```bash
curl -sS http://PUBLIC_IP_NGINX_1/nginx-health
curl -sS -i http://PUBLIC_IP_NGINX_1/api/quizzes
```

### 7.8 Chạy frontend để demo public

Có 2 lựa chọn:

- **A) Chạy local (máy cá nhân)** và trỏ API về Nginx public:
  - Sửa `frontend/vite.config.ts` proxy target thành `http://PUBLIC_IP_NGINX_1`
- **B) Host frontend lên một static host** (khuyến nghị) và trỏ base API về domain của Nginx (tuỳ cách bạn cấu hình frontend).


