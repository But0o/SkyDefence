package Test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import skydefense.Jugador;

import static org.junit.jupiter.api.Assertions.*;

class JugadorTest {

    private Jugador jugador;

    @BeforeEach
    void setUp() {
        jugador = new Jugador("Player1", 4);
    }

    @Test
    void testCreacionConValoresIniciales() {
        System.out.println("--- Creacion del jugador ---");
        System.out.println("  Nombre:  " + jugador.getNombre());
        System.out.println("  Puntaje: " + jugador.getPuntaje());
        System.out.println("  Vidas:   " + jugador.getVidas());

        assertEquals("Player1", jugador.getNombre());
        assertEquals(0, jugador.getPuntaje());
        assertEquals(4, jugador.getVidas());
    }

    @Test
    void testSumarPuntos() {
        System.out.println("--- Sumar puntos ---");
        System.out.println("  Puntaje inicial: " + jugador.getPuntaje());

        jugador.sumarPuntos(40);
        System.out.println("  Puntaje tras +40: " + jugador.getPuntaje());
        assertEquals(40, jugador.getPuntaje());

        jugador.sumarPuntos(20);
        System.out.println("  Puntaje tras +20: " + jugador.getPuntaje());
        assertEquals(60, jugador.getPuntaje());
    }

    @Test
    void testPerderVida() {
        System.out.println("--- Perder vida ---");
        System.out.println("  Vidas antes: " + jugador.getVidas());

        jugador.perderVida();

        System.out.println("  Vidas despues: " + jugador.getVidas());
        assertEquals(3, jugador.getVidas());
    }

    @Test
    void testPerderVidaNoVaMenosDeCero() {
        Jugador jugadorUnaVida = new Jugador("Test", 1);
        jugadorUnaVida.perderVida();
        jugadorUnaVida.perderVida(); // no deberia ir a -1

        System.out.println("--- Vidas no bajan de 0 ---");
        System.out.println("  Vidas: " + jugadorUnaVida.getVidas());

        assertEquals(0, jugadorUnaVida.getVidas());
    }

    @Test
    void testAumentarVidaExtra() {
        System.out.println("--- Vida extra ---");
        System.out.println("  Vidas antes: " + jugador.getVidas());

        jugador.aumentarVidaExtra();

        System.out.println("  Vidas despues: " + jugador.getVidas());
        assertEquals(5, jugador.getVidas());
    }
}
