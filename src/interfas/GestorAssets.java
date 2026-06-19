package interfas;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class GestorAssets {

    private static final String[] TEMAS = {"ember", "rose", "violet"};

    private static final BufferedImage bgNebula;
    private static final BufferedImage bgFar;
    private static final BufferedImage bgMid;
    private static final BufferedImage bgNear;

    // Menú principal
    private static final BufferedImage menuFondo;
    private static final BufferedImage menuOverlay;
    private static final BufferedImage menuLogo;
    private static final BufferedImage menuBotonJugar;
    private static final BufferedImage menuBotonSkin;
    private static final BufferedImage menuBotonSalir;

    // Selector de skins
    private static final BufferedImage selectorOverlay;
    private static final BufferedImage selectorTitulo;
    private static final BufferedImage botonElegir;
    private static final BufferedImage botonVolver;
    // cards[tema][0=normal, 1=seleccionada]
    private static final BufferedImage[][] selectorCards   = new BufferedImage[3][2];
    private static final BufferedImage[]   selectorNombres = new BufferedImage[3];

    private static final BufferedImage[] player   = new BufferedImage[3];
    private static final BufferedImage[] boss     = new BufferedImage[3];
    private static final BufferedImage[] missile  = new BufferedImage[3];
    private static final BufferedImage[] shot     = new BufferedImage[3];

    // [tipo 0-2][tema 0-2]
    private static final BufferedImage[][] drones = new BufferedImage[3][3];
    private static final String[] TIPOS_DRON = {"a", "b", "c"};

    private static final BufferedImage[] explosion = new BufferedImage[6];

    static {
        bgNebula = load("/assets/background/nebula_tile.png");
        bgFar    = load("/assets/background/stars_far.png");
        bgMid    = load("/assets/background/stars_mid.png");
        bgNear   = load("/assets/background/stars_near.png");

        menuFondo      = load("/assets/menu/pantalla_inicio_fondo.png");
        menuOverlay    = load("/assets/menu/pantalla_inicio.png");
        menuLogo       = load("/assets/menu/logo_sky_defence.png");
        menuBotonJugar = load("/assets/menu/boton_jugar.png");
        menuBotonSkin  = load("/assets/menu/boton_skin.png");
        menuBotonSalir = load("/assets/menu/boton_salir.png");

        selectorOverlay = load("/assets/menu/selector_skin.png");
        selectorTitulo  = load("/assets/menu/titulo_elegi_tu_nave.png");
        botonElegir     = load("/assets/menu/boton_elegir.png");
        botonVolver     = load("/assets/menu/boton_volver.png");

        String[] temasNombre = {"ember", "rose", "violet"};
        for (int i = 0; i < 3; i++) {
            selectorCards[i][0]  = load("/assets/menu/card_"   + temasNombre[i] + ".png");
            selectorCards[i][1]  = load("/assets/menu/card_"   + temasNombre[i] + "_sel.png");
            selectorNombres[i]   = load("/assets/menu/nombre_" + temasNombre[i] + ".png");
        }

        for (int t = 0; t < 3; t++) {
            String tema = TEMAS[t];
            player[t]  = load("/assets/player/player_"           + tema + ".png");
            boss[t]    = load("/assets/boss/boss_"               + tema + ".png");
            missile[t] = load("/assets/projectiles/missile_"     + tema + ".png");
            shot[t]    = load("/assets/projectiles/shot_"        + tema + ".png");
            for (int d = 0; d < 3; d++) {
                drones[d][t] = load("/assets/drones/drone_" + TIPOS_DRON[d] + "_" + tema + ".png");
            }
        }
        for (int i = 0; i < 6; i++) {
            explosion[i] = load("/assets/explosion/explosion_" + i + ".png");
        }
    }

    private static BufferedImage load(String path) {
        try (InputStream is = GestorAssets.class.getResourceAsStream(path)) {
            if (is == null) return null;
            return ImageIO.read(is);
        } catch (IOException e) {
            return null;
        }
    }

    public static BufferedImage getSelectorOverlay()           { return selectorOverlay; }
    public static BufferedImage getSelectorTitulo()            { return selectorTitulo; }
    public static BufferedImage getBotonElegir()               { return botonElegir; }
    public static BufferedImage getBotonVolver()               { return botonVolver; }
    public static BufferedImage getSelectorCard(int tema, boolean sel) { return selectorCards[clamp(tema)][sel ? 1 : 0]; }
    public static BufferedImage getSelectorNombre(int tema)    { return selectorNombres[clamp(tema)]; }

    public static BufferedImage getMenuFondo()      { return menuFondo; }
    public static BufferedImage getMenuOverlay()    { return menuOverlay; }
    public static BufferedImage getMenuLogo()       { return menuLogo; }
    public static BufferedImage getMenuBotonJugar() { return menuBotonJugar; }
    public static BufferedImage getMenuBotonSkin()  { return menuBotonSkin; }
    public static BufferedImage getMenuBotonSalir() { return menuBotonSalir; }

    public static BufferedImage getBackground(String capa) {
        return switch (capa) {
            case "nebula" -> bgNebula;
            case "far"    -> bgFar;
            case "mid"    -> bgMid;
            case "near"   -> bgNear;
            default       -> null;
        };
    }

    public static BufferedImage getPlayer(int tema)            { return player[clamp(tema)]; }
    public static BufferedImage getBoss(int tema)              { return boss[clamp(tema)]; }
    public static BufferedImage getMissile(int tema)           { return missile[clamp(tema)]; }
    public static BufferedImage getShot(int tema)              { return shot[clamp(tema)]; }
    public static BufferedImage getDrone(int tipo, int tema)   { return drones[tipo % 3][clamp(tema)]; }
    public static BufferedImage getExplosionFrame(int frame)   { return explosion[Math.min(5, Math.max(0, frame))]; }

    /** Dibuja img en tiles con scroll vertical sobre g2. */
    public static void drawTiledLayer(Graphics2D g2, BufferedImage img, float scrollY, float alpha, int targetW, int targetH) {
        if (img == null) return;
        int iw = img.getWidth(), ih = img.getHeight();
        Composite prev = null;
        if (alpha < 1.0f) {
            prev = g2.getComposite();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
        }
        int oy = (int)(scrollY % ih);
        for (int x = 0; x < targetW; x += iw) {
            for (int y = oy - ih; y < targetH; y += ih) {
                g2.drawImage(img, x, y, null);
            }
        }
        if (prev != null) g2.setComposite(prev);
    }

    private static int clamp(int t) { return Math.max(0, Math.min(2, t)); }
}
