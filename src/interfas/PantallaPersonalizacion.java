package interfas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

public class PantallaPersonalizacion extends JFrame {

    private static final int ANCHO = 900;
    private static final int ALTO  = 650;

    public PantallaPersonalizacion() {
        setTitle("Sky Defense - Personalizar");
        setSize(ANCHO, ALTO);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        add(new PanelPersonalizacion());
        setVisible(true);
    }

    // =========================================================================

    private class PanelPersonalizacion extends JPanel {

        // Título: 720×220 → escala 0.67 → 480×147
        private static final int TITULO_W = 480;
        private static final int TITULO_H = 147;
        private static final int TITULO_X = (ANCHO - TITULO_W) / 2;
        private static final int TITULO_Y = 10;

        // Cards: 256×350 → escala 0.78 → 200×273
        private static final int CARD_W    = 200;
        private static final int CARD_H    = 273;
        private static final int CARD_GAP  = 30;
        private static final int CARD_Y    = TITULO_Y + TITULO_H + 14;   // 171
        private static final int CARD_X0   = (ANCHO - 3 * CARD_W - 2 * CARD_GAP) / 2; // 120

        // Nombres: 540×130 → escalados al ancho de la card → 200×48
        private static final int NOMBRE_H  = 48;
        private static final int NOMBRE_Y  = CARD_Y + CARD_H + 8;       // 452

        // Botones: 580×120 → escala 0.52 → 302×62
        private static final int BTN_W     = 302;
        private static final int BTN_H     = 62;
        private static final int BTN_GAP   = 40;
        private static final int BTN_Y     = NOMBRE_Y + NOMBRE_H + 16;  // 516
        private static final int BTN_ELEGIR_X = (ANCHO - 2 * BTN_W - BTN_GAP) / 2;
        private static final int BTN_VOLVER_X = BTN_ELEGIR_X + BTN_W + BTN_GAP;

        // Scroll del fondo
        private float scrollNebula = 0, scrollFar = 0, scrollMid = 0, scrollNear = 0;
        private final Timer timerFondo;

        // Hover state de las cards
        private int hoverCard = -1;

        // Tema guardado al entrar (para cancelar con VOLVER)
        private final int temaOriginal = ConfiguracionSkins.tema;

        PanelPersonalizacion() {
            setLayout(null);
            setBackground(Color.BLACK);

            crearCards();
            crearBotones();

            timerFondo = new Timer(16, e -> {
                scrollNebula = (scrollNebula + 0.06f) % 256;
                scrollFar    = (scrollFar   + 0.20f) % 256;
                scrollMid    = (scrollMid   + 0.55f) % 256;
                scrollNear   = (scrollNear  + 1.40f) % 256;
                repaint();
            });
            timerFondo.start();
        }

        // ── Creación de componentes ──────────────────────────────────────────

        private void crearCards() {
            for (int i = 0; i < 3; i++) {
                final int tema = i;
                int cx = CARD_X0 + i * (CARD_W + CARD_GAP);

                // Área invisible clicable sobre la card + nombre
                JPanel area = new JPanel() {
                    @Override protected void paintComponent(Graphics g) {}
                };
                area.setOpaque(false);
                area.setBounds(cx, CARD_Y, CARD_W, CARD_H + 8 + NOMBRE_H);
                area.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                area.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e)  {
                        ConfiguracionSkins.tema = tema;
                        repaint();
                    }
                    @Override public void mouseEntered(MouseEvent e)  { hoverCard = tema; repaint(); }
                    @Override public void mouseExited(MouseEvent e)   { hoverCard = -1;   repaint(); }
                });
                add(area);
            }
        }

        private void crearBotones() {
            JButton btnElegir = crearBoton(GestorAssets.getBotonElegir(), BTN_ELEGIR_X, BTN_Y);
            JButton btnVolver = crearBoton(GestorAssets.getBotonVolver(), BTN_VOLVER_X, BTN_Y);

            btnElegir.addActionListener(e -> {
                // La selección ya está guardada en ConfiguracionSkins.tema
                timerFondo.stop();
                PantallaPersonalizacion.this.dispose();
                new PantallaInicio();
            });
            btnVolver.addActionListener(e -> {
                // Restaurar tema original al cancelar
                ConfiguracionSkins.tema = temaOriginal;
                timerFondo.stop();
                PantallaPersonalizacion.this.dispose();
                new PantallaInicio();
            });

            add(btnElegir);
            add(btnVolver);
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
            pintarTitulo(g2);
            pintarCards(g2);
            pintarNombres(g2);
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

        private void pintarTitulo(Graphics2D g2) {
            BufferedImage img = GestorAssets.getSelectorTitulo();
            if (img != null)
                g2.drawImage(img, TITULO_X, TITULO_Y, TITULO_W, TITULO_H, null);
        }

        private void pintarCards(Graphics2D g2) {
            for (int i = 0; i < 3; i++) {
                boolean sel   = (ConfiguracionSkins.tema == i);
                boolean hover = (hoverCard == i);
                int cx = CARD_X0 + i * (CARD_W + CARD_GAP);

                BufferedImage img = GestorAssets.getSelectorCard(i, sel);
                if (img == null) continue;

                if (hover && !sel) {
                    // brillo sutil al pasar el mouse sobre una card no seleccionada
                    Composite prev = g2.getComposite();
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.85f));
                    g2.drawImage(img, cx, CARD_Y, CARD_W, CARD_H, null);
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
                    g2.setColor(Color.WHITE);
                    g2.fillRect(cx, CARD_Y, CARD_W, CARD_H);
                    g2.setComposite(prev);
                } else {
                    g2.drawImage(img, cx, CARD_Y, CARD_W, CARD_H, null);
                }
            }
        }

        private void pintarNombres(Graphics2D g2) {
            for (int i = 0; i < 3; i++) {
                BufferedImage img = GestorAssets.getSelectorNombre(i);
                if (img == null) continue;
                int cx = CARD_X0 + i * (CARD_W + CARD_GAP);
                g2.drawImage(img, cx, NOMBRE_Y, CARD_W, NOMBRE_H, null);
            }
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

    // =========================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PantallaPersonalizacion::new);
    }
}
