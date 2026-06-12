package interfas;

import skydefense.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class PantallaJuego extends JFrame {

    private static final int ANCHO    = 900;
    private static final int ALTO     = 650;
    private static final int ALTO_HUD = 65;

    public PantallaJuego() {
        setTitle("Sky Defense");
        setSize(ANCHO, ALTO);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        PanelJuego panel = new PanelJuego();
        add(panel);
        setVisible(true);
        panel.requestFocusInWindow();
    }

    // Altitude [1000, 6000] → screen Y [635, 75]  (HUD = 65px)
    static int altToScreen(double alt) {
        double clamped = Math.max(1000.0, Math.min(6000.0, alt));
        return (int)(635.0 - (clamped - 1000.0) / 5000.0 * 560.0);
    }

    static int gameXToScreen(double x) {
        return (int)(x * ANCHO / 1000.0);
    }

    // =========================================================================

    private class PanelJuego extends JPanel {

        private final ControladorJuego ctrl = new ControladorJuego("Jugador", 3);
        private final Timer   timerJuego;
        private final Random  rand = new Random();

        private boolean movIzquierda, movDerecha, subir, bajar, keySpace;
        private boolean pausado = false;

        // Auto-avance de overlay (nivel superado / game over)
        private int overlayTicks = 0;
        private static final int TICKS_OVERLAY = 300;

        // Efectos visuales
        private int flashDanio = 0;
        private final List<int[]> puntosFlotantes = new ArrayList<>(); // [sx,sy,frame,pts]
        private final List<int[]> explosionesDron  = new ArrayList<>(); // [sx,sy,frame]
        private final List<Dron>  snapshotDrones   = new ArrayList<>();
        private int vidasPrevias;
        private int energiaPrevia;

        // Estrella: [x, y, velocidad, brillo, tamaño]
        private final float[][] capa0 = new float[100][5]; // lenta – fondo
        private final float[][] capa1 = new float[60][5];  // media
        private final float[][] capa2 = new float[30][5];  // rápida – primer plano

        // Explosión: [screenX, screenY, ticksRestantes]
        private final List<int[]> explosiones = new ArrayList<>();

        // Objeto espacial: [x, y, velocidad, radio, r, g, b, alpha, tipo(0=planeta,1=nebulosa)]
        private final float[][] objEspaciales   = new float[6][9];
        private final BufferedImage[] imgObjEsp = new BufferedImage[6];

        // Overlays pre-renderizados (se calculan una sola vez)
        private BufferedImage imgScanlines;
        private BufferedImage imgViñeta;

        // Nivel jefe
        private boolean bossIntroActivo = false;
        private int     bossIntroTicks  = 0;
        private float   naveGiganteX    = -600;
        private BufferedImage imgNaveGigante;

        PanelJuego() {
            setBackground(Color.BLACK);
            initEstrellas();
            initObjEspaciales();
            initOverlays();
            imgNaveGigante = renderNaveGigante();
            vidasPrevias  = ctrl.getJugador().getVidas();
            energiaPrevia = ctrl.getAvion().getEnergia();
            bindKeys();
            timerJuego = new Timer(8, e -> tick()); // 16 ms ≈ 60 fps
            timerJuego.start();
        }

        // ── Inicialización ──────────────────────────────────────────────────

        private void initEstrellas() {
            for (float[] s : capa0) {
                s[0] = rand.nextFloat() * ANCHO;
                s[1] = rand.nextFloat() * ALTO;
                s[2] = 0.25f + rand.nextFloat() * 0.15f;
                s[3] = 40 + rand.nextInt(90);
                s[4] = 1;
            }
            for (float[] s : capa1) {
                s[0] = rand.nextFloat() * ANCHO;
                s[1] = rand.nextFloat() * ALTO;
                s[2] = 0.65f + rand.nextFloat() * 0.35f;
                s[3] = 90 + rand.nextInt(100);
                s[4] = rand.nextBoolean() ? 1 : 2;
            }
            for (float[] s : capa2) {
                s[0] = rand.nextFloat() * ANCHO;
                s[1] = rand.nextFloat() * ALTO;
                s[2] = 1.7f + rand.nextFloat() * 0.9f;
                s[3] = 160 + rand.nextInt(95);
                s[4] = rand.nextInt(3) == 0 ? 3 : 2;
            }
        }

        // ── Paletas de galaxias ──────────────────────────────────────────────
        // [galaxia][color][R,G,B]
        private static final int[][][] GALAXIA_PLANETAS = {
            {{80,120,255},{200,80,50},{80,175,100},{160,130,90},{60,160,210}},     // 0 azul
            {{180,70,255},{140,50,200},{220,90,255},{100,55,185},{200,140,255}},   // 1 púrpura
            {{255,95,45}, {230,65,25},{255,155,75},{205,75,55}, {255,125,55}},    // 2 rojo
            {{70,215,255},{95,255,235},{45,175,215},{125,240,255},{55,195,225}},   // 3 cian
            {{255,210,75},{238,175,45},{255,228,95},{218,165,55},{248,198,85}},    // 4 dorado
        };
        private static final int[][][] GALAXIA_NEBULOSAS = {
            {{150,40,200},{30,80,200},{200,80,150}},    // 0
            {{195,45,255},{145,28,218},{175,75,252}},   // 1
            {{252,75,45},{218,95,28},{252,135,75}},     // 2
            {{45,195,238},{75,252,228},{28,175,252}},   // 3
            {{252,198,45},{238,168,28},{252,218,75}},   // 4
        };
        // Gradiente de fondo: {topR,topG,topB, botR,botG,botB}
        private static final int[][] GALAXIA_FONDO = {
            {0,0,20,  0,4,32},     // 0 azul profundo
            {8,0,22,  14,4,40},    // 1 índigo profundo
            {22,4,0,  32,8,4},     // 2 rojo profundo
            {0,8,28,  0,18,38},    // 3 cian profundo
            {18,13,0, 28,18,4},    // 4 dorado profundo
        };
        // Color del título en la pantalla de transición
        private static final int[][] GALAXIA_COLOR_TITULO = {
            {100,180,255},{200,100,255},{255,120,80},{80,220,255},{255,215,80}
        };
        private static final String[] GALAXIA_NOMBRES = {
            "GALAXIA ALFA","GALAXIA BETA","GALAXIA GAMMA","GALAXIA DELTA","GALAXIA OMEGA"
        };

        private int galaxiaActual            = 0;
        private int galaxiaTransicionTicks   = 0;
        private static final int TICKS_TRANS = 220;

        private void initObjEspaciales() {
            for (int i = 0; i < objEspaciales.length; i++) {
                float[] o = objEspaciales[i];
                o[0] = rand.nextFloat() * ANCHO;
                o[1] = rand.nextFloat() * ALTO;   // distribuidos al inicio
                o[2] = 0.05f + rand.nextFloat() * 0.18f; // velocidad muy lenta
                boolean esPlaneta = i < 4;
                o[8] = esPlaneta ? 0 : 1;
                if (esPlaneta) {
                    o[3] = 22 + rand.nextInt(32);
                    colorearObjEspacial(o, true);
                    o[7] = 75 + rand.nextInt(55);
                } else {
                    o[3] = 55 + rand.nextInt(65);
                    colorearObjEspacial(o, false);
                    o[7] = 20 + rand.nextInt(25);
                }
                imgObjEsp[i] = renderObjEspacial(o);
            }
        }

        private void colorearObjEspacial(float[] o, boolean esPlaneta) {
            int g = Math.min(galaxiaActual, GALAXIA_PLANETAS.length - 1);
            int[][] palette = esPlaneta ? GALAXIA_PLANETAS[g] : GALAXIA_NEBULOSAS[g];
            int[] c = palette[rand.nextInt(palette.length)];
            o[4] = c[0]; o[5] = c[1]; o[6] = c[2];
        }

        private void regenerarObjEspaciales() {
            for (int i = 0; i < objEspaciales.length; i++) {
                float[] o = objEspaciales[i];
                boolean esPlaneta = ((int) o[8]) == 0;
                colorearObjEspacial(o, esPlaneta);
                imgObjEsp[i] = renderObjEspacial(o);
            }
        }

        private int getGalaxia() {
            return Math.min((ctrl.getNivel().getNumero() - 1) / 10,
                            GALAXIA_FONDO.length - 1);
        }

        private BufferedImage renderObjEspacial(float[] o) {
            int radius = (int) o[3];
            int r = (int) o[4], g = (int) o[5], b = (int) o[6], alpha = (int) o[7];
            int tipo = (int) o[8];
            int size = radius * 2 + 6;
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gx = img.createGraphics();
            gx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int cx = size / 2, cy = size / 2;
            if (tipo == 0) { // planeta
                Color c0 = new Color(Math.min(255,r+70), Math.min(255,g+70), Math.min(255,b+70), alpha);
                Color c1 = new Color(r, g, b, alpha * 6 / 10);
                Color c2 = new Color(0, 0, 0, 0);
                gx.setPaint(new RadialGradientPaint(cx - radius/4f, cy - radius/4f, radius,
                        new float[]{0f, 0.65f, 1f}, new Color[]{c0, c1, c2}));
                gx.fillOval(3, 3, radius * 2, radius * 2);
                // Atmósfera tenue
                gx.setColor(new Color(r, g, b, alpha / 6));
                gx.setStroke(new BasicStroke(2.5f));
                gx.drawOval(3, 3, radius * 2, radius * 2);
            } else { // nebulosa: capas concéntricas semitransparentes
                for (int ring = 4; ring >= 1; ring--) {
                    int rr = radius * ring / 4;
                    int aa = alpha * ring / 4;
                    gx.setColor(new Color(r, g, b, aa));
                    Composite prev = gx.getComposite();
                    gx.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
                    gx.fillOval(cx - rr, cy - rr, rr * 2, rr * 2);
                    gx.setComposite(prev);
                }
            }
            gx.dispose();
            return img;
        }

        private BufferedImage renderNaveGigante() {
            int w = 520, h = 150;
            BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gx = img.createGraphics();
            gx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Casco principal
            gx.setColor(new Color(50, 50, 62));
            gx.fillRect(18, 50, w - 36, 60);

            // Capa interior del casco (detalle)
            gx.setColor(new Color(70, 70, 85));
            gx.fillRect(50, 57, w - 100, 46);

            // Proa (lado derecho → avanza hacia la derecha)
            int[] xp = {w - 18, w, w - 18};
            int[] yp = {50, 80, 110};
            gx.setColor(new Color(60, 60, 75));
            gx.fillPolygon(xp, yp, 3);

            // Popa con motores (lado izquierdo)
            gx.setColor(new Color(35, 35, 45));
            gx.fillRect(18, 60, 28, 40);
            // Llamas de motor
            for (int ey : new int[]{68, 82, 96}) {
                gx.setColor(new Color(255, 110, 0, 200));
                gx.fillOval(4, ey - 5, 18, 10);
                gx.setColor(new Color(255, 215, 80, 130));
                gx.fillOval(6, ey - 3, 10, 6);
            }

            // Torretas superiores
            int[] turX = {90, 170, 265, 360, 440};
            for (int tx : turX) {
                gx.setColor(new Color(45, 45, 58));
                gx.fillRect(tx - 10, 36, 20, 18);
                gx.setColor(new Color(80, 80, 95));
                gx.fillOval(tx - 12, 29, 24, 14);
                gx.setColor(new Color(110, 110, 125));
                gx.fillRect(tx - 3, 8, 6, 24);
            }

            // Línea de acento roja
            gx.setColor(new Color(210, 25, 25, 190));
            gx.setStroke(new BasicStroke(2.5f));
            gx.drawLine(30, 80, w - 28, 80);

            // Ventanas/luces
            for (int lx = 70; lx < w - 60; lx += 28) {
                gx.setColor(new Color(180, 220, 255, 160));
                gx.fillRect(lx, 69, 6, 4);
            }

            // Contorno del casco
            gx.setColor(new Color(95, 95, 110, 160));
            gx.setStroke(new BasicStroke(1.5f));
            gx.drawRect(18, 50, w - 36, 60);

            gx.dispose();
            return img;
        }

        private void initOverlays() {
            // Scanlines: franjas negras semitransparentes cada 3px — se dibuja una sola vez
            imgScanlines = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gs = imgScanlines.createGraphics();
            gs.setColor(new Color(0, 0, 0, 15)); // alpha 15/255 ≈ 6%
            for (int y = 0; y < ALTO; y += 3) gs.fillRect(0, y, ANCHO, 1);
            gs.dispose();

            // Viñeta: oscurece los bordes — se dibuja una sola vez
            imgViñeta = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gv = imgViñeta.createGraphics();
            float radio = Math.max(ANCHO, ALTO) * 0.72f;
            gv.setPaint(new RadialGradientPaint(
                    ANCHO / 2f, ALTO / 2f, radio,
                    new float[]{0.42f, 1.0f},
                    new Color[]{new Color(0, 0, 0, 0), new Color(0, 0, 0, 180)}));
            gv.fillRect(0, 0, ANCHO, ALTO);
            gv.dispose();
        }

        private void bindKeys() {
            InputMap  im = getInputMap(WHEN_IN_FOCUSED_WINDOW);
            ActionMap am = getActionMap();

            // Flechas
            bind(im, am, KeyEvent.VK_LEFT,  false, "izqP", e -> movIzquierda = true);
            bind(im, am, KeyEvent.VK_LEFT,  true,  "izqR", e -> movIzquierda = false);
            bind(im, am, KeyEvent.VK_RIGHT, false, "derP", e -> movDerecha   = true);
            bind(im, am, KeyEvent.VK_RIGHT, true,  "derR", e -> movDerecha   = false);
            bind(im, am, KeyEvent.VK_UP,    false, "subP", e -> subir        = true);
            bind(im, am, KeyEvent.VK_UP,    true,  "subR", e -> subir        = false);
            bind(im, am, KeyEvent.VK_DOWN,  false, "bajP", e -> bajar        = true);
            bind(im, am, KeyEvent.VK_DOWN,  true,  "bajR", e -> bajar        = false);
            // WASD
            bind(im, am, KeyEvent.VK_A,     false, "aP",   e -> movIzquierda = true);
            bind(im, am, KeyEvent.VK_A,     true,  "aR",   e -> movIzquierda = false);
            bind(im, am, KeyEvent.VK_D,     false, "dP",   e -> movDerecha   = true);
            bind(im, am, KeyEvent.VK_D,     true,  "dR",   e -> movDerecha   = false);
            bind(im, am, KeyEvent.VK_W,     false, "wP",   e -> subir        = true);
            bind(im, am, KeyEvent.VK_W,     true,  "wR",   e -> subir        = false);
            bind(im, am, KeyEvent.VK_S,     false, "sP",   e -> bajar        = true);
            bind(im, am, KeyEvent.VK_S,     true,  "sR",   e -> bajar        = false);
            // Disparo
            bind(im, am, KeyEvent.VK_SPACE, false, "spP",  e -> keySpace = true);
            bind(im, am, KeyEvent.VK_SPACE, true,  "spR",  e -> keySpace = false);
            // ENTER: saltar intro boss / saltar countdown del overlay
            bind(im, am, KeyEvent.VK_ENTER, false, "enter", e -> {
                if (bossIntroActivo) bossIntroActivo = false;
                else if (ctrl.nivelSuperado() || ctrl.gameOver()) overlayTicks = TICKS_OVERLAY;
            });
            // ESC: pausar (en juego) o saltar intro boss
            bind(im, am, KeyEvent.VK_ESCAPE, false, "esc", e -> {
                if (bossIntroActivo) bossIntroActivo = false;
                else if (!ctrl.nivelSuperado() && !ctrl.gameOver()) pausado = !pausado;
            });
            // M: menú principal (solo cuando está pausado)
            bind(im, am, KeyEvent.VK_M, false, "menu", e -> {
                if (pausado) { timerJuego.stop(); PantallaJuego.this.dispose(); new PantallaInicio(); }
            });
        }

        private void bind(InputMap im, ActionMap am, int key, boolean onRelease,
                          String name, ActionListener al) {
            im.put(KeyStroke.getKeyStroke(key, 0, onRelease), name);
            am.put(name, new AbstractAction() {
                public void actionPerformed(ActionEvent e) { al.actionPerformed(e); }
            });
        }

        // ── Game loop ────────────────────────────────────────────────────────

        private void tick() {
            if (pausado) { repaint(); return; }

            scrollEstrellas();
            scrollObjEspaciales();

            if (bossIntroActivo) {
                tickBossIntro();
            } else {
                if (ctrl.esNivelBoss() && ctrl.juegoEnCurso()) scrollNaveGigante(0.35f);

                if (ctrl.juegoEnCurso()) {
                    moverJugador();

                    // Snapshot de drones ANTES del update para detectar destrucciones
                    snapshotDrones.clear();
                    snapshotDrones.addAll(ctrl.getActivosDrones());

                    if (keySpace) ctrl.dispararBala();
                    detectarYActualizarExplosiones();

                    // Drones destruidos por bala en este tick
                    for (Dron d : snapshotDrones) {
                        if (d.isDestruido()) {
                            int sx = gameXToScreen(d.getPosicionX());
                            int sy = altToScreen(d.getPosicionY());
                            explosionesDron.add(new int[]{sx, sy, 45});
                            puntosFlotantes.add(new int[]{sx, sy, 55, ControladorJuego.PUNTOS_DESTRUIR_DRON});
                        }
                    }

                    // Detectar daño para flash
                    int vidasNow   = ctrl.getJugador().getVidas();
                    int energiaNow = ctrl.getAvion().getEnergia();
                    if (vidasNow < vidasPrevias || energiaNow < energiaPrevia) flashDanio = 8;
                    vidasPrevias  = vidasNow;
                    energiaPrevia = energiaNow;
                }

                // Aging de efectos visuales
                explosionesDron.removeIf(e -> --e[2] <= 0);
                puntosFlotantes.removeIf(p -> --p[2] <= 0);
                if (flashDanio > 0) flashDanio--;

                // Auto-avance de nivel / reinicio automático
                if (ctrl.nivelSuperado() || ctrl.gameOver()) {
                    overlayTicks++;
                    if (overlayTicks >= TICKS_OVERLAY) {
                        overlayTicks = 0;
                        if (ctrl.nivelSuperado()) {
                            ctrl.avanzarNivel();
                            if (ctrl.esNivelBoss()) activarBossIntro();
                        } else {
                            ctrl.iniciarJuego();
                            bossIntroActivo = false;
                        }
                        limpiarEfectos();
                        vidasPrevias  = ctrl.getJugador().getVidas();
                        energiaPrevia = ctrl.getAvion().getEnergia();
                    }
                } else {
                    overlayTicks = 0;
                }
            }

            // Detectar cambio de galaxia
            int galNueva = getGalaxia();
            if (galNueva != galaxiaActual) {
                galaxiaActual = galNueva;
                galaxiaTransicionTicks = TICKS_TRANS;
                regenerarObjEspaciales();
            }
            if (galaxiaTransicionTicks > 0) galaxiaTransicionTicks--;

            repaint();
        }

        private void limpiarEfectos() {
            explosiones.clear();
            explosionesDron.clear();
            puntosFlotantes.clear();
            flashDanio = 0;
        }

        private void activarBossIntro() {
            bossIntroActivo = true;
            bossIntroTicks  = 480; // ~4 s a 120 fps
            naveGiganteX    = -600;
        }

        private void tickBossIntro() {
            scrollNaveGigante(1.8f);
            bossIntroTicks--;
            if (bossIntroTicks <= 0) bossIntroActivo = false;
        }

        private void scrollNaveGigante(float vel) {
            naveGiganteX += vel;
            if (naveGiganteX > ANCHO + 600) naveGiganteX = -600;
        }

        private void scrollObjEspaciales() {
            for (int i = 0; i < objEspaciales.length; i++) {
                float[] o = objEspaciales[i];
                o[1] += o[2];
                if (o[1] - o[3] > ALTO) {   // salió por abajo
                    o[1] = -o[3];
                    o[0] = rand.nextFloat() * ANCHO;
                    imgObjEsp[i] = renderObjEspacial(o); // regenera la imagen
                }
            }
        }

        private void scrollEstrellas() {
            for (float[] s : capa0) { s[1] += s[2]; if (s[1] > ALTO) { s[1] = 0; s[0] = rand.nextFloat() * ANCHO; } }
            for (float[] s : capa1) { s[1] += s[2]; if (s[1] > ALTO) { s[1] = 0; s[0] = rand.nextFloat() * ANCHO; } }
            for (float[] s : capa2) { s[1] += s[2]; if (s[1] > ALTO) { s[1] = 0; s[0] = rand.nextFloat() * ANCHO; } }
        }

        private void moverJugador() {
            Avion av = ctrl.getAvion();
            if (movIzquierda && av.getPosicionX() >= 10)  av.mover("IZQUIERDA");
            if (movDerecha   && av.getPosicionX() <= 990) av.mover("DERECHA");
            if (subir) av.variarAltitud(Math.min(av.getAltitud() + 25, Avion.getAltitudMax()));
            if (bajar) av.variarAltitud(Math.max(av.getAltitud() - 25, Avion.getAltitudMin()));
        }

        private void detectarYActualizarExplosiones() {
            List<Misil> snapshot = new ArrayList<>(ctrl.getActivosMisiles());
            ctrl.actualizarJuego();
            for (Misil m : snapshot) {
                if (m.haExplotado()) {
                    explosiones.add(new int[]{
                        gameXToScreen(m.getPosicionX()),
                        altToScreen(m.getAltitudDetonacion()),
                        18
                    });
                }
            }
            Iterator<int[]> it = explosiones.iterator();
            while (it.hasNext()) { if (--it.next()[2] <= 0) it.remove(); }
        }

        // ── Pintura ──────────────────────────────────────────────────────────

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,      RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            pintarFondo(g2);
            pintarObjEspaciales(g2);
            if (ctrl.esNivelBoss() || bossIntroActivo) pintarNaveGigante(g2);
            pintarEstrellas(g2);
            pintarCarrileroDrones(g2);
            pintarDrones(g2);
            pintarMisiles(g2);
            pintarBalas(g2);
            pintarExplosiones(g2);
            pintarExplosionesDestrDron(g2);
            pintarPuntosFlotantes(g2);
            pintarAvion(g2);
            pintarHUD(g2);
            pintarScanlines(g2);
            pintarViñeta(g2);

            if (flashDanio > 0)                  pintarFlashDanio(g2);
            if (galaxiaTransicionTicks > 0)      pintarMensajeGalaxia(g2);
            if (bossIntroActivo)                 pintarBossIntroOverlay(g2);
            if (ctrl.getEntreOleadasTicks() > 0) pintarMensajeOleada(g2);
            if (ctrl.nivelSuperado())            pintarOverlay(g2, false);
            if (ctrl.gameOver())                 pintarOverlay(g2, true);
            if (pausado)                         pintarPausa(g2);
        }

        private void pintarFondo(Graphics2D g2) {
            int g = Math.min(galaxiaActual, GALAXIA_FONDO.length - 1);
            int[] c = GALAXIA_FONDO[g];
            g2.setPaint(new GradientPaint(0, 0, new Color(c[0], c[1], c[2]), 0, ALTO, new Color(c[3], c[4], c[5])));
            g2.fillRect(0, 0, ANCHO, ALTO);
        }

        private void pintarObjEspaciales(Graphics2D g2) {
            for (int i = 0; i < objEspaciales.length; i++) {
                float[] o = objEspaciales[i];
                int cx = (int) o[0];
                int cy = (int) o[1];
                int radius = (int) o[3];
                BufferedImage img = imgObjEsp[i];
                if (img != null) g2.drawImage(img, cx - radius - 3, cy - radius - 3, null);
            }
        }

        private void pintarEstrellas(Graphics2D g2) {
            for (float[] s : capa0) {
                int b = (int) s[3];
                g2.setColor(new Color(b, b, b));
                g2.fillRect((int) s[0], (int) s[1], 1, 1);
            }
            for (float[] s : capa1) {
                int b = (int) s[3];
                g2.setColor(new Color(b, b, Math.min(255, b + 15)));
                int sz = (int) s[4];
                g2.fillOval((int) s[0], (int) s[1], sz, sz);
            }
            for (float[] s : capa2) {
                int b  = (int) s[3];
                int sz = (int) s[4];
                g2.setColor(new Color(b, b, b));
                g2.fillOval((int) s[0] - sz / 2, (int) s[1] - sz / 2, sz, sz);
                if (sz >= 3) {
                    Composite prev = g2.getComposite();
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
                    g2.setColor(Color.WHITE);
                    g2.fillOval((int) s[0] - sz - 1, (int) s[1] - sz - 1, sz * 2 + 2, sz * 2 + 2);
                    g2.setComposite(prev);
                }
            }
        }

        // Línea tenue que indica la franja donde vuelan los drones
        private void pintarCarrileroDrones(Graphics2D g2) {
            int y = altToScreen(Dron.getAltitudDron());
            Composite prev = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.08f));
            g2.setColor(new Color(255, 100, 30));
            g2.fillRect(0, y - 30, ANCHO, 60);
            g2.setComposite(prev);
            g2.setColor(new Color(255, 100, 30, 30));
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    0, new float[]{8, 8}, 0));
            g2.drawLine(0, y, ANCHO, y);
            g2.setStroke(new BasicStroke(1f));
        }

        private void pintarDrones(Graphics2D g2) {
            for (Dron d : ctrl.getActivosDrones()) {
                if (!d.estaActivo()) continue;
                int sx = gameXToScreen(d.getPosicionX());
                int sy = altToScreen(d.getPosicionY());
                Graphics2D gd = (Graphics2D) g2.create();
                gd.translate(sx, sy);
                gd.scale(0.72, 0.72);
                if (ConfiguracionSkins.skinDron == 0) PantallaPersonalizacion.dibujarDronOriginal(gd, 0, 0);
                else                                  PantallaPersonalizacion.dibujarDronModerno(gd, 0, 0);
                gd.dispose();
            }
        }

        private void pintarMisiles(Graphics2D g2) {
            for (Misil m : ctrl.getActivosMisiles()) {
                if (m.haExplotado()) continue;
                int sx = gameXToScreen(m.getPosicionX());
                int sy = altToScreen(m.getPosicionY());
                Graphics2D gm = (Graphics2D) g2.create();
                gm.translate(sx, sy);
                if (ConfiguracionSkins.skinDron == 0) PantallaPersonalizacion.dibujarMisilOriginal(gm, 0, 0);
                else                                  PantallaPersonalizacion.dibujarMisilModerno(gm, 0, 0);
                gm.dispose();
            }
        }

        private void pintarAvion(Graphics2D g2) {
            Avion av = ctrl.getAvion();
            int sx = gameXToScreen(av.getPosicionX());
            int sy = altToScreen(av.getAltitud());
            Graphics2D ga = (Graphics2D) g2.create();
            ga.translate(sx, sy);
            ga.scale(0.6, 0.6);
            if (ConfiguracionSkins.skinAvion == 0) PantallaPersonalizacion.dibujarAvionOriginal(ga, 0, 0);
            else                                   PantallaPersonalizacion.dibujarAvionModerno(ga, 0, 0);
            ga.dispose();
        }

        private void pintarExplosiones(Graphics2D g2) {
            for (int[] exp : explosiones) {
                float prog  = (18f - exp[2]) / 18f;
                int   radio = (int) (prog * 75);
                float alpha = (1f - prog) * 0.85f;
                Composite prev = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.setColor(new Color(255, 175, 55));
                g2.setStroke(new BasicStroke(3f));
                if (radio > 0) g2.drawOval(exp[0] - radio, exp[1] - radio, radio * 2, radio * 2);
                if (prog < 0.3f) {
                    g2.setColor(new Color(255, 255, 210));
                    g2.fillOval(exp[0] - 15, exp[1] - 15, 30, 30);
                }
                g2.setComposite(prev);
                g2.setStroke(new BasicStroke(1f));
            }
        }

        private void pintarHUD(Graphics2D g2) {
            g2.setPaint(new GradientPaint(0, 0, new Color(0, 0, 24), 0, ALTO_HUD, new Color(0, 5, 28)));
            g2.fillRect(0, 0, ANCHO, ALTO_HUD);
            g2.setColor(new Color(0, 180, 90, 80));
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(0, ALTO_HUD, ANCHO, ALTO_HUD);
            g2.setStroke(new BasicStroke(1f));

            Font fLbl = new Font("Monospaced", Font.BOLD, 11);
            Font fBig = new Font("Monospaced", Font.BOLD, 20);
            Color VERDE     = new Color(0, 255, 145);
            Color VERDE_DIM = new Color(0, 140, 72);
            Color ROJO      = new Color(255, 55, 55);

            Jugador jug = ctrl.getJugador();
            Avion   av  = ctrl.getAvion();

            // ── NIVEL ─────────────────────────────────────────────────────
            g2.setFont(fLbl); g2.setColor(VERDE_DIM); g2.drawString("NIVEL", 16, 20);
            g2.setFont(fBig); g2.setColor(ctrl.esNivelBoss() ? new Color(255, 140, 50) : VERDE);
            g2.drawString(String.valueOf(ctrl.getNivel().getNumero()), 16, 48);

            // ── ENERGÍA + DRONES ──────────────────────────────────────────
            int ex = 96;
            g2.setFont(fLbl); g2.setColor(VERDE_DIM); g2.drawString("ENERGÍA", ex, 20);
            int en = av.getEnergia(), barW = 140, barH = 13;
            g2.setColor(new Color(10, 10, 32)); g2.fillRoundRect(ex, 26, barW, barH, 4, 4);
            Color bc = en > 50 ? new Color(0, 215, 100) : en > 25 ? new Color(255, 175, 0) : ROJO;
            int fw = Math.max(0, (int)(barW * en / 100.0));
            if (fw > 0) { g2.setColor(bc); g2.fillRoundRect(ex, 26, fw, barH, 4, 4); }
            g2.setColor(bc.darker()); g2.setStroke(new BasicStroke(1.2f));
            g2.drawRoundRect(ex, 26, barW, barH, 4, 4); g2.setStroke(new BasicStroke(1f));
            g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g2.setColor(Color.WHITE); g2.drawString(en + "%", ex + barW + 5, 37);
            Escuadron esc = ctrl.getEscuadron();
            String drStr = ctrl.esNivelBoss()
                ? "OLEADA " + ctrl.getOleadaActual() + "/" + ctrl.getTotalOleadas() + "  |  " + esc.getDronesGenerados() + "/" + esc.getTotalDrones()
                : "DRONES: " + esc.getDronesGenerados() + "/" + esc.getTotalDrones();
            g2.setFont(fLbl); g2.setColor(ctrl.esNivelBoss() ? new Color(255, 175, 80) : VERDE_DIM);
            g2.drawString(drStr, ex, 55);

            // ── PUNTAJE ───────────────────────────────────────────────────
            int px = ANCHO / 2 - 60;
            g2.setFont(fLbl); g2.setColor(VERDE_DIM); g2.drawString("PUNTAJE", px, 20);
            g2.setFont(fBig); g2.setColor(VERDE);
            g2.drawString(String.format("%06d", jug.getPuntaje()), px, 48);

            // ── VIDAS (corazones) ─────────────────────────────────────────
            int vx = ANCHO - 165;
            g2.setFont(fLbl); g2.setColor(VERDE_DIM); g2.drawString("VIDAS", vx, 20);
            g2.setFont(new Font("Monospaced", Font.BOLD, 22));
            g2.setColor(new Color(230, 60, 80));
            StringBuilder hearts = new StringBuilder();
            for (int i = 0; i < Math.min(jug.getVidas(), 5); i++) hearts.append("♥ ");
            g2.drawString(hearts.toString().trim(), vx, 50);

            // ── Indicador de altitud (barra lateral derecha) ──────────────
            int bx = ANCHO - 20, bw = 8;
            int by = ALTO_HUD + 8, bh = ALTO - ALTO_HUD - 20;
            g2.setColor(new Color(10, 10, 36, 160)); g2.fillRoundRect(bx - 2, by, bw + 4, bh, 4, 4);
            double ratio  = (av.getAltitud() - Avion.getAltitudMin()) / (Avion.getAltitudMax() - Avion.getAltitudMin());
            int markerY = by + bh - (int)(ratio * bh);
            g2.setColor(new Color(0, 255, 145, 30)); g2.fillRoundRect(bx - 2, by, bw + 4, bh, 4, 4);
            g2.setColor(VERDE); g2.setStroke(new BasicStroke(2.2f));
            g2.drawLine(bx - 6, markerY, bx + bw + 6, markerY); g2.setStroke(new BasicStroke(1f));
            g2.setFont(new Font("Monospaced", Font.PLAIN, 8)); g2.setColor(VERDE_DIM);
            g2.drawString("5K", bx - 4, by + 9);
            g2.drawString("1K", bx - 4, by + bh);

            // ── Controles ─────────────────────────────────────────────────
            g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g2.setColor(new Color(0, 85, 50));
            g2.drawString("← → / WASD   ESPACIO disparar   ESC pausa   M menú", 14, ALTO - 8);
        }

        private void pintarMiniAvion(Graphics2D g2, int cx, int cy, int skin) {
            Graphics2D ga = (Graphics2D) g2.create();
            ga.translate(cx, cy);
            ga.scale(0.55, 0.55);
            ga.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (skin == 0) PantallaPersonalizacion.dibujarAvionOriginal(ga, 0, 0);
            else           PantallaPersonalizacion.dibujarAvionModerno(ga, 0, 0);
            ga.dispose();
        }

        private void pintarBalas(Graphics2D g2) {
            for (BalaJugador b : ctrl.getBalasJugador()) {
                if (!b.estaActiva()) continue;
                int bx = gameXToScreen(b.getPosicionX());
                int by = altToScreen(b.getPosicionY());
                g2.setColor(new Color(0, 255, 145));
                g2.setStroke(new BasicStroke(3f));
                g2.drawLine(bx, by, bx, by + 18);
                g2.setColor(Color.WHITE);
                g2.fillOval(bx - 3, by - 4, 6, 6);
                Composite prev = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.28f));
                g2.setColor(new Color(0, 255, 145));
                g2.fillOval(bx - 6, by - 6, 12, 12);
                g2.setComposite(prev);
                g2.setStroke(new BasicStroke(1f));
            }
        }

        private void pintarExplosionesDestrDron(Graphics2D g2) {
            for (int[] ex : explosionesDron) {
                float p = (45f - ex[2]) / 45f;
                int   r = (int)(14 + 90 * p);
                float alpha = (1f - p) * 0.85f;
                Composite prev = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.55f));
                RadialGradientPaint rgp = new RadialGradientPaint(
                    ex[0], ex[1], Math.max(1, r),
                    new float[]{0f, 0.4f, 1f},
                    new Color[]{Color.WHITE, new Color(100, 255, 180), new Color(0, 200, 100, 0)}
                );
                g2.setPaint(rgp);
                g2.fillOval(ex[0]-r, ex[1]-r, r*2, r*2);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.7f));
                g2.setColor(new Color(80, 255, 180));
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawOval(ex[0]-r, ex[1]-r, r*2, r*2);
                g2.setComposite(prev);
                g2.setStroke(new BasicStroke(1f));
            }
        }

        private void pintarPuntosFlotantes(Graphics2D g2) {
            for (int[] p : puntosFlotantes) {
                float prog  = (55f - p[2]) / 55f;
                float alpha = prog < 0.7f ? 1f : (1f - prog) / 0.3f;
                int   drawY = p[1] - (int)(40 * prog);
                Composite prev = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.setFont(new Font("Monospaced", Font.BOLD, 17));
                g2.setColor(new Color(255, 220, 30));
                String txt = "+" + p[3];
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(txt, p[0] - fm.stringWidth(txt) / 2, drawY);
                g2.setComposite(prev);
            }
        }

        private void pintarMensajeGalaxia(Graphics2D g2) {
            float prog  = 1f - (float) galaxiaTransicionTicks / TICKS_TRANS;
            float alpha = prog < 0.18f ? prog / 0.18f
                        : prog > 0.78f ? (1f - prog) / 0.22f
                        : 1f;
            Composite prev = g2.getComposite();

            // Fondo oscuro
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha * 0.82f));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, ALTO_HUD, ANCHO, ALTO - ALTO_HUD);

            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

            int g = Math.min(galaxiaActual, GALAXIA_COLOR_TITULO.length - 1);
            int[] ct = GALAXIA_COLOR_TITULO[g];
            Color colorTitulo = new Color(ct[0], ct[1], ct[2]);

            String nombre = galaxiaActual < GALAXIA_NOMBRES.length
                    ? GALAXIA_NOMBRES[galaxiaActual] : "GALAXIA " + (galaxiaActual + 1);

            g2.setFont(new Font("Monospaced", Font.BOLD, 44));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (ANCHO - fm.stringWidth(nombre)) / 2;
            int ty = ALTO / 2 - 5;

            // Glow
            for (int d = 5; d >= 1; d--) {
                g2.setColor(new Color(ct[0], ct[1], ct[2], 13 * (6 - d)));
                g2.drawString(nombre, tx - d, ty);
                g2.drawString(nombre, tx + d, ty);
            }
            g2.setColor(colorTitulo);
            g2.drawString(nombre, tx, ty);

            g2.setFont(new Font("Monospaced", Font.PLAIN, 16));
            FontMetrics fm2 = g2.getFontMetrics();
            String sub = "NUEVA ZONA ESPACIAL DESCUBIERTA";
            g2.setColor(new Color(180, 180, 200));
            g2.drawString(sub, (ANCHO - fm2.stringWidth(sub)) / 2, ty + 44);

            g2.setComposite(prev);
        }

        private void pintarFlashDanio(Graphics2D g2) {
            Composite prev = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, flashDanio / 8.0f * 0.38f));
            g2.setColor(new Color(255, 40, 40));
            g2.fillRect(0, ALTO_HUD, ANCHO, ALTO - ALTO_HUD);
            g2.setComposite(prev);
        }

        private void pintarPausa(Graphics2D g2) {
            Composite prev = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.68f));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, ALTO_HUD, ANCHO, ALTO - ALTO_HUD);
            g2.setComposite(prev);

            String txt = "PAUSA";
            g2.setFont(new Font("Monospaced", Font.BOLD, 52));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (ANCHO - fm.stringWidth(txt)) / 2, ty = ALTO / 2 - 20;
            for (int d = 5; d >= 1; d--) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.11f * (6-d)));
                g2.setColor(new Color(0, 255, 145));
                g2.drawString(txt, tx-d, ty); g2.drawString(txt, tx+d, ty);
            }
            g2.setComposite(prev);
            g2.setColor(new Color(0, 255, 145));
            g2.drawString(txt, tx, ty);

            g2.setFont(new Font("Monospaced", Font.PLAIN, 17));
            FontMetrics fm2 = g2.getFontMetrics();
            String op1 = "ESC  ─  CONTINUAR";
            String op2 = " M   ─  MENÚ PRINCIPAL";
            g2.setColor(new Color(0, 140, 72));
            g2.drawString(op1, (ANCHO - fm2.stringWidth(op1)) / 2, ty + 52);
            g2.setColor(new Color(180, 50, 50));
            g2.drawString(op2, (ANCHO - fm2.stringWidth(op2)) / 2, ty + 78);
        }

        private void pintarScanlines(Graphics2D g2) {
            g2.drawImage(imgScanlines, 0, 0, null);
        }

        private void pintarViñeta(Graphics2D g2) {
            g2.drawImage(imgViñeta, 0, 0, null);
        }

        private void pintarMensajeOleada(Graphics2D g2) {
            int ticks = ctrl.getEntreOleadasTicks();
            if ((ticks / 10) % 2 == 0) {
                String msg = "OLEADA " + ctrl.getOleadaActual() + " / " + ctrl.getTotalOleadas();
                g2.setFont(new Font("Monospaced", Font.BOLD, 28));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (ANCHO - fm.stringWidth(msg)) / 2;
                int ty = ALTO / 2 - 10;
                for (int d = 4; d >= 1; d--) {
                    g2.setColor(new Color(255, 130, 30, 14 * (5 - d)));
                    g2.drawString(msg, tx - d, ty); g2.drawString(msg, tx + d, ty);
                }
                g2.setColor(new Color(255, 165, 60));
                g2.drawString(msg, tx, ty);
            }
        }

        private void pintarNaveGigante(Graphics2D g2) {
            if (imgNaveGigante == null) return;
            int y = ALTO / 2 - imgNaveGigante.getHeight() / 2 - 20;
            float alpha = bossIntroActivo ? 0.80f : 0.13f;
            Composite prev = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.drawImage(imgNaveGigante, (int) naveGiganteX, y, null);
            g2.setComposite(prev);
        }

        private void pintarBossIntroOverlay(Graphics2D g2) {
            // Oscurecer fondo
            Composite prev = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, ANCHO, ALTO);
            g2.setComposite(prev);

            // Texto parpadeante cada 12 ticks
            if ((bossIntroTicks / 12) % 2 == 0) {
                String warn = "! SE ACERCA LA TROPA ENEMIGA !";
                g2.setFont(new Font("Monospaced", Font.BOLD, 30));
                FontMetrics fm = g2.getFontMetrics();
                int tx = (ANCHO - fm.stringWidth(warn)) / 2;
                int ty = ALTO / 2 - 45;
                for (int d = 5; d >= 1; d--) {
                    g2.setColor(new Color(255, 40, 40, 14 * (6 - d)));
                    g2.drawString(warn, tx - d, ty); g2.drawString(warn, tx + d, ty);
                }
                g2.setColor(new Color(255, 75, 75));
                g2.drawString(warn, tx, ty);

                g2.setFont(new Font("Monospaced", Font.PLAIN, 16));
                FontMetrics fm2 = g2.getFontMetrics();
                String info = "NIVEL " + ctrl.getNivel().getNumero()
                        + "  ──  5 OLEADAS × 10 DRONES";
                g2.setColor(new Color(255, 155, 50));
                g2.drawString(info, (ANCHO - fm2.stringWidth(info)) / 2, ty + 44);
            }

            // Hint para saltear
            g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
            g2.setColor(new Color(120, 120, 130));
            String skip = "[ ENTER ] omitir intro";
            FontMetrics fmSkip = g2.getFontMetrics();
            g2.drawString(skip, ANCHO - fmSkip.stringWidth(skip) - 18, ALTO - 12);
        }

        private void pintarOverlay(Graphics2D g2, boolean gameOver) {
            Composite prev = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.72f));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, ANCHO, ALTO);
            g2.setComposite(prev);

            String msg = gameOver ? "GAME OVER" : "NIVEL SUPERADO";
            g2.setFont(new Font("Monospaced", Font.BOLD, 54));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (ANCHO - fm.stringWidth(msg)) / 2;
            int ty = ALTO / 2 - 20;

            for (int d = 5; d >= 1; d--) {
                int alpha = 14 * (6 - d);
                g2.setColor(gameOver ? new Color(255, 30, 30, alpha) : new Color(0, 255, 140, alpha));
                g2.drawString(msg, tx - d, ty);
                g2.drawString(msg, tx + d, ty);
                g2.drawString(msg, tx, ty - d);
                g2.drawString(msg, tx, ty + d);
            }
            g2.setColor(gameOver ? new Color(255, 65, 65) : new Color(0, 255, 150));
            g2.drawString(msg, tx, ty);

            int msLeft   = (TICKS_OVERLAY - overlayTicks) * 8;
            int secsLeft = (int) Math.ceil(msLeft / 1000.0);
            String sub = gameOver
                    ? String.format("Puntaje final: %06d", ctrl.getJugador().getPuntaje())
                    : String.format("+300 PTS  ·  Continúa en %ds...  [ ENTER saltar ]", secsLeft);
            g2.setFont(new Font("Monospaced", Font.PLAIN, 15));
            FontMetrics fm2 = g2.getFontMetrics();
            g2.setColor(gameOver ? new Color(0, 200, 100) : new Color(0, 235, 120));
            g2.drawString(sub, (ANCHO - fm2.stringWidth(sub)) / 2, ty + 58);

            if (gameOver) {
                g2.setFont(new Font("Monospaced", Font.PLAIN, 14));
                FontMetrics fm3 = g2.getFontMetrics();
                String reinicia = String.format("Reiniciando en %ds...  [ ENTER saltar ]", secsLeft);
                g2.setColor(new Color(180, 80, 80));
                g2.drawString(reinicia, (ANCHO - fm3.stringWidth(reinicia)) / 2, ty + 85);
            }
        }
    }

    // =========================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PantallaJuego::new);
    }
}
