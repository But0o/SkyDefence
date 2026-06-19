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
        private final List<int[]> puntosFlotantes  = new ArrayList<>(); // [sx,sy,frame,pts]
        private final List<int[]> explosionesDron  = new ArrayList<>(); // [sx,sy,ticks]
        private final List<Dron>  snapshotDrones   = new ArrayList<>();
        private int vidasPrevias;
        private int energiaPrevia;

        // Scroll de fondo — 4 capas parallax (256×256 tiles)
        private float scrollNebula = 0, scrollFar = 0, scrollMid = 0, scrollNear = 0;

        // Explosiones de misil: [screenX, screenY, ticksRestantes] (18 ticks)
        private final List<int[]> explosiones = new ArrayList<>();

        // Variantes visuales por dron: 9 combos (3 tipos × 3 colores) en secuencia
        private static final int[][] SECUENCIA_DRONES = {
            {0,0},{1,1},{2,2},  // a-ember, b-rose, c-violet
            {0,1},{1,2},{2,0},  // a-rose,  b-violet, c-ember
            {0,2},{1,0},{2,1},  // a-violet, b-ember, c-rose
        };
        private final java.util.IdentityHashMap<Dron, int[]> variantesDrones = new java.util.IdentityHashMap<>();
        private int contadorVariante = 0;

        // Overlays pre-renderizados
        private BufferedImage imgScanlines;
        private BufferedImage imgViñeta;

        // Nivel jefe — nave de fondo con movimiento aleatorio
        private boolean bossIntroActivo  = false;
        private int     bossIntroTicks   = 0;
        private float   naveGiganteX     = -600;
        private float   naveGiganteY     = 300;
        private float   naveGiganteVelX  = 0;
        private float   naveGiganteVelY  = 0;
        private int     naveGiganteMode  = -1; // -1 = sin inicializar
        private int bossExplosionTicks = 0;

        PanelJuego() {
            setBackground(Color.BLACK);
            initOverlays();
            vidasPrevias  = ctrl.getJugador().getVidas();
            energiaPrevia = ctrl.getAvion().getEnergia();
            bindKeys();
            timerJuego = new Timer(20, e -> tick());
            timerJuego.start();
        }

        // ── Inicialización ──────────────────────────────────────────────────

        private void initOverlays() {
            imgScanlines = new BufferedImage(ANCHO, ALTO, BufferedImage.TYPE_INT_ARGB);
            Graphics2D gs = imgScanlines.createGraphics();
            gs.setColor(new Color(0, 0, 0, 15));
            for (int y = 0; y < ALTO; y += 3) gs.fillRect(0, y, ANCHO, 1);
            gs.dispose();

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

            bind(im, am, KeyEvent.VK_LEFT,   false, "izqP",  e -> movIzquierda = true);
            bind(im, am, KeyEvent.VK_LEFT,   true,  "izqR",  e -> movIzquierda = false);
            bind(im, am, KeyEvent.VK_RIGHT,  false, "derP",  e -> movDerecha   = true);
            bind(im, am, KeyEvent.VK_RIGHT,  true,  "derR",  e -> movDerecha   = false);
            bind(im, am, KeyEvent.VK_UP,     false, "subP",  e -> subir        = true);
            bind(im, am, KeyEvent.VK_UP,     true,  "subR",  e -> subir        = false);
            bind(im, am, KeyEvent.VK_DOWN,   false, "bajP",  e -> bajar        = true);
            bind(im, am, KeyEvent.VK_DOWN,   true,  "bajR",  e -> bajar        = false);
            bind(im, am, KeyEvent.VK_A,      false, "aP",    e -> movIzquierda = true);
            bind(im, am, KeyEvent.VK_A,      true,  "aR",    e -> movIzquierda = false);
            bind(im, am, KeyEvent.VK_D,      false, "dP",    e -> movDerecha   = true);
            bind(im, am, KeyEvent.VK_D,      true,  "dR",    e -> movDerecha   = false);
            bind(im, am, KeyEvent.VK_W,      false, "wP",    e -> subir        = true);
            bind(im, am, KeyEvent.VK_W,      true,  "wR",    e -> subir        = false);
            bind(im, am, KeyEvent.VK_S,      false, "sP",    e -> bajar        = true);
            bind(im, am, KeyEvent.VK_S,      true,  "sR",    e -> bajar        = false);
            bind(im, am, KeyEvent.VK_SPACE,  false, "spP",   e -> keySpace     = true);
            bind(im, am, KeyEvent.VK_SPACE,  true,  "spR",   e -> keySpace     = false);
            bind(im, am, KeyEvent.VK_ENTER,  false, "enter", e -> {
                if (bossIntroActivo) bossIntroActivo = false;
                else if (ctrl.nivelSuperado() || ctrl.gameOver()) overlayTicks = TICKS_OVERLAY;
            });
            bind(im, am, KeyEvent.VK_ESCAPE, false, "esc",   e -> {
                if (bossIntroActivo) bossIntroActivo = false;
                else if (!ctrl.nivelSuperado() && !ctrl.gameOver()) pausado = !pausado;
            });
            bind(im, am, KeyEvent.VK_M,      false, "menu",  e -> {
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

            scrollFondo();

            if (bossIntroActivo) {
                tickBossIntro();
            } else {
                if (ctrl.esNivelBoss() && ctrl.juegoEnCurso()) tickNaveGigante();

                if (ctrl.juegoEnCurso()) {
                    moverJugador();

                    snapshotDrones.clear();
                    snapshotDrones.addAll(ctrl.getActivosDrones());

                    if (keySpace) ctrl.dispararBala();
                    detectarYActualizarExplosiones();

                    for (Dron d : snapshotDrones) {
                        if (d.isDestruido()) {
                            int sx = gameXToScreen(d.getPosicionX());
                            int sy = altToScreen(d.getPosicionY());
                            explosionesDron.add(new int[]{sx, sy, 45});
                            puntosFlotantes.add(new int[]{sx, sy, 55, ControladorJuego.PUNTOS_DESTRUIR_DRON});
                        }
                    }

                    NaveBoss boss = ctrl.getNaveBoss();
                    if (boss != null && !boss.estaActiva() && bossExplosionTicks == 0) {
                        bossExplosionTicks = 90;
                    }

                    int vidasNow   = ctrl.getJugador().getVidas();
                    int energiaNow = ctrl.getAvion().getEnergia();
                    if (vidasNow < vidasPrevias || energiaNow < energiaPrevia) flashDanio = 8;
                    vidasPrevias  = vidasNow;
                    energiaPrevia = energiaNow;
                }

                explosionesDron.removeIf(e -> --e[2] <= 0);
                puntosFlotantes.removeIf(p -> --p[2] <= 0);
                if (flashDanio > 0) flashDanio--;

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

            if (bossExplosionTicks > 0) bossExplosionTicks--;

            repaint();
        }

        private void limpiarEfectos() {
            explosiones.clear();
            explosionesDron.clear();
            puntosFlotantes.clear();
            flashDanio = 0;
            variantesDrones.clear();
        }

        private void activarBossIntro() {
            bossIntroActivo = true;
            bossIntroTicks  = 480;
            naveGiganteMode = -1; // fuerza nuevo modo al próximo tick
        }

        private void tickBossIntro() {
            tickNaveGigante();
            bossIntroTicks--;
            if (bossIntroTicks <= 0) bossIntroActivo = false;
        }

        /** Movimiento aleatorio de la nave de fondo: 5 modos distintos. */
        private void tickNaveGigante() {
            int displaySize = bossIntroActivo ? 320 : 220;
            float speed     = bossIntroActivo ? 5.0f : 2.8f;

            // Detectar si la nave salió de la pantalla o no está inicializada
            boolean salida = naveGiganteMode < 0
                    || naveGiganteX > ANCHO + displaySize + 50
                    || naveGiganteX < -displaySize - 50
                    || naveGiganteY > ALTO  + displaySize + 50
                    || naveGiganteY < -displaySize - 50;

            if (salida) {
                // Elegir modo aleatorio distinto al actual
                int nuevoModo;
                do { nuevoModo = rand.nextInt(5); } while (nuevoModo == naveGiganteMode);
                naveGiganteMode = nuevoModo;

                switch (naveGiganteMode) {
                    case 0 -> { // izquierda → derecha
                        naveGiganteX    = -displaySize;
                        naveGiganteY    = ALTO_HUD + rand.nextInt(ALTO - ALTO_HUD - displaySize);
                        naveGiganteVelX = speed;
                        naveGiganteVelY = 0;
                    }
                    case 1 -> { // derecha → izquierda
                        naveGiganteX    = ANCHO;
                        naveGiganteY    = ALTO_HUD + rand.nextInt(ALTO - ALTO_HUD - displaySize);
                        naveGiganteVelX = -speed;
                        naveGiganteVelY = 0;
                    }
                    case 2 -> { // abajo → arriba
                        naveGiganteX    = rand.nextInt(ANCHO - displaySize);
                        naveGiganteY    = ALTO;
                        naveGiganteVelX = 0;
                        naveGiganteVelY = -speed;
                    }
                    case 3 -> { // arriba → abajo
                        naveGiganteX    = rand.nextInt(ANCHO - displaySize);
                        naveGiganteY    = -displaySize;
                        naveGiganteVelX = 0;
                        naveGiganteVelY = speed;
                    }
                    case 4 -> { // diagonal aleatoria desde borde aleatorio
                        float d = speed * 0.75f;
                        int lado = rand.nextInt(4);
                        naveGiganteVelX = rand.nextBoolean() ? d : -d;
                        naveGiganteVelY = rand.nextBoolean() ? d : -d;
                        switch (lado) {
                            case 0 -> { naveGiganteX = -displaySize; naveGiganteY = ALTO_HUD + rand.nextInt(ALTO - ALTO_HUD); naveGiganteVelX =  Math.abs(naveGiganteVelX); }
                            case 1 -> { naveGiganteX = ANCHO;         naveGiganteY = ALTO_HUD + rand.nextInt(ALTO - ALTO_HUD); naveGiganteVelX = -Math.abs(naveGiganteVelX); }
                            case 2 -> { naveGiganteY = -displaySize;  naveGiganteX = rand.nextInt(ANCHO); naveGiganteVelY =  Math.abs(naveGiganteVelY); }
                            case 3 -> { naveGiganteY = ALTO;           naveGiganteX = rand.nextInt(ANCHO); naveGiganteVelY = -Math.abs(naveGiganteVelY); }
                        }
                    }
                }
            } else {
                naveGiganteX += naveGiganteVelX;
                naveGiganteY += naveGiganteVelY;
            }
        }

        private void scrollFondo() {
            scrollNebula = (scrollNebula + 0.06f) % 256;
            scrollFar    = (scrollFar   + 0.20f) % 256;
            scrollMid    = (scrollMid   + 0.55f) % 256;
            scrollNear   = (scrollNear  + 1.40f) % 256;
        }

        private void moverJugador() {
            Avion av = ctrl.getAvion();
            if (movIzquierda && av.getPosicionX() >= 10)  av.mover(Direccion.IZQUIERDA);
            if (movDerecha   && av.getPosicionX() <= 990) av.mover(Direccion.DERECHA);
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
            if (ctrl.esNivelBoss() || bossIntroActivo) pintarNaveGigante(g2);
            pintarCarrileroDrones(g2);
            pintarDrones(g2);
            if (ctrl.getNaveBoss() != null) pintarNaveBoss(g2);
            pintarMisiles(g2);
            pintarBalas(g2);
            pintarExplosiones(g2);
            pintarExplosionesDestrDron(g2);
            pintarPuntosFlotantes(g2);
            pintarAvion(g2);
            pintarHUD(g2);
            pintarScanlines(g2);
            pintarViñeta(g2);

            if (bossExplosionTicks > 0)          pintarExplosionBoss(g2);
            if (ctrl.getNaveBoss() != null && ctrl.getNaveBoss().estaActiva()) pintarBarraVidaBoss(g2);
            if (flashDanio > 0)                  pintarFlashDanio(g2);
            if (bossIntroActivo)                 pintarBossIntroOverlay(g2);
            if (ctrl.getEntreOleadasTicks() > 0) pintarMensajeOleada(g2);
            if (ctrl.nivelSuperado())            pintarOverlay(g2, false);
            if (ctrl.gameOver())                 pintarOverlay(g2, true);
            if (pausado)                         pintarPausa(g2);
        }

        // ── Fondo parallax ───────────────────────────────────────────────────

        private void pintarFondo(Graphics2D g2) {
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, ANCHO, ALTO);
            GestorAssets.drawTiledLayer(g2, GestorAssets.getBackground("nebula"), scrollNebula, 0.28f, ANCHO, ALTO);
            GestorAssets.drawTiledLayer(g2, GestorAssets.getBackground("far"),    scrollFar,    0.80f, ANCHO, ALTO);
            GestorAssets.drawTiledLayer(g2, GestorAssets.getBackground("mid"),    scrollMid,    0.90f, ANCHO, ALTO);
            GestorAssets.drawTiledLayer(g2, GestorAssets.getBackground("near"),   scrollNear,   1.00f, ANCHO, ALTO);
        }

        // ── Sprites ──────────────────────────────────────────────────────────

        private Color accentColor() {
            return switch (ConfiguracionSkins.tema) {
                case 1  -> new Color(255, 110, 190);
                case 2  -> new Color(160, 80,  255);
                default -> new Color(255, 140,  50);
            };
        }

        private void pintarDrones(Graphics2D g2) {
            // Eliminar drones que ya no están activos del mapa
            variantesDrones.keySet().removeIf(d -> !ctrl.getActivosDrones().contains(d));

            for (Dron d : ctrl.getActivosDrones()) {
                if (!d.estaActivo()) continue;

                // Asignar variante al primer avistamiento del dron
                if (!variantesDrones.containsKey(d)) {
                    variantesDrones.put(d, SECUENCIA_DRONES[contadorVariante % 9]);
                    contadorVariante++;
                }

                int[] v = variantesDrones.get(d);
                BufferedImage img = GestorAssets.getDrone(v[0], v[1]);
                if (img == null) continue;
                int sx = gameXToScreen(d.getPosicionX());
                int sy = altToScreen(d.getPosicionY());
                g2.drawImage(img, sx - 32, sy - 32, 64, 64, null);
            }
        }

        private void pintarMisiles(Graphics2D g2) {
            BufferedImage img = GestorAssets.getMissile(ConfiguracionSkins.tema);
            for (Misil m : ctrl.getActivosMisiles()) {
                if (m.haExplotado()) continue;
                int sx = gameXToScreen(m.getPosicionX());
                int sy = altToScreen(m.getPosicionY());
                if (img != null) {
                    g2.drawImage(img, sx - 16, sy - 24, 32, 48, null);
                }
            }
        }

        private void pintarAvion(Graphics2D g2) {
            Avion av = ctrl.getAvion();
            int sx = gameXToScreen(av.getPosicionX());
            int sy = altToScreen(av.getAltitud());
            BufferedImage img = GestorAssets.getPlayer(ConfiguracionSkins.tema);
            if (img != null) {
                g2.drawImage(img, sx - 36, sy - 36, 72, 72, null);
            }
        }

        private void pintarBalas(Graphics2D g2) {
            BufferedImage img = GestorAssets.getShot(ConfiguracionSkins.tema);
            for (BalaJugador b : ctrl.getBalasJugador()) {
                if (!b.estaActiva()) continue;
                int bx = gameXToScreen(b.getPosicionX());
                int by = altToScreen(b.getPosicionY());
                if (img != null) {
                    g2.drawImage(img, bx - 12, by - 22, 24, 44, null);
                }
            }
        }

        private void pintarNaveBoss(Graphics2D g2) {
            NaveBoss boss = ctrl.getNaveBoss();
            if (boss == null || !boss.estaActiva()) return;
            BufferedImage img = GestorAssets.getBoss(ConfiguracionSkins.tema);
            if (img == null) return;
            int displaySize = 200;
            int sx = gameXToScreen(boss.getPosX()) - displaySize / 2;
            // Asegura que no quede cortado por el HUD (65px de alto)
            int sy = Math.max(ALTO_HUD + 10, altToScreen(NaveBoss.ALTITUD) - displaySize / 2);
            g2.drawImage(img, sx, sy, displaySize, displaySize, null);
        }

        // ── Explosiones ──────────────────────────────────────────────────────

        private void pintarExplosiones(Graphics2D g2) {
            for (int[] exp : explosiones) {
                int tick     = exp[2]; // 18 → 1
                int frameIdx = Math.min(5, (18 - tick) * 6 / 18);
                BufferedImage frame = GestorAssets.getExplosionFrame(frameIdx);
                if (frame == null) continue;
                float alpha = 0.15f + (tick / 18f) * 0.85f;
                Composite prev = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.drawImage(frame, exp[0] - 48, exp[1] - 48, 96, 96, null);
                g2.setComposite(prev);
            }
        }

        private void pintarExplosionesDestrDron(Graphics2D g2) {
            for (int[] ex : explosionesDron) {
                int tick     = ex[2]; // 45 → 1
                int frameIdx = Math.min(5, (45 - tick) * 6 / 45);
                BufferedImage frame = GestorAssets.getExplosionFrame(frameIdx);
                if (frame == null) continue;
                float alpha = tick < 15 ? tick / 15f : 1f;
                Composite prev = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.drawImage(frame, ex[0] - 64, ex[1] - 64, 128, 128, null);
                g2.setComposite(prev);
            }
        }

        // ── HUD ──────────────────────────────────────────────────────────────

        private void pintarCarrileroDrones(Graphics2D g2) {
            int y = altToScreen(Dron.getAltitudDron());
            Composite prev = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.08f));
            Color ac = accentColor();
            g2.setColor(ac);
            g2.fillRect(0, y - 30, ANCHO, 60);
            g2.setComposite(prev);
            g2.setColor(new Color(ac.getRed(), ac.getGreen(), ac.getBlue(), 30));
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL,
                    0, new float[]{8, 8}, 0));
            g2.drawLine(0, y, ANCHO, y);
            g2.setStroke(new BasicStroke(1f));
        }

        private void pintarHUD(Graphics2D g2) {
            g2.setPaint(new GradientPaint(0, 0, new Color(0, 0, 24), 0, ALTO_HUD, new Color(0, 5, 28)));
            g2.fillRect(0, 0, ANCHO, ALTO_HUD);

            Color ACENTO     = accentColor();
            Color ACENTO_DIM = ACENTO.darker().darker();
            Color ROJO       = new Color(255, 55, 55);

            g2.setColor(new Color(ACENTO.getRed(), ACENTO.getGreen(), ACENTO.getBlue(), 80));
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(0, ALTO_HUD, ANCHO, ALTO_HUD);
            g2.setStroke(new BasicStroke(1f));

            Font fLbl = new Font("Monospaced", Font.BOLD, 11);
            Font fBig = new Font("Monospaced", Font.BOLD, 20);

            Jugador jug = ctrl.getJugador();
            Avion   av  = ctrl.getAvion();

            // ── NIVEL ─────────────────────────────────────────────────────
            g2.setFont(fLbl); g2.setColor(ACENTO_DIM); g2.drawString("NIVEL", 16, 20);
            g2.setFont(fBig); g2.setColor(ctrl.esNivelBoss() ? new Color(255, 140, 50) : ACENTO);
            g2.drawString(String.valueOf(ctrl.getNivel().getNumero()), 16, 48);

            // ── ENERGÍA ───────────────────────────────────────────────────
            int ex = 96;
            g2.setFont(fLbl); g2.setColor(ACENTO_DIM); g2.drawString("ENERGÍA", ex, 20);
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
            g2.setFont(fLbl); g2.setColor(ctrl.esNivelBoss() ? new Color(255, 175, 80) : ACENTO_DIM);
            g2.drawString(drStr, ex, 55);

            // ── PUNTAJE ───────────────────────────────────────────────────
            int px = ANCHO / 2 - 60;
            g2.setFont(fLbl); g2.setColor(ACENTO_DIM); g2.drawString("PUNTAJE", px, 20);
            g2.setFont(fBig); g2.setColor(ACENTO);
            g2.drawString(String.format("%06d", jug.getPuntaje()), px, 48);

            // ── VIDAS ─────────────────────────────────────────────────────
            int vx = ANCHO - 165;
            g2.setFont(fLbl); g2.setColor(ACENTO_DIM); g2.drawString("VIDAS", vx, 20);
            g2.setFont(new Font("Monospaced", Font.BOLD, 22));
            g2.setColor(new Color(230, 60, 80));
            StringBuilder hearts = new StringBuilder();
            for (int i = 0; i < Math.min(jug.getVidas(), 5); i++) hearts.append("♥ ");
            g2.drawString(hearts.toString().trim(), vx, 50);

            // ── Indicador de altitud ──────────────────────────────────────
            int bx = ANCHO - 20, bw2 = 8;
            int by = ALTO_HUD + 8, bh = ALTO - ALTO_HUD - 20;
            g2.setColor(new Color(10, 10, 36, 160)); g2.fillRoundRect(bx - 2, by, bw2 + 4, bh, 4, 4);
            double ratio  = (av.getAltitud() - Avion.getAltitudMin()) / (Avion.getAltitudMax() - Avion.getAltitudMin());
            int markerY   = by + bh - (int)(ratio * bh);
            g2.setColor(new Color(ACENTO.getRed(), ACENTO.getGreen(), ACENTO.getBlue(), 30));
            g2.fillRoundRect(bx - 2, by, bw2 + 4, bh, 4, 4);
            g2.setColor(ACENTO); g2.setStroke(new BasicStroke(2.2f));
            g2.drawLine(bx - 6, markerY, bx + bw2 + 6, markerY); g2.setStroke(new BasicStroke(1f));
            g2.setFont(new Font("Monospaced", Font.PLAIN, 8)); g2.setColor(ACENTO_DIM);
            g2.drawString("5K", bx - 4, by + 9);
            g2.drawString("1K", bx - 4, by + bh);

            // ── Controles ─────────────────────────────────────────────────
            g2.setFont(new Font("Monospaced", Font.PLAIN, 10));
            g2.setColor(new Color(ACENTO.getRed()/3, ACENTO.getGreen()/3, ACENTO.getBlue()/3));
            g2.drawString("← → / WASD   ESPACIO disparar   ESC pausa   M menú", 14, ALTO - 8);
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

        // ── Overlays / efectos ───────────────────────────────────────────────

        private void pintarScanlines(Graphics2D g2) {
            g2.drawImage(imgScanlines, 0, 0, null);
        }

        private void pintarViñeta(Graphics2D g2) {
            g2.drawImage(imgViñeta, 0, 0, null);
        }

        private void pintarFlashDanio(Graphics2D g2) {
            Composite prev = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, flashDanio / 8.0f * 0.38f));
            g2.setColor(new Color(255, 40, 40));
            g2.fillRect(0, ALTO_HUD, ANCHO, ALTO - ALTO_HUD);
            g2.setComposite(prev);
        }

        private void pintarBarraVidaBoss(Graphics2D g2) {
            NaveBoss boss = ctrl.getNaveBoss();
            if (boss == null) return;
            float ratio = (float) boss.getVida() / boss.getVidaMax();
            int barY = ALTO_HUD + 5, barH = 14;

            g2.setColor(new Color(35, 0, 0, 210));
            g2.fillRect(0, barY, ANCHO, barH);

            Color bc = ratio > 0.5f ? new Color(215, 45, 45)
                     : ratio > 0.25f ? new Color(255, 120, 30) : new Color(255, 200, 30);
            int fw = (int)(ANCHO * ratio);
            if (fw > 0) { g2.setColor(bc); g2.fillRect(0, barY, fw, barH); }

            g2.setColor(new Color(200, 45, 45, 145));
            g2.setStroke(new BasicStroke(1f));
            g2.drawRect(0, barY, ANCHO - 1, barH);

            g2.setFont(new Font("Monospaced", Font.BOLD, 10));
            g2.setColor(Color.WHITE);
            g2.drawString("NAVE BOSS  HP: " + boss.getVida() + "/" + boss.getVidaMax(), 10, barY + barH - 2);
        }

        private void pintarExplosionBoss(Graphics2D g2) {
            float prog  = 1f - (float) bossExplosionTicks / 90f;
            int   bossY = altToScreen(NaveBoss.ALTITUD);
            Composite prev = g2.getComposite();

            if (prog < 0.18f) {
                float a = (0.18f - prog) / 0.18f;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a * 0.85f));
                g2.setColor(new Color(255, 200, 100));
                g2.fillRect(0, ALTO_HUD, ANCHO, ALTO - ALTO_HUD);
            }

            for (int ring = 0; ring < 5; ring++) {
                float rProg = Math.min(1f, prog + ring * 0.14f);
                int   r     = (int)(rProg * 480);
                float alpha = (1f - rProg) * (1f - prog) * 0.75f;
                if (alpha <= 0) continue;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.setColor(new Color(255, 140 + ring * 18, ring * 25));
                g2.setStroke(new BasicStroke(5f - ring * 0.6f));
                g2.drawOval(ANCHO/2 - r, bossY - r/2, r * 2, r);
                g2.setStroke(new BasicStroke(1f));
            }
            g2.setComposite(prev);
        }

        private void pintarNaveGigante(Graphics2D g2) {
            BufferedImage img = GestorAssets.getBoss(ConfiguracionSkins.tema);
            if (img == null) return;
            int displaySize = bossIntroActivo ? 320 : 220;
            float alpha     = bossIntroActivo ? 0.85f : 0.14f;
            Composite prev  = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.drawImage(img, (int) naveGiganteX, (int) naveGiganteY, displaySize, displaySize, null);
            g2.setComposite(prev);
        }

        private void pintarBossIntroOverlay(Graphics2D g2) {
            Composite prev = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.55f));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, 0, ANCHO, ALTO);
            g2.setComposite(prev);

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

            g2.setFont(new Font("Monospaced", Font.PLAIN, 12));
            g2.setColor(new Color(120, 120, 130));
            String skip = "[ ENTER ] omitir intro";
            FontMetrics fmSkip = g2.getFontMetrics();
            g2.drawString(skip, ANCHO - fmSkip.stringWidth(skip) - 18, ALTO - 12);
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
                g2.drawString(msg, tx - d, ty); g2.drawString(msg, tx + d, ty);
                g2.drawString(msg, tx, ty - d); g2.drawString(msg, tx, ty + d);
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

        private void pintarPausa(Graphics2D g2) {
            Composite prev = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.68f));
            g2.setColor(Color.BLACK);
            g2.fillRect(0, ALTO_HUD, ANCHO, ALTO - ALTO_HUD);
            g2.setComposite(prev);

            Color ac = accentColor();
            String txt = "PAUSA";
            g2.setFont(new Font("Monospaced", Font.BOLD, 52));
            FontMetrics fm = g2.getFontMetrics();
            int tx = (ANCHO - fm.stringWidth(txt)) / 2, ty = ALTO / 2 - 20;
            for (int d = 5; d >= 1; d--) {
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.11f * (6-d)));
                g2.setColor(ac);
                g2.drawString(txt, tx-d, ty); g2.drawString(txt, tx+d, ty);
            }
            g2.setComposite(prev);
            g2.setColor(ac);
            g2.drawString(txt, tx, ty);

            g2.setFont(new Font("Monospaced", Font.PLAIN, 17));
            FontMetrics fm2 = g2.getFontMetrics();
            String op1 = "ESC  ─  CONTINUAR";
            String op2 = " M   ─  MENÚ PRINCIPAL";
            g2.setColor(ac.darker());
            g2.drawString(op1, (ANCHO - fm2.stringWidth(op1)) / 2, ty + 52);
            g2.setColor(new Color(180, 50, 50));
            g2.drawString(op2, (ANCHO - fm2.stringWidth(op2)) / 2, ty + 78);
        }

    }

    // =========================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PantallaJuego::new);
    }
}
