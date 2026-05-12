FROM python:3.12-alpine

WORKDIR /app

COPY . /app

RUN chmod +x /app/entrypoint.sh

EXPOSE 80

CMD ["/app/entrypoint.sh"]
