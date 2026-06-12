package interfas;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

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

    enum TipoSkin { AVION, DRON }

    // =========================================================================
    //  PANEL PRINCIPAL
    // =========================================================================

    private class PanelPersonalizacion extends JPanel {

        private final int[][] posEstrellas;
        private final int[]   brilloEstrellas;
        private final Timer   timerParpadeo;
        private final TarjetaSkin[] tarjetasAvion = new TarjetaSkin[2];
        private final TarjetaSkin[] tarjetasDron  = new TarjetaSkin[2];

        PanelPersonalizacion() {
            setLayout(null);
            setBackground(new Color(0, 0, 10));

            posEstrellas    = new int[180][2];
            brilloEstrellas = new int[180];
            Random rand = new Random();
            for (int i = 0; i < posEstrellas.length; i++) {
                posEstrellas[i][0] = rand.nextInt(ANCHO);
                posEstrellas[i][1] = rand.nextInt(ALTO);
                brilloEstrellas[i] = rand.nextInt(180) + 60;
            }

            crearTarjetas();
            crearBotonVolver();

            timerParpadeo = new Timer(120, e -> {
                Random r = new Random();
                for (int i = 0; i < brilloEstrellas.length; i++) {
                    if (r.nextInt(25) == 0) brilloEstrellas[i] = r.nextInt(180) + 60;
                }
                repaint();
            });
            timerParpadeo.start();
        }

        private void crearTarjetas() {
            int cw = 175, ch = 250, cardY = 150;

            tarjetasAvion[0] = new TarjetaSkin(TipoSkin.AVION, 0, "ORIGINAL");
            tarjetasAvion[1] = new TarjetaSkin(TipoSkin.AVION, 1, "MODERNO");
            tarjetasDron[0]  = new TarjetaSkin(TipoSkin.DRON,  0, "ORIGINAL");
            tarjetasDron[1]  = new TarjetaSkin(TipoSkin.DRON,  1, "MODERNO");

            tarjetasAvion[0].setBounds(43,  cardY, cw, ch);
            tarjetasAvion[1].setBounds(233, cardY, cw, ch);
            tarjetasDron[0].setBounds(490, cardY, cw, ch);
            tarjetasDron[1].setBounds(680, cardY, cw, ch);

            for (TarjetaSkin t : tarjetasAvion) {
                t.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) {
                        ConfiguracionSkins.skinAvion = t.indice;
                        for (TarjetaSkin ts : tarjetasAvion) ts.repaint();
                    }
                });
                add(t);
            }
            for (TarjetaSkin t : tarjetasDron) {
                t.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) {
                        ConfiguracionSkins.skinDron = t.indice;
                        for (TarjetaSkin ts : tarjetasDron) ts.repaint();
                    }
                });
                add(t);
            }
        }

        private void crearBotonVolver() {
            JButton btn = new JButton("◄  VOLVER") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                    boolean hover = getModel().isRollover();
                    Color   color = hover ? new Color(50, 255, 160) : new Color(0, 210, 110);
                    g2.setColor(hover ? new Color(0, 255, 150, 25) : new Color(0, 0, 10));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                    g2.setStroke(new BasicStroke(2f));
                    g2.setColor(color);
                    g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 6, 6);
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
            int bw = 220, bh = 48;
            btn.setBounds((ANCHO - bw) / 2, 568, bw, bh);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(false);
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            btn.addActionListener(e -> {
                PantallaPersonalizacion.this.dispose();
                new PantallaInicio();
            });
            add(btn);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            pintarFondo(g2);
            pintarEstrellas(g2);
            pintarTitulo(g2);
            pintarEtiquetasSecciones(g2);
            pintarDecoraciones(g2);
            pintarScanlines(g2);
            pintarViñeta(g2);
        }

        private void pintarFondo(Graphics2D g2) {
            g2.setPaint(new GradientPaint(0, 0, new Color(0, 0, 18), 0, ALTO, new Color(0, 5, 35)));
            g2.fillRect(0, 0, ANCHO, ALTO);
        }

        private void pintarEstrellas(Graphics2D g2) {
            for (int i = 0; i < posEstrellas.length; i++) {
                int b  = brilloEstrellas[i];
                int sz = b > 210 ? 2 : 1;
                g2.setColor(new Color(b, b, b));
                g2.fillOval(posEstrellas[i][0], posEstrellas[i][1], sz, sz);
            }
        }

        private void pintarTitulo(Graphics2D g2) {
            String titulo = "PERSONALIZAR";
            g2.setFont(new Font("Monospaced", Font.BOLD, 42));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (ANCHO - fm.stringWidth(titulo)) / 2;
            int ty = 78;
            for (int d = 4; d >= 1; d--) {
                g2.setColor(new Color(0, 255, 140, 15 * (5 - d)));
                g2.drawString(titulo, tx - d, ty);
                g2.drawString(titulo, tx + d, ty);
                g2.drawString(titulo, tx, ty - d);
                g2.drawString(titulo, tx, ty + d);
            }
            g2.setColor(new Color(0, 255, 145));
            g2.drawString(titulo, tx, ty);
        }

        private void pintarEtiquetasSecciones(Graphics2D g2) {
            g2.setFont(new Font("Monospaced", Font.BOLD, 14));
            FontMetrics fm = g2.getFontMetrics();

            // Sección izquierda: NAVE DEL JUGADOR (centrada entre x=43 y x=408)
            String lblA = "──  NAVE DEL JUGADOR  ──";
            int cxA = (43 + 408) / 2;
            g2.setColor(new Color(0, 200, 110));
            g2.drawString(lblA, cxA - fm.stringWidth(lblA) / 2, 132);

            // Sección derecha: NAVES ENEMIGAS (centrada entre x=490 y x=855)
            String lblD = "──  NAVES ENEMIGAS  ──";
            int cxD = (490 + 855) / 2;
            g2.drawString(lblD, cxD - fm.stringWidth(lblD) / 2, 132);

            // Línea superior
            g2.setColor(new Color(0, 255, 140, 40));
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(30, 100, ANCHO - 30, 100);

            // Línea inferior
            g2.drawLine(30, 425, ANCHO - 30, 425);

            // Separador vertical central
            g2.setColor(new Color(0, 255, 140, 25));
            g2.drawLine(448, 105, 448, 420);
        }

        private void pintarDecoraciones(Graphics2D g2) {
            g2.setColor(new Color(0, 200, 100, 85));
            g2.setStroke(new BasicStroke(2f));
            int m = 22, l = 22;
            g2.drawLine(m,         m,        m + l,     m        );
            g2.drawLine(m,         m,        m,         m + l    );
            g2.drawLine(ANCHO - m, m,        ANCHO-m-l, m        );
            g2.drawLine(ANCHO - m, m,        ANCHO - m, m + l    );
            g2.drawLine(m,         ALTO - m, m + l,     ALTO - m );
            g2.drawLine(m,         ALTO - m, m,         ALTO-m-l );
            g2.drawLine(ANCHO - m, ALTO - m, ANCHO-m-l, ALTO - m );
            g2.drawLine(ANCHO - m, ALTO - m, ANCHO - m, ALTO-m-l );
        }

        private void pintarScanlines(Graphics2D g2) {
            Composite ant = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.07f));
            g2.setColor(Color.BLACK);
            for (int y = 0; y < ALTO; y += 3) g2.fillRect(0, y, ANCHO, 1);
            g2.setComposite(ant);
        }

        private void pintarViñeta(Graphics2D g2) {
            float radio = Math.max(ANCHO, ALTO) * 0.70f;
            RadialGradientPaint vp = new RadialGradientPaint(
                    ANCHO / 2f, ALTO / 2f, radio,
                    new float[]{0.40f, 1.0f},
                    new Color[]{new Color(0,0,0,0), new Color(0,0,0,200)}
            );
            g2.setPaint(vp);
            g2.fillRect(0, 0, ANCHO, ALTO);
        }
    }

    // =========================================================================
    //  TARJETA DE SKIN
    // =========================================================================

    private class TarjetaSkin extends JPanel {

        final TipoSkin tipo;
        final int      indice;
        final String   nombre;
        private boolean hover = false;

        TarjetaSkin(TipoSkin tipo, int indice, String nombre) {
            this.tipo   = tipo;
            this.indice = indice;
            this.nombre = nombre;
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { hover = true;  repaint(); }
                @Override public void mouseExited (MouseEvent e) { hover = false; repaint(); }
            });
        }

        private boolean estaSeleccionado() {
            return tipo == TipoSkin.AVION
                    ? ConfiguracionSkins.skinAvion == indice
                    : ConfiguracionSkins.skinDron  == indice;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            boolean sel    = estaSeleccionado();
            Color   cBorde = sel   ? new Color(0, 255, 150)
                           : hover ? new Color(0, 180, 100)
                           :         new Color(0, 70,  40);

            // Fondo
            g2.setColor(sel ? new Color(0, 30, 15, 210) : new Color(0, 0, 20, 210));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);

            // Borde
            g2.setStroke(new BasicStroke(sel ? 2.5f : 1.5f));
            g2.setColor(cBorde);
            g2.drawRoundRect(1, 1, getWidth()-2, getHeight()-2, 10, 10);

            int cx = getWidth() / 2;

            if (tipo == TipoSkin.AVION) {
                int cy = 118;
                if (indice == 0) dibujarAvionOriginal(g2, cx, cy);
                else             dibujarAvionModerno(g2, cx, cy);
            } else {
                // Drone arriba, separador, misil abajo
                int cyDron  = 72;
                int cyMisil = 172;

                if (indice == 0) {
                    dibujarDronOriginal(g2, cx, cyDron);
                    dibujarMisilOriginal(g2, cx, cyMisil);
                } else {
                    dibujarDronModerno(g2, cx, cyDron);
                    dibujarMisilModerno(g2, cx, cyMisil);
                }

                // Separador "MISIL:" entre drone y misil
                g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
                g2.setColor(new Color(0, 160, 85, 180));
                FontMetrics fmm = g2.getFontMetrics();
                String sep = "─ MISIL ─";
                g2.drawString(sep, cx - fmm.stringWidth(sep) / 2, 128);
            }

            // Etiqueta de nombre de skin en el fondo de la tarjeta
            g2.setFont(new Font("Monospaced", Font.BOLD, 13));
            FontMetrics fm = g2.getFontMetrics();
            String label = (sel ? "✔ " : "  ") + nombre;
            g2.setColor(sel ? new Color(0, 255, 150) : new Color(0, 160, 90));
            g2.drawString(label, (getWidth() - fm.stringWidth(label)) / 2, getHeight() - 12);

            g2.dispose();
        }
    }

    // =========================================================================
    //  METODOS DE DIBUJO DE SKINS (reutilizables por el juego)
    // =========================================================================

    // ─── AVION ORIGINAL ──────────────────────────────────────────────────────
    public static void dibujarAvionOriginal(Graphics2D g2, int cx, int cy) {
        g2.setStroke(new BasicStroke(1.5f));

        // Fuselaje triangular apuntando hacia arriba
        int[] xf = { cx,     cx-16, cx+16 };
        int[] yf = { cy-60,  cy+26, cy+26 };
        g2.setColor(new Color(0, 200, 120));
        g2.fillPolygon(xf, yf, 3);

        // Alas laterales
        int[] xwl = { cx-16, cx-60, cx-16 };
        int[] ywl = { cy+5,  cy+26, cy+26 };
        int[] xwr = { cx+16, cx+60, cx+16 };
        int[] ywr = { cy+5,  cy+26, cy+26 };
        g2.setColor(new Color(0, 170, 100));
        g2.fillPolygon(xwl, ywl, 3);
        g2.fillPolygon(xwr, ywr, 3);

        // Motor / tobera
        g2.setColor(new Color(0, 110, 65));
        g2.fillRect(cx-9, cy+26, 18, 16);

        // Cabina
        g2.setColor(new Color(140, 255, 210, 210));
        g2.fillOval(cx-7, cy-50, 14, 22);

        // Outlines
        g2.setColor(new Color(0, 90, 55));
        g2.drawPolygon(xf, yf, 3);
        g2.drawPolygon(xwl, ywl, 3);
        g2.drawPolygon(xwr, ywr, 3);
    }

    // ─── AVION MODERNO ───────────────────────────────────────────────────────
    public static void dibujarAvionModerno(Graphics2D g2, int cx, int cy) {
        g2.setStroke(new BasicStroke(1f));

        // Fuselaje delgado
        int[] xf = { cx,    cx-7,  cx-7,  cx+7,  cx+7  };
        int[] yf = { cy-62, cy+8,  cy+36, cy+36, cy+8  };
        g2.setPaint(new GradientPaint(cx-7, cy-62, new Color(50, 130, 255),
                                      cx+7, cy+36, new Color(0,  215, 255)));
        g2.fillPolygon(xf, yf, 5);

        // Alas delta
        int[] xdl = { cx-7, cx-65, cx-22 };
        int[] ydl = { cy+2, cy+36, cy+36 };
        int[] xdr = { cx+7, cx+65, cx+22 };
        int[] ydr = { cy+2, cy+36, cy+36 };
        g2.setPaint(new GradientPaint(cx, cy+2, new Color(30, 100, 220),
                                      cx, cy+36, new Color(0, 170, 220)));
        g2.fillPolygon(xdl, ydl, 3);
        g2.fillPolygon(xdr, ydr, 3);

        // Tobera
        g2.setColor(new Color(0, 200, 255, 210));
        g2.fillRect(cx-6, cy+36, 12, 14);

        // Llama del motor
        int[] xflm = { cx-5, cx+5, cx };
        int[] yflm = { cy+50, cy+50, cy+64 };
        g2.setColor(new Color(255, 170, 0, 190));
        g2.fillPolygon(xflm, yflm, 3);
        int[] xflm2 = { cx-2, cx+2, cx };
        int[] yflm2 = { cy+50, cy+50, cy+72 };
        g2.setColor(new Color(255, 240, 100, 130));
        g2.fillPolygon(xflm2, yflm2, 3);

        // Cabina alargada
        g2.setPaint(new GradientPaint(cx-4, cy-56, new Color(160, 225, 255),
                                      cx+4, cy-18, new Color(20,  70,  140)));
        g2.fillOval(cx-5, cy-56, 10, 42);

        // Outlines
        g2.setColor(new Color(0, 160, 255, 150));
        g2.drawPolygon(xf,  yf,  5);
        g2.drawPolygon(xdl, ydl, 3);
        g2.drawPolygon(xdr, ydr, 3);
    }

    // ─── DRON ORIGINAL ───────────────────────────────────────────────────────
    public static void dibujarDronOriginal(Graphics2D g2, int cx, int cy) {
        g2.setStroke(new BasicStroke(1.5f));

        // Alas
        g2.setColor(new Color(190, 60, 0));
        g2.fillRect(cx-62, cy-9,  38, 18);
        g2.fillRect(cx+24, cy-9,  38, 18);

        // Cuerpo rectangular
        g2.setColor(new Color(225, 80, 0));
        g2.fillRect(cx-24, cy-16, 48, 32);

        // Detalle interior
        g2.setColor(new Color(255, 130, 30, 190));
        g2.fillRect(cx-10, cy-8, 20, 16);

        // Antena
        g2.setColor(new Color(255, 150, 0));
        g2.setStroke(new BasicStroke(2.5f));
        g2.drawLine(cx, cy-16, cx, cy-32);
        g2.fillOval(cx-4, cy-36, 8, 8);

        // Outlines
        g2.setColor(new Color(140, 40, 0));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRect(cx-24, cy-16, 48, 32);
        g2.drawRect(cx-62, cy-9,  38, 18);
        g2.drawRect(cx+24, cy-9,  38, 18);
    }

    // ─── DRON MODERNO ────────────────────────────────────────────────────────
    public static void dibujarDronModerno(Graphics2D g2, int cx, int cy) {
        // Brazos
        g2.setColor(new Color(80, 0, 160));
        g2.setStroke(new BasicStroke(5f));
        int[][] armTips = {{cx-38, cy-27}, {cx+38, cy-27}, {cx-38, cy+27}, {cx+38, cy+27}};
        for (int[] tip : armTips) g2.drawLine(cx, cy, tip[0], tip[1]);

        // Rotores: brillo + anillo
        for (int[] tip : armTips) {
            g2.setStroke(new BasicStroke(1f));
            Composite prev = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
            g2.setColor(new Color(200, 100, 255));
            g2.fillOval(tip[0]-16, tip[1]-16, 32, 32);
            g2.setComposite(prev);
            g2.setColor(new Color(170, 50, 255));
            g2.setStroke(new BasicStroke(2f));
            g2.drawOval(tip[0]-14, tip[1]-14, 28, 28);
            // Cruz del rotor
            g2.setColor(new Color(220, 130, 255, 200));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawLine(tip[0]-9, tip[1], tip[0]+9, tip[1]);
            g2.drawLine(tip[0], tip[1]-9, tip[0], tip[1]+9);
        }

        // Cuerpo hexagonal
        int r = 22;
        int[] xh = new int[6], yh = new int[6];
        for (int i = 0; i < 6; i++) {
            double ang = Math.PI / 2 + i * Math.PI / 3;
            xh[i] = cx + (int)(r * Math.cos(ang));
            yh[i] = cy + (int)(r * Math.sin(ang));
        }
        g2.setPaint(new GradientPaint(cx-r, cy-r, new Color(130, 0, 230),
                                      cx+r, cy+r, new Color(40,  0, 110)));
        g2.fillPolygon(xh, yh, 6);
        g2.setColor(new Color(185, 85, 255));
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawPolygon(xh, yh, 6);

        // Sensor central
        g2.setColor(new Color(210, 210, 255, 220));
        g2.fillOval(cx-6, cy-6, 12, 12);
        g2.setColor(new Color(100, 150, 255));
        g2.setStroke(new BasicStroke(1f));
        g2.drawOval(cx-6, cy-6, 12, 12);
    }

    // ─── MISIL ORIGINAL ──────────────────────────────────────────────────────
    public static void dibujarMisilOriginal(Graphics2D g2, int cx, int cy) {
        g2.setStroke(new BasicStroke(1.5f));

        // Cuerpo
        g2.setColor(new Color(220, 80, 0));
        g2.fillRect(cx-5, cy-22, 10, 30);

        // Punta (abajo, apunta hacia el suelo)
        int[] xp = { cx-5, cx+5, cx };
        int[] yp = { cy+8, cy+8, cy+26 };
        g2.fillPolygon(xp, yp, 3);

        // Aletas (arriba)
        g2.setColor(new Color(185, 50, 0));
        int[] xfl = { cx-5, cx-13, cx-5 };
        int[] yfl = { cy-22, cy-10, cy-4  };
        int[] xfr = { cx+5, cx+13, cx+5  };
        int[] yfr = { cy-22, cy-10, cy-4  };
        g2.fillPolygon(xfl, yfl, 3);
        g2.fillPolygon(xfr, yfr, 3);

        // Outline
        g2.setColor(new Color(150, 40, 0));
        g2.drawRect(cx-5, cy-22, 10, 30);
    }

    // ─── MISIL MODERNO ───────────────────────────────────────────────────────
    public static void dibujarMisilModerno(Graphics2D g2, int cx, int cy) {
        g2.setStroke(new BasicStroke(1f));

        // Cuerpo aerodinámico
        int[] xb = { cx-6, cx-6, cx, cx+6, cx+6 };
        int[] yb = { cy-20, cy+10, cy+28, cy+10, cy-20 };
        g2.setPaint(new GradientPaint(cx-6, cy, new Color(130, 0, 230),
                                      cx+6, cy, new Color(210, 80, 255)));
        g2.fillPolygon(xb, yb, 5);

        // Glow del cuerpo
        Composite prev = g2.getComposite();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.22f));
        g2.setColor(new Color(190, 80, 255));
        g2.fillOval(cx-12, cy-22, 24, 52);
        g2.setComposite(prev);

        // Llama en la cola (arriba)
        int[] xf1 = { cx-5, cx+5, cx };
        int[] yf1 = { cy-20, cy-20, cy-38 };
        g2.setColor(new Color(255, 120, 0, 210));
        g2.fillPolygon(xf1, yf1, 3);
        int[] xf2 = { cx-2, cx+2, cx };
        int[] yf2 = { cy-20, cy-20, cy-46 };
        g2.setColor(new Color(255, 235, 80, 160));
        g2.fillPolygon(xf2, yf2, 3);

        // Outline
        g2.setColor(new Color(200, 100, 255, 190));
        g2.setStroke(new BasicStroke(1f));
        g2.drawPolygon(xb, yb, 5);
    }

    // =========================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PantallaPersonalizacion::new);
    }
}
