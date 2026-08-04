import javax.swing.*;
import java.awt.*;
import java.util.List;

public class DialogoBorradores extends JDialog {
    private SupabaseReportesClient.ResumenBorrador seleccionado;
    private final JList<SupabaseReportesClient.ResumenBorrador> lista = new JList<>();

    public DialogoBorradores(Frame parent, List<SupabaseReportesClient.ResumenBorrador> borradores){

        super(parent, "Reportes Guradados", true);

        DefaultListModel<SupabaseReportesClient.ResumenBorrador> modelo = new DefaultListModel<>();

        for(SupabaseReportesClient.ResumenBorrador b: borradores){
            modelo.addElement(b);
        }
        lista.setModel(modelo);
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setVisibleRowCount(12);

        JButton botonAbrir = new BotonColoreado();
        botonAbrir.setText("Abrir");
        botonAbrir.setBackground(new Color(0, 172, 193));
        botonAbrir.setForeground(Color.WHITE);
        botonAbrir.addActionListener(e -> {
           seleccionado = lista.getSelectedValue();
           if(seleccionado != null){
               dispose();
           } else{
               JOptionPane.showMessageDialog(this, "Seleccione un registro para abrir");
           }
        });

        JButton botonCancelar = new BotonColoreado();
        botonCancelar.setText("Cancelar");
        botonCancelar.setBackground(new Color(120, 120, 120));
        botonCancelar.setForeground(Color.WHITE);
        botonCancelar.addActionListener(e -> dispose());

        JPanel botones = new JPanel(new FlowLayout());
        botones.add(botonAbrir);
        botones.add(botonCancelar);

        setLayout(new BorderLayout());
        if(borradores.isEmpty()){
            add(new JLabel("No tienes reportes guardados todavía.", SwingConstants.CENTER), BorderLayout.CENTER);
        } else {
            add(new JScrollPane(lista), BorderLayout.CENTER);
        }
        add(botones, BorderLayout.SOUTH);

        setSize(300, 300);
        setLocationRelativeTo(parent);

    }

    public static SupabaseReportesClient.ResumenBorrador mostrar(Frame parent, List<SupabaseReportesClient.ResumenBorrador> borradores){
        DialogoBorradores dialogo = new DialogoBorradores(parent, borradores);
        dialogo.setVisible(true);
        return dialogo.seleccionado;
    }


}
