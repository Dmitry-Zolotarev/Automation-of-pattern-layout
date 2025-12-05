import javax.swing.tree.*;

import java.awt.event.InputEvent;
import javax.swing.tree.TreePath;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.swing.filechooser.FileNameExtensionFilter;

public class Form1 extends JFrame 
{//Класс основного окна программы
	JMenuBar menuBar = new JMenuBar();
	JMenuItem createItem = new JMenuItem("Создать новый");
	JMenuItem generateItem = new JMenuItem("Сгенерировать тестовые лекала");
	JMenuItem loadItem = new JMenuItem("Открыть");
	JMenuItem saveItem = new JMenuItem("Сохранить");
	JMenuItem saveAS = new JMenuItem("Сохранить как");
	JMenuItem rotate = new JMenuItem("Повернуть (Alt + R)");
	JMenuItem horizontal = new JMenuItem("Отразить по горизонтали (Alt + H)");
	JMenuItem vertical = new JMenuItem("Отразить по вертикали (Alt + V)");
	JMenuItem scale2 = new JMenuItem("Изменить размер (Alt + S)");
	JMenuItem shiftX = new JMenuItem("Сдвинуть по оси X (Alt + X)");
	JMenuItem shiftY = new JMenuItem("Сдвинуть по оси Y (Alt + Y)");
	
	JMenuItem rotate2 = new JMenuItem("Повернуть деталь (Alt + R)");
	JMenuItem horizontal2 = new JMenuItem("Отразить по горизонтали (Alt + H)");
	JMenuItem vertical2 = new JMenuItem("Отразить по вертикали (Alt + V)");
	JMenuItem scale3 = new JMenuItem("Изменить размер детали (Alt + S)");
	JMenuItem shiftX2 = new JMenuItem("Сдвинуть по оси X (Alt + X)");
	JMenuItem shiftY2 = new JMenuItem("Сдвинуть по оси Y (Alt + Y)");
	
	JMenuItem about = new JMenuItem("О программе");
	JMenuItem fastRasclad = new JMenuItem("Быстрая раскладка (Shift + F)");
	JMenuItem RascladAI = new JMenuItem("ИИ-раскладка (Shift + A)");
	JMenuItem changeRasclad = new JMenuItem("Редактировать раскладку (Shift + E)");
	JMenuItem exitRasclad = new JMenuItem("Выйти из режима раскладки (Shift + X)");
	JMenu fileMenu = new JMenu("Файл");
	JMenu rascladMenu = new JMenu("Раскладка");
	JMenu editMenu = new JMenu("Действия над деталью");
	JMenu helpMenu = new JMenu("Справка");	
	JToolBar toolBar = new JToolBar();
	JLabel X = new JLabel("X");
	JLabel Y = new JLabel("Y");
	JLabel thisDetail = new JLabel();
	JCheckBox onRasclad = new JCheckBox("На раскладку");
	JLabel dotCoords = new JLabel("Координаты вершин в мм");
    JButton save = new ToolButton("save.png", "Сохранить (Ctrl + S)");
    JButton undo = new ToolButton("undo.png", "Отменить (Ctrl + Z)");
    JButton redo = new ToolButton("redo.png", "Вернуть (Ctrl + Shift + Z)");
    JButton saveImage = new JButton("Сохранить как PNG");
    JButton area = new JButton("Площадь детали");
    JButton scale = new JButton("Масштаб: 100%");
	JTextArea editPanel = new JTextArea();
	JFileChooser createProject = new JFileChooser();
	Timer timer; 
	Product product;
	Detail detail;	
	JTree tree;
	JPopupMenu rightClick = new JPopupMenu();
	JPopupMenu popupMenu1 = new JPopupMenu();
	JPopupMenu popupMenu2 = new JPopupMenu();
	JScrollPane canvas = new JScrollPane();
	JScrollPane hierarchy;
	JScrollPane editScroll;
	JMenuItem delete = new JMenuItem("Удалить");
	JMenuItem exitRasclad2 = new JMenuItem("Редактировать деталь");
	Boolean dotChanged = true, didLayout = false;
	int H = 0, W = 0, treeX = 0, treeY = 0, undoRedo = -1, selected = 0;
	
