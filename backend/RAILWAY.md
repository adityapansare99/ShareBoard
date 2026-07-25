# Deploying the Planar Backend on Railway

Step-by-step guide to get `com.cb` (Spring Boot 3 / Java 21) running on Railway with a real
Postgres database.

> Read this before clicking "Deploy": the app **defaults to in-memory H2**. If you deploy
> without wiring up Postgres, every redeploy wipes all data. Step 4 is not optional.

---

## 0. Prerequisites

- A Railway account → https://railway.app
- This repo pushed to GitHub **or** the Railway CLI installed (`npm i -g @railway/cli`).
- (For email/OTP login) an SMTP provider — e.g. a Gmail **App Password**.

The backend lives in `backend/` and ships a multi-stage `backend/Dockerfile`
(Maven build → slim JRE runtime, exposes 8080). Railway will build from that.

---

## 1. (Already done) Make the port Railway-aware

`application.yml` uses `server.port: ${PORT:8080}` so the container binds to Railway's
injected `PORT` and falls back to `8080` locally. No action needed — just don't revert it.

---

## 2. Push to GitHub

```bash
git add -A
git commit -m "railway-ready: PORT binding + deploy docs"
git push origin main
```

---

## 3. Create the Railway project

1. https://railway.app → **New Project** → **Deploy from GitHub repo**.
2. Pick this repository.
3. Under **Settings → Source → Root Directory**, set it to **`backend`**
   (so Railway uses `backend/Dockerfile`, not the repo root).
4. Railway starts a build from `Dockerfile`. It will succeed but the service will be
   unhealthy until Postgres + env vars are set — that's expected.

---

## 4. Add Postgres (do NOT skip)

1. In the project → **+ New → Database → Add PostgreSQL**.
2. A `postgres` service appears with auto-generated connection variables
   (`PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`, …).

Railway exposes the URL as `postgresql://…` — but Spring needs the **`jdbc:postgresql://`**
form. So we build `PLANAR_DB_URL` from the individual `PG*` vars (see table below).

---

## 5. Set backend environment variables

On the **backend** service → **Variables → New Variable**. Use Railway's reference syntax
`${{Postgres.VAR}}` so values stay in sync if Postgres rotates.

| Variable             | Value                                                                  |
|----------------------|------------------------------------------------------------------------|
| `PLANAR_DB_URL`      | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` |
| `PLANAR_DB_USERNAME` | `${{Postgres.PGUSER}}`                                                 |
| `PLANAR_DB_PASSWORD` | `${{Postgres.PGPASSWORD}}`                                             |
| `PLANAR_DB_DRIVER`   | `org.postgresql.Driver`                                                |
| `SMTP_HOST`          | `smtp.gmail.com` (or your provider)                                    |
| `SMTP_PORT`          | `587`                                                                  |
| `SMTP_USERNAME`      | your email address                                                     |
| `SMTP_PASSWORD`      | your Gmail **App Password** (not your login password)                 |

> `PLANAR_DB_DRIVER` is what flips the app from H2 (`org.h2.Driver`) to Postgres. Without
> it, the app happily connects to in-memory H2 and silently loses data on every redeploy.

Only set the `SMTP_*` vars if you actually use email/OTP registration; otherwise the mail
bean initializes lazily and login-by-password still works.

---

## 6. Expose a public URL + healthcheck

1. Backend service → **Settings → Networking → Generate Domain**
   → you get `https://<random>.up.railway.app`.
2. **Settings → Healthcheck** → set **Path** to `/` (the healthcheck route added in
   `HealthController`). Railway will wait for a `200` before routing traffic, so deploys
   won't serve a half-started app.

---

## 7. Deploy & verify

Railway redeploys automatically when variables change. Open **Deployments → View Logs** and
wait for `Started PlanarApplication`. Then:

```bash
curl https://<your-domain>.up.railway.app/
# {"status":"UP","service":"planar-backend","timestamp":"..."}
```

That `200` is your green light.

---

## 8. Gotchas (read once, avoid pain)

1. **CORS will block your frontend.** `CorsConfig` only allows `http://localhost:3000`
   and `http://localhost:5173`. Before pointing a hosted frontend at this backend, those
   origins must include the frontend's real URL — otherwise browsers block every request.
   Make it env-driven (e.g. `planar.cors.origins`) and set it on Railway. Ask and I'll wire it.
2. **`ddl-auto: update`** runs Hibernate's schema sync on boot. Fine for a prototype, but
   it can silently drift in prod. Migrate to Flyway/Liquibase before this matters.
3. **`show-sql: true` + `format_sql: true`** is noisy and slow in production logs. Set them
   to `false` (or override via env) once you're past debugging.
4. **Don't commit `backend/.env`.** Locally the `springboot3-dotenv` dependency loads it;
   on Railway all secrets come from the Variables pane. `.gitignore` should already exclude it.
5. **WebSocket (`/ws/**`)** is permitAll and works over Railway's HTTPS/wss automatically —
   no extra config, just use `wss://<your-domain>.up.railway.app/ws` from the client.

---

## Alternative: Railway CLI (no GitHub needed)

```bash
npm i -g @railway/cli
railway login
cd backend
railway init           # link or create a project
railway add            # add the Postgres plugin, then set the vars from Step 5
railway up             # builds backend/Dockerfile and deploys
railway domain         # print / generate the public URL
railway logs           # tail logs
```

Set the same variables from Step 5 via `railway variables set PLANAR_DB_DRIVER=org.postgresql.Driver`.
```
