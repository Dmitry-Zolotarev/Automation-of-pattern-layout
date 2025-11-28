import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * Главное окно приложения для автоматизации раскладки лекал.
 * Управляет интерфейсом и взаимодействием пользователя с деталями и раскладками.
 */
public class Form1 extends JFrame {
    
    // ========== КОНСТАНТЫ ==========
    private static final int TIMER_DELAY_MS = 10;
    private static final int MAX_NAME_LENGTH = 30;
    private static final int CLICK_DURATION_THRESHOLD_MS = 200;
    private static final int KEYBOARD_ROTATION_STEP = 5; // градусы
    private static final float DEFAULT_DISTANCE_MM = 10f;
    private static final int EDIT_PANEL_OFFSET_Y = 60;
    private static final int EDIT_PANEL_ROW_HEIGHT = 30;
    private static final float MIN_SCALE = 0.01f;
    private static final float MAX_SCALE = 1f;
    private static final String DEFAULT_FILE_EXTENSION = ".xml";
    
    // ========== КОМПОНЕНТЫ МЕНЮ И ПАНЕЛИ ==========
    private final JMenuBar menuBar = new JMenuBar();
    private final JToolBar toolBar = new JToolBar();
    private final JScrollPane canvasPane = new JScrollPane();
    private final JScrollPane hierarchyPane;
    private final JScrollPane editScrollPane;
    private final JTextArea editPanel = new JTextArea();
    
    // ========== КОМПОНЕНТЫ ЛЕВОГО МЕНЮ ==========
    private final JTree detailTree;
    private final JPopupMenu detailPopupMenu = new JPopupMenu();
    private final JPopupMenu emptyPopupMenu = new JPopupMenu();
    private final JPopupMenu rootPopupMenu = new JPopupMenu();
    
    // ========== КОМПОНЕНТЫ ИНСТРУМЕНТОВ ==========
    private final JButton saveButton = createToolButton("save.png", "Сохранить (Ctrl + S)");
    private final JButton undoButton = createToolButton("undo.png", "Отменить (Ctrl + Z)");
    private final JButton redoButton = createToolButton("redo.png", "Вернуть (Ctrl + Shift + Z)");
    private final JButton saveImageButton = new JButton("Сохранить как PNG");
    private final JButton propertiesButton = new JButton("Площадь детали");
    private final JButton scaleButton = new JButton("Масштаб: 100%");
    private final JLabel detailLabel = new JLabel();
    private final JCheckBox layoutCheckbox = new JCheckBox("На раскладку");
    
    // ========== ДАННЫЕ ==========
    private Product product;
    private Detail currentDetail;
    private int selectedDetailIndex = 0;
    
    // ========== СОСТОЯНИЕ ==========
    private Timer renderTimer;
    private int canvasHeight = 0;
    private int canvasWidth = 0;
    private int treeMouseX = 0;
    private int treeMouseY = 0;
    
    // ========== ИСТОРИЯ ОПЕРАЦИЙ ==========
    private final List<Product> undoStack = new ArrayList<>();
    private final List<Integer> selectedStack = new ArrayList<>();
    private int currentHistoryIndex = -1;
    
    // ========== ПОЛЯ РЕДАКТИРОВАНИЯ ВЕРШИН ==========
    private final List<NumericField> xCoordinateFields = new ArrayList<>();
    private final List<NumericField> yCoordinateFields = new ArrayList<>();
    private boolean shouldRedrawVertices = true;
    
    // ========== ВРЕМЯ И СОБЫТИЯ ==========
    private long mousePresTime;
    private long clickDuration;
    
    // ========== КОНСТРУКТОР ==========
    
    /**
     * Инициализирует главное окно приложения.
     * @param product объект изделия
     * @param filePath путь к файлу проекта
     */
    public Form1(Product product, String filePath) {
        this.product = product;
        this.product.filePath = filePath;
        this.product.mainWindow = this;
        
        recordAction();
        
        initializeDetailTree();
        initializePopupMenus();
        initializeMenuBar();
        initializeToolBar();
        initializeEventListeners();
        initializeWindowSettings();
        
        startRenderTimer();
        setVisible(true);
    }
    
    // ========== ИНИЦИАЛИЗАЦИЯ КОМПОНЕНТОВ ==========
    
    private void initializeDetailTree() {
        detailTree = new JTree(product.root);
        hierarchyPane = new JScrollPane(detailTree);
        hierarchyPane.getViewport().setPreferredSize(new Dimension(200, 0));
        detailTree.addMouseListener(createTreeMouseListener());
        selectedDetailIndex = 0;
        currentDetail = product.details.get(selectedDetailIndex);
    }
    
    private void initializePopupMenus() {
        setupDetailContextMenu();
        setupEmptyAreaContextMenu();
        setupRootContextMenu();
    }
    
    private void setupDetailContextMenu() {
        var renameItem = new JMenuItem("Переименовать");
        renameItem.addActionListener(e -> renameDetail());
        
        var duplicateItem = new JMenuItem("Дублировать");
        duplicateItem.addActionListener(e -> addDetail(new Detail(currentDetail), 1, selectedDetailIndex));
        
        var editItem = new JMenuItem("Редактировать деталь");
        editItem.addActionListener(e -> exitLayout());
        
        var deleteItem = new JMenuItem("Удалить");
        deleteItem.addActionListener(e -> removeDetail());
        
        detailPopupMenu.add(renameItem);
        detailPopupMenu.add(duplicateItem);
        detailPopupMenu.add(editItem);
        detailPopupMenu.add(deleteItem);
    }
    
    private void setupEmptyAreaContextMenu() {
        var addItem = new JMenuItem("Создать новую деталь");
        addItem.addActionListener(e -> addDetail(new Detail(), 0, product.details.size() - 1));
        emptyPopupMenu.add(addItem);
    }
    
