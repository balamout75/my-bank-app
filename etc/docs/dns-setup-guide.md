# MyBank K8s — Настройка DNS

## Обзор

MyBank использует два уровня DNS:
- **Windows hosts** — чтобы браузер находил `mybank.local` и `keycloak.mybank.local` на `127.0.0.1`
- **CoreDNS rewrite** — чтобы поды внутри кластера находили `keycloak.mybank.local` как внутренний сервис

Без этой настройки OAuth2 не работает: токены выдаются с `issuer: http://keycloak.mybank.local/realms/mybank`, и этот адрес должен резолвиться и снаружи (браузер), и внутри кластера (сервисы).

---

## 1. Windows Hosts файл

### Расположение
```
C:\Windows\System32\drivers\etc\hosts
```

### Редактирование
Откройте Notepad **от администратора** и добавьте строку:
```
127.0.0.1 mybank.local keycloak.mybank.local
```

### Проверка
```bash
ping mybank.local
ping keycloak.mybank.local
```
Оба должны показать `127.0.0.1`.

---

## 2. CoreDNS Rewrite (внутри кластера)

### Зачем
Сервисы (accounts, cash, transfer) обращаются к Keycloak по адресу `http://keycloak.mybank.local/realms/mybank` для валидации токенов. Внутри кластера этот адрес неизвестен — нужно перенаправить его на K8s-сервис `mybank-keycloak`.

### Схема
```
┌─────────────────────────────────────────────────────┐
│  Kubernetes кластер                                 │
│                                                     │
│  Pod (accounts-service)                             │
│    → http://keycloak.mybank.local/realms/mybank     │
│    → CoreDNS rewrite                                │
│    → mybank-keycloak.mybank.svc.cluster.local:80    │
│    → Pod (keycloak) :8080                           │
│                                                     │
└─────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────┐
│  Браузер (Windows)                                  │
│    → http://keycloak.mybank.local                   │
│    → hosts: 127.0.0.1                               │
│    → Ingress-nginx :80                              │
│    → K8s Service (keycloak) :80 → Pod :8080         │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### Настройка

**Шаг 1.** Открыть ConfigMap CoreDNS:
```bash
kubectl edit configmap coredns -n kube-system
```

**Шаг 2.** Добавить строку `rewrite` **перед** блоком `kubernetes`:
```
.:53 {
    errors
    health {
       lameduck 5s
    }
    ready
    rewrite name keycloak.mybank.local mybank-keycloak.mybank.svc.cluster.local
    kubernetes cluster.local in-addr.arpa ip6.arpa {
       pods insecure
       fallthrough in-addr.arpa ip6.arpa
       ttl 30
    }
    prometheus :9153
    forward . /etc/resolv.conf {
       max_concurrent 1000
    }
    cache 30 {
       disable success cluster.local
       disable denial cluster.local
    }
    loop
    reload
    loadbalance
}
```

**Шаг 3.** Сохранить и выйти: `:wq`

**Шаг 4.** Перезапустить CoreDNS:
```bash
kubectl rollout restart deployment coredns -n kube-system
```

### Проверка

```bash
# CoreDNS rewrite на месте?
kubectl get configmap coredns -n kube-system -o yaml | findstr rewrite

# CoreDNS работает?
kubectl get pods -n kube-system | findstr coredns

# DNS резолвится изнутри пода?
kubectl exec -n mybank <любой-под> -- nslookup keycloak.mybank.local

# Keycloak отвечает изнутри кластера?
kubectl exec -n mybank <любой-под> -- curl -s http://keycloak.mybank.local/realms/mybank
```

Ожидаемый результат `nslookup`:
```
Name:   keycloak.mybank.local
Address: <IP пода keycloak>
```

---

## 3. Windows Proxy — исключения

Если используется корпоративный прокси, добавьте в исключения:

**Settings → Сеть → Прокси → Не использовать прокси для:**
```
mybank.local;keycloak.mybank.local;*.mybank.local
```

Или через PowerShell (от администратора):
```powershell
$proxy = Get-ItemProperty -Path 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Internet Settings' -Name ProxyOverride
$newValue = $proxy.ProxyOverride + ";mybank.local;keycloak.mybank.local;*.mybank.local"
Set-ItemProperty -Path 'HKCU:\Software\Microsoft\Windows\CurrentVersion\Internet Settings' -Name ProxyOverride -Value $newValue
```

---

## 4. Почему именно такая схема

### Проблема
OAuth2 токены содержат `iss` (issuer) — адрес, по которому был выдан токен. При валидации сервис обращается по этому адресу для получения JWK-ключей. Адрес должен быть **одинаковым** для браузера и для сервисов, иначе Spring Security откажет в валидации (`issuer mismatch`).

### Варианты решения

| Подход | Плюсы | Минусы |
|--------|-------|--------|
| **CoreDNS rewrite** ✅ | Единый issuer-uri везде, простая конфигурация сервисов | Нужно настраивать CoreDNS |
| Разные internal/external URL | Не трогаем CoreDNS | Сложная конфигурация, разные jwk-set-uri и issuer-uri |
| Keycloak hostname SPI | "Правильный" подход | Сложная настройка Keycloak |

Мы выбрали **CoreDNS rewrite** — минимум конфигурации в сервисах, один `issuer-uri` везде.

---

## 5. Важно помнить

- **После Reset Kubernetes Cluster** CoreDNS сбрасывается — нужно заново добавить rewrite
- **Порядок строк** в Corefile важен — `rewrite` должен быть **перед** `kubernetes`
- **Формат rewrite**: `rewrite name <что> <на что>` — без кавычек, без точек в конце
- Имя K8s-сервиса: `mybank-keycloak.mybank.svc.cluster.local` = `<release>-keycloak.<namespace>.svc.cluster.local`
- Keycloak слушает на порту **8080** внутри пода, но K8s Service маппит **80 → 8080**
