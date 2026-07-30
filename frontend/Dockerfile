FROM public.ecr.aws/docker/library/python:3.12-slim

WORKDIR /app

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# API_BASE_URL is injected at run time (docker-compose env, or the
# cloud provider's env config) — defaults to localhost for bare
# `docker run` testing, but Compose will override it to the backend
# service name (see D5 in the Plan of Attack).
ENV API_BASE_URL=http://localhost:8080

EXPOSE 8501
ENTRYPOINT ["streamlit", "run", "app.py", "--server.address=0.0.0.0", "--server.port=8501"]
