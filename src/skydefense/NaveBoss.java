package skydefense;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class NaveBoss {

    public static final double ALTITUD          = 5600.0;
    private static final int   FRECUENCIA_DISP  = 70;   // ticks entre salvas
    private static final int   MISILES_POR_SALVA = 7;

    private int vida;
    private final int vidaMax;
    private int ticksDisparo = 0;
    private boolean activa   = true;

    public NaveBoss(int vida) {
        this.vida    = vida;
        this.vidaMax = vida;
    }

    /** Avanza un tick; devuelve los misiles que dispara esta actualización. */
    public List<Misil> tick(double velMisil, Random rand) {
        List<Misil> nuevos = new ArrayList<>();
        if (!activa) return nuevos;
        ticksDisparo++;
        if (ticksDisparo >= FRECUENCIA_DISP) {
            ticksDisparo = 0;
            double paso = 1000.0 / (MISILES_POR_SALVA + 1);
            for (int i = 1; i <= MISILES_POR_SALVA; i++) {
                double x = paso * i + (rand.nextDouble() - 0.5) * paso * 0.55;
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

    public boolean estaActiva() { return activa; }
    public int getVida()        { return vida; }
    public int getVidaMax()     { return vidaMax; }
}
