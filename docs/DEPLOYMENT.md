# AutoScientist AI — Production Deployment Guide

## 1. System Requirements & Architecture
- **Operating System**: Linux (Ubuntu 22.04 LTS / Debian 12 / RHEL 9), macOS, Windows WSL2.
- **Java Runtime**: JDK 17 or JDK 21 (OpenJDK / Eclipse Temurin).
- **Node.js**: Node 18.x or 20.x LTS + npm 9+.
- **Database**: MySQL 8.0+ (Production) or in-memory H2 (Dev / Testing fallback).
- **Memory**: Minimum 2 GB RAM (4 GB recommended for large ML models).
- **Disk**: 1 GB free disk space.

---

## 2. Environment Variables Configuration

Create `.env` in `backend/` and `frontend/` by copying the provided templates:

### Backend Configuration (`backend/.env`)
```bash
# Server Port
PORT=8080

# Database Configuration (MySQL 8.0+)
SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/autoscientist_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
SPRING_DATASOURCE_USERNAME=autoscientist_user
SPRING_DATASOURCE_PASSWORD=YourSecureProductionPassword123!

# JWT Security Secret (HS512 Key, at least 64 characters)
JWT_SECRET=super_secret_production_key_for_autoscientist_ai_jwt_security_token_generation_2026_spring_boot
JWT_EXPIRATION_MS=86400000

# File Upload Storage Directory
APP_UPLOAD_DIR=./uploads/datasets
```

### Frontend Configuration (`frontend/.env`)
```bash
VITE_API_BASE_URL=http://localhost:8080/api
```

---

## 3. Database Initialization (MySQL)

Log in to MySQL as root and initialize the schema:
```bash
mysql -u root -p < database/schema.sql
```
Create the application database user:
```sql
CREATE USER IF NOT EXISTS 'autoscientist_user'@'localhost' IDENTIFIED BY 'YourSecureProductionPassword123!';
GRANT ALL PRIVILEGES ON autoscientist_db.* TO 'autoscientist_user'@'localhost';
FLUSH PRIVILEGES;
```

---

## 4. Production Build & Packaging

### 4.1 Packaging Spring Boot Jar
From the repository root:
```bash
cd backend
./mvnw clean package -DskipTests
```
The resulting executable JAR will be generated at:
`backend/target/autoscientist-backend-1.0.0-SNAPSHOT.jar`

### 4.2 Building React Production Bundle
From the repository root:
```bash
cd frontend
npm install
npm run build
```
Static production assets are compiled to `frontend/dist/`.

---

## 5. Running the Application

### 5.1 Starting Backend
Using the provided launcher script:
```bash
./scripts/run_backend.sh
```
Or directly via Java:
```bash
java -jar backend/target/autoscientist-backend-1.0.0-SNAPSHOT.jar
```

### 5.2 Starting Frontend
For development/preview mode:
```bash
./scripts/run_frontend.sh
```
Or for production hosting, serve `frontend/dist` with Nginx or a static HTTP server.

---

## 6. Nginx Reverse Proxy Production Setup (Recommended)

```nginx
server {
    listen 80;
    server_name autoscientist.yourdomain.com;

    # Frontend Static Files
    location / {
        root /Applications/AutoScientist\ AI/frontend/dist;
        try_files $uri $uri/ /index.html;
    }

    # Backend API Proxy
    location /api/ {
        proxy_pass http://127.0.0.1:8080/api/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```