	List<NumericField> Xfields = new ArrayList<>();
	List<NumericField> Yfields = new ArrayList<>();
	List<Product> Backup = new ArrayList<>();
	List<Integer> Selected = new ArrayList<>();
	long pressTime, clickDuration;
	Form1(Product t, String file) 
	{	
		
		product = t; 
		product.filePath = file;
		recordAction();
		product.main = this;
		tree = new JTree(product.root);	
		detail = product.details.get(selected);
		popupMenu1 = new JPopupMenu();
        var rename = new JMenuItem("Переименовать");
        rename.addActionListener(e -> rename());
        var duplicate = new JMenuItem("Дублировать");
        duplicate.addActionListener(e -> addDetail(new Detail(detail), 1, selected));
        exitRasclad2.addActionListener(e -> exitRasclad());
        delete.addActionListener(e -> removeDetail());
        popupMenu1.add(rename);
        popupMenu1.add(duplicate);
        popupMenu1.add(exitRasclad2);
        popupMenu2 = new JPopupMenu();
        var addItem = new JMenuItem("Создать новую деталь");   
        addItem.addActionListener(e -> addDetail(new Detail(), 0, product.details.size() - 1));
        popupMenu2.add(addItem);
        
        var popupMenu3 = new JPopupMenu();
        var rename2 = new JMenuItem("Переименовать");
        rename2.addActionListener(e -> rename());
        var setDescription = new JMenuItem("Редактировать описание");
        setDescription.addActionListener(e -> setDescription());
        popupMenu3.add(rename2);
        popupMenu3.add(setDescription);
        
        tree.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mousePressed(java.awt.event.MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
                    TreePath path2 = tree.getPathForRow(0);
                    tree.setSelectionPath(path);
                    treeX = e.getX(); treeY = e.getY();
                    if (path != null) {
                    	if(path == path2) popupMenu3.show(tree, treeX, treeY);
                    	else popupMenu1.show(tree, treeX, treeY);
                    }
                    else popupMenu2.show(tree, treeX, treeY);
                }
                else treeClick(0);
            }
        });
		hierarchy = new JScrollPane(tree);
		detail = product.details.get(0);
    	
		timer = new Timer(10, e -> onTimer());//Обновление изображения деталей 100 раз в секунду
        timer.start();
        canvas.setComponentPopupMenu(rightClick);      
        canvas.addMouseListener(new MouseAdapter() {
        	@Override
            public void mousePressed(MouseEvent e) {
                // Запоминаем момент нажатия
                if (SwingUtilities.isLeftMouseButton(e)) pressTime = System.currentTimeMillis();

                // Правая кнопка — контекстное меню
                //else if (!product.rascladMode && SwingUtilities.isRightMouseButton(e)) rightClick.show(canvas, e.getX(), e.getY());

            }
            @Override//Обработка клика по холсту для разметки лекал деталей
            public void mouseReleased(MouseEvent e) {
            	
                clickDuration = System.currentTimeMillis() - pressTime;
            	if(!product.rascladMode) {//Если режим ракладки выключен, то происходит рисование новой детали по точкам.
                	if (SwingUtilities.isLeftMouseButton(e) && clickDuration < 200) {
                		var dot = new Dot(e.getX() * 1f / H / product.scaling, e.getY() * 1f / H / product.scaling);
                		detail.addVertex(dot);
                		product.changed = true;
                        updateFields(0);                 
                	}//Если режим ракладки включен, то происходит выбор детали на раскладке мышью.
                	
            	}
            	else {
            		for(int i = 0; i < product.details.size(); i++) {
            			var d = product.details.get(i);
            			int n = d.vertices.size(), xPoints[] = new int[n], yPoints[] = new int[n];
            			for(int j = 0; j < n; j++) {
            	            xPoints[j] = d.vertices.get(j).intX(H * product.scaling);
            	            yPoints[j] = d.vertices.get(j).intY(H * product.scaling);
            	    	}
            			var figure = new Polygon(xPoints, yPoints, n);
            			if(figure.contains(e.getX(), e.getY())) {
            				detail = d;
            				selected = i;
            				break;
            			}
            		}
            	}	
            }  
            
        });   
        canvas.addMouseWheelListener(new MouseWheelListener() {
            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                try {
                    if (detail == null || product.rascladMode) return;
                    if (detail.vertices.size() <= 1) return; // нечего вращать
                    // Один "щелчок" колёсика — 5 градусов (можно изменить)
                    if (detail.minX() < 0) detail.shiftX(-detail.minX());
                    if (detail.minY() < 0) detail.shiftY(-detail.minY());
                    detail.rotate(e.getWheelRotation());
                    // В режиме раскладки отменяем вращение при пересечении с другими деталями
                    if (product.rascladMode) {
                        for (var d : product.details) {
                            if (d != detail && detail.intersects(d)) {
                                // откатить вращение
                                detail.rotate(-e.getWheelRotation());
                                break;
                            }
                        }
                    }
                    updateFields(0);
                    reDraw(detail, canvas.getGraphics(), 0);
                } catch (Exception ex) { /* игнорируем ошибки */ }
            }
        });
        canvas.addMouseMotionListener(new MouseMotionAdapter() {
            Point last = null;
            @Override
            public void mouseDragged(MouseEvent e) {
            	if(clickDuration < 300 && !product.rascladMode || detail == null) return;
                // если нет выбранной детали — ничего не делаем
                if (!SwingUtilities.isLeftMouseButton(e)) { 
                    last = null; 
                    return; 
                }

                if (last == null) { 
                    last = e.getPoint(); 
                    return; 
                }
                float dxPixels = e.getX() - last.x,  dyPixels = e.getY() - last.y,
                	  dx = dxPixels / (H * product.scaling), dy = dyPixels / (H * product.scaling),
                	  remainingX = dx, remainingY = dy, step = 0.005f;
                last = e.getPoint();
                // === Перемещение по X ===
                while (Math.abs(remainingX) > 1e-6f) {
                    float s = Math.signum(remainingX) * Math.min(step, Math.abs(remainingX));
                    // ограничение по левой границе
                    if (detail.minX() + s < 0) {
                        s = -detail.minX();
                        if (Math.abs(s) < 1e-6f) break;
                    }

                    detail.shiftX(s);
                    remainingX -= s;
                }
                // === Перемещение по Y ===
                while (Math.abs(remainingY) > 1e-6f) {
                    float s = Math.signum(remainingY) * Math.min(step, Math.abs(remainingY));

                    // ограничение по верхней границе
                    if (detail.minY() + s < 0) {
                        s = -detail.minY();
                        if (Math.abs(s) < 1e-6f) break;
                    }

                    detail.shiftY(s);

                    remainingY -= s;
                }
                reDraw(detail, canvas.getGraphics(), 0);
                updateFields(0);
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                last = null; // сбрасываем начальную точку при обычном движении мыши
            }
        });
        saveAS.addActionListener(e -> saveFileAs());
        saveItem.addActionListener(e -> saveFile());
        generateItem.addActionListener(e -> {
        	product.generateTestDetails();
        	detail = product.details.get(0);
        	updateFields(0);
        	
        });
        loadItem.addActionListener(e -> openFile());
        rotate.addActionListener(e -> rotate());
        vertical.addActionListener(e -> flipVertical());
        horizontal.addActionListener(e -> flipHorizontal());
        shiftX.addActionListener(e -> shiftX());
        shiftY.addActionListener(e -> shiftY());
        scale2.addActionListener(e -> scaleDetail());
        rotate2.addActionListener(e -> rotate());
        vertical2.addActionListener(e -> flipVertical());
        horizontal2.addActionListener(e -> flipHorizontal());
        scale3.addActionListener(e -> scaleDetail());
        shiftX2.addActionListener(e -> shiftX());
        shiftY2.addActionListener(e -> shiftY());
        save.addActionListener(e -> saveFile());
        fastRasclad.addActionListener(e -> Rasclad(1));
        RascladAI.addActionListener(e -> Rasclad(2));
        changeRasclad.addActionListener(e -> Rasclad(0));
        exitRasclad.addActionListener(e -> exitRasclad());
        area.addActionListener(e -> getArea());  
        scale.addActionListener(e -> setScale(0));
        undo.addActionListener(e -> undo());
        redo.addActionListener(e -> redo());
        about.addActionListener(e -> about());
        saveImage.addActionListener(e -> saveImage());
        
        createItem.addActionListener(e -> {	
        	saveAsk(true); dispose();
        	new Form1(new Product(), "");
        });
        onRasclad.addItemListener(new ItemListener() {
            @Override
            public void itemStateChanged(ItemEvent e) {
                detail.onRasclad = e.getStateChange() == ItemEvent.SELECTED;
            }
        });       
        canvas.setDoubleBuffered(true);
        
        fileMenu.add(createItem);
        fileMenu.add(generateItem);
		fileMenu.add(loadItem);
		fileMenu.add(saveItem);
		fileMenu.add(saveAS);
		
		editMenu.add(rotate);
		editMenu.add(vertical);
		editMenu.add(horizontal);
		editMenu.add(scale2);
		editMenu.add(shiftX);
		editMenu.add(shiftY);
		
		rightClick.add(rotate2);
		rightClick.add(vertical2);
		rightClick.add(horizontal2);
		rightClick.add(scale3);
		rightClick.add(shiftX2);
		rightClick.add(shiftY2);
		
		rascladMenu.add(fastRasclad);
		rascladMenu.add(RascladAI);
		rascladMenu.add(changeRasclad);
		rascladMenu.add(exitRasclad);
		
		helpMenu.add(about);
		
		menuBar.add(fileMenu);
		menuBar.add(rascladMenu);
		menuBar.add(editMenu);
		menuBar.add(helpMenu);
		
		toolBar.setBackground(new Color(240, 240, 240));
		toolBar.add(save);
        toolBar.addSeparator();
        toolBar.add(undo);
        toolBar.add(redo);
        toolBar.addSeparator();
        toolBar.add(saveImage);
        toolBar.addSeparator();
        toolBar.add(area);
        toolBar.addSeparator(); 
        toolBar.add(scale);
        toolBar.addSeparator();
        toolBar.add(thisDetail);
        toolBar.addSeparator();
        toolBar.add(onRasclad);	
		
		canvas.getViewport().setPreferredSize(new Dimension(850, 0));
		hierarchy.getViewport().setPreferredSize(new Dimension(200, 0));
		canvas.getViewport().setBackground(new Color(40, 80, 120));
		editPanel.setBackground(new Color(240, 240, 240));
		
		editPanel.setEditable(false);
		JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, hierarchy, canvas);
		splitPane.setEnabled(false);
		splitPane.setDividerSize(0);
		
		scale.setToolTipText("Изменить масштаб");
		thisDetail.setToolTipText("Текущая деталь");
		
		dotCoords.setBounds(15, 10, 180, 20);
		X.setBounds(55, 35, 20, 20);
		Y.setBounds(125, 35, 20, 20);
		
		dotCoords.setFont(new Font("Arial", Font.PLAIN, 14));
		X.setFont(new Font("Arial", Font.PLAIN, 16));
		Y.setFont(new Font("Arial", Font.PLAIN, 16));
		updateFields(0);
		reDraw(detail, canvas.getGraphics(), 0);
		keyControl();
		
	    setVisible(true);
		setSize(1280, 768);
		setLayout(new BorderLayout());
		setTitle("Автоматизация раскладки лекал");
		setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		
		addWindowListener(new WindowListener() {
			public void windowActivated(WindowEvent event) {
			}
			public void windowClosed(WindowEvent event) {}
			public void windowClosing(WindowEvent event) {saveAsk(true);}
			public void windowDeactivated(WindowEvent event) {}
			public void windowDeiconified(WindowEvent event) {}
			public void windowIconified(WindowEvent event) {}
			public void windowOpened(WindowEvent event) {
			}
		});
		editScroll = new JScrollPane(editPanel);
		editScroll.setPreferredSize(new Dimension(220, 0)); 
		editScroll.setVerticalScrollBarPolicy(22);
		add(editScroll, BorderLayout.EAST);
		add(toolBar, BorderLayout.NORTH);
		add(splitPane, BorderLayout.CENTER);
		setJMenuBar(menuBar);	
		
	}
	private void onTimer() {
		scale2.setVisible(!product.rascladMode);
		rotate.setVisible(!product.rascladMode);
    	shiftX.setVisible(!product.rascladMode);
    	shiftY.setVisible(!product.rascladMode);
    	
    	scale3.setVisible(!product.rascladMode);
		rotate2.setVisible(!product.rascladMode);
    	shiftX2.setVisible(!product.rascladMode);
    	shiftY2.setVisible(!product.rascladMode);
    	//Сокрытие функций, недоступных в режиме раскладки
		if(H != canvas.getHeight() || W != canvas.getWidth()) {
			
			H = canvas.getHeight();
			W = canvas.getWidth();
			updateFields(0);
		}
		if(!product.rascladMode) {
			scale.setVisible(true);
			for(int i = 0; i < detail.vertices.size(); i++) {
				try {
					float X = Float.parseFloat(Xfields.get(i).getText()) / 1000,
						  Y = Float.parseFloat(Yfields.get(i).getText()) / 1000;
					if(detail.vertices.get(i).X != X || detail.vertices.get(i).Y != Y) {
						detail.vertices.get(i).X = X;
						detail.vertices.get(i).Y = Y;
						canvas.paint(canvas.getGraphics());
						break;
					}	
        		} catch (Exception ex) { break;}
			}	
			reDraw(detail, canvas.getGraphics(), 0);	
        }
		else Rasclad(0);	
		
	}
	private void keyControl() {//Обработка нажатий и комбинаций клавиш
		Action ctrlZ = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { undo(); }    
        }; 
        InputMap inputMap = canvas.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = canvas.getActionMap();
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK), "ctrl + Z");
        actionMap.put("ctrl + Z", ctrlZ);
        
        Action ctrlShiftZ = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { redo(); }    
        };
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "ctrl + Shift + Z");
        actionMap.put("ctrl + Shift + Z", ctrlShiftZ);
        
        Action save = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { saveFile(); }
        };
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK), "Ctrl + S");
        actionMap.put("Ctrl + S", save);
            
        Action rotate = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { rotate(); }
        };
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_DOWN_MASK), "Alt + R");
        actionMap.put("Alt + R", rotate);
        
        Action horizontal = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { flipHorizontal(); }
        };
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_H, InputEvent.ALT_DOWN_MASK), "Alt + H");
        actionMap.put("Alt + H", horizontal);
        
        Action vertical = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { flipVertical(); }
        };
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, InputEvent.ALT_DOWN_MASK), "Alt + V");
        actionMap.put("Alt + V", vertical);
        
        Action scale = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { scaleDetail(); }
        };
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_DOWN_MASK), "Alt + S");
        actionMap.put("Alt + S", scale);
        
        Action shiftX = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { shiftX(); }
        };
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.ALT_DOWN_MASK), "Alt + X");
        actionMap.put("Alt + X", shiftX);
        
        Action shiftY = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { shiftY(); }
        };
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, InputEvent.ALT_DOWN_MASK), "Alt + Y");
        actionMap.put("Alt + Y", shiftY);
        
        Action changeRasclad = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { 
            	if(!product.changed) Rasclad(0);
            }
        };
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_E, InputEvent.SHIFT_DOWN_MASK), "Shift + E");
        actionMap.put("Shift + E", changeRasclad);
        
        Action fastRasclad = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { Rasclad(1); }
        };
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, InputEvent.SHIFT_DOWN_MASK), "Shift + F");
        actionMap.put("Shift + F", fastRasclad);
        
        Action RascladAI = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { Rasclad(2); }
        };
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, InputEvent.SHIFT_DOWN_MASK), "Shift + A");
        actionMap.put("Shift + A", RascladAI);
        
        Action exitRasclad = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { exitRasclad();} 	   
        };
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, InputEvent.SHIFT_DOWN_MASK), "Shift + X");
        actionMap.put("Shift + X", exitRasclad);
        
        Action update = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { updateFields(1);} 	   
        };
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "F5");
        actionMap.put("F5", update);
        
        Action delete = new AbstractAction() {
            public void actionPerformed(ActionEvent e) { 
            	if(detail.current >= 0) deleteDot(detail.current); 
            }
        };
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "Del");
        actionMap.put("Del", delete);
	}
	
	private void reDraw(Detail d, Graphics g, int param) //Функция для отрисовки детали
	{	
		if(g == null) return;
	    int i, n = d.vertices.size();
	    int[] xPoints = new int[n];
	    int[] yPoints = new int[n];
	    for(i = 0; i < n; i++) {
            xPoints[i] = d.vertices.get(i).intX(H * product.scaling);
            yPoints[i] = d.vertices.get(i).intY(H * product.scaling);
    	}//Отрисовка точек на панели canvas с использованием масштабирования нормализованных координат
    	if(n > 2) {
    		if(d == detail && product.rascladMode) g.setColor(Color.yellow);
	            else g.setColor(new Color(0, 228, 228)); 
            g.fillPolygon(xPoints, yPoints, n);
        }
    	else if(n == 2) {
        	if(d == detail && product.rascladMode && param == 0) g.setColor(Color.yellow);
	            else g.setColor(new Color(0, 228, 228)); 
        	g.drawLine(xPoints[0], yPoints[0], xPoints[1], yPoints[1]); 
        }
        if(n > 0 && d == detail && !product.rascladMode) {
        	g.setColor(Color.red); 
            g.fillOval(xPoints[detail.current] - 3, yPoints[detail.current] - 3, 6, 6);     
        }  	
	    if(dotChanged) {
	        canvas.paint(canvas.getGraphics());
	        dotChanged = false;
	    }
	}
	private void setScale(int mode) {
	    try {
	        if (mode == 0) { // Ручной ввод масштаба
	            product.scaling = Float.parseFloat(JOptionPane.showInputDialog("Введите масштаб в %: ")) / 100f;

	            if (product.scaling <= 0f) product.scaling = 1f;
	            scale.setText("Масштаб: " + (int)(product.scaling * 100) + "%");
	            updateFields(0);
	        }
	        else if (mode == 1) { // Автоматическое масштабирование под окно
	            // Расчёт масштабов по ширине и высоте
	            float scaleByWidth  = (canvas.getWidth() * 0.95f) / (product.listWidth  * H);
	            float scaleByHeight = (canvas.getHeight() * 0.95f) / (product.listHeight * H);
	            // Берём минимальный, чтобы всё влезло
	            product.scaling = Math.min(scaleByWidth, scaleByHeight);

	            if (product.scaling > 1f) product.scaling = 1f;
	            if (product.scaling <= 0f) product.scaling = 1f;
	            scale.setText("Масштаб: " + (int)(product.scaling * 100) + "%");
	            if(dotChanged) {
	            	updateFields(1);
	            	dotChanged = false;
	            }
	        }
	    }
	    catch (Exception e) {
	        // игнорируем неверный ввод
	    }
	}

	void Rasclad(int mode) {//Функция для раскладки
		if(mode == 2 && product.details.size() < 3) {
			JOptionPane.showMessageDialog(null, "Для ИИ-раскладки нужно не менее 3 деталей!", "Сообщение", JOptionPane.WARNING_MESSAGE);
			return;
		}
		scale.setVisible(false);
		if(mode == 0)setScale(1);
		int count = 0;
		for(var d : product.details) {
			if(d.vertices.size() < 3) d.onRasclad = false;
			if(d.onRasclad) count++;
		}
		if (count > 0) {
			float height = product.listHeight;
			if(!product.rascladMode || mode > 0) {
				if(mode > 0) try {
					updateFields(1); 
					String input = JOptionPane.showInputDialog("Введите расстояние между лекалами в мм: ", 10);
			        product.distance = Float.parseFloat(input) / 1000;
			        input = JOptionPane.showInputDialog("Введите ширину полотна в мм: ", (int)(product.listHeight * 1000));
			        height = Float.parseFloat(input) / 1000;
		        } 
				catch (Exception e) { 
					return; 
				}    	
				if(mode == 2) setVisible(false);
				if(mode > 0 && product.findRect(height, mode)) {
					product.rascladMode = true;	
					if(!detail.onRasclad) 
						for(var d : product.details) 
							if(d.onRasclad) {
								detail = d;
								selected = d.index;
								break;
							}
					scale.setText("Масштаб: "+ Math.round(product.scaling * 100) + "%");
					updateFields(1); 
				}	
				else undo();
			}
			else {
				product.listWidth = 0;
				for(var d : product.details) 
					if(d.Xmax() > product.listWidth) product.listWidth = d.Xmax();
			}
			var g = canvas.getGraphics();
			if(g != null) {
				for(var d : product.details) if(d.onRasclad) reDraw(d, g, 0);
				g.setColor(Color.cyan);
				g.drawRect(0, 0, (int)(product.listWidth * H * product.scaling) + 4, (int)(product.listHeight * H * product.scaling) + 4);
			}
			
			scale.setText("Масштаб: " + (int)(product.scaling * 100) + "%");
			
			product.changed = false;
			product.getProperties();		
			area.setText("Параметры изделия");	
			
		}
		else {
			JOptionPane.showMessageDialog(null, "Нет деталей для раскладки!", "Ошибка", JOptionPane.WARNING_MESSAGE);	
			exitRasclad();
		}
	}		
	
	private void getArea() {
		if(product.rascladMode) {
			JOptionPane.showMessageDialog(null, product.properties, "Параметры изделия", JOptionPane.INFORMATION_MESSAGE);
		}
		else JOptionPane.showMessageDialog(null, "Площадь детали = " + detail.S() + " cм2", "Площадь детали", JOptionPane.INFORMATION_MESSAGE);	
	}
	private Boolean saveAsk(Boolean need_dispose) {
		if (product.totalVertices() == 0 && product.details.size() < 2) {
            if(need_dispose) dispose(); // нет данных — просто закрываем
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
            dispose();
            return true;
        } else if (option == JOptionPane.NO_OPTION) {
            dispose();
            return true;
        } 
        return false;
    }
	public void saveFile() {
		if(product.filePath.length() > 2) product.saveToFile(product.filePath);
		else saveFileAs();
	}
	private void openFile() {
		try {
			if(!saveAsk(false)) return;
			var fileChooser = new JFileChooser();
	        fileChooser.setDialogTitle("Открыть файл");
	        var filter = new FileNameExtensionFilter("*.xml", "xml");
	        fileChooser.setFileFilter(filter);
	        
	        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
	            File fileToSave = fileChooser.getSelectedFile();
	            product.filePath = fileToSave.getAbsolutePath();
	            if(product.filePath.endsWith(".xml")) {
	            	dispose();
	            	new Form1(new Product(product.filePath), product.filePath);
	            }
	            else JOptionPane.showMessageDialog(null, "Неверное расширение файла!", "Ошибка", JOptionPane.ERROR_MESSAGE);            
	        }
	        else {
	        	dispose();
	        	new Form1(product, product.filePath);
	        	JOptionPane.showMessageDialog(null, "Открытие файла отменено.", "Сообщение", JOptionPane.INFORMATION_MESSAGE);
	        };
		}
		catch(Exception ex) {
			JOptionPane.showMessageDialog(null, "Данные в файле повреждены!", "Ошибка", JOptionPane.ERROR_MESSAGE);
		}
    }
	private void saveImage() {
		try {
			if(product.rascladMode || detail.vertices.size() > 1) {
				var fileChooser = new JFileChooser();
		        fileChooser.setDialogTitle("Сохранить как PNG");
		        var filter = new FileNameExtensionFilter("*.png", "Изображение");
		        fileChooser.setFileFilter(filter);
		        int userSelection = fileChooser.showSaveDialog(null);
		        if (userSelection == JFileChooser.APPROVE_OPTION) {
		        	BufferedImage image;
		        	if(product.rascladMode) {
		        		int h = (int)(product.listHeight * H * product.scaling),
			        	w = (int)(product.listWidth * W * product.scaling / 1.55f);
		        		image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			        	var g = image.createGraphics();
			        	g.setColor(new Color(40, 80, 120));
			        	g.fillRect(0, 0, w, h);
			        	for(var d : product.details) if(d.onRasclad) reDraw(d, g, 0);
			        	g.dispose();
		        	}
		        	else {
		        		var d = new Detail(detail);
		        		d.normalize();
		        		int h = Math.round(d.Ymax() * H * product.scaling), w = (int)(d.Xmax() * W * product.scaling / 1.55f);
		        		image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
			        	var g = image.createGraphics();
			        	g.setColor(new Color(40, 80, 120));
			        	g.fillRect(0, 0, w, h); reDraw(d, g, 1); 
		    		    g.dispose();
		        	}     
		            String path = fileChooser.getSelectedFile().getAbsolutePath();
		            if(!path.endsWith(".png") && !path.endsWith(".PNG")) path += ".png";
		            ImageIO.write(image, "PNG", new File(path));
		            JOptionPane.showMessageDialog(null, "Снимок раскладки сохранён в файл " + path, "Сообщение", JOptionPane.INFORMATION_MESSAGE);
		            Desktop.getDesktop().open(new File(path));
		        }
			}
			else JOptionPane.showMessageDialog(null, "Линии отсутствуют.", "Ошибка", JOptionPane.WARNING_MESSAGE);
		}	
		catch(Exception e) {}		
    }
	private void saveFileAs() {
		if(product.totalVertices() < 3 && product.details.size() < 2) {
			JOptionPane.showMessageDialog(null, "Нечего сохранять.", "Ошибка", JOptionPane.WARNING_MESSAGE);
			return;
		}
		var fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Сохранить как");
        var filter = new FileNameExtensionFilter("*.xml", "XML");
        fileChooser.setFileFilter(filter);
        int userSelection = fileChooser.showSaveDialog(null);
        Product backup = new Product(product, 0);
        try {
        	if (userSelection == JFileChooser.APPROVE_OPTION) {
                File fileToSave = fileChooser.getSelectedFile();
                product.filePath = fileToSave.getAbsolutePath();
                product.saveToFile(product.filePath);
            }
        }
        catch(Exception ex) {
        	product = backup;
        }
        updateFields(0);
    }
	public void updateFields(int param) //Перерисовка всех элементов интерфейса
	{
		product.updateTree();
		onRasclad.setVisible(!product.rascladMode);
		if(editScroll != null) editScroll.setVisible(!product.rascladMode);
		if(editPanel != null) editPanel.setVisible(!product.rascladMode);
		exitRasclad.setVisible(product.rascladMode);
		exitRasclad2.setVisible(product.rascladMode);
		
		if(product.properties != null && product.properties != "" && !product.rascladMode && !product.changed) 
		{
			changeRasclad.setVisible(true);
		}
		else changeRasclad.setVisible(false);
			
		DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
        model.setRoot(product.root);
		model.reload();
		thisDetail.setText(selected + 1 + ". " + detail.name);
		onRasclad.setSelected(detail.onRasclad);
		if(param == 0) recordAction();
		editPanel.removeAll();
		editPanel.setText("");
		Xfields.clear(); Yfields.clear();
		if(!product.rascladMode) {
			editPanel.add(dotCoords);
			editPanel.add(X);
			editPanel.add(Y);
			for(int i = 0; i < detail.vertices.size(); i++) {
				editPanel.setText(editPanel.getText() + "\n\n");
				var Xfield = new NumericField(i);
		        var Yfield = new NumericField(i);
		        
		        Xfield.addMouseListener(new MouseAdapter() {
		            @Override
		            public void mousePressed(MouseEvent e) {
		            	if(Xfield.index != detail.current) {
		            		detail.current = Xfield.index;	
			            	updateFields(0);
		            	}
		            }
		        });
		        Yfield.addMouseListener(new MouseAdapter() {
		            @Override
		            public void mousePressed(MouseEvent e) {
		            	if(Yfield.index != detail.current) {
		            		detail.current = Yfield.index;
		            		updateFields(0);
		            	}
		            }
		        });
		        Xfield.setText(Math.round(detail.vertices.get(i).intX(100000)) / 100f + "");
		        Yfield.setText(Math.round(detail.vertices.get(i).intY(100000)) / 100f + "");
		        Xfields.add(Xfield);
		        Yfields.add(Yfield);
		        if(i == detail.current) updateList(i, 60, Font.BOLD);
		        else updateList(i, 60, Font.PLAIN);
			}
		}
		repaint();
	}
	private void updateList(int i, int offset, int font)
	{ //Перерисовка правой панели
		dotChanged = true;
		var num = new JLabel(i + 1 + "");		
		var delete = new ToolButton("delete.png", "Удалить точку (клавиша Del)");
	    delete.setBounds(170, offset + 1 + 30 * i, 23, 23);
	    delete.addActionListener(e -> deleteDot(i));    
	    num.setBounds(8, offset + 2 + 30 * i, 20, 20);  
	    num.setFont(new Font("Arial", font, 14));
	    editPanel.add(num);
	    editPanel.add(delete);
	    Xfields.get(i).setBounds(30,  offset + 30 * i, 65, 25);
	    Yfields.get(i).setBounds(100, offset + 30 * i, 65, 25);
	    editPanel.add(Xfields.get(i)); 
   	 	editPanel.add(Yfields.get(i)); 
	}
	private void deleteDot(int i) {
		detail.vertices.remove(i);
		if(detail.current > 0) detail.current--;
		updateFields(0);
	}	
	private void recordAction() {
		undoRedo++;
		scale.setText("Масштаб: " + (int)(product.scaling * 100) + "%");
		while(undoRedo < Backup.size()) {
			Backup.remove(Backup.size() - 1);
			Selected.remove(Selected.size() - 1); 
		}		
		Backup.add(new Product(product, 0));
		Selected.add(selected);
	}
	private void undo() {
		if(undoRedo > 0) {
			undoRedo--;
			product = new Product(Backup.get(undoRedo), 0);
			selected = Selected.get(undoRedo);
			detail = product.details.get(selected);
			scale.setText("Масштаб: " + (int)(product.scaling * 100) + "%");
			updateFields(1);
			tree.setSelectionRow(selected + 1);
			dotChanged = true;
		}		
	}
	private void redo() {		 
		if(undoRedo < Backup.size() - 1) {
			undoRedo++;
			product = new Product(Backup.get(undoRedo), 0);
			selected = Selected.get(undoRedo);
			detail = product.details.get(selected);
			updateFields(1);
			tree.setSelectionRow(selected + 1);
			dotChanged = true;
		} 	
	}
	private void rotate() {
		try {
			if(!product.rascladMode) {
				if(detail.vertices.size() > 1) {
					String S = JOptionPane.showInputDialog("Введите угол поворота в градусах: ");
					detail.rotate(Double.parseDouble(S)); 
					if(detail.minX() < 0) detail.shiftX(-detail.minX());
					if(detail.minY() < 0) detail.shiftY(-detail.minY());
					updateFields(0);
				}
				else JOptionPane.showMessageDialog(null, "Вращать нечего.", "Ошибка", JOptionPane.WARNING_MESSAGE);
			}
		}
		catch(Exception e) {}	
	}
	private void scaleDetail() {
		try {//Диалоговое окно установления масштаба изображения.
			if(!product.rascladMode) {
				if(detail.vertices.size() > 1) {
					String S = JOptionPane.showInputDialog("Во сколько раз изменить размер детали?", 1);
					detail.scale(Float.parseFloat(S));
					updateFields(0);
				}
				else JOptionPane.showMessageDialog(null, "Отрезки не найдены!", "Ошибка", JOptionPane.WARNING_MESSAGE);   
			}
		} catch(Exception e) {}
	}
	private void shiftX() {
		try {//Диалоговое окно сдвига детали по оси X
			if(!product.rascladMode) {
				if(detail.vertices.size() > 0) {
					String S = JOptionPane.showInputDialog("На сколько мм сдвинуть деталь по оси X", 0);
					detail.shiftX(Float.parseFloat(S) / 1000);
					if(detail.minX() < 0) detail.shiftX(-detail.minX());
					updateFields(0);
				}
				else JOptionPane.showMessageDialog(null, "Вершины не найдены!", "Ошибка", JOptionPane.ERROR_MESSAGE);   
			}
			
		} catch(Exception e) {}
	}
	private void shiftY() {
		try {//Диалоговое окно сдвига детали по оси Y
			if(!product.rascladMode) {			
				if(detail.vertices.size() > 0) {
					String S = JOptionPane.showInputDialog("На сколько мм сдвинуть деталь по оси X", 0);
					detail.shiftY(Float.parseFloat(S) / 1000);
					if(detail.minY() < 0) detail.shiftX(-detail.minY());
					updateFields(0);
				}
				else JOptionPane.showMessageDialog(null, "Вершины не найдены!", "Ошибка", JOptionPane.ERROR_MESSAGE);   
			}
		} catch(Exception e) {}
	}
	private void flipVertical() 
	{//Отражение детали по вертикали
		if(detail.vertices.size() > 1) {
			detail.flipVertical();
			updateFields(0);
		}
		else JOptionPane.showMessageDialog(null, "Отрезки не найдены!", "Ошибка", JOptionPane.ERROR_MESSAGE);
	}
	private void flipHorizontal() 
	{//Отражение детали по горизонтали
		if(detail.vertices.size() > 1) {
			detail.flipHorizontal();
			updateFields(0);
		}
		else JOptionPane.showMessageDialog(null, "Отрезки не найдены!", "Ошибка", JOptionPane.ERROR_MESSAGE);
	}
	private void exitRasclad() {
		product.rascladMode = false;
    	updateFields(0);//Выход из режима раскладки
	}
	//Действия через левое меню
	private void addDetail(Detail t, int mode, int index) 
	{//Создание новой детали
		var select = tree.getSelectionModel();
    	select.setSelectionPath(tree.getPathForRow(0));
        if(mode == 0) {
        	String S = JOptionPane.showInputDialog("Введите название детали: ", t.name);
        	
        	if(S != null && S.length() > 0) {
        		if(S.length() > 30) {
            		JOptionPane.showMessageDialog(null, "Название должно быть не больше 30 символов!", "Сообщение", JOptionPane.WARNING_MESSAGE);  
            	}
        		else t.name = S; 
        	}
        	else return;
        }
        selected = index + 1; detail = t;
        product.details.add(selected, detail);
        detail = product.details.get(selected);
        product.rascladMode = false;         
        var newNode = new DefaultMutableTreeNode(detail.name);
        var selectedNode = (DefaultMutableTreeNode) tree.getSelectionPath().getLastPathComponent();
        selectedNode.insert(newNode, selected);
        
        updateFields(1);
        tree.setSelectionPath(tree.getPathForRow(selected + 1)); 
        treeClick(0);
	}
	private void removeDetail() 
	{//Создание выбранной детали
		TreePath path = tree.getPathForLocation(treeX, treeY);
        if (path != null) {
            var selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
            selected = selectedNode.getParent().getIndex(selectedNode);            
            product.details.remove(selected);
            
            while(selected >= product.details.size()) selected--;
            detail = product.details.get(selected);
            tree.setSelectionPath(tree.getPathForRow(selected + 1)); 
            treeClick(0);
        }
	}
	private void rename() 
	{//Переименование выбранной детали
		TreePath path = tree.getPathForLocation(treeX, treeY);
		TreePath path2 = tree.getPathForRow(0);
        if (path != null) {
        	String S0 = "";
        	Detail d = detail;
        	var selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        	if(path != path2) {
                var parent = (DefaultMutableTreeNode) selectedNode.getParent();
    	        int i = parent.getIndex(selectedNode);
    	        d = product.details.get(i);
    	        S0 = d.name;
        	}
        	else S0 = product.name;
        	String S = JOptionPane.showInputDialog("Введите новое имя: ", S0);
            if(S != null && S.length() > 0 && S != S0) {
            	if(S.length() > 30) {
            		JOptionPane.showMessageDialog(null, "Название должно быть не больше 30 символов!", "Сообщение", JOptionPane.WARNING_MESSAGE);  
            		return;
            	}
            	selectedNode.setUserObject(S); 
            	if(path == path2) product.name = S;
            	else d.name = S;
                
            }              
        }
        tree.setSelectionPath(tree.getPathForRow(selected + 1)); 
        treeClick(0);
	}
	private void setDescription() {
		String S = JOptionPane.showInputDialog("Описание изделия: ", product.description);
		if(S != null && S.length() > 0) product.description = S;
	}
	private void about() {//Окно о программе
		String[] array = {
				"Автоматизация раскладки лекал", 
				"© Золотарёв Дмитрий Андреевич,", 
				"Черненко Елена Александровна." };				
        StringBuilder sb = new StringBuilder("<html>");
        for (String str : array) sb.append(str).append("<br>");
        sb.append("</html>");
        JOptionPane.showMessageDialog(null, sb.toString(), "О программе", JOptionPane.INFORMATION_MESSAGE);
	}
	
	private void treeClick(int mode) {
		try {//Выбор детали через левое меню со списом деталей в изделии
        	if (tree.getSelectionPaths() != null) 
        	    for (TreePath path : tree.getSelectionPaths()) 
        	    {               	    	
        	    	var selectedNode = (DefaultMutableTreeNode) path.getLastPathComponent();
        	        var parent = (DefaultMutableTreeNode) selectedNode.getParent();
        	        selected = parent.getIndex(selectedNode);
        	        detail = product.details.get(selected);
        	        if(!detail.onRasclad) product.rascladMode = false;
        	        area.setText("Площадь детали");
        	        reDraw(detail, canvas.getGraphics(), 0); 
        	        updateFields(mode);
        	        tree.setSelectionPath(tree.getPathForRow(selected + 1));
        	        if(product.details.size() > 1) popupMenu1.add(delete);
        			else popupMenu1.remove(delete);
        	    }
        }
        catch(Exception ex) { return; }	
	}
}