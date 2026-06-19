package interfas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class PantallaInicio extends JFrame {

    private static final int ANCHO = 900;
    private static final int ALTO  = 650;

    public PantallaInicio() {
        setTitle("Sky Defense");
        setSize(ANCHO, ALTO);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        add(new PanelMenu());
        setVisible(true);
    }

    // -------------------------------------------------------------------------

    private class PanelMenu extends JPanel {

        // Logo: 720×400 fuente → escala 0.55 → 396×220
        private static final int LOGO_W = 396;
        private static final int LOGO_H = 220;
        private static final int LOGO_X = (ANCHO - LOGO_W) / 2;
        private static final int LOGO_Y = 18;

        // Botones: 580×120 fuente → escala 0.52 → 302×62
        private static final int BTN_W  = 240;
        private static final int BTN_H  = 55;
        private static final int BTN_X  = (ANCHO - BTN_W) / 2;
        // Primera fila justo debajo del logo
        private static final int BTN_Y0 = LOGO_Y + LOGO_H + 32;   // 270
        private static final int BTN_GAP = 14;

        private float scrollNebula = 0, scrollFar = 0, scrollMid = 0, scrollNear = 0;
        private final Timer timerFondo;

        PanelMenu() {
            setLayout(null);
            setBackground(Color.BLACK);
            agregarBotones();

            timerFondo = new Timer(16, e -> {
                scrollNebula = (scrollNebula + 0.06f) % 256;
                scrollFar    = (scrollFar   + 0.20f) % 256;
                scrollMid    = (scrollMid   + 0.55f) % 256;
                scrollNear   = (scrollNear  + 1.40f) % 256;
                repaint();
            });
            timerFondo.start();
        }

        private void agregarBotones() {
            JButton btnJugar        = crearBoton(GestorAssets.getMenuBotonJugar(), BTN_X, BTN_Y0);
            JButton btnPersonalizar = crearBoton(GestorAssets.getMenuBotonSkin(),  BTN_X, BTN_Y0 + BTN_H + BTN_GAP);
            JButton btnSalir        = crearBoton(GestorAssets.getMenuBotonSalir(), BTN_X, BTN_Y0 + (BTN_H + BTN_GAP) * 2);

            btnJugar.addActionListener(e -> { timerFondo.stop(); PantallaInicio.this.dispose(); new PantallaJuego(); });
            btnPersonalizar.addActionListener(e -> { timerFondo.stop(); PantallaInicio.this.dispose(); new PantallaPersonalizacion(); });
            btnSalir.addActionListener(e -> System.exit(0));

            add(btnJugar);
            add(btnPersonalizar);
            add(btnSalir);
        }

        private JButton crearBoton(BufferedImage img, int x, int y) {
            JButton btn = new JButton() {
                @Override
                protected void paintComponent(Graphics g) {
                    if (img == null) return;
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    boolean hover = getModel().isRollover();
                    if (hover) {
                        int dx = 8, dy = 4;
                        Composite prev = g2.getComposite();
                        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.20f));
                        g2.setColor(Color.WHITE);
                        g2.fillRoundRect(-dx/2, -dy/2, getWidth()+dx, getHeight()+dy, 8, 8);
                        g2.setComposite(prev);
                        g2.drawImage(img, -dx/2, -dy/2, getWidth()+dx, getHeight()+dy, null);
                    } else {
                        g2.drawImage(img, 0, 0, getWidth(), getHeight(), null);
                    }
                    g2.dispose();
                }
                @Override protected void paintBorder(Graphics g) {}
            };
            btn.setBounds(x, y, BTN_W, BTN_H);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return btn;
        }

        // ── Pintado ──────────────────────────────────────────────────────────

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,     RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            pintarFondo(g2);
            pintarLogo(g2);
            pintarScanlines(g2);
            pintarViñeta(g2);
        }

        private void pintarFondo(Graphics2D g2) {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, ANCHO, ALTO);
            GestorAssets.drawTiledLayer(g2, GestorAssets.getBackground("nebula"), scrollNebula, 0.28f, ANCHO, ALTO);
            GestorAssets.drawTiledLayer(g2, GestorAssets.getBackground("far"),    scrollFar,    0.80f, ANCHO, ALTO);
            GestorAssets.drawTiledLayer(g2, GestorAssets.getBackground("mid"),    scrollMid,    0.90f, ANCHO, ALTO);
            GestorAssets.drawTiledLayer(g2, GestorAssets.getBackground("near"),   scrollNear,   1.00f, ANCHO, ALTO);
        }

        private void pintarLogo(Graphics2D g2) {
            BufferedImage logo = GestorAssets.getMenuLogo();
            if (logo == null) return;
            g2.drawImage(logo, LOGO_X, LOGO_Y, LOGO_W, LOGO_H, null);
        }

        private void pintarScanlines(Graphics2D g2) {
            Composite prev = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.05f));
            g2.setColor(Color.BLACK);
            for (int y = 0; y < ALTO; y += 3) g2.fillRect(0, y, ANCHO, 1);
            g2.setComposite(prev);
        }

        private void pintarViñeta(Graphics2D g2) {
            float radio = Math.max(ANCHO, ALTO) * 0.72f;
            g2.setPaint(new RadialGradientPaint(
                    ANCHO / 2f, ALTO / 2f, radio,
                    new float[]{0.45f, 1.0f},
                    new Color[]{new Color(0,0,0,0), new Color(0,0,0,160)}
            ));
            g2.fillRect(0, 0, ANCHO, ALTO);
        }
    }

    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PantallaInicio::new);
    }
}
