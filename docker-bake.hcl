group "default" {
  targets = [
    "accounts-service",
    "cash-service",
    "transfer-service",
    "notifications-service",
    "gateway-service",
    "front-ui"
  ]
}

variable "REGISTRY" {
  default = "mybank"
}

# Локальный кэш билдкита (быстро и удобно)
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

target "front-ui" {
  inherits = ["_common"]
  args = { MODULE = "front-ui" }
  tags = ["${REGISTRY}/yp-mybank-frontend:latest"]
}
