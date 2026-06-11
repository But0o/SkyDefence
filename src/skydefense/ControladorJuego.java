package skydefense;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ControladorJuego {

    public static final int PUNTOS_VIDA_EXTRA = 1000;
    public static final int PUNTOS_SUPERAR_NIVEL = 300;

    public enum Estado { EN_CURSO, NIVEL_SUPERADO, GAME_OVER }

    private Jugador jugador;
    private Nivel nivel;
    private Escuadron escuadron;
    private Avion avion;
    private List<Dron> activosDrones;
    private List<Misil> activosMisiles;
    private double velocidadDronActual;
    private double velocidadMisilActual;
    private double frecuenciaDisparoActual;

    private Estado estado;
    private int ticksDesdeUltimoDisparo;
    private int puntosUltimaVidaExtra;
    private Random random;

    public ControladorJuego(String nombreJugador, int vidasIniciales) {
        this.jugador = new Jugador(nombreJugador, vidasIniciales);
        this.nivel = new Nivel(1);
        this.escuadron = new Escuadron();
        this.avion = new Avion(500.0, 3000.0);
        this.activosDrones = new ArrayList<>();
        this.activosMisiles = new ArrayList<>();
        this.estado = Estado.EN_CURSO;
        this.ticksDesdeUltimoDisparo = 0;
        this.puntosUltimaVidaExtra = 0;
        this.random = new Random();
        actualizarVelocidades();
    }

    public void iniciarJuego() {
        this.jugador = new Jugador(jugador.getNombre(), jugador.getVidas());
        this.nivel = new Nivel(1);
        this.escuadron = new Escuadron();
        this.avion = new Avion(500.0, 3000.0);
        this.activosDrones = new ArrayList<>();
        this.activosMisiles = new ArrayList<>();
        this.estado = Estado.EN_CURSO;
        this.ticksDesdeUltimoDisparo = 0;
        this.puntosUltimaVidaExtra = 0;
        actualizarVelocidades();
    }

    public void actualizarJuego() {
        if (estado != Estado.EN_CURSO) return;
        verificarGeneracionDron();
        procesarDisparosDrones();
        actualizarPosicionesObjetos();
        verificarDetonacionesYImpacto();
        verificarEstadoJugador();
        verificarFinDeNivel();
        limpiarElementosActivos();
    }

    public void verificarGeneracionDron() {
        if (escuadron.puedeGenerarMasDrones(activosDrones.size())) {
            int dir = random.nextBoolean() ? Dron.IZQUIERDA_A_DERECHA : Dron.DERECHA_A_IZQUIERDA;
            Dron nuevoDron = new Dron(dir, velocidadDronActual);
            escuadron.registrarGeneracionDron(nuevoDron);
            activosDrones.add(nuevoDron);
        }
    }

    public void procesarDisparosDrones() {
        ticksDesdeUltimoDisparo++;
        if (ticksDesdeUltimoDisparo >= (int) frecuenciaDisparoActual) {
            for (Dron dron : activosDrones) {
                Misil misil = dron.lanzarMisil(velocidadMisilActual);
                activosMisiles.add(misil);
            }
            ticksDesdeUltimoDisparo = 0;
        }
    }

    public void actualizarPosicionesObjetos() {
        for (Dron dron : activosDrones) {
            dron.mover();
        }
        for (Misil misil : activosMisiles) {
            misil.mover();
        }
    }

    public void verificarDetonacionesYImpacto() {
        for (Misil misil : activosMisiles) {
            if (misil.haExplotado()) {
                double distancia = misil.calcularDistanciaA(avion);
                calcularDanio(distancia);
            }
        }
    }

    public void calcularDanio(double distancia) {
        if (distancia > 150) {
            jugador.sumarPuntos(40);
            verificarVidaExtra();
        } else if (distancia >= 80) {
            jugador.sumarPuntos(20);
            evaluarConsecuenciasImpacto(20);
        } else if (distancia >= 20) {
            evaluarConsecuenciasImpacto(40);
        } else {
            jugador.perderVida();
            avion.reiniciarEnergia();
            verificarVidaExtra();
        }
    }

    public void evaluarConsecuenciasImpacto(int porcentajeDanio) {
        avion.recibirDanio(porcentajeDanio);
        verificarVidaExtra();
    }

    public void verificarEstadoJugador() {
        if (jugador.getVidas() <= 0) {
            estado = Estado.GAME_OVER;
        }
    }

    public void verificarFinDeNivel() {
        if (estado != Estado.EN_CURSO) return;
        boolean todosMisilesExplotados = activosMisiles.stream().allMatch(Misil::haExplotado);
        if (escuadron.escuadronTerminado() && todosMisilesExplotados) {
            jugador.sumarPuntos(PUNTOS_SUPERAR_NIVEL);
            estado = Estado.NIVEL_SUPERADO;
        }
    }

    public void limpiarElementosActivos() {
        activosDrones.removeIf(d -> !d.estaActivo());
        activosMisiles.removeIf(Misil::haExplotado);
    }

    public void avanzarNivel() {
        if (estado != Estado.NIVEL_SUPERADO) return;
        nivel.avanzarNivel();
        escuadron = new Escuadron();
        activosDrones.clear();
        activosMisiles.clear();
        ticksDesdeUltimoDisparo = 0;
        estado = Estado.EN_CURSO;
        actualizarVelocidades();
    }

    private void actualizarVelocidades() {
        this.velocidadDronActual = nivel.getVelocidadDron();
        this.velocidadMisilActual = nivel.getVelocidadMisil();
        this.frecuenciaDisparoActual = nivel.getFrecuenciaDisparo();
    }

    private void verificarVidaExtra() {
        int puntosActuales = jugador.getPuntaje();
        int millaresNuevos = puntosActuales / PUNTOS_VIDA_EXTRA;
        int millaresAnteriores = puntosUltimaVidaExtra / PUNTOS_VIDA_EXTRA;
        if (millaresNuevos > millaresAnteriores) {
            jugador.aumentarVidaExtra();
            puntosUltimaVidaExtra = puntosActuales;
        }
    }

    public boolean juegoEnCurso() { return estado == Estado.EN_CURSO; }
    public boolean nivelSuperado() { return estado == Estado.NIVEL_SUPERADO; }
    public boolean gameOver() { return estado == Estado.GAME_OVER; }

    public Estado getEstado() { return estado; }
    public Jugador getJugador() { return jugador; }
    public Avion getAvion() { return avion; }
    public Nivel getNivel() { return nivel; }
    public Escuadron getEscuadron() { return escuadron; }
    public List<Dron> getActivosDrones() { return activosDrones; }
    public List<Misil> getActivosMisiles() { return activosMisiles; }
}
