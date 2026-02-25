import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

public class Cell implements Comparable<Cell> {
    int col, row;
    int size;
    boolean[] walls = {true, true, true, true};
    boolean visited = false;
    Cell parent;

    // 0=Default, 1=Grass, 5=Mud, 10=Water
    int terrainCost;

    double g = Double.POSITIVE_INFINITY;
    double h = 0;
    double f = Double.POSITIVE_INFINITY;

    private static final Color WALL_COLOR = new Color(0, 0, 0);
    private static final BasicStroke WALL_STROKE = new BasicStroke(3);

    public Cell(int col, int row, int size) {
        this.col = col;
        this.row = row;
        this.size = size;
        this.terrainCost = 0; 
    }

    @Override
    public int compareTo(Cell other) {
        return Double.compare(this.f, other.f);
    }

    public void drawTerrain(Graphics2D g2d) {
        int x = col * size;
        int y = row * size;
        Color terrainColor;

        if (terrainCost == 0) terrainColor = new Color(105, 105, 105);    // Default Terrace (Dim Gray)
        else if (terrainCost == 1) terrainColor = new Color(60, 179, 113); // Grass (Medium Sea Green)
        else if (terrainCost == 5) terrainColor = new Color(205, 133, 63); // Mud (Peru Brown)
        else terrainColor = new Color(70, 130, 180);                      // Water (Steel Blue)

        g2d.setColor(terrainColor);
        g2d.fillRect(x, y, size, size);
    }

    public void drawWalls(Graphics2D g2d) {
        int x = col * size;
        int y = row * size;
        g2d.setColor(WALL_COLOR);
        g2d.setStroke(WALL_STROKE);

        if (walls[0]) g2d.drawLine(x, y, x + size, y);
        if (walls[1]) g2d.drawLine(x + size, y, x + size, y + size);
        if (walls[2]) g2d.drawLine(x + size, y + size, x, y + size);
        if (walls[3]) g2d.drawLine(x, y + size, x, y);
    }

    public static void removeWalls(Cell a, Cell b) {
        int x = a.col - b.col;
        int y = a.row - b.row;
        if (x == 1) { a.walls[3] = false; b.walls[1] = false; }
        if (x == -1) { a.walls[1] = false; b.walls[3] = false; }
        if (y == 1) { a.walls[0] = false; b.walls[2] = false; }
        if (y == -1) { a.walls[2] = false; b.walls[0] = false; }
    }
}
