import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.tree.*;
import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.*;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.*;

public class Product {//Класс для описания абстрактной модели изделия
	DefaultMutableTreeNode root = null;
	List<Detail> details = new ArrayList<>();
	String name = "Новое изделие", description = "", properties = "";
	String filePath = "";
	Boolean rascladMode = false, changed = false;
	Form1 main;
	Document doc;
	float listWidth = 0, listHeight = 1,  minX = 99, minY = 99, scaling = 1, distance = 0; 	
	Product() { 
		details.add(new Detail("Новая деталь"));
		updateTree();
	}
	public Product(Product other, int mode) {
		name = other.name;
		distance = other.distance;
		filePath = other.filePath;
		description = other.description;
		properties = other.properties;
		listHeight = other.listHeight;
		rascladMode = other.rascladMode;
		scaling = other.scaling;
	    for(int i = 0; i < other.details.size(); i++) 
	    	if(other.details.get(i).onRasclad || mode == 0){
	    		Detail detail = new Detail(other.details.get(i));   
	    		detail.index = i;
	    		details.add(detail);
	    	}	
	}
	public Product(String xmlFile) {
	    try {	    	
	    	filePath = xmlFile;
	        details.clear();
	        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	        DocumentBuilder builder = factory.newDocumentBuilder();
	        Document doc = builder.parse(new File(xmlFile));
	        Element rootElement = doc.getDocumentElement();
	        name = rootElement.getAttribute("Имя");
	        description = rootElement.getAttribute("Описание");
	        properties = rootElement.getAttribute("Свойства");
	        listHeight = Float.parseFloat(rootElement.getAttribute("Ширина_полотна"));
	        listWidth = Float.parseFloat(rootElement.getAttribute("Длина_полотна"));
	        scaling = Float.parseFloat(rootElement.getAttribute("Масштаб"));
	        rascladMode = Boolean.parseBoolean(rootElement.getAttribute("Раскладка"));
	        changed = Boolean.parseBoolean(rootElement.getAttribute("Было_ли_редактирование"));
	        NodeList detailNodes = rootElement.getElementsByTagName("Деталь");
	        for (int i = 0; i < detailNodes.getLength(); i++) {
	            Element detailElement = (Element) detailNodes.item(i);
	            Detail detail = new Detail(detailElement.getAttribute("Имя")); 
	            detail.onRasclad = Boolean.parseBoolean(detailElement.getAttribute("На_раскладку"));
	            NodeList vertexNodes = detailElement.getElementsByTagName("Точка");
	            for (int j = 0; j < vertexNodes.getLength(); j++) {
	                Element vertexElement = (Element) vertexNodes.item(j);
	                float x = Float.parseFloat(vertexElement.getAttribute("X"));
	                float y = Float.parseFloat(vertexElement.getAttribute("Y"));
	                detail.vertices.add(new Dot(x, y));
	            }
	            details.add(detail);
	        }
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    updateTree();
	}
	public void SortDetails() {
		int i, j = 0, n = details.size();
		for(i = 0; i < n; i++) {
			details.get(i).normalize();
			details.get(i).S = details.get(i).S();
			details.get(i).index = i;
		}
		for (i = n - 2; i >= 0; i--) 
		    for (j = 0; j <= i; j++) { 
		        if (details.get(j).S < details.get(j + 1).S) {
		            Detail d = details.get(j);
		            details.set(j, details.get(j + 1));
		            details.set(j + 1, d);
		        }
		    }
	}
	public Boolean findRect(float height, int mode) 
	{//Нахождение прямоугольного листа с РАСКЛАДКОЙ ДЕТАЛЕЙ с заданной шириной и наименьшей длиной 
    	listHeight = height; 
    	for(int i = 0; i < details.size(); i++) {
    		float W = details.get(i).Xmax() - details.get(i).minX();
    		float H = details.get(i).Ymax() - details.get(i).minY();
    		if(H > height && W > height) {
    			JOptionPane.showMessageDialog(null, "Деталь №" + (i + 1) + "Не влезает по ширине!", "Ошибка", JOptionPane.ERROR_MESSAGE); 
    			return false;
    		}
    		else if (H > height) details.get(i).rotate(90);
    	}
    	fastLayout();
    	if(mode == 2) AILayout(distance, height);
    	return true;
    }
	private void fastLayout() {
	    listWidth = 0;
	    Product t = new Product(this, 1);
	    t.SortDetails();

	    ArrayList<Detail> used = new ArrayList<>();

	    for (int i = 0; i < t.details.size(); i++) {
	        Detail d = t.details.get(i);
	        if (i > 0) {
	            float H = used.get(used.size() - 1).Ymax() + distance;
	            if (H + d.Ymax() > listHeight) {
	                d.rotate(90);
	                d.normalize();
	            }
	            if (H + d.Ymax() <= listHeight) d.shiftY(H);
	            else {
	                d.rotate(-90);
	                d.normalize();
	            }
	            d.shiftX(listWidth + distance);
	        }
	        d.packX(used, 0.01f, distance);
	        d.packY(used, 0.01f, distance);
	        details.get(d.index).vertices = d.vertices;
	        if (d.Xmax() > listWidth) listWidth = d.Xmax();
	        used.add(d);
	    }
	}

	private void AILayout(float distance, float height) {  
		try {
			var input = JOptionPane.showInputDialog("Число просчётов ИИ-раскладки, влияющее на её эффективность: ", 1000);
			if(input == null) {
				main.setVisible(true);
				return;
			}
			int accuracy = Integer.parseInt(input) * totalVertices();	
			findRect(height, 1);
			Detail d = new Detail();
			//Инициализация окна, показывающего процент завершения раскладки.
			var indicator = new JFrame();
			var progress = new JProgressBar();
			progress.setMinimum(0);
			progress.setMaximum(1000);
			var label = new JLabel("Раскладка завершена на 0.0%");
			label.setFont(new Font("Arial", Font.PLAIN, 24));
			label.setHorizontalAlignment(SwingConstants.CENTER); 
			label.setAlignmentY(100);
			
			indicator.setLocation(200, 150);
			indicator.setSize(640, 160);
			indicator.add(progress);
			indicator.add(label, BorderLayout.SOUTH);
			indicator.setVisible(true);
			indicator.setLayout(new BorderLayout());
			indicator.setTitle("ИИ-раскладка");
			indicator.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			indicator.setResizable(false); 
			indicator.addWindowListener(new WindowListener() {
				public void windowActivated(WindowEvent event) {}
				public void windowClosed(WindowEvent event) {			
				}
				public void windowClosing(WindowEvent event) {
					progress.setValue(1000);
					indicator.dispose();
					JOptionPane.showMessageDialog(null, "ИИ-раскладка завершена пользователем", "Сообщение", JOptionPane.INFORMATION_MESSAGE);
					main.setVisible(true);
				}
				public void windowDeactivated(WindowEvent event) {}
				public void windowDeiconified(WindowEvent event) {}
				public void windowIconified(WindowEvent event) {}
				public void windowOpened(WindowEvent event) {}
			});
			Action stop = new AbstractAction() {
	            public void actionPerformed(ActionEvent e) { 
					progress.setValue(1000);
					indicator.dispose();
					JOptionPane.showMessageDialog(null, "ИИ-раскладка завершена пользователем", "Сообщение", JOptionPane.INFORMATION_MESSAGE);
					main.setVisible(true);
				}    
	        }; 
	        InputMap inputMap = progress.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
	        ActionMap actionMap = progress.getActionMap();
	        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "Escape");
	        actionMap.put("Escape", stop);	
			new AI(this, d, distance, accuracy, main, progress, label, indicator).start();	
		}
		catch(Exception e) {
			main.setVisible(true);
			return;
		}
	}
	public int totalVertices() {
		int count = 0;
		for(var i : details)
			for(var j : i.vertices) count++;
		return count;
	}
	public int Area() {
		int area = 0;
		for (var d : details) area += d.S();
		return area;
	}
	public float rectArea() {
		return listHeight * listWidth * 10000;
	}
	public float Xmax() {
		float Xmax = 0;
		for(var d: details) if(d.Xmax() > Xmax) Xmax = d.Xmax();
		return Xmax;
	}
	public float Ymax() {
		float Ymax = 0;
		for(var d: details) if(d.Ymax() > Ymax) Ymax = d.Ymax();
		return Ymax;
	}
	private void createDoc() {
		try {
	        doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
	        Element productElement = doc.createElement("Изделие");
	        productElement.setAttribute("Имя", name);
	        productElement.setAttribute("Описание", description);
	        productElement.setAttribute("Свойства", properties);
	        productElement.setAttribute("Раскладка", Boolean.toString(rascladMode));
	        productElement.setAttribute("Ширина_полотна", Float.toString(listHeight));
	        productElement.setAttribute("Длина_полотна", Float.toString(listWidth));
	        productElement.setAttribute("Масштаб", Float.toString(scaling));
	        productElement.setAttribute("Было_ли_редактирование", Boolean.toString(changed));
	        doc.appendChild(productElement);
	        if (!details.isEmpty()) {
	            for (Detail detail : details) 
	            	if(!detail.vertices.isEmpty()) {
	            		Element detailElement = doc.createElement("Деталь");
		                detailElement.setAttribute("Имя", detail.name);
		                detailElement.setAttribute("На_раскладку", Boolean.toString(detail.onRasclad));
		                for (Dot vertex : detail.vertices) {
		                    Element vertexElement = doc.createElement("Точка");
		                    vertexElement.setAttribute("X", String.valueOf(vertex.X));
		                    vertexElement.setAttribute("Y", String.valueOf(vertex.Y));
		                    detailElement.appendChild(vertexElement);
		                }	                
		                productElement.appendChild(detailElement);
	            	}   
	        }	        
	        updateTree();
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	}
	public void saveToFile(String xmlFilePath) {
	    try {
	        createDoc();
	        if(!xmlFilePath.endsWith(".xml")) xmlFilePath += ".xml";
	        Transformer transformer = TransformerFactory.newInstance().newTransformer();
	        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
	        DOMSource source = new DOMSource(doc);
	        
	        StreamResult result = new StreamResult(new File(xmlFilePath));
	        transformer.transform(source, result);
	        JOptionPane.showMessageDialog(null, "Данные сохранены в файл " + xmlFilePath, "Сообщение", JOptionPane.INFORMATION_MESSAGE);
	    } catch (Exception e) {  }
	}
    public void updateTree() {
    	root = new DefaultMutableTreeNode(name);
    	for(int i = 0; i < details.size(); i++) {
    		var file = new DefaultMutableTreeNode(i + 1 + ". "+ details.get(i).name);
    		root.add(file);  		
    	}
    }
    public void generateTestDetails() 
    {
    	try {
    		details.clear();
        	int n = 0;
    		var input = JOptionPane.showInputDialog("Число тестовых лекал: ", 20);
    		if (input != null) n = Integer.parseInt(input);
    		
    		if(n < 1 || n > 100) 
    		{
    			 JOptionPane.showMessageDialog(null, "Введено некорректное число!", "Ошибка", JOptionPane.ERROR_MESSAGE);
    			 return;
    		}
    		for(int i = 0; i < n; i++) 
    		{
    			var detail = new Detail();
    			detail.generateDots();
    			details.add(detail);
    		}
    	}
    	catch(Exception e) {
    		
    	} 	
    }
    public float findListWidth() 
    {
    	for(var d: details) {
    		if(d.Xmax() > listWidth) listWidth = d.Xmax();
    	}
    	return listWidth;
    }
    public Boolean collision() 
    {
        for (int i = 0; i < details.size() - 1; i++) 
        	for (int j = i + 1; j < details.size(); j++) 
        		if (details.get(i).intersects(details.get(j))) return true;
        return false;
    }
    public void getProperties() {
    	String[] array = {"Ошибка: есть лекала, выходящие за полотно!"};
    	String[] array2 = {"Ошибка: есть пересечения лекал!"};
    	
    	if(collision()) array = array2;
    		
    	if(Ymax() <= listHeight && array != array2) {
    		String[] array3 = {
    				"Название: " + name,
    				"Описание: " + description,
    				"Суммарная площадь деталей: " + (int) Area() + " cм2.",
    				"Размеры полотна:", 
    				"Длина: " + Math.round(listWidth * 1000) + " мм;", 
    				"Ширина: " + Math.round(listHeight * 1000) + " мм;",	
    				"Межлекальных отходов: " + (int)((rectArea() - Area()) / rectArea() * 1000) / 10f + "%"};
    		array = array3;
    	}						
        StringBuilder sb = new StringBuilder("<html>");
        for (String str : array) sb.append(str).append("<br>");
        sb.append("</html>");
        properties = sb.toString();       
	}
}
