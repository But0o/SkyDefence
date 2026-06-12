package interfas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Random;

public class PantallaInicio extends JFrame {

    private static final int ANCHO = 900;
    private static final int ALTO = 650;

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

        private final int[][] posEstrellas;
        private final int[] brilloEstrellas;
        private final Timer timerParpadeo;
        private Font fuenteCRT;

        PanelMenu() {
            setLayout(null);
            setBackground(new Color(0, 0, 10));

            cargarFuenteCRT();

            posEstrellas = new int[180][2];
            brilloEstrellas = new int[180];
            Random rand = new Random();
            for (int i = 0; i < posEstrellas.length; i++) {
                posEstrellas[i][0] = rand.nextInt(ANCHO);
                posEstrellas[i][1] = rand.nextInt(ALTO);
                brilloEstrellas[i] = rand.nextInt(180) + 60;
            }

            agregarBotones();

            timerParpadeo = new Timer(120, e -> {
                Random r = new Random();
                for (int i = 0; i < brilloEstrellas.length; i++) {
                    if (r.nextInt(25) == 0) brilloEstrellas[i] = r.nextInt(180) + 60;
                }
                repaint();
            });
            timerParpadeo.start();
        }

        private void cargarFuenteCRT() {
            try {
                fuenteCRT = Font.createFont(Font.TRUETYPE_FONT,
                        new File("/usr/share/fonts/liberation/LiberationMono-Bold.ttf"));
                GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(fuenteCRT);
            } catch (Exception e) {
                fuenteCRT = null;
            }
        }

        private void agregarBotones() {
            int anchoBtn = 300;
            int altoBtn  = 52;
            int x        = (ANCHO - anchoBtn) / 2;

            JButton btnJugar        = crearBoton("►  JUGAR",          x, 280, anchoBtn, altoBtn);
            btnJugar.addActionListener(e -> { PantallaInicio.this.dispose(); new PantallaJuego(); });
            JButton btnPersonalizar = crearBoton("✦  PERSONALIZAR",    x, 350, anchoBtn, altoBtn);
            btnPersonalizar.addActionListener(e -> { PantallaInicio.this.dispose(); new PantallaPersonalizacion(); });
            JButton btnSalir        = crearBoton("✕  SALIR",           x, 420, anchoBtn, altoBtn);

            btnSalir.addActionListener(e -> System.exit(0));

            add(btnJugar);
            add(btnPersonalizar);
            add(btnSalir);
        }

        private JButton crearBoton(String texto, int x, int y, int ancho, int alto) {
            Color colorNormal = new Color(0, 210, 110);
            Color colorHover  = new Color(50, 255, 160);

            JButton btn = new JButton(texto) {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

                    boolean hover = getModel().isRollover();
                    Color color   = hover ? colorHover : colorNormal;

                    g2.setColor(hover ? new Color(0, 255, 150, 25) : new Color(0, 0, 10));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);

                    g2.setStroke(new BasicStroke(2f));
                    g2.setColor(color);
                    g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 6, 6);

