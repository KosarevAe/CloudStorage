package ru.geekbrains.netty.server;

import javafx.scene.control.Alert;

import java.sql.*;

class DBConnection {
    private static Connection connection;
    private static Statement stmt;
    private static final String user = "root";
    private static final String password = "root";

    static  void connect() {
        System.out.println("DB connection start");
        try {
            Class.forName("com.mysql.cj.jdbc.Driver").getDeclaredConstructor().newInstance();
            connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/users", user, password);
            stmt = connection.createStatement();
            System.out.println("DB connection start");

        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Connection error");
        }
    }

    static String getIdByLoginAndPass(String login, String password) {
        String sql = String.format("SELECT nickname FROM main " + "WHERE login = '%s' AND password = '%s'", login, password);
        try {
            ResultSet resultSet = stmt.executeQuery(sql);
            if (resultSet.next()) {
                return resultSet.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    static void registrationByLoginPassAndNick(String login, String password, String nick){
        try {
            String query = "INSERT INTO main (login, password, nickname) VALUES (?, ?, ?);";
            PreparedStatement ps = connection.prepareStatement(query);
            ps.setString(1, login);
            ps.setString(2, password);
            ps.setString(3, nick);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static void disconnect() {
        try {
            connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
