# LIMS 後端架構 (Backend Architecture)

本後端專案是 LIMS 雲原生系統的核心業務與資料處理中樞，採用 Java 25 與 Spring Boot 構建。系統嚴格遵循「三層式架構 (3-Tier Architecture)」原則，將路由控制、商業邏輯與資料庫操作徹底解耦，並導入了無狀態 JWT 與 ECDSA 數位簽章以符合企業級資安標準。

## 專案架構 (Project Structure)

專案結構依照「領域驅動設計 (Domain-Driven Design)」的初步概念，以功能模組 (Feature Modules) 進行封裝：

```text
backend/
├── .mvn/wrapper/                       # Maven Wrapper 設定檔，確保跨平台建置版本一致
│
├── src/main/java/com/tsmc/lims/backend/
│   │
│   ├── config/                         # 全域設定模組
│   │   ├── ApplicationConfig.java      # 定義 PasswordEncoder (BCrypt) 與其他基礎 Bean 
│   │   └── SecurityConfig.java         # 負責設定無狀態 JWT 攔截器、CORS 白名單與 CSRF 防護
│   │
│   ├── auth/                           # 身分驗證與授權模組 (Authentication & Authorization)
│   │   ├── controller/                 # 負責處理登入、註冊的 HTTP 請求
│   │   ├── dto/                        # 登入與註冊用的資料傳輸載體 (Data Transfer Objects)
│   │   ├── entity/                     # JPA 實體層，對應 PostgreSQL 的 users 與 roles 表格
│   │   ├── repository/                 # 負責操作 User 與 Role 的資料庫 CRUD 介面
│   │   ├── security/                   # JWT 簽發、過濾器與 ECDSA 密碼學加密工具
│   │   └── service/                    # 負責身分驗證邏輯、TOTP 註冊快取與密碼雜湊比對
│   │
│   ├── machine/                        # 機台與稼動率管理模組 (Machine Operations)
│   │   ├── controller/                 # 負責提供儀表板所需的機台狀態 API
│   │   ├── dto/                        # 定義 MachineDashboardDto，確保資料回傳格式明確
│   │   └── service/                    # 負責組合機台資料與 WIP 正在處理的晶圓數量
│   │
│   ├── notification/                   # 系統稽核日誌模組 (Audit Trail & Observability)
│   │   ├── controller/                 # 負責提供前端登出時寫入日誌的端點
│   │   ├── dto/                        # 定義 NotificationDto，確保資料回傳格式明確
│   │   ├── entity/                     # 對應 notifications 表格的 JPA 實體
│   │   ├── repository/                 # 負責持久化儲存稽核資料的介面
│   │   └── service/                    # 負責「只進不出 (Append-only)」的日誌寫入邏輯
│   │
│   ├── profile/                        # 使用者設定檔模組 (Profile Management)
│   │   ├── controller/                 # 處理大頭貼上傳、個資修改與密碼變更 API
│   │   └── service/                    # 處理修改信箱與密碼的 OTP 驗證防呆與資料庫更新
│   │
│   ├── shared/                         # 跨模組共用服務 (Shared Utilities)
│   │   └── service/
│   │       └── EmailService.java       # 統一的 SMTP HTML 郵件發送器 (用於 TOTP 驗證)
│   │
│   └── BackendApplication.java         # 程式進入點，啟動 Spring Boot 應用程式
│
├── src/main/resources/
│   └── application.properties          # 系統環境變數設定 (資料庫連線、SMTP、JWT 密鑰)
│
├── src/test/                           # 單元測試與整合測試目錄
│
├── target/                             # Maven 編譯後輸出的 class 檔與 jar 檔目錄 (由 gitignore 排除)
│
├── .gitattributes                      # Git 檔案屬性設定
├── .gitignore                          # 排除不需版控的檔案 (如 target/, .env)
├── mvnw                                # Maven Wrapper 執行檔 (Linux/macOS)
├── mvnw.cmd                            # Maven Wrapper 執行檔 (Windows)
├── pom.xml                             # Maven 依賴套件與建置生命週期設定檔
└── README.md
```

### 核心設計重點

* **三層式架構 (3-Tier Architecture)**：
    * **Controller 層**：僅處理 HTTP 路由與 DTO 參數映射。
    * **Service 層**：封裝核心商業邏輯、TOTP 演算與 Email 發送，確保交易 (Transaction) 完整性。
    * **Repository / Entity 層**：透過 JPA (Hibernate) 管理資料庫綱要與關聯映射。


* **無狀態安全驗證 (Stateless Security)**：徹底捨棄 HTTP Session，改用 JWT 進行路由保護。CORS 嚴格限制僅允許 `http://localhost:5173` 存取。
* **雙重驗證 (2FA) 與資安防護**：整合 Google Authenticator 的 TOTP 機制。任何涉及信箱修改或密碼變更的操作，皆須透過共用的 `EmailService` 寄發一次性驗證碼。
* **自動化資料庫初始化**：透過 `init.sql` 啟動時自動建立表格、導入 RBAC 權限角色、實驗室機台與 60 筆預設 WIP 測試資料。

---

## 啟動指南 (Getting Started)

請確保您的電腦已安裝 **Java 25** 與 **PostgreSQL 15+**，並已建立名為 `lims_db` 的資料庫。

### Step 1: 啟動 Spring Boot 後端伺服器

```bash
cd backend
./mvnw clean spring-boot:run  # macOS / Linux
.\mvnw clean spring-boot:run  # Windows
```

* 伺服器預設將會啟動於 **http://localhost:8080**。
* 啟動時，Hibernate 會自動驗證 Entity 與資料庫欄位型別是否匹配，並確保連線池 (HikariCP) 正常運作。

### Step 2: 執行後端單元測試

在提交程式碼前，請務必執行測試以確保 Controller 與 Service 邏輯正常：

```bash
./mvnw clean test -U  # macOS / Linux
.\mvnw clean test -U  # Windows
```
* **-U** : Update 參數的作用是強制檢查 Snapshot 依賴的更新，在網路環境不穩或依賴頻繁更新時使用是正確的。

---

## 開發規範

* **編碼標準**：為維持企業級專案一致性，所有的程式碼、變數名稱及註解 **統一使用英文 (English)**。
* **資料傳遞 (Data Transfer)**：禁止在 Controller 層直接回傳 `Map<String, Object>` 或資料庫 Entity，必須宣告專屬的 Java Record (DTO) 作為 Payload。
* **依賴注入 (Dependency Injection)**：優先使用 Constructor Injection (建構子注入)，避免使用 `@Autowired` 標註於欄位上，以利單元測試 Mocking。