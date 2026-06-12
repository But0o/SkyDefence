package skydefense;

public class Jugador {

    private String nombre;
    private int puntaje;
    private int vidas;

    public Jugador(String nombre, int vidas) {
        this.nombre = nombre;
        this.puntaje = 0;
        this.vidas = vidas;
    }

    public void sumarPuntos(int puntos) {
        this.puntaje += puntos;
    }

    public void perderVida() {
        if (this.vidas > 0) {
            this.vidas--;
        }
    }

    public void aumentarVidaExtra() {
        if (this.vidas < 5) this.vidas++;
    }

    public int getPuntaje() { return puntaje; }
    public int getVidas() { return vidas; }
    public String getNombre() { return nombre; }
}
