package ru.geekbrains.netty.common;


public class RegistrationMessage extends AbstractMessage {
    public String login;
    public String password;
    public String nick;
    public String message;

    public RegistrationMessage(String login, String password, String nick) {
        this.login = login;
        this.password = password;
        this.nick = nick;
    }

    public RegistrationMessage(String message) { this.message = message;}
}

