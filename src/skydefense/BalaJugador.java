package skydefense;

public class BalaJugador extends ObjetoVolador {

    private static final double VELOCIDAD_BALA  = 160.0;
    private static final double ALTITUD_SALIDA  = Dron.getAltitudDron() + 500.0;

    private boolean activa;

    public BalaJugador(double posicionX, double altitudInicial) {
        super(posicionX, altitudInicial, VELOCIDAD_BALA);
        this.activa = true;
    }

    @Override
    public void mover() {
        if (!activa) return;
        posicionY += velocidad;
        if (posicionY >= ALTITUD_SALIDA) activa = false;
    }

    public boolean estaActiva() { return activa; }
    public void desactivar()    { activa = false; }
}
