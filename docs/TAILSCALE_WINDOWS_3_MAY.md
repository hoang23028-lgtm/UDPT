# Triển khai UDPT trên 3 máy Windows qua Tailscale

Tài liệu này mô tả cách chạy **cùng mô hình phân tán** như mục 7 trong [HUONG_DAN_CHAY_DU_AN.md](HUONG_DAN_CHAY_DU_AN.md), nhưng khi **3 máy ở 3 mạng khác nhau** (Wi‑Fi nhà khác nhau, không cùng LAN). Dùng **[Tailscale](https://tailscale.com/download)** để các máy có IP riêng trong một **mạng ảo** (thường `100.x.x.x`) và gọi nhau như nội bộ, **không cần** mở port trên router nhà cho DB/Redis/Kafka.

## Mô hình vai trò (giống mục 7.2)

| Máy | Vai trò |
|-----|---------|
| **Máy 1** | Nginx (ingress) + `app-1` + Redpanda (Kafka) |
| **Máy 2** | CockroachDB `crdb-1` + `app-2` + Redis |
| **Máy 3** | CockroachDB `crdb-2` + `crdb-3` + `app-3` |

**Quy ước IP:** gọi IP Tailscale của Máy 1/2/3 lần lượt là **`TS1`**, **`TS2`**, **`TS3`** (lấy bằng `tailscale ip -4`).

## Yêu cầu

- Windows trên cả 3 máy, **Docker Desktop** đã cài và chạy.
- **Tailscale** đã cài, đăng nhập **cùng một tailnet** trên cả 3 máy.
- Kiểm tra kết nối: trên Máy 1, `ping TS2` và `ping TS3` phải thông.

## Cài Tailscale (Windows)

1. Tải và cài từ [https://tailscale.com/download](https://tailscale.com/download).
2. Đăng nhập trên từng máy (cùng tài khoản/team).
3. Lấy IP:

```powershell
tailscale ip -4
```

## Bước 1 – Máy 2: `crdb-1` + Redis

Thay mọi `TS2`, `TS3` bằng IP thật.

### CockroachDB `crdb-1`

```powershell
docker run -d --name crdb-1 --restart unless-stopped `
  -p 26257:26257 -p 8080:8080 `
  cockroachdb/cockroach:v23.2.5 start --insecure `
  --advertise-addr=TS2:26257 `
  --http-addr=0.0.0.0:8080 `
  --listen-addr=0.0.0.0:26257 `
  --join=TS2:26257,TS3:26258,TS3:26259
```

### Redis

```powershell
docker run -d --name redis --restart unless-stopped `
  -p 6379:6379 `
  redis:7-alpine redis-server --appendonly yes --maxmemory 256mb --maxmemory-policy allkeys-lru
```

## Bước 2 – Máy 3: `crdb-2` + `crdb-3`

Thay `TS2`, `TS3` bằng IP thật.

### `crdb-2` (host port `26258`)

```powershell
docker run -d --name crdb-2 --restart unless-stopped `
  -p 26258:26257 -p 8082:8080 `
  cockroachdb/cockroach:v23.2.5 start --insecure `
  --advertise-addr=TS3:26258 `
  --http-addr=0.0.0.0:8080 `
  --listen-addr=0.0.0.0:26257 `
  --join=TS2:26257,TS3:26258,TS3:26259
```

### `crdb-3` (host port `26259`)

```powershell
docker run -d --name crdb-3 --restart unless-stopped `
  -p 26259:26257 -p 8083:8080 `
  cockroachdb/cockroach:v23.2.5 start --insecure `
  --advertise-addr=TS3:26259 `
  --http-addr=0.0.0.0:8080 `
  --listen-addr=0.0.0.0:26257 `
  --join=TS2:26257,TS3:26258,TS3:26259
```

Đợt vài chục giây cho cluster ổn định.

### Tạo database `quizdb` (một lần, trên Máy 2)

```powershell
docker exec -it crdb-1 cockroach sql --insecure --host=TS2:26257 -e "CREATE DATABASE IF NOT EXISTS quizdb;"
```

**Chuỗi JDBC** cho Spring Boot:

```text
jdbc:postgresql://TS2:26257,TS3:26258,TS3:26259/quizdb?sslmode=disable
```

## Bước 3 – Máy 1: Redpanda

Thay `TS1` bằng IP Tailscale của Máy 1.

```powershell
docker run -d --name redpanda --restart unless-stopped `
  -p 9092:9092 `
  docker.redpanda.com/redpandadata/redpanda:v23.3.18 `
  redpanda start --smp=1 --memory=512M --reserve-memory=0M --overprovisioned --node-id=0 --check=false `
  --kafka-addr=PLAINTEXT://0.0.0.0:9092 `
  --advertise-kafka-addr=PLAINTEXT://TS1:9092
```

## Bước 4 – Build và chạy 3 app node

Trên **mỗi** máy (hoặc build một máy rồi `docker save` / `docker load` sang máy khác):

```powershell
git clone https://github.com/hoang23028-lgtm/UDPT.git
cd UDPT
docker build -t udpt-app:latest .
```

Dùng **cùng một** `APP_JWT_SECRET` trên cả 3 máy (chuỗi dài, ngẫu nhiên).

### Máy 1 – `app-1` (bật Flyway)

```powershell
docker run -d --name app-1 --restart unless-stopped `
  -p 8080:8080 `
  -e SPRING_PROFILES_ACTIVE=docker `
  -e SPRING_FLYWAY_ENABLED=true `
  -e "SPRING_DATASOURCE_URL=jdbc:postgresql://TS2:26257,TS3:26258,TS3:26259/quizdb?sslmode=disable" `
  -e SPRING_DATA_REDIS_HOST=TS2 `
  -e SPRING_DATA_REDIS_PORT=6379 `
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=TS1:9092 `
  -e APP_JWT_SECRET="your-256-bit-secret-here" `
  udpt-app:latest
```

### Máy 2 – `app-2` (tắt Flyway)

```powershell
docker run -d --name app-2 --restart unless-stopped `
  -p 8080:8080 `
  -e SPRING_PROFILES_ACTIVE=docker `
  -e SPRING_FLYWAY_ENABLED=false `
  -e "SPRING_DATASOURCE_URL=jdbc:postgresql://TS2:26257,TS3:26258,TS3:26259/quizdb?sslmode=disable" `
  -e SPRING_DATA_REDIS_HOST=TS2 `
  -e SPRING_DATA_REDIS_PORT=6379 `
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=TS1:9092 `
  -e APP_JWT_SECRET="your-256-bit-secret-here" `
  udpt-app:latest
```

### Máy 3 – `app-3` (tắt Flyway)

```powershell
docker run -d --name app-3 --restart unless-stopped `
  -p 8080:8080 `
  -e SPRING_PROFILES_ACTIVE=docker `
  -e SPRING_FLYWAY_ENABLED=false `
  -e "SPRING_DATASOURCE_URL=jdbc:postgresql://TS2:26257,TS3:26258,TS3:26259/quizdb?sslmode=disable" `
  -e SPRING_DATA_REDIS_HOST=TS2 `
  -e SPRING_DATA_REDIS_PORT=6379 `
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=TS1:9092 `
  -e APP_JWT_SECRET="your-256-bit-secret-here" `
  udpt-app:latest
```

## Bước 5 – Máy 1: Nginx

1. Trong repo, sửa `nginx/nginx.conf`: khối `upstream quiz_api` trỏ tới 3 app:

```nginx
upstream quiz_api {
    least_conn;
    server 127.0.0.1:8080 max_fails=3 fail_timeout=30s weight=1;
    server TS2:8080 max_fails=3 fail_timeout=30s weight=1;
    server TS3:8080 max_fails=3 fail_timeout=30s weight=1;
    keepalive 64;
}
```

2. Từ thư mục gốc repo `UDPT` trên Máy 1:

```powershell
docker run -d --name nginx --restart unless-stopped `
  -p 80:80 `
  -v "${PWD}\nginx\nginx.conf:/etc/nginx/nginx.conf:ro" `
  nginx:1.25-alpine
```

## Kiểm tra

Từ bất kỳ máy nào:

```powershell
curl.exe -sS http://TS1/nginx-health
curl.exe -sS -i http://TS1/api/quizzes
```

## An toàn và gợi ý

- **Chỉ public** cổng **80/443** của Nginx nếu cần người ngoài truy cập; DB/Redis/Kafka nên chỉ reachable trong Tailscale (hoặc firewall chỉ allow IP trong tailnet).
- CockroachDB đang `--insecure`: phù hợp **demo**, không dùng cho production.
- Nếu Nginx 502: xem `docker logs nginx`; kiểm tra từ Máy 1 tới `http://TS2:8080` và `http://TS3:8080`; kiểm tra **Windows Firewall** có chặn inbound **8080** trên Máy 2/3 không.

## Frontend dev trỏ về Nginx qua Tailscale

Trên máy bạn chạy Vite, sửa `frontend/vite.config.ts`: proxy `/api` về `http://TS1` (IP Tailscale của Máy 1).

## Tài liệu liên quan

- [HUONG_DAN_CHAY_DU_AN.md](HUONG_DAN_CHAY_DU_AN.md) – mục 7 (cùng mô hình, dùng public IP thay cho TS1/TS2/TS3 nếu không dùng Tailscale).
