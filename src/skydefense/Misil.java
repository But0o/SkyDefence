package skydefense;

public class Misil extends ObjetoVolador {

    private static final int ALTITUD_DETONACION_MIN = 1200;
    private static final int ALTITUD_DETONACION_MAX = 4500;

    private double altitudDetonacion;
    private boolean explotado;

    public Misil(double posicionX, double altitudInicial, double altitudDetonacion, double velocidadCaida) {
        super(posicionX, altitudInicial, velocidadCaida);
        if (altitudDetonacion < ALTITUD_DETONACION_MIN || altitudDetonacion > ALTITUD_DETONACION_MAX) {
            throw new IllegalArgumentException("Altitud de detonación fuera de rango (1200-4500).");
        }
        this.altitudDetonacion = altitudDetonacion;
        this.explotado = false;
    }

    @Override
    public void mover() {
        if (explotado) return;
        posicionY -= velocidad;
        if (posicionY <= altitudDetonacion) {
            posicionY = altitudDetonacion;
            explotado = true;
        }
    }

    public double calcularDistanciaA(Avion avion) {
        double dx = Math.abs(this.posicionX - avion.getPosicionX());
        double dy = Math.abs(this.altitudDetonacion - avion.getAltitud());
        return Math.sqrt(dx * dx + dy * dy);
    }

    public boolean haExplotado() { return explotado; }
    public double getAltitudDetonacion() { return altitudDetonacion; }

    public static int getAltitudDetonacionMin() { return ALTITUD_DETONACION_MIN; }
    public static int getAltitudDetonacionMax() { return ALTITUD_DETONACION_MAX; }
}
