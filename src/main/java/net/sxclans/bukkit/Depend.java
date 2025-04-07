package net.sxclans.bukkit;

import com.sun.net.httpserver.HttpServer;
import net.lielibrary.bukkit.command.BaseCommand;
import net.lielibrary.gui.impl.MenuListener;
import net.sxclans.bukkit.command.UserCommands;
import net.sxclans.bukkit.control.ControlerFights;
import net.sxclans.bukkit.files.FilesManager;
import net.sxclans.bukkit.holograms.HologramManager;
import net.sxclans.common.clan.base.ClanBaseManager;
import net.sxclans.common.clan.base.FileBasedClanManager;
import net.sxclans.common.clan.base.MySQLClanManager;
import net.sxclans.common.clan.base.PostgreSQLClanManager;
import net.sxclans.common.clan.models.ClanRank;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class Depend extends JavaPlugin {

    private static Depend instance;


    private static ClanBaseManager clanManage;


    private FilesManager filesManager;

    private HologramManager hologramManager;

    private final String telegramBotToken = "7715307360:AAFvEpydlOSPY_pGT51AbExRAaECg-1P06Q";
    private final String telegramChatId = "-1002431189334";



    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        ClanRank.loadConfig(getConfig());

        String type = getConfig().getString("storage.type").toLowerCase();

        switch (type) {
            case "mysql" -> {
                String host = getConfig().getString("mysql.host");
                int port = getConfig().getInt("mysql.port");
                String database = getConfig().getString("mysql.database");
                String user = getConfig().getString("mysql.user");
                String password = getConfig().getString("mysql.password");

                Connection conn;
                try {
                    conn = DriverManager.getConnection(
                            "jdbc:mysql://" + host + ":" + port + "/" + database,
                            user, password);
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
                clanManage = new MySQLClanManager(conn);
            }
            case "postgresql", "postgres" -> {
                String host = getConfig().getString("postgres.host");
                int port = getConfig().getInt("postgres.port");
                String database = getConfig().getString("postgres.database");
                String user = getConfig().getString("postgres.user");
                String password = getConfig().getString("postgres.password");

                Connection conn;
                try {
                    conn = DriverManager.getConnection(
                            "jdbc:postgresql://" + host + ":" + port + "/" + database,
                            user, password
                    );
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }

                clanManage = new PostgreSQLClanManager(conn);
            }
            default -> {
                File file = new File(this.getDataFolder(), "clans");
                if (!file.exists()) file.mkdirs();

                clanManage = new FileBasedClanManager(file);
            }
        }

        clanManage.loadClans();
        filesManager = new FilesManager(this);

        hologramManager = new HologramManager(this);
        hologramManager.init();

        getServer().getPluginManager().registerEvents(new ControlerFights(), this);
        getServer().getPluginManager().registerEvents(new MenuListener(), this);
        BaseCommand.register(this, new UserCommands("clan"));

        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext("/webhook", exchange -> {
                if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                    InputStream requestBody = exchange.getRequestBody();
                    String payload = new String(requestBody.readAllBytes(), StandardCharsets.UTF_8);

                    try {
                        JSONObject jsonPayload = new JSONObject(payload);
                        handleGitHubWebhook(jsonPayload);
                        exchange.sendResponseHeaders(200, "Webhook processed successfully!".getBytes().length);
                        exchange.getResponseBody().write("Webhook processed successfully!".getBytes());
                    } catch (Exception e) {
                        getLogger().severe("Error processing webhook: " + e.getMessage());
                        exchange.sendResponseHeaders(500, "Error processing webhook".getBytes().length);
                        exchange.getResponseBody().write("Error processing webhook".getBytes());
                    }
                } else {
                    exchange.sendResponseHeaders(405, "Method Not Allowed".getBytes().length);
                    exchange.getResponseBody().write("Method Not Allowed".getBytes());
                }
                exchange.close();
            });

            server.setExecutor(null); // Используем дефолтный executor
            server.start();
            getLogger().info("HTTP Server started on port 8080");
        } catch (IOException e) {
            getLogger().severe("Failed to start HTTP server: " + e.getMessage());
        }
    }

    private void sendDevlogToTelegram(String message) {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    int maxLength = 4096;
                    String localMessage = message; // Создаем локальную копию message
                    while (!localMessage.isEmpty()) {
                        String part = localMessage.substring(0, Math.min(maxLength, localMessage.length()));
                        localMessage = localMessage.substring(Math.min(maxLength, localMessage.length()));

                        URL url = new URL("https://api.telegram.org/bot" + telegramBotToken + "/sendMessage");
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("POST");
                        connection.setRequestProperty("Content-Type", "application/json");
                        connection.setDoOutput(true);

                        JSONObject json = new JSONObject();
                        json.put("chat_id", telegramChatId);
                        json.put("text", part);

                        OutputStream os = connection.getOutputStream();
                        os.write(json.toString().getBytes());
                        os.flush();
                        os.close();

                        int responseCode = connection.getResponseCode();
                        if (responseCode == 200) {
                            getLogger().info("Message sent to Telegram successfully!");
                        } else {
                            getLogger().warning("Failed to send message to Telegram. Response code: " + responseCode);
                        }
                    }
                } catch (Exception e) {
                    getLogger().severe("Error sending message to Telegram: " + e.getMessage());
                }
            }
        }.runTaskAsynchronously(this);
    }


    private void handleGitHubWebhook(JSONObject payload) {
        try {
            // Проверяем, есть ли поле "commits"
            if (!payload.has("commits")) {
                getLogger().warning("No 'commits' field found in the webhook payload.");
                return;
            }

            JSONArray commits = payload.getJSONArray("commits");

            // Проверяем, есть ли хотя бы один коммит
            if (commits.length() == 0) {
                getLogger().warning("No commits found in the webhook payload.");
                return;
            }

            // Формируем сообщение для Telegram
            StringBuilder devlogMessage = new StringBuilder("New update pushed to GitHub!\n\n");

            for (int i = 0; i < commits.length(); i++) {
                JSONObject commit = commits.getJSONObject(i);
                String commitMessage = commit.getString("message");
                String authorName = commit.getJSONObject("author").getString("name");

                devlogMessage.append("Commit ").append(i + 1).append(":\n")
                        .append("Message: ").append(commitMessage).append("\n")
                        .append("Author: ").append(authorName).append("\n\n");
            }

            // Отправляем сообщение в Telegram
            sendDevlogToTelegram(devlogMessage.toString());
        } catch (Exception e) {
            getLogger().severe("Error parsing GitHub webhook payload: " + e.getMessage());
        }
    }


    @Override
    public void onDisable() {
        filesManager.saveMenuConfigs();
        clanManage.saveClans();
        hologramManager.disable();
    }

    public FilesManager getFilesManager() {
        return filesManager;
    }

    public static ClanBaseManager getClanManage() {
        return clanManage;
    }

    public static Depend getInstance() {
        return instance;
    }
}