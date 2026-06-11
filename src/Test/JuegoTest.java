package Test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import skydefense.ControladorJuego;

import static org.junit.jupiter.api.Assertions.*;

class JuegoTest {

    private ControladorJuego controlador;

    @BeforeEach
    void setUp() {
        controlador = new ControladorJuego("Player1", 4);
    }

    @Test
    void testEstadoInicial() {
        System.out.println("--- Estado inicial del juego ---");
        System.out.println("  Estado:  " + controlador.getEstado());
        System.out.println("  Nivel:   " + controlador.getNivel().getNumero());
        System.out.println("  Vidas:   " + controlador.getJugador().getVidas());
        System.out.println("  Puntos:  " + controlador.getJugador().getPuntaje());

        assertEquals(ControladorJuego.Estado.EN_CURSO, controlador.getEstado());
        assertEquals(1, controlador.getNivel().getNumero());
        assertEquals(4, controlador.getJugador().getVidas());
        assertEquals(0, controlador.getJugador().getPuntaje());
    }

    @Test
    void testExplosionLejana_sumaPuntosSinDanio() {
        // Avion en x=500, altitud=3000. Misil en x=700, alt.detonacion=3000 → distancia horizontal=200 > 150
        System.out.println("--- Explosion lejana (distancia 200) ---");
        System.out.println("  Puntos antes:  " + controlador.getJugador().getPuntaje());
        System.out.println("  Energia antes: " + controlador.getAvion().getEnergia());

        controlador.calcularDanio(200.0);

        System.out.println("  Puntos despues:  " + controlador.getJugador().getPuntaje() + " (+40, zona segura)");
        System.out.println("  Energia despues: " + controlador.getAvion().getEnergia() + " (sin danio)");

        assertEquals(40, controlador.getJugador().getPuntaje());
        assertEquals(100, controlador.getAvion().getEnergia());
    }

    @Test
    void testExplosionCercana_sumaPuntosYReduceEnergia20() {
        // Distancia 100, rango 80-150
        System.out.println("--- Explosion cercana (distancia 100) ---");
        System.out.println("  Energia antes: " + controlador.getAvion().getEnergia());
        System.out.println("  Puntos antes:  " + controlador.getJugador().getPuntaje());

        controlador.calcularDanio(100.0);

        System.out.println("  Energia despues: " + controlador.getAvion().getEnergia() + " (bajo un 20%)");
        System.out.println("  Puntos despues:  " + controlador.getJugador().getPuntaje() + " (+20)");

        assertEquals(20, controlador.getJugador().getPuntaje());
        assertEquals(80, controlador.getAvion().getEnergia());
    }

    @Test
    void testExplosionMuyCercana_soloReduceEnergia40() {
        // Distancia 50, rango 20-79
        System.out.println("--- Explosion muy cercana (distancia 50) ---");
        System.out.println("  Energia antes: " + controlador.getAvion().getEnergia());

        controlador.calcularDanio(50.0);

        System.out.println("  Energia despues: " + controlador.getAvion().getEnergia() + " (bajo un 40%)");
        System.out.println("  Puntos:          " + controlador.getJugador().getPuntaje() + " (sin puntos)");

        assertEquals(0, controlador.getJugador().getPuntaje());
        assertEquals(60, controlador.getAvion().getEnergia());
    }

    @Test
    void testExplosionDirecta_pierdaVida() {
        // Distancia 5 < 20, impacto directo
        System.out.println("--- Impacto directo (distancia 5) ---");
        System.out.println("  Vidas antes:   " + controlador.getJugador().getVidas());
        System.out.println("  Energia antes: " + controlador.getAvion().getEnergia());

        controlador.getAvion().recibirDanio(40); // baja energia primero
        controlador.calcularDanio(5.0);

        System.out.println("  Vidas despues:   " + controlador.getJugador().getVidas() + " (perdio 1)");
        System.out.println("  Energia despues: " + controlador.getAvion().getEnergia() + " (se reseteo)");

        assertEquals(3, controlador.getJugador().getVidas());
        assertEquals(100, controlador.getAvion().getEnergia());
    }

    @Test
    void testVidaExtraAlSuperarMilPuntos() {
        System.out.println("--- Vida extra por puntos ---");
        System.out.println("  Vidas antes: " + controlador.getJugador().getVidas());

        // 25 explosiones lejanas × 40 puntos = 1000 puntos → vida extra
        for (int i = 0; i < 25; i++) {
            controlador.calcularDanio(200.0);
        }

        System.out.println("  Puntos: " + controlador.getJugador().getPuntaje());
        System.out.println("  Vidas despues: " + controlador.getJugador().getVidas() + " (gano 1 vida extra)");

        assertEquals(5, controlador.getJugador().getVidas());
    }

    @Test
    void testGameOverCuandoJugadorPierdeTodasLasVidas() {
        ControladorJuego ctrl = new ControladorJuego("Test", 1);

        System.out.println("--- Game Over ---");
        System.out.println("  Vidas iniciales: " + ctrl.getJugador().getVidas());
        System.out.println("  Estado antes:    " + ctrl.getEstado());

        ctrl.calcularDanio(5.0); // impacto directo → pierde la unica vida
        ctrl.verificarEstadoJugador();

        System.out.println("  Vidas despues: " + ctrl.getJugador().getVidas());
        System.out.println("  Estado final:  " + ctrl.getEstado());

        assertEquals(ControladorJuego.Estado.GAME_OVER, ctrl.getEstado());
    }

    @Test
    void testAvanzarNivelNoActuaSiJuegoEnCurso() {
        System.out.println("--- Intento de avanzar nivel en medio del juego ---");
        System.out.println("  Estado antes: " + controlador.getEstado());
        System.out.println("  Nivel antes:  " + controlador.getNivel().getNumero());

        controlador.avanzarNivel(); // no debe hacer nada

        System.out.println("  Estado despues: " + controlador.getEstado() + " (no cambio)");
        System.out.println("  Nivel despues:  " + controlador.getNivel().getNumero() + " (no avanzo)");

        assertEquals(ControladorJuego.Estado.EN_CURSO, controlador.getEstado());
        assertEquals(1, controlador.getNivel().getNumero());
    }
}
