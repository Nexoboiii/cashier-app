#!/bin/bash
cd "$(dirname "$0")"

JAR="cashier-app.jar"
[ -f "$JAR" ] || JAR="cashier-app-0.0.1-SNAPSHOT.jar"
[ -f "$JAR" ] || JAR="target/cashier-app-0.0.1-SNAPSHOT.jar"

if [ ! -f "$JAR" ]; then
  echo "Could not find the jar next to this file."
  read -n 1 -s -r -p "Press any key to close."
  exit 1
fi

if ! command -v java >/dev/null 2>&1; then
  echo "Java is not installed. Install Temurin 21 first."
  read -n 1 -s -r -p "Press any key to close."
  exit 1
fi

echo "Starting the till..."
nohup java -jar "$JAR" >/dev/null 2>&1 &

# give spring time to bind the port before chrome asks for it
sleep 8

if [ -d "/Applications/Google Chrome.app" ]; then
  open -na "Google Chrome" --args --app=http://localhost:8080
else
  open http://localhost:8080
fi
