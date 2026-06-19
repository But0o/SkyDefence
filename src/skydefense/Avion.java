package skydefense;

public class Avion extends ObjetoVolador {

    private static final double ALTITUD_MIN = 1000.0;
    private static final double ALTITUD_MAX = 5000.0;
    private static final int ENERGIA_MAX = 100;
    private static final double DELTA_MOVIMIENTO = 10.0;

    private int energia;

    public Avion(double posicionX, double altitud) {
        super(posicionX, altitud, DELTA_MOVIMIENTO);
        if (altitud < ALTITUD_MIN || altitud > ALTITUD_MAX) {
            throw new IllegalArgumentException("Altitud fuera de rango permitido (1000-5000).");
        }
        this.energia = ENERGIA_MAX;
    }

    @Override
    public void mover() {
        // sin dirección especificada el avión no se mueve
    }

    public void mover(Direccion dir) {
        if (dir == Direccion.IZQUIERDA) {
            posicionX -= velocidad;
        } else if (dir == Direccion.DERECHA) {
            posicionX += velocidad;
        }
    }

    public void variarAltitud(double nuevaAltitud) {
        if (nuevaAltitud < ALTITUD_MIN || nuevaAltitud > ALTITUD_MAX) {
            throw new IllegalArgumentException("Altitud fuera de rango permitido (1000-5000).");
        }
        this.posicionY = nuevaAltitud;
    }

    public void recibirDanio(int porcentaje) {
        this.energia -= porcentaje;
        if (this.energia < 0) {
            this.energia = 0;
        }
    }

    public void reiniciarEnergia() {
        this.energia = ENERGIA_MAX;
    }

    public int getEnergia() { return energia; }
    public double getAltitud() { return posicionY; }

    public static double getAltitudMin() { return ALTITUD_MIN; }
    public static double getAltitudMax() { return ALTITUD_MAX; }
}
