#!/bin/bash
cd "$(dirname "$0")"

echo "Stopping the till - this runs the shutdown backup."

if curl -s -X POST http://localhost:8080/api/system/shutdown >/dev/null; then
  sleep 6
  echo "Done. Check the backups folder for a -shutdown.zip"
else
  echo "Could not reach the till. It may already be stopped."
fi

read -n 1 -s -r -p "Press any key to close."
