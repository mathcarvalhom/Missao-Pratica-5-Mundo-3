package cadastroserver;

import controller.ProdutoJPAController;
import controller.UsuarioJPAController;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.Produto;
import model.Usuario;

public class CadastroThread extends Thread {

    private final ProdutoJPAController ctrl;
    private final UsuarioJPAController ctrlUsu;
    private final Socket s1;

    public CadastroThread(ProdutoJPAController ctrl, UsuarioJPAController ctrlUsu, Socket s1) {
        this.ctrl = ctrl;
        this.ctrlUsu = ctrlUsu;
        this.s1 = s1;
    }

    @Override
    public void run() {
        System.out.println("Thread is running...");

        try (ObjectInputStream in = new ObjectInputStream(s1.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(s1.getOutputStream())) {

            String login = (String) in.readObject();
            String senha = (String) in.readObject();

            Usuario user = ctrlUsu.findUsuario(login, senha);
            if (user == null) {
                out.writeObject("nok");
                return;
            }
            out.writeObject("ok");

            String input;
            do {
                input = (String) in.readObject();
                if ("l".equalsIgnoreCase(input)) {
                    List<Produto> produtos = ctrl.findProdutoEntities();
                    out.writeObject(produtos);
                } else if (!"x".equalsIgnoreCase(input)) {
                    System.out.println("Comando inválido recebido: " + input);
                }

            } while (!input.equalsIgnoreCase("x"));

        } catch (ClassNotFoundException | IOException ex) {
            Logger.getLogger(CadastroThread.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            System.out.println("Thread finalizada...");
        }
    }
}
