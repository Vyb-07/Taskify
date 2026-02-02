# Taskify: Design & Decision Records

This document outlines the architectural and product design decisions made during the development of Taskify. It serves as a guide for maintainers to understand the underlying logic, trade-offs, and constraints that shaped the current system.

## 1. Architectural Decisions

### A. URL-Based API Versioning
**Problem**: As the API evolves, breaking changes are inevitable. We need a strategy to support multiple versions of the API simultaneously without confusing clients or complicating server-side routing.

**Decision**: URL-based versioning (e.g., `/api/v1/**`).

**Reasoning**:
- **Clarity and Debuggability**: The version is explicitly visible in browser logs, proxy logs, and network traces. This makes it trivial to identify which version a client is using without inspecting headers.
- **Client Simplicity**: Many client-side developers find it easier to manage a base URL than to correctly configure custom HTTP headers or Media Type negotiation for every request.
- **Caching**: Downstream caches (CDNs, proxies) often vary responses by URL but may ignore headers, making URL versioning more reliable for performance.

**Alternatives Considered**:
- **Header-Based Versioning**: Rejected because it complicates debugging and can break through certain proxies that strip custom headers.
- **Media-Type Versioning (Content Negotiation)**: Rejected as it increases complexity for clients and makes the API less discoverable via simple tools like a browser or `curl`.

**Future Evolution**: If the version overhead becomes excessive (e.g., maintaining v1 through v10), we may shift to a "latest" alias with a sunset period for older versions.

---

### B. Optimistic Locking (Versioning)
**Problem**: Simultaneous updates to the same task by different users or devices can lead to the "lost update" problem, where the last write wins and overwrites intermediate data.

**Decision**: Optimistic locking using a `@Version` field.

**Reasoning**:
- **Concurrency Profile**: Task management is typically a low-contention environment. It is rare for two humans to edit the exact same field at the exact same millisecond. Optimistic locking provides high performance by avoiding database locks during the read-modify-write cycle.
- **User Experience**: We catch conflicts at the application level and return a clear 409 Conflict error, allowing the client to decide how to merge or notify the user.

**Alternatives Considered**:
- **Pessimistic Locking**: Rejected because it holds locks on the database for the duration of the transaction. In a web environment where a user might stay on an "Edit" screen indefinitely, this could lead to deadlocks or connection pool exhaustion.

**When this might change**: If Taskify evolves into a collaborative real-time editor (like a shared whiteboard) where contention is high, we might consider a Operational Transformation (OT) or CRDT approach rather than simple locking.

---

### C. Idempotency on POST Operations
**Problem**: Network instability can cause clients to retry requests. If a `POST /tasks` request is retried, the system might create duplicate tasks, leading to data corruption and user frustration.

**Decision**: Persisted idempotency keys specifically for POST operations.

**Reasoning**:
- **Non-Idempotent by Nature**: By HTTP definition, POST is not naturally idempotent. We must explicitly manage state to ensure safety.
- **Complexity Management**: PUT and DELETE are already idempotent by definition (updating or deleting the same resource multiple times results in the same final state). Adding custom idempotency logic to those operations adds unnecessary complexity to the codebase and database.
- **Scope Control**: By focusing only on POST, we solve 90% of duplication risks with 10% of the implementation effort.

**Future Evolution**: If we introduce complex PUT operations that trigger significant side effects (like charging a credit card or sending emails), we may expand the idempotency filter to those specific endpoints.

---

## 2. Product & UX Decisions

### D. Focus Mode over Reminders/Notifications
**Problem**: Traditional task managers rely on push notifications and reminders to drive engagement. However, "notification fatigue" often leads users to ignore or disable these alerts, decreasing long-term utility.

**Decision**: Focused decision support ("Focus Mode") surfacing the top 5 urgent/prioritized tasks.

**Reasoning**:
- **Decision Fatigue**: Users often have 50+ tasks but don't know where to start. Reducing the choice set to 5 "high-signal" items helps users overcome paralysis.
- **Pull vs. Push**: By choosing to enter "Focus Mode," the user is mentally prepared to work. This is more effective than an intrusive notification that might interrupt their existing flow.
- **Intentionality**: Surfaces priority based on objective signals (due date, stagnation, priority level) rather than loud interruptions.

**Future Evolution**: We might introduce "passive" notifications (like a daily summary email) if users report that they forget to check the app entirely, but the core focus will remain on decision support.

---

### E. Intent Buckets over Tags/Projects
**Problem**: Users need a way to group tasks, but traditional "Projects" feel like heavy units of execution, while "Tags" often become a cluttered mess with no clear hierarchy.

**Decision**: Intent Buckets (Work Themes) limited to zero or one per task.

**Reasoning**:
- **Purpose and Attention**: Intent Buckets represent high-level themes (Work, Health, Personal). A task usually serves one primary purpose at a time.
- **Aggregation Clarity**: By limiting a task to one bucket, we can provide clean, non-overlapping analytics on where a user is spending their time and attention.
- **Reflection Support**: This supports our "Weekly Review" and "Stagnation" features by providing a structured lens through which to view behavioral patterns.

**Alternatives Considered**:
- **Projects**: Rejected for now because "Projects" usually imply a finish line and sub-tasks, which adds more complexity than the current "Attention Management" goal requires.
- **Tags (Many-to-Many)**: Rejected because many-to-many relationships make aggregation and UX-driven insights much more difficult to visualize clearly.

**Future Evolution**: If the system moves toward complex project management (with Gantt charts or dependencies), we would likely introduce Projects as a separate entity that *resides within* an Intent Bucket.

---

## 3. Constraints & Philosophy

### Feature Restraint
Taskify prioritizes "Backend UX Intelligence" over "Frontend Bells & Whistles." We deliberately avoid:
- **ML/AI Scoring**: Until we have enough data for accuracy, manual priority and time-based signals (stagnation) are more trustworthy.
- **Gamification**: We believe productivity is about focus and reflection, not earning points or badges that can lead to "gaming the system."
- **Social/Collab**: The current focus is on individual effectiveness.

### Philosophy of Evolution
The system is built on **Separation of Concerns**. Business logic is isolated from security and versioning, allowing us to:
- Swap out the persistence layer (MySQL) if performance needs change.
- Retire API versions safely without touching core service logic.
- Add new UX-driven "decision support" features by leveraging the existing JPA Specification engine.
