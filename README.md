# 🌟 sxClans

**sxClans** is a feature-rich Minecraft plugin that enhances your server with a robust clan system, including customizable ranks, invitations, and clan management features, all designed to bring more interaction and excitement to your gameplay! 🏰👑

## 🚀 Features

- **Clan Creation & Management** – Create and manage clans with customizable ranks and permissions.
- **Dynamic Clan Ranking** – Players can rise through the ranks in their clan, with special roles like Leader, Co-Leader, and Moderator.
- **Invitations & Member Limits** – Invite players to your clan and manage your clan's size with player limits and timed invitations.
- **Customizable Clan Messages** – Fully customizable messages for invitations, promotions, and notifications to keep your players engaged.
- **Holograms for Top Clans** – Display top clans by currency or level on holograms, with automatic updates.
- **Sound Notifications** – Play customizable sounds for events like clan creation, joining, and leaving.
- **Multi-Version Support** – Compatible with multiple Minecraft versions and Spigot/Paper servers.

## 👥 Installation

1. Download the latest version from [Releases](https://github.com/sxkadamn/sxClans/releases).
2. Place `sxClans.jar` in your server's `plugins` folder.
3. Restart your server.
4. Configure the plugin through `plugins/sxClans/config.yml`.

## 🎮 Commands  

| Command                      | Description                                   |  
|------------------------------|-----------------------------------------------|  
| `/clan`           | command which opening menu.       |  
## ⚙️ Configuration (`config.yml`)  

Example basic configuration:  

```yaml
storage:
  type: sqlite #postgresql, mysql
  mysql:
    host: localhost
    port: 3306
    database: sxclans
    username: root
    password: ""
  postgresql:
    host: localhost
    port: 5432
    database: sxclans
    username: postgres
    password: ""

war_xp_reward: 100
war_duration: 60
war_start_delay: 10
default_member_limit: 10
clan_ranks:
  leader: " OWNER"
  co_leader: " CO-OWNER"
  moderator: " MODERATOR"
  member: " MEMBER"
invite:
  invite_message: " &e{sender} приглашает вас в клан  {clan}"
  sent_message: " &aВы отправили приглашение игроку {target}"
  timeout: 1200
  buttons:
    accept: "✅ &a[ПРИНЯТЬ]"
    deny: "❌ &c[ОТКЛОНИТЬ]"
  errors:
    max_limit_reached: "&cКлан достиг максимального лимита игроков!"
    already_invited: "&cЭтот игрок уже имеет активное приглашение!"
    no_invite: "&cУ вас нет активных приглашений!"
    expired_sender: "&cПриглашение игроку {target} истекло."
    expired_target: "&cВаше приглашение в клан истекло."
    accepted: "&aВы вступили в клан  {clan}!"
    accepted_sender: "&aИгрок {player} присоединился к клану!"
    denied: "&cВы отклонили приглашение в клан."
    denied_sender: "&c{player} отклонил ваше приглашение."
sounds:
  clan_created:
    - "ENTITY_PLAYER_LEVELUP:1.0:1.0"
  clan_joined:
    - "ENTITY_VILLAGER_YES:1.0:1.0"
  clan_left:
    - "ENTITY_VILLAGER_NO:1.0:1.0"
  clan_teleport:
    - "ENTITY_ENDERMAN_TELEPORT:1.0:1.0"

holograms:
  money_top:
    enabled: true
    location:
      world: world
      x: 100.5
      y: 64.0
      z: 200.5
    update_interval_seconds: 300
    header:
      - "&6&l◈ &e&lТОП-10 КЛАНОВ ПО ВАЛЮТЕ (МОНЕТЫ) &6&l◈"
      - "&7&m---------------------"
    lines:
      - "&e1. &6{name} &8- &a{bank}$ &7(Лидер: {leader})"
      - "&72. &f{name} &8- &a{bank}$ &7(Лидер: {leader})"
      - "&e3. &6{name} &8- &a{bank}$ &7(Лидер: {leader})"
      - "&74. &f{name} &8- &a{bank}$ &7(Лидер: {leader})"
      - "&e5. &6{name} &8- &a{bank}$ &7(Лидер: {leader})"
    footer:
      - "&7&m---------------------"
      - "&6Обновлено: &e{time}"
  rubles_top:
    enabled: true
    location:
      world: world
      x: 100.5
      y: 64.0
      z: 200.5
    update_interval_seconds: 300
    header:
      - "&6&l◈ &e&lТОП-10 КЛАНОВ ПО ВАЛЮТЕ (РУБЛИ) &6&l◈"
      - "&7&m---------------------"
    lines:
      - "&e1. &6{name} &8- &a{rubles}$ &7(Лидер: {leader})"
      - "&72. &f{name} &8- &a{rubles}$ &7(Лидер: {leader})"
      - "&e3. &6{name} &8- &a{rubles}$ &7(Лидер: {leader})"
      - "&74. &f{name} &8- &a{rubles}$ &7(Лидер: {leader})"
      - "&e5. &6{name} &8- &a{rubles}$ &7(Лидер: {leader})"
    footer:
      - "&7&m---------------------"
      - "&6Обновлено: &e{time}"
  level_top:
    enabled: true
    location:
      world: world
      x: 103.5
      y: 64.0
      z: 200.5
    update_interval_seconds: 300
    header:
      - "&b&l◈ &3&lТОП-10 КЛАНОВ ПО УРОВНЮ &b&l◈"
      - "&7&m---------------------"
    lines:
      - "&61. &e{name} &8- &bУр. {level} &7(Лидер: {leader})"
      - "&72. &f{name} &8- &bУр. {level} &7(Лидер: {leader})"
      - "&63. &e{name} &8- &bУр. {level} &7(Лидер: {leader})"
    footer:
      - "&7&m---------------------"
      - "&6Обновлено: &e{time}"


messages:
  score_notification: "&6Текущий счет: &a{clan1} &7- &c{clan2}&6Необходимо для победы: &e{max_score}"
  player_eliminated: "&7Игрок из клана {clan_name} погиб! (+1 очко {killer_clan})"
  war_start_notification: "&cВойна начнется через {time} секунд. Подготовьтесь!"
  war_end_winner: "&aКлан &e{winner_clan} &aпобедил в войне!"
  war_end_loser: "&cКлан &e{loser_clan} &cпроиграл в войне."
  all_players_eliminated: "&cВсе участники клана &e{clan_name} &cбыли уничтожены."
  plugin_not_enabled: "Для работы плагина требуется: {pluginName}"
  pvp_disabled: "&cPvP между соклановцами выключено!"
  kick_success: "&cВы исключили {player} из клана."
  kicked: "&cВы были исключены из клана."
  promote_success: "&aВы повысили {player} до {rank}."
  promoted: "&aВы были повышены до {rank}."
  max_rank: "&cЭтот игрок уже имеет максимальный ранг."

war_world_folder: "wars"
war_template_world: "world"
war_locations:
  - clan1_x: 100
    clan1_y: 64
    clan1_z: 100
    clan1_yaw: 0
    clan1_pitch: 0
    clan2_x: 200
    clan2_y: 64
    clan2_z: 100
    clan2_yaw: 180
    clan2_pitch: 0
  - clan1_x: 1000
    clan1_y: 64
    clan1_z: 1000
    clan1_yaw: 0
    clan1_pitch: 0
    clan2_x: 1200
    clan2_y: 64
    clan2_z: 1000
    clan2_yaw: 180
    clan2_pitch: 0

scoreboard:
  lines:
    - "&7⏳ Время: &f{time_left}s"
    - "&8──────────────"
    - "&b⚔ {clan1_name} &8➤ &a{score1}"
    - "&c⚔ {clan2_name} &8➤ &c{score2}"
    - "&8──────────────"
    - "&e🏆 До победы: &6{max_score}"
    - "&a🟢 Живые: &a{alive1} &8| &c{alive2}"
    - "&7☠ Убито: &7{dead1} &8| &7{dead2}"
  animations:
    title:
      frames:
        - "&c&lВ О Й Н А   К Л А Н О В"
        - "&4&lВ О Й Н А   К Л А Н О В"
        - "&6&lВ О Й Н А   К Л А Н О В"
      interval: 10

```

*For advanced configuration options, check the [documentation](https://github.com/sxkadamn/sxClans/wiki).*  

## 📌 Requirements  

- Minecraft 1.16+  
- Spigot/Paper server  
- Java 16+  

## 🛠️ Contributing  

Want to improve sxAirDrops? Fork the repository, make your changes, and submit a Pull Request!  

## ❓ Support  

Have questions or suggestions? Open an issue in the [repository](https://github.com/sxkadamn/sxClans/issues)!  

---

🔹 **Developer:** sxkadamn  

💥 Enhance your Minecraft server with exciting airdrops today! 🔥
