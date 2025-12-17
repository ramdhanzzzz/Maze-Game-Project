import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.util.*;

public class MazeGame extends JPanel {
    private final int COLS = 35;
    private final int ROWS = 25;
    private final int TILE_SIZE = 25;
    public final int PAN_WIDTH = COLS * TILE_SIZE;
    public final int PAN_HEIGHT = ROWS * TILE_SIZE;

    // Visual Colors
    private final Color START_COLOR = new Color(50, 255, 50);
    private final Color END_COLOR = new Color(255, 50, 50);
    private final Color HEAD_COLOR = new Color(255, 255, 0);
    private final Color PATH_COLOR = new Color(255, 255, 255);
    private final Color VISITED_OVERLAY = new Color(255, 255, 255, 90);

    private ArrayList<Cell> grid = new ArrayList<>();
    private Cell startCell, endCell;

    private HashSet<Cell> visitedSet = new HashSet<>();
    private ArrayList<Cell> finalPath = new ArrayList<>();
    private Cell currentHead = null;
    public boolean isSolving = false;
    private boolean isSolved = false;
    private String currentAlgo = "-";

    private double currentCost = 0;
    private int finalTotalCost = 0;

    public JLabel lblAlgo, lblCurrentCost, lblFinalCost, lblStatus;

    public MazeGame() {
        setPreferredSize(new Dimension(PAN_WIDTH, PAN_HEIGHT));
        setBackground(Color.DARK_GRAY);
        setFocusable(true);
        requestFocusInWindow();
        resetMaze();
    }

    public void startSolving(String algo) {
        if (isSolving) return;
        updateStatus("Running " + algo + "...", Color.YELLOW);
        new Thread(() -> solveLogic(algo)).start();
    }

