import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class GraphProgram extends JFrame {
    private GraphPanel graphPanel;
    private JButton selectGraphButton;
    private JButton shortestPathButton;

    public GraphProgram() {
        setTitle("Поможем очистить город");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        graphPanel = new GraphPanel();
        selectGraphButton = new JButton("Выбрать граф");
        shortestPathButton = new JButton("Найти наикратчайший путь");
        JFileChooser fileChooserOpen;


        fileChooserOpen = new JFileChooser();
        fileChooserOpen.setCurrentDirectory(new File("."));
        FileFilter filter = new FileNameExtensionFilter("Text files", "txt");
        fileChooserOpen.addChoosableFileFilter(filter);

        JFileChooser finalFileChooserOpen = fileChooserOpen;

        selectGraphButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (finalFileChooserOpen.showOpenDialog(graphPanel) == JFileChooser.APPROVE_OPTION) {
                    int[][] adjacencyMatrix = ArrayUtils.readIntArray2FromFile(finalFileChooserOpen.getSelectedFile().getPath());
                    graphPanel.setGraph(adjacencyMatrix);
                    graphPanel.resetPathHighlight();
                }
            }
        });

        shortestPathButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int shortestPath = graphPanel.getShortestPath();
                JOptionPane.showMessageDialog(GraphProgram.this,
                        "Shortest Path Length: " + shortestPath,
                        "Shortest Path",
                        JOptionPane.INFORMATION_MESSAGE);
                graphPanel.highlightShortestPath();
            }
        });

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(selectGraphButton);
        buttonPanel.add(shortestPathButton);

        add(graphPanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new GraphProgram();
            }
        });
    }
}

class GraphPanel extends JPanel {
    private List<Node> nodes;
    private List<Edge> edges;
    private int[][] adjacencyMatrix;
    private List<Integer> shortestPath;

    public GraphPanel() {
        nodes = new ArrayList<>();
        edges = new ArrayList<>();
        adjacencyMatrix = null;
        shortestPath = new ArrayList<>();
    }
    public int[][] getGraph() {
        return adjacencyMatrix;
    }

    public void setGraph(int[][] graph) {
        adjacencyMatrix = graph;
        int numNodes = graph.length;

        nodes.clear();
        edges.clear();

        int nodeSize = 30;
        int padding = 50;
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;

        double angleIncrement = 2 * Math.PI / numNodes;
        double currentAngle = 0;

        // Создаем узлы и рассчитываем их позиции по окружности
        for (int i = 0; i < numNodes; i++) {
            int x = (int) (centerX + Math.cos(currentAngle) * (centerX - padding));
            int y = (int) (centerY + Math.sin(currentAngle) * (centerY - padding));

            Node node = new Node(x, y, nodeSize, i);
            nodes.add(node);

            currentAngle += angleIncrement;
        }

        // Создаем ребра на основе матрицы смежности
        for (int i = 0; i < numNodes; i++) {
            for (int j = i + 1; j < numNodes; j++) {
                if (graph[i][j] > 0) {
                    Edge edge = new Edge(nodes.get(i), nodes.get(j), graph[i][j]);
                    edges.add(edge);
                }
            }
        }
    }

    public int getShortestPath() {
        if (adjacencyMatrix == null) {
            return 0;
        }
        int numNodes = adjacencyMatrix.length;
        boolean[] visited = new boolean[numNodes];
        shortestPath.clear();

        int currentNode = 0;
        visited[currentNode] = true;
        shortestPath.add(currentNode);

        // Проходим по всем остальным узлам
        while (shortestPath.size() < numNodes) {
            int nextNode = -1;
            int minWeight = Integer.MAX_VALUE;

            // Ищем ближайший непосещенный узел
            for (int i = 0; i < numNodes; i++) {
                if (!visited[i] && adjacencyMatrix[currentNode][i] > 0 && adjacencyMatrix[currentNode][i] < minWeight) {
                    nextNode = i;
                    minWeight = adjacencyMatrix[currentNode][i];
                }
            }

            if (nextNode != -1) {
                currentNode = nextNode;
                visited[currentNode] = true;
                shortestPath.add(currentNode);
            } else {
                // Если не удалось найти следующий узел, значит граф не связный, а значит нам не подходит
                break;
            }
        }

        // Суммируем веса ребер пути
        int totalWeight = 0;
        for (int i = 1; i < shortestPath.size(); i++) {
            int nodeA = shortestPath.get(i - 1);
            int nodeB = shortestPath.get(i);
            totalWeight += adjacencyMatrix[nodeA][nodeB];
        }

        return totalWeight;
    }

    public void highlightShortestPath() {
        // Подсветка кратчайшего пути на графе
        resetPathHighlight();

        for (int i = 1; i < shortestPath.size(); i++) {
            int nodeA = shortestPath.get(i - 1);
            int nodeB = shortestPath.get(i);

            // Подсветка узлов
            nodes.get(nodeA).setHighlighted(true);
            nodes.get(nodeB).setHighlighted(true);

            // Подсветка ребер
            for (Edge edge : edges) {
                if ((edge.getNodeA().getId() == nodeA && edge.getNodeB().getId() == nodeB)
                        || (edge.getNodeA().getId() == nodeB && edge.getNodeB().getId() == nodeA)) {
                    edge.setHighlighted(true);
                    break;
                }
            }
        }

        // Перерисовка графа для отображения подсветки
        repaint();
    }

    public void resetPathHighlight() {
        // Сброс подсветки пути на графе
        for (Node node : nodes) {
            node.setHighlighted(false);
        }

        for (Edge edge : edges) {
            edge.setHighlighted(false);
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2d = (Graphics2D) g;

        // Отрисовка ребер
        for (Edge edge : edges) {
            if (edge.isHighlighted()) {
                g2d.setColor(Color.RED);
            } else {
                g2d.setColor(Color.BLACK);
            }
            g2d.setStroke(new BasicStroke(2));
            g2d.drawLine(edge.getNodeA().getX(), edge.getNodeA().getY(), edge.getNodeB().getX(), edge.getNodeB().getY());
        }

        // Отрисовка узлов
        for (Node node : nodes) {
            if (node.isHighlighted()) {
                g2d.setColor(Color.RED);
            } else {
                g2d.setColor(Color.BLUE);
            }
            g2d.fillOval(node.getX() - node.getSize() / 2, node.getY() - node.getSize() / 2, node.getSize(), node.getSize());
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Arial", Font.BOLD, 12));
            g2d.drawString(Integer.toString(node.getId()), node.getX() - 5, node.getY() + 5);
        }
    }
}

class Node {
    private int x;
    private int y;
    private int size;
    private int id;
    private boolean highlighted;

    public Node(int x, int y, int size, int id) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.id = id;
        this.highlighted = false;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
    public int getSize() {
        return size;
    }

    public int getId() {
        return id;
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }
}

class Edge {
    private Node nodeA;
    private Node nodeB;
    private int weight;
    private boolean highlighted;

    public Edge(Node nodeA, Node nodeB, int weight) {
        this.nodeA = nodeA;
        this.nodeB = nodeB;
        this.weight = weight;
        this.highlighted = false;
    }

    public Node getNodeA() {
        return nodeA;
    }

    public Node getNodeB() {
        return nodeB;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }
}