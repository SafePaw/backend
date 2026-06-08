# SafePaw OCI 서버 — 운영 가이드 (oracle2)

> **대상**: 초기 세팅이 끝난 뒤, **재배포·Docker 변경·DB/JPA·MinIO 버킷** 을 스스로 할 때 보는 문서.  
> **초기 설치** 는 `docker/` 의 `docker-compose.yml` 주석 및 팀 공유 `oracle.md` 참고.

| 표기 | 의미 |
| --- | --- |
| **맥** | 로컬 Mac 터미널 / IntelliJ |
| **서버** | SSH `ubuntu@<PUBLIC_IP>` (예: `168.107.2.91`) |
| **OCI** | [cloud.oracle.com](https://cloud.oracle.com) 콘솔 |

---

## 1. 서버 세팅은 끝났나?

**인프라·API 기동 기준으로는 완료** 로 보면 됩니다.

| 항목 | 상태 |
| --- | --- |
| Docker (PostGIS, Redis, MinIO) | ✅ `docker ps` 에 3개 Up |
| MinIO 버킷 3개 (`minio-init`) | ✅ `dog-markers`, `share-cards`, `presets` |
| Spring Boot JAR + systemd | ✅ `safepaw-api` active |
| 내부 health | ✅ 서버에서 `curl http://127.0.0.1:8080/actuator/health` → `UP` |
| 외부 health | ✅ 맥에서 `curl http://<PUBLIC_IP>:8080/actuator/health` → `UP` |
| iptables 8080 | ✅ INPUT에 `8080 ACCEPT` + `netfilter-persistent save` |

**아직 선택·미완 (기능 개발 시)**

| 항목 | 비고 |
| --- | --- |
| `auth_tables.sql` / OAuth 테이블 | OAuth·Entity 작업 시 1회 실행 |
| OCI **9000** 인그레스 | MinIO 직접 노출 줄이려면 **제거 권장** (§6) |
| HTTPS(443) + nginx | 실서비스 오픈 전 |
| `application-prod.yml` 공인 IP | IP 바뀌면 JAR **재빌드** 후 재배포 |

---

## 2. VM 안 구조 (한눈에)

```
/opt/safepaw/
├── docker-compose.yml    # backend/docker/docker-compose.yml 과 동일하게 유지
├── .env.data             # Compose 전용
├── .env.prod             # systemd → Spring
└── safepaw-api.jar

Repo: backend/docker/docker-compose.yml
```

---

## 3. Spring Boot만 바꿨을 때 (가장 자주)

Docker는 **건드리지 않음**.

### 3.1 맥 — 빌드

```bash
cd ~/Documents/GitHub/safepaw/backend
./gradlew bootJar
```

### 3.2 맥 → 서버

IntelliJ SFTP → `/opt/safepaw/safepaw-api.jar`  
또는:

```bash
scp -i ~/.ssh/oci_key \
  build/libs/safepaw-api-0.0.1-SNAPSHOT.jar \
  ubuntu@<PUBLIC_IP>:/opt/safepaw/safepaw-api.jar
```

### 3.3 서버 — 재시작

```bash
sudo systemctl restart safepaw-api
curl -s http://127.0.0.1:8080/actuator/health
```

### 3.4 맥 — 확인

```bash
curl http://<PUBLIC_IP>:8080/actuator/health
```

`application-prod.yml` / `application-secret.yml` 변경 시 **반드시 재빌드**.  
`.env.prod` 만 변경 시 **재빌드 없이** `systemctl restart`.

---

## 4. `docker-compose.yml` 변경 시

### 4.1 맥

```bash
scp -i ~/.ssh/oci_key \
  docker/docker-compose.yml \
  ubuntu@<PUBLIC_IP>:/opt/safepaw/docker-compose.yml
```

### 4.2 서버

```bash
cd /opt/safepaw
docker-compose --env-file .env.data up -d
# 이미지 변경 시: pull 후 up -d
sudo systemctl restart safepaw-api
```

### 4.3 새 VM — iptables 8080

```bash
sudo iptables -I INPUT 5 -p tcp -m tcp --dport 8080 -j ACCEPT
sudo netfilter-persistent save
```

---

## 5. DB · JPA

**prod (`application-prod.yml`): `ddl-auto: validate`**

- Entity만 추가하고 JAR 올려도 **테이블 자동 생성 안 됨**.
- 로컬 `application.yaml` 의 `update` 는 prod에서 **덮어씀**.

| 방법 | 설명 |
| --- | --- |
| SQL | `oracle/schema/auth_tables.sql` (SafePaw repo) 등 |
| Flyway | `src/main/resources/db/migration/V*.sql` 추가 후 배포 |

체크: `journalctl -u safepaw-api -n 100`

---

## 6. MinIO 버킷

| 버킷 | yml 키 |
| --- | --- |
| `dog-markers` | `bucket-markers` |
| `share-cards` | `bucket-share-cards` |
| `presets` | `bucket-presets` |

- API → MinIO: `endpoint: http://127.0.0.1:9000`
- 공개 URL: `public-base-url` + `/{bucket}/{key}`

서버에서 목록:

```bash
cd /opt/safepaw && set -a && source .env.data && set +a
docker run --rm --network host minio/mc:latest sh -c "
  mc alias set local http://127.0.0.1:9000 \$MINIO_ROOT_USER \$MINIO_ROOT_PASSWORD &&
  mc ls local/
"
```

OCI **9000 제거**해도 API↔MinIO(localhost)는 동작. 클라이언트 직접 URL은 막힘.

---

## 7. 명령 치트시트

| 위치 | 명령 |
| --- | --- |
| 서버 | `docker ps` / `systemctl status safepaw-api` / `journalctl -u safepaw-api -f` |
| 서버 | `docker exec safepaw-postgres psql -U safepaw -d safepaw -c "\dt"` |
| 맥 | `curl http://<PUBLIC_IP>:8080/actuator/health` |

---

## 8. 배포 요약

| 변경 | 맥 | 서버 |
| --- | --- | --- |
| Java / JAR yml | `bootJar` + upload | `systemctl restart safepaw-api` |
| `.env.prod` | upload | `systemctl restart` |
| compose / `.env.data` | upload | `docker-compose up -d` + API restart |

전체 상세·예시·다이어그램: `SafePaw/oracle/oracle2.md` (동일 내용 확장본).

---

*2026-06-03 · Spring Boot 3.5 · Java 17*
