# UDPT – Distributed Quiz System (Spring Boot + CockroachDB + Kafka/Redpanda + Redis + Nginx + React/Vite)

UDPT là **hệ thống Quiz phân tán** gồm nhiều node API Spring Boot chạy phía sau Nginx, dữ liệu nằm trên **CockroachDB cluster**, dùng **Redis** để cache (leaderboard), và **Kafka-compatible broker (Redpanda)** để xử lý event-driven khi người dùng nộp bài. Frontend được viết bằng **React + Vite**.

## Tài liệu trong repo

- **Hướng dẫn chạy dự án (chi tiết, từ A–Z)**: [docs/HUONG_DAN_CHAY_DU_AN.md](docs/HUONG_DAN_CHAY_DU_AN.md)
- **Load test submit (k6)**: [scripts/k6/submit.js](scripts/k6/submit.js)

## Kiến trúc tổng quan

- **API (backend)**: Spring Boot 3.2.x, Java 21 (`pom.xml`)
- **Database**: CockroachDB 3 node (PostgreSQL wire protocol) + Flyway migrations (`src/main/resources/db/migration`)
- **Cache**: Redis (cache leaderboard)
- **Event bus**: Redpanda (Kafka API)
  - Topic chính: `quiz.submitted`
  - Producer publish event **sau khi DB commit** (AFTER_COMMIT)
  - Consumer thiết kế theo hướng **idempotent** + retry
- **Load balancer**: Nginx (`nginx/nginx.conf`) cân bằng tải `least_conn` → `app1`, `app2`, `app3`
- **Frontend**: React 18 + Vite 5 (`frontend/`)

### Luồng “submit quiz” (tóm tắt)

1. Client gọi `POST /api/quizzes/{id}/submit`
2. Backend ghi `submissions/results` vào DB
3. Sau khi transaction commit, backend publish event `quiz.submitted`
4. Consumer xử lý event (claim idempotent theo `submission_id`), cập nhật/tái tính toán (nếu cần), và invalidate cache leaderboard

## Cấu trúc thư mục (điểm chính)

- **`src/`**: mã nguồn backend (Spring Boot)
- **`frontend/`**: frontend React/Vite
- **`docker-compose.yml`**: dựng môi trường phân tán (CockroachDB/Redis/Redpanda/3×app/Nginx)
- **`nginx/nginx.conf`**: cấu hình load balancer
- **`scripts/k6/`**: kịch bản test tải (k6)

## Cách chạy nhanh (Quickstart)

> Chi tiết (Windows/Linux/macOS), kèm troubleshooting: xem [docs/HUONG_DAN_CHAY_DU_AN.md](docs/HUONG_DAN_CHAY_DU_AN.md).

### 1) Chạy backend stack (Docker Compose)

```bash
docker compose up -d --build
docker compose ps
```

Test nhanh:

```bash
curl -sS http://localhost/nginx-health
curl -sS -i http://localhost/api/quizzes
```

### 2) Chạy frontend (dev)

```bash
cd frontend
npm install
npm run dev
```

Frontend dev server mặc định chạy ở `http://localhost:5173` và proxy `/api` về `http://localhost` (Nginx/API).

## Idempotency khi submit (chống timeout / retry / duplicate)

- Endpoint: `POST /api/quizzes/{id}/submit`
- Header khuyến nghị: **`Idempotency-Key`**

Ví dụ:

```bash
curl -sS -X POST "http://localhost/api/quizzes/<QUIZ_ID>/submit" \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Idempotency-Key: 7c9e6679-7425-40de-944b-e07fc1f90ae7" \
  -H "Content-Type: application/json" \
  -d '{"answers":{"<QUESTION_ID>":0}}'
```

Quy tắc:

- Gửi lại cùng `Idempotency-Key` → server trả lại **kết quả cũ** (không tạo submission mới)
- Nếu đã submit quiz đó rồi mà dùng key khác → trả **409 Conflict**

## Cổng dịch vụ (mặc định)

- **Nginx/API**: `http://localhost` (port 80)
- **Frontend dev**: `http://localhost:5173`
- **CockroachDB Admin UI**:
  - `http://localhost:8081` (crdb-1)
  - `http://localhost:8082` (crdb-2)
  - `http://localhost:8083` (crdb-3)
- **Kafka (Redpanda)**: `localhost:9092`
- **Redis**: `localhost:6379`


ADMIN: admin@udpt.local / Passw0rd
TEACHER: teacher@udpt.local / Passw0rd
STUDENT: student@udpt.local / Passw0rd