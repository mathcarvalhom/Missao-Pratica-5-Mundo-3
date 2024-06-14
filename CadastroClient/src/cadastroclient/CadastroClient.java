package cadastroclient;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import model.Produto;

public class CadastroClient {

    public static void main(String[] args) {
        try (Socket clientSocket = new Socket(InetAddress.getByName("localhost"), 4321);
             ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream())) {

            performLogin(out, in);

            System.out.println("Usuario conectado com sucesso!!");

            retrieveAndDisplayProducts(out, in);

            out.writeObject("X");

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void performLogin(ObjectOutputStream out, ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        out.writeObject("op1");
        out.writeObject("op1");

        String result = (String) in.readObject();
        if (!"ok".equals(result)) {
            System.out.println("Erro de login");
            System.exit(1);
        }
    }

    private static void retrieveAndDisplayProducts(ObjectOutputStream out, ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        out.writeObject("L");

        List<Produto> produtos = (List<Produto>) in.readObject();
        for (Produto produto : produtos) {
            System.out.println(produto.getNome());
        }
    }
}
