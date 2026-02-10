group "default" {
  targets = [
    "accounts-service",
    "cash-service",
    "transfer-service",
    "notifications-service",
    "gateway-service",
    "config-service",
    "discovery-service",
    "front-ui"
  ]
}

variable "REGISTRY" {
  default = "mybank"
}

# Локальный кэш билдкита (быстро и удобно)
# Можно заменить на registry cache, если хочешь делиться кэшем в CI.
target "_common" {
  context    = "."
  dockerfile = "Dockerfile.build"

}

target "accounts-service" {
  inherits = ["_common"]
  args = { MODULE = "accounts-service" }
  tags = ["${REGISTRY}/yp-mybank-accounts:latest"]
}

target "cash-service" {
  inherits = ["_common"]
  args = { MODULE = "cash-service" }
  tags = ["${REGISTRY}/yp-mybank-cash:latest"]
}

target "transfer-service" {
  inherits = ["_common"]
  args = { MODULE = "transfer-service" }
  tags = ["${REGISTRY}/yp-mybank-transfer:latest"]
}

target "notifications-service" {
  inherits = ["_common"]
  args = { MODULE = "notifications-service" }
  tags = ["${REGISTRY}/yp-mybank-notifications:latest"]
}

target "gateway-service" {
  inherits = ["_common"]
  args = { MODULE = "gateway-service" }
  tags = ["${REGISTRY}/yp-mybank-gateway:latest"]
}

target "config-service" {
  inherits = ["_common"]
  args = { MODULE = "config-service" }
  tags = ["${REGISTRY}/yp-mybank-config:latest"]
}

target "discovery-service" {
  inherits = ["_common"]
  args = { MODULE = "discovery-service" }
  tags = ["${REGISTRY}/yp-mybank-discovery:latest"]
}

target "front-ui" {
  inherits = ["_common"]
  args = { MODULE = "front-ui" }
  tags = ["${REGISTRY}/yp-mybank-frontend:latest"]
}
