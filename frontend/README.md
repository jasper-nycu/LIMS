# LIMS 前端架構 (Frontend Architecture)

本前端專案是 LIMS 雲原生系統的使用者介面，採用 React 18 + Vite + TypeScript 構建，並使用 Tailwind CSS v4 進行樣式開發。我們遵循「關注點分離 (Separation of Concerns)」原則，將介面拆分為佈局框架、頁面視圖與原子組件。

## 專案架構 (Project Structure)

```text
frontend/
├── index.html                    # 網頁的 Metadata 以及 React 的掛載點
└── src/
    ├── assets/                   # 靜態資源
    ├── components/layout/        # 全域佈局重複使用組件
    │   ├── __tests__/            # 佈局組件的單元測試 (Header, Sidebar 測試)
    │   ├── Header.tsx            # 頂部導覽列 (語系切換、通知、使用者資訊)
    │   └── Sidebar.tsx           # 側邊導覽選單 (負責視圖導覽與 RWD 縮放)
    │
    ├── views/                        # 頁面視圖
    │   ├── __tests__/                # 頁面邏輯單元測試 (Auth, FabRequest, LabOps 等測試)
    │   ├── AuthView.tsx              # 登入與註冊頁面
    │   ├── FabRequestView.tsx        # 廠區建立委託單
    │   ├── LabOperationsView.tsx     # 實驗室人員操作
    │   ├── ManagerDashboardView.tsx  # 實驗室主管簽核
    │   ├── CapacityAnalyticsView.tsx # 機台分析圖表
    │   └── MyProfileView.tsx         # 個人帳號設定
    │
    ├── test/                     # 測試環境配置
    │   └── setup.ts              # Vitest 環境設定 (引入 jest-dom 擴充)
    │
    ├── App.tsx                   # 應用程式根組件 (負責全域狀態管理與視圖跳轉邏輯)
    ├── main.tsx                  # 程式進入點 (負責將 React 掛載至 HTML DOM)
    └── index.css                 # 全域樣式設定與 Tailwind CSS v4 主題定義
```

### 核心設計重點

* **佈局與視圖分離 (Layout & Views)**：將導覽框架（Header/Sidebar）與業務頁面（Views）解耦，讓開發者能專注於頁面功能開發。
* **有限狀態機 (FSM) 驗證**：針對複雜業務實作嚴謹的狀態轉換邏輯（IDLE, PROCESSING, ALARM, MAINTENANCE）於測試中確保狀態變遷安全性。
* **自動化測試 (Unit Testing)**：在 `components/` 與 `views/` 目錄下均建立 `__tests__` 資料夾。採用動態資料生成，確保測試環境無狀態。

---

## 啟動指南 (Getting Started)

請確保您的電腦已安裝 [Node.js](https://nodejs.org/) 環境，然後依照以下步驟操作：

### Step 1: 安裝依賴套件

```bash
cd frontend
npm install
```

* **npm install**：讀取 `package.json` 並下載專案所需的所有工具（React, Tailwind, Vitest 等）到 `node_modules` 資料夾中。

### Step 2: 啟動 React 前端開發伺服器

```bash
npm run dev
```

* **npm run dev**：Vite 提供「熱更新 (HMR)」功能，修改程式碼後瀏覽器會立即反映變更。
* **預設網址**：**[http://localhost:5173](https://www.google.com/search?q=http://localhost:5173)**。

### Step 3: 執行前端單元測試

在提交程式碼前，請務必執行測試以確保核心邏輯（如 Stateless 顯示、語系切換）正常：

```bash
npm run test
```

---

## 開發規範

* **編碼標準**：為了避免編碼錯誤或亂碼，所有的程式碼、變數名稱及註解 **統一使用英文 (English)**。
* **樣式使用**：採用 Tailwind CSS v4 語法，自定義主題變數（如 `--color-corporate-blue`）定義在 `index.css` 中。
* **元件開發**：優先使用函數式組件 (Functional Components) 與 React Hooks。