# <center>🛡️ AuthCord</center>

AuthCord Whitelist is a plugin for securing your Minecraft server. This plugin combines server whitelisting with an authentication system powered by a Discord bot.

### 🔥 Key Features:

- Two Operation Modes: Full support for online-mode=true/false servers. 

- SSO (Passwordless Login): Links a player's in-game account to their Discord profile and IP address. If a player connects from a trusted IP, they are authenticated automatically. 

- Discord Notifications: If someone attempts to log in using your username from a different IP address, the bot will instantly send you a notification via direct message. 

- Full Folia Compatibility: The plugin is designed with Folia's multi-threaded architecture in mind and works perfectly on standard Paper servers. 

- Features a cooldown system for code requests and limited code validity periods (protecting against brute-force attacks and spam). 

### 🔧 Operation Modes

**online-mode=true**

  - When a player not currently on the whitelist attempts to join, the server kicks them while displaying a unique verification code on their screen. 

  - The player enters this code into a direct message to the bot on Discord. 

  - The bot verifies the player's license via the Mojang API and adds them to the server whitelist.

**online-mode=false**

  - A secure alternative to traditional password-based plugins. 

  - The player joins the server but remains "frozen": they cannot move, chat, or break blocks. 

  - The player types `/getcode` in-game to receive a verification code, then sends that code to the bot. 

  - The bot links a player's Discord ID and IP address to their in-game nickname. 

  - **Protection:** If the IP address changes (or if a different person attempts to log in), the plugin will freeze the player again and send a notification to the account owner via Discord, including a button to confirm the new IP address.

### 🗒️ Commands

**In-game:**

  - /getcode — Obtain a temporary code for Discord verification.

**In Discord:**

  - /verify [code] — Confirm account ownership and join the whitelist.

### 🤖 Installation Instructions

- Install the plugin on your server. 
- Create a bot in the [Discord Developers Portal](https://discord.com/developers/). 
- Enable the "Server Members Intent" in the bot's settings. 
- Enter the bot's token and your server's Guild ID into the `config.yml` file. 
- Select an operating mode (ONLINE or OFFLINE) and customize the messages to your liking. 
- Restart the server.

## ⚙️ config.yml


```
discord:
  token: "token"
  guild-id: "guild id"

settings:
  # ru/en console output
  language: "en"
  auth-mode: "OFFLINE" #ONLINE/OFFLINE
  code-expiration-minutes: 5
  request-cooldown-seconds: 30
  discord-invite-url: "https://discord.gg/your-invite"

messages:
  in-game:
    messages:
      in-game:
        kick-online: "<red>You are not on the whitelist!</red>\n<white>Your code: <yellow>%code%</yellow></white>\n<gray>Verify it in our Discord server > <blue><underlined>%url%</underlined></blue></gray>"
        not-registered: "<red>You are not registered!</red>\n<white>Enter <yellow>/getcode</yellow> and confirm it in </white><click:open_url:'%url%'><hover:show_text:'<aqua>Open Discord Link</aqua>'><blue>[Discord]</blue></hover></click>"
        action-blocked: "<red>To play you need to confirm Discord: </red><click:open_url:'%url%'><hover:show_text:'<aqua>Join our Discord</aqua>'><blue><underlined>[Click here]</underlined></blue></hover></click>"
        new-ip: "<red>A new IP address has been detected!</red>\n<white>For security reasons, enter <yellow>/getcode</yellow> and confirm your login via Discord.</white>"
        auth-success: "<green>IP authentication successful!</green>"
        getcode-success: "<white>Your code: <yellow>%code%</yellow></white>\n<gray>Send <aqua>/verify %code%</aqua> in Discord.</gray>"
        verify-success: "<green>Authentication successful!</green> <white>Your IP address has been saved.</white>"
        cooldown: "<red>Please wait <yellow>%time%</yellow> seconds before requesting another code!</red>"
        code-expired: "<red>The code has expired! Request a new one in-game.</red>"

  discord:
    discord-already-used: "This Discord account is already linked!"
    invalid-code: "Invalid or expired code!"
    already-linked: "This username is already linked to another Discord user!"
    verify-success: "Account **%player%** successfully verified!"
    player-offline: "The player must be online on the server during verification!"
    dm-alert: |
      **ATTENTION! LOGIN ATTEMPT!**
      Someone is attempting to log in to your Minecraft account **%player%**. **IP Address:** `%ip%`
  
      **If this is you:** Type `/getcode` in-game and confirm the new IP using the `/verify` command on the Discord server.
      **If this is NOT you:** Simply ignore this message. The intruder has been frozen on the server and cannot do anything!
```

## ⚙️ data.yml


```
users:
  nickname:
    discord: 'discord_id'
    ip: player's last IP // 'mojang_verified' if auth-mode: "ONLINE"
```
