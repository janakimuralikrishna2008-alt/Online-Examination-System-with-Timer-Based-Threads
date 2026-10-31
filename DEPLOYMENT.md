# Deployment Guide: Online Examination System with Timer-Based Threads

This guide provides end-to-end instructions for deploying the **Online Examination System with Timer-Based Threads** to production:
- **Backend API**: Deployed to [Render](https://render.com) (Spring Boot 3 + Java 21 LTS).
- **Frontend SPA**: Deployed to [Cloudflare Pages](https://pages.cloudflare.com) (React 19 + TypeScript + Vite).
- **Database**: Cloud-hosted MySQL 8+ database (Render MySQL, Aiven, PlanetScale, Railway, or TiDB).

---

## 🏗️ Architecture Overview

```
                      +---------------------------------------+
                      |           Cloudflare Pages            |
                      |     (React 19 + Vite Frontend)        |
                      |  https://<your-project>.pages.dev     |
                      +---------------------------------------+
                                          |
                                   HTTPS / REST API
                                (JWT Authentication)
                                          v
                      +---------------------------------------+
                      |             Render.com                |
                      |   (Spring Boot 3 Web Backend API)     |
                      | https://<backend-app>.onrender.com    |
                      +---------------------------------------+
                                          |
                                   JDBC (TLS/SSL)
                                          v
                      +---------------------------------------+
                      |         Cloud MySQL 8.0+              |
                      | (database/schema.sql + migration_v2)  |
                      +---------------------------------------+
```

---

## 📋 Prerequisites

Before deploying, ensure you have:
1. A **GitHub** account containing your repository.
2. A free **[Render.com](https://render.com)** account.
3. A free **[Cloudflare](https://dash.cloudflare.com)** account.
4. A cloud MySQL 8.0+ database instance (e.g. from [Aiven](https://aiven.io), [Railway](https://railway.app), [TiDB Cloud](https://tidbcloud.com), or [Clever Cloud](https://www.clever-cloud.com)).

---

## Step 1: Database Initialization

1. Obtain your cloud MySQL connection parameters:
   - **Host** (e.g. `mysql-xxxx.aivencloud.com`)
   - **Port** (e.g. `3306` or cloud port)
   - **Database Name** (e.g. `online_exam_db`)
   - **Username** (e.g. `avnadmin` or `root`)
   - **Password**

2. Connect to your database using MySQL Workbench, DBeaver, or the MySQL CLI:
   ```bash
   mysql -h <host> -P <port> -u <user> -p <database_name>
   ```

3. Run the database setup scripts in sequence:
   - [`database/schema.sql`](database/schema.sql) — Initializes core tables (`users`, `subjects`, `topics`, `study_materials`, `exams`, `questions`, `student_exams`, `student_answers`).
   - [`database/migration_v2.sql`](database/migration_v2.sql) — Sets up learning platform enhancements and seed data.

---

## Step 2: Backend Deployment on Render

### Option A: 1-Click Blueprint Deployment (Recommended)

1. Log in to [Render Dashboard](https://dashboard.render.com).
2. Click **New +** > **Blueprint**.
3. Connect your GitHub repository.
4. Render detects [`render.yaml`](render.yaml) automatically.
5. In the configuration screen, fill in the environment variables:
   - `DB_URL`: `jdbc:mysql://<host>:<port>/<db>?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC`
   - `DB_USER`: Your database username
   - `DB_PASSWORD`: Your database password
   - `ADMIN_INVITATION_CODE`: A strong private code for administrator registration (e.g. `PROD-ADMIN-2026-X9K2`)
   - `CORS_ALLOWED_ORIGINS`: Your Cloudflare Pages URL (e.g. `https://online-exam-system.pages.dev`)
   - `JWT_SECRET`: Leave as auto-generated (or provide a 32+ character random string)
6. Click **Apply**. Render will build and deploy the Docker container.

---

### Option B: Manual Web Service Deployment on Render

If creating the service manually:

1. Click **New +** > **Web Service**.
2. Connect your GitHub repository.
3. Configure settings:
   - **Name**: `online-examination-backend`
   - **Region**: Choose the region closest to your MySQL database (e.g. *Oregon* or *Frankfurt*).
   - **Environment**: **Docker**
   - **Dockerfile Path**: `Dockerfile`
   - **Branch**: `main` (or your primary branch)
   - **Plan**: **Free** (or Starter)
4. Under **Health Check Path**, enter:
   ```
   /api/health
   ```
5. Under **Environment Variables**, add:

| Variable | Recommended Value / Description | Required |
|---|---|:---:|
| `PORT` | `8080` (Render dynamically routes traffic to this port) | Yes |
| `DB_URL` | `jdbc:mysql://<host>:<port>/<database>?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC` | Yes |
| `DB_USER` | `<cloud_mysql_user>` | Yes |
| `DB_PASSWORD` | `<cloud_mysql_password>` | Yes |
| `JWT_SECRET` | 32+ character random string (e.g. `9f8e7d6c5b4a3120efcdab8967452301fedcba9876543210`) | Yes |
| `ADMIN_INVITATION_CODE` | Secure invitation code required for Admin registration | Yes |
| `CORS_ALLOWED_ORIGINS` | Comma-separated allowed origins (e.g. `https://<your-project>.pages.dev`) | Yes |
| `AI_PROVIDER` | `AUTO` (or `GEMINI`, `MOCK`) | Optional |
| `AI_API_KEY` | Google Gemini API key for AI question generation | Optional |

6. Click **Create Web Service**.
7. Wait for the build to finish. Once live, test health status:
   ```bash
   curl https://<your-backend-app>.onrender.com/api/health
   ```
   Expected response:
   ```json
   {"status":"UP","service":"Online Examination System","database":"UP"}
   ```

---

## Step 3: Frontend Deployment on Cloudflare Pages

1. Log in to the [Cloudflare Dashboard](https://dash.cloudflare.com).
2. Go to **Compute (Workers) > Workers & Pages > Create application > Pages > Connect to Git**.
3. Select your GitHub repository.
4. Configure Build Settings:
   - **Project Name**: `online-examination-system` (or your preferred name)
   - **Production Branch**: `main`
   - **Framework preset**: `Vite`
   - **Root directory**: `web/frontend`
   - **Build command**: `npm run build`
   - **Build output directory**: `dist`
5. Under **Environment variables (Production)**:

| Variable | Value |
|---|---|
| `VITE_API_BASE_URL` | `https://<your-backend-app>.onrender.com/api` |

6. Click **Save and Deploy**.
7. Cloudflare Pages will build the frontend bundle. When complete, your application will be available at:
   ```
   https://<your-project>.pages.dev
   ```

> 💡 **SPA Routing**: The file [`web/frontend/public/_redirects`](web/frontend/public/_redirects) automatically ensures routes like `/register/student`, `/register/admin`, `/student`, and `/admin` redirect cleanly to `/index.html` on browser refresh without returning 404 errors.

---

## Step 4: Verification Checklist

Once both services are deployed:

1. **Visit Frontend URL**: Open `https://<your-project>.pages.dev` in your browser.
2. **Student Registration**:
   - Navigate to `/register/student`.
   - Register a new student account.
   - Verify automatic login and redirection to the Student Dashboard.
3. **Admin Registration**:
   - Navigate to `/register/admin`.
   - Enter your `ADMIN_INVITATION_CODE`.
   - Verify successful creation and access to the Admin Console.
4. **Invalid Invitation Code Verification**:
   - Try registering an admin with an incorrect code.
   - Verify that HTTP 403 Forbidden is returned with the error message.
5. **Subject Management & Safe Deletion**:
   - In Admin Console, create a subject and topic.
   - Delete the subject — verify the modal displays dependent items and `"Delete All (Force)"` safely removes them inside a single transaction.
6. **Take an Examination**:
   - Log in as a student, start an exam, submit, and view scorecard and results.

---

## 🔒 Security Best Practices

- **Never commit secrets**: Keep database passwords, JWT secrets, and invitation codes strictly in Render environment variables.
- **HTTPS Enforcement**: Both Render and Cloudflare Pages enforce SSL/TLS encryption by default.
- **CORS Restriction**: Ensure `CORS_ALLOWED_ORIGINS` contains only your trusted Cloudflare Pages domain in production.
- **Render Free Tier Spin-Down**: Render's free tier services spin down after 15 minutes of inactivity. The first request after idle can take ~30-50 seconds to initialize. Consider upgrading to the Starter tier ($7/mo) for 24/7 instant availability.

