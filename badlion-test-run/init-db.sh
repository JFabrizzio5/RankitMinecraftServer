#!/bin/bash
# Script para inicializar bases de datos de Badlion
set -e

echo "========================================="
echo "INICIALIZANDO BASES DE DATOS DE BADLION"
echo "========================================="

echo "[1/2] Esperando a que CouchDB se inicie..."
until docker exec badlion-couchdb curl -s http://admin:admin_pass@127.0.0.1:5984/ > /dev/null; do
  echo "CouchDB no está listo aún - esperando 2 segundos..."
  sleep 2
done

echo "[2/2] Creando base de datos 'badlion' en CouchDB..."
docker exec badlion-couchdb curl -s -X PUT http://admin:admin_pass@127.0.0.1:5984/badlion || echo "La base de datos 'badlion' ya existe o hubo un problema no crítico al crearla."

echo "========================================="
echo "¡INICIALIZACIÓN DE BASES DE DATOS COMPLETADA!"
echo "========================================="
