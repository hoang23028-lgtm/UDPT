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

