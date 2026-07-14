# JAVA RetroChat

A tiny Java Swing chatroom with a 2000s desktop-chat vibe: pixel-ish fonts, NES-style colors, one server, and multiple clients.

## How To Run

### Git Bash

Use forward slashes in Git Bash:

```bash
cd /c/.../JAVA_retrochat
javac -d out src/retrochat/*.java
java -cp out retrochat.ChatServer
```

Open one or more clients in new Git Bash terminals:

```bash
cd /c/.../JAVA_retrochat
java -cp out retrochat.RetroChatClient
```

### PowerShell

Use backslashes in PowerShell:

```powershell
cd C:\...\JAVA_retrochat
javac -d out src\retrochat\*.java
java -cp out retrochat.ChatServer
```

Open one or more clients in new PowerShell terminals:

```powershell
cd C:\...\JAVA_retrochat
java -cp out retrochat.RetroChatClient
```

Use `localhost` and port `5050` when the client asks where to connect.

If you see `Invalid filename: src/retrochat/*.java`, you are probably not inside the `JAVA_retrochat` folder yet.

## What To Build Next

1. Private messages: add commands like `/msg luis hello`.
2. Multiple rooms: add commands like `/join games` and make the server store clients by room name.
3. User list: make the server send a `USERS|name1,name2` update after joins/leaves.
4. Message history: write messages to a text file or SQLite database.
5. Better retro polish: add sound effects, CRT scanlines, avatars, custom bitmap font, and room tabs.