    public void resetMaze() {
        grid.clear();
        visitedSet.clear();
        finalPath.clear();
        isSolved = false;
        isSolving = false;
        currentAlgo = "-";
        currentCost = 0;
        finalTotalCost = 0;
        currentHead = null;
        updateStatus("Map Ready.", Color.WHITE);
        updateScoreUI();

        // 1. Init Grid
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                grid.add(new Cell(c, r, TILE_SIZE));
            }
        }

        // 2. STEP 1: Generate Maze using PRIM'S ALGORITHM
        generatePrimsMaze();

        // 3. STEP 3: Assign Weighted Terrains
        assignTerrains();

        startCell = grid.get(0);
        endCell = grid.get(grid.size() - 1);

        // Ensure Start/End are walkable (Default/Grass)
        startCell.terrainCost = 0;
        endCell.terrainCost = 0;
        repaint();
    }

    // STEP 3: Random Terrain Generation including Default (0)
    private void assignTerrains() {
        Random rand = new Random();
        for (Cell c : grid) {
            double chance = rand.nextDouble();
            if (chance < 0.40) c.terrainCost = 0;       // 40% Default Terrace (Cost 0)
            else if (chance < 0.70) c.terrainCost = 1;  // 30% Grass (Cost 1)
            else if (chance < 0.90) c.terrainCost = 5;  // 20% Mud (Cost 5)
            else c.terrainCost = 10;                    // 10% Water (Cost 10)
        }
    }

    public void updateStatus(String text, Color color) {
        SwingUtilities.invokeLater(() -> {
            if (lblStatus != null) { lblStatus.setText(text); lblStatus.setForeground(color); }
        });
    }

    private void updateScoreUI() {
        SwingUtilities.invokeLater(() -> {
            if (lblAlgo != null) lblAlgo.setText(currentAlgo);
            if (lblCurrentCost != null) lblCurrentCost.setText(String.format("%.0f", currentCost));
            if (lblFinalCost != null) lblFinalCost.setText(isSolved ? String.valueOf(finalTotalCost) : "-");
        });
    }

    private int getIndex(int c, int r) {
        if (c < 0 || r < 0 || c >= COLS || r >= ROWS) return -1;
        return c + r * COLS;
    }

    // --- STEP 1: PRIM'S ALGORITHM IMPLEMENTATION ---
    private void generatePrimsMaze() {
        ArrayList<Cell> frontier = new ArrayList<>();
        Random rand = new Random();

        // Start with a random cell
        Cell start = grid.get(rand.nextInt(grid.size()));
        start.visited = true;
        frontier.addAll(getPotentialNeighbors(start));

        while (!frontier.isEmpty()) {
            // Pick random cell from frontier
            int randIndex = rand.nextInt(frontier.size());
            Cell current = frontier.remove(randIndex);

            // Find visited neighbors
            ArrayList<Cell> visitedNeighbors = new ArrayList<>();
            for (Cell n : getPotentialNeighbors(current)) {
                if (n.visited) visitedNeighbors.add(n);
            }

            if (!visitedNeighbors.isEmpty()) {
                // Connect to random visited neighbor
                Cell connected = visitedNeighbors.get(rand.nextInt(visitedNeighbors.size()));
                Cell.removeWalls(current, connected);
                current.visited = true;

                // Add unvisited neighbors to frontier
                for (Cell n : getPotentialNeighbors(current)) {
                    if (!n.visited && !frontier.contains(n)) {
                        frontier.add(n);
                    }
                }
            }
        }

        // Reset visited for solver
        for (Cell c : grid) c.visited = false;

        // STEP 1: Make Multiple Paths (Loops)
        for (int i = 0; i < (COLS * ROWS * 0.15); i++) {
            Cell c1 = grid.get(rand.nextInt(grid.size()));
            Cell c2 = getRandomNeighbor(c1);
            if (c2 != null) Cell.removeWalls(c1, c2);
        }
    }

    private ArrayList<Cell> getPotentialNeighbors(Cell c) {
        ArrayList<Cell> neighbors = new ArrayList<>();
        int[] dx = {0, 1, 0, -1}; int[] dy = {-1, 0, 1, 0};
        for (int i = 0; i < 4; i++) {
            int idx = getIndex(c.col + dx[i], c.row + dy[i]);
            if (idx != -1) neighbors.add(grid.get(idx));
        }
        return neighbors;
    }

    private Cell getRandomNeighbor(Cell c) {
        ArrayList<Cell> neighbors = getPotentialNeighbors(c);
        if (neighbors.size() > 0) return neighbors.get(new Random().nextInt(neighbors.size()));
        return null;
    }

    // GANTI METHOD solveLogic YANG LAMA DENGAN INI
    private void solveLogic(String algo) {
        isSolving = true;
        currentAlgo = algo;
        visitedSet.clear();
        finalPath.clear();
        finalTotalCost = 0;
        currentCost = 0;

        // Tentukan apakah algoritma ini Unweighted (Tidak peduli lumpur/air)
        boolean isUnweighted = algo.equals("BFS") || algo.equals("DFS");

        // Reset Grid Data
        for(Cell c : grid) {
            c.g = Double.POSITIVE_INFINITY;
            c.f = Double.POSITIVE_INFINITY;
            c.parent = null;
            c.visited = false;
        }

        // Struktur Data untuk berbagai algoritma
        Queue<Cell> queue = new LinkedList<>();       // Untuk BFS
        Stack<Cell> stack = new Stack<>();            // Untuk DFS
        PriorityQueue<Cell> pq = new PriorityQueue<>(); // Untuk Dijkstra & A*

        // Setup Start Node
        startCell.g = 0;
        startCell.visited = true;

        if (algo.equals("BFS")) queue.add(startCell);
        else if (algo.equals("DFS")) stack.push(startCell);
        else {
            // A* pakai heuristic, Dijkstra 0
            startCell.f = algo.equals("A*") ? heuristic(startCell, endCell) : 0;
            pq.add(startCell);
        }

        boolean found = false;

        // --- LOOP PENCARIAN (ANIMASI ADA DI SINI) ---
        while (!queue.isEmpty() || !stack.isEmpty() || !pq.isEmpty()) {
            Cell current;

            // 1. Ambil Node berikutnya
            if (algo.equals("BFS")) current = queue.poll();
            else if (algo.equals("DFS")) current = stack.pop();
            else current = pq.poll();

            // Tandai sudah dikunjungi (Visualisasi warna putih transparan)
            visitedSet.add(current);
            currentHead = current; // Kepala kuning (posisi saat ini)

            // Update UI Score secara Realtime
            // Jika Unweighted, cost = jumlah kotak yg dikunjungi. Jika Weighted, cost = nilai g.
            currentCost = isUnweighted ? visitedSet.size() : current.g;
            updateScoreUI();

            // PENTING: Repaint agar perubahan terlihat di layar
            repaint();

            // Cek Finish
            if (current == endCell) {
                found = true;
                break;
            }

            // 2. Ambil Tetangga
            ArrayList<Cell> neighbors = getValidNeighbors(current);

            // Khusus DFS: Shuffle biar jalannya random/seru, kalau tidak dia akan lurus terus
            if (algo.equals("DFS")) Collections.shuffle(neighbors);

            for (Cell neighbor : neighbors) {
                // Hitung Cost per langkah
                // Jika Unweighted (BFS/DFS), anggap cost tanah = 1 (abaikan lumpur/air)
                // Jika Weighted, ambil terrainCost asli
                double moveCost = isUnweighted ? 1 : neighbor.terrainCost;
                double newG = current.g + moveCost;

                // LOGIKA BFS & DFS (Unweighted)
                if (isUnweighted) {
                    if (!neighbor.visited) {
                        neighbor.visited = true;
                        neighbor.parent = current;
                        neighbor.g = newG; // Simpan jarak langkah

                        if (algo.equals("BFS")) queue.add(neighbor);
                        else stack.push(neighbor);
                    }
                }
                // LOGIKA DIJKSTRA & A* (Weighted)
                else {
                    if (newG < neighbor.g) {
                        neighbor.g = newG;
                        neighbor.h = algo.equals("A*") ? heuristic(neighbor, endCell) : 0;
                        neighbor.f = neighbor.g + neighbor.h;
                        neighbor.parent = current;

                        // Refresh Priority Queue
                        pq.remove(neighbor);
                        pq.add(neighbor);
                    }
                }
            }

            // --- ANIMASI DELAY ---
            // Thread tidur sebentar agar mata user bisa mengikuti pergerakan
            try {
                // DFS lebih cepat (10ms) karena jalurnya panjang, BFS/Dijkstra (15ms)
                Thread.sleep(algo.equals("DFS") ? 10 : 15);
            } catch (Exception e) {}
        }

        // --- SELESAI PENCARIAN ---
        if (found) {
            reconstructPath(); // Gambar garis path
            isSolved = true;
            updateStatus("FINISHED! (" + algo + ")", Color.GREEN);
        } else {
            updateStatus("Path Not Found.", Color.RED);
        }

        isSolving = false;
        currentHead = null; // Hapus kepala kuning
        updateScoreUI();
        repaint();
    }

    private double heuristic(Cell a, Cell b) {
        return (Math.abs(a.col - b.col) + Math.abs(a.row - b.row)) * 1.0;
    }

    private ArrayList<Cell> getValidNeighbors(Cell c) {
        ArrayList<Cell> neighbors = new ArrayList<>();
        if (!c.walls[0]) addIfValid(neighbors, c.col, c.row - 1);
        if (!c.walls[1]) addIfValid(neighbors, c.col + 1, c.row);
        if (!c.walls[2]) addIfValid(neighbors, c.col, c.row + 1);
        if (!c.walls[3]) addIfValid(neighbors, c.col - 1, c.row);
        return neighbors;
    }

    private void addIfValid(ArrayList<Cell> list, int c, int r) {
        int idx = getIndex(c, r);
        if (idx != -1) list.add(grid.get(idx));
    }

    private void reconstructPath() {
        Cell curr = endCell;

        // Cek apakah algoritma terakhir adalah Unweighted (BFS/DFS)
        boolean isUnweighted = currentAlgo.equals("BFS") || currentAlgo.equals("DFS");

        while (curr != startCell && curr != null) {
            // Jika BFS/DFS: Cost tambah 1 per langkah (Steps)
            // Jika Dijkstra/A*: Cost tambah sesuai berat tanah (Terrain Cost)
            if (isUnweighted) {
                finalTotalCost += 1;
            } else {
                finalTotalCost += curr.terrainCost;
            }

            finalPath.add(curr);
            curr = curr.parent;
        }
        finalPath.add(startCell);
        Collections.reverse(finalPath);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Layers
        for (Cell c : grid) c.drawTerrain(g2d);
        g2d.setColor(VISITED_OVERLAY);
        for (Cell c : visitedSet) g2d.fillRect(c.col * TILE_SIZE, c.row * TILE_SIZE, TILE_SIZE, TILE_SIZE);
        for (Cell c : grid) c.drawWalls(g2d);

        drawSpecialCell(g2d, startCell, START_COLOR);
        drawSpecialCell(g2d, endCell, END_COLOR);

        if (isSolved) {
            g2d.setColor(PATH_COLOR);
            g2d.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            for (int i = 0; i < finalPath.size() - 1; i++) {
                Cell c1 = finalPath.get(i);
                Cell c2 = finalPath.get(i+1);
                g2d.drawLine(c1.col*TILE_SIZE+TILE_SIZE/2, c1.row*TILE_SIZE+TILE_SIZE/2,
                        c2.col*TILE_SIZE+TILE_SIZE/2, c2.row*TILE_SIZE+TILE_SIZE/2);
            }
        }
        if (currentHead != null && !isSolved) drawSpecialCell(g2d, currentHead, HEAD_COLOR);
    }

    private void drawSpecialCell(Graphics2D g2d, Cell c, Color color) {
        g2d.setColor(color);
        int p = 5;
        g2d.fillOval(c.col * TILE_SIZE + p, c.row * TILE_SIZE + p, TILE_SIZE - p*2, TILE_SIZE - p*2);
        g2d.setColor(Color.BLACK);
        g2d.setStroke(new BasicStroke(2));
        g2d.drawOval(c.col * TILE_SIZE + p, c.row * TILE_SIZE + p, TILE_SIZE - p*2, TILE_SIZE - p*2);
    }

    // ==========================================
    // MAIN UI SETUP (REVISI DESIGN)
    // ==========================================
    public static void main(String[] args) {
        JFrame frame = new JFrame("Java Maze Pathfinder");
        frame.setLayout(new BorderLayout());
        frame.getContentPane().setBackground(new Color(40, 40, 40));

        MazeGame gamePanel = new MazeGame();
        frame.add(gamePanel, BorderLayout.CENTER);

        // 1. SETUP SIDEBAR DENGAN GRIDBAGLAYOUT (Agar Center Rapi)
        JPanel sidebar = new JPanel(new GridBagLayout());
        sidebar.setBackground(new Color(50, 50, 50));
        sidebar.setPreferredSize(new Dimension(260, gamePanel.PAN_HEIGHT));
        sidebar.setBorder(new EmptyBorder(10, 10, 10, 10));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 1.0;

        // --- HEADER ---
        gbc.gridy = 0;
        JLabel titleLabel = new JLabel("CONTROL PANEL", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 20));
        titleLabel.setForeground(Color.WHITE);
        sidebar.add(titleLabel, gbc);

        gbc.gridy++;
        sidebar.add(Box.createVerticalStrut(10), gbc);

        // --- LEGEND SECTION ---
        gbc.gridy++;
        sidebar.add(createHeaderLabel("Terrain Legend"), gbc);

        JPanel legendPanel = new JPanel(new GridLayout(2, 2, 5, 5));
        legendPanel.setBackground(new Color(50, 50, 50));
        legendPanel.add(createLegendItem(new Color(105, 105, 105), "Normal"));
        legendPanel.add(createLegendItem(new Color(60, 179, 113), "Grass(1)"));
        legendPanel.add(createLegendItem(new Color(205, 133, 63), "Mud(5)"));
        legendPanel.add(createLegendItem(new Color(70, 130, 180), "Water(10)"));
        gbc.gridy++;
        sidebar.add(legendPanel, gbc);

        // --- STATS SECTION ---
        gbc.gridy++;
        sidebar.add(Box.createVerticalStrut(15), gbc);

        JPanel statsPanel = new JPanel(new GridLayout(3, 2));
        statsPanel.setBackground(new Color(50, 50, 50));
        statsPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Color.GRAY), "Statistics",
                0, 0, new Font("SansSerif", Font.PLAIN, 12), Color.LIGHT_GRAY));

        gamePanel.lblAlgo = createDataLabel("-", Color.CYAN);
        gamePanel.lblCurrentCost = createDataLabel("0", Color.YELLOW);
        gamePanel.lblFinalCost = createDataLabel("-", Color.GREEN);

        statsPanel.add(createSmallLabel("Algorithm:")); statsPanel.add(gamePanel.lblAlgo);
        statsPanel.add(createSmallLabel("Live Cost:")); statsPanel.add(gamePanel.lblCurrentCost);
        statsPanel.add(createSmallLabel("Final Cost:")); statsPanel.add(gamePanel.lblFinalCost);

        gbc.gridy++;
        sidebar.add(statsPanel, gbc);

        // --- STATUS TEXT ---
        gbc.gridy++;
        sidebar.add(Box.createVerticalStrut(10), gbc);
        gamePanel.lblStatus = new JLabel("Ready", SwingConstants.CENTER);
        gamePanel.lblStatus.setForeground(Color.WHITE);
        gamePanel.lblStatus.setFont(new Font("Segoe UI", Font.ITALIC, 14));
        sidebar.add(gamePanel.lblStatus, gbc);

        // --- UNWEIGHTED BUTTONS ---
        gbc.gridy++;
        sidebar.add(Box.createVerticalStrut(15), gbc);
        sidebar.add(createHeaderLabel("Unweighted Search"), gbc);

        gbc.gridy++;
        JButton btnBFS = createStyledButton("BFS", new Color(52, 152, 219));
        btnBFS.addActionListener(e -> gamePanel.startSolving("BFS"));
        sidebar.add(btnBFS, gbc);

        gbc.gridy++;
        JButton btnDFS = createStyledButton("DFS", new Color(155, 89, 182));
        btnDFS.addActionListener(e -> gamePanel.startSolving("DFS"));
        sidebar.add(btnDFS, gbc);

        // --- WEIGHTED BUTTONS ---
        gbc.gridy++;
        sidebar.add(Box.createVerticalStrut(15), gbc);
        sidebar.add(createHeaderLabel("Weighted Search"), gbc);

        gbc.gridy++;
        JButton btnDijkstra = createStyledButton("Dijkstra", new Color(230, 126, 34));
        btnDijkstra.addActionListener(e -> gamePanel.startSolving("Dijkstra"));
        sidebar.add(btnDijkstra, gbc);

        gbc.gridy++;
        JButton btnAStar = createStyledButton("A-Star", new Color(192, 57, 43));
        btnAStar.addActionListener(e -> gamePanel.startSolving("A*"));
        sidebar.add(btnAStar, gbc);

        // --- MAZE GENERATION ---
        gbc.gridy++;
        sidebar.add(Box.createVerticalStrut(25), gbc);

        // WARNA HIJAU CERAH UNTUK TOMBOL MAZE
        JButton btnMaze = createStyledButton("Generate New Prim Maze", new Color(39, 174, 96));
        btnMaze.setFont(new Font("Segoe UI", Font.BOLD, 14)); // Font sedikit lebih tebal
        btnMaze.addActionListener(e -> gamePanel.resetMaze());
        sidebar.add(btnMaze, gbc);

        frame.add(sidebar, BorderLayout.EAST);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    // --- UI HELPER METHODS ---

    private static JLabel createHeaderLabel(String text) {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(new Font("Segoe UI", Font.BOLD, 14));
        l.setForeground(Color.LIGHT_GRAY);
        return l;
    }

    private static JLabel createSmallLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.LIGHT_GRAY);
        return l;
    }

    private static JLabel createDataLabel(String text, Color c) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Consolas", Font.BOLD, 14));
        l.setForeground(c);
        return l;
    }

    private static JPanel createLegendItem(Color c, String text) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        p.setBackground(new Color(50, 50, 50));
        JPanel box = new JPanel();
        box.setPreferredSize(new Dimension(12, 12));
        box.setBackground(c);
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("SansSerif", Font.PLAIN, 11));
        p.add(box); p.add(l);
        return p;
    }

    private static JButton createStyledButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btn.setBackground(bg);
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false); // Flat style
        btn.setOpaque(true);
        // Trik agar warna muncul di beberapa L&F (Mac/Windows)
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(bg.darker(), 1),
                new EmptyBorder(8, 15, 8, 15)
        ));

        // Custom UI Painter untuk background warna
        btn.setUI(new javax.swing.plaf.basic.BasicButtonUI() {
            @Override
            public void paint(Graphics g, JComponent c) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c.getBackground());
                g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 10, 10); // Rounded corners
                super.paint(g2, c);
                g2.dispose();
            }
        });
        return btn;
    }
}