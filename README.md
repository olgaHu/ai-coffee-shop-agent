# AI 咖啡店智能助理（AI Coffee Shop Agent）
咖啡店智能助理 結合 **AI Agent、RAG（Retrieval-Augmented Generation）** 與 **Tool Calling**
的 Side Project。

這是一個展示 **AI Agent 架構設計能力** 的 Side Project。

本專案**刻意先使用 n8n 建立 POC（Proof of Concept）**，
用來快速驗證以下關鍵能力是否可行：

- AI Agent 的意圖判斷能力
- RAG（菜單知識查詢）
- Tool Calling（訂位等實際業務動作）

---

## 🔍 POC 架構總覽（n8n）

> 以下為本專案第一階段（POC）的 AI Agent 實際運作流程

![AI Agent POC Overview](./poc-n8n/n8n-agent-overview.png)

此流程展示：
- 使用者以自然語言互動
- AI Agent 判斷是「查詢」或「動作」
- 查詢走 RAG，動作走 Tool
- 業務邏輯與 AI 推理分離


## 🎯 專案目標

打造一個「**可實際應用、可逐步擴充**」的咖啡店 AI 助理，具備：

- 📖 **菜單查詢 / 問答（RAG）**
- 🪑 **訂位處理（Tool Calling）**
- 🧠 AI Agent 能根據使用者意圖，自行判斷要「回答問題」或「執行動作」

本專案會先以 **n8n 做 POC 驗證**，再 **遷移到 Java Spring Boot 後端實作**。


---

## 🧠 核心概念（Core Concepts）

- **AI Agent**：負責理解使用者意圖與決策
- **RAG**：用向量資料庫查詢咖啡店菜單與知識
- **Tool Calling**：當使用者要訂位時，AI 呼叫實際的業務工具
- **AI 與業務邏輯分離**：方便未來改成正式後端系統

---

## 🛠 技術規劃（Tech Stack）

### Phase 1：POC（快速驗證）
- n8n（AI Agent / RAG / Tool Workflow）
- OpenAI / OpenRouter
- Google Sheets（訂位資料）

### Phase 2：正式後端
- Java 17
- Spring Boot
- RESTful API
- Vector DB（pgvector / Qdrant）

---

## 🗂 專案結構

```text
ai-coffee-shop-agent/
├─ docs/                # 架構與設計說明
├─ poc-n8n/             # n8n POC（截圖、流程）
├─ backend/             # Java Spring Boot（規劃中）
└─ README.md
