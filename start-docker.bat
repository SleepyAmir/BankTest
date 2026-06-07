@echo off
echo Starting Docker containers...
docker start banking-monolith-db
docker start banking-transaction-db
docker start banking-analytics-db
docker start banking-fraud-db
docker start banking-audit-db
docker start banking-redis
docker start banking-rabbitmq
echo All containers started!
docker ps
pause