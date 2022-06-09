package ru.geekbrains.netty.client;

import ru.geekbrains.netty.common.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.*;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicInteger;

public class MainController implements Initializable {

    private static String nick;
    private static SecretKey key;

    static {
        try {
            key = AESUtil.generateKey(128);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    private static String algorithm = "AES/CBC/PKCS5Padding";
    private static IvParameterSpec ivParameterSpec = AESUtil.generateIv();

    private static final AtomicInteger count = new AtomicInteger(1);
    private static int fileID;

    @FXML
    static
    ListView<String> clientFilesList;

    @FXML
    ListView<String> serverFilesList;

    @FXML
    HBox cloudPanel;

    @FXML
    HBox authPanel;

    @FXML
    TextField loginField;

    @FXML
    PasswordField passwordField;

    @FXML
    Button authButton;

    @FXML
    TextField loginField1;

    @FXML
    TextField nickField;

    @FXML
    PasswordField passwordField1;

    @FXML
    PasswordField passwordField2;

    @FXML
    Button registerButton;

    public MainController() throws NoSuchAlgorithmException {
        testSonar();
    }


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setAuthorized(false);
        Network.start();
        Thread thread = new Thread(() -> {

            try {
                while (true) {
                    AbstractMessage abstractMessage = Network.readObject();
                    if(abstractMessage instanceof  RegistrationMessage){
                        RegistrationMessage r = (RegistrationMessage) abstractMessage;
                        if (r.message.equals("/not_null_userId")) {
                             Platform.runLater(() -> registerButton.setText("Ник занят."));
                        } else {
                            String nick = r.message.split(" ")[1];
                            Files.createDirectory(Paths.get("client" + nick));
                               Platform.runLater(() -> registerButton.setText("Регистрация успешно завершена."));
                        }
                    }
                    if (abstractMessage instanceof AuthMessage) {
                        AuthMessage authMessage = (AuthMessage) abstractMessage;
                        if (authMessage.message.startsWith("/authOk")) {
                            setAuthorized(true);
                            nick = authMessage.message.split(" ")[1];
                            System.out.println("Подключился клиент " + nick);
                            break;
                        }
                        if ("/null_userId".equals(authMessage.message)) {
                            Platform.runLater(() -> authButton.setText("Неверный логин или пароль."));
                        }
                    }
                }
                Network.sendMsg(new RefreshServerFileListMessage());
                refreshLocalFilesList();
                while (true) {
                    AbstractMessage abstractMessage = Network.readObject();
                    if (abstractMessage instanceof FileMessage) {
                        FileMessage fileMessage = (FileMessage) abstractMessage;
                        if (!Files.exists(Paths.get("client" + nick + "/" + fileMessage.getFilename()))) {
                            Files.write(Paths.get("client" + nick + "/" + fileMessage.getFilename()),
                                    fileMessage.getData(), StandardOpenOption.CREATE);
                            refreshLocalFilesList();
                        }
                    }
                    if (abstractMessage instanceof RefreshServerFileListMessage) {
                        RefreshServerFileListMessage refreshServerMsg = (RefreshServerFileListMessage) abstractMessage;
                        refreshServerFileList(refreshServerMsg.getServerFileList());
                    }

                }

            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                Network.stop();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    private void setAuthorized(boolean isAuthorized) {
        if (!isAuthorized) {
            authPanel.setVisible(true);
            authPanel.setManaged(true);
            cloudPanel.setVisible(false);
            cloudPanel.setManaged(false);
        } else {
            authPanel.setVisible(false);
            authPanel.setManaged(false);
            cloudPanel.setVisible(true);
            cloudPanel.setManaged(true);
        }
    }

    public void tryToAuth() {
        Network.sendMsg(new AuthMessage(loginField.getText(), passwordField.getText()));
        loginField.clear();
        passwordField.clear();
    }

    public void pressOnDownloadButton(ActionEvent actionEvent) {
        Network.sendMsg(new DownloadMessage("/" + serverFilesList.getSelectionModel().getSelectedItem()));
    }

    public void pressOnSendToCloudButton(ActionEvent actionEvent) {
        try {
            Path userFile = Paths.get("client" + nick + "/encryptedFiles/"
                    + clientFilesList.getSelectionModel().getSelectedItem());
            String glob = "glob:**.{encrypted}";
            PathMatcher matcher = FileSystems.getDefault().getPathMatcher(glob);

            if(matcher.matches(userFile)){
                Network.sendMsg(new FileMessage(userFile));
            }
            else {
                showEncryptionSendAlert();
            }
        } catch (IOException e) {
            showSendAlert();
            e.printStackTrace();
        }
    }

    public void deleteButton(ActionEvent actionEvent){
        String userFile = Paths.get(clientFilesList.getSelectionModel().getSelectedItem()).getFileName().toString();
        deleteUserFiles(userFile);
    }

    public void deleteUserFiles(String userFile){
        if(userFile.contains("encrypted_")){
            deleteFromEncryptedFilesList();
        }else{
            deleteFromClient();
        }
    }

    public void testSonar(){

    }

    public void deleteFromEncryptedFilesList(){
        try {
            Files.delete(Paths.get("client" + nick + "/encryptedFiles/" + clientFilesList.getSelectionModel().getSelectedItem()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        refreshLocalFilesList();
    }

    public void deleteFromClient() {
        try {
            Files.delete(Paths.get("client" + nick + "/" + clientFilesList.getSelectionModel().getSelectedItem()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        refreshLocalFilesList();
    }

    public void deleteFromServer(ActionEvent actionEvent) {
        Network.sendMsg(new DeleteMessage("/" + serverFilesList.getSelectionModel().getSelectedItem()));
    }

    private void refreshLocalFilesList() {
        updateUI(() -> {
            try {
                clientFilesList.getItems().clear();
                Files.list(Paths.get("client" + nick)).map(p -> p.getFileName().toString()).
                        forEach(o -> clientFilesList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void refreshServerFileList(ArrayList<String> fileList) {
        updateUI(() -> {
            serverFilesList.getItems().clear();
            serverFilesList.getItems().addAll(fileList);
        });
    }

    private static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

    public void returnOnMainWindow(ActionEvent actionEvent){
        updateUI(() -> {
            try {
                clientFilesList.getItems().clear();
                Files.list(Paths.get("client" + nick)).map(p -> p.getFileName().toString()).
                        forEach(o -> clientFilesList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void decryptUserFile(ActionEvent actionEvent) throws IOException, NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException{
        fileID = count.incrementAndGet();
        File encryptedFile = Paths.get("client" + nick + "/encryptedFiles/"
                        + clientFilesList.getSelectionModel().getSelectedItem())
                .toFile();
        String fileExtension = getExtension(encryptedFile.getName());

        Path newPath = Paths.get("client" + nick + "/" + "decrypted_" + fileID + "." + fileExtension);

        AESUtil.decryptFile(algorithm,key,ivParameterSpec,encryptedFile,newPath.toFile());

        showDecryptionSuccess();
    }

    public static String getExtension(String fileName) {
        char ch;
        int len;
        if(fileName==null ||
                (len = fileName.length())==0 ||
                (ch = fileName.charAt(len-1))=='/' || ch=='\\' ||
                ch=='.' )
            return "";
        int dotInd = fileName.lastIndexOf('.'),
                sepInd = Math.max(fileName.lastIndexOf('/'), fileName.lastIndexOf('\\'));
        if( dotInd<=sepInd )
            return "";
        else
            return fileName.substring(dotInd+1).toLowerCase();
    }

    public static void EncryptUserFile(ActionEvent actionEvent) throws IOException, NoSuchPaddingException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException,
            BadPaddingException, IllegalBlockSizeException {
        fileID = count.incrementAndGet();
        File inputFile = Paths.get("client" + nick + "/"
                        + clientFilesList.getSelectionModel().getSelectedItem())
                .toFile();
        File encryptedFile = new File((Paths.get("client" + nick + "/encryptedFiles/" + "encrypted_" + inputFile.getName())).toString());

        AESUtil.encryptFile(algorithm, key, ivParameterSpec, inputFile, encryptedFile);
        showEncryptionSuccess();
    }

    public void registrationOnServer(ActionEvent actionEvent) {
        if (passwordField1.getText().equals(passwordField2.getText())) {
            Network.sendMsg(new RegistrationMessage(loginField1.getText(), passwordField1.getText(), nickField.getText()));
        } else {
            System.out.println("Введите еще раз.");
        }
    }

    public void openUserFolder (ActionEvent actionEvent) throws IOException {
        updateUI(() -> {
            try {
                String userChoise = clientFilesList.getSelectionModel().getSelectedItem();
                clientFilesList.getItems().clear();
                if(userChoise.endsWith(".txt") || userChoise.endsWith(".txt.encrypted") || userChoise.endsWith(".encrypted")){
                    showAlert();
                }else {
                    Files.list(Paths.get("client" + nick + "/" + userChoise)).map(p -> p.getFileName().toString()).
                            forEach(o -> clientFilesList.getItems().add(o));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void showAlert(){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText("Ошибка при открывании папки");
        alert.setContentText("Нельзя открыть, так как это не папка");

        alert.showAndWait();
    }

    public static void showEncryptionSuccess(){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Шифрование");
        alert.setHeaderText("Шифрование файла");
        alert.setContentText("Файл зашифрован успешно");

        alert.showAndWait();
    }

    public void showDecryptionSuccess(){
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Дешифрование");
        alert.setHeaderText("Дешифрование файла");
        alert.setContentText("Файл дешифрован успешно");

        alert.showAndWait();
    }

    public void showSendAlert(){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText("Ошибка при отправке файла");
        alert.setContentText("Нельзя отправить файл");

        alert.showAndWait();
    }

    public void showEncryptionSendAlert(){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText("Ошибка при отправке файла");
        alert.setContentText("Нельзя отправить незашифрованный файл");

        alert.showAndWait();
    }

    public static void showAuthAlert(){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText("Ошибка при авторизации");
        alert.setContentText("Введен неверный логин или пароль");

        alert.showAndWait();
    }

    public void openServerFolder (ActionEvent actionEvent){
        updateUI(() -> {
            try {
                serverFilesList.getItems().clear();
                Files.list(Paths.get("server_" + nick + "/encryptedFiles/")).map(p -> p.getFileName().toString()).
                        forEach(o -> serverFilesList.getItems().add(o));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }


    public void closeConnection(ActionEvent actionEvent) {
        Network.sendMsg(new AuthMessage("/connection_close"));
        MainClient.launch();

    }
}

