package net.strangled.maladan.cli;


import net.MaladaN.Tor.thoughtcrime.InitData;
import net.MaladaN.Tor.thoughtcrime.SignalCrypto;
import net.i2p.client.streaming.I2PSocket;
import net.i2p.client.streaming.I2PSocketManager;
import net.i2p.client.streaming.I2PSocketManagerFactory;
import net.i2p.data.Destination;
import net.strangled.maladan.serializables.AuthResults;
import net.strangled.maladan.serializables.ServerInit;
import net.strangled.maladan.serializables.ServerLogin;
import net.strangled.maladan.serializables.User;
import net.strangled.maladan.shared.IncomingMessageThread;
import net.strangled.maladan.shared.LocalLoginDataStore;
import net.strangled.maladan.shared.OutgoingMessageThread;
import org.whispersystems.libsignal.SignalProtocolAddress;

import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Main {
    private static InitData data = null;

    public static void main(String[] args) {
        data = SignalCrypto.initStore();
    }

    public static byte[] hashData(String data) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(data.getBytes());
        return messageDigest.digest();
    }

    public static byte[] serializeObject(Object object) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out = new ObjectOutputStream(bos);
        out.writeObject(object);
        out.flush();
        return bos.toByteArray();
    }

    public static Object reconstructSerializedObject(byte[] object) throws Exception {
        ByteArrayInputStream bis = new ByteArrayInputStream(object);
        ObjectInput in = new ObjectInputStream(bis);
        return in.readObject();
    }

    I2PSocket connect() {
        I2PSocket sock;

        try {
            I2PSocketManager manager = I2PSocketManagerFactory.createManager();
            Destination destination = new Destination("JaiCQHfweWn8Acp1XyTse1GL1392f-ZKzal9kyOhBAo-oYtnXAJIe8JU73taAjROnWApCe-hRUOlb6RkwW3kL2orqR8zhO6RDQMmOMy7FYqCq3UlNOOEQbLO1wo3kd65PA8D1zkhdFYqfYsQk4uEgci4~bamadKNOJXE1C~A53kEY-kYQ-vRSdV9LSFCRGay5BNDVJ1lFI~CYJRmreMx1hvd9YAsUg0fuy-U0AzylXwigSRejBhCNfsF-6-dLCQa8KYg8gzxe0DHUNRw18Yf1VwnvV7X2gM0CRQVcMhu7YgD3iwfT~DKFjZqRbNse~xEF0RtMCfhg7LgyCBRlJGVTj2PeXgxVtWHm3L-BtZ4bB5Ugb6K3ZdUFq9zP~VyKUmUJXSpApqhGdiGUWjj91-OZDJYnh6xgT17i-g0T2tEYLoSx9em~YZQQ~-mO3iSpiccSvmPjOpg9X1XVp9QvIyvWQIwrkv6y6ZgHeTrsxsG8HBZhPbMy6flinJRsCcPnIOlAAAA");
            sock = manager.connect(destination);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return sock;
    }

    //TODO needs to send and verify public identity key too
    AuthResults login(String username, String password) throws Exception {
        if (username.isEmpty() || password.isEmpty()) {
            return null;
        }

        SignalProtocolAddress address = new SignalProtocolAddress("SERVER", 0);

        byte[] hashedUsername = hashData(username);
        String base64Username = DatatypeConverter.printBase64Binary(hashedUsername);

        byte[] hashedPassword = hashData(password);
        byte[] encryptedPassword = SignalCrypto.encryptByteMessage(hashedPassword, address, null);

        ServerLogin login = new ServerLogin(base64Username, encryptedPassword);

        OutgoingMessageThread.addNewMessage(login);

        //wait for the login results to come back
        return waitForData();
    }

    AuthResults register(String username, String password, String uniqueId) throws Exception {
        if (username.isEmpty() || password.isEmpty() || uniqueId.isEmpty()) {
            return null;
        }

        byte[] hashedUsername = hashData(username);
        ServerInit init = new ServerInit(hashedUsername, uniqueId, Main.data);

        IncomingMessageThread.setData(password, username);
        OutgoingMessageThread.addNewMessage(init);

        LocalLoginDataStore.saveLocaluser(new User(true));

        return waitForData();
    }

    private AuthResults waitForData() throws Exception {
        while (IncomingMessageThread.getAuthResults() == null) {
            Thread.sleep(1000);
        }

        return IncomingMessageThread.getAuthResults();
    }

    boolean sendStringMessage(String message) {
        return false;
    }

    boolean sendFileMessage(File file) {
        return false;
    }
}