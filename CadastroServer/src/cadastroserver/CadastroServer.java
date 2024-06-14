package cadastroserver;

import java.awt.EventQueue;
import java.io.IOException;

public class CadastroServer {

    public static void main(String[] args) {
        EventQueue.invokeLater(() -> {
            try {
                SaidaFrame frame = new SaidaFrame();
                ThreadClient client = new ThreadClient(frame.getTexto());
                client.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
