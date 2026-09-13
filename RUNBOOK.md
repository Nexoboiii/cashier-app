# Till — runbook

Print this. Put it in the box with the cables.

Event: **31 October 2026**. One laptop, one operator, no network needed.

---

## Start

Double-click **`start-till.bat`**.

Chrome opens with no address bar. Wait ~8 seconds — if you see a connection
error, the app just hadn't finished starting. Refresh with **F5**.

If it's installed as an app, the desktop/taskbar icon works too, but **the icon
does not start the server**. Run the `.bat` first, always.

## Open the day

**Day** tab → count the cash in the tin → enter it → **Open the day**.

The box is prefilled with the usual float. Change it if you counted something
different. Every reconciliation figure at close derives from this number.

**No day open = no selling.** The till refuses sales until you do this.

## Selling

Hands stay on the keyboard.

| Key | Does |
|---|---|
| `1`–`0` | add the numbered product |
| type letters | narrow the grid, badges renumber |
| `F2` | exact tender |
| `⌫` | undo one (search first, then cart) |
| `Enter` | take cash |
| `⇧Enter` | take card |
| `Esc` | clear (search first, then cart) |

Change is shown in large green type after a cash sale. Read it out.

## Close the day

**Day** tab → count the tin → enter the figure → add a note if the variance
needs explaining → **Close the day**.

Then **Print** (Ctrl+P, destination "Save as PDF" or a real printer). That sheet
is the settlement record — takings, per-supplier breakdown, reconciliation.

A closed day cannot be reopened. Closing also writes a backup.

## Stop

**Close till** in the top right, or **`stop-till.bat`**.

Either one writes a shutdown backup. **Do not kill it from Task Manager** — that
skips the backup and the audit record.

---

## Where things live

```
Desktop\comic-con_stuff\till\
  data\cashier.mv.db      the database - this is the real asset
  backups\*.zip           day close, 23:59:59 nightly, and every shutdown
  logs\cashier.log        everything
  logs\cashier-error.log  only problems - read this one first
```

Settings: **`config\application.properties`** beside the jar. Notepad edits it;
restart to apply. Forward slashes in paths, even on Windows.

---

## If it won't start

1. Open **`logs\cashier-error.log`**, read the last entry.
2. "Port 8080 already in use" → it's already running. Just open
   `http://localhost:8080`.
3. "Wrong user name or password" → the database path in `config\` points
   somewhere it didn't create. Check the path before touching anything else.
4. Still stuck → `java -jar target\cashier-app-0.0.1-SNAPSHOT.jar` in a terminal
   and read what it prints.

## If the screen goes wrong

A crash shows a "Something went wrong" panel with a **Reload** button.
Photograph the message, click Reload. Sales already taken are safe.

A frozen or blank screen: **F5**. The server is a separate process — the
browser can be reloaded as often as you like without losing anything.

## If the database is lost or corrupt

**Fastest recovery — catalogue only:** start the app, **Products** →
**Import CSV** → `catalogue.csv`. Trading again in two minutes. Sales history
is not recovered this way.

**Full restore from backup:**

1. Stop the till.
2. Rename `data\cashier.mv.db` to `cashier.mv.db.broken` — never delete it.
3. Open the newest zip in `backups\`, extract `cashier.mv.db` into `data\`.
4. Start the till. You have lost only what happened after that backup.

Backups run at day close, at 23:59:59, and on every clean shutdown.

---

## Never do these

- **Task Manager → End task.** Skips the shutdown backup.
- **Editing `cashier.mv.db` while the app is running.** H2 locks it for a reason.
- **Deleting a backup to free space.** The app keeps the newest 20 and purges
  the rest itself.
