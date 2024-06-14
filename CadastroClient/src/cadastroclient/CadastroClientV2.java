package cadastroclient;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.List;
import model.Produto;

public class CadastroClientV2 {

    public static void main(String[] args) {
        try (Socket clientSocket = new Socket(InetAddress.getByName("localhost"), 4321);
             ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
             BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {

            performLogin(out, in, reader);

            String command;
            do {
                System.out.println("Digite o Comando (L – Listar, E – Entrada, S – Saída, X – Finalizar)");
                command = reader.readLine();
                out.writeObject(command);

                if ("l".equalsIgnoreCase(command)) {
                    displayProductList(in);
                } else if ("e".equalsIgnoreCase(command) || "s".equalsIgnoreCase(command)) {
                    processEntryExit(out, reader);
                }

            } while (!"x".equalsIgnoreCase(command));

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void performLogin(ObjectOutputStream out, ObjectInputStream in, BufferedReader reader)
            throws IOException, ClassNotFoundException {
        System.out.println("Digite o Usuário");
        out.writeObject(reader.readLine());

        System.out.println("Digite a Senha");
        out.writeObject(reader.readLine());

        String result = (String) in.readObject();
        if (!"ok".equals(result)) {
            System.out.println("Erro de login");
            System.exit(1);
        }
        System.out.println("Login feito com sucesso");
    }

    private static void displayProductList(ObjectInputStream in) throws IOException, ClassNotFoundException {
        List<Produto> produtos = (List<Produto>) in.readObject();
        for (Produto produto : produtos) {
            System.out.println(produto.getNome());
        }
    }

    private static void processEntryExit(ObjectOutputStream out, BufferedReader reader) throws IOException {
        System.out.println("Digite o id da Pessoa");
        out.writeObject(reader.readLine());

        System.out.println("Digite o id do Produto");
        out.writeObject(reader.readLine());

        System.out.println("Digite a quantidade do Produto");
        out.writeObject(reader.readLine());

        System.out.println("Digite o valor do Produto");
        out.writeObject(reader.readLine());
    }
}
