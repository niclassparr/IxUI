# ---- Stage 1: Build the frontend ----------------------------------------
FROM node:20-alpine AS frontend-build

WORKDIR /build/frontend

# Install dependencies first for better layer caching
COPY frontend/package*.json ./
RUN npm ci

# Copy source and build
COPY frontend/ ./
RUN npm run build

# ---- Stage 2: Python runtime serving backend + built frontend ------------
FROM python:3.12-slim

WORKDIR /app

# Install Python dependencies
COPY backend/requirements.txt ./backend/
RUN pip install --no-cache-dir -r backend/requirements.txt

# Copy backend source
COPY backend/ ./backend/

# Copy built frontend assets from Stage 1
COPY --from=frontend-build /build/frontend/dist ./frontend/dist

# Copy static assets (images, etc.)
COPY frontend/static ./frontend/static

EXPOSE 8000

CMD ["python", "-m", "uvicorn", "backend.main:app", "--host", "0.0.0.0", "--port", "8000"]