                    g2.setFont(new Font("Monospaced", Font.BOLD, 17));
                    FontMetrics fm = g2.getFontMetrics();
                    int tx = (getWidth()  - fm.stringWidth(getText())) / 2;
                    int ty = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                    g2.setColor(color);
                    g2.drawString(getText(), tx, ty);
                    g2.dispose();
                }

                @Override protected void paintBorder(Graphics g) {}
            };

            btn.setBounds(x, y, ancho, alto);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            return btn;
        }

        // -------------------------------------------------------------------------

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            pintarFondo(g2);
            pintarEstrellas(g2);
            pintarTitulo(g2);
            pintarLineasDecorativas(g2);
            pintarPieVersion(g2);
            pintarScanlines(g2);
            pintarViñeta(g2);
        }

        private void pintarFondo(Graphics2D g2) {
            g2.setPaint(new GradientPaint(0, 0, new Color(0, 0, 18), 0, ALTO, new Color(0, 5, 35)));
            g2.fillRect(0, 0, ANCHO, ALTO);
        }

        private void pintarEstrellas(Graphics2D g2) {
            for (int i = 0; i < posEstrellas.length; i++) {
                int b = brilloEstrellas[i];
                g2.setColor(new Color(b, b, b));
                int sz = b > 210 ? 2 : 1;
                g2.fillOval(posEstrellas[i][0], posEstrellas[i][1], sz, sz);
            }
        }

        private void pintarTitulo(Graphics2D g2) {
            String titulo = "SKY DEFENSE";
            Font fuenteTitulo = fuenteCRT != null
                    ? fuenteCRT.deriveFont(Font.BOLD, 74f)
                    : new Font("Monospaced", Font.BOLD, 74);
            g2.setFont(fuenteTitulo);
            FontMetrics fm = g2.getFontMetrics();
            int tx = (ANCHO - fm.stringWidth(titulo)) / 2;
            int ty = 190;

            // Halo verde difuso
            for (int d = 6; d >= 1; d--) {
                g2.setColor(new Color(0, 255, 140, 12 * (7 - d)));
                g2.drawString(titulo, tx - d, ty - d);
                g2.drawString(titulo, tx + d, ty - d);
                g2.drawString(titulo, tx - d, ty + d);
                g2.drawString(titulo, tx + d, ty + d);
            }

            // Aberración cromática: canal rojo a la izquierda, azul a la derecha
            Composite original = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.50f));
            g2.setColor(new Color(255, 0, 0));
            g2.drawString(titulo, tx - 3, ty);
            g2.setColor(new Color(0, 60, 255));
            g2.drawString(titulo, tx + 3, ty);
            g2.setComposite(original);

            // Canal verde principal encima
            g2.setColor(new Color(0, 255, 145));
            g2.drawString(titulo, tx, ty);

            // Subtítulo
            String sub = " - -  D E F I E N D E   E L   C I E L O  - - ";
            Font fuenteSub = new Font("Monospaced", Font.PLAIN, 15);
            g2.setFont(fuenteSub);
            FontMetrics fmSub = g2.getFontMetrics();
            int sx = (ANCHO - fmSub.stringWidth(sub)) / 2;
            g2.setColor(new Color(0, 160, 90));
            g2.drawString(sub, sx, ty + 32);
        }

        private void pintarLineasDecorativas(Graphics2D g2) {
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(0, 255, 140, 45));

            g2.setColor(new Color(0, 200, 100, 90));
            g2.setStroke(new BasicStroke(2f));
            int m = 22, l = 24;
            g2.drawLine(m,         m,        m + l,     m        );
            g2.drawLine(m,         m,        m,         m + l    );
            g2.drawLine(ANCHO - m, m,        ANCHO-m-l, m        );
            g2.drawLine(ANCHO - m, m,        ANCHO - m, m + l    );
            g2.drawLine(m,         ALTO - m, m + l,     ALTO - m );
            g2.drawLine(m,         ALTO - m, m,         ALTO-m-l );
            g2.drawLine(ANCHO - m, ALTO - m, ANCHO-m-l, ALTO - m );
            g2.drawLine(ANCHO - m, ALTO - m, ANCHO - m, ALTO-m-l );
        }

        // Líneas horizontales semitransparentes que simulan el fosforescente de un CRT
        private void pintarScanlines(Graphics2D g2) {
            Composite anterior = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.07f));
            g2.setColor(Color.BLACK);
            for (int y = 0; y < ALTO; y += 3) {
                g2.fillRect(0, y, ANCHO, 1);
            }
            g2.setComposite(anterior);
        }

        // Oscurece los bordes como la curvatura de una pantalla CRT
        private void pintarViñeta(Graphics2D g2) {
            float radio = Math.max(ANCHO, ALTO) * 0.70f;
            RadialGradientPaint viñeta = new RadialGradientPaint(
                    ANCHO / 2f, ALTO / 2f, radio,
                    new float[]{0.40f, 1.0f},
                    new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 200)}
            );
            g2.setPaint(viñeta);
            g2.fillRect(0, 0, ANCHO, ALTO);
        }

        private void pintarPieVersion(Graphics2D g2) {
            g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
            g2.setColor(new Color(0, 120, 65));
            g2.drawString("v0.1", 26, ALTO - 25);
        }
    }

    // -------------------------------------------------------------------------

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PantallaInicio::new);
    }
}
