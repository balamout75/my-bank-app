#!/bin/bash

# Скрипт запуска My Bank App (БЕЗ Keycloak)
# Запускает инфраструктурные сервисы

echo "🚀 Starting My Bank App (Development Mode - Without Keycloak)"
echo "=============================================================="
echo ""

# Цвета для вывода
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Переходим в корень проекта
cd "$(dirname "$0")"

# Функция запуска сервиса
start_service() {
    local service_name=$1
    local port=$2
    local delay=$3
    
    echo -e "${BLUE}[INFO]${NC} Starting $service_name on port $port..."
    
    cd "$service_name"
    mvn spring-boot:run > "../logs/$service_name.log" 2>&1 &
    echo $! > "../logs/$service_name.pid"
    cd ..
    
    echo -e "${GREEN}[OK]${NC} $service_name started (PID: $(cat logs/$service_name.pid))"
    
    if [ ! -z "$delay" ]; then
        echo -e "${YELLOW}[WAIT]${NC} Waiting $delay seconds for $service_name to initialize..."
        sleep $delay
    fi
    echo ""
}

# Создаем директорию для логов
mkdir -p logs

echo "Step 1/4: Starting Discovery Service (Eureka)"
echo "----------------------------------------------"
start_service "discovery-service" 8761 30

echo "Step 2/4: Starting Config Service"
echo "----------------------------------------------"
start_service "config-service" 8888 15

echo "Step 3/4: Starting Gateway Service"
echo "----------------------------------------------"
start_service "gateway-service" 8090 15

echo "Step 4/4: Starting Front UI"
echo "----------------------------------------------"
start_service "front-ui" 8080 15

echo ""
echo "=============================================================="
echo -e "${GREEN}✅ All services started successfully!${NC}"
echo "=============================================================="
echo ""
echo "📊 Service Status:"
echo "  • Discovery Service (Eureka):  http://localhost:8761"
echo "  • Config Service:              http://localhost:8888/actuator/health"
echo "  • Gateway Service:             http://localhost:8090/actuator/health"
echo "  • Front UI:                    http://localhost:8080"
echo ""
echo "🌐 Open your browser:"
echo "  http://localhost:8080"
echo ""
echo "🔐 Login credentials (development mode):"
echo "  Username: alice"
echo "  Password: password"
echo ""
echo "⚠️  Note: Business services (accounts, cash, transfer) are not started."
echo "   Front UI will show mock data when backend is unavailable."
echo ""
echo "📝 Logs location: ./logs/"
echo ""
echo "🛑 To stop all services, run:"
echo "  ./stop-all.sh"
echo ""
