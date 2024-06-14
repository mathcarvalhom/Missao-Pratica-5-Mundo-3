package cadastroserver;

import controller.MovimentoJPAController;
import controller.PessoaJPAController;
import controller.ProdutoJPAController;
import controller.UsuarioJPAController;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.swing.JTextArea;

public class ThreadClient extends Thread {

    private final JTextArea entrada;

    public ThreadClient(JTextArea entrada) {
        this.entrada = entrada;
    }

    @Override
    public void run() {
        try {
            EntityManagerFactory em = Persistence.createEntityManagerFactory("CadastroServerPU");
            ProdutoJPAController ctrlProduto = new ProdutoJPAController(em);
            UsuarioJPAController ctrlUsuario = new UsuarioJPAController(em);
            PessoaJPAController ctrlPessoa = new PessoaJPAController(em);
            MovimentoJPAController ctrlMovimento = new MovimentoJPAController(em);

            ServerSocket serverSocket = new ServerSocket(4321);
            log("Servidor iniciado. Aguardando conexões...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                log("Nova conexão recebida: " + clientSocket.getInetAddress());

                CadastroThread cadastroThread = new CadastroThread(
                        ctrlProduto, ctrlUsuario, ctrlPessoa, ctrlMovimento, entrada, clientSocket);
                cadastroThread.start();
            }
        } catch (IOException ex) {
            Logger.getLogger(ThreadClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void log(String mensagem) {
        entrada.append(mensagem + "\n");
    }

    public static void main(String[] args) {
        JTextArea exemploTextArea = new JTextArea();
        ThreadClient threadClient = new ThreadClient(exemploTextArea);
        threadClient.start();
    }
}
