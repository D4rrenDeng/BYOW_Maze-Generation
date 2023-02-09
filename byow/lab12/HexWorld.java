package byow.lab12;
import org.junit.Test;
import static org.junit.Assert.*;

import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;

import java.util.Random;

/**
 * Draws a world consisting of hexagonal regions.
 */
public class HexWorld {
    TETile[][] world;
    private static int width, height;
    private static final long SEED = 2873123;
    private static final Random RANDOM = new Random(SEED);
    private static TERenderer ter;

    public HexWorld(int s) {
        width = 11 * s - 6;
        height = 10 * s;
        // initialize tile rendering engine
        ter = new TERenderer();
        ter.initialize(width, height);
        // initialize tiles
        world = new TETile[width][height];

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                world[i][j] = Tileset.NOTHING;
            }
        }
    }

    private static TETile randomTile() {
        int tileNum = RANDOM.nextInt(5);
        switch (tileNum) {
            case 0: return Tileset.WALL;
            case 1: return Tileset.FLOWER;
            case 2: return Tileset.GRASS;
            case 3: return Tileset.FLOOR;
            case 4: return Tileset.MOUNTAIN;
            default: return Tileset.WATER;
        }
    }

    // given the coordinate of the first cell of the top middle row
    public void addHexagon(int w, int h, int s) {
        TETile cur = randomTile();
        // add the upper part of Hexagon
        for (int i = 0; i < s; i++) {
            for (int j = 0; j < 3 * s - 2 - 2 * i; j++) {
                world[w + i + j][h + i] = cur;
            }
        }
        //add the lower part of Hexagon
        for (int i = 0; i < s; i++) {
            for (int j = 0; j < 3 * s - 2 - 2 * i; j++) {
                world[w + i + j][h - 1 - i] = cur;
            }
        }
    }

    public void renderWorld() {
        ter.renderFrame(world);
    }

    public static void main(String[] args) {
        int s = 5;
        HexWorld myWorld = new HexWorld(s);
        // add the left and right most column of hexagons
        for (int i = 0; i <= 8 * s - 4; i += 8 * s - 4) {
            for (int j = 3 * s; j <= 7 * s; j += 2 * s) {
                myWorld.addHexagon(i, j, s);
            }
        }
        // add the 2nd and 4th column of hexagons
        for (int i = 2 * s - 1; i <= 6 * s - 3; i += 4 * s - 2) {
            for (int j = 2 * s; j <= 8 * s; j += 2 * s) {
                myWorld.addHexagon(i, j, s);
            }
        }
        // add the middle column of hexagon
        for (int i = s; i <= 9 * s; i += 2 * s) {
            myWorld.addHexagon(4 * s - 2, i, s);
        }
        myWorld.renderWorld();
    }
}
