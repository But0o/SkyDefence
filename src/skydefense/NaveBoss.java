package skydefense;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NaveBoss {

    public static final double ALTITUD          = 5600.0;
    private static final int   FRECUENCIA_DISP  = 70;
    private static final int   MISILES_POR_SALVA = 7;

    // Ticks de gracia al aparecer: el boss no recibe daño hasta completarlos
    private static final int GRACE_TICKS = 90;

    private int vida;
    private final int vidaMax;
    private int ticksDisparo = 0;
    private int  ticksActiva = 0;
    private boolean activa   = true;

    private double posX = 500.0;
    private double velX = 0.45;

    public NaveBoss(int vida) {
        this.vida    = vida;
        this.vidaMax = vida;
    }

    public List<Misil> tick(double velMisil, Random rand) {
        List<Misil> nuevos = new ArrayList<>();
        if (!activa) return nuevos;

        ticksActiva++;

        // Movimiento horizontal oscilante
        posX += velX;
        if (posX > 780) { posX = 780; velX = -Math.abs(velX); }
        if (posX < 220) { posX = 220; velX =  Math.abs(velX); }

        ticksDisparo++;
        if (ticksDisparo >= FRECUENCIA_DISP) {
            ticksDisparo = 0;
            double espaciado = 120.0;
            int mitad = MISILES_POR_SALVA / 2;
            for (int i = -mitad; i <= mitad; i++) {
                double x = posX + i * espaciado + (rand.nextDouble() - 0.5) * 35;
                x = Math.max(20, Math.min(980, x));
                int altDet = Misil.getAltitudDetonacionMin()
                        + rand.nextInt(Misil.getAltitudDetonacionMax() - Misil.getAltitudDetonacionMin() + 1);
                nuevos.add(new Misil(x, ALTITUD, altDet, velMisil));
            }
        }
        return nuevos;
    }

    public void recibirDanio(int danio) {
        vida = Math.max(0, vida - danio);
        if (vida == 0) activa = false;
    }

    public boolean estaActiva()     { return activa; }
    public boolean estaVulnerable() { return ticksActiva > GRACE_TICKS; }
    public int    getVida()         { return vida; }
    public int    getVidaMax()      { return vidaMax; }
    public double getPosX()         { return posX; }
}
