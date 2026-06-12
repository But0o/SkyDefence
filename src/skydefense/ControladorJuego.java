package skydefense;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class ControladorJuego {

    public static final int    PUNTOS_VIDA_EXTRA     = 1000;
    public static final int    PUNTOS_SUPERAR_NIVEL  = 300;
    public static final int    PUNTOS_DESTRUIR_DRON  = 100;
    private static final double SEPARACION_MINIMA_DRON = 160.0;
    private static final double RADIO_IMPACTO_BALA     = 55.0;
    private static final int    COOLDOWN_BALA_TICKS    = 12;

    public enum Estado { EN_CURSO, NIVEL_SUPERADO, GAME_OVER }

    private Jugador jugador;
    private Nivel nivel;
    private Escuadron escuadron;
    private Avion avion;
    private List<Dron>        activosDrones;
    private List<Misil>       activosMisiles;
    private List<BalaJugador> balasJugador;
    private double velocidadDronActual;
    private double velocidadMisilActual;
    private double frecuenciaDisparoActual;

    private Estado estado;
    private int ticksDesdeUltimoDisparo;
    private int puntosUltimaVidaExtra;
    private int oleadaActual;
    private int totalOleadas;
    private int entreOleadasTicks;
    private int cooldownBala;
    private NaveBoss naveBoss = null;
    private final int vidasIniciales;
    private Random random;

    public ControladorJuego(String nombreJugador, int vidasIniciales) {
        this.vidasIniciales = vidasIniciales;
        this.jugador = new Jugador(nombreJugador, vidasIniciales);
        this.nivel = new Nivel(1);
        this.avion = new Avion(500.0, 3000.0);
        this.activosDrones  = new ArrayList<>();
        this.activosMisiles = new ArrayList<>();
        this.balasJugador   = new ArrayList<>();
        this.estado = Estado.EN_CURSO;
        this.ticksDesdeUltimoDisparo = 0;
        this.cooldownBala = 0;
        this.puntosUltimaVidaExtra = 0;
        this.random = new Random();
        actualizarVelocidades();
        setupEscuadron();
    }

    public void iniciarJuego() {
        this.jugador = new Jugador(jugador.getNombre(), vidasIniciales);
        this.nivel = new Nivel(1);
        this.avion = new Avion(500.0, 3000.0);
        this.activosDrones  = new ArrayList<>();
        this.activosMisiles = new ArrayList<>();
        this.balasJugador   = new ArrayList<>();
        this.estado = Estado.EN_CURSO;
        this.ticksDesdeUltimoDisparo = 0;
        this.cooldownBala = 0;
        this.naveBoss = null;
        this.puntosUltimaVidaExtra = 0;
        actualizarVelocidades();
        setupEscuadron();
    }

    private void setupEscuadron() {
        oleadaActual = 1;
        if (nivel.esBossLevel()) {
            totalOleadas = 5;
            escuadron = new Escuadron(10);
        } else {
            totalOleadas = 1;
            escuadron = new Escuadron(nivel.getTotalDronesNivel());
        }
    }

    public void actualizarJuego() {
        if (estado != Estado.EN_CURSO) return;
        verificarGeneracionDron();
        procesarDisparosDrones();
        actualizarPosicionesObjetos();
        procesarBalasJugador();
        if (naveBoss != null && naveBoss.estaActiva() && entreOleadasTicks == 0) {
            activosMisiles.addAll(naveBoss.tick(velocidadMisilActual, random));
        }
        verificarDetonacionesYImpacto();
        verificarEstadoJugador();
        verificarFinDeNivel();
        limpiarElementosActivos();
    }

    public void verificarGeneracionDron() {
        if (entreOleadasTicks > 0) { entreOleadasTicks--; return; }
        if (!escuadron.puedeGenerarMasDrones(activosDrones.size())) return;
        int    dir    = random.nextBoolean() ? Dron.IZQUIERDA_A_DERECHA : Dron.DERECHA_A_IZQUIERDA;
        double spawnX = dir == Dron.IZQUIERDA_A_DERECHA ? 0.0 : 1000.0;
        boolean bloqueado = activosDrones.stream()
                .anyMatch(d -> Math.abs(d.getPosicionX() - spawnX) < SEPARACION_MINIMA_DRON);
        if (!bloqueado) {
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
        for (Dron dron : activosDrones) dron.mover();
        for (Misil misil : activosMisiles) misil.mover();
        for (BalaJugador bala : balasJugador) bala.mover();
    }

    public boolean dispararBala() {
        if (cooldownBala > 0) return false;
        balasJugador.add(new BalaJugador(avion.getPosicionX(), avion.getAltitud()));
        cooldownBala = COOLDOWN_BALA_TICKS;
        return true;
    }

    public void procesarBalasJugador() {
        if (cooldownBala > 0) cooldownBala--;
        for (BalaJugador bala : balasJugador) {
            if (!bala.estaActiva()) continue;
            for (Dron dron : activosDrones) {
                if (!dron.estaActivo()) continue;
                double dx = bala.getPosicionX() - dron.getPosicionX();
                double dy = bala.getPosicionY() - dron.getPosicionY();
                if (Math.sqrt(dx * dx + dy * dy) < RADIO_IMPACTO_BALA) {
                    dron.destruir();
                    bala.desactivar();
                    jugador.sumarPuntos(PUNTOS_DESTRUIR_DRON);
                    verificarVidaExtra();
                    break;
                }
            }
        }
    }

    // Ancho y alto del área de juego en píxeles (deben coincidir con PantallaJuego)
    private static final double PANTALLA_PX_X   = 900.0;
    private static final double PANTALLA_PX_Y   = 545.0;
    private static final double RANGO_JUEGO_X   = 1000.0;
    private static final double RANGO_ALTITUD   = 5000.0;

    public void verificarDetonacionesYImpacto() {
        for (Misil misil : activosMisiles) {
            if (misil.haExplotado()) {
                // Convertir a píxeles de pantalla para que los umbrales sean intuitivos
                double dx = Math.abs(misil.getPosicionX() - avion.getPosicionX());
                double dy = Math.abs(misil.getAltitudDetonacion() - avion.getAltitud());
                double screenDx = dx * PANTALLA_PX_X / RANGO_JUEGO_X;
                double screenDy = dy * PANTALLA_PX_Y / RANGO_ALTITUD;
                double distPx = Math.sqrt(screenDx * screenDx + screenDy * screenDy);
                calcularDanio(distPx);
            }
        }
    }

    // distancia en píxeles de pantalla
    public void calcularDanio(double distancia) {
        double radio = nivel.getMultiplicadorRadio();
        double danio = nivel.getMultiplicadorDanio();
        if (distancia > 200 * radio) {
            jugador.sumarPuntos(40);
            verificarVidaExtra();
        } else if (distancia >= 110 * radio) {
            jugador.sumarPuntos(20);
            evaluarConsecuenciasImpacto((int)(20 * danio));
        } else if (distancia >= 50 * radio) {
            evaluarConsecuenciasImpacto((int)(40 * danio));
        } else {
            jugador.perderVida();
            avion.reiniciarEnergia();
            verificarVidaExtra();
        }
    }

    public void evaluarConsecuenciasImpacto(int porcentajeDanio) {
        avion.recibirDanio(porcentajeDanio);
        if (avion.getEnergia() <= 0) {
            jugador.perderVida();
            avion.reiniciarEnergia();
        }
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
            if (oleadaActual < totalOleadas) {
                oleadaActual++;
                activosDrones.clear();
                ticksDesdeUltimoDisparo = 0;
                entreOleadasTicks = 240;
                // Última oleada de nivel boss → aparece la nave boss
                if (nivel.esBossLevel() && oleadaActual == totalOleadas) {
                    escuadron = new Escuadron(0);  // sin drones normales
                    naveBoss  = new NaveBoss(calcularVidaBoss());
                } else {
                    escuadron = new Escuadron(10);
                    naveBoss  = null;
                }
            } else {
                if (naveBoss != null && naveBoss.estaActiva()) return; // boss aún vivo
                jugador.sumarPuntos(PUNTOS_SUPERAR_NIVEL);
                estado  = Estado.NIVEL_SUPERADO;
                naveBoss = null;
            }
        }
    }

    private int calcularVidaBoss() {
        return 70 + (nivel.getNumero() / 10 - 1) * 40;
    }

    public void limpiarElementosActivos() {
        activosDrones.removeIf(d -> !d.estaActivo());
        activosMisiles.removeIf(Misil::haExplotado);
        // Colisión bala → nave boss
        if (naveBoss != null && naveBoss.estaActiva()) {
            for (BalaJugador bala : balasJugador) {
                if (!bala.estaActiva()) continue;
                if (bala.getPosicionY() >= NaveBoss.ALTITUD - 300) {
                    naveBoss.recibirDanio(10);
                    bala.desactivar();
                    jugador.sumarPuntos(20);
                    verificarVidaExtra();
                }
            }
        }

        balasJugador.removeIf(b -> !b.estaActiva());
    }

    public void avanzarNivel() {
        if (estado != Estado.NIVEL_SUPERADO) return;
        nivel.avanzarNivel();
        activosDrones.clear();
        activosMisiles.clear();
        balasJugador.clear();
        cooldownBala = 0;
        naveBoss = null;
        ticksDesdeUltimoDisparo = 0;
        estado = Estado.EN_CURSO;
        actualizarVelocidades();
        setupEscuadron();
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
    public boolean esNivelBoss() { return nivel.esBossLevel(); }

    public Estado getEstado() { return estado; }
    public Jugador getJugador() { return jugador; }
    public Avion getAvion() { return avion; }
    public Nivel getNivel() { return nivel; }
    public Escuadron getEscuadron() { return escuadron; }
    public List<Dron>        getActivosDrones()  { return activosDrones; }
    public List<Misil>       getActivosMisiles() { return activosMisiles; }
    public List<BalaJugador> getBalasJugador()   { return balasJugador; }
    public NaveBoss          getNaveBoss()        { return naveBoss; }
    public int getOleadaActual()     { return oleadaActual; }
    public int getTotalOleadas()     { return totalOleadas; }
    public int getEntreOleadasTicks(){ return entreOleadasTicks; }
}