    private void setupRootContextMenu() {
        var renameItem = new JMenuItem("Переименовать");
        renameItem.addActionListener(e -> renameDetail());
        
        var descriptionItem = new JMenuItem("Редактировать описание");
        descriptionItem.addActionListener(e -> setProductDescription());
        
        rootPopupMenu.add(renameItem);
        rootPopupMenu.add(descriptionItem);
    }
    
    private void initializeMenuBar() {
        JMenu fileMenu = createFileMenu();
        JMenu layoutMenu = createLayoutMenu();
        JMenu editMenu = createEditMenu();
        JMenu helpMenu = createHelpMenu();
        
        menuBar.add(fileMenu);
        menuBar.add(layoutMenu);
        menuBar.add(editMenu);
        menuBar.add(helpMenu);
        setJMenuBar(menuBar);
    }
    
    private JMenu createFileMenu() {
        JMenu menu = new JMenu("Файл");
        addMenuItemWithListener(menu, "Создать новый", e -> createNewProject());
        addMenuItemWithListener(menu, "Открыть", e -> openFile());
        addMenuItemWithListener(menu, "Сохранить", e -> saveFile());
        addMenuItemWithListener(menu, "Сохранить как", e -> saveFileAs());
        return menu;
    }
    
    private JMenu createLayoutMenu() {
        JMenu menu = new JMenu("Раскладка");
        addMenuItemWithListener(menu, "Быстрая раскладка (Shift + F)", e -> performLayout(1));
        addMenuItemWithListener(menu, "ИИ-раскладка (Shift + A)", e -> performLayout(2));
        addMenuItemWithListener(menu, "Редактировать раскладку (Shift + E)", e -> performLayout(0));
        addMenuItemWithListener(menu, "Выйти из режима раскладки (Shift + X)", e -> exitLayout());
        return menu;
    }
    
    private JMenu createEditMenu() {
        JMenu menu = new JMenu("Действия над деталью");
        addMenuItemWithListener(menu, "Повернуть (Alt + R)", e -> rotateDetail());
        addMenuItemWithListener(menu, "Отразить по горизонтали (Alt + H)", e -> flipDetailHorizontally());
        addMenuItemWithListener(menu, "Отразить по вертикали (Alt + V)", e -> flipDetailVertically());
        addMenuItemWithListener(menu, "Изменить размер (Alt + S)", e -> scaleDetail());
        addMenuItemWithListener(menu, "Сдвинуть по оси X (Alt + X)", e -> shiftDetailX());
        addMenuItemWithListener(menu, "Сдвинуть по оси Y (Alt + Y)", e -> shiftDetailY());
        return menu;
    }
    
    private JMenu createHelpMenu() {
        JMenu menu = new JMenu("Справка");
        addMenuItemWithListener(menu, "О программе", e -> showAbout());
        return menu;
    }
    
    private void addMenuItemWithListener(JMenu menu, String text, java.awt.event.ActionListener listener) {
        var item = new JMenuItem(text);
        item.addActionListener(listener);
        menu.add(item);
    }
    
    private void initializeToolBar() {
        toolBar.setBackground(new Color(240, 240, 240));
        toolBar.add(saveButton);
        toolBar.addSeparator();
        toolBar.add(undoButton);
        toolBar.add(redoButton);
        toolBar.addSeparator();
        toolBar.add(saveImageButton);
        toolBar.addSeparator();
        toolBar.add(propertiesButton);
        toolBar.addSeparator();
        toolBar.add(scaleButton);
        toolBar.addSeparator();
        toolBar.add(detailLabel);
        toolBar.addSeparator();
        toolBar.add(layoutCheckbox);
        
        saveButton.addActionListener(e -> saveFile());
        undoButton.addActionListener(e -> undo());
        redoButton.addActionListener(e -> redo());
        saveImageButton.addActionListener(e -> saveAsImage());
        propertiesButton.addActionListener(e -> showProperties());
        scaleButton.addActionListener(e -> setCanvasScale(0));
        layoutCheckbox.addItemListener(e -> currentDetail.onRasclad = e.getStateChange() == ItemEvent.SELECTED);
    }
    
    private JButton createToolButton(String iconName, String tooltip) {
        return new ToolButton(iconName, tooltip);
    }
    
    private void initializeEventListeners() {
        setupCanvasMouseListeners();
        setupKeyboardShortcuts();
        initializeEditPanel();
    }
    
    private void setupCanvasMouseListeners() {
        canvasPane.setComponentPopupMenu(new JPopupMenu());
        canvasPane.setDoubleBuffered(true);
        canvasPane.getViewport().setPreferredSize(new Dimension(850, 0));
        canvasPane.getViewport().setBackground(new Color(40, 80, 120));
        
        canvasPane.addMouseListener(createCanvasMouseListener());
        canvasPane.addMouseWheelListener(createMouseWheelListener());
        canvasPane.addMouseMotionListener(createCanvasMouseMotionListener());
    }
    
