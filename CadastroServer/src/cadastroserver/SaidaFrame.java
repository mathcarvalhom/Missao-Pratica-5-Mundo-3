package cadastroserver;

import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class SaidaFrame extends JDialog {

    private JTextArea texto;

    public SaidaFrame() {
        initUI();
    }

    private void initUI() {
        texto = new JTextArea();
        JScrollPane scrollPane = new JScrollPane(texto);
        this.add(scrollPane);

        JButton limparButton = new JButton("Limpar");
        limparButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                texto.setText("");
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(limparButton);

        this.add(buttonPanel, BorderLayout.SOUTH);

        this.setTitle("Saída do Servidor");
        this.setSize(new Dimension(400, 300));
        this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        this.setVisible(true);
        this.setModal(false);
    }

    /**
     * @return the texto
     */
    public JTextArea getTexto() {
        return texto;
    }

    /**
     * @param texto the texto to set
     */
    public void setTexto(JTextArea texto) {
        this.texto = texto;
    }

    public static void main(String[] args) {
        SaidaFrame frame = new SaidaFrame();
        frame.getTexto().append("Exemplo de texto na saída.\n");
    }
}
