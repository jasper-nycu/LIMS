# 運維與可靠性監控報告

## 一、監控架構總覽

本系統採用 **Prometheus + Grafana + Spring Boot Actuator** 三層式監控架構，對後端服務進行即時指標收集與視覺化呈現。

```
┌─────────────────────────────────────────────────────────┐
│                    Docker Compose                        │
│                                                         │
│  ┌──────────────┐   每 15 秒抓取    ┌──────────────┐   │
│  │ LIMS Backend │ ──────────────→  │  Prometheus  │   │
│  │  :8080       │  /actuator/      │    :9090     │   │
│  │              │   prometheus     │              │   │
│  └──────────────┘                  └──────┬───────┘   │
│                                           │ 查詢指標   │
│  ┌──────────────┐                  ┌──────▼───────┐   │
│  │  PostgreSQL  │                  │   Grafana    │   │
│  │    :5432     │                  │    :3000     │   │
│  └──────────────┘                  └──────────────┘   │
└─────────────────────────────────────────────────────────┘
```

| 元件 | 角色 | 連線位址 |
|------|------|----------|
| Spring Boot Actuator | 暴露 `/actuator/prometheus` 端點，提供指標原始資料 | `http://localhost:8080/actuator/prometheus` |
| Prometheus | 定期抓取 Actuator 資料並儲存時序資料庫 | `http://localhost:9090` |
| Grafana | 查詢 Prometheus 並以折線圖視覺化呈現 | `http://localhost:3000` |

---

## 二、啟動方式

### 前置條件

所有服務均容器化，透過 Docker Compose 統一管理，只需一行指令即可啟動：

```bash
docker-compose up -d
```

啟動後各服務狀態確認：

```bash
docker ps
```

預期輸出：

```
NAMES             PORTS                    STATUS
lims_backend      0.0.0.0:8080->8080/tcp   Up
lims_db           0.0.0.0:5432->5432/tcp   Up
lims_prometheus   0.0.0.0:9090->9090/tcp   Up
lims_grafana      0.0.0.0:3000->3000/tcp   Up
lims_pgadmin      0.0.0.0:5050->80/tcp     Up
```

### 連線步驟

**Step 1 — 確認後端健康狀態**

打開瀏覽器，前往：
```
http://localhost:8080/actuator/health
```

預期回應（`status: UP` 表示服務正常）：

```json
{
  "status": "UP",
  "components": {
    "db":        { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "ping":      { "status": "UP" }
  }
}
```

**Step 2 — 確認 Prometheus 正在抓取後端**

前往 `http://localhost:9090/targets`，確認 `lims-backend` 的 State 欄位為 **UP**：

```
Endpoint                                    State   Labels
http://backend:8080/actuator/prometheus     UP      job="lims-backend"
```

> **【截圖位置】** Prometheus Targets 頁面，顯示 lims-backend State=UP

**Step 3 — 登入 Grafana**

```
URL:      http://localhost:3000
帳號:     admin
密碼:     admin
```

---

## 三、監控儀表板

### 儀表板建立方式

進入 Grafana 後，新增一個名為 **LIMS Monitoring** 的 Dashboard，包含以下四個面板，每個面板使用 PromQL 直接查詢 Prometheus：

| Panel | PromQL 查詢 | 視覺化類型 |
|-------|------------|-----------|
| Heap Memory Used | `sum(jvm_memory_used_bytes{application="lims-backend", area="heap"})` | Time series |
| HTTP Request Rate | `rate(http_server_requests_seconds_count{application="lims-backend"}[1m])` | Time series |
| CPU Usage | `process_cpu_usage{application="lims-backend"}` | Time series |
| Live Threads | `jvm_threads_live_threads{application="lims-backend"}` | Time series |

> **【截圖位置】** Grafana 儀表板完整畫面，包含四個折線圖面板

---

## 四、監控指標說明

### 4.1 Heap Memory Used（JVM 堆積記憶體使用量）

**監控目的**：偵測記憶體洩漏，確保長時間運行的服務穩定性。

LIMS 後端需長時間維護機台 FSM 狀態、WIP 佇列與通知推播，若記憶體持續攀升而不釋放，表示存在 memory leak 風險。Heap 使用量接近 JVM 上限時，會觸發 Full GC 甚至 OutOfMemoryError，導致服務崩潰。

| 正常範圍 | 告警條件 |
|----------|----------|
| 穩定波動，GC 後能有效回收 | 持續上升且 GC 後不下降 |

> **【截圖位置】** Heap Memory Used 折線圖，顯示記憶體使用量隨時間的變化

---

### 4.2 HTTP Request Rate（HTTP 請求速率）

**監控目的**：掌握 API 流量分佈，及早發現異常暴增或服務無回應。

此指標按 URI 細分，可觀察 `/api/v1/machines`（機台操作）、`/api/v1/lab/wips`（WIP 佇列）、`/api/v1/fab-requests`（申請單）等各端點的請求速率。HTTP 403/404/500 的請求同樣被追蹤，可用於偵測未授權存取或業務邏輯異常。

| 正常範圍 | 告警條件 |
|----------|----------|
| 與使用者活躍度吻合 | 流量突降（服務可能無法接收請求）或突增（可能遭受異常攻擊） |

> **【截圖位置】** HTTP Request Rate 折線圖，顯示各 API 端點的請求速率

---

### 4.3 CPU Usage（CPU 使用率）

**監控目的**：反映運算密集操作的負載，作為水平擴展的決策依據。

LIMS 後端的 ECDSA 非對稱加密簽章（FabRequest 非抵賴性驗證）、JWT 簽發與驗證、FSM 狀態批次轉換等操作均為 CPU 密集型。若 CPU 持續高於 80%，需考慮增加後端實例進行水平擴展。

| 正常範圍 | 告警條件 |
|----------|----------|
| 請求期間短暫升高，閒置時接近 0% | 長時間維持 80% 以上 |

> **【截圖位置】** CPU Usage 折線圖，顯示 CPU 使用率隨請求量的變化

---

### 4.4 Live Threads（存活執行緒數）

**監控目的**：監控執行緒池健康度，預防服務因執行緒耗盡而無法回應。

Spring Boot 內嵌的 Tomcat 採用執行緒池模型，每個 HTTP 連線佔用一條執行緒。若執行緒數持續暴增不回落，通常代表有請求在等待阻塞（如 DB 連線池耗盡），最終導致整個服務無法接受新請求。

| 正常範圍 | 告警條件 |
|----------|----------|
| 20–50 條（視並發量） | 持續攀升超過 200 條 |

> **【截圖位置】** Live Threads 折線圖，顯示執行緒數量的穩定狀態

---

## 五、系統健康指標總結

| 指標 | 工具 | 監控層級 | 告警意義 |
|------|------|----------|----------|
| 服務整體健康 | Spring Boot Actuator `/health` | 服務層 | DB 連線中斷、磁碟不足 |
| Heap Memory | Prometheus + Grafana | JVM 層 | 記憶體洩漏、OOM 風險 |
| HTTP Request Rate | Prometheus + Grafana | API 層 | 流量異常、服務無回應 |
| CPU Usage | Prometheus + Grafana | 系統層 | 運算過載、需擴展 |
| Live Threads | Prometheus + Grafana | JVM 層 | 執行緒耗盡、請求堆積 |

本監控架構以最小配置成本（僅新增兩個 Docker 容器）實現生產級別的可觀測性，符合雲原生系統對 **可靠性（Reliability）** 與 **可觀測性（Observability）** 的基本要求。