    private MouseAdapter createTreeMouseListener() {
        return new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    handleTreeContextMenu(e);
                } else {
                    selectDetailFromTree();
                }
            }
        };
    }
    
    private void handleTreeContextMenu(MouseEvent e) {
        TreePath path = detailTree.getPathForLocation(e.getX(), e.getY());
        TreePath rootPath = detailTree.getPathForRow(0);
        treeMouseX = e.getX();
        treeMouseY = e.getY();
        
        if (path != null) {
            detailTree.setSelectionPath(path);
            if (path == rootPath) {
                rootPopupMenu.show(detailTree, treeMouseX, treeMouseY);
            } else {
                detailPopupMenu.show(detailTree, treeMouseX, treeMouseY);
            }
        } else {
            emptyPopupMenu.show(detailTree, treeMouseX, treeMouseY);
        }
    }
    
    private MouseAdapter createCanvasMouseListener() {
        return new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mousePresTime = System.currentTimeMillis();
            }
            
            @Override
            public void mouseReleased(MouseEvent e) {
                handleCanvasClick(e);
            }
        };
    }
    
    private void handleCanvasClick(MouseEvent e) {
        clickDuration = System.currentTimeMillis() - mousePresTime;
        
        if (!product.rascladMode) {
            handleDetailDrawing(e);
        } else {
            selectDetailOnLayout(e);
        }
    }
    
    private void handleDetailDrawing(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e) && clickDuration < CLICK_DURATION_THRESHOLD_MS) {
            float x = e.getX() * 1f / canvasHeight / product.scaling;
            float y = e.getY() * 1f / canvasHeight / product.scaling;
            currentDetail.addVertex(new Dot(x, y));
            product.changed = true;
            updateDisplay(0);
        }
    }
    
    private void selectDetailOnLayout(MouseEvent e) {
        for (int i = 0; i < product.details.size(); i++) {
            Detail detail = product.details.get(i);
            if (isPointInDetailPolygon(e.getX(), e.getY(), detail)) {
                currentDetail = detail;
                selectedDetailIndex = i;
                break;
            }
        }
    }
    
    private boolean isPointInDetailPolygon(int x, int y, Detail detail) {
        int vertexCount = detail.vertices.size();
        int[] xPoints = new int[vertexCount];
        int[] yPoints = new int[vertexCount];
        
        for (int i = 0; i < vertexCount; i++) {
            xPoints[i] = detail.vertices.get(i).intX(canvasHeight * product.scaling);
            yPoints[i] = detail.vertices.get(i).intY(canvasHeight * product.scaling);
        }
        
        Polygon polygon = new Polygon(xPoints, yPoints, vertexCount);
        return polygon.contains(x, y);
    }
    
    private MouseWheelListener createMouseWheelListener() {
        return e -> {
            try {
                if (currentDetail == null || product.rascladMode) return;
                if (currentDetail.vertices.size() <= 1) return;
                
                normalizeDetailPosition(currentDetail);
                currentDetail.rotate(e.getWheelRotation());
                
                if (product.rascladMode && hasDetailCollision(currentDetail)) {
                    currentDetail.rotate(-e.getWheelRotation());
                }
                
                updateDisplay(0);
                redraw(currentDetail, canvasPane.getGraphics(), 0);
            } catch (Exception ex) {
                // Игнорируем ошибки при вращении
            }
        };
    }
    
    private boolean hasDetailCollision(Detail detail) {
        for (Detail other : product.details) {
            if (other != detail && detail.intersects(other)) {
                return true;
            }
        }
        return false;
    }
    
    private MouseMotionAdapter createCanvasMouseMotionListener() {
        return new MouseMotionAdapter() {
            private Point lastPoint = null;
            
            @Override
            public void mouseDragged(MouseEvent e) {
                if (clickDuration < 300 && !product.rascladMode || currentDetail == null) {
                    return;
                }
                
                if (!SwingUtilities.isLeftMouseButton(e)) {
                    lastPoint = null;
                    return;
                }
                
                if (lastPoint == null) {
                    lastPoint = e.getPoint();
                    return;
                }
                
                moveDetailOnCanvas(e);
                redraw(currentDetail, canvasPane.getGraphics(), 0);
                updateDisplay(0);
            }
            
            @Override
            public void mouseMoved(MouseEvent e) {
                lastPoint = null;
            }
        };
    }
    
    private void moveDetailOnCanvas(MouseEvent e) {
        float dxPixels = e.getX() - lastPoint.x;
        float dyPixels = e.getY() - lastPoint.y;
        float dx = dxPixels / (canvasHeight * product.scaling);
        float dy = dyPixels / (canvasHeight * product.scaling);
        
        lastPoint = e.getPoint();
        
        moveDetailWithConstraints(dx, dy);
    }
    
    private void moveDetailWithConstraints(float dx, float dy) {
        float step = 0.005f;
        moveDetailAxis(dx, true, step);
        moveDetailAxis(dy, false, step);
    }
    
    private void moveDetailAxis(float delta, boolean isXAxis, float step) {
        float remaining = delta;
        while (Math.abs(remaining) > 1e-6f) {
            float stepSize = Math.signum(remaining) * Math.min(step, Math.abs(remaining));
            
            if (isXAxis) {
                if (currentDetail.minX() + stepSize < 0) {
                    stepSize = -currentDetail.minX();
                }
                if (Math.abs(stepSize) > 1e-6f) {
                    currentDetail.shiftX(stepSize);
                }
            } else {
                if (currentDetail.minY() + stepSize < 0) {
                    stepSize = -currentDetail.minY();
                }
                if (Math.abs(stepSize) > 1e-6f) {
                    currentDetail.shiftY(stepSize);
                }
            }
            remaining -= stepSize;
        }
    }
    
    private void setupKeyboardShortcuts() {
        InputMap inputMap = canvasPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = canvasPane.getActionMap();
        
        registerKeyboardShortcut(inputMap, actionMap, KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK, "Undo", this::undo);
        registerKeyboardShortcut(inputMap, actionMap, KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK, "Redo", this::redo);
        registerKeyboardShortcut(inputMap, actionMap, KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK, "Save", this::saveFile);
        registerKeyboardShortcut(inputMap, actionMap, KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK, "Rotate", this::rotateDetail);
        registerKeyboardShortcut(inputMap, actionMap, KeyEvent.VK_H, InputEvent.ALT_DOWN_MASK, "FlipH", this::flipDetailHorizontally);
        registerKeyboardShortcut(inputMap, actionMap, KeyEvent.VK_V, InputEvent.ALT_DOWN_MASK, "FlipV", this::flipDetailVertically);
        registerKeyboardShortcut(inputMap, actionMap, KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK, "Scale", this::scaleDetail);
        registerKeyboardShortcut(inputMap, actionMap, KeyEvent.VK_X, InputEvent.ALT_DOWN_MASK, "ShiftX", this::shiftDetailX);
        registerKeyboardShortcut(inputMap, actionMap, KeyEvent.VK_Y, InputEvent.ALT_DOWN_MASK, "ShiftY", this::shiftDetailY);
        registerKeyboardShortcut(inputMap, actionMap, KeyEvent.VK_E, InputEvent.SHIFT_DOWN_MASK, "EditLayout", () -> performLayout(0));
        registerKeyboardShortcut(inputMap, actionMap, KeyEvent.VK_F, InputEvent.SHIFT_DOWN_MASK, "FastLayout", () -> performLayout(1));
        registerKeyboardShortcut(inputMap, actionMap, KeyEvent.VK_A, InputEvent.SHIFT_DOWN_MASK, "AILayout", () -> performLayout(2));
        registerKeyboardShortcut(inputMap, actionMap, KeyEvent.VK_X, InputEvent.SHIFT_DOWN_MASK, "ExitLayout", this::exitLayout);
        registerKeyboardShortcut(inputMap, actionMap, KeyEvent.VK_F5, 0, "Refresh", () -> updateDisplay(1));
        registerKeyboardShortcut(inputMap, actionMap, KeyEvent.VK_DELETE, 0, "Delete", this::deleteCurrentVertex);
    }
    
    private void registerKeyboardShortcut(InputMap inputMap, ActionMap actionMap, int keyCode, int modifiers, String actionName, Runnable action) {
        KeyStroke keyStroke = KeyStroke.getKeyStroke(keyCode, modifiers);
        inputMap.put(keyStroke, actionName);
        actionMap.put(actionName, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }
    
    private void initializeEditPanel() {
        editPanel.setBackground(new Color(240, 240, 240));
        editPanel.setEditable(false);
        editScrollPane = new JScrollPane(editPanel);
        editScrollPane.setPreferredSize(new Dimension(220, 0));
        editScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    }
    
    private void initializeWindowSettings() {
        setVisible(true);
        setSize(1280, 768);
        setLayout(new BorderLayout());
        setTitle("Автоматизация раскладки лекал");
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        
        addWindowListener(new WindowListener() {
            public void windowClosing(WindowEvent e) { askToSave(true); }
            public void windowActivated(WindowEvent e) {}
            public void windowClosed(WindowEvent e) {}
            public void windowDeactivated(WindowEvent e) {}
            public void windowDeiconified(WindowEvent e) {}
            public void windowIconified(WindowEvent e) {}
            public void windowOpened(WindowEvent e) {}
        });
        
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, hierarchyPane, canvasPane);
        splitPane.setEnabled(false);
        splitPane.setDividerSize(0);
        
        add(editScrollPane, BorderLayout.EAST);
        add(toolBar, BorderLayout.NORTH);
        add(splitPane, BorderLayout.CENTER);
    }
    
    // ========== РЕНДЕРИНГ И ОТРИСОВКА ==========
    
    private void startRenderTimer() {
        renderTimer = new Timer(TIMER_DELAY_MS, e -> onTimerTick());
        renderTimer.start();
    }
    
    private void onTimerTick() {
        updateVisibility();
        checkCanvasResize();
    }
    
    private void updateVisibility() {
        boolean isNotLayoutMode = !product.rascladMode;
        scaleButton.setVisible(isNotLayoutMode);
        layoutCheckbox.setVisible(isNotLayoutMode);
    }
    
    private void checkCanvasResize() {
        if (canvasHeight != canvasPane.getHeight() || canvasWidth != canvasPane.getWidth()) {
            canvasHeight = canvasPane.getHeight();
            canvasWidth = canvasPane.getWidth();
            updateDisplay(0);
        }
    }
    
    private void redraw(Detail detail, Graphics g, int param) {
        if (g == null) return;
        
        int vertexCount = detail.vertices.size();
        int[] xPoints = new int[vertexCount];
        int[] yPoints = new int[vertexCount];
        
        for (int i = 0; i < vertexCount; i++) {
            xPoints[i] = detail.vertices.get(i).intX(canvasHeight * product.scaling);
            yPoints[i] = detail.vertices.get(i).intY(canvasHeight * product.scaling);
        }
        
        if (vertexCount > 2) {
            g.setColor(isDetailSelected(detail) && product.rascladMode ? Color.yellow : new Color(0, 228, 228));
            g.fillPolygon(xPoints, yPoints, vertexCount);
        } else if (vertexCount == 2) {
            g.setColor(isDetailSelected(detail) && product.rascladMode && param == 0 ? Color.yellow : new Color(0, 228, 228));
            g.drawLine(xPoints[0], yPoints[0], xPoints[1], yPoints[1]);
        }
        
        if (vertexCount > 0 && detail == currentDetail && !product.rascladMode) {
            g.setColor(Color.red);
            int highlightVertex = currentDetail.current;
            g.fillOval(xPoints[highlightVertex] - 3, yPoints[highlightVertex] - 3, 6, 6);
        }
        
        if (shouldRedrawVertices) {
            canvasPane.paint(canvasPane.getGraphics());
            shouldRedrawVertices = false;
        }
    }
    
    private boolean isDetailSelected(Detail detail) {
        return detail == currentDetail;
    }
    
    // ========== ОПЕРАЦИИ С ДЕТАЛЯМИ ==========
    
    private void addDetail(Detail detail, int mode, int index) {
        var select = detailTree.getSelectionModel();
        select.setSelectionPath(detailTree.getPathForRow(0));
        
        if (mode == 0) {
            String name = JOptionPane.showInputDialog("Введите название детали: ", detail.name);
            if (name == null || name.isEmpty()) return;
            
            if (name.length() > MAX_NAME_LENGTH) {
                JOptionPane.showMessageDialog(null, "Название должно быть не больше " + MAX_NAME_LENGTH + " символов!", "Сообщение", JOptionPane.WARNING_MESSAGE);
                return;
            }
            detail.name = name;
        }
        
        selectedDetailIndex = index + 1;
        currentDetail = detail;
        product.details.add(selectedDetailIndex, detail);
        currentDetail = product.details.get(selectedDetailIndex);
        product.rascladMode = false;
        
        var newNode = new DefaultMutableTreeNode(detail.name);
        var selectedNode = (DefaultMutableTreeNode) detailTree.getSelectionPath().getLastPathComponent();
        selectedNode.insert(newNode, selectedDetailIndex);
        
        updateDisplay(1);
        detailTree.setSelectionPath(detailTree.getPathForRow(selectedDetailIndex + 1));
        selectDetailFromTree();
    }
    
    private void removeDetail() {
        TreePath path = detailTree.getPathForLocation(treeMouseX, treeMouseY);
        if (path == null) return;
        
        var selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        selectedDetailIndex = selectedNode.getParent().getIndex(selectedNode);
        product.details.remove(selectedDetailIndex);
        
        while (selectedDetailIndex >= product.details.size()) {
            selectedDetailIndex--;
        }
        
        currentDetail = product.details.get(selectedDetailIndex);
        detailTree.setSelectionPath(detailTree.getPathForRow(selectedDetailIndex + 1));
        selectDetailFromTree();
    }
    
    private void renameDetail() {
        TreePath path = detailTree.getPathForLocation(treeMouseX, treeMouseY);
        TreePath rootPath = detailTree.getPathForRow(0);
        if (path == null) return;
        
        String currentName = "";
        Detail detailToRename = currentDetail;
        var selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        
        if (path != rootPath) {
            var parent = (DefaultMutableTreeNode) selectedNode.getParent();
            int index = parent.getIndex(selectedNode);
            detailToRename = product.details.get(index);
            currentName = detailToRename.name;
        } else {
            currentName = product.name;
        }
        
        String newName = JOptionPane.showInputDialog("Введите новое имя: ", currentName);
        if (newName == null || newName.isEmpty() || newName.equals(currentName)) return;
        
        if (newName.length() > MAX_NAME_LENGTH) {
            JOptionPane.showMessageDialog(null, "Название должно быть не больше " + MAX_NAME_LENGTH + " символов!", "Сообщение", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        selectedNode.setUserObject(newName);
        if (path == rootPath) {
            product.name = newName;
        } else {
            detailToRename.name = newName;
        }
        
        detailTree.setSelectionPath(detailTree.getPathForRow(selectedDetailIndex + 1));
        selectDetailFromTree();
    }
    
    private void normalizeDetailPosition(Detail detail) {
        if (detail.minX() < 0) detail.shiftX(-detail.minX());
        if (detail.minY() < 0) detail.shiftY(-detail.minY());
    }
    
    private void rotateDetail() {
        try {
            if (!product.rascladMode && currentDetail.vertices.size() > 1) {
                String input = JOptionPane.showInputDialog("Введите угол поворота в градусах: ");
                currentDetail.rotate(Double.parseDouble(input));
                normalizeDetailPosition(currentDetail);
                updateDisplay(0);
            } else {
                showError("Вращать нечего.");
            }
        } catch (Exception e) {
            // Игнорируем ошибки ввода
        }
    }
    
    private void scaleDetail() {
        try {
            if (!product.rascladMode && currentDetail.vertices.size() > 1) {
                String input = JOptionPane.showInputDialog("Во сколько раз изменить размер детали?", 1);
                currentDetail.scale(Float.parseFloat(input));
                updateDisplay(0);
            } else {
                showError("Отрезки не найдены!");
            }
        } catch (Exception e) {
            // Игнорируем ошибки ввода
        }
    }
    
    private void flipDetailVertically() {
        if (currentDetail.vertices.size() > 1) {
            currentDetail.flipVertical();
            updateDisplay(0);
        } else {
            showError("Отрезки не найдены!");
        }
    }
    
    private void flipDetailHorizontally() {
        if (currentDetail.vertices.size() > 1) {
            currentDetail.flipHorizontal();
            updateDisplay(0);
        } else {
            showError("Отрезки не найдены!");
        }
    }
    
    private void shiftDetailX() {
        try {
            if (!product.rascladMode && currentDetail.vertices.size() > 0) {
                String input = JOptionPane.showInputDialog("На сколько мм сдвинуть деталь по оси X", 0);
                currentDetail.shiftX(Float.parseFloat(input) / 1000);
                normalizeDetailPosition(currentDetail);
                updateDisplay(0);
            } else {
                showError("Вершины не найдены!");
            }
        } catch (Exception e) {
            // Игнорируем ошибки ввода
        }
    }
    
    private void shiftDetailY() {
        try {
            if (!product.rascladMode && currentDetail.vertices.size() > 0) {
                String input = JOptionPane.showInputDialog("На сколько мм сдвинуть деталь по оси Y", 0);
                currentDetail.shiftY(Float.parseFloat(input) / 1000);
                normalizeDetailPosition(currentDetail);
                updateDisplay(0);
            } else {
                showError("Вершины не найдены!");
            }
        } catch (Exception e) {
            // Игнорируем ошибки ввода
        }
    }
    
    private void deleteCurrentVertex() {
        if (currentDetail.current >= 0) {
            deleteVertex(currentDetail.current);
        }
    }
    
    private void deleteVertex(int index) {
        currentDetail.vertices.remove(index);
        if (currentDetail.current > 0) currentDetail.current--;
        updateDisplay(0);
    }
    
    // ========== ОПЕРАЦИИ С РАСКЛАДКОЙ ==========
    
    private void performLayout(int mode) {
        if (mode == 2 && product.details.size() < 3) {
            showWarning("Для ИИ-раскладки нужно не менее 3 деталей!");
            return;
        }
        
        scaleButton.setVisible(false);
        if (mode == 0) setCanvasScale(1);
        
        int detailsForLayout = countDetailsForLayout();
        if (detailsForLayout > 0) {
            performLayoutOperation(mode, detailsForLayout);
        } else {
            showWarning("Нет деталей для раскладки!");
            exitLayout();
        }
    }
    
    private int countDetailsForLayout() {
        int count = 0;
        for (Detail detail : product.details) {
            if (detail.vertices.size() < 3) {
                detail.onRasclad = false;
            }
            if (detail.onRasclad) count++;
        }
        return count;
    }
    
    private void performLayoutOperation(int mode, int detailsForLayout) {
        float layoutHeight = product.listHeight;
        
        if (!product.rascladMode || mode > 0) {
            if (mode > 0) {
                try {
                    updateDisplay(1);
                    String distanceInput = JOptionPane.showInputDialog("Введите расстояние между лекалами в мм: ", (int)(DEFAULT_DISTANCE_MM * 1000));
                    product.distance = Float.parseFloat(distanceInput) / 1000;
                    String heightInput = JOptionPane.showInputDialog("Введите ширину полотна в мм: ", (int)(product.listHeight * 1000));
                    layoutHeight = Float.parseFloat(heightInput) / 1000;
                } catch (Exception e) {
                    return;
                }
            }
            
            if (mode == 2) setVisible(false);
            
            if (mode > 0 && product.findRect(layoutHeight, mode)) {
                product.rascladMode = true;
                selectFirstLayoutDetail();
                updateScaleLabel();
                updateDisplay(1);
            } else {
                undo();
            }
        } else {
            updateLayoutBoundaries();
            drawLayoutBoundaries();
            updateScaleLabel();
        }
        
        product.changed = false;
        product.getProperties();
        propertiesButton.setText("Параметры изделия");
    }
    
    private int selectFirstLayoutDetail() {
        if (!currentDetail.onRasclad) {
            for (Detail d : product.details) {
                if (d.onRasclad) {
                    currentDetail = d;
                    selectedDetailIndex = d.index;
                    return d.index;
                }
            }
        }
        return selectedDetailIndex;
    }
    
    private void updateLayoutBoundaries() {
        product.listWidth = 0;
        for (Detail detail : product.details) {
            if (detail.Xmax() > product.listWidth) {
                product.listWidth = detail.Xmax();
            }
        }
    }
    
    private void drawLayoutBoundaries() {
        var g = canvasPane.getGraphics();
        if (g != null) {
            for (Detail detail : product.details) {
                if (detail.onRasclad) redraw(detail, g, 0);
            }
            g.setColor(Color.cyan);
            g.drawRect(0, 0, (int)(product.listWidth * canvasHeight * product.scaling) + 4, (int)(product.listHeight * canvasHeight * product.scaling) + 4);
        }
    }
    
    private void exitLayout() {
        product.rascladMode = false;
        updateDisplay(0);
    }
    
    // ========== МАСШТАБИРОВАНИЕ ==========
    
    private void setCanvasScale(int mode) {
        try {
            if (mode == 0) {
                String input = JOptionPane.showInputDialog("Введите масштаб в %: ");
                product.scaling = Float.parseFloat(input) / 100f;
                if (product.scaling <= 0f) product.scaling = 1f;
                if (product.scaling > MAX_SCALE) product.scaling = MAX_SCALE;
            } else if (mode == 1) {
                product.scaling = calculateAutoScale();
                if (shouldRedrawVertices) {
                    updateDisplay(1);
                    shouldRedrawVertices = false;
                }
            }
            updateScaleLabel();
            updateDisplay(0);
        } catch (Exception e) {
            // Игнорируем неверный ввод
        }
    }
    
    private float calculateAutoScale() {
        float scaleByWidth = (canvasPane.getWidth() * 0.95f) / (product.listWidth * canvasHeight);
        float scaleByHeight = (canvasPane.getHeight() * 0.95f) / (product.listHeight * canvasHeight);
        float scale = Math.min(scaleByWidth, scaleByHeight);
        return Math.max(MIN_SCALE, Math.min(scale, MAX_SCALE));
    }
    
    private void updateScaleLabel() {
        scaleButton.setText("Масштаб: " + (int)(product.scaling * 100) + "%");
    }
    
    // ========== ФАЙЛОВЫЕ ОПЕРАЦИИ ==========
    
    public void saveFile() {
        if (product.filePath.length() > 2) {
            product.saveToFile(product.filePath);
        } else {
            saveFileAs();
        }
    }
    
    private void saveFileAs() {
        if (product.totalVertices() < 3 && product.details.size() < 2) {
            showWarning("Нечего сохранять.");
            return;
        }
        
        var fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить как");
        var filter = new FileNameExtensionFilter("*.xml", "XML");
        fileChooser.setFileFilter(filter);
        
        Product backup = new Product(product, 0);
        try {
            if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                product.filePath = fileToSave.getAbsolutePath();
                product.saveToFile(product.filePath);
            }
        } catch (Exception ex) {
            product = backup;
        }
        
        updateDisplay(0);
    }
    
    private void openFile() {
        try {
            if (!askToSave(false)) return;
            
            var fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Открыть файл");
            var filter = new FileNameExtensionFilter("*.xml", "xml");
            fileChooser.setFileFilter(filter);
            
            if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
                File fileToOpen = fileChooser.getSelectedFile();
                product.filePath = fileToOpen.getAbsolutePath();
                
                if (product.filePath.endsWith(DEFAULT_FILE_EXTENSION)) {
                    dispose();
                    new Form1(new Product(product.filePath), product.filePath);
                } else {
                    showError("Неверное расширение файла!");
                }
            } else {
                dispose();
                new Form1(product, product.filePath);
            }
        } catch (Exception ex) {
            showError("Данные в файле повреждены!");
        }
    }
    
    private void createNewProject() {
        askToSave(true);
        dispose();
        new Form1(new Product(), "");
    }
    
    private void saveAsImage() {
        try {
            if (!product.rascladMode && currentDetail.vertices.size() < 2) {
                showWarning("Линии отсутствуют.");
                return;
            }
            
            var fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Сохранить как PNG");
            var filter = new FileNameExtensionFilter("*.png", "PNG");
            fileChooser.setFileFilter(filter);
            
            int userSelection = fileChooser.showSaveDialog(null);
            if (userSelection == JFileChooser.APPROVE_OPTION) {
                BufferedImage image = generateLayoutImage();
                String path = fileChooser.getSelectedFile().getAbsolutePath();
                if (!path.endsWith(".png") && !path.endsWith(".PNG")) {
                    path += ".png";
                }
                
                ImageIO.write(image, "PNG", new File(path));
                showInfo("Снимок раскладки сохранён в файл " + path);
                Desktop.getDesktop().open(new File(path));
            }
        } catch (Exception e) {
            // Игнорируем ошибки
        }
    }
    
    private BufferedImage generateLayoutImage() {
        BufferedImage image;
        
        if (product.rascladMode) {
            int height = (int)(product.listHeight * canvasHeight * product.scaling);
            int width = (int)(product.listWidth * canvasWidth * product.scaling / 1.55f);
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics g = image.createGraphics();
            g.setColor(new Color(40, 80, 120));
            g.fillRect(0, 0, width, height);
            for (Detail detail : product.details) {
                if (detail.onRasclad) redraw(detail, g, 0);
            }
            g.dispose();
        } else {
            Detail detailCopy = new Detail(currentDetail);
            detailCopy.normalize();
            int height = Math.round(detailCopy.Ymax() * canvasHeight * product.scaling);
            int width = (int)(detailCopy.Xmax() * canvasWidth * product.scaling / 1.55f);
            image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            Graphics g = image.createGraphics();
            g.setColor(new Color(40, 80, 120));
            g.fillRect(0, 0, width, height);
            redraw(detailCopy, g, 1);
            g.dispose();
        }
        
        return image;
    }
    
    // ========== ИСТОРИЯ ОПЕРАЦИЙ ==========
    
    private void recordAction() {
        currentHistoryIndex++;
        while (currentHistoryIndex < undoStack.size()) {
            undoStack.remove(undoStack.size() - 1);
            selectedStack.remove(selectedStack.size() - 1);
        }
        
        undoStack.add(new Product(product, 0));
        selectedStack.add(selectedDetailIndex);
    }
    
    private void undo() {
        if (currentHistoryIndex > 0) {
            currentHistoryIndex--;
            product = new Product(undoStack.get(currentHistoryIndex), 0);
            selectedDetailIndex = selectedStack.get(currentHistoryIndex);
            currentDetail = product.details.get(selectedDetailIndex);
            updateScaleLabel();
            updateDisplay(1);
            detailTree.setSelectionRow(selectedDetailIndex + 1);
            shouldRedrawVertices = true;
        }
    }
    
    private void redo() {
        if (currentHistoryIndex < undoStack.size() - 1) {
            currentHistoryIndex++;
            product = new Product(undoStack.get(currentHistoryIndex), 0);
            selectedDetailIndex = selectedStack.get(currentHistoryIndex);
            currentDetail = product.details.get(selectedDetailIndex);
            updateDisplay(1);
            detailTree.setSelectionRow(selectedDetailIndex + 1);
            shouldRedrawVertices = true;
        }
    }
    
    // ========== ОБНОВЛЕНИЕ ИНТЕРФЕЙСА ==========
    
    private void updateDisplay(int param) {
        updateDetailTree();
        updateUIElementsVisibility();
        updateDetailLabel();
        updateVertexFields(param);
    }
    
    private void updateDetailTree() {
        product.updateTree();
        DefaultTreeModel model = (DefaultTreeModel) detailTree.getModel();
        model.setRoot(product.root);
        model.reload();
    }
    
    private void updateUIElementsVisibility() {
        boolean isNotLayoutMode = !product.rascladMode;
        layoutCheckbox.setVisible(isNotLayoutMode);
        if (editScrollPane != null) editScrollPane.setVisible(isNotLayoutMode);
    }
    
    private void updateDetailLabel() {
        detailLabel.setText((selectedDetailIndex + 1) + ". " + currentDetail.name);
        layoutCheckbox.setSelected(currentDetail.onRasclad);
    }
    
    private void updateVertexFields(int param) {
        if (param == 0) recordAction();
        
        editPanel.removeAll();
        editPanel.setText("");
        xCoordinateFields.clear();
        yCoordinateFields.clear();
        
        if (!product.rascladMode) {
            addCoordinateLabels();
            
            for (int i = 0; i < currentDetail.vertices.size(); i++) {
                editPanel.setText(editPanel.getText() + "\n\n");
                addVertexRow(i);
            }
        }
        
        repaint();
    }
    
    private void addCoordinateLabels() {
        JLabel coordLabel = new JLabel("Координаты вершин в мм");
        JLabel xLabel = new JLabel("X");
        JLabel yLabel = new JLabel("Y");
        
        coordLabel.setBounds(15, 10, 180, 20);
        xLabel.setBounds(55, 35, 20, 20);
        yLabel.setBounds(125, 35, 20, 20);
        
        coordLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        xLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        yLabel.setFont(new Font("Arial", Font.PLAIN, 16));
        
        editPanel.add(coordLabel);
        editPanel.add(xLabel);
        editPanel.add(yLabel);
    }
    
    private void addVertexRow(int index) {
        var xField = new NumericField(index);
        var yField = new NumericField(index);
        
        xField.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (xField.index != currentDetail.current) {
                    currentDetail.current = xField.index;
                    updateDisplay(0);
                }
            }
        });
        
        yField.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (yField.index != currentDetail.current) {
                    currentDetail.current = yField.index;
                    updateDisplay(0);
                }
            }
        });
        
        xField.setText(Math.round(currentDetail.vertices.get(index).intX(100000)) / 100f + "");
        yField.setText(Math.round(currentDetail.vertices.get(index).intY(100000)) / 100f + "");
        
        xCoordinateFields.add(xField);
        yCoordinateFields.add(yField);
        
        int fontStyle = index == currentDetail.current ? Font.BOLD : Font.PLAIN;
        addVertexRowComponents(index, fontStyle);
    }
    
    private void addVertexRowComponents(int index, int fontStyle) {
        shouldRedrawVertices = true;
        
        var numLabel = new JLabel((index + 1) + "");
        var deleteButton = createToolButton("delete.png", "Удалить точку (клавиша Del)");
        
        deleteButton.setBounds(170, EDIT_PANEL_OFFSET_Y + 1 + EDIT_PANEL_ROW_HEIGHT * index, 23, 23);
        deleteButton.addActionListener(e -> deleteVertex(index));
        
        numLabel.setBounds(8, EDIT_PANEL_OFFSET_Y + 2 + EDIT_PANEL_ROW_HEIGHT * index, 20, 20);
        numLabel.setFont(new Font("Arial", fontStyle, 14));
        
        editPanel.add(numLabel);
        editPanel.add(deleteButton);
        
        xCoordinateFields.get(index).setBounds(30, EDIT_PANEL_OFFSET_Y + EDIT_PANEL_ROW_HEIGHT * index, 65, 25);
        yCoordinateFields.get(index).setBounds(100, EDIT_PANEL_OFFSET_Y + EDIT_PANEL_ROW_HEIGHT * index, 65, 25);
        
        editPanel.add(xCoordinateFields.get(index));
        editPanel.add(yCoordinateFields.get(index));
    }
    
    private void selectDetailFromTree() {
        try {
            if (detailTree.getSelectionPaths() != null) {
                for (TreePath path : detailTree.getSelectionPaths()) {
                    var selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
                    var parent = (DefaultMutableTreeNode) selectedNode.getParent();
                    selectedDetailIndex = parent.getIndex(selectedNode);
                    currentDetail = product.details.get(selectedDetailIndex);
                    
                    if (!currentDetail.onRasclad) {
                        product.rascladMode = false;
                    }
                    
                    propertiesButton.setText("Площадь детали");
                    redraw(currentDetail, canvasPane.getGraphics(), 0);
                    updateDisplay(0);
                    detailTree.setSelectionPath(detailTree.getPathForRow(selectedDetailIndex + 1));
                }
            }
        } catch (Exception e) {
            // Игнорируем ошибки
        }
    }
    
    // ========== ДИАЛОГОВЫЕ ОКНА И СООБЩЕНИЯ ==========
    
    private void showProperties() {
        if (product.rascladMode) {
            showInfo(product.properties);
        } else {
            showInfo("Площадь детали = " + currentDetail.S() + " cм2");
        }
    }
    
    private void setProductDescription() {
        String description = JOptionPane.showInputDialog("Описание изделия: ", product.description);
        if (description != null && !description.isEmpty()) {
            product.description = description;
        }
    }
    
    private void showAbout() {
        String[] authorInfo = {
            "Автоматизация раскладки лекал",
            "© Золотарёв Дмитрий Андреевич,",
            "Черненко Елена Александровна."
        };
        StringBuilder message = new StringBuilder();
        for (String line : authorInfo) {
            message.append(line).append("\n");
        }
        
        JOptionPane.showMessageDialog(null, message.toString(), "О программе", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private boolean askToSave(boolean needDispose) {
        if (product.totalVertices() == 0 && product.details.size() < 2) {
            if (needDispose) dispose();
            return true;
        }
        
        int option = JOptionPane.showOptionDialog(
            Form1.this,
            "Сохранить текущие данные?",
            "Сохранение данных",
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,
            new Object[]{"Да", "Нет", "Отмена"},
            "Да"
        );
        
        if (option == JOptionPane.YES_OPTION) {
            saveFileAs();
            if (needDispose) dispose();
            return true;
        } else if (option == JOptionPane.NO_OPTION) {
            if (needDispose) dispose();
            return true;
        }
        
        return false;
    }
    
    private void showInfo(String message) {
        JOptionPane.showMessageDialog(null, message, "Информация", JOptionPane.INFORMATION_MESSAGE);
    }
    
    private void showWarning(String message) {
        JOptionPane.showMessageDialog(null, message, "Предупреждение", JOptionPane.WARNING_MESSAGE);
    }
    
    private void showError(String message) {
        JOptionPane.showMessageDialog(null, message, "Ошибка", JOptionPane.ERROR_MESSAGE);
    }
}
