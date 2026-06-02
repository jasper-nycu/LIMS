# 測試報告

## 一、測試策略總覽

本系統採用**分層測試策略**，依測試目的與依賴程度分為四個層級：

| 層級 | 類型 | 工具 | 特性 |
|------|------|------|------|
| L1 | 前端元件 / 單元測試 | Vitest + Testing Library | 不需後端；mock API 與 fetch |
| L2 | 後端服務 / 控制器單元測試 | JUnit 5 + Mockito | 不需 DB；mock 所有依賴，毫秒級執行 |
| L3 | 後端整合測試（H2） | JUnit 5 + Spring Boot | 啟動完整 Spring Context；使用 in-memory H2 |
| L4 | 後端整合測試（PostgreSQL） | JUnit 5 + Spring Boot | 需真實 DB；驗證跨服務業務流程與 SQL 正確性 |

---

## 二、測試覆蓋範圍

### 2.1 前端（Vitest）

| 測試檔案 | 測試對象 | Tests | 驗證重點 |
|----------|----------|-------|----------|
| `AuthView.test.tsx` | 登入 / 註冊頁 | — | 表單驗證、錯誤訊息顯示 |
| `FabRequestView.test.tsx` | 廠務申請頁 | — | Wafer ID 格式、重複防呆、實驗室切換、送出流程 |
| `LabOperationsView.test.tsx` | 實驗室操作頁 | — | 機台 FSM 狀態機（Dispatch / Unload / EMG / Alarm） |
| `ManagerDashboardView.test.tsx` | 主管審核頁 | — | 核准 / 拒絕流程、清單顯示 |
| `CapacityAnalyticsView.test.tsx` | 產能分析頁 | — | 圖表渲染、數據呈現 |
| `MyProfileView.test.tsx` | 個人資料頁 | — | 頭像上傳、密碼變更 |
| `Header.test.tsx` | 頁首元件 | — | 導航連結、角色顯示 |
| `Sidebar.test.tsx` | 側欄元件 | — | 選單項目、收合行為 |
| `TotpInput.test.tsx` | TOTP 輸入元件 | 12 | 渲染、數字過濾、倒數計時狀態、resend 事件 |
| `machineApi.test.ts` | 機台 API 層 | 25 | 各 API 函式、HTTP method、錯誤處理、null fallback |
| `CapacityAnalyticsStatusTimeline.test.tsx` | 狀態時間軸 | — | 時間軸事件排序與顯示 |
| **合計** | | **95** | |

### 2.2 後端（JUnit 5）

#### L2 — 單元測試（不需 DB）

| 測試類別 | 測試對象 | Tests | 驗證重點 |
|----------|----------|-------|----------|
| `FabRequestServiceTest` | FabRequestService | 15 | Wafer 格式驗證、重複防呆、優先權、實驗室歸屬、notification fan-out、大小寫正規化 |
| `FabRequestControllerTest` | FabRequestController | 4 | listLabs / createRequest / listRequests 委派行為 |
| `LabManagerServiceTest` | LabManagerService | 9 | 核准/拒絕的 404 / 409 錯誤情境、WIP task 產生、通知觸發 |
| `LabManagerControllerTest` | LabManagerController | 5 | pending / approve / reject（含 null body）委派 |
| `LabOperationsControllerTest` | LabOperationsController | 2 | listPendingWips 委派 |
| `WipServiceTest` | WipService | 4 | findQueue / findPendingSorting / findAll |
| `NotificationServiceTest` | NotificationService | 11 | createNotification、Session Terminated isRead、fan-out、delete 所有權驗證 |
| `NotificationControllerTest` | NotificationController | 7 | 過濾 SessionTerminated、clear / delete / markRead / logout-log |
| `ProfileServiceTest` | ProfileController | 10 | 頭像更新、舊密碼驗證、密碼變更、2FA 驗證 |
| `MachineControllerTest` | MachineController | 1 | getAll 委派與回傳格式 |
| `MachineServiceTest`（unit） | MachineService.findAll() | 1 | WIP count 映射至 MachineResponse |

#### L3 — 整合測試（Spring Boot + H2 in-memory）

| 測試類別 | Tests | 驗證重點 |
|----------|-------|----------|
| `SecurityIntegrationTest` | 2 | JWT 簽發與驗證、ECDSA 非對稱加密簽章流程 |
| `LabOperationsFsmTest` | 12 | 機台完整 FSM：Dispatch → Unload / EMG Unload / Alarm → Maintenance → Online；Guard 條件；addRecipe |

#### L4 — 整合測試（Spring Boot + PostgreSQL）

