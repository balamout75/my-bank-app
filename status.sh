#!/bin/bash

# Скрипт проверки статуса сервисов My Bank App

echo "📊 My Bank App - Services Status"
echo "================================="
echo ""

# Цвета для вывода
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Переходим в корень проекта
cd "$(dirname "$0")"

# Функция проверки статуса сервиса
check_service() {
    local service_name=$1
    local port=$2
    local health_url=$3
    local pid_file="logs/$service_name.pid"
    
    echo -n "[$service_name]"
    
    # Проверка PID файла
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        
        # Проверка что процесс запущен
        if ps -p $pid > /dev/null 2>&1; then
            echo -n " PID:$pid "
            
            # Проверка health endpoint
            if [ ! -z "$health_url" ]; then
                local http_code=$(curl -s -o /dev/null -w "%{http_code}" "$health_url" 2>/dev/null)
                
                if [ "$http_code" = "200" ]; then
                    echo -e "${GREEN}✅ RUNNING${NC} - http://localhost:$port"
                else
                    echo -e "${YELLOW}⚠️  STARTING${NC} (HTTP $http_code) - http://localhost:$port"
                fi
            else
                echo -e "${GREEN}✅ RUNNING${NC} - http://localhost:$port"
            fi
        else
            echo -e " ${RED}❌ STOPPED${NC} (PID file exists but process not found)"
        fi
    else
        echo -e " ${RED}❌ STOPPED${NC} (no PID file)"
    fi
}

# Проверяем все сервисы
check_service "discovery-service" 8761 "http://localhost:8761/actuator/health"
check_service "config-service" 8888 "http://localhost:8888/actuator/health"
check_service "gateway-service" 8090 "http://localhost:8090/actuator/health"
check_service "front-ui" 8080 "http://localhost:8080/actuator/health"

echo ""
echo "================================="
echo ""
echo "🌐 Quick Links:"
echo "  • Eureka Dashboard:  http://localhost:8761"
echo "  • Gateway Health:    http://localhost:8090/actuator/health"
echo "  • Front UI:          http://localhost:8080"
echo ""
echo "📝 View logs:"
echo "  tail -f logs/discovery-service.log"
echo "  tail -f logs/config-service.log"
echo "  tail -f logs/gateway-service.log"
echo "  tail -f logs/front-ui.log"
echo ""
