#!/bin/bash

# Script para generar claves RSA para JWT
# Ejecutar desde la raíz del proyecto

echo "Generando claves RSA para JWT..."

# Crear directorio META-INF si no existe
mkdir -p src/main/resources/META-INF

# Generar clave privada
openssl genrsa -out src/main/resources/META-INF/privateKey.pem 2048

# Generar clave pública desde la privada
openssl rsa -in src/main/resources/META-INF/privateKey.pem -pubout -out src/main/resources/META-INF/publicKey.pem

echo "Claves generadas:"
echo "- Clave privada: src/main/resources/META-INF/privateKey.pem"
echo "- Clave pública: src/main/resources/META-INF/publicKey.pem"

# Mostrar el contenido de las claves (opcional)
echo ""
echo "=== Clave Privada ==="
cat src/main/resources/META-INF/privateKey.pem

echo ""
echo "=== Clave Pública ==="
cat src/main/resources/META-INF/publicKey.pem