<div align="center">

# 🏦 CryptoBank

### Enterprise Session-Authenticated Banking Platform & Double-Entry Ledger
*Engineered with **Java 21 LTS**, **Spring Boot 3.3.4**, **Spring Security 6**, **Spring Data JPA**, and **PostgreSQL / H2**.*

<br/>

[![Java](https://img.shields.io/badge/Java-21%20LTS-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-Sessions%20%2B%202FA-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![H2 Database](https://img.shields.io/badge/H2-In--Memory%20Dev-1E88E5?style=for-the-badge&logo=h2&logoColor=white)](https://www.h2database.com/)
[![Docker](https://img.shields.io/badge/Docker-Multi--stage%20Build-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)](LICENSE)

<br/>

**[✨ Key Features](#-features)** &nbsp;·&nbsp; **[🏗️ System Architecture](#-system-architecture)** &nbsp;·&nbsp; **[📂 Project Structure](#-project-structure)** &nbsp;·&nbsp; **[📡 REST API Reference](#-rest-api-reference)** &nbsp;·&nbsp; **[🚀 Quick Start](#-getting-started)** &nbsp;·&nbsp; **[🧪 Testing](#-testing)**

</div>

---

## 📌 Overview

**CryptoBank** is a modern, production-ready banking service engineered to eliminate the critical failure points of traditional core banking architectures:

* **No Floating-Point Imprecision**: Currency calculations strictly use `BigDecimal` with 2 decimal places (`precision = 19, scale = 2`) across entities, requests, services, and database columns.
* **Deadlock-Free Concurrent Transfers**: Opposing simultaneous transfers between accounts are row-locked using pessimistic write locks (`PESSIMISTIC_WRITE`) acquired in a strictly deterministic order (lowest account number first), eliminating race conditions and deadlocks.
* **Immutable Double-Entry Ledger**: Every transfer creates paired, balanced `DEBIT` and `CREDIT` ledger entries under a shared transaction reference (`TXN-XXXXXXXX`), recording immutable post-transaction balance snapshots.
* **Zero Secrets in Source**: 100% environment-driven configuration for both development (H2 zero-setup) and production (PostgreSQL connection pool).
* **Multi-Factor Authentication**: Native RFC 6238 TOTP (Time-based One-Time Password) engine enforced on high-value transfers (≥ ₹50,000) and user accounts.
* **Single-Artifact Full-Stack Delivery**: Modern responsive customer dashboard, split-view KYC authentication portal, and administrative console served directly through Spring Boot with persistent Light / Dark / System themes.

---

## ✨ Features

<table>
<tr>
<td width="50%" valign="top">

### 🔐 Accounts & Security
- **Spring Security 6 Session Management** with `HttpOnly` and `SameSite=Lax` cookies.
- **BCrypt Password & PIN Hashing** with unique salts.
- **RFC 6238 TOTP 2FA**: Native HMAC-SHA1 authenticator engine compatible with Google Authenticator, Authy, and 1Password.
- **Server-Side Ownership Verification**: Every account, transfer, card, and bill payment re-validates identity against the session user.
- **KYC Verification**: Verified capture of PAN, date of birth, phone, and residential address.

</td>
<td width="50%" valign="top">

### 💸 Money Movement & Ledger
- **Deterministic Pessimistic Row Locking**: Eliminates concurrency races.
- **Double-Entry Statement Ledger**: Audit-compliant `DEBIT` and `CREDIT` records.
- **Paginated Ledger History** with real-time balance reconciliation.
- **Complete Statement CSV Export**: Streams entire transaction history directly from the ledger.
- **Beneficiary Management**: Save, nickname, and fast-transfer to validated payees.

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 💳 Virtual Debit Cards
- **Lazy Card Issuance**: Dynamically provisioned on first request without manual database migrations.
- **3D Card Rendering**: Interactive live card flip with masked card numbers.
- **Security Toggles**: Real-time server-side controls for Card Freeze, Contactless NFC, and Online E-Commerce payments.
- **Replacement Workflow**: Physical card replacement tracking.

</td>
<td width="50%" valign="top">

### 🧾 Utility Bills & Calculators
- **Utility Bill Pay & Recharge**: Electricity, Mobile Recharge, Water, Gas, Broadband, and DTH debited directly through the ledger.
- **Fixed Deposit (FD) Calculator**: Dynamic interest breakdown and maturity projections.
- **Recurring Deposit (RD) Calculator**: Compounding calculation with visual percentage split.
- **Loan EMI Calculator**: Principal vs. interest amortization breakdown.

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 🛡️ Administration & Helpdesk
- **Role-Based Admin Console** (`/admin.html`) protected by `@PreAuthorize("hasRole('ADMIN')")`.
- **Global User & Account Directory**: Real-time view of customer accounts and total balances.
- **Fraud Hold Action**: Instant administrative Freeze / Unfreeze capability.
- **Support Ticket Triage**: Customer inquiries tracked with lifecycle statuses (`OPEN` / `CLOSED`).

</td>
<td width="50%" valign="top">

### 🎨 Design & Experience
- **Curated Design Tokens**: Tailored HSL color palette, glassmorphism, and micro-animations.
- **Zero-Flash Theme Switcher**: Pre-paint theme application for Light, Dark, and System modes.
- **Interactive OpenAPI 3.0 / Swagger UI** at `/swagger-ui.html`.
- **Spring Boot Actuator** health monitoring at `/actuator/health`.

</td>
</tr>
</table>

---

## 🏗️ System Architecture

```
                       ┌─────────────────────────────────────────┐
                       │           Client Web Browser            │
                       │  (Vanilla ES6 SPA · Light/Dark Tokens)   │
                       └────────────────────┬────────────────────┘
                                            │ HTTP / JSON (Session Cookie)
                                            ▼
                       ┌─────────────────────────────────────────┐
                       │          Security Filter Chain          │
                       │  (CSRF Guard · SessionAuth · CSP · RBAC)│
                       └────────────────────┬────────────────────┘
                                            │
                                            ▼
                       ┌─────────────────────────────────────────┐
                       │          REST API Controllers           │
                       │  (AuthController · AccountController …)  │
                       └────────────────────┬────────────────────┘
                                            │ Request DTOs (Java 21 Records)
                                            ▼
                       ┌─────────────────────────────────────────┐
                       │             Service Layer               │
                       │  (Pessimistic Locking · Double Entry    │
                       │   TOTP Validation · PIN Verification)   │
                       └────────────────────┬────────────────────┘
                                            │
                                            ▼
                       ┌─────────────────────────────────────────┐
                       │       Spring Data JPA Repositories      │
                       │  (Query Methods · PESSIMISTIC_WRITE)    │
                       └────────────────────┬────────────────────┘
                                            │ Hibernate ORM / JDBC
                                            ▼
                       ┌─────────────────────────────────────────┐
                       │               Database                  │
                       │     H2 (Dev)  ·  PostgreSQL (Prod)      │
                       └─────────────────────────────────────────┘
```

---

## 📂 Project Structure

```
e:/CryptoBank/
├── pom.xml                                   # Spring Boot 3.3.4, Java 21, JPA, Security, Actuator, OpenAPI
├── mvnw / mvnw.cmd / .mvn/                   # Maven 3.9.9 wrapper
├── Dockerfile                                # Multi-stage Eclipse Temurin 21 build
├── docker-compose.yml                        # App + PostgreSQL container orchestration
├── .env.example                              # Environment configuration template
│
├── src/main/resources/
│   ├── application.properties                # Dev profile (H2 in-memory DB, auto-DDL)
│   ├── application-prod.properties           # Prod profile (PostgreSQL connection pool)
│   └── static/                               # Customer & Admin Web Frontend
│       ├── index.html                        # NetBanking dashboard (8 main modules)
│       ├── login.html                        # Split-view login & 4-step KYC registration
│       ├── admin.html                        # Platform administration console
│       ├── css/
│       │   ├── bank.css                      # Design tokens, themes, modals, cards
│       │   └── admin.css                     # Admin console grid and tables
│       ├── js/
│       │   ├── theme-init.js                 # Pre-paint theme switcher (prevents flash)
│       │   ├── auth.js                       # Client-side session and auth validation
│       │   ├── app.js                        # Dashboard logic, calculators, transfers, cards
│       │   └── admin.js                      # Admin console controller
│       └── assets/                           # Vector brand assets and favicons
│
└── src/main/java/com/cryptobank/
    ├── CryptoBankApplication.java            # Spring Boot application bootstrap entry point
    │
    ├── config/                               # Configuration beans
    │   ├── JpaAuditingConfig.java            # JPA auditing (@CreatedDate, @LastModifiedDate)
    │   ├── OpenApiConfig.java                # OpenAPI 3.0 / Swagger documentation metadata
    │   └── DataSeederConfig.java             # Database seeder initializing demo customer & admin
    │
    ├── domain/                               # Domain entities & enums
    │   ├── entity/                           # JPA Entities
    │   │   ├── BaseEntity.java               # MappedSuperclass with ID, createdAt, updatedAt
    │   │   ├── UserEntity.java               # Customer & Admin user model
    │   │   ├── AccountEntity.java            # Bank account with BigDecimal balance and PIN
    │   │   ├── LedgerEntryEntity.java        # Immutable double-entry leg (DEBIT/CREDIT)
    │   │   ├── BeneficiaryEntity.java        # Saved transfer payees
    │   │   ├── CardEntity.java               # Virtual debit card with toggle controls
    │   │   ├── BillPaymentEntity.java        # Utility bill payment record
    │   │   └── SupportTicketEntity.java      # Customer support ticket
    │   └── enums/                            # Domain enumerations
    │       ├── UserRole.java                 # ROLE_CUSTOMER, ROLE_ADMIN
    │       ├── LedgerType.java               # CREDIT, DEBIT
    │       └── TicketStatus.java             # OPEN, CLOSED
    │
    ├── repository/                           # Spring Data JPA repositories
    │   ├── UserRepository.java               # Unique email queries
    │   ├── AccountRepository.java            # Queries + PESSIMISTIC_WRITE row locking
    │   ├── LedgerEntryRepository.java        # Statement pagination and full history export
    │   ├── BeneficiaryRepository.java        # Saved payees per owner
    │   ├── CardRepository.java               # Card resolution by account
    │   ├── BillPaymentRepository.java        # Bill payment history
    │   └── SupportTicketRepository.java      # Ticket queries
    │
    ├── service/                              # Business service interfaces & implementations
    │   ├── AccountService.java               # Core account, transfer, and ledger engine
    │   ├── UserService.java                  # Registration and profile services
    │   ├── BeneficiaryService.java           # Payee management
    │   ├── CardService.java                  # Lazy debit card issuance & control updates
    │   ├── BillPayService.java               # Utility billing coordination
    │   ├── SupportService.java               # Ticket lifecycle management
    │   └── impl/                             # Service implementations
    │
    ├── web/                                  # Web layer
    │   ├── controller/                       # REST API controllers
    │   │   ├── AuthController.java           # Authentication, session login, MFA, profile
    │   │   ├── AccountController.java        # Account opening, credit, debit, transfer, CSV
    │   │   ├── BeneficiaryController.java    # Beneficiary CRUD
    │   │   ├── CardController.java           # Virtual card list and toggle updates
    │   │   ├── BillPayController.java        # Utility bill payment processing
    │   │   ├── SupportTicketController.java  # Support tickets
    │   │   ├── TwoFactorController.java      # TOTP 2FA setup, enable, and disable
    │   │   └── AdminController.java          # Admin operations (freeze, triage)
    │   ├── dto/                              # Immutable Java 21 Records
    │   │   ├── request/                      # Request payloads with Jakarta validation
    │   │   └── response/                     # Response payloads (User, Account, Ledger, etc.)
    │   └── exception/                        # REST Exception Handling
    │       ├── ApiException.java             # Domain exception
    │       ├── ErrorResponse.java            # Standardized RFC 7807 error format
    │       └── GlobalExceptionHandler.java   # Centralized @RestControllerAdvice
    │
    ├── security/                             # Spring Security 6 infrastructure
    │   ├── SecurityConfig.java               # Filter chain, session cookie, CSP, RBAC
    │   ├── AppUserDetailsService.java        # UserDetailsService implementation
    │   ├── SecurityUtils.java                # Current authenticated user resolution
    │   └── TotpService.java                  # RFC 6238 HMAC-SHA1 2FA algorithm
    │
    └── util/                                 # Cryptographic & reference generators
        ├── AccountNumberGenerator.java       # Collision-free 8-digit account numbers
        └── ReferenceGenerator.java           # Standard transaction references (TXN-XXXXXXXX)
```

---

## 📡 REST API Reference

All endpoints under `/api/**` (except registration, login, and public static assets) require an authenticated session.

| Method | Endpoint | Access | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | Public | Register a new customer account with verified KYC |
| `POST` | `/api/auth/login` | Public | Authenticate user credentials, initiate session or prompt MFA |
| `POST` | `/api/auth/login/2fa` | Public | Complete session login with 6-digit TOTP code |
| `POST` | `/api/auth/logout` | Authenticated | Terminate session and invalidate cookie |
| `GET` | `/api/auth/me` | Authenticated | Retrieve authenticated user profile |
| `POST` | `/api/auth/change-password` | Authenticated | Update user password verified against current hash |
| `GET` | `/api/accounts` | Customer | List all accounts owned by current user |
| `POST` | `/api/accounts` | Customer | Open a new bank account with PIN |
| `GET` | `/api/accounts/{number}` | Customer | Get account details (ownership enforced) |
| `GET` | `/api/accounts/{number}/history` | Customer | Get paginated double-entry ledger statement |
| `GET` | `/api/accounts/{number}/statement.csv`| Customer | Export complete unpaginated statement as CSV |
| `POST` | `/api/accounts/credit` | Customer | Deposit funds (verified by PIN) |
| `POST` | `/api/accounts/debit` | Customer | Withdraw funds (verified by PIN) |
| `POST` | `/api/accounts/transfer` | Customer | Execute atomic, row-locked inter-account transfer |
| `GET` | `/api/beneficiaries` | Customer | List saved transfer payees |
| `POST` | `/api/beneficiaries` | Customer | Save a new transfer payee |
| `DELETE`| `/api/beneficiaries/{id}` | Customer | Delete a saved payee |
| `GET` | `/api/cards` | Customer | List virtual debit cards (lazily issued) |
| `PATCH`| `/api/cards/{accountNumber}` | Customer | Update card toggles (freeze, contactless, online) |
| `POST` | `/api/cards/{accountNumber}/request-replacement` | Customer | Submit physical card replacement request |
| `GET` | `/api/billpay/history` | Customer | Paginated utility bill payment history |
| `POST` | `/api/billpay` | Customer | Pay utility bill (Electricity, Mobile, Gas, etc.) |
| `GET` | `/api/support/tickets` | Customer | List customer support tickets |
| `POST` | `/api/support/tickets` | Customer | Raise a new support inquiry |
| `GET` | `/api/2fa/status` | Customer | Query TOTP 2FA activation status |
| `POST` | `/api/2fa/setup` | Customer | Generate fresh TOTP secret and otpauth URI |
| `POST` | `/api/2fa/enable` | Customer | Verify code and activate TOTP 2FA |
| `POST` | `/api/2fa/disable` | Customer | Deactivate TOTP 2FA (password + code required) |
| `GET` | `/api/admin/users` | Admin | Directory of all users and account balances |
| `POST` | `/api/admin/accounts/{number}/freeze` | Admin | Place administrative fraud hold on an account |
| `POST` | `/api/admin/accounts/{number}/unfreeze` | Admin | Lift administrative fraud hold on an account |
| `GET` | `/api/admin/tickets` | Admin | List all support tickets across the platform |
| `POST` | `/api/admin/tickets/{id}/close` | Admin | Mark customer inquiry ticket as closed |

📘 **Full interactive documentation, request/response schemas, and a test console are available at `/swagger-ui.html`.**

---

## 🚀 Getting Started

### Prerequisites
- **Java 21 LTS** (Installed or configured in `PATH`).
- Maven ships via the included standalone wrapper (`mvnw.cmd` / `mvnw`).

### Run Locally (Dev Profile — Zero Setup with H2)

```powershell
# Navigate to repository root
cd e:\CryptoBank

# Point to Java 21 LTS (if not in PATH)
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.12.101-hotspot"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path

# Start application
.\mvnw.cmd spring-boot:run
```

Once started, access in your browser:
* 🏦 **Customer NetBanking Dashboard**: [http://localhost:8080/](http://localhost:8080/)
* 🔐 **Sign In / Registration Portal**: [http://localhost:8080/login.html](http://localhost:8080/login.html)
* 🛠️ **Admin Management Console**: [http://localhost:8080/admin.html](http://localhost:8080/admin.html)
* 📘 **Swagger UI API Documentation**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
* 💓 **Health Endpoint**: [http://localhost:8080/actuator/health](http://localhost:8080/actuator/health)
* 🗄️ **H2 Database Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:cryptobank`)

---

## 🔑 Pre-Seeded Demo Accounts

The application automatically seeds demonstration accounts on first boot:

| Role | Email | Password | Details |
|---|---|---|---|
| **Customer** | `demo@bank.app` | `demo12345` | Two accounts (Primary: ₹50,000.00, Savings: ₹12,500.00), PIN: `1234` |
| **Secondary Customer** | `priya@example.com` | `password123` | Pre-saved beneficiary for Demo User, PIN: `5678` |
| **Administrator** | `admin@bank.app` | `admin12345` | Unlocks the `/admin.html` console to freeze accounts and triage tickets |

---

## 🐳 Docker Deployment

To build and run the full stack with PostgreSQL using Docker Compose:

```bash
docker compose up --build -d
```

This starts:
1. `cryptobank-db`: PostgreSQL 16 Alpine container with persistent volume.
2. `cryptobank-app`: Multi-stage Eclipse Temurin 21 container running CryptoBank on port 8080.

---

## 🧪 Testing

Execute the automated integration test suite:

```powershell
.\mvnw.cmd clean test
```

`BankingIntegrationTest` covers full-context MockMvc verification:
- ✅ User registration and authenticated session establishment
- ✅ Account opening with initial balance and PIN encryption
- ✅ Cash deposit and withdrawal ledger verification
- ✅ Atomic two-party transfer with dual-leg ledger validation
- ✅ Insufficient balance rejection (HTTP 400)
- ✅ Incorrect security PIN rejection (HTTP 403)
- ✅ Unauthenticated endpoint rejection (HTTP 401)
- ✅ Cross-tenant unauthorized account access rejection (HTTP 403)
- ✅ Administrative endpoint role guards (`ROLE_ADMIN`)

---

## 👤 Author

**Suraj Kumar**
* GitHub: [@surajkumar11292](https://github.com/surajkumar11292)
* Repository: [https://github.com/surajkumar11292/CryptoBank](https://github.com/surajkumar11292/CryptoBank)

---

## 📄 License

This project is open-source and distributed under the **MIT License**.
