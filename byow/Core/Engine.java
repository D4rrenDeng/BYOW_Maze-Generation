package byow.Core;

import byow.TileEngine.TERenderer;
import byow.TileEngine.TETile;
import byow.TileEngine.Tileset;

import javax.swing.*;
import javax.swing.text.Position;
import java.util.*;

public class Engine {
    TERenderer ter = new TERenderer();
    /* Feel free to change the width and height. */
    public static final int WIDTH = 79;
    public static final int HEIGHT = 29;
    public static final int LIMIT = 80;
    private static TETile[][] world;
    private static TETile[][] roomPositions;
    private static boolean[][] visited;
    private long SEED;
    private Random random;
    private static ArrayList<Room> rooms;

    /**
     * Method used for exploring a fresh world. This method should handle all inputs,
     * including inputs from the main menu.
     */
    public void interactWithKeyboard() {
    }

    /**
     * Method used for autograding and testing your code. The input string will be a series
     * of characters (for example, "n123sswwdasdassadwas", "n123sss:q", "lwww". The engine should
     * behave exactly as if the user typed these characters into the engine using
     * interactWithKeyboard.
     *
     * Recall that strings ending in ":q" should cause the game to quite save. For example,
     * if we do interactWithInputString("n123sss:q"), we expect the game to run the first
     * 7 commands (n123sss) and then quit and save. If we then do
     * interactWithInputString("l"), we should be back in the exact same state.
     *
     * In other words, both of these calls:
     *   - interactWithInputString("n123sss:q")
     *   - interactWithInputString("lww")
     *
     * should yield the exact same world state as:
     *   - interactWithInputString("n123sssww")
     *
     * @param input the input string to feed to your program
     * @return the 2D TETile[][] representing the state of the world
     */
    public TETile[][] interactWithInputString(String input) {
        // TODO: Fill out this method so that it run the engine using the input
        // passed in as an argument, and return a 2D tile representation of the
        // world that would have been drawn if the same inputs had been given
        // to interactWithKeyboard().
        //
        // See proj3.byow.InputDemo for a demo of how you can make a nice clean interface
        // that works for many different input types.
        SEED = Integer.parseInt(input);
        random = new Random(SEED);
        ter.initialize(WIDTH, HEIGHT);
        world = new TETile[WIDTH][HEIGHT];
        roomPositions = new TETile[WIDTH][HEIGHT];
        visited = new boolean[WIDTH][HEIGHT];
        rooms = new ArrayList<>();

        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                world[i][j] = Tileset.NOTHING;
                roomPositions[i][j] = Tileset.NOTHING;
            }
        }
        buildBaseMap();
        fillWorldWithRooms();
        Location startLocation = findStartingLocation();
        // find starting point to generate the maze
        generateHallWays(startLocation);
        connectRoomToHallway();
        cleanWorld();
        return world;
    }

    /* build a maze as base map using Prim algorithm, the idea was inspired by BoL0150
    (https://blog.csdn.net/qq_45698833/article/details/115276316)
     */
    public void buildBaseMap() {
        // initialize world with WALLS
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                world[i][j] = Tileset.WALL;
                roomPositions[i][j] = Tileset.NOTHING;
            }
        }
        // initialize crossing walls
        for (int i = 1; i < WIDTH - 1; i += 2) {
            for (int j = 1; j < HEIGHT - 1; j += 2) {
                world[i][j] = Tileset.NOTHING;
            }
        }
    }

    public Location findStartingLocation() {
        while (true) {
            int startX = random.nextInt(WIDTH);
            int startY = random.nextInt(HEIGHT);
            if (world[startX][startY].equals(Tileset.NOTHING)) {
                return new Location(startX, startY);
            }
        }
    }

    public void generateHallWays(Location location) {
        List<Location> locations = new LinkedList<>();

        locations.add(location);
        while (!locations.isEmpty()) {
            int index = random.nextInt(locations.size());
            Location cur = locations.get(index);
            int curX = cur.x;
            int curY = cur.y;
            // mark the current location as floor
            world[curX][curY] = Tileset.FLOOR;
            exploreSurrundingLocations(cur, locations);
            locations.remove(index);
        }
    }

    public void exploreSurrundingLocations(Location location, List<Location> locations) {
        int[][] directions = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};
        /* randomly check the surrounding locations and add them to the list if not visited yet and mark them as GRASS, it has
        to be made sure that all the locations are visited
         */
        // mark for each direction
        boolean[] visited = new boolean[4];
        // can only connect to one neighbor otherwise there won't be any wall left when finished
        boolean connected = false;
        while (visited[0] == false || visited[1] == false || visited[2] == false || visited[3] == false) {
            int index = random.nextInt(4);
            if (visited[index]) {
                continue;
            }
            visited[index] = true;
            int nextX = location.x + directions[index][0];
            int nextY = location.y + directions[index][1];
            // skip if visited or out of bounds
            if (nextX < 0 || nextY < 0 || nextX >= WIDTH || nextY >= HEIGHT) {
                continue;
            }
            if (world[nextX][nextY].equals(Tileset.NOTHING)) {
                world[nextX][nextY] = Tileset.GRASS;
                locations.add(new Location(nextX, nextY));
            }
            if (world[nextX][nextY].equals(Tileset.FLOOR) && !connected) {
                world[(location.x + nextX) / 2][(location.y + nextY) / 2] = Tileset.FLOOR;
                connected = true;
            }
        }
    }

    public void connectRoomToHallway() {
        for (int i = 0; i < rooms.size(); i++) {
            removeWall(rooms.get(i));
        }
    }

    public void removeWall(Room cur) {
        while (true) {
            int direction = random.nextInt(4);
            int wallX, wallY;

            switch(direction) {
                // remove the wall on the left side
                case 0:
                    wallX = cur.x;
                    wallY = cur.y + random.nextInt(cur.height) + 1;
                    if (!removable(wallX, wallY, direction)) {
                        continue;
                    }
                    world[wallX][wallY] = Tileset.FLOOR;
                    /* check if removing this wall connects the room to the hallway
                     (hallway is FLOOR, and room is filled with FLOWER for now)
                     */
                    // if it connects to hallway, then we are finished with this room
                    if (world[wallX - 1][wallY].equals(Tileset.FLOOR)) {
                        return;
                    }
                    world[wallX - 1][wallY] = Tileset.FLOOR;
                    // if it connects to another room, we need to keep removing walls, there's 2 walls between rooms
                    if (world[wallX - 2][wallY].equals(Tileset.FLOWER)) {
                        continue;
                    }
                    return;
                // remove the wall on the top side
                case 1:
                    wallX = cur.x + random.nextInt(cur.width) + 1;
                    wallY = cur.y + cur.height + 1;
                    if (!removable(wallX, wallY, direction)) {
                        continue;
                    }
                    world[wallX][wallY] = Tileset.FLOOR;
                    /* check if removing this wall connects the room to the hallway
                     (hallway is FLOOR, and room is filled with FLOWER for now)
                     */
                    // if it connects to hallway, then we are finished with this room
                    if (world[wallX][wallY + 1].equals(Tileset.FLOOR)) {
                        return;
                    }
                    world[wallX][wallY + 1] = Tileset.FLOOR;
                    // if it connects to another room, we need to keep removing walls, there's 2 walls between rooms
                    if (world[wallX][wallY + 2].equals(Tileset.FLOWER)) {
                        continue;
                    }
                    return;
                // remove the wall on the right side
                case 2:
                    wallX = cur.x + cur.width + 1;
                    wallY = cur.y + random.nextInt(cur.height) + 1;
                    if (!removable(wallX, wallY, direction)) {
                        continue;
                    }
                    world[wallX][wallY] = Tileset.FLOOR;
                    /* check if removing this wall connects the room to the hallway
                     (hallway is FLOOR, and room is filled with FLOWER for now)
                     */
                    // if it connects to hallway, then we are finished with this room
                    if (world[wallX + 1][wallY].equals(Tileset.FLOOR)) {
                        return;
                    }
                    world[wallX + 1][wallY] = Tileset.FLOOR;
                    // if it connects to another room, we need to keep removing walls, there's 2 walls between rooms
                    if (world[wallX + 2][wallY].equals(Tileset.FLOWER)) {
                        continue;
                    }
                    return;
                // remove the wall on the bottom side
                case 3:
                    wallX = cur.x + random.nextInt(cur.width) + 1;
                    wallY = cur.y;
                    if (!removable(wallX, wallY, direction)) {
                        continue;
                    }
                    world[wallX][wallY] = Tileset.FLOOR;
                    /* check if removing this wall connects the room to the hallway
                     (hallway is FLOOR, and room is filled with FLOWER for now)
                     */
                    // if it connects to hallway, then we are finished with this room
                    if (world[wallX][wallY - 1].equals(Tileset.FLOOR)) {
                        return;
                    }
                    world[wallX][wallY - 1] = Tileset.FLOOR;
                    // if it connects to another room, we need to keep removing walls, there's 2 walls between rooms
                    if (world[wallX][wallY - 2].equals(Tileset.FLOWER)) {
                        continue;
                    }
                    return;
            }
        }
    }

    public boolean removable(int x, int y, int direction) {
        // check if the current wall has already been removed
        if (world[x][y].equals(Tileset.FLOOR)) {
            return false;
        }
        switch (direction) {
            // remove towards left
            case 0:
                // if there isn't a hallway or room separated by 2 walls or it goes out of bound, do remove wall
                if (x < 2 || world[x - 2][y].equals(Tileset.WALL)) {
                    return false;
                }
                return true;
            // remove upwards
            case 1:
                // if there isn't a hallway or room separated by 2 walls or it goes out of bound, do remove wall
                if (y > HEIGHT - 3 || world[x][y + 2].equals(Tileset.WALL)) {
                    return false;
                }
                return true;
            // remove towards right
            case 2:
                // if there isn't a hallway or room separated by 2 walls or it goes out of bound, do remove wall
                if (x > WIDTH - 3 || world[x + 2][y].equals(Tileset.WALL)) {
                    return false;
                }
                return true;
            // remove downwards
            case 3:
                // if there isn't a hallway or room separated by 2 walls or it goes out of bound, do remove wall
                if (y < 2 || world[x][y - 2].equals(Tileset.WALL)) {
                    return false;
                }
                return true;
        }
        return true;
    }

    /* Step 1: Itrerate through all the cells and remove the flowers in the room and fill the hallways that lead to deadends with walls
    Step 2: Remove all the extra walls
     */
    public void cleanWorld() {
        int[][] directions = {
                {-1, 0},
                {1, 0},
                {0, -1},
                {0, 1},
                {-1, 1},
                {-1, -1},
                {1, 1},
                {1, -1},
        };
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                if (world[i][j].equals(Tileset.FLOWER)) {
                    world[i][j] = Tileset.FLOOR;
                    visited[i][j] = true;
                }
                /* we use a DFS logic to remove hallways, so we always start from the end of the hallway
                so that we can fill the hallwqy that leads to the deadend from the end to the start
                 */
                if (world[i][j].equals(Tileset.FLOOR)) {
                    int wallCount = 0;
                    for (int d = 0; d < 4; d++) {
                        int newX = i + directions[d][0];
                        int newY = j + directions[d][1];

                        if (newX < 0 || newX >= WIDTH || newY < 0 || newY >= HEIGHT) {
                            continue;
                        }
                        if (world[newX][newY].equals(Tileset.WALL)) {
                            wallCount += 1;
                        }
                    }
                    if (wallCount >= 3) {
                        dfs(i, j);
                    }
                }
            }
        }
        // Remove extra walls
        for (int i = 0; i < WIDTH; i++) {
            for (int j = 0; j < HEIGHT; j++) {
                int floorCount = 0;
                if (!world[i][j].equals(Tileset.WALL)) {
                    continue;
                }
                for (int d = 0; d < 8; d++) {
                    int newX = i + directions[d][0];
                    int newY = j + directions[d][1];
                    if (newX < 0 || newX >= WIDTH || newY < 0 || newY >= HEIGHT) {
                        continue;
                    }
                    if (world[newX][newY].equals(Tileset.FLOOR)) {
                        floorCount += 1;
                    }

                }
                if (floorCount == 0) {
                    world[i][j] = Tileset.NOTHING;
                }
            }
        }
    }

    /* explore the current tile in the DFS manner, check if it's a dead-end (replace with wall if so)
    and check it again after exploring its passable neighbors
     */
    public boolean dfs(int x, int y) {
        int[][] directions = {
                {-1, 0},
                {1, 0},
                {0, -1},
                {0, 1},
        };
        Queue<Location> path = new LinkedList<>();
        int wallCount = 0;

        for (int i = 0; i < 4; i++) {
            int nextX = x + directions[i][0];
            int nextY = y + directions[i][1];
            // check if out of bound
            if (nextX < 0 || nextX >= WIDTH || nextY < 0 || nextY >= HEIGHT) {
                continue;
            }
            if (world[nextX][nextY].equals(Tileset.WALL)) {
                wallCount += 1;
                continue;
            }
            /* only check if visited already after counting the neighboring walls, otherwise
            miss counting will happen
             */
            if (visited[nextX][nextY]) {
                continue;
            }
            if (world[nextX][nextY].equals(Tileset.FLOOR)) {
                path.offer(new Location(nextX, nextY));
            }
        }
        // check if already a dead end
        if (wallCount >= 3) {
            world[x][y] = Tileset.WALL;
        }
        while (!path.isEmpty()) {
            Location cur = path.poll();
            visited[cur.x][cur.y] = true;
            /* if the cur neighbor was a deadend and filled with WALL, it might make the current tile
            a deadend as well
             */
            if (dfs(cur.x, cur.y)) {
                wallCount += 1;
            }
            if (wallCount >= 3) {
                world[x][y] = Tileset.WALL;
            }
        }
        return wallCount >= 3;
    }

    // fill the world with rooms
    public void fillWorldWithRooms() {
        for (int i = 0; i <= LIMIT; i++) {
            int x = random.nextInt(WIDTH);
            int y = random.nextInt(HEIGHT);
            int width = random.nextInt(WIDTH / 8) + 2;
            int height = random.nextInt(HEIGHT / 3) + 2;

            // check if the room is out of bound
            if (x + width + 1 >= WIDTH || y + height + 1 >= HEIGHT) {
                continue;
            }
            // check if it's overlapping with existing rooms
            if (checkOverlap(x, y, width, height)) {
                continue;
            }
            buildRoom(x, y, width, height);
            rooms.add(new Room(x, y, width, height));
        }
    }

    public boolean checkOverlap(int x, int y, int w, int h) {
        for (int i = x; i <= x + w + 1; i++) {
            for (int j = y; j <= y + h + 1; j++) {
                if (roomPositions[i][j].equals(Tileset.WALL)) {
                    return true;
                }
            }
        }
        return false;
    }

    // roomPositions is used to track existing rooms, fill each room with walls
    public void buildRoom(int x, int y, int w, int h) {
        for (int i = x; i <= x + w + 1; i++) {
            for (int j = y; j <= y + h + 1; j++) {
                if (i == x || i == x + w + 1 || j == y || j == y + h + 1) {
                    world[i][j] = Tileset.WALL;
                    roomPositions[i][j] = Tileset.WALL;
                    continue;
                }
                world[i][j] = Tileset.FLOWER;
                roomPositions[i][j] = Tileset.WALL;
            }
        }
    }

    public void renderWorld() {
        ter.renderFrame(world);
    }

    public static void main(String[] args) {
        Engine e = new Engine();
        e.interactWithInputString("666");
        e.renderWorld();
    }
}
