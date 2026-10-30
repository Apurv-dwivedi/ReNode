<div align="center">
  <img src="renode.webp" alt="ReNode Logo" width="80" height="80" style="border-radius: 16px; box-shadow: 0 4px 20px rgba(56, 189, 248, 0.3);">
  <h1 style="font-family: 'Montserrat', sans-serif; font-weight: 800; background: linear-gradient(135deg, #38bdf8, #10b981); -webkit-background-clip: text; -webkit-text-fill-color: transparent;">ReNode</h1>
  <p><strong>Full-Stack B2B Industrial Waste Exchange & Circular Economy Platform</strong></p>
</div>

---

## 🚀 Overview

**ReNode** is a robust, full-stack B2B platform designed to accelerate the industrial circular economy. It enables manufacturing units, recycling plants, and industrial enterprises to exchange, buy, and sell industrial by-products and waste materials securely, efficiently, and transparently.

Equipped with smart spatial matching, real-time interactive mapping, an asynchronous email outbox notification system, and an integrated deal negotiation module, ReNode bridges the gap between industrial waste generators and resource demand partners.

---

## 🛠️ Tech Stack

* **Backend:** Spring Boot (Java), Spring Data JPA, Hibernate, RESTful APIs
* **Database:** Relational Database (MySQL / PostgreSQL / H2 compatible)
* **Frontend:** HTML5, CSS3, Modern JavaScript (ES6+), Tailwind/Glassmorphism UI
* **Mapping & GIS:** Leaflet.js, OpenStreetMap API
* **Visualizations & Reports:** Chart.js, html2pdf.js
* **Security & Authentication:** JWT (JSON Web Tokens), BCrypt Password Hashing

---

## ✨ Core Features

1. **Secure Authentication & Management:**
   - Company registration with verification compliance document uploads.
   - Secure login and profile settings with outbox pattern email notifications.
2. **Dynamic Inventory Management:**
   - Dual-intent listings: **List Available Waste (Supply)** vs. **List Material Requirement (Demand)**.
   - Comprehensive tracking of quantity (Metric Tons), target pricing, purity specs, and custom location tags.
3. **Smart Spatial Matching Engine:**
   - Automatically computes proximity and material compatibility between regional industrial partners using configurable radius filters (50km to 500km).
4. **Interactive Network Map:**
   - GIS-powered Leaflet map displaying real-time verified facility nodes across industrial corridors.
5. **Trade Request & Action Center Pipeline:**
   - End-to-end deal lifecycle management (Pending $\rightarrow$ Accepted / Declined $\rightarrow$ Completed Verification).
6. **Direct Deal Messaging & Notes:**
   - Real-time inquiry and term negotiation threads embedded directly within trade requests.

---

## 📂 Project Structure

```text
ReNode/
├── backend/
│   ├── src/main/java/com/renode/
│   │   ├── controller/     # REST Endpoints (Auth, Companies, Requests, Resources)
│   │   ├── entity/         # JPA Models (Company, ResourceListing, TradeRequest, TradeMessage)
│   │   ├── repository/     # Data Access Repositories
│   │   └── service/        # Business Logic & Outbox Email Services
│   └── src/main/resources/ # Application properties & configurations
└── frontend/
    ├── index.html          # Landing & Marketing page
    ├── login.html          # Authentication portals
    ├── dashboard.html      # Central Command Workspace, Map, & Inventory
    └── images/             # Assets and branding logos
```

---

## ⚙️ Getting Started & Installation

### Prerequisites
* Java Development Kit (JDK 17+)
* Maven or Gradle
* A modern Web Browser

### 1. Backend Setup
1. Clone the repository and navigate to the backend folder.
2. Configure your database credentials in `src/main/resources/application.properties`.
3. Run the Spring Boot application:
   ```bash
   mvn spring-boot:run
   ```
   *(The backend server will start on `http://localhost:8080`).*

### 2. Frontend Setup
1. Open the `frontend/` directory.
2. Serve `index.html` via a local live server (e.g., Live Server extension in VS Code) or open it directly in your browser.

---

## 🤝 Contributing
Contributions, bug reports, and feature requests are welcome! Feel free to open an issue or submit a pull request.

---

## 📄 License
This project is developed as an enterprise MVP for sustainable industrial resource management.