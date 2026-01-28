#!/bin/bash

# Скрипт остановки всех сервисов My Bank App

echo "🛑 Stopping My Bank App services..."
echo "====================================="
echo ""

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m' # No Color

# Переходим в корень проекта
cd "$(dirname "$0")"

# Функция остановки сервиса
stop_service() {
    local service_name=$1
    local pid_file="logs/$service_name.pid"
    
    if [ -f "$pid_file" ]; then
        local pid=$(cat "$pid_file")
        echo -e "Stopping $service_name (PID: $pid)..."
        
        if ps -p $pid > /dev/null 2>&1; then
            kill $pid 2>/dev/null
            sleep 2
            
            # Если процесс все еще работает, используем kill -9
            if ps -p $pid > /dev/null 2>&1; then
                echo -e "${RED}[WARN]${NC} Process still running, using force kill..."
                kill -9 $pid 2>/dev/null
            fi
            
            echo -e "${GREEN}[OK]${NC} $service_name stopped"
        else
            echo -e "${RED}[WARN]${NC} Process $pid not found (already stopped?)"
        fi
        
        rm "$pid_file"
    else
        echo -e "${RED}[WARN]${NC} PID file not found for $service_name"
    fi
    echo ""
}

# Останавливаем сервисы в обратном порядке запуска
echo "Stopping services..."
echo ""

stop_service "front-ui"
stop_service "gateway-service"
stop_service "config-service"
stop_service "discovery-service"

echo "====================================="
echo -e "${GREEN}✅ All services stopped${NC}"
echo "====================================="
echo ""

# Очистка логов (опционально)
read -p "Do you want to clean log files? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    rm -f logs/*.log
    echo -e "${GREEN}✅ Log files cleaned${NC}"
fi

echo ""
echo "Done!"
echo ""