| 測試類別 | Tests | 驗證重點 |
|----------|-------|----------|
| `BackendApplicationTests` | 1 | Spring Context 啟動（Smoke Test） |
| `FabManagerWorkflowIntegrationTests` | 1 | 跨服務端對端流程：建立申請單 → 核准 → WIP 佇列優先權排序（CRITICAL > URGENT > NORMAL） |

#### 後端合計

| | Tests |
|---|---|
| L2 單元測試 | 69 |
| L3 整合測試（H2） | 14 |
| L4 整合測試（PostgreSQL） | 2 |
| **後端合計** | **85** |

---

## 三、測試執行方式

### 前置條件

```bash
# 啟動 PostgreSQL 容器（L4 整合測試需要）
docker-compose up -d
```

### 執行前端測試

```bash
cd frontend
npm run test:ci
```

### 執行後端測試

```bash
cd backend
./mvnw.cmd test --no-transfer-progress   # Windows
./mvnw test --no-transfer-progress       # macOS / Linux
```

---

## 四、測試結果

### 4.1 前端測試結果

> **【截圖位置】** 執行 `npm run test:ci` 後的終端輸出

```
 RUN  v4.1.7

 Test Files  11 passed (11)
      Tests  95 passed (95)
   Duration  ~4s
```

### 4.2 後端測試結果

> **【截圖位置】** 執行 `./mvnw.cmd test` 後的終端輸出

```
[INFO] Tests run: 85, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 五、關鍵測試案例說明

### 案例一：Fab 申請單輸入驗證（FabRequestServiceTest）

**目的**：確保 API 在錯誤輸入下回傳正確的 HTTP 400，不會將非法資料寫入資料庫。

| 測試方法 | 輸入 | 預期結果 |
|----------|------|----------|
| `createRequest_invalidWaferFormat` | waferIds = `["INVALID"]` | 400 Bad Request：格式必須為 W-XXXX |
| `createRequest_duplicateWaferIds` | waferIds = `["W-1234", "W-1234"]` | 400 Bad Request：禁止重複 Wafer ID |
| `createRequest_invalidPriority` | priority = `"RUSH"` | 400 Bad Request：必須為 NORMAL / URGENT / CRITICAL |
| `createRequest_requesterNotFound` | requesterId = `"TS-9999"`（不存在） | 404 Not Found |
| `createRequest_waferIdNormalizesToUppercase` | waferIds = `["w-1234"]`（小寫） | 成功，自動轉為 `W-1234` |

### 案例二：機台狀態機（LabOperationsFsmTest）

**目的**：確保機台 FSM 狀態轉換符合業務規則，非法轉換會拋出例外。

```
IDLE ──dispatch──→ PROCESSING ──simulateError──→ ALARM
                                                    │
                                         toMaintenance / resolveAlarm
                                                    │
                                          MAINTENANCE / PROCESSING
```

| 測試方法 | 驗證內容 |
|----------|----------|
| `testDispatch` | Machine = PROCESSING；WIP tasks 狀態 = PROCESSING；Manager / Operator 收到通知 |
| `testSafeUnload` | Machine = IDLE；WIP tasks 狀態 = COMPLETED |
| `testEmgUnloadReuse` | Machine = IDLE；WIP tasks 狀態 = PENDING_SORTING（回到等待排程） |
| `testEmgUnloadScrap` | Machine = IDLE；WIP tasks 狀態 = SCRAPPED |
| `testSimulateError` | Machine = ALARM；Machine Owner 收到 error 類型通知 |
| `testCannotSimulateErrorOnIdle` | 對 IDLE 機台呼叫 simulateError → 拋出 InvalidStateTransitionException |

### 案例三：跨服務 WIP 優先權排序（FabManagerWorkflowIntegrationTests）

**目的**：確保核准申請單後，WIP 佇列依優先權排序（不只測 Service 層，連 SQL ORDER BY 邏輯一起驗）。

```
建立申請單順序：NORMAL → CRITICAL(1) → URGENT → CRITICAL(2)
核准後 WIP 佇列預期排序：CRITICAL(1) → CRITICAL(2) → URGENT → NORMAL
```

此測試直接對 PostgreSQL 執行，確保 `findByStatusInOrderedForSorting` 的 JPQL 排序在真實資料庫上也正確。

---

## 六、總結

| | 前端 | 後端 | 合計 |
|---|---|---|---|
| 測試數量 | 95 | 85 | **180** |
| Failures | 0 | 0 | **0** |
| 測試類型 | 元件 / 單元 / API | 單元 / 整合（H2）/ 整合（PG） | — |
