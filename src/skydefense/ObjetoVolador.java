package skydefense;

public abstract class ObjetoVolador {

    protected double posicionX;
    protected double posicionY;
    protected double velocidad;

    public ObjetoVolador(double posicionX, double posicionY, double velocidad) {
        this.posicionX = posicionX;
        this.posicionY = posicionY;
        this.velocidad = velocidad;
    }

    public abstract void mover();

    public double getPosicionX() { return posicionX; }
    public double getPosicionY() { return posicionY; }
    public void setVelocidad(double velocidad) { this.velocidad = velocidad; }
    public double getVelocidad() { return velocidad; }
}
